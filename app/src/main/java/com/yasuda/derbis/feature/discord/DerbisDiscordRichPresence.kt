package com.yasuda.derbis.feature.discord

import android.app.Activity
import android.content.Context
import com.yasuda.derbis.BuildConfig
import com.yasuda.derbis.domain.Track
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
 * Discord Rich Presence engine for Derbis.
 *
 * Supports both:
 *  1. OuterTune / KizzyRPC direct Discord Gateway WebSocket & REST client
 *     (with clickable buttons, progress bar, dual artwork/artist icon assets, and connection tester)
 *  2. Optional Android Discord Social SDK native bridge when available.
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
    val detail: String = "",
    val username: String = "",
    val globalName: String = "",
    val avatarUrl: String = "",
    val latencyMs: Long = 0L
) {
    val isConnected: Boolean get() = status == DerbisDiscordConnectionStatus.Connected
}

/** A rate-safe snapshot for one Discord Rich Presence activity update. */
internal data class DerbisDiscordPresenceSnapshot(
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val artistId: String = "",
    val artworkUrl: String = "",
    val artistThumbnailUrl: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val songUrl: String = "",
    val artistUrl: String = "",
    val playing: Boolean = false
) {
    /** Re-publish on a track/state change and periodically enough to recover from a seek. */
    val syncKey: String
        get() = listOf(
            trackId,
            title,
            artist,
            artistId,
            artworkUrl,
            artistThumbnailUrl,
            durationMs.toString(),
            playing.toString(),
            if (playing) (positionMs.coerceAtLeast(0L) / DISCORD_POSITION_RESYNC_MS).toString() else ""
        ).joinToString(separator = "\u0000")
}

private const val DISCORD_PRESENCE_TEXT_LIMIT = 128
private const val DISCORD_POSITION_RESYNC_MS = 30_000L

/**
 * Creates a rate-safe presence snapshot from active playback metadata.
 */
internal fun derbisDiscordPresenceSnapshot(
    track: Track?,
    positionMs: Long,
    playing: Boolean
): DerbisDiscordPresenceSnapshot = derbisDiscordPresenceSnapshot(
    track = track,
    positionMs = positionMs,
    durationMs = 0L,
    playing = playing
)

internal fun derbisDiscordPresenceSnapshot(
    track: Track?,
    positionMs: Long,
    durationMs: Long,
    playing: Boolean
): DerbisDiscordPresenceSnapshot {
    val title = track?.title.orEmpty().discordPresenceText()
    val artist = track?.artist.orEmpty().discordPresenceText()
    val artistId = track?.artistBrowseIds?.firstOrNull().orEmpty()
    val artwork = sequenceOf(track?.largeThumbnailUrl.orEmpty(), track?.thumbnailUrl.orEmpty())
        .map(String::discordExternalArtworkUrl)
        .firstOrNull(String::isNotBlank)
        .orEmpty()
    val effectiveDuration = if (durationMs > 0L) durationMs else (track?.durationMs ?: 0L)
    val songUrl = if (track?.id?.isNotBlank() == true) "https://derbis-web.vercel.app/t/${track.id}" else ""
    val artistUrl = if (artistId.isNotBlank()) "https://music.youtube.com/channel/$artistId" else ""

    return DerbisDiscordPresenceSnapshot(
        trackId = track?.id.orEmpty(),
        title = title,
        artist = artist,
        artistId = artistId,
        artworkUrl = artwork,
        artistThumbnailUrl = artwork,
        positionMs = positionMs.coerceAtLeast(0L),
        durationMs = effectiveDuration.coerceAtLeast(0L),
        songUrl = songUrl,
        artistUrl = artistUrl,
        playing = playing && title.isNotBlank()
    )
}

private fun String.discordPresenceText(): String =
    trim().replace(Regex("\\s+"), " ").take(DISCORD_PRESENCE_TEXT_LIMIT)

private fun String.discordExternalArtworkUrl(): String {
    val candidate = trim()
    return candidate.takeIf {
        it.startsWith("https://", ignoreCase = true) && it.length <= 2_048
    }.orEmpty()
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
        DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
    )

    val connectionState: StateFlow<DerbisDiscordConnectionState> = _connectionState.asStateFlow()

    @Volatile private var libraryLoaded = false
    @Volatile private var loadAttempted = false
    @Volatile private var initialized = false
    @Volatile private var restoreAttempted = false
    @Volatile private var lastPresence = DerbisDiscordPresenceSnapshot()
    @Volatile private var lastPublishedPresenceKey = ""
    @Volatile private var nativePresenceActive = false
    @Volatile private var applicationContext: Context? = null
    private var statePollingJob: Job? = null

    fun isAvailable(): Boolean = true

    fun attach(activity: Activity) {
        applicationContext = activity.applicationContext
        if (BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE && applicationId > 0L) {
            runCatching {
                Class.forName("com.discord.socialsdk.DiscordSocialSdkInit")
                    .getMethod("setEngineActivity", Activity::class.java)
                    .invoke(null, activity)
            }.onFailure {
                Timber.w(it, "Discord Social SDK activity attachment failed")
            }
        }
        initialize(activity.applicationContext)
    }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        val savedSession = DerbisDiscordAccountStore.read(context)
        if (savedSession != null && savedSession.hasValidToken) {
            _connectionState.value = DerbisDiscordConnectionState(
                status = DerbisDiscordConnectionStatus.Connected,
                detail = "Connected",
                username = savedSession.username,
                globalName = savedSession.globalName,
                avatarUrl = savedSession.avatarUrl
            )
            DiscordRpcClient.connect(savedSession.effectiveToken)
        } else {
            if (_connectionState.value.status != DerbisDiscordConnectionStatus.SigningIn) {
                _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
            }
        }

        // Try native bridge if configured
        if (BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE && applicationId > 0L && loadBridge()) {
            if (!initialized) {
                initialized = nativeInitialize(applicationId)
            }
            if (savedSession != null && savedSession.refreshToken.isNotBlank() && !restoreAttempted) {
                restoreAttempted = true
                nativeRestoreSession(savedSession.refreshToken)
            }
            ensureStatePolling()
        }
    }

    /**
     * Token login (OuterTune pattern): directly verifies and connects using user token.
     */
    fun loginWithToken(context: Context, token: String, onResult: (DiscordDiagnosticResult) -> Unit) {
        applicationContext = context.applicationContext
        _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connecting, "Verifying token...")
        bridgeScope.launch {
            val diagnostic = DiscordRpcClient.testConnection(token)
            if (diagnostic.success) {
                val session = DerbisDiscordSession(
                    userToken = token.trim(),
                    username = diagnostic.username,
                    globalName = diagnostic.globalName,
                    avatarUrl = diagnostic.avatarUrl
                )
                DerbisDiscordAccountStore.save(context, session)
                _connectionState.value = DerbisDiscordConnectionState(
                    status = DerbisDiscordConnectionStatus.Connected,
                    detail = "Connected as @${diagnostic.username}",
                    username = diagnostic.username,
                    globalName = diagnostic.globalName,
                    avatarUrl = diagnostic.avatarUrl,
                    latencyMs = diagnostic.latencyMs
                )
                setEnabled(context, true)
                DiscordRpcClient.connect(token.trim())
                publishStored(context)
            } else {
                _connectionState.value = DerbisDiscordConnectionState(
                    status = DerbisDiscordConnectionStatus.Error,
                    detail = diagnostic.message
                )
            }
            onResult(diagnostic)
        }
    }

    /**
     * Connection tester: runs 4-phase diagnostics to troubleshoot any connection issues.
     */
    fun testConnection(context: Context, onResult: (DiscordDiagnosticResult) -> Unit) {
        val session = DerbisDiscordAccountStore.read(context)
        val token = session?.effectiveToken.orEmpty()
        bridgeScope.launch {
            val diagnostic = DiscordRpcClient.testConnection(token)
            if (diagnostic.success) {
                _connectionState.value = _connectionState.value.copy(
                    username = diagnostic.username,
                    globalName = diagnostic.globalName,
                    avatarUrl = diagnostic.avatarUrl,
                    latencyMs = diagnostic.latencyMs
                )
            }
            onResult(diagnostic)
        }
    }

    fun beginSignIn(context: Context) {
        initialize(context)
        if (loadBridge() && nativeBeginSignIn()) {
            _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.SigningIn)
            ensureStatePolling()
        } else {
            // Provide feedback for manual token or web flow
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Disconnected,
                "Enter your Discord token or log in below"
            )
        }
    }

    fun disconnect(context: Context) {
        setEnabled(context, false)
        DerbisDiscordAccountStore.clear(context)
        DiscordRpcClient.disconnect()
        if (loadBridge()) nativeDisconnect()
        restoreAttempted = false
        _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
    }

    fun isEnabled(context: Context): Boolean =
        connectionState.value.isConnected &&
            DerbisDiscordAccountStore.read(context) != null &&
            context.applicationContext
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(ENABLED_KEY, true) // Enabled by default once connected

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

    internal fun publish(context: Context, presence: DerbisDiscordPresenceSnapshot) {
        applicationContext = context.applicationContext
        lastPresence = presence
        if (!presence.playing || !isEnabled(context)) {
            clear()
            return
        }

        // Publish via WebSocket Gateway RPC (clickable buttons, progress bar, dual artwork/artist icon)
        DiscordRpcClient.updatePresence(presence)

        // Publish via native bridge if loaded
        if (loadBridge()) {
            publishNative(presence)
        }
    }

    fun clear() {
        DiscordRpcClient.clearPresence()
        if (nativePresenceActive && loadBridge()) nativeClear()
        nativePresenceActive = false
        lastPublishedPresenceKey = ""
    }

    fun release() {
        statePollingJob?.cancel()
        statePollingJob = null
        clear()
        DiscordRpcClient.disconnect()
        if (loadBridge()) nativeRelease()
        initialized = false
        restoreAttempted = false
    }

    private fun publishStored(context: Context) {
        if (!lastPresence.playing || !isEnabled(context)) return
        DiscordRpcClient.updatePresence(lastPresence)
        if (loadBridge()) {
            publishNative(lastPresence, force = true)
        }
    }

    private fun publishNative(presence: DerbisDiscordPresenceSnapshot, force: Boolean = false) {
        if (!force && nativePresenceActive && lastPublishedPresenceKey == presence.syncKey) return
        val startedAtEpochSeconds = (
            System.currentTimeMillis() - presence.positionMs.coerceAtLeast(0L)
        ).coerceAtLeast(0L) / 1_000L
        nativeUpdate(
            title = presence.title,
            artist = presence.artist,
            artworkUrl = presence.artworkUrl,
            startedAtEpochSeconds = startedAtEpochSeconds,
            playing = presence.playing
        )
        nativePresenceActive = true
        lastPublishedPresenceKey = presence.syncKey
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
        when (nativeStatus) {
            NATIVE_DISCONNECTED -> if (!DiscordRpcClient.isConnected) {
                _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
            }
            NATIVE_SIGNING_IN -> _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.SigningIn)
            NATIVE_CONNECTING -> _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connecting)
            NATIVE_CONNECTED -> {
                val accessToken = nativeTakeAccessToken()
                val refreshToken = nativeTakeRefreshToken()
                if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                    DerbisDiscordAccountStore.save(
                        context,
                        DerbisDiscordSession(accessToken, refreshToken, nativeTakeTokenType())
                    )
                    DiscordRpcClient.connect(accessToken)
                }
                _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connected, "Connected")
            }
            NATIVE_ERROR -> {
                if (!DiscordRpcClient.isConnected) {
                    _connectionState.value = DerbisDiscordConnectionState(
                        DerbisDiscordConnectionStatus.Error,
                        nativeConnectionError().ifBlank { "Discord connection failed" }
                    )
                }
            }
            else -> {}
        }
        if (previous != DerbisDiscordConnectionStatus.Connected && _connectionState.value.isConnected && isEnabled(context)) {
            publishStored(context)
        }
    }

    private fun loadBridge(): Boolean {
        if (!BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE) return false
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
    private external fun nativeUpdate(
        title: String,
        artist: String,
        artworkUrl: String,
        startedAtEpochSeconds: Long,
        playing: Boolean
    )
    private external fun nativeClear()
    private external fun nativeRelease()
}
