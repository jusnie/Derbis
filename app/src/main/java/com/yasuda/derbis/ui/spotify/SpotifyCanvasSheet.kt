package com.yasuda.derbis.ui.spotify

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.yasuda.derbis.ui.derbisSafeTopPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yasuda.derbis.data.spotify.DerbisSpotifySession
import com.yasuda.derbis.data.spotify.SpotifyCookie
import com.yasuda.derbis.feature.motion.MotionArtworkRuntime
import com.yasuda.derbis.feature.motion.SpotifyOfficialCanvas
import com.yasuda.derbis.feature.motion.SpotifyTokenProvider
import com.yasuda.derbis.ui.DerbisIsLight
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisGlassBorder
import com.yasuda.derbis.ui.theme.DerbisInk
import com.yasuda.derbis.ui.theme.DerbisIsPixel
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisPanel
import com.yasuda.derbis.ui.theme.DerbisText
import com.yasuda.derbis.ui.theme.PixelDesign
import com.yasuda.derbis.ui.theme.PixelPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val SPOTIFY_ORIGIN = "https://open.spotify.com"
private const val SPOTIFY_WWW_ORIGIN = "https://www.spotify.com"
private const val SPOTIFY_ACCOUNTS_ORIGIN = "https://accounts.spotify.com"
private const val SPOTIFY_STATUS_URL = "https://accounts.spotify.com/en/status"

/**
 * Canonical first-party Spotify web login endpoint (`continue=https://open.spotify.com/`).
 * Using `open.spotify.com` as the registered `continue` target satisfies Spotify's server-side
 * login redirect validation, while [createSpotifyWebView] intercepts the post-login handoff before
 * the DRM web player loads and redirects to [SPOTIFY_STATUS_URL] if `sp_dc` is still flushing.
 */
internal const val SPOTIFY_LOGIN_URL =
    "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F"

private const val DEFAULT_CHROMIUM_MAJOR = "131"
private val CHROMIUM_MAJOR_PATTERN = Regex("""Chrome/(\d{2,3})\.""")

/**
 * Builds a genuine Chrome-on-Android Reduced User-Agent matching the device WebView's real
 * Chromium major version and form factor while stripping `; wv`, `Version/4.0`, and `Build/...`.
 *
 * Spoofing a desktop Windows UA inside an Android WebView causes Google reCAPTCHA Enterprise on
 * `accounts.spotify.com` to reject email sign-in ("Oops! Something went wrong") because the HTTP
 * `Sec-CH-UA*` client hints (`Android`, `mobile=?1`) and `navigator.platform` (`Linux aarch64`)
 * contradict a Windows desktop `User-Agent`.
 */
internal fun buildSpotifyLoginUserAgent(rawWebViewUserAgent: String?): String {
    val raw = rawWebViewUserAgent?.trim().orEmpty()
    val major = CHROMIUM_MAJOR_PATTERN.find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { version -> version.toIntOrNull()?.let { it in 90..999 } == true }
        ?: DEFAULT_CHROMIUM_MAJOR
    val isAndroidTablet = raw.contains("Android", ignoreCase = true) &&
        !raw.contains("Mobile", ignoreCase = true)
    val mobileToken = if (isAndroidTablet) "" else "Mobile "
    return "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/$major.0.0.0 ${mobileToken}Safari/537.36"
}

/** True for the web player, the one Spotify surface a plain WebView cannot draw. */
internal fun isSpotifyWebPlayerHost(host: String?): Boolean =
    host?.equals("open.spotify.com", ignoreCase = true) == true ||
        host?.equals("play.spotify.com", ignoreCase = true) == true

private fun isSpotifyWebPlayer(url: Uri): Boolean = isSpotifyWebPlayerHost(url.host)

internal fun isGoogleAuthHost(host: String?): Boolean {
    val lowered = host?.lowercase().orEmpty()
    return lowered == "accounts.google.com" || lowered.endsWith(".accounts.google.com")
}

internal fun isSpotifyPostLoginStatusUrl(url: String?): Boolean {
    val raw = url?.trim().orEmpty()
    if (raw.isBlank()) return false
    val normalized = raw.lowercase()
    return normalized.startsWith("https://accounts.spotify.com/") &&
        (normalized.contains("/status") || normalized.contains("/account/overview"))
}

internal fun shouldTreatWebViewErrorAsFatal(
    isForMainFrame: Boolean,
    errorCode: Int,
    description: String?,
    suppressedAbort: Boolean
): Boolean {
    if (!isForMainFrame) return false
    val desc = description?.lowercase().orEmpty()
    if (suppressedAbort || desc.contains("err_aborted") || desc.contains("err_blocked_by_orb")) {
        return false
    }
    // WebViewClient.ERROR_UNKNOWN (-1) is commonly emitted when stopLoading() or a custom-scheme
    // handoff cancels an in-flight navigation after login has already succeeded.
    if (errorCode == WebViewClient.ERROR_UNKNOWN && desc.isBlank()) return false
    return true
}

/**
 * Clears transient third-party OAuth / CSRF flow cookies left behind if a user tapped
 * "Continue with Google" before switching to email sign-in, while preserving `sp_t` and
 * `__Host-device_id` device trust cookies.
 */
private fun clearSpotifyLoginFlowCookies() {
    runCatching {
        val manager = CookieManager.getInstance()
        val transientCookies = listOf(
            "__Secure-TPASESSION",
            "sp_sso_csrf_token",
            "sp_tr",
            "__Host-sp_csrf_sid"
        )
        val origins = listOf(
            SPOTIFY_ACCOUNTS_ORIGIN,
            SPOTIFY_ORIGIN,
            "https://spotify.com"
        )
        for (origin in origins) {
            for (name in transientCookies) {
                manager.setCookie(origin, "$name=; Path=/; Max-Age=0; Secure")
                if (!name.startsWith("__Host-")) {
                    manager.setCookie(origin, "$name=; Domain=.spotify.com; Path=/; Max-Age=0; Secure")
                    manager.setCookie(origin, "$name=; Domain=accounts.spotify.com; Path=/; Max-Age=0; Secure")
                }
            }
        }
    }
}

/**
 * Expires Spotify session cookies in the shared WebView jar without touching other services (such
 * as YouTube Music) that share Android's [CookieManager].
 */
internal fun clearSpotifyWebCookies() {
    runCatching {
        val manager = CookieManager.getInstance()
        val expiredCookies = listOf(
            "sp_dc",
            "sp_key",
            "sp_t",
            "sp_landing",
            "sp_tr",
            "sp_m",
            "__Secure-TPASESSION",
            "sp_sso_csrf_token",
            "__Host-sp_csrf_sid"
        )
        val origins = listOf(
            SPOTIFY_ORIGIN,
            SPOTIFY_WWW_ORIGIN,
            SPOTIFY_ACCOUNTS_ORIGIN,
            "https://spotify.com"
        )
        for (origin in origins) {
            for (name in expiredCookies) {
                manager.setCookie(origin, "$name=; Path=/; Max-Age=0; Secure")
                if (!name.startsWith("__Host-")) {
                    manager.setCookie(origin, "$name=; Domain=.spotify.com; Path=/; Max-Age=0; Secure")
                    manager.setCookie(origin, "$name=; Domain=accounts.spotify.com; Path=/; Max-Age=0; Secure")
                }
            }
        }
        manager.flush()
    }
}

/**
 * The sheet's session test uses a well-known track. It does not matter whether this one still has
 * a Canvas: a miss is reported as "the session works, this track has none", which is exactly the
 * difference the test exists to show.
 */
private val CANVAS_TEST_TRACK_IDS = listOf(
    "0VjIjW4GlUZAMYd2vXMi3b", // The Weeknd — Blinding Lights
    "2plbrEY59IikOBgBGLjaoe", // Lady Gaga & Bruno Mars — Die With A Smile
    "6dOtVTDdiauQNBQEDOtlAB"  // Billie Eilish — BIRDS OF A FEATHER
)

private val SpotifyGreen = Color(0xFF1ED760)

/**
 * Spotify Canvas setup.
 *
 * Two ways in, on purpose. The plain one is the browser: sign in on Spotify's own page, and the
 * cookie the web player leaves behind is read out of this WebView's jar and saved — no typing, no
 * dev tools. The other one is the Developer panel in the top right, which shows the `sp_dc` that
 * was captured, lets it be copied, and accepts a value pasted in from a desktop browser instead.
 *
 * Spotify has no public Canvas API; the session cookie is the only way in, which is why it is the
 * one thing this screen is about.
 */
@Composable
internal fun SpotifyCanvasSheet(
    onDismiss: () -> Unit,
    onSessionSaved: () -> Unit = {},
    startInBrowser: Boolean = false
) {
    val strings = LocalDerbisStrings.current
    val context = LocalContext.current
    val session by DerbisSpotifySession.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Connect opens the browser itself: the sheet is the sign-in, not a menu about it.
    var browsing by remember { mutableStateOf(startInBrowser) }
    var developerOpen by remember { mutableStateOf(false) }
    var detected by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(context) {
        DerbisSpotifySession.initialize(context)
        SpotifyTokenProvider.init(context)
    }

    val stored = remember(session) { DerbisSpotifySession.cookie() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = DerbisInk, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .derbisSafeTopPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.close, tint = DerbisText)
                    }
                    Text(
                        text = strings.spotifyCanvasTitle,
                        color = DerbisText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    // The developer door, top right. It brings the sp_dc field up from the bottom
                    // of the screen — the same end the keyboard opens on, so nothing jumps.
                    IconButton(onClick = { developerOpen = !developerOpen }) {
                        Icon(
                            Icons.Rounded.Code,
                            contentDescription = strings.spotifyCanvasDeveloper,
                            tint = if (developerOpen) SpotifyGreen else DerbisMuted
                        )
                    }
                }

                if (browsing) {
                    SpotifyBrowserPane(
                        modifier = Modifier.weight(1f),
                        // The developer dock replaces the browser's own bar while it is up:
                        // two rows of buttons under a keyboard is one row too many.
                        footer = !developerOpen,
                        hasStoredSession = stored.isNotEmpty(),
                        onDetected = { detected = it },
                        onCancel = { browsing = false },
                        onCopy = { cookie ->
                            copyCookie(context, cookie)
                            status = strings.spotifyCanvasCopied
                        },
                        onUse = { cookie ->
                            val (accepted, message) = applySession(context, cookie, strings, onSessionSaved)
                            status = message
                            // Sign-in finished: hand the listener back to the settings card,
                            // which is where the unlocked state is shown.
                            if (accepted) {
                                runCatching {
                                    Toast.makeText(context, strings.spotifyCanvasUnlocked, Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = strings.spotifyCanvasSubtitle,
                            color = DerbisMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )

                        SessionCard(
                            stored = stored,
                            busy = busy,
                            status = status,
                            onSignIn = {
                                detected = null
                                browsing = true
                            },
                            onTest = {
                                scope.launch {
                                    busy = true
                                    status = ""
                                    var finalMessage = strings.spotifyCanvasTestNoCanvas
                                    for (testId in CANVAS_TEST_TRACK_IDS) {
                                        when (val result = SpotifyOfficialCanvas.probe(testId)) {
                                            is SpotifyOfficialCanvas.Result.Found -> {
                                                finalMessage = strings.spotifyCanvasTestOk
                                                break
                                            }
                                            is SpotifyOfficialCanvas.Result.Failed -> {
                                                finalMessage = result.reason
                                                break
                                            }
                                            is SpotifyOfficialCanvas.Result.NoCanvas -> continue
                                        }
                                    }
                                    status = finalMessage
                                    busy = false
                                }
                            }
                        )

                        Text(
                            text = strings.spotifyCanvasDeveloperHint,
                            color = DerbisMuted.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Bottom dock: the sp_dc field, pasted or typed, with the captured value above it.
                if (developerOpen) {
                    DeveloperDock(
                        stored = stored,
                        status = status,
                        revealed = revealed,
                        draft = draft,
                        onDraft = { draft = it },
                        onToggleReveal = { revealed = !revealed },
                        onCopy = {
                            copyCookie(context, stored)
                            status = strings.spotifyCanvasCopied
                        },
                        onPaste = {
                            pasteCookie(context)?.let { draft = it }
                        },
                        onSave = {
                            val (accepted, message) = applySession(context, draft, strings, onSessionSaved)
                            status = message
                            if (accepted) draft = ""
                        },
                        onRemove = {
                            DerbisSpotifySession.clear(context)
                            clearSpotifyWebCookies()
                            SpotifyOfficialCanvas.invalidate()
                            SpotifyTokenProvider.invalidate()
                            SpotifyTokenProvider.rewindWarmUpBudget()
                            MotionArtworkRuntime.bumpSessionGeneration()
                            detected = null
                            draft = ""
                            status = strings.spotifyCanvasRemoved
                        },
                        onUseDetected = detected?.let { cookie ->
                            {
                                val (_, message) = applySession(context, cookie, strings, onSessionSaved)
                                status = message
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Copies the cookie as a *sensitive* clip. A live sp_dc is a password: Android 13+ hides clips
 * marked this way from the system preview and from clipboard-history UIs.
 */
private fun copyCookie(context: Context, value: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        ?: return
    val clip = android.content.ClipData.newPlainText("sp_dc", value)
    clip.description.extras = android.os.PersistableBundle().apply {
        // ClipDescription.EXTRA_IS_SENSITIVE, spelled out so it compiles below API 33 too.
        putBoolean("android.content.extra.IS_SENSITIVE", true)
    }
    manager.setPrimaryClip(clip)
}

/** The clipboard's first item, if it has anything usable — used by the Developer panel's paste. */
private fun pasteCookie(context: Context): String? {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        ?: return null
    val text = manager.primaryClip?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
        ?.trim()
    return text?.takeIf { it.isNotEmpty() }
}

/**
 * Stores a cookie and drops every cache that was built from the previous one, so a new account is
 * not served the old account's Canvas. Returns the line to show under the card.
 */
private fun applySession(
    context: Context,
    raw: String,
    strings: DerbisStrings,
    onSaved: () -> Unit
): Pair<Boolean, String> {
    val saved = DerbisSpotifySession.save(context, raw)
    if (!saved.hasCookie) return false to saved.lastError
    runCatching { CookieManager.getInstance().flush() }
    SpotifyTokenProvider.invalidate()
    SpotifyOfficialCanvas.invalidate()
    MotionArtworkRuntime.bumpSessionGeneration()
    SpotifyTokenProvider.rewindWarmUpBudget()
    SpotifyTokenProvider.warmUp()
    // Whatever was playing a second ago was resolved without this session; let it try again.
    onSaved()
    return true to saved.lastError.ifBlank { strings.spotifyCanvasSaved }
}

@Composable
private fun SessionCard(
    stored: String,
    busy: Boolean,
    status: String,
    onSignIn: () -> Unit,
    onTest: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val unlocked = stored.isNotEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (unlocked) SpotifyGreen.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                RoundedCornerShape(18.dp)
            )
            .border(
                1.dp,
                if (unlocked) SpotifyGreen.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (unlocked) SpotifyGreen.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    tint = if (unlocked) SpotifyGreen else DerbisMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (unlocked) strings.spotifyCanvasUnlocked else strings.spotifyCanvasLocked,
                    color = if (unlocked) DerbisText else DerbisMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (unlocked) SpotifyCookie.masked(stored) else strings.spotifyCanvasNoSession,
                    color = DerbisMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = SpotifyGreen
                )
            } else if (unlocked) {
                TextButton(onClick = onTest) { Text(strings.spotifyCanvasTest, color = SpotifyGreen, fontSize = 12.sp) }
            }
        }
        // The sign-in action belongs to this card: it is what turns "No session stored" into a
        // session, and a second button above the card said the same thing again.
        if (!unlocked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SpotifyGreen.copy(alpha = 0.14f))
                    .border(1.dp, SpotifyGreen.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onSignIn)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInBrowser,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = strings.spotifyCanvasOpenBrowser,
                    color = DerbisText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (status.isNotBlank()) {
            Text(status, color = DerbisMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun DeveloperDock(
    stored: String,
    status: String,
    revealed: Boolean,
    draft: String,
    onDraft: (String) -> Unit,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onUseDetected: (() -> Unit)?
) {
    val strings = LocalDerbisStrings.current
    val pixel = DerbisIsPixel
    val drawerShape = if (pixel) RectangleShape else RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    val drawerFill = when {
        pixel -> PixelPalette.ink
        DerbisIsLight -> DerbisPanel
        else -> Color(0xFF141418)
    }
    val drawerBorder = when {
        pixel -> PixelPalette.outline
        DerbisIsLight -> DerbisGlassBorder
        else -> Color.White.copy(alpha = 0.10f)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(drawerFill, drawerShape)
            .border(
                if (pixel) PixelDesign.border else 1.dp,
                drawerBorder,
                drawerShape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // What is stored right now, with the two things anyone wants from it: look, copy.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(strings.spotifyCanvasCookieLabel, color = DerbisMuted, fontSize = 10.sp, letterSpacing = 0.6.sp)
                Text(
                    text = when {
                        stored.isEmpty() -> strings.spotifyCanvasNoSession
                        revealed -> stored
                        else -> SpotifyCookie.masked(stored)
                    },
                    color = if (stored.isEmpty()) DerbisMuted else DerbisText,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    maxLines = if (revealed) 3 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleReveal, enabled = stored.isNotEmpty()) {
                Icon(
                    imageVector = if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = strings.spotifyCanvasReveal,
                    tint = DerbisMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onCopy, enabled = stored.isNotEmpty()) {
                Icon(
                    Icons.Rounded.ContentCopy,
                    contentDescription = strings.spotifyCanvasCopy,
                    tint = DerbisMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        OutlinedTextField(
            value = draft,
            onValueChange = onDraft,
            label = { Text(strings.spotifyCanvasPasteLabel) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onPaste) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = strings.spotifyCanvasPaste, tint = DerbisMuted)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DerbisText,
                unfocusedTextColor = DerbisText,
                focusedBorderColor = DerbisCyan,
                unfocusedBorderColor = DerbisMuted.copy(alpha = 0.4f),
                cursorColor = DerbisCyan
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSave, enabled = draft.isNotBlank()) {
                Text(strings.save, color = if (draft.isNotBlank()) SpotifyGreen else DerbisMuted)
            }
            onUseDetected?.let { use ->
                TextButton(onClick = use) { Text(strings.spotifyCanvasUseDetected, color = SpotifyGreen) }
            }
            Spacer(Modifier.weight(1f))
            if (stored.isNotEmpty()) {
                TextButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = DerbisMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(strings.spotifyCanvasRemove, color = DerbisMuted, fontSize = 12.sp)
                }
            }
        }

        if (status.isNotBlank()) {
            Text(status, color = DerbisMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun SpotifyBrowserPane(
    modifier: Modifier = Modifier,
    footer: Boolean = true,
    hasStoredSession: Boolean = false,
    onDetected: (String?) -> Unit,
    onCancel: () -> Unit,
    onCopy: (String) -> Unit,
    onUse: (String) -> Unit
) {
    val strings = LocalDerbisStrings.current
    val currentHasStoredSession by rememberUpdatedState(hasStoredSession)
    val currentOnDetected by rememberUpdatedState(onDetected)
    val currentOnUse by rememberUpdatedState(onUse)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var onGoogleAuth by remember { mutableStateOf(false) }
    var postLoginReached by remember { mutableStateOf(false) }
    var initialCookie by remember { mutableStateOf(captureSession()) }
    var detected by remember { mutableStateOf(initialCookie) }
    var autoApplied by remember { mutableStateOf(false) }

    fun handleCookieFound(cookie: String) {
        detected = cookie
        currentOnDetected(cookie)
        val isFreshSignIn = cookie != initialCookie
        val shouldAutoComplete = !autoApplied && (isFreshSignIn || (!currentHasStoredSession && postLoginReached))
        if (shouldAutoComplete) {
            autoApplied = true
            currentOnUse(cookie)
        }
    }

    // Spotify's React login page submits credentials via fetch() and navigates with pushState,
    // which does not always fire WebViewClient.onPageFinished; poll the jar while open so sp_dc
    // is picked up immediately after email/password or 6-digit code sign-in.
    LaunchedEffect(Unit) {
        initialCookie?.let(onDetected)
        while (isActive && !autoApplied) {
            delay(600L)
            captureSession()?.let(::handleCookieFound)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    BackHandler {
        if (webView?.canGoBack() == true) webView?.goBack() else onCancel()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = SpotifyGreen)
        if (onGoogleAuth) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            DerbisIsPixel -> PixelPalette.panel
                            DerbisIsLight -> DerbisPanel
                            else -> Color(0xFF1F2430)
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Google blocks embedded browser sign-in. Enter your Google email in Spotify's Email field below to sign in with a 6-digit email code.",
                    color = DerbisText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        onGoogleAuth = false
                        failed = false
                        val view = webView
                        if (view != null && view.url?.startsWith(SPOTIFY_ACCOUNTS_ORIGIN, ignoreCase = true) != true) {
                            clearSpotifyLoginFlowCookies()
                            view.loadUrl(SPOTIFY_LOGIN_URL)
                        }
                    }
                ) {
                    Text("Got it", color = SpotifyGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (failed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.spotifyCanvasWebError,
                    color = DerbisMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        failed = false
                        loading = true
                        clearSpotifyLoginFlowCookies()
                        webView?.loadUrl(SPOTIFY_LOGIN_URL)
                    }
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Reload", color = SpotifyGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { context ->
                if (!currentHasStoredSession) {
                    clearSpotifyLoginFlowCookies()
                }
                createSpotifyWebView(
                    context = context,
                    onLoading = { isLoad ->
                        loading = isLoad
                        if (isLoad) failed = false
                    },
                    onClearError = { failed = false },
                    onGoogleAuthBlocked = { onGoogleAuth = true },
                    onPostLoginReached = { postLoginReached = true },
                    onError = {
                        loading = false
                        if (captureSession() == null) {
                            failed = true
                        }
                    },
                    onDetected = ::handleCookieFound
                ).also { webView = it }
            }
        )
        if (footer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DerbisInk)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bottom left is the cookie: what this browser is holding, tap to copy it.
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable(enabled = detected != null) { detected?.let(onCopy) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = if (detected != null) SpotifyGreen else DerbisMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = when {
                            detected == null -> strings.spotifyCanvasSigningIn
                            else -> "sp_dc " + SpotifyCookie.masked(detected.orEmpty())
                        },
                        color = if (detected != null) SpotifyGreen else DerbisMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.weight(1f))
                if (detected != null) {
                    TextButton(
                        onClick = {
                            clearSpotifyWebCookies()
                            initialCookie = null
                            detected = null
                            postLoginReached = false
                            autoApplied = false
                            failed = false
                            onDetected(null)
                            webView?.loadUrl(SPOTIFY_LOGIN_URL)
                        }
                    ) {
                        Text("Switch", color = DerbisMuted, fontSize = 12.sp)
                    }
                } else {
                    TextButton(onClick = onCancel) { Text(strings.cancel, color = DerbisMuted) }
                }
                TextButton(onClick = { detected?.let(onUse) }, enabled = detected != null) {
                    Text(
                        strings.spotifyCanvasUseAccount,
                        color = if (detected != null) SpotifyGreen else DerbisMuted,
                        fontWeight = if (detected != null) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun createSpotifyWebView(
    context: Context,
    onLoading: (Boolean) -> Unit,
    onClearError: () -> Unit,
    onGoogleAuthBlocked: () -> Unit,
    onPostLoginReached: () -> Unit,
    onError: () -> Unit,
    onDetected: (String) -> Unit
): WebView = WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    isFocusable = true
    isFocusableInTouchMode = true
    setOnTouchListener { view, event ->
        if ((event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) && !view.hasFocus()) {
            view.requestFocus()
        }
        false
    }
    val defaultUa = runCatching { WebSettings.getDefaultUserAgent(context) }.getOrNull()
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.javaScriptCanOpenWindowsAutomatically = false
    settings.setSupportMultipleWindows(false)
    settings.mediaPlaybackRequiresUserGesture = false
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    settings.userAgentString = buildSpotifyLoginUserAgent(defaultUa)
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    webChromeClient = WebChromeClient()

    var suppressNextAbortError = false

    fun pollCookieWithRetries(view: WebView) {
        captureSession()?.let {
            onDetected(it)
            return
        }
        for (delayMs in listOf(250L, 650L, 1_300L, 2_200L)) {
            view.postDelayed({
                captureSession()?.let(onDetected)
            }, delayMs)
        }
    }

    fun handleWebPlayerHandoff(view: WebView) {
        onClearError()
        onLoading(false)
        onPostLoginReached()
        val immediate = captureSession()
        if (immediate != null) {
            onDetected(immediate)
        } else {
            view.post { view.loadUrl(SPOTIFY_STATUS_URL) }
            pollCookieWithRetries(view)
        }
    }

    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val scheme = request.url.scheme?.lowercase().orEmpty()
            if (request.isForMainFrame && scheme != "https" && scheme != "http") {
                // Custom-scheme links (spotify://, intent://, market://) are native-app handoff
                // probes emitted right after sign-in; stay in the sheet and harvest sp_dc instead
                // of treating the handoff as a load failure.
                onPostLoginReached()
                pollCookieWithRetries(view)
                return true
            }
            if (request.isForMainFrame && isGoogleAuthHost(request.url.host)) {
                // Block navigation to accounts.google.com before Google's embedded-WebView check
                // strands the user or leaves half-baked OAuth flow cookies on accounts.spotify.com.
                suppressNextAbortError = true
                clearSpotifyLoginFlowCookies()
                onGoogleAuthBlocked()
                view.post { view.loadUrl(SPOTIFY_LOGIN_URL) }
                return true
            }
            // The session is already on the device once the accounts page hands off to the web
            // player, and the player itself is a DRM shell. Stay out of open.spotify.com and read
            // sp_dc (or bounce to /en/status so Chromium flushes the cookie jar).
            if (request.isForMainFrame && isSpotifyWebPlayer(request.url)) {
                suppressNextAbortError = true
                handleWebPlayerHandoff(view)
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            val target = url?.let(Uri::parse)
            if (target != null && isGoogleAuthHost(target.host)) {
                suppressNextAbortError = true
                view.stopLoading()
                clearSpotifyLoginFlowCookies()
                onGoogleAuthBlocked()
                view.post { view.loadUrl(SPOTIFY_LOGIN_URL) }
                return
            }
            if (isSpotifyPostLoginStatusUrl(url)) {
                onPostLoginReached()
                pollCookieWithRetries(view)
            }
            if (target != null && isSpotifyWebPlayer(target)) {
                suppressNextAbortError = true
                view.stopLoading()
                handleWebPlayerHandoff(view)
                return
            }
            onLoading(true)
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            if (isSpotifyPostLoginStatusUrl(url)) {
                onPostLoginReached()
                pollCookieWithRetries(view)
            } else {
                captureSession()?.let(onDetected)
            }
        }

        override fun onPageCommitVisible(view: WebView, url: String?) {
            super.onPageCommitVisible(view, url)
            captureSession()?.let(onDetected)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            onLoading(false)
            if (isSpotifyPostLoginStatusUrl(url)) {
                onPostLoginReached()
            }
            pollCookieWithRetries(view)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            val wasSuppressed = suppressNextAbortError
            if (request.isForMainFrame) {
                suppressNextAbortError = false
            }
            if (
                shouldTreatWebViewErrorAsFatal(
                    isForMainFrame = request.isForMainFrame,
                    errorCode = error.errorCode,
                    description = error.description?.toString(),
                    suppressedAbort = wasSuppressed
                )
            ) {
                onError()
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            // 4xx responses on accounts.spotify.com carry interactive HTML (invalid credentials,
            // verification prompts, CAPTCHA); only 5xx server outages break the sign-in page.
            if (request.isForMainFrame && errorResponse.statusCode >= 500) {
                onError()
            }
        }
    }
    loadUrl(SPOTIFY_LOGIN_URL)
}

/**
 * Reads `sp_dc` out of the WebView's cookie jar. The cookie is httpOnly, so JavaScript on the page
 * cannot see it — but the jar this app owns can, which is the whole point of signing in here.
 */
private fun captureSession(): String? {
    val manager = runCatching { CookieManager.getInstance() }.getOrNull() ?: return null
    // `sp_dc` is written on `.spotify.com`, so any Spotify origin jar can hold it.
    for (origin in listOf(SPOTIFY_ORIGIN, SPOTIFY_ACCOUNTS_ORIGIN, SPOTIFY_WWW_ORIGIN, SPOTIFY_STATUS_URL)) {
        val header = runCatching { manager.getCookie(origin) }.getOrNull().orEmpty()
        if (header.isBlank()) continue
        SpotifyCookie.parse(header)?.let { return it }
    }
    return null
}

