package com.yasuda.derbis.feature.discord

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the Discord OAuth & User Token session encrypted at rest with an Android Keystore key.
 *
 * Supports both normal login (OAuth PKCE) and token login with profile metadata.
 */
internal data class DerbisDiscordSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val tokenType: Int = 0,
    val userToken: String = "",
    val username: String = "",
    val globalName: String = "",
    val avatarUrl: String = ""
) {
    val effectiveToken: String
        get() = userToken.ifBlank { accessToken }

    val hasValidToken: Boolean
        get() = effectiveToken.isNotBlank()
}

internal object DerbisDiscordAccountStore {
    private const val PREFS = "derbis_discord_account"
    private const val SESSION = "encrypted_session_v2"
    private const val SESSION_LEGACY = "encrypted_session_v1"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "derbis_discord_account_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun read(context: Context): DerbisDiscordSession? = runCatching {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val encoded = prefs.getString(SESSION, null) ?: prefs.getString(SESSION_LEGACY, null).orEmpty()
        if (encoded.isBlank()) return null
        val separator = encoded.indexOf(':')
        if (separator <= 0 || separator == encoded.lastIndex) throw IllegalStateException("Invalid Discord session")
        val iv = Base64.decode(encoded.substring(0, separator), Base64.NO_WRAP)
        val ciphertext = Base64.decode(encoded.substring(separator + 1), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        }
        val json = JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
        DerbisDiscordSession(
            accessToken = json.optString("access_token"),
            refreshToken = json.optString("refresh_token"),
            tokenType = json.optInt("token_type", 0),
            userToken = json.optString("user_token"),
            username = json.optString("username"),
            globalName = json.optString("global_name"),
            avatarUrl = json.optString("avatar_url")
        ).takeIf { it.hasValidToken }
            ?: throw IllegalStateException("Incomplete Discord session")
    }.getOrElse {
        clear(context)
        null
    }

    fun save(context: Context, session: DerbisDiscordSession) {
        if (!session.hasValidToken) return
        val plaintext = JSONObject().apply {
            put("access_token", session.accessToken)
            put("refresh_token", session.refreshToken)
            put("token_type", session.tokenType)
            put("user_token", session.userToken)
            put("username", session.username)
            put("global_name", session.globalName)
            put("avatar_url", session.avatarUrl)
        }.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key())
        }
        val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(cipher.doFinal(plaintext), Base64.NO_WRAP)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(SESSION, encoded)
            .apply()
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(SESSION)
            .remove(SESSION_LEGACY)
            .apply()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }
}
