package com.yasuda.derbis.feature.motion

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.yasuda.derbis.data.spotify.DerbisSpotifySession
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber

/**
 * The bearer token behind Spotify's own web player, minted from the listener's session cookie
 * rather than from an app credential. Ported from BitChord (`data/canvas/SpotifyToken.kt`), which
 * explains why it has to work this way: `/api/token` checks a TOTP computed from a secret in the
 * web player's own JS bundle, and a token signed by hand gets a 429 from both api.spotify.com and
 * spclient on its very first request. So instead of signing anything, this loads
 * open.spotify.com for real in an offscreen WebView with the cookie applied, and reads the token
 * the page mints for itself by hooking `fetch`/XHR before its own bundle runs.
 *
 * Call [init] once at process start. Every method here is best-effort: a null answer means "no
 * Canvas this time", never an exception.
 */
internal object SpotifyTokenProvider {
    private const val BRIDGE_NAME = "DerbisSpotifyTokenBridge"
    private const val HARVEST_TIMEOUT_MS = 20_000L
    private const val DEFAULT_LIFETIME_MS = 3_600_000L
    private const val CLIENT_TOKEN_URL = "https://clienttoken.spotify.com/v1/clienttoken"
    private const val WEB_PLAYER_URL = "https://open.spotify.com"

    /**
     * The browser UA these endpoints expect. spclient turns away anything that is not a known
     * client, so the requests below wear this instead of the app's own.
     */
    const val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/122.0.0.0 Safari/537.36"

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(16, TimeUnit.SECONDS)
        .build()

    private val harvestMutex = Mutex()

    @Volatile private var appContext: Context? = null

    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiresAtMs = 0L
    @Volatile private var tokenCookie = ""
    @Volatile private var cachedClientId: String? = null

    @Volatile private var cachedClientToken: String? = null
    @Volatile private var clientTokenExpiresAtMs = 0L

    @Volatile private var cachedSession: SessionInfo? = null

    private data class SessionInfo(val clientVersion: String, val deviceId: String)
    private data class HarvestedToken(val token: String, val expiresAtMs: Long, val clientId: String?)

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Drops every cached credential. Called when the session cookie is replaced or removed. */
    fun invalidate() {
        cachedToken = null
        tokenExpiresAtMs = 0L
        tokenCookie = ""
        cachedClientId = null
        cachedClientToken = null
        clientTokenExpiresAtMs = 0L
    }

    /**
     * A live bearer token, or null when there is no cookie, [init] was never called, or the
     * harvest failed. Cached until shortly before it expires, and thrown away as soon as the
     * cookie it was minted from changes.
     */
    suspend fun accessToken(): String? {
        val cookie = DerbisSpotifySession.cookie()
        if (cookie.isBlank()) return null

        val now = System.currentTimeMillis()
        cachedToken?.let { token ->
            if (tokenCookie == cookie && now < tokenExpiresAtMs - 30_000) return token
        }

        return harvestMutex.withLock {
            val stillNow = System.currentTimeMillis()
            cachedToken?.let { token ->
                if (tokenCookie == cookie && stillNow < tokenExpiresAtMs - 30_000) return@withLock token
            }

            val context = appContext
            if (context == null) {
                Timber.w("SpotifyTokenProvider.init was never called; no context for the harvest")
                return@withLock null
            }

            val harvested = withContext(Dispatchers.Main) { harvest(context, cookie) }
            if (harvested == null) {
                Timber.d("Spotify token harvest failed or timed out")
                return@withLock null
            }

            cachedToken = harvested.token
            tokenExpiresAtMs = harvested.expiresAtMs
            tokenCookie = cookie
            harvested.clientId?.let { cachedClientId = it }
            harvested.token
        }
    }

    /**
     * The second header these endpoints demand next to the bearer — a request carrying only the
     * bearer is answered with a 429 that reads exactly like rate limiting. Needs [accessToken] to
     * have succeeded at least once, because the client id it takes comes off that response.
     */
    suspend fun clientToken(): String? {
        val now = System.currentTimeMillis()
        cachedClientToken?.let { if (now < clientTokenExpiresAtMs - 30_000) return it }

        val clientId = cachedClientId ?: return null
        val session = session() ?: return null

        val payload = JSONObject()
            .put(
                "client_data",
                JSONObject()
                    .put("client_version", session.clientVersion)
                    .put("client_id", clientId)
                    .put(
                        "js_sdk_data",
                        JSONObject()
                            .put("device_brand", "unknown")
                            .put("device_model", "unknown")
                            .put("os", "android")
                            .put("os_version", Build.VERSION.RELEASE.orEmpty())
                            .put("device_id", session.deviceId)
                            .put("device_type", "smartphone")
                    )
            )
            .toString()

        // The ByteArray overload, deliberately: the String overload rewrites a charset-less
        // MediaType to "application/json; charset=utf-8", and clienttoken 400s on exactly that.
        val request = Request.Builder()
            .url(CLIENT_TOKEN_URL)
            .post(payload.toByteArray(Charsets.UTF_8).toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .header("User-Agent", BROWSER_UA)
            .build()

        val body = runCatching {
            withContext(Dispatchers.IO) {
                http.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }
        }.onFailure { Timber.d(it, "client-token request failed") }.getOrNull() ?: return null

        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (root.optString("response_type") != "RESPONSE_GRANTED_TOKEN_RESPONSE") return null
        val granted = root.optJSONObject("granted_token") ?: return null
        val token = granted.optString("token").trim().ifBlank { return null }
        val ttlSeconds = granted.optLong("expires_after_seconds", 3_600L).coerceAtLeast(60L)

        cachedClientToken = token
        clientTokenExpiresAtMs = now + ttlSeconds * 1_000L
        return token
    }

    /**
     * [SessionInfo.clientVersion] and [SessionInfo.deviceId] both come off the plain,
     * unauthenticated web player page: the client version from the JSON blob the page embeds for
     * itself, the device id from the `sp_t` cookie it hands to every visitor.
     */
    private suspend fun session(): SessionInfo? {
        cachedSession?.let { return it }

        val request = Request.Builder()
            .url(WEB_PLAYER_URL)
            .header("User-Agent", BROWSER_UA)
            .build()
        val page = runCatching {
            withContext(Dispatchers.IO) {
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string()
                    val deviceId = response.headers("Set-Cookie").firstNotNullOfOrNull { header ->
                        Regex("""sp_t=([^;]+)""").find(header)?.groupValues?.getOrNull(1)
                    }
                    body to deviceId
                }
            }
        }.getOrNull() ?: (null to null)
        val html = page.first ?: return null

        val configBase64 = Regex("""<script id="appServerConfig" type="text/plain">([^<]+)</script>""")
            .find(html)?.groupValues?.getOrNull(1) ?: return null
        val clientVersion = runCatching {
            val decoded = String(Base64.decode(configBase64, Base64.DEFAULT), Charsets.UTF_8)
            JSONObject(decoded).optString("clientVersion").trim()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null

        val info = SessionInfo(clientVersion, page.second ?: UUID.randomUUID().toString())
        cachedSession = info
        return info
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun harvest(context: Context, cookie: String): HarvestedToken? {
        val deferred = CompletableDeferred<HarvestedToken?>()

        val cookieManager = CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setCookie("https://open.spotify.com/", "sp_dc=$cookie; Domain=.spotify.com; Path=/; Secure")
            setCookie("https://accounts.spotify.com/", "sp_dc=$cookie; Domain=.spotify.com; Path=/; Secure")
            flush()
        }
        // The player keeps its token in web storage and skips a fresh /api/token call whenever a
        // live one is already there, which would leave the hook with nothing to see.
        runCatching { WebStorage.getInstance().deleteAllData() }

        var webView: WebView? = null
        return try {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = BROWSER_UA
                cookieManager.setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(TokenBridge(deferred), BRIDGE_NAME)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view.evaluateJavascript(HOOK_SCRIPT, null)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        // Re-assert: the player can navigate client-side after the first injection.
                        view.evaluateJavascript(HOOK_SCRIPT, null)
                    }
                }
                loadUrl(WEB_PLAYER_URL)
            }

            withTimeoutOrNull(HARVEST_TIMEOUT_MS) { deferred.await() }
        } catch (error: Exception) {
            Timber.d(error, "Spotify token harvest threw")
            null
        } finally {
            runCatching {
                webView?.removeJavascriptInterface(BRIDGE_NAME)
                webView?.stopLoading()
                webView?.destroy()
            }
        }
    }

    /** Receives the raw `/api/token` body from the hooked page. */
    // internal, not private: the bridge is reached by reflection from android.webkit, which
    // cannot open a package-private class. Kotlin `internal` stays public in bytecode.
    internal class TokenBridge(private val deferred: CompletableDeferred<HarvestedToken?>) {
        @JavascriptInterface
        fun onTokenPayload(payload: String?) {
            if (payload.isNullOrBlank() || deferred.isCompleted) return
            runCatching {
                val root = JSONObject(payload)
                val token = root.optString("accessToken").trim()
                // The player mints an anonymous token before the cookie takes effect; that one
                // cannot read a Canvas, so keep waiting for the signed-in one.
                if (token.isBlank() || root.optBoolean("isAnonymous", false)) return
                val expiresAt = root.optLong("accessTokenExpirationTimestampMs")
                    .takeIf { it > System.currentTimeMillis() }
                    ?: (System.currentTimeMillis() + DEFAULT_LIFETIME_MS)
                val clientId = root.optString("clientId").trim().ifBlank { null }
                deferred.complete(HarvestedToken(token, expiresAt, clientId))
            }.onFailure { Timber.d(it, "Unreadable /api/token payload") }
        }
    }

    private val HOOK_SCRIPT = """
        (function () {
          if (window.__derbisTokenHook) return;
          window.__derbisTokenHook = true;
          var report = function (body) {
            try { $BRIDGE_NAME.onTokenPayload(body); } catch (e) {}
          };
          var isToken = function (u) {
            try { return String(u).indexOf('/api/token') !== -1; } catch (e) { return false; }
          };
          var origFetch = window.fetch;
          if (origFetch) {
            window.fetch = function (input, init) {
              var url = (input && input.url) ? input.url : input;
              var result = origFetch.apply(this, arguments);
              if (isToken(url)) {
                try {
                  result.then(function (res) {
                    res.clone().text().then(report).catch(function () {});
                  }).catch(function () {});
                } catch (e) {}
              }
              return result;
            };
          }
          var origOpen = XMLHttpRequest.prototype.open;
          XMLHttpRequest.prototype.open = function (method, url) {
            this.__derbisUrl = url;
            return origOpen.apply(this, arguments);
          };
          var origSend = XMLHttpRequest.prototype.send;
          XMLHttpRequest.prototype.send = function () {
            var xhr = this;
            try {
              xhr.addEventListener('load', function () {
                if (isToken(xhr.__derbisUrl)) {
                  try { report(xhr.responseText); } catch (e) {}
                }
              });
            } catch (e) {}
            return origSend.apply(this, arguments);
          };
        })();
    """.trimIndent()
}
