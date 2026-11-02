package com.yasuda.derbis.ui.spotify

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.yasuda.derbis.feature.motion.SpotifyOfficialCanvas
import com.yasuda.derbis.feature.motion.SpotifyTokenProvider
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisInk
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisText
import kotlinx.coroutines.launch

private const val SPOTIFY_ORIGIN = "https://open.spotify.com"
private const val SPOTIFY_ACCOUNTS_ORIGIN = "https://accounts.spotify.com"

/**
 * Sign-in stays on `accounts.spotify.com`. The old URL sent the browser through
 * `continue=open.spotify.com`, and that web player is a DRM app: in a plain WebView it paints its
 * dark shell and then nothing, which is the black page this sheet used to show. The `sp_dc` cookie
 * is written by the accounts page, so the player was only ever the blank doorstep after it.
 */
private const val SPOTIFY_LOGIN_URL = "https://accounts.spotify.com/en/login"

/**
 * A real Chrome user agent. The stock WebView UA carries `; wv` and `Version/4.0`, which Spotify's
 * accounts page answers with a variant that renders no form at all - the other half of the blank
 * screen. [com.yasuda.derbis.feature.motion.SpotifyTokenProvider] sends the same identity when it
 * mints a token, so both halves of the feature look like the same browser.
 */
private const val SPOTIFY_BROWSER_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/122.0.0.0 Mobile Safari/537.36"

/** True for the web player, the one Spotify surface a plain WebView cannot draw. */
private fun isSpotifyWebPlayer(url: Uri): Boolean =
    url.host?.equals("open.spotify.com", ignoreCase = true) == true ||
        url.host?.equals("play.spotify.com", ignoreCase = true) == true

/**
 * The sheet's session test uses a well-known track. It does not matter whether this one still has
 * a Canvas: a miss is reported as "the session works, this track has none", which is exactly the
 * difference the test exists to show.
 */
private const val CANVAS_TEST_TRACK_ID = "0VjIjW4GlUZAMYd2vXMi3b"

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
                    .statusBarsPadding()
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
                            if (accepted) onDismiss()
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
                                    status = when (val result = SpotifyOfficialCanvas.probe(CANVAS_TEST_TRACK_ID)) {
                                        is SpotifyOfficialCanvas.Result.Found -> strings.spotifyCanvasTestOk
                                        is SpotifyOfficialCanvas.Result.NoCanvas -> strings.spotifyCanvasTestNoCanvas
                                        is SpotifyOfficialCanvas.Result.Failed -> result.reason
                                    }
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
                            SpotifyOfficialCanvas.invalidate()
                            SpotifyTokenProvider.invalidate()
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
    SpotifyTokenProvider.invalidate()
    SpotifyOfficialCanvas.invalidate()
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141418), RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
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
    onDetected: (String?) -> Unit,
    onCancel: () -> Unit,
    onCopy: (String) -> Unit,
    onUse: (String) -> Unit
) {
    val strings = LocalDerbisStrings.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var detected by remember { mutableStateOf<String?>(null) }

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
        if (failed) {
            Text(
                text = strings.spotifyCanvasWebError,
                color = DerbisMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { context ->
                createSpotifyWebView(
                    context = context,
                    onLoading = { loading = it },
                    onError = { failed = true },
                    onDetected = { cookie ->
                        detected = cookie
                        onDetected(cookie)
                    }
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
                TextButton(onClick = onCancel) { Text(strings.cancel, color = DerbisMuted) }
                TextButton(onClick = { detected?.let(onUse) }, enabled = detected != null) {
                    Text(strings.spotifyCanvasUseAccount, color = if (detected != null) SpotifyGreen else DerbisMuted)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createSpotifyWebView(
    context: Context,
    onLoading: (Boolean) -> Unit,
    onError: () -> Unit,
    onDetected: (String?) -> Unit
): WebView = WebView(context).apply {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    settings.userAgentString = SPOTIFY_BROWSER_UA
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val secure = request.url.scheme.equals("https", ignoreCase = true)
            if (request.isForMainFrame && !secure) {
                onError()
                return true
            }
            // The session is already on the device once the accounts page hands off to the web
            // player, and the player itself is a black rectangle here. Take the cookie and stay on
            // the page that rendered instead of walking into the blank one.
            if (request.isForMainFrame && isSpotifyWebPlayer(request.url)) {
                val cookie = captureSession()
                if (cookie != null) onDetected(cookie) else onError()
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            // A server-side redirect does not always pass through shouldOverrideUrlLoading, so the
            // hand-off to the web player is caught again here and the load is stopped before the
            // blank page can paint over the page that was working.
            val target = url?.let(Uri::parse)
            if (target != null && isSpotifyWebPlayer(target)) {
                view.stopLoading()
                onLoading(false)
                val cookie = captureSession()
                if (cookie != null) onDetected(cookie) else onError()
                return
            }
            onLoading(true)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            onLoading(false)
            // Only report a real cookie: the sign-in hop passes through several pages, and
            // clearing the detection on each of them would disable "Use this account" at random.
            captureSession()?.let(onDetected)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) onError()
        }

        // A refused page is not a network error: it used to arrive as a finished load with
        // nothing on it, so the sheet sat on an empty screen with no message at all.
        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            if (request.isForMainFrame) onError()
        }
    }
    loadUrl(SPOTIFY_LOGIN_URL)
}

/**
 * Reads `sp_dc` out of the WebView's cookie jar. The cookie is httpOnly, so JavaScript on the page
 * cannot see it — but the jar this app owns can, which is the whole point of signing in here.
 */
private fun captureSession(): String? {
    val manager = CookieManager.getInstance()
    manager.flush()
    // `sp_dc` is written on `.spotify.com`, so either jar has it; checking the accounts origin too
    // means a sign-in is picked up even when the player page was never visited at all.
    for (origin in listOf(SPOTIFY_ORIGIN, SPOTIFY_ACCOUNTS_ORIGIN)) {
        val header = manager.getCookie(origin).orEmpty()
        if (header.isBlank()) continue
        SpotifyCookie.parse(header)?.let { return it }
    }
    return null
}

