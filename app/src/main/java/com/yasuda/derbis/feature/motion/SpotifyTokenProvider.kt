package com.yasuda.derbis.feature.motion

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.yasuda.derbis.data.spotify.DerbisSpotifySession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
 * Mints and caches Spotify's own web-player credentials (`Bearer` access token, `client-token`,
 * `spotify-app-version`, and live Pathfinder persisted-query hashes) from the listener's stored
 * `sp_dc` cookie.
 *
 * `/api/token` checks a TOTP derived inside Spotify's web-player JS bundle, so we load
 * `open.spotify.com` in an offscreen `WebView` with `sp_dc` attached and capture both the signed-in
 * token and the `client-token` / `spotify-app-version` headers the web player mints for itself.
 */
internal object SpotifyTokenProvider {
    private const val BRIDGE_NAME = "DerbisSpotifyTokenBridge"
    private const val HARVEST_TIMEOUT_MS = 20_000L
    private const val CLIENT_TOKEN_SETTLE_MS = 500L
    private const val DEFAULT_LIFETIME_MS = 3_600_000L
    private const val DEFAULT_WEB_CLIENT_ID = "d8a5ed958d274c2e8ee717e6a4b0971d"
    private const val FALLBACK_CLIENT_VERSION = "1.2.60.564.g8cd2f22c"
    private const val CLIENT_TOKEN_URL = "https://clienttoken.spotify.com/v1/clienttoken"
    private const val WEB_PLAYER_URL = "https://open.spotify.com/"

    const val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.36"

    data class SessionCredentials(
        val accessToken: String,
        val clientId: String,
        val clientToken: String?,
        val clientVersion: String,
        val expiresAtMs: Long
    )

    internal data class HarvestedToken(
        val token: String,
        val expiresAtMs: Long,
        val clientId: String?,
        val clientToken: String?,
        val clientVersion: String?,
        val sawAnonymousOnly: Boolean = false
    )

    private data class SessionInfo(val clientVersion: String, val deviceId: String)

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(16, TimeUnit.SECONDS)
        .build()

    private val harvestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val harvestMutex = Mutex()

    @Volatile private var appContext: Context? = null
    @Volatile private var inFlightHarvest: Deferred<HarvestedToken?>? = null
    @Volatile private var inFlightCookie: String = ""

    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiresAtMs = 0L
    @Volatile private var tokenCookie = ""
    @Volatile private var cachedClientId: String? = null

    @Volatile private var cachedClientToken: String? = null
    @Volatile private var clientTokenExpiresAtMs = 0L
    @Volatile private var cachedClientVersion: String? = null
    @Volatile private var cachedSession: SessionInfo? = null
    @Volatile private var lastHarvestRejectedCookie: Boolean = false

    private val pathfinderHashes = ConcurrentHashMap<String, String>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun hasWarmToken(): Boolean {
        val cookie = DerbisSpotifySession.cookie()
        if (cookie.isBlank()) return false
        val token = cachedToken ?: return false
        return token.isNotBlank() &&
            tokenCookie == cookie &&
            System.currentTimeMillis() < tokenExpiresAtMs - 30_000L
    }

    fun warmUp() {
        if (!DerbisSpotifySession.hasSession() || hasWarmToken()) return
        harvestScope.launch {
            runCatching { credentials() }
        }
    }

    fun lastHarvestSawExpiredCookie(): Boolean = lastHarvestRejectedCookie

    fun pathfinderHash(operationName: String): String? = pathfinderHashes[operationName]

    internal fun recordPathfinderHash(operationName: String, sha256Hash: String) {
        if (operationName.isNotBlank() && HEX_64.matches(sha256Hash)) {
            pathfinderHashes[operationName] = sha256Hash
        }
    }

    /** Drops every cached credential. Called when the session cookie is replaced or removed. */
    fun invalidate() {
        cachedToken = null
        tokenExpiresAtMs = 0L
        tokenCookie = ""
        cachedClientId = null
        cachedClientToken = null
        clientTokenExpiresAtMs = 0L
        lastHarvestRejectedCookie = false
    }

    suspend fun accessToken(): String? = credentials()?.accessToken

    suspend fun credentials(forceRefresh: Boolean = false): SessionCredentials? {
        val cookie = DerbisSpotifySession.cookie()
        if (cookie.isBlank()) return null

        if (forceRefresh) {
            invalidate()
        }

        val now = System.currentTimeMillis()
        cachedToken?.let { token ->
            if (tokenCookie == cookie && now < tokenExpiresAtMs - 30_000L) {
                val clientId = cachedClientId ?: DEFAULT_WEB_CLIENT_ID
                val cToken = cachedClientToken?.takeIf { now < clientTokenExpiresAtMs - 30_000L }
                    ?: mintClientToken(clientId)
                val cVersion = cachedClientVersion ?: cachedSession?.clientVersion ?: FALLBACK_CLIENT_VERSION
                return SessionCredentials(
                    accessToken = token,
                    clientId = clientId,
                    clientToken = cToken,
                    clientVersion = cVersion,
                    expiresAtMs = tokenExpiresAtMs
                )
            }
        }

        val sharedDeferred = harvestMutex.withLock {
            val checkNow = System.currentTimeMillis()
            cachedToken?.let { token ->
                if (tokenCookie == cookie && checkNow < tokenExpiresAtMs - 30_000L) {
                    return@withLock CompletableDeferred(
                        HarvestedToken(
                            token = token,
                            expiresAtMs = tokenExpiresAtMs,
                            clientId = cachedClientId,
                            clientToken = cachedClientToken,
                            clientVersion = cachedClientVersion
                        )
                    )
                }
            }
            val existing = inFlightHarvest
            if (existing != null && existing.isActive && inFlightCookie == cookie) {
                return@withLock existing
            }
            val context = appContext
            if (context == null) {
                Timber.w("SpotifyTokenProvider.init was never called; no context for the harvest")
                return@withLock null
            }
            inFlightCookie = cookie
            harvestScope.async {
                try {
                    val harvested = withContext(Dispatchers.Main) { harvest(context, cookie) }
                    if (harvested != null && harvested.token.isNotBlank()) {
                        lastHarvestRejectedCookie = false
                        cachedToken = harvested.token
                        tokenExpiresAtMs = harvested.expiresAtMs
                        tokenCookie = cookie
                        cachedClientId = harvested.clientId ?: DEFAULT_WEB_CLIENT_ID
                        harvested.clientVersion?.takeIf { it.isNotBlank() }?.let { cachedClientVersion = it }
                        harvested.clientToken?.takeIf { it.isNotBlank() }?.let { ct ->
                            cachedClientToken = ct
                            clientTokenExpiresAtMs = System.currentTimeMillis() + DEFAULT_LIFETIME_MS
                        }
                    } else if (harvested?.sawAnonymousOnly == true) {
                        lastHarvestRejectedCookie = true
                    }
                    harvested
                } finally {
                    if (inFlightCookie == cookie) {
                        inFlightHarvest = null
                    }
                }
            }.also { inFlightHarvest = it }
        } ?: return null

        val harvested = try {
            sharedDeferred.await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Timber.d(error, "Spotify token harvest await failed")
            null
        } ?: return null

        if (harvested.token.isBlank()) return null

        val clientId = harvested.clientId ?: cachedClientId ?: DEFAULT_WEB_CLIENT_ID
        val nowAfter = System.currentTimeMillis()
        val resolvedClientToken = harvested.clientToken?.takeIf { it.isNotBlank() }
            ?: cachedClientToken?.takeIf { nowAfter < clientTokenExpiresAtMs - 30_000L }
            ?: mintClientToken(clientId)
        val resolvedVersion = harvested.clientVersion?.takeIf { it.isNotBlank() }
            ?: cachedClientVersion
            ?: cachedSession?.clientVersion
            ?: FALLBACK_CLIENT_VERSION

        return SessionCredentials(
            accessToken = harvested.token,
            clientId = clientId,
            clientToken = resolvedClientToken,
            clientVersion = resolvedVersion,
            expiresAtMs = harvested.expiresAtMs
        )
    }

    suspend fun clientToken(): String? {
        val now = System.currentTimeMillis()
        cachedClientToken?.let { if (now < clientTokenExpiresAtMs - 30_000L) return it }
        val clientId = cachedClientId ?: credentials()?.clientId ?: DEFAULT_WEB_CLIENT_ID
        cachedClientToken?.let { if (System.currentTimeMillis() < clientTokenExpiresAtMs - 30_000L) return it }
        return mintClientToken(clientId)
    }

    private suspend fun mintClientToken(clientId: String): String? {
        val now = System.currentTimeMillis()
        cachedClientToken?.let { if (now < clientTokenExpiresAtMs - 30_000L) return it }

        val session = session()
        val clientVersion = cachedClientVersion
            ?.takeIf { it.isNotBlank() }
            ?: session?.clientVersion
            ?: FALLBACK_CLIENT_VERSION
        val deviceId = session?.deviceId ?: UUID.randomUUID().toString()

        val payload = JSONObject()
            .put(
                "client_data",
                JSONObject()
                    .put("client_version", clientVersion)
                    .put("client_id", clientId.ifBlank { DEFAULT_WEB_CLIENT_ID })
                    .put(
                        "js_sdk_data",
                        JSONObject()
                            .put("device_brand", "unknown")
                            .put("device_model", "unknown")
                            .put("os", "windows")
                            .put("os_version", "NT 10.0")
                            .put("device_id", deviceId)
                            .put("device_type", "computer")
                    )
            )
            .toString()

        // Use ByteArray to keep MediaType strictly "application/json" without "; charset=utf-8".
        val request = Request.Builder()
            .url(CLIENT_TOKEN_URL)
            .post(payload.toByteArray(Charsets.UTF_8).toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .header("Origin", "https://open.spotify.com")
            .header("Referer", "https://open.spotify.com/")
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

    private suspend fun session(): SessionInfo? {
        cachedSession?.let { return it }

        val request = Request.Builder()
            .url(WEB_PLAYER_URL)
            .header("User-Agent", BROWSER_UA)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()
        val page = runCatching {
            withContext(Dispatchers.IO) {
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string()
                    val deviceId = response.headers("Set-Cookie").firstNotNullOfOrNull { header ->
                        SP_T_COOKIE_REGEX.find(header)?.groupValues?.getOrNull(1)
                    }
                    body to deviceId
                }
            }
        }.getOrNull() ?: (null to null)

        val html = page.first
        val parsedVersion = html?.let(::extractClientVersionFromHtml)
            ?: cachedClientVersion
            ?: FALLBACK_CLIENT_VERSION
        val info = SessionInfo(parsedVersion, page.second ?: UUID.randomUUID().toString())
        if (html != null) {
            cachedSession = info
        }
        return info
    }

    private fun extractClientVersionFromHtml(html: String): String? {
        APP_SERVER_CONFIG_SCRIPT_REGEX.find(html)?.groupValues?.getOrNull(1)?.trim()?.let { raw ->
            val fromJson = runCatching { JSONObject(raw).optString("clientVersion").trim() }
                .getOrNull()?.takeIf { it.isNotBlank() }
            if (fromJson != null) return fromJson
            val fromBase64 = runCatching {
                val decoded = String(Base64.decode(raw, Base64.DEFAULT), Charsets.UTF_8)
                JSONObject(decoded).optString("clientVersion").trim()
            }.getOrNull()?.takeIf { it.isNotBlank() }
            if (fromBase64 != null) return fromBase64
        }
        CLIENT_VERSION_JSON_REGEX.find(html)?.groupValues?.getOrNull(1)?.trim()?.let { direct ->
            if (direct.isNotBlank()) return direct
        }
        return null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun harvest(context: Context, cookie: String): HarvestedToken? {
        val deferred = CompletableDeferred<HarvestedToken?>()
        val bridge = TokenBridge(deferred)

        val cookieManager = CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setCookie("https://spotify.com/", "sp_dc=$cookie; Domain=.spotify.com; Path=/; Secure")
            setCookie("https://open.spotify.com/", "sp_dc=$cookie; Domain=.spotify.com; Path=/; Secure")
            setCookie("https://accounts.spotify.com/", "sp_dc=$cookie; Domain=.spotify.com; Path=/; Secure")
            flush()
        }
        runCatching { WebStorage.getInstance().deleteAllData() }

        var webView: WebView? = null
        return try {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = BROWSER_UA
                cookieManager.setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(bridge, BRIDGE_NAME)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view.evaluateJavascript(HOOK_SCRIPT, null)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript(HOOK_SCRIPT, null)
                        view.evaluateJavascript(DOM_SCAN_SCRIPT, null)
                    }
                }
                loadUrl(WEB_PLAYER_URL)
            }

            val completed = withTimeoutOrNull(HARVEST_TIMEOUT_MS) { deferred.await() }
            completed ?: bridge.snapshotOnTimeout()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.d(error, "Spotify token harvest threw")
            bridge.snapshotOnTimeout()
        } finally {
            bridge.dispose()
            runCatching {
                webView?.removeJavascriptInterface(BRIDGE_NAME)
                webView?.stopLoading()
                webView?.destroy()
            }
        }
    }

    // internal so android.webkit reflection can reach the @JavascriptInterface entry points.
    internal class TokenBridge(private val deferred: CompletableDeferred<HarvestedToken?>) {
        private val mainHandler = Handler(Looper.getMainLooper())
        @Volatile private var pendingToken: String? = null
        @Volatile private var pendingExpiresAtMs: Long = 0L
        @Volatile private var pendingClientId: String? = null
        @Volatile private var pendingClientToken: String? = null
        @Volatile private var pendingClientVersion: String? = null
        @Volatile private var sawAnonymousToken: Boolean = false
        @Volatile private var settleScheduled: Boolean = false

        private val settleRunnable = Runnable {
            completeIfReady(force = true)
        }

        fun dispose() {
            mainHandler.removeCallbacks(settleRunnable)
        }

        fun snapshotOnTimeout(): HarvestedToken? {
            val token = pendingToken
            if (!token.isNullOrBlank()) {
                return HarvestedToken(
                    token = token,
                    expiresAtMs = pendingExpiresAtMs.takeIf { it > System.currentTimeMillis() }
                        ?: (System.currentTimeMillis() + DEFAULT_LIFETIME_MS),
                    clientId = pendingClientId,
                    clientToken = pendingClientToken,
                    clientVersion = pendingClientVersion
                )
            }
            if (sawAnonymousToken) {
                return HarvestedToken(
                    token = "",
                    expiresAtMs = 0L,
                    clientId = null,
                    clientToken = null,
                    clientVersion = null,
                    sawAnonymousOnly = true
                )
            }
            return null
        }

        private fun completeIfReady(force: Boolean) {
            if (deferred.isCompleted) return
            val token = pendingToken?.takeIf { it.isNotBlank() } ?: return
            val hasClientMeta = !pendingClientToken.isNullOrBlank()
            if (hasClientMeta || force) {
                mainHandler.removeCallbacks(settleRunnable)
                deferred.complete(
                    HarvestedToken(
                        token = token,
                        expiresAtMs = pendingExpiresAtMs.takeIf { it > System.currentTimeMillis() }
                            ?: (System.currentTimeMillis() + DEFAULT_LIFETIME_MS),
                        clientId = pendingClientId,
                        clientToken = pendingClientToken,
                        clientVersion = pendingClientVersion
                    )
                )
            } else if (!settleScheduled) {
                settleScheduled = true
                mainHandler.postDelayed(settleRunnable, CLIENT_TOKEN_SETTLE_MS)
            }
        }

        @JavascriptInterface
        fun onTokenPayload(payload: String?) {
            if (payload.isNullOrBlank() || deferred.isCompleted) return
            runCatching {
                val root = JSONObject(payload)
                val token = root.optString("accessToken").trim()
                if (token.isBlank()) return
                if (root.optBoolean("isAnonymous", false)) {
                    sawAnonymousToken = true
                    return
                }
                pendingToken = token
                pendingExpiresAtMs = root.optLong("accessTokenExpirationTimestampMs")
                    .takeIf { it > System.currentTimeMillis() }
                    ?: (System.currentTimeMillis() + DEFAULT_LIFETIME_MS)
                root.optString("clientId").trim().takeIf { it.isNotBlank() }?.let { pendingClientId = it }
                completeIfReady(force = false)
            }.onFailure { Timber.d(it, "Unreadable Spotify token payload") }
        }

        @JavascriptInterface
        fun onClientTokenPayload(payload: String?) {
            if (payload.isNullOrBlank()) return
            runCatching {
                val root = JSONObject(payload)
                val granted = root.optJSONObject("granted_token") ?: return
                val token = granted.optString("token").trim()
                if (token.isNotBlank()) {
                    pendingClientToken = token
                    completeIfReady(force = false)
                }
            }
        }

        @JavascriptInterface
        fun onObservedHeaders(authHeader: String?, clientToken: String?, appVersion: String?) {
            if (!clientToken.isNullOrBlank()) {
                pendingClientToken = clientToken.trim()
            }
            if (!appVersion.isNullOrBlank()) {
                pendingClientVersion = appVersion.trim()
            }
            if (!authHeader.isNullOrBlank() && pendingToken.isNullOrBlank()) {
                val bearer = authHeader.trim()
                if (bearer.startsWith("Bearer ", ignoreCase = true)) {
                    val rawToken = bearer.substring(7).trim()
                    // Only accept observed Bearer header if we have not seen an anonymous session.
                    if (rawToken.length > 40 && !sawAnonymousToken) {
                        pendingToken = rawToken
                        if (pendingExpiresAtMs <= 0L) {
                            pendingExpiresAtMs = System.currentTimeMillis() + DEFAULT_LIFETIME_MS
                        }
                    }
                }
            }
            if (!pendingToken.isNullOrBlank() && !pendingClientToken.isNullOrBlank()) {
                completeIfReady(force = true)
            }
        }

        @JavascriptInterface
        fun onPathfinderHash(operationName: String?, sha256Hash: String?) {
            if (!operationName.isNullOrBlank() && !sha256Hash.isNullOrBlank()) {
                recordPathfinderHash(operationName.trim(), sha256Hash.trim())
            }
        }
    }

    private val SP_T_COOKIE_REGEX = Regex("""sp_t=([^;]+)""")
    private val HEX_64 = Regex("^[0-9a-fA-F]{64}$")
    private val APP_SERVER_CONFIG_SCRIPT_REGEX =
        Regex("""<script[^>]*\bid=["']appServerConfig["'][^>]*>([^<]+)</script>""", RegexOption.IGNORE_CASE)
    private val CLIENT_VERSION_JSON_REGEX =
        Regex(""""clientVersion"\s*:\s*"([^"]+)"""")

    private val HOOK_SCRIPT = """
        (function () {
          if (window.__derbisTokenHook) return;
          window.__derbisTokenHook = true;
          var reportToken = function (body) {
            try { $BRIDGE_NAME.onTokenPayload(body); } catch (e) {}
          };
          var reportClientToken = function (body) {
            try { $BRIDGE_NAME.onClientTokenPayload(body); } catch (e) {}
          };
          var reportHeaders = function (auth, ct, ver) {
            try { $BRIDGE_NAME.onObservedHeaders(auth || '', ct || '', ver || ''); } catch (e) {}
          };
          var reportHash = function (op, hash) {
            try { $BRIDGE_NAME.onPathfinderHash(op || '', hash || ''); } catch (e) {}
          };
          var isTokenUrl = function (u) {
            try {
              var s = String(u);
              return s.indexOf('/api/token') !== -1 || s.indexOf('/get_access_token') !== -1;
            } catch (e) { return false; }
          };
          var isClientTokenUrl = function (u) {
            try { return String(u).indexOf('clienttoken.spotify.com') !== -1; } catch (e) { return false; }
          };
          var readHeader = function (headers, name) {
            if (!headers) return '';
            try {
              if (typeof headers.get === 'function') return headers.get(name) || headers.get(name.toLowerCase()) || '';
              if (Array.isArray(headers)) {
                for (var i = 0; i < headers.length; i++) {
                  if (headers[i] && String(headers[i][0]).toLowerCase() === name.toLowerCase()) return headers[i][1] || '';
                }
              } else if (typeof headers === 'object') {
                for (var k in headers) {
                  if (Object.prototype.hasOwnProperty.call(headers, k) && k.toLowerCase() === name.toLowerCase()) {
                    return headers[k] || '';
                  }
                }
              }
            } catch (e) {}
            return '';
          };
          var inspectBody = function (body) {
            try {
              if (typeof body !== 'string' || body.indexOf('persistedQuery') === -1) return;
              var j = JSON.parse(body);
              var op = j && j.operationName;
              var h = j && j.extensions && j.extensions.persistedQuery && j.extensions.persistedQuery.sha256Hash;
              if (op && h) reportHash(op, h);
            } catch (e) {}
          };
          var origFetch = window.fetch;
          if (origFetch) {
            window.fetch = function (input, init) {
              var url = (input && input.url) ? input.url : input;
              try {
                var hdrs = (init && init.headers) || (input && input.headers);
                var auth = readHeader(hdrs, 'authorization');
                var ct = readHeader(hdrs, 'client-token');
                var ver = readHeader(hdrs, 'spotify-app-version');
                if (auth || ct || ver) reportHeaders(auth, ct, ver);
                if (init && init.body) inspectBody(init.body);
              } catch (e) {}
              var result = origFetch.apply(this, arguments);
              if (isTokenUrl(url)) {
                try {
                  result.then(function (res) {
                    res.clone().text().then(reportToken).catch(function () {});
                  }).catch(function () {});
                } catch (e) {}
              } else if (isClientTokenUrl(url)) {
                try {
                  result.then(function (res) {
                    res.clone().text().then(reportClientToken).catch(function () {});
                  }).catch(function () {});
                } catch (e) {}
              }
              return result;
            };
          }
          var origOpen = XMLHttpRequest.prototype.open;
          XMLHttpRequest.prototype.open = function (method, url) {
            this.__derbisUrl = url;
            this.__derbisHeaders = {};
            return origOpen.apply(this, arguments);
          };
          var origSetHeader = XMLHttpRequest.prototype.setRequestHeader;
          XMLHttpRequest.prototype.setRequestHeader = function (name, value) {
            try {
              if (this.__derbisHeaders && name) this.__derbisHeaders[String(name).toLowerCase()] = String(value || '');
            } catch (e) {}
            return origSetHeader.apply(this, arguments);
          };
          var origSend = XMLHttpRequest.prototype.send;
          XMLHttpRequest.prototype.send = function (body) {
            var xhr = this;
            try {
              var h = xhr.__derbisHeaders || {};
              if (h['authorization'] || h['client-token'] || h['spotify-app-version']) {
                reportHeaders(h['authorization'], h['client-token'], h['spotify-app-version']);
              }
              if (typeof body === 'string') inspectBody(body);
              xhr.addEventListener('load', function () {
                if (isTokenUrl(xhr.__derbisUrl)) {
                  try { reportToken(xhr.responseText); } catch (e) {}
                } else if (isClientTokenUrl(xhr.__derbisUrl)) {
                  try { reportClientToken(xhr.responseText); } catch (e) {}
                }
              });
            } catch (e) {}
            return origSend.apply(this, arguments);
          };
        })();
    """.trimIndent()

    private val DOM_SCAN_SCRIPT = """
        (function () {
          try {
            var sessionEl = document.getElementById('session') || document.querySelector('script[data-testid="session"]');
            if (sessionEl && sessionEl.textContent) {
              $BRIDGE_NAME.onTokenPayload(sessionEl.textContent);
            }
            var cfgEl = document.getElementById('appServerConfig');
            if (cfgEl && cfgEl.textContent) {
              try {
                var raw = cfgEl.textContent.trim();
                var decoded = raw.charAt(0) === '{' ? raw : atob(raw);
                var cfg = JSON.parse(decoded);
                if (cfg && cfg.clientVersion) {
                  $BRIDGE_NAME.onObservedHeaders('', '', String(cfg.clientVersion));
                }
              } catch (e) {}
            }
            if (!window.__derbisBundleScanned) {
              window.__derbisBundleScanned = true;
              var scripts = document.querySelectorAll('script[src]');
              for (var i = 0; i < scripts.length; i++) {
                var src = scripts[i].src || '';
                if (src.indexOf('xpui') !== -1 || src.indexOf('web-player') !== -1) {
                  fetch(src).then(function (r) { return r.text(); }).then(function (txt) {
                    var ops = ['canvas', 'searchDesktop'];
                    for (var j = 0; j < ops.length; j++) {
                      var op = ops[j];
                      var m = new RegExp('"' + op + '","query","([0-9a-f]{64})"').exec(txt) ||
                              new RegExp('name:"' + op + '",operation:"query",value:"([0-9a-f]{64})"').exec(txt) ||
                              new RegExp('"name":"' + op + '","operation":"query","value":"([0-9a-f]{64})"').exec(txt);
                      if (m && m[1]) $BRIDGE_NAME.onPathfinderHash(op, m[1]);
                    }
                  }).catch(function () {});
                }
              }
            }
          } catch (e) {}
        })();
    """.trimIndent()
}
