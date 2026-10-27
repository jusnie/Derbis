package com.luc4n3x.derbis.feature.discord

import android.app.Activity
import android.content.Context
import com.luc4n3x.derbis.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Optional Android Discord Social SDK bridge.
 *
 * Rich Presence is published only after the listener has explicitly linked their Discord account
 * through Discord's mobile PKCE flow and enabled sharing. The Social SDK AAR remains a proprietary
 * release-only dependency and is never added to source control.
 */
enum class DerbisDiscordConnectionStatus {
    Unavailable,
    Disconnected,
    SigningIn,
    Connecting,
    Connected,
    Error
}

data class DerbisDiscordConnectionState(
    val status: DerbisDiscordConnectionStatus,
    val detail: String = ""
) {
    val isConnected: Boolean get() = status == DerbisDiscordConnectionStatus.Connected
}

object DerbisDiscordRichPresence {
    private const val PREFERENCES = "derbis_discord_presence"
    private const val ENABLED_KEY = "enabled"
    private const val NATIVE_UNAVAILABLE = 0
    private const val NATIVE_DISCONNECTED = 1
    private const val NATIVE_SIGNING_IN = 2
    private const val NATIVE_CONNECTING = 3
    private const val NATIVE_CONNECTED = 4
    private const val NATIVE_ERROR = 5

    private val applicationId: Long = BuildConfig.DISCORD_APPLICATION_ID.toLongOrNull() ?: 0L
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _connectionState = MutableStateFlow(
        DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Unavailable)
    )

    val connectionState: StateFlow<DerbisDiscordConnectionState> = _connectionState.asStateFlow()

    @Volatile private var libraryLoaded = false
    @Volatile private var loadAttempted = false
    @Volatile private var initialized = false
    @Volatile private var restoreAttempted = false
    @Volatile private var lastTitle: String = ""
    @Volatile private var lastArtist: String = ""
    @Volatile private var lastPlaying: Boolean = false
    @Volatile private var applicationContext: Context? = null
    private var statePollingJob: Job? = null

    fun isAvailable(): Boolean = BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE && applicationId > 0L && loadBridge()

    fun attach(activity: Activity) {
        applicationContext = activity.applicationContext
        if (!isAvailable()) {
            _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Unavailable)
            return
        }
        // Required by Discord's Android authentication activity. Reflection keeps source-only and
        // F-Droid builds free of the proprietary Java dependency.
        val attached = runCatching {
            Class.forName("com.discord.socialsdk.DiscordSocialSdkInit")
                .getMethod("setEngineActivity", Activity::class.java)
                .invoke(null, activity)
        }.onFailure {
            Timber.w(it, "Discord Social SDK activity attachment failed")
        }.isSuccess
        if (!attached) {
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Error,
                "Discord sign-in is unavailable on this device"
            )
            return
        }
        initialize(activity.applicationContext)
    }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        if (!isAvailable()) {
            _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Unavailable)
            return
        }
        if (!initialized) {
            initialized = nativeInitialize(applicationId)
            if (!initialized) {
                _connectionState.value = DerbisDiscordConnectionState(
                    DerbisDiscordConnectionStatus.Error,
                    "Discord could not start"
                )
                return
            }
        }
        val savedSession = DerbisDiscordAccountStore.read(context)
        if (savedSession == null) {
            if (_connectionState.value.status != DerbisDiscordConnectionStatus.SigningIn) {
                _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
            }
        } else if (!restoreAttempted) {
            restoreAttempted = true
            if (!nativeRestoreSession(savedSession.refreshToken)) {
                _connectionState.value = DerbisDiscordConnectionState(
                    DerbisDiscordConnectionStatus.Error,
                    "Discord could not restore the saved sign-in"
                )
            }
        }
        ensureStatePolling()
        synchronizeNativeState(context.applicationContext)
    }

    fun beginSignIn(context: Context) {
        initialize(context)
        if (!isAvailable() || !initialized) return
        if (!nativeBeginSignIn()) {
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Error,
                nativeConnectionError().ifBlank { "Discord sign-in could not start" }
            )
            return
        }
        _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.SigningIn)
        ensureStatePolling()
    }

    fun disconnect(context: Context) {
        setEnabled(context, false)
        DerbisDiscordAccountStore.clear(context)
        if (loadBridge()) nativeDisconnect()
        restoreAttempted = false
        _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
    }

    fun isEnabled(context: Context): Boolean =
        isAvailable() && connectionState.value.isConnected &&
            DerbisDiscordAccountStore.read(context) != null &&
            context.applicationContext
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(ENABLED_KEY, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        val allowed = enabled && connectionState.value.isConnected &&
            DerbisDiscordAccountStore.read(context) != null
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ENABLED_KEY, allowed)
            .apply()
        if (allowed) publishStored(context) else clear()
    }

    fun publish(context: Context, title: String?, artist: String?, playing: Boolean) {
        applicationContext = context.applicationContext
        lastTitle = title.orEmpty().trim()
        lastArtist = artist.orEmpty().trim()
        lastPlaying = playing
        if (!isEnabled(context)) {
            clear()
            return
        }
        nativeUpdate(lastTitle, lastArtist, lastPlaying)
    }

    fun clear() {
        if (loadBridge()) nativeClear()
    }

    fun release() {
        statePollingJob?.cancel()
        statePollingJob = null
        if (loadBridge()) nativeRelease()
        initialized = false
        restoreAttempted = false
    }

    private fun publishStored(context: Context) {
        if (!isEnabled(context) || !loadBridge()) return
        nativeUpdate(lastTitle, lastArtist, lastPlaying)
    }

    private fun ensureStatePolling() {
        if (statePollingJob?.isActive == true) return
        statePollingJob = bridgeScope.launch {
            while (true) {
                applicationContext?.let(::synchronizeNativeState)
                delay(750L)
            }
        }
    }

    private fun synchronizeNativeState(context: Context) {
        if (!initialized || !loadBridge()) return
        val previous = _connectionState.value.status
        val nativeStatus = nativeConnectionState()
        val state = when (nativeStatus) {
            NATIVE_DISCONNECTED -> DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
            NATIVE_SIGNING_IN -> DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.SigningIn)
            NATIVE_CONNECTING -> DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connecting)
            NATIVE_CONNECTED -> {
                val accessToken = nativeTakeAccessToken()
                val refreshToken = nativeTakeRefreshToken()
                if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                    DerbisDiscordAccountStore.save(
                        context,
                        DerbisDiscordSession(accessToken, refreshToken, nativeTakeTokenType())
                    )
                }
                if (DerbisDiscordAccountStore.read(context) == null) {
                    DerbisDiscordConnectionState(
                        DerbisDiscordConnectionStatus.Error,
                        "Discord did not return a secure session"
                    )
                } else {
                    DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connected, "Connected")
                }
            }
            NATIVE_ERROR -> DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Error,
                nativeConnectionError().ifBlank { "Discord connection failed" }
            )
            NATIVE_UNAVAILABLE -> DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Unavailable)
            else -> DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Error, "Discord returned an unknown state")
        }
        _connectionState.value = state
        if (previous != DerbisDiscordConnectionStatus.Connected && state.isConnected && isEnabled(context)) {
            publishStored(context)
        }
    }

    private fun loadBridge(): Boolean {
        if (loadAttempted) return libraryLoaded
        synchronized(this) {
            if (loadAttempted) return libraryLoaded
            loadAttempted = true
            libraryLoaded = runCatching {
                System.loadLibrary("derbis_discord_bridge")
                true
            }.onFailure { Timber.w(it, "Discord Rich Presence bridge unavailable") }.getOrDefault(false)
            return libraryLoaded
        }
    }

    private external fun nativeInitialize(applicationId: Long): Boolean
    private external fun nativeBeginSignIn(): Boolean
    private external fun nativeRestoreSession(refreshToken: String): Boolean
    private external fun nativeDisconnect()
    private external fun nativeConnectionState(): Int
    private external fun nativeConnectionError(): String
    private external fun nativeTakeAccessToken(): String
    private external fun nativeTakeRefreshToken(): String
    private external fun nativeTakeTokenType(): Int
    private external fun nativeUpdate(title: String, artist: String, playing: Boolean)
    private external fun nativeClear()
    private external fun nativeRelease()
}
