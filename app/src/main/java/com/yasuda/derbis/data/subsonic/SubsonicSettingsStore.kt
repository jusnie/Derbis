package com.yasuda.derbis.data.subsonic

import android.content.Context
import com.yasuda.derbis.data.security.AndroidKeystoreCredentialStore

/**
 * Where a server address lives, and where its password does not.
 *
 * The address, the username and the on/off switch are ordinary settings in their own small
 * preferences file, the same way the credential store keeps its own. They are not secret and they
 * are meant to be read on every launch.
 *
 * The password is kept in the Keystore-backed store under its own name, so it never touches the
 * settings file, never reaches an exported backup, and dies with the Keystore key on a reinstall.
 */
class SubsonicSettingsStore(
    context: Context,
    private val credentials: AndroidKeystoreCredentialStore =
        AndroidKeystoreCredentialStore(context.applicationContext)
) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /**
     * The saved server, or null when none is configured — which is the normal state and must never
     * be reported as an error. A half-filled server is treated as none, because a provider that
     * cannot authenticate would only waste a timeout on every search.
     */
    fun load(): SubsonicCredentials? {
        if (!isEnabled()) return null
        val serverUrl = preferences.getString(KEY_SERVER_URL, null).orEmpty().trim()
        val username = preferences.getString(KEY_USERNAME, null).orEmpty().trim()
        if (serverUrl.isBlank() || username.isBlank()) return null
        val password = credentials.read(PASSWORD_KEY).orEmpty()
        if (password.isBlank()) return null
        return SubsonicCredentials(serverUrl = serverUrl, username = username, password = password)
    }

    /** What the settings row shows, without handing the password to the UI. */
    fun summary(): SubsonicServerSummary {
        return SubsonicServerSummary(
            serverUrl = preferences.getString(KEY_SERVER_URL, null).orEmpty(),
            username = preferences.getString(KEY_USERNAME, null).orEmpty(),
            hasPassword = !credentials.read(PASSWORD_KEY).isNullOrBlank(),
            enabled = isEnabled()
        )
    }

    fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, false)

    /**
     * The host the listener typed, for the cast receiver's allow-list.
     *
     * Reads no secret: this is consulted while building a cast transfer, and a server address is
     * not something that needs the Keystore opened to find out.
     */
    fun serverHost(): String? {
        if (!isEnabled()) return null
        val raw = preferences.getString(KEY_SERVER_URL, null).orEmpty()
        return SubsonicClient.normalizeServerUrl(raw)?.host
    }

    fun save(credentials: SubsonicCredentials, enabled: Boolean = true) {
        preferences.edit()
            .putString(KEY_SERVER_URL, credentials.serverUrl.trim())
            .putString(KEY_USERNAME, credentials.username.trim())
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        this.credentials.write(PASSWORD_KEY, credentials.password)
    }

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * Forgets the server. The secret goes first: if only the settings were cleared, a stale
     * password would sit in the Keystore waiting for the next server that reused the slot.
     */
    fun clear() {
        credentials.clear(PASSWORD_KEY)
        preferences.edit()
            .remove(KEY_SERVER_URL)
            .remove(KEY_USERNAME)
            .putBoolean(KEY_ENABLED, false)
            .apply()
    }

    private companion object {
        const val PREFERENCES = "derbis_subsonic"
        const val PASSWORD_KEY = "subsonic_password"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_USERNAME = "username"
        const val KEY_ENABLED = "enabled"
    }
}

/** What the settings row shows without loading the secret. */
data class SubsonicServerSummary(
    val serverUrl: String = "",
    val username: String = "",
    val hasPassword: Boolean = false,
    val enabled: Boolean = false
) {
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && hasPassword

    /** Host only, for a row that has one line to spend. */
    val displayHost: String
        get() = SubsonicClient.normalizeServerUrl(serverUrl)?.host ?: serverUrl
}
