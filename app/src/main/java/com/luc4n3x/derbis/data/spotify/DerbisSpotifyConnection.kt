package com.luc4n3x.derbis.data.spotify

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.luc4n3x.derbis.BuildConfig
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

enum class SpotifyConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Unavailable,
    Error
}

data class SpotifyConnectionState(
    val status: SpotifyConnectionStatus = SpotifyConnectionStatus.Disconnected,
    val displayName: String = "",
    val detail: String = ""
) {
    val isConnected: Boolean get() = status == SpotifyConnectionStatus.Connected
}

/**
 * Official Spotify Authorization Code + PKCE connection. No client secret, Spotify password, or
 * browser cookie is requested or stored. Tokens and the short-lived PKCE verifier are encrypted
 * with an Android Keystore AES key before they reach disk.
 */
object DerbisSpotifyConnection {
    private const val AUTHORIZE_URL = "https://accounts.spotify.com/authorize"
    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    private const val PROFILE_URL = "https://api.spotify.com/v1/me"
    private const val TOKEN_SKEW_MS = 30_000L
    private val AUTH_SCOPES = listOf(
        "user-read-private",
        "user-read-email",
        "playlist-read-private",
        "playlist-read-collaborative"
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient()
    private val tokenMutex = Mutex()
    private val _state = MutableStateFlow(SpotifyConnectionState())
    val state: StateFlow<SpotifyConnectionState> = _state.asStateFlow()

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            if (!isConfigured()) {
                _state.value = SpotifyConnectionState(
                    status = SpotifyConnectionStatus.Unavailable,
                    detail = "Spotify connection has not been configured for this build."
                )
                return
            }
            val token = SpotifySecureStore(context.applicationContext).loadToken()
            _state.value = token?.let {
                SpotifyConnectionState(SpotifyConnectionStatus.Connected, it.displayName)
            } ?: SpotifyConnectionState()
        }
    }

    fun begin(context: Context): Uri? {
        initialize(context)
        if (!isConfigured()) return null
        val verifier = randomUrlSafeValue(64)
        val state = randomUrlSafeValue(32)
        SpotifySecureStore(context.applicationContext).savePending(PendingAuthorization(state, verifier))
        _state.value = SpotifyConnectionState(SpotifyConnectionStatus.Connecting, detail = "Continue in Spotify to connect your account.")
        return AUTHORIZE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("client_id", BuildConfig.SPOTIFY_CLIENT_ID)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("redirect_uri", BuildConfig.SPOTIFY_REDIRECT_URI)
            .addQueryParameter("state", state)
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("code_challenge", codeChallenge(verifier))
            .addQueryParameter("scope", AUTH_SCOPES.joinToString(" "))
            .build()
            .toString()
            .let(Uri::parse)
    }

    fun receives(uri: Uri?): Boolean = uri != null &&
        uri.scheme.equals(Uri.parse(BuildConfig.SPOTIFY_REDIRECT_URI).scheme, ignoreCase = true) &&
        uri.host.equals(Uri.parse(BuildConfig.SPOTIFY_REDIRECT_URI).host, ignoreCase = true)

    fun handleRedirect(context: Context, uri: Uri?) {
        if (!receives(uri)) return
        initialize(context)
        val appContext = context.applicationContext
        val store = SpotifySecureStore(appContext)
        val error = uri?.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            store.clearPending()
            _state.value = SpotifyConnectionState(SpotifyConnectionStatus.Error, detail = "Spotify sign-in was cancelled or denied: $error")
            return
        }
        val code = uri?.getQueryParameter("code")
        val returnedState = uri?.getQueryParameter("state")
        val pending = store.takePending()
        if (code.isNullOrBlank() || pending == null || returnedState != pending.state) {
            _state.value = SpotifyConnectionState(SpotifyConnectionStatus.Error, detail = "Spotify sign-in could not be verified. Please try again.")
            return
        }
        _state.value = SpotifyConnectionState(SpotifyConnectionStatus.Connecting, detail = "Finishing Spotify sign-in…")
        scope.launch {
            val result = runCatching { exchangeCode(code, pending.verifier) }
            result.onSuccess { token ->
                store.saveToken(token)
                _state.value = SpotifyConnectionState(SpotifyConnectionStatus.Connected, token.displayName)
            }.onFailure { error ->
                Timber.w(error, "Spotify PKCE sign-in failed")
                _state.value = SpotifyConnectionState(
                    SpotifyConnectionStatus.Error,
                    detail = "Spotify could not complete this connection. Check the app’s Spotify access and try again."
                )
            }
        }
    }

    fun disconnect(context: Context) {
        SpotifySecureStore(context.applicationContext).clear()
        _state.value = SpotifyConnectionState()
    }

    /**
     * Returns a currently valid user token for an official Spotify Web API request. The token is
     * never exposed to Compose or persisted in plaintext; an expired token is refreshed while
     * holding a mutex so concurrent playlist requests cannot race a refresh.
     */
    suspend fun accessToken(context: Context): String? = withContext(Dispatchers.IO) {
        initialize(context)
        if (!isConfigured()) return@withContext null
        val appContext = context.applicationContext
        val store = SpotifySecureStore(appContext)
        tokenMutex.withLock {
            val current = store.loadToken() ?: return@withLock null
            if (current.expiresAtMs > System.currentTimeMillis()) return@withLock current.accessToken
            runCatching { refreshToken(current) }
                .onSuccess { refreshed ->
                    store.saveToken(refreshed)
                    _state.value = SpotifyConnectionState(SpotifyConnectionStatus.Connected, refreshed.displayName)
                }
                .onFailure { error ->
                    Timber.w(error, "Spotify token refresh failed")
                    store.clear()
                    _state.value = SpotifyConnectionState(
                        SpotifyConnectionStatus.Error,
                        detail = "Spotify sign-in expired. Connect your account again to search playlists."
                    )
                }
                .getOrNull()
                ?.accessToken
        }
    }

    /** Invalidates an access token rejected by Spotify before any playlist data is retained. */
    fun invalidateAccess(context: Context) {
        SpotifySecureStore(context.applicationContext).clear()
        _state.value = SpotifyConnectionState(
            SpotifyConnectionStatus.Error,
            detail = "Spotify sign-in was no longer accepted. Connect your account again."
        )
    }

    private suspend fun exchangeCode(code: String, verifier: String): SpotifyToken = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("client_id", BuildConfig.SPOTIFY_CLIENT_ID)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", BuildConfig.SPOTIFY_REDIRECT_URI)
            .add("code_verifier", verifier)
            .build()
        val request = Request.Builder().url(TOKEN_URL).post(body).build()
        val tokenJson = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Spotify token exchange failed (${response.code})")
            JSONObject(response.body.string())
        }
        val accessToken = tokenJson.optString("access_token").trim()
        val refreshToken = tokenJson.optString("refresh_token").trim()
        if (accessToken.isBlank() || refreshToken.isBlank()) error("Spotify did not return a refreshable token")
        val expiresInSeconds = tokenJson.optLong("expires_in", 3_600L).coerceAtLeast(60L)
        val profile = loadProfile(accessToken)
        SpotifyToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMs = System.currentTimeMillis() + expiresInSeconds * 1_000L - TOKEN_SKEW_MS,
            displayName = profile
        )
    }

    private suspend fun refreshToken(current: SpotifyToken): SpotifyToken = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("client_id", BuildConfig.SPOTIFY_CLIENT_ID)
            .add("grant_type", "refresh_token")
            .add("refresh_token", current.refreshToken)
            .build()
        val tokenJson = httpClient.newCall(Request.Builder().url(TOKEN_URL).post(body).build()).execute().use { response ->
            if (!response.isSuccessful) error("Spotify token refresh failed (${response.code})")
            JSONObject(response.body.string())
        }
        val accessToken = tokenJson.optString("access_token").trim()
        if (accessToken.isBlank()) error("Spotify did not return a refreshed access token")
        val refreshToken = tokenJson.optString("refresh_token").trim().ifBlank { current.refreshToken }
        val expiresInSeconds = tokenJson.optLong("expires_in", 3_600L).coerceAtLeast(60L)
        current.copy(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMs = System.currentTimeMillis() + expiresInSeconds * 1_000L - TOKEN_SKEW_MS
        )
    }

    private fun loadProfile(accessToken: String): String {
        val request = Request.Builder().url(PROFILE_URL)
            .header("Authorization", "Bearer $accessToken")
            .build()
        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use ""
                JSONObject(response.body.string()).optString("display_name").trim()
            }
        }.getOrDefault("")
    }

    private fun isConfigured(): Boolean =
        BuildConfig.SPOTIFY_CLIENT_ID.isNotBlank() && BuildConfig.SPOTIFY_REDIRECT_URI.isNotBlank()

    private fun randomUrlSafeValue(bytes: Int): String {
        val raw = ByteArray(bytes)
        SecureRandom().nextBytes(raw)
        return Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun codeChallenge(verifier: String): String = Base64.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.US_ASCII)),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    )
}

private data class PendingAuthorization(val state: String, val verifier: String)

private data class SpotifyToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMs: Long,
    val displayName: String
)

private class SpotifySecureStore(context: Context) {
    private val preferences = context.getSharedPreferences("derbis_spotify_connection", Context.MODE_PRIVATE)

    fun savePending(value: PendingAuthorization) = save(
        PENDING_KEY,
        JSONObject().put("state", value.state).put("verifier", value.verifier).toString()
    )

    fun takePending(): PendingAuthorization? {
        val value = load(PENDING_KEY)
        clearPending()
        return value?.let {
            runCatching {
                JSONObject(it).let { json ->
                    PendingAuthorization(json.getString("state"), json.getString("verifier"))
                }
            }.getOrNull()
        }
    }

    fun clearPending() {
        preferences.edit().remove(PENDING_KEY).apply()
    }

    fun saveToken(value: SpotifyToken) = save(
        TOKEN_KEY,
        JSONObject()
            .put("accessToken", value.accessToken)
            .put("refreshToken", value.refreshToken)
            .put("expiresAtMs", value.expiresAtMs)
            .put("displayName", value.displayName)
            .toString()
    )

    fun loadToken(): SpotifyToken? = load(TOKEN_KEY)?.let {
        runCatching {
            JSONObject(it).let { json ->
                SpotifyToken(
                    accessToken = json.getString("accessToken"),
                    refreshToken = json.getString("refreshToken"),
                    expiresAtMs = json.getLong("expiresAtMs"),
                    displayName = json.optString("displayName")
                )
            }
        }.getOrNull()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun save(name: String, plainText: String) {
        val cipher = cipher(Cipher.ENCRYPT_MODE)
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val joined = cipher.iv + encrypted
        preferences.edit().putString(name, Base64.encodeToString(joined, Base64.NO_WRAP)).apply()
    }

    private fun load(name: String): String? {
        val encoded = preferences.getString(name, null) ?: return null
        return runCatching {
            val joined = Base64.decode(encoded, Base64.NO_WRAP)
            require(joined.size > IV_LENGTH) { "Invalid encrypted Spotify value" }
            val iv = joined.copyOfRange(0, IV_LENGTH)
            val cipherText = joined.copyOfRange(IV_LENGTH, joined.size)
            String(cipher(Cipher.DECRYPT_MODE, iv).doFinal(cipherText), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    private fun cipher(mode: Int, iv: ByteArray? = null): Cipher = Cipher.getInstance(TRANSFORMATION).apply {
        if (iv == null) init(mode, key()) else init(mode, key(), javax.crypto.spec.GCMParameterSpec(128, iv))
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "derbis_spotify_oauth_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val PENDING_KEY = "pending"
        const val TOKEN_KEY = "token"
    }
}
