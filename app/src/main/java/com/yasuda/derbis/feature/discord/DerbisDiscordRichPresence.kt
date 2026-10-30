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
import kotlinx.coroutines.isActive
import timber.log.Timber

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
            songUrl,
            artistUrl,
            durationMs.toString(),
            playing.toString(),
            if (playing) (positionMs.coerceAtLeast(0L) / DISCORD_POSITION_RESYNC_MS).toString() else ""
        ).joinToString(separator = "\u0000")
}

private const val DISCORD_PRESENCE_TEXT_LIMIT = 128
private const val DISCORD_POSITION_RESYNC_MS = 30_000L
private val DiscordPresenceWhitespace = Regex("\\s+")
private val DiscordVideoId = Regex("[A-Za-z0-9_-]{11}")
private val DiscordArtistId = Regex("UC[A-Za-z0-9_-]{22}")
private const val DiscordSongUrlPrefix = "https://derbis-web.vercel.app/t/"
private const val DiscordArtistUrlPrefix = "https://music.youtube.com/channel/"


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
    val publicVideoId = if (track != null && !track.source.contains("local", ignoreCase = true)) {
        sequenceOf(track.id, track.counterpartVideoId, track.audioVideoId).firstOrNull(DiscordVideoId::matches).orEmpty()
    } else ""
    val songUrl = publicVideoId.takeIf(String::isNotBlank)?.let { "$DiscordSongUrlPrefix$it" }.orEmpty()
    val artistUrl = artistId.takeIf(DiscordArtistId::matches)?.let { "$DiscordArtistUrlPrefix$it" }.orEmpty()

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
    trim().replace(DiscordPresenceWhitespace, " ").take(DISCORD_PRESENCE_TEXT_LIMIT)

private fun String.discordExternalArtworkUrl(): String {
    val candidate = trim()
    return candidate.takeIf {
        it.startsWith("https://", ignoreCase = true) && it.length <= 2_048
    }.orEmpty()
}

internal data class DerbisDiscordActivityPayload(
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val artistThumbnailUrl: String,
    val songUrl: String,
    val artistUrl: String,
    val artworkText: String,
    val startedAtEpochMs: Long,
    val endsAtEpochMs: Long
)

internal fun derbisDiscordActivityPayload(
    presence: DerbisDiscordPresenceSnapshot,
    nowMs: Long
): DerbisDiscordActivityPayload {
    val duration = presence.durationMs.coerceAtLeast(0L)
    val position = presence.positionMs.coerceAtLeast(0L).let { if (duration > 0L) it.coerceAtMost(duration) else it }
    val started = (nowMs.coerceAtLeast(0L) - position).coerceAtLeast(0L)
    val ends = if (started > 0L && duration > 0L && duration <= Long.MAX_VALUE - started) started + duration else 0L
    val title = presence.title.discordPresenceText()
    val artist = presence.artist.discordPresenceText()
    return DerbisDiscordActivityPayload(
        title = title,
        artist = artist,
        artworkUrl = presence.artworkUrl.discordExternalArtworkUrl(),
        artistThumbnailUrl = presence.artistThumbnailUrl.discordExternalArtworkUrl(),
        songUrl = presence.songUrl.takeIf {
            it.startsWith(DiscordSongUrlPrefix) && DiscordVideoId.matches(it.removePrefix(DiscordSongUrlPrefix))
        }.orEmpty(),
        artistUrl = presence.artistUrl.takeIf {
            it.startsWith(DiscordArtistUrlPrefix) && DiscordArtistId.matches(it.removePrefix(DiscordArtistUrlPrefix))
        }.orEmpty(),
        artworkText = listOf(title, artist).filter(String::isNotBlank).joinToString(" — ").take(DISCORD_PRESENCE_TEXT_LIMIT),
        startedAtEpochMs = started,
        endsAtEpochMs = ends
    )
}

internal fun discordAuthorizationAvailable(sdkAvailable: Boolean, applicationId: Long): Boolean =
    sdkAvailable && applicationId > 0L

object DerbisDiscordRichPresence {
    private const val PREFERENCES = "derbis_discord_presence"
    private const val ENABLED_KEY = "enabled"
    private const val NATIVE_UNAVAILABLE = 0
    private const val NATIVE_DISCONNECTED = 1
    private const val NATIVE_SIGNING_IN = 2
    private const val NATIVE_CONNECTING = 3
    private const val NATIVE_CONNECTED = 4
    private const val NATIVE_ERROR = 5

    private val applicationId: Long = BuildConfig.DISCORD_APPLICATION_ID.trim().toLongOrNull() ?: 0L
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _connectionState = MutableStateFlow(
        DerbisDiscordConnectionState(
            if (isAvailable()) DerbisDiscordConnectionStatus.Disconnected else DerbisDiscordConnectionStatus.Unavailable
        )
    )
    val connectionState: StateFlow<DerbisDiscordConnectionState> = _connectionState.asStateFlow()

    @Volatile private var libraryLoaded = false
    @Volatile private var loadAttempted = false
    @Volatile private var activityAttached = false
    @Volatile private var initialized = false
    @Volatile private var restoreAttempted = false
    @Volatile private var enableAfterSignIn = false
    @Volatile private var lastPresence = DerbisDiscordPresenceSnapshot()
    @Volatile private var lastPublishedPresenceKey = ""
    @Volatile private var nativePresenceActive = false
    @Volatile private var applicationContext: Context? = null
    private var statePollingJob: Job? = null

    fun isAvailable(): Boolean = discordAuthorizationAvailable(BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE, applicationId)

    fun attach(activity: Activity) {
        applicationContext = activity.applicationContext
        if (isAvailable()) {
            activityAttached = runCatching {
                Class.forName("com.discord.socialsdk.DiscordSocialSdkInit")
                    .getMethod("setEngineActivity", Activity::class.java)
                    .invoke(null, activity)
            }.onFailure { Timber.w(it, "Discord Social SDK activity attachment failed") }.isSuccess
        }
        initialize(activity.applicationContext)
    }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        val session = DerbisDiscordAccountStore.read(context)
        if (!isAvailable() || !loadBridge()) {
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Unavailable,
                "Discord authorization requires an Application ID and the authorized Social SDK."
            )
            return
        }
        if (!activityAttached) {
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Error, "Discord authorization needs the app activity to be initialized."
            )
            return
        }
        if (!initialized) initialized = runCatching { nativeInitialize(applicationId) }.getOrDefault(false)
        if (!initialized) {
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Unavailable, "Discord authorization could not initialize."
            )
            return
        }
        if (!restoreAttempted && session != null) {
            restoreAttempted = true
            if (runCatching { nativeRestoreSession(session.refreshToken) }.getOrDefault(false)) {
                _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connecting)
                ensureStatePolling()
            } else {
                _connectionState.value = DerbisDiscordConnectionState(
                    DerbisDiscordConnectionStatus.Error, "Reconnect with Discord to authorize this app."
                )
            }
        }
    }

    fun beginSignIn(context: Context) {
        initialize(context)
        if (!initialized || !isAvailable()) return
        val status = _connectionState.value.status
        if (status == DerbisDiscordConnectionStatus.SigningIn || status == DerbisDiscordConnectionStatus.Connecting) return
        if (runCatching { nativeBeginSignIn() }.getOrDefault(false)) {
            enableAfterSignIn = true
            _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.SigningIn)
            ensureStatePolling()
        } else {
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Error, "Discord authorization could not start."
            )
        }
    }

    fun disconnect(context: Context) {
        enableAfterSignIn = false
        setEnabled(context, false)
        DerbisDiscordAccountStore.clear(context)
        statePollingJob?.cancel()
        statePollingJob = null
        if (loadBridge()) runCatching { nativeDisconnect() }
        restoreAttempted = false
        _connectionState.value = DerbisDiscordConnectionState(
            if (isAvailable()) DerbisDiscordConnectionStatus.Disconnected else DerbisDiscordConnectionStatus.Unavailable
        )
    }

    fun isEnabled(context: Context): Boolean =
        connectionState.value.isConnected && DerbisDiscordAccountStore.read(context) != null &&
            context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(ENABLED_KEY, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        val allowed = enabled && connectionState.value.isConnected && DerbisDiscordAccountStore.read(context) != null
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(ENABLED_KEY, allowed).apply()
        if (allowed) publishStored(context) else clear()
    }

    internal fun publish(context: Context, presence: DerbisDiscordPresenceSnapshot) {
        applicationContext = context.applicationContext
        lastPresence = presence
        if (!presence.playing || !isEnabled(context)) {
            clear()
            return
        }
        if (loadBridge()) publishNative(presence)
    }

    fun clear() {
        if (nativePresenceActive && loadBridge()) runCatching { nativeClear() }
        nativePresenceActive = false
        lastPublishedPresenceKey = ""
    }

    fun release() {
        statePollingJob?.cancel()
        statePollingJob = null
        enableAfterSignIn = false
        clear()
        if (loadBridge()) runCatching { nativeRelease() }
        initialized = false
        restoreAttempted = false
        _connectionState.value = DerbisDiscordConnectionState(
            if (isAvailable()) DerbisDiscordConnectionStatus.Disconnected else DerbisDiscordConnectionStatus.Unavailable
        )
    }

    private fun publishStored(context: Context) {
        if (lastPresence.playing && isEnabled(context) && loadBridge()) publishNative(lastPresence, force = true)
    }

    private fun publishNative(presence: DerbisDiscordPresenceSnapshot, force: Boolean = false) {
        if (!force && nativePresenceActive && lastPublishedPresenceKey == presence.syncKey) return
        val payload = derbisDiscordActivityPayload(presence, System.currentTimeMillis())
        runCatching {
            nativeUpdate(
                payload.title, payload.artist, payload.artworkUrl, payload.artistThumbnailUrl,
                payload.songUrl, payload.artistUrl, payload.artworkText,
                payload.startedAtEpochMs, payload.endsAtEpochMs, presence.playing
            )
            nativePresenceActive = true
            lastPublishedPresenceKey = presence.syncKey
        }.onFailure { Timber.w(it, "Discord native presence update failed") }
    }

    private fun ensureStatePolling() {
        if (statePollingJob?.isActive == true) return
        statePollingJob = bridgeScope.launch {
            while (isActive) {
                applicationContext?.let(::synchronizeNativeState)
                when (_connectionState.value.status) {
                    DerbisDiscordConnectionStatus.Disconnected,
                    DerbisDiscordConnectionStatus.Unavailable,
                    DerbisDiscordConnectionStatus.Error -> break
                    else -> delay(750L)
                }
            }
        }
    }

    private fun synchronizeNativeState(context: Context) {
        if (!initialized || !loadBridge()) return
        val previous = _connectionState.value.status
        when (runCatching { nativeConnectionState() }.getOrDefault(NATIVE_DISCONNECTED)) {
            NATIVE_UNAVAILABLE -> _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Unavailable)
            NATIVE_DISCONNECTED -> _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
            NATIVE_SIGNING_IN -> _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.SigningIn)
            NATIVE_CONNECTING -> _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connecting)
            NATIVE_CONNECTED -> {
                val accessToken = runCatching { nativeTakeAccessToken() }.getOrDefault("")
                val refreshToken = runCatching { nativeTakeRefreshToken() }.getOrDefault("")
                val tokenType = runCatching { nativeTakeTokenType() }.getOrDefault(0)
                if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                    DerbisDiscordAccountStore.save(context, DerbisDiscordSession(accessToken, refreshToken, tokenType))
                }
                val session = DerbisDiscordAccountStore.read(context)
                if (session == null) {
                    _connectionState.value = DerbisDiscordConnectionState(
                        DerbisDiscordConnectionStatus.Error, "Discord returned no refreshable OAuth session."
                    )
                    return
                }
                restoreAttempted = true
                _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connected)
                if (enableAfterSignIn) {
                    enableAfterSignIn = false
                    context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                        .edit().putBoolean(ENABLED_KEY, true).apply()
                }
            }
            NATIVE_ERROR -> {
                enableAfterSignIn = false
                val detail = runCatching { nativeConnectionError() }.getOrDefault("")
                _connectionState.value = DerbisDiscordConnectionState(
                    DerbisDiscordConnectionStatus.Error, detail.ifBlank { "Discord authorization failed." }
                )
            }
        }
        if (previous != DerbisDiscordConnectionStatus.Connected && _connectionState.value.isConnected) publishStored(context)
    }

    private fun loadBridge(): Boolean {
        if (!isAvailable()) return false
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
        artistThumbnailUrl: String,
        songUrl: String,
        artistUrl: String,
        artworkText: String,
        startedAtEpochMs: Long,
        endsAtEpochMs: Long,
        playing: Boolean
    )
    private external fun nativeClear()
    private external fun nativeRelease()
}
