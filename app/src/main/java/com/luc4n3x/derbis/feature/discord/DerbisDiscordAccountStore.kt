package com.luc4n3x.derbis.feature.discord

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
 * Keeps the Discord OAuth session encrypted at rest with an Android Keystore key.
 *
 * A Discord client secret never belongs in an installed application. The Social SDK public-client
 * PKCE flow returns short-lived user and refresh tokens instead; only this encrypted local record
 * is retained so a user stays linked between launches.
 */
internal data class DerbisDiscordSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: Int
)

internal object DerbisDiscordAccountStore {
    private const val PREFS = "derbis_discord_account"
    private const val SESSION = "encrypted_session_v1"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "derbis_discord_account_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun read(context: Context): DerbisDiscordSession? = runCatching {
        val encoded = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(SESSION, null)
            .orEmpty()
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
            tokenType = json.optInt("token_type", 0)
        ).takeIf { it.accessToken.isNotBlank() && it.refreshToken.isNotBlank() }
            ?: throw IllegalStateException("Incomplete Discord session")
    }.getOrElse {
        // Android backups do not include Keystore keys. A restored or corrupted encrypted record
        // cannot be safely reused, so require a fresh explicit sign-in rather than retaining junk.
        clear(context)
        null
    }

    fun save(context: Context, session: DerbisDiscordSession) {
        if (session.accessToken.isBlank() || session.refreshToken.isBlank()) return
        val plaintext = JSONObject().apply {
            put("access_token", session.accessToken)
            put("refresh_token", session.refreshToken)
            put("token_type", session.tokenType)
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
