package com.luc4n3x.derbis.feature.discord

import android.app.Activity
import android.content.Context
import com.luc4n3x.derbis.BuildConfig
import timber.log.Timber

/**
 * Optional Android Discord Social SDK Rich Presence bridge. It only publishes the currently
 * playing Derbis track when explicitly enabled by the user and a signed-in Discord app is present.
 * The proprietary SDK is fetched only for upstream release builds and is never committed here.
 */
object DerbisDiscordRichPresence {
    private const val PREFERENCES = "derbis_discord_presence"
    private const val ENABLED_KEY = "enabled"
    private val applicationId: Long = BuildConfig.DISCORD_APPLICATION_ID.toLongOrNull() ?: 0L

    @Volatile private var libraryLoaded = false
    @Volatile private var loadAttempted = false
    @Volatile private var lastTitle: String = ""
    @Volatile private var lastArtist: String = ""
    @Volatile private var lastPlaying: Boolean = false

    fun isAvailable(): Boolean = BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE && applicationId > 0L && loadBridge()

    fun isEnabled(context: Context): Boolean =
        isAvailable() && context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(ENABLED_KEY, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ENABLED_KEY, enabled && isAvailable())
            .apply()
        if (enabled) publishStored(context) else clear()
    }

    fun attach(activity: Activity) {
        if (!isAvailable()) return
        // The SDK uses this Activity to reach the installed Discord Android application. Reflection
        // keeps F-Droid and source-only builds free of the proprietary Discord Java dependency.
        runCatching {
            Class.forName("com.discord.socialsdk.DiscordSocialSdkInit")
                .getMethod("setEngineActivity", Activity::class.java)
                .invoke(null, activity)
        }.onFailure { Timber.w(it, "Discord Social SDK activity attachment failed") }
    }

    fun publish(context: Context, title: String?, artist: String?, playing: Boolean) {
        lastTitle = title.orEmpty().trim()
        lastArtist = artist.orEmpty().trim()
        lastPlaying = playing
        if (!isEnabled(context)) {
            clear()
            return
        }
        if (!loadBridge() || !nativeStart(applicationId)) return
        nativeUpdate(lastTitle, lastArtist, lastPlaying)
    }

    fun clear() {
        if (loadBridge()) nativeClear()
    }

    fun release() {
        if (loadBridge()) nativeStop()
    }

    private fun publishStored(context: Context) {
        if (!isEnabled(context) || !loadBridge() || !nativeStart(applicationId)) return
        nativeUpdate(lastTitle, lastArtist, lastPlaying)
    }

    private fun loadBridge(): Boolean {
        if (loadAttempted) return libraryLoaded
        synchronized(this) {
            if (loadAttempted) return libraryLoaded
            loadAttempted = true
            libraryLoaded = runCatching {
                System.loadLibrary("derbis_discord_bridge")
                true
            }.onFailure { Timber.w(it, "Discord Rich Presence bridge unavailable") }.getOrDefault(false)
            return libraryLoaded
        }
    }

    private external fun nativeStart(applicationId: Long): Boolean
    private external fun nativeUpdate(title: String, artist: String, playing: Boolean)
    private external fun nativeClear()
    private external fun nativeStop()
}
