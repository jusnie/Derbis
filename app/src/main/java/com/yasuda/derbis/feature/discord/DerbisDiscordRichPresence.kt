package com.yasuda.derbis.feature.discord

import android.app.Activity
import android.content.Context
import android.net.Uri
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
 *     (track title, artist, cover art and artist icon, plus a connection tester)
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
            playing.toString()
        ).joinToString(separator = "\u0000")
}

private const val DISCORD_PRESENCE_TEXT_LIMIT = 128

/**
 * Discord only accepts five game-status updates per 20 seconds per session
 * (https://discord.com/developers/docs/events/gateway-events#update-presence),
 * so updates are spaced at least this far apart and bursts are coalesced into a
 * single trailing update instead of being dropped.
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

    return DerbisDiscordPresenceSnapshot(
        trackId = track?.id.orEmpty(),
        title = title,
        artist = artist,
        artistId = artistId,
        artworkUrl = artwork,
        artistThumbnailUrl = artwork,
        positionMs = positionMs.coerceAtLeast(0L),
        durationMs = effectiveDuration.coerceAtLeast(0L),
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
    private const val AUTHORIZE_ENDPOINT = "https://discord.com/oauth2/authorize"
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
            // The gateway authenticates account tokens only; an OAuth access token is for REST.
            if (savedSession.userToken.isNotBlank()) {
                DiscordRpcClient.connect(savedSession.userToken)
            }
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

    /** True when this build carries a Discord application id and a redirect URI to call back to. */
    fun isConfigured(): Boolean =
        BuildConfig.DISCORD_APPLICATION_ID.isNotBlank() && BuildConfig.DISCORD_REDIRECT_URI.isNotBlank()

    /**
     * The Discord authorization page for Derbis, or null when this build has no application id.
     *
     * `response_type=token` keeps the exchange on the device: Discord hands the access token back
     * in the redirect, so the app never ships a client secret. Callers open this in the Discord
     * app first and fall back to a browser, so signing in happens where the user is already logged in.
     */
    fun beginAuthorization(context: Context): Uri? {
        applicationContext = context.applicationContext
        if (!isConfigured()) {
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Unavailable,
                "This build has no Discord application id, so Discord sign-in is unavailable."
            )
            return null
        }
        _connectionState.value = DerbisDiscordConnectionState(
            DerbisDiscordConnectionStatus.SigningIn,
            "Approve Derbis inside Discord to finish signing in."
        )
        return Uri.parse(AUTHORIZE_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", BuildConfig.DISCORD_APPLICATION_ID)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("scope", "identify")
            .appendQueryParameter("redirect_uri", BuildConfig.DISCORD_REDIRECT_URI)
            .build()
    }

    /** Whether a deep link is the redirect this app is waiting for. */
    fun receives(uri: Uri?): Boolean {
        if (uri == null) return false
        val expected = Uri.parse(BuildConfig.DISCORD_REDIRECT_URI)
        return uri.scheme.equals(expected.scheme, ignoreCase = true) &&
            uri.host.equals(expected.host, ignoreCase = true)
    }

    /**
     * Completes the authorization started by [beginAuthorization]. Discord returns the token in
     * the redirect fragment, which Android hands back as `Uri.fragment`.
     */
    fun handleRedirect(context: Context, uri: Uri?) {
        if (!receives(uri) || uri == null) return
        applicationContext = context.applicationContext
        // The implicit grant returns the token in the fragment; browsers that rewrite the
        // redirect may hand the same keys back as a query instead, so both are accepted.
        val fragment = uri.fragment.orEmpty()
        val params = if (fragment.isBlank()) {
            uri
        } else {
            // Uri reads queries, not fragments, so re-home the fragment as the query.
            Uri.parse("${BuildConfig.DISCORD_REDIRECT_URI}?$fragment")
        }
        fun read(key: String): String =
            params.getQueryParameter(key) ?: uri.getQueryParameter(key).orEmpty()
        val error = read("error")
        if (error.isNotBlank()) {
            val description = read("error_description")
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Error,
                description.ifBlank { "Discord sign-in was cancelled or denied ($error)." }
            )
            return
        }
        val accessToken = read("access_token")
        if (accessToken.isBlank()) {
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Error,
                "Discord did not return an access token. Try signing in again."
            )
            return
        }
        completeAuthorization(context, accessToken)
    }

    private fun completeAuthorization(context: Context, accessToken: String) {
        _connectionState.value = DerbisDiscordConnectionState(
            DerbisDiscordConnectionStatus.Connecting,
            "Verifying your Discord account..."
        )
        bridgeScope.launch {
            val diagnostic = DiscordRpcClient.testConnection("Bearer $accessToken")
            if (!diagnostic.success) {
                _connectionState.value = DerbisDiscordConnectionState(
                    status = DerbisDiscordConnectionStatus.Error,
                    detail = diagnostic.message
                )
                return@launch
            }
            DerbisDiscordAccountStore.save(
                context,
                DerbisDiscordSession(
                    accessToken = accessToken,
                    tokenType = 1,
                    username = diagnostic.username,
                    globalName = diagnostic.globalName,
                    avatarUrl = diagnostic.avatarUrl
                )
            )
            _connectionState.value = DerbisDiscordConnectionState(
                status = DerbisDiscordConnectionStatus.Connected,
                detail = "Connected as @${diagnostic.username}",
                username = diagnostic.username,
                globalName = diagnostic.globalName,
                avatarUrl = diagnostic.avatarUrl,
                latencyMs = diagnostic.latencyMs
            )
            // An OAuth access token authenticates the REST API, not the Discord gateway, so
            // presence can only go out through the official Social SDK bridge.
            if (BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE) {
                setEnabled(context, true)
                publishStored(context)
            }
        }
    }

    /**
     * Connection tester: runs 4-phase diagnostics to troubleshoot any connection issues.
     */
    fun testConnection(context: Context, onResult: (DiscordDiagnosticResult) -> Unit) {
        val session = DerbisDiscordAccountStore.read(context)
        // OAuth sessions carry a bearer token; legacy user-token sessions send the account token raw.
        val token = session?.let { session ->
            session.userToken.ifBlank {
                session.accessToken.takeIf(String::isNotBlank)?.let { "Bearer $it" }.orEmpty()
            }
        }.orEmpty()
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

    internal fun publish(context: Context, presence: DerbisDiscordPresenceSnapshot) {
        applicationContext = context.applicationContext
        lastPresence = presence
        if (!presence.playing || !isEnabled(context)) {
            clear()
            return
        }

        // Publish via WebSocket Gateway RPC (cover art, artist icon, title and artist only)
        enqueueGatewayPresence(presence)

        // Publish via native bridge if loaded.
        if (loadBridge()) {
            publishNative(presence)
        }
    }

    /**
     * Sends a gateway presence update immediately when the 5-per-20-seconds budget allows,
     * otherwise keeps only the newest snapshot and flushes it once the window reopens.
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
            publishNative(lastPresence, force = true)
        }
    }

    private fun publishNative(presence: DerbisDiscordPresenceSnapshot, force: Boolean = false) {
        if (!force && nativePresenceActive && lastPublishedPresenceKey == presence.syncKey) return
        runCatching {
            nativeUpdate(
                title = presence.title,
                artist = presence.artist,
                artworkUrl = presence.artworkUrl,
                // No start timestamp: the activity carries no progress bar.
                startedAtEpochSeconds = 0L,
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
