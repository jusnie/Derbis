package com.yasuda.derbis.data.spotify

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import timber.log.Timber

/**
 * The listener's own Spotify **web** session, i.e. the `sp_dc` cookie.
 *
 * Derbis already has an official OAuth connection ([DerbisSpotifyConnection]) for playlists, but
 * that one needs a client id registered with Spotify and a redirect URI, and it cannot read
 * Canvas: there is no public API for it. The web player's own session can, because it is the same
 * session the web player uses to fetch one. That session is a single cookie, and this class is
 * where it lives — encrypted with an Android Keystore key, exactly like the OAuth tokens, so a
 * backup of the prefs file is worth nothing on another device.
 *
 * Nothing here talks to the network. Minting a token out of the cookie is
 * [com.yasuda.derbis.feature.motion.SpotifyTokenProvider]'s job.
 */
data class SpotifySessionState(
    val hasCookie: Boolean = false,
    val savedAtMs: Long = 0L,
    val lastError: String = ""
) {
    val isConnected: Boolean get() = hasCookie
}

object DerbisSpotifySession {
    private val _state = MutableStateFlow(SpotifySessionState())
    val state: StateFlow<SpotifySessionState> = _state.asStateFlow()

    @Volatile
    private var initialized = false

    @Volatile
    private var cookie: String = ""

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            val stored = runCatching { SpotifyCookieStore(context.applicationContext).load() }
                .onFailure { Timber.w(it, "Could not read the stored Spotify session") }
                .getOrNull()
            cookie = stored?.cookie.orEmpty()
            _state.value = SpotifySessionState(
                hasCookie = cookie.isNotBlank(),
                savedAtMs = stored?.savedAtMs ?: 0L
            )
        }
    }

    /** The raw cookie, or an empty string. Safe to call before [initialize]. */
    fun cookie(): String = cookie

    fun hasSession(): Boolean = cookie.isNotBlank()

    /**
     * Stores whatever the listener handed over — a bare value, or a whole `Cookie:` header pasted
     * out of a browser's dev tools — and returns the state it settled on. Invalid input keeps the
     * previous session and reports why.
     */
    fun save(context: Context, raw: String): SpotifySessionState {
        initialize(context)
        val parsed = SpotifyCookie.parse(raw)
        if (parsed == null) {
            val rejected = _state.value.copy(
                lastError = "That does not look like an sp_dc cookie. Copy the whole value after sp_dc=."
            )
            _state.value = rejected
            return rejected
        }
        val savedAt = System.currentTimeMillis()
        val failure = runCatching { SpotifyCookieStore(context.applicationContext).save(parsed, savedAt) }
            .exceptionOrNull()
        failure?.let { Timber.w(it, "Could not store the Spotify session") }
        cookie = parsed
        // Kept in memory either way, so Canvas still unlocks for this run; say so rather than
        // pretend the cookie will survive a restart.
        val next = SpotifySessionState(
            hasCookie = true,
            savedAtMs = savedAt,
            lastError = if (failure == null) "" else {
                "Session is only active until Derbis restarts — it could not be stored securely."
            }
        )
        _state.value = next
        return next
    }

    fun clear(context: Context) {
        initialize(context)
        runCatching { SpotifyCookieStore(context.applicationContext).clear() }
            .onFailure { Timber.w(it, "Could not clear the Spotify session") }
        cookie = ""
        _state.value = SpotifySessionState()
    }
}

/** Parsing and masking for the one cookie this feature cares about. */
object SpotifyCookie {
    private val SP_DC = Regex("""(?:^|;\s*)sp_dc=([^;]+)""")

    /**
     * Accepts `sp_dc=AQ...`, a whole `Cookie: a=b; sp_dc=AQ...; c=d` header, or the bare value.
     * Returns null when the result cannot be an sp_dc — the web player issues a long base64url
     * token, so a short or oddly punctuated paste is a typo, not a session.
     */
    fun parse(raw: String): String? {
        val trimmed = raw.trim().removeSurrounding("\"").removeSurrounding("'")
        if (trimmed.isEmpty()) return null
        val match = SP_DC.find(trimmed)
        val value = (match?.groupValues?.getOrNull(1) ?: trimmed).trim()
        if (value.length < 40 || value.length > 4096) return null
        if (!value.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }) return null
        return value
    }

    /** `AQAy7j…9fK2` — enough to recognise a session, not enough to use one. */
    fun masked(value: String): String = when {
        value.isEmpty() -> ""
        value.length <= 12 -> "•".repeat(value.length)
        else -> value.take(6) + "…" + value.takeLast(4)
    }
}

private data class StoredSpotifySession(val cookie: String, val savedAtMs: Long)

private class SpotifyCookieStore(context: Context) {
    private val preferences = context.getSharedPreferences("derbis_spotify_session", Context.MODE_PRIVATE)

    fun save(cookie: String, savedAtMs: Long) {
        val payload = JSONObject().put("cookie", cookie).put("savedAtMs", savedAtMs).toString()
        val cipher = cipher(Cipher.ENCRYPT_MODE)
        val encrypted = cipher.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
        val joined = cipher.iv + encrypted
        preferences.edit().putString(SESSION_KEY, Base64.encodeToString(joined, Base64.NO_WRAP)).apply()
    }

    fun load(): StoredSpotifySession? {
        val encoded = preferences.getString(SESSION_KEY, null) ?: return null
        return runCatching {
            val joined = Base64.decode(encoded, Base64.NO_WRAP)
            require(joined.size > IV_LENGTH) { "Invalid encrypted Spotify session" }
            val iv = joined.copyOfRange(0, IV_LENGTH)
            val cipherText = joined.copyOfRange(IV_LENGTH, joined.size)
            val plain = String(cipher(Cipher.DECRYPT_MODE, iv).doFinal(cipherText), StandardCharsets.UTF_8)
            val json = JSONObject(plain)
            StoredSpotifySession(
                cookie = json.getString("cookie"),
                savedAtMs = json.optLong("savedAtMs")
            )
        }.getOrNull()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun cipher(mode: Int, iv: ByteArray? = null): Cipher = Cipher.getInstance(TRANSFORMATION).apply {
        if (iv == null) init(mode, key()) else init(mode, key(), GCMParameterSpec(128, iv))
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
        const val KEY_ALIAS = "derbis_spotify_spdc_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val SESSION_KEY = "session"
    }
}
