package com.yasuda.derbis.feature.discord

import android.app.Activity
import android.content.Context
import android.os.SystemClock
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
    val playing: Boolean = false,
    /**
     * Current synced lyric line, already sanitized to Discord's 128 character activity limit.
     * Blank whenever the lyrics-in-presence option is off or the track has no synced lyrics.
     */
    val lyricLine: String = ""
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
            if (playing) (positionMs.coerceAtLeast(0L) / DISCORD_POSITION_RESYNC_MS).toString() else "",
            lyricLine
        ).joinToString(separator = "\u0000")

    /** Second activity line: keeps the track visible while the first line shows lyrics. */
    val artistTitleLine: String
        get() = listOf(artist, title)
            .filter(String::isNotBlank)
            .joinToString(separator = " — ")
}

private const val DISCORD_PRESENCE_TEXT_LIMIT = 128
private const val DISCORD_POSITION_RESYNC_MS = 30_000L

/**
 * Discord only accepts five game-status updates per 20 seconds per session
 * (https://discord.com/developers/docs/events/gateway-events#update-presence),
 * so live lyric lines are spaced at least this far apart and bursts are coalesced
 * into a single trailing update instead of being dropped.
 */
internal const val DISCORD_PRESENCE_MIN_INTERVAL_MS = 4_000L

/**
 * Pure rate-gate decision for gateway presence updates.
 *
 * @param nowElapsedMs monotonic clock reading (never wall-clock, so clock jumps cannot stall it).
 * @param lastSentElapsedMs when the previous update went out, or 0 when nothing was sent yet.
 */
internal fun shouldFlushPresenceNow(
    nowElapsedMs: Long,
    lastSentElapsedMs: Long,
    minIntervalMs: Long = DISCORD_PRESENCE_MIN_INTERVAL_MS
): Boolean = lastSentElapsedMs <= 0L || nowElapsedMs - lastSentElapsedMs >= minIntervalMs

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
    playing: Boolean,
    lyricLine: String = ""
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
        playing = playing && title.isNotBlank(),
        lyricLine = if (playing && title.isNotBlank()) lyricLine.discordPresenceText() else ""
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
    private const val LYRICS_KEY = "lyrics_in_presence"
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

    /** Guards the coalescing rate gate below; touched from the player thread and the bridge scope. */
    private val presenceGateLock = Any()
    private var pendingGatewayPresence: DerbisDiscordPresenceSnapshot? = null
    private var pendingFlushJob: Job? = null
    private var lastGatewaySendElapsedMs = 0L

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

        // Only restore native bridge if the user specifically saved a native refresh token
        if (BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE && applicationId > 0L && savedSession != null && savedSession.refreshToken.isNotBlank()) {
            if (loadBridge()) {
                if (!initialized) {
                    initialized = runCatching { nativeInitialize(applicationId) }.getOrDefault(false)
                }
                if (initialized && !restoreAttempted) {
                    restoreAttempted = true
                    runCatching { nativeRestoreSession(savedSession.refreshToken) }
                }
                ensureStatePolling()
            }
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
        if (loadBridge()) {
            if (!initialized) {
                initialized = runCatching { nativeInitialize(applicationId) }.getOrDefault(false)
            }
            if (initialized && runCatching { nativeBeginSignIn() }.getOrDefault(false)) {
                _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.SigningIn)
                ensureStatePolling()
                return
            }
        }
        _connectionState.value = DerbisDiscordConnectionState(
            DerbisDiscordConnectionStatus.Disconnected,
            "Enter your Discord token or log in below"
        )
    }

    fun disconnect(context: Context) {
        setEnabled(context, false)
        DerbisDiscordAccountStore.clear(context)
        DiscordRpcClient.disconnect()
        if (loadBridge()) {
            runCatching { nativeDisconnect() }
        }
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

    /**
     * Lyrics in presence is opt-in: it publishes the exact line being sung, which is more
     * granular than the track title and artist that the presence already shows.
     */
    fun isLyricsEnabled(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(LYRICS_KEY, false)

    fun setLyricsEnabled(context: Context, enabled: Boolean) {
        applicationContext = context.applicationContext
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(LYRICS_KEY, enabled)
            .apply()
        // Drop the line immediately when turning the option off instead of waiting for the
        // next playback tick; turning it on lets the player republish with the current line.
        val current = lastPresence
        if (!enabled && current.lyricLine.isNotBlank()) {
            lastPresence = current.copy(lyricLine = "")
            publishStored(context)
        }
    }

    internal fun publish(context: Context, presence: DerbisDiscordPresenceSnapshot) {
        applicationContext = context.applicationContext
        // The preference is authoritative here so a stale snapshot can never leak lyrics.
        val effective = if (presence.lyricLine.isBlank() || isLyricsEnabled(context)) {
            presence
        } else {
            presence.copy(lyricLine = "")
        }
        lastPresence = effective
        if (!effective.playing || !isEnabled(context)) {
            clear()
            return
        }

        // Publish via WebSocket Gateway RPC (clickable buttons, progress bar, dual artwork/artist icon)
        enqueueGatewayPresence(effective)

        // Publish via native bridge if loaded. The Social SDK activity has no lyric field, so
        // strip it before dedupe to avoid re-sending the same title/artist on every line change.
        if (loadBridge()) {
            publishNative(effective.copy(lyricLine = ""))
        }
    }

    /**
     * Sends a gateway presence update immediately when the 5-per-20-seconds budget allows,
     * otherwise keeps only the newest snapshot and flushes it once the window reopens.
     * Without this a rapidly changing lyric line would trip Discord's presence rate limit.
     */
    private fun enqueueGatewayPresence(presence: DerbisDiscordPresenceSnapshot) {
        val now = SystemClock.elapsedRealtime()
        val sendNow = synchronized(presenceGateLock) {
            val allowed = shouldFlushPresenceNow(now, lastGatewaySendElapsedMs)
            if (allowed) {
                lastGatewaySendElapsedMs = now
                pendingGatewayPresence = null
            } else {
                pendingGatewayPresence = presence
            }
            allowed
        }
        if (sendNow) {
            DiscordRpcClient.updatePresence(presence)
        } else {
            scheduleTrailingGatewayPresence()
        }
    }

    private fun scheduleTrailingGatewayPresence() {
        synchronized(presenceGateLock) {
            if (pendingFlushJob?.isActive == true) return
            val elapsed = SystemClock.elapsedRealtime() - lastGatewaySendElapsedMs
            val waitMs = (DISCORD_PRESENCE_MIN_INTERVAL_MS - elapsed)
                .coerceIn(1L, DISCORD_PRESENCE_MIN_INTERVAL_MS)
            pendingFlushJob = bridgeScope.launch {
                delay(waitMs)
                val latest = synchronized(presenceGateLock) {
                    val pending = pendingGatewayPresence
                    pendingGatewayPresence = null
                    if (pending != null) lastGatewaySendElapsedMs = SystemClock.elapsedRealtime()
                    pending
                }
                if (latest != null) DiscordRpcClient.updatePresence(latest)
            }
        }
    }

    private fun resetGatewayPresenceGate() {
        synchronized(presenceGateLock) {
            pendingFlushJob?.cancel()
            pendingFlushJob = null
            pendingGatewayPresence = null
            lastGatewaySendElapsedMs = 0L
        }
    }

    fun clear() {
        resetGatewayPresenceGate()
        DiscordRpcClient.clearPresence()
        if (nativePresenceActive && loadBridge()) {
            runCatching { nativeClear() }
        }
        nativePresenceActive = false
        lastPublishedPresenceKey = ""
    }

    fun release() {
        statePollingJob?.cancel()
        statePollingJob = null
        clear()
        DiscordRpcClient.disconnect()
        if (loadBridge()) {
            runCatching { nativeRelease() }
        }
        initialized = false
        restoreAttempted = false
    }

    private fun publishStored(context: Context) {
        if (!lastPresence.playing || !isEnabled(context)) return
        enqueueGatewayPresence(lastPresence)
        if (loadBridge()) {
            publishNative(lastPresence.copy(lyricLine = ""), force = true)
        }
    }

    private fun publishNative(presence: DerbisDiscordPresenceSnapshot, force: Boolean = false) {
        if (!force && nativePresenceActive && lastPublishedPresenceKey == presence.syncKey) return
        val startedAtEpochSeconds = (
            System.currentTimeMillis() - presence.positionMs.coerceAtLeast(0L)
        ).coerceAtLeast(0L) / 1_000L
        runCatching {
            nativeUpdate(
                title = presence.title,
                artist = presence.artist,
                artworkUrl = presence.artworkUrl,
                startedAtEpochSeconds = startedAtEpochSeconds,
                playing = presence.playing
            )
            nativePresenceActive = true
            lastPublishedPresenceKey = presence.syncKey
        }.onFailure {
            Timber.w(it, "Discord native presence update failed")
        }
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
        val nativeStatus = runCatching { nativeConnectionState() }.getOrDefault(NATIVE_DISCONNECTED)
        when (nativeStatus) {
            NATIVE_DISCONNECTED -> if (!DiscordRpcClient.isConnected) {
                _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
            }
            NATIVE_SIGNING_IN -> _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.SigningIn)
            NATIVE_CONNECTING -> _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connecting)
            NATIVE_CONNECTED -> {
                val accessToken = runCatching { nativeTakeAccessToken() }.getOrDefault("")
                val refreshToken = runCatching { nativeTakeRefreshToken() }.getOrDefault("")
                val tokenType = runCatching { nativeTakeTokenType() }.getOrDefault(0)
                if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                    DerbisDiscordAccountStore.save(
                        context,
                        DerbisDiscordSession(accessToken, refreshToken, tokenType)
                    )
                    DiscordRpcClient.connect(accessToken)
                }
                _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connected, "Connected")
            }
            NATIVE_ERROR -> {
                if (!DiscordRpcClient.isConnected) {
                    val err = runCatching { nativeConnectionError() }.getOrDefault("Discord connection failed")
                    _connectionState.value = DerbisDiscordConnectionState(
                        DerbisDiscordConnectionStatus.Error,
                        err.ifBlank { "Discord connection failed" }
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
