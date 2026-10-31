package com.yasuda.derbis.ui.spotify

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
private const val SPOTIFY_LOGIN_URL =
    "https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F"

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
    onSessionSaved: () -> Unit = {}
) {
    val strings = LocalDerbisStrings.current
    val context = LocalContext.current
    val session by DerbisSpotifySession.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var browsing by remember { mutableStateOf(false) }
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

    val stored = DerbisSpotifySession.cookie()

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
                    // The developer door: what was captured, and where to paste a different one.
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
                        onDetected = { detected = it },
                        onCancel = { browsing = false },
                        onUse = { cookie ->
                            val (accepted, message) = applySession(context, cookie, strings, onSessionSaved)
                            status = message
                            if (accepted) browsing = false
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

                        SpotifyActionButton(
                            icon = Icons.Rounded.OpenInBrowser,
                            label = strings.spotifyCanvasOpenBrowser,
                            emphasis = true
                        ) {
                            detected = null
                            browsing = true
                        }

                        SessionCard(
                            stored = stored,
                            busy = busy,
                            status = status,
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

                        if (developerOpen) {
                            DeveloperCard(
                                stored = stored,
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
                        } else if (stored.isNotEmpty()) {
                            TextButton(onClick = { developerOpen = true }) {
                                Text(strings.spotifyCanvasDeveloper, color = DerbisMuted, fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
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
        if (status.isNotBlank()) {
            Text(status, color = DerbisMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun DeveloperCard(
    stored: String,
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
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(strings.spotifyCanvasDeveloper, color = DerbisText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(strings.spotifyCanvasCookieLabel, color = DerbisMuted, fontSize = 11.sp, letterSpacing = 0.6.sp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    stored.isEmpty() -> strings.spotifyCanvasNoSession
                    revealed -> stored
                    else -> SpotifyCookie.masked(stored)
                },
                color = if (stored.isEmpty()) DerbisMuted else DerbisText,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = if (revealed) 4 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
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

        Text(
            text = strings.spotifyCanvasDeveloperHint,
            color = DerbisMuted.copy(alpha = 0.75f),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun SpotifyBrowserPane(
    modifier: Modifier = Modifier,
    onDetected: (String?) -> Unit,
    onCancel: () -> Unit,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (detected != null) strings.spotifyCanvasDetected else strings.spotifyCanvasSigningIn,
                color = DerbisMuted,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) { Text(strings.cancel, color = DerbisMuted) }
            TextButton(onClick = { detected?.let(onUse) }, enabled = detected != null) {
                Text(strings.spotifyCanvasUseAccount, color = if (detected != null) SpotifyGreen else DerbisMuted)
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
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val secure = request.url.scheme.equals("https", ignoreCase = true)
            if (request.isForMainFrame && !secure) {
                onError()
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
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
    val header = manager.getCookie(SPOTIFY_ORIGIN).orEmpty()
    if (header.isBlank()) return null
    return SpotifyCookie.parse(header)
}

@Composable
private fun SpotifyActionButton(
    icon: ImageVector,
    label: String,
    emphasis: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (emphasis) SpotifyGreen.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (emphasis) SpotifyGreen.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (emphasis) SpotifyGreen else DerbisMuted,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = DerbisText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}
