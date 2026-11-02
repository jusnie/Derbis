/*
 * Session-wide kill switch and forensics for the Liquid Glass GPU path.
 *
 * The backdrop engine draws through RenderEffect and AGSL RuntimeShader. Both are
 * perfectly legal Kotlin calls that a GPU driver can still refuse at paint time — and
 * some do it by returning garbage, some by throwing from the draw pass, a few by
 * taking the process down natively. Nothing on the Kotlin side can predict which.
 *
 * So the rule is: glass is allowed to look worse, never to crash the app.
 *
 *  - A failure raised while drawing glass is reported here once; every glass node
 *    reads [disabled] on its next draw and falls back to the plain tinted material.
 *    Because the check happens inside the draw pass the fallback is immediate, it does
 *    not wait for a recomposition.
 *  - A native fault never reaches Kotlin, so there is a second, cruder signal: while
 *    glass is actually on screen in the foreground a marker is kept on disk, and the
 *    marker is cleared as soon as the app goes to the background. If the process dies
 *    in the foreground — the only way that marker survives — the next launch knows the
 *    last session ended under glass and keeps glass off for a while instead of letting
 *    the user walk into the same crash again.
 *  - Java crashes that happen while glass is drawing are written to disk by the
 *    uncaught-exception handler installed in [install] and shown by [describe], which
 *    is appended to Settings → Share diagnostics.
 */
package com.yasuda.derbis.ui.components.backdrop

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.yasuda.derbis.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import timber.log.Timber

object DerbisGlassSafety {

    private const val PREFS = "derbis_glass_safety"
    private const val KEY_ARMED = "glass_armed_at"
    private const val KEY_LAST_BREAK = "glass_last_break_at"
    private const val KEY_BREAK_VERSION = "glass_last_break_version"
    private const val KEY_DEATHS = "glass_foreground_deaths"
    private const val KEY_VERSION = "glass_version"
    private const val CRASH_FILE = "liquid-glass-crash.txt"

    /**
     * How long glass stays off after a session ended while it was drawing.
     *
     * Deliberately short. The old six-hour window meant one bad launch - a driver fault, a
     * kill from the system, or a crash anywhere in the app - left every surface on the flat
     * frosted fallback for the rest of the day, which is indistinguishable from "the glass
     * fix did nothing". A failed session is worth one cautious retry, not a morning.
     */
    private const val RETRY_WINDOW_MS = 15L * 60L * 1000L

    /**
     * How many foreground deaths in a row are tolerated before glass is blamed for them.
     *
     * One is not evidence. The common case is a user swiping the app out of recents: the
     * process goes away before the background write that disarms the marker lands, and the
     * next launch then looked like "glass killed the process". Two in a row - the material
     * came back and the process died under it again - is worth backing off from.
     */
    private const val MAX_FOREGROUND_DEATHS = 2

    @Volatile
    private var failure: Throwable? = null

    /** Set once in this process a backdrop has actually been recorded and drawn. */
    @Volatile
    private var drewThisSession = false

    @Volatile
    private var appContext: Context? = null

    /** True once the GPU glass path has failed; every surface degrades from then on. */
    val disabled: Boolean
        get() = failure != null

    /** The failure that switched the material off, for diagnostics. */
    val reason: Throwable?
        get() = failure

    /** Records the first failure only: the first cause is the useful one. */
    fun report(error: Throwable) {
        if (failure != null) return
        failure = error
        runCatching { Timber.e(error, "Liquid Glass disabled for this session") }
    }

    /** Called by the glass layer on its first successful draw. */
    fun noteDrawn() {
        drewThisSession = true
    }

    /** Called from Application.onCreate: restores the guard and installs log capture. */
    fun install(context: Context) {
        val app = context.applicationContext
        appContext = app
        val store = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // A new build is a fresh attempt: the guard from the previous version is dropped
        // so a fix can actually be tried.
        if (store.getInt(KEY_VERSION, Int.MIN_VALUE) != BuildConfig.VERSION_CODE) {
            store.edit()
                .putInt(KEY_VERSION, BuildConfig.VERSION_CODE)
                .putLong(KEY_ARMED, 0L)
                .putLong(KEY_LAST_BREAK, 0L)
                .putInt(KEY_BREAK_VERSION, 0)
                .putInt(KEY_DEATHS, 0)
                .apply()
        }
        val armedAt = store.getLong(KEY_ARMED, 0L)
        val brokenAt = store.getLong(KEY_LAST_BREAK, 0L)
        // A break recorded by a different build says nothing about this one: the whole point
        // of shipping a fix is that the next install gets to try again immediately.
        val brokenBuild = store.getInt(KEY_BREAK_VERSION, Int.MIN_VALUE)
        val now = System.currentTimeMillis()
        if (armedAt > 0L) {
            // Previous process died in the foreground with glass on screen: a driver fault,
            // an OOM kill, a Java crash that beat the handler - or, far more often, the user
            // swiping the app out of recents before the disarm write landed. Count it, and
            // only stop when the same thing happens twice in a row.
            val deaths = store.getInt(KEY_DEATHS, 0) + 1
            if (deaths >= MAX_FOREGROUND_DEATHS) {
                store.edit()
                    .putLong(KEY_ARMED, 0L)
                    .putInt(KEY_DEATHS, 0)
                    .putLong(KEY_LAST_BREAK, now)
                    .putInt(KEY_BREAK_VERSION, BuildConfig.VERSION_CODE)
                    .commit()
                if (failure == null) {
                    failure = IllegalStateException(
                        "$deaths foreground sessions in a row ended while Liquid Glass was " +
                            "on screen"
                    )
                }
            } else {
                // Disarm durably before the next launch: commit() writes synchronously, and
                // apply() is exactly what is likely to be lost when a process is killed.
                store.edit().putLong(KEY_ARMED, 0L).putInt(KEY_DEATHS, deaths).commit()
            }
        } else if (
            brokenAt > 0L &&
            brokenBuild == BuildConfig.VERSION_CODE &&
            now - brokenAt < RETRY_WINDOW_MS
        ) {
            if (failure == null) {
                failure = IllegalStateException(
                    "Liquid Glass paused until ${(brokenAt + RETRY_WINDOW_MS) / 1000L}s " +
                        "(unix) after a previous break"
                )
            }
        }
        installCrashCapture(app)
    }

    /** Foreground: if glass is on screen, mark the session as "under glass". */
    fun onForeground() {
        if (disabled || !drewThisSession) return
        val store = prefs() ?: return
        store.edit().putLong(KEY_ARMED, System.currentTimeMillis()).apply()
    }

    /**
     * Background: a process killed from here was killed by the system, not by glass.
     *
     * This is also where the guard heals itself. Reaching a clean background with glass
     * drawn and no failure means the GPU path worked for a whole session in the foreground,
     * which is the best evidence available that the build is fine — so the break marker is
     * dropped and the next launch starts with the real material again.
     */
    fun onBackground() {
        val store = prefs() ?: return
        val healed = failure == null && drewThisSession &&
            store.getLong(KEY_LAST_BREAK, 0L) != 0L
        if (healed) {
            store.edit()
                .putLong(KEY_LAST_BREAK, 0L)
                .putInt(KEY_BREAK_VERSION, 0)
                .commit()
        }
        if (store.getLong(KEY_ARMED, 0L) == 0L && store.getInt(KEY_DEATHS, 0) == 0) return
        store.edit().putLong(KEY_ARMED, 0L).putInt(KEY_DEATHS, 0).commit()
    }

    /** Human-readable state for Settings → Share diagnostics. */
    fun describe(): String = buildString {
        appendLine("Liquid Glass: " + if (disabled) "off" else "on")
        reason?.let { error ->
            appendLine("reason: ${error.javaClass.simpleName}: ${error.message}")
            appendLine(stackTraceOf(error).lineSequence().take(24).joinToString("\n"))
        }
        appendLine("runtime shader (AGSL): ${isRuntimeShaderSupported()}")
        appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else Build.HARDWARE
        appendLine("hardware: ${Build.HARDWARE} / soc $soc")
        appContext?.let { app ->
            val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            appendLine("GLES: ${activityManager?.deviceConfigurationInfo?.glEsVersion}")
        }
        val crash = readCrashLog()
        if (crash.isNotBlank()) {
            appendLine("--- last Liquid Glass crash ---")
            appendLine(crash)
        }
    }

    /** Test hook. Not used by the app. */
    internal fun reset() {
        failure = null
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun crashFile(context: Context) = File(context.filesDir, CRASH_FILE)

    private fun readCrashLog(): String = runCatching {
        val context = appContext ?: return ""
        crashFile(context).takeIf { it.isFile }?.readText().orEmpty().take(8_000)
    }.getOrDefault("")

    private fun installCrashCapture(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is GlassCrashHandler) return
        Thread.setDefaultUncaughtExceptionHandler(GlassCrashHandler(context, previous))
    }

    private fun stackTraceOf(error: Throwable): String {
        val writer = StringWriter()
        error.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private class GlassCrashHandler(
        private val context: Context,
        private val previous: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, error: Throwable) {
            runCatching {
                // Only record crashes that happened while glass was actually on screen;
                // an unrelated crash elsewhere in the app says nothing about the GPU.
                if (!drewThisSession) return@runCatching
                val text = buildString {
                    appendLine("thread: ${thread.name}")
                    appendLine("glass: " + if (disabled) "already off" else "on")
                    reason?.let { appendLine("earlier glass failure: $it") }
                    appendLine()
                    appendLine(stackTraceOf(error))
                }
                crashFile(context).writeText(text.take(64_000))
                // Glass was on screen when this thread died: keep it off next launch.
                // commit(), not apply(): this runs on the way into process death, which is
                // the one moment an asynchronous write is most likely to be dropped.
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_ARMED, 0L)
                    .putInt(KEY_DEATHS, 0)
                    .putLong(KEY_LAST_BREAK, System.currentTimeMillis())
                    .putInt(KEY_BREAK_VERSION, BuildConfig.VERSION_CODE)
                    .commit()
            }
            previous?.uncaughtException(thread, error)
        }
    }
}
