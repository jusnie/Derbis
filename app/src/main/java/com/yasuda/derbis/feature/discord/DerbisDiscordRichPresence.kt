package com.yasuda.derbis.feature.discord

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Base64
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
 *     (clickable buttons, progress bar, dual artwork/artist icon assets, live lyric line,
 *     the Derbis logo as artwork fallback, and a connection tester)
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

/** Activity name shown by the native bridge, matching what the gateway client sends. */
private const val DISCORD_ACTIVITY_NAME = "Listening in Derbis"

/** Placeholder line for a track whose metadata has no artist. */
private const val DISCORD_UNKNOWN_ARTIST = "Unknown Artist"

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
 * Shifts the playback clock by the user's calibrated lyric delay before the active line is
 * resolved, so a Bluetooth output that lags the audio clock does not push the presence ahead.
 */
internal fun discordLyricPositionMs(positionMs: Long, offsetMs: Long): Long =
    (positionMs - offsetMs).coerceAtLeast(0L)

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

/**
 * Matches a candidate deep-link URI against the configured Discord mobile redirect URI across all
 * custom-scheme forms Android browsers and the Discord app emit:
 * - `discord-<id>:/authorize/callback?...` (documented single-slash hostless form)
 * - `discord-<id>://authorize/callback?...` (authority + path normalization)
 * - `discord-<id>:authorize/callback?...` (opaque scheme-specific part without leading slash)
 */
internal fun matchesDiscordRedirect(candidateUri: String?, configuredRedirectUri: String): Boolean {
    val rawCandidate = candidateUri?.trim().orEmpty()
    val rawExpected = configuredRedirectUri.trim()
    if (rawCandidate.isBlank() || rawExpected.isBlank()) return false

    fun splitSchemeAndPath(raw: String): Pair<String, String>? {
        val colon = raw.indexOf(':')
        if (colon <= 0) return null
        val scheme = raw.substring(0, colon).lowercase()
        val withoutQuery = raw.substring(colon + 1)
            .substringBefore('#')
            .substringBefore('?')
        val normalizedPath = withoutQuery.trimStart('/').trimEnd('/').lowercase()
        return scheme to normalizedPath
    }

    val (candidateScheme, candidatePath) = splitSchemeAndPath(rawCandidate) ?: return false
    val (expectedScheme, expectedPath) = splitSchemeAndPath(rawExpected) ?: return false
    if (candidateScheme != expectedScheme) return false
    if (expectedPath.isBlank()) return true
    return candidatePath == expectedPath
}

/**
 * Extracts query and fragment parameters from a Discord OAuth redirect URI without relying on
 * hierarchical `android.net.Uri.getQueryParameter` (which throws on opaque `scheme:path?query` URIs).
 */
internal fun parseDiscordCallbackParams(candidateUri: String?): Map<String, String> {
    val raw = candidateUri?.trim().orEmpty()
    if (raw.isBlank()) return emptyMap()
    val result = LinkedHashMap<String, String>()

    fun decodeComponent(value: String): String = runCatching {
        java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
    }.getOrDefault(value)

    fun parsePairList(encoded: String) {
        if (encoded.isBlank()) return
        for (entry in encoded.split('&')) {
            if (entry.isBlank()) continue
            val eq = entry.indexOf('=')
            val key = if (eq >= 0) decodeComponent(entry.substring(0, eq)) else decodeComponent(entry)
            val value = if (eq >= 0) decodeComponent(entry.substring(eq + 1)) else ""
            if (key.isNotBlank() && !result.containsKey(key)) {
                result[key] = value
            }
        }
    }

    val fragment = raw.substringAfter('#', "")
    val withoutFragment = raw.substringBefore('#')
    val query = withoutFragment.substringAfter('?', "")
    // Prefer query parameters (`?code=...&state=...`), falling back to fragment (`#access_token=...`).
    parsePairList(query)
    parsePairList(fragment)
    return result
}

internal fun mergeDiscordOAuthScopes(sdkDefaultScopes: String, includeSdkScopes: Boolean): String {
    if (!includeSdkScopes || sdkDefaultScopes.isBlank()) return "identify"
    return (sdkDefaultScopes.trim().split(Regex("\\s+")) + "identify")
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .joinToString(" ")
}

object DerbisDiscordRichPresence {
    private const val AUTHORIZE_ENDPOINT = "https://discord.com/oauth2/authorize"
    private const val PREFERENCES = "derbis_discord_presence"
    private const val ENABLED_KEY = "enabled"
    private const val LYRICS_KEY = "lyrics_in_presence"
    private const val PKCE_VERIFIER_KEY = "pkce_verifier"
    private const val OAUTH_STATE_KEY = "oauth_state"
    private const val IDENTIFY_ONLY_SCOPE_KEY = "identify_only_scope"

    /**
     * How long a started sign-in is treated as still in flight. Nothing guarantees a callback -
     * the Discord app can be closed from the consent screen, the deep link can land nowhere, the
     * phone can kill the browser - and a panel that waits forever with every control disabled has
     * no way out of itself. Past this, the panel goes back to "not signed in" on its own.
     */
    private const val SIGN_IN_TIMEOUT_MS = 5 * 60 * 1000L
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

    /** When [beginAuthorization] last ran, so a sign-in nobody finished can expire. */
    @Volatile
    private var signInStartedAtMs = 0L

    val connectionState: StateFlow<DerbisDiscordConnectionState> = _connectionState.asStateFlow()

    @Volatile private var libraryLoaded = false
    @Volatile private var loadAttempted = false
    @Volatile private var initialized = false
    @Volatile private var restoreAttempted = false
    @Volatile private var lastHandledRedirectSignature = ""
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

    private fun ensureNativeInitialized(): Boolean {
        if (!BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE || applicationId <= 0L) return false
        if (!loadBridge()) return false
        if (!initialized) {
            initialized = runCatching { nativeInitialize(applicationId) }.getOrDefault(false)
        }
        return initialized
    }

    private fun connectNativeSession(session: DerbisDiscordSession) {
        if (!ensureNativeInitialized()) return
        val bearer = session.accessToken
        if (bearer.isNotBlank()) {
            val tokenType = if (session.tokenType > 0) session.tokenType else 1
            runCatching { nativeApplyToken(bearer, session.refreshToken, tokenType) }
        } else if (session.refreshToken.isNotBlank()) {
            runCatching { nativeRestoreSession(session.refreshToken) }
        }
        ensureStatePolling()
    }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        val savedSession = DerbisDiscordAccountStore.read(context)
        if (savedSession != null && savedSession.hasValidToken) {
            val statusDetail = if (savedSession.username.isNotBlank()) {
                "Connected as @${savedSession.username}"
            } else {
                "Connected"
            }
            _connectionState.value = DerbisDiscordConnectionState(
                status = DerbisDiscordConnectionStatus.Connected,
                detail = statusDetail,
                username = savedSession.username,
                globalName = savedSession.globalName,
                avatarUrl = savedSession.avatarUrl
            )
            // The gateway authenticates raw account tokens only; an OAuth access token is for REST/SDK.
            if (savedSession.userToken.isNotBlank()) {
                DiscordRpcClient.connect(savedSession.userToken)
            }
            if (!restoreAttempted) {
                restoreAttempted = true
                connectNativeSession(savedSession)
                verifyOrRefreshStoredSession(context.applicationContext, savedSession)
            }
        } else {
            if (
                _connectionState.value.status != DerbisDiscordConnectionStatus.SigningIn &&
                _connectionState.value.status != DerbisDiscordConnectionStatus.Connecting
            ) {
                _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
            }
        }
    }

    private fun verifyOrRefreshStoredSession(context: Context, session: DerbisDiscordSession) {
        bridgeScope.launch {
            if (session.userToken.isNotBlank()) {
                val res = DiscordRpcClient.testConnection(session.userToken, preferBearer = false)
                if (res.success) {
                    val updated = session.copy(
                        username = res.username.ifBlank { session.username },
                        globalName = res.globalName.ifBlank { session.globalName },
                        avatarUrl = res.avatarUrl.ifBlank { session.avatarUrl }
                    )
                    DerbisDiscordAccountStore.save(context, updated)
                    _connectionState.value = _connectionState.value.copy(
                        status = DerbisDiscordConnectionStatus.Connected,
                        username = updated.username,
                        globalName = updated.globalName,
                        avatarUrl = updated.avatarUrl,
                        latencyMs = res.latencyMs
                    )
                }
                return@launch
            }

            if (session.accessToken.isBlank()) return@launch
            var activeAccess = session.accessToken
            var activeRefresh = session.refreshToken
            var diagnostic = DiscordRpcClient.testConnection(activeAccess, preferBearer = true)
            if (!diagnostic.success && diagnostic.httpCode == 401 && activeRefresh.isNotBlank() && isConfigured()) {
                val refreshed = DiscordRpcClient.refreshAuthorizationToken(
                    clientId = BuildConfig.DISCORD_APPLICATION_ID,
                    refreshToken = activeRefresh
                )
                if (refreshed.success) {
                    activeAccess = refreshed.accessToken
                    activeRefresh = refreshed.refreshToken
                    diagnostic = DiscordRpcClient.testConnection(activeAccess, preferBearer = true)
                    if (diagnostic.success) {
                        val updated = session.copy(
                            accessToken = activeAccess,
                            refreshToken = activeRefresh,
                            tokenType = 1,
                            username = diagnostic.username.ifBlank { session.username },
                            globalName = diagnostic.globalName.ifBlank { session.globalName },
                            avatarUrl = diagnostic.avatarUrl.ifBlank { session.avatarUrl }
                        )
                        DerbisDiscordAccountStore.save(context, updated)
                        connectNativeSession(updated)
                    }
                }
            }
            if (diagnostic.success) {
                val updated = session.copy(
                    accessToken = activeAccess,
                    refreshToken = activeRefresh,
                    username = diagnostic.username.ifBlank { session.username },
                    globalName = diagnostic.globalName.ifBlank { session.globalName },
                    avatarUrl = diagnostic.avatarUrl.ifBlank { session.avatarUrl }
                )
                DerbisDiscordAccountStore.save(context, updated)
                _connectionState.value = _connectionState.value.copy(
                    status = DerbisDiscordConnectionStatus.Connected,
                    detail = if (BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE) {
                        "Connected as @${updated.username}"
                    } else {
                        "Signed in as @${updated.username}. Rich Presence needs the Discord Social SDK, which this build does not carry."
                    },
                    username = updated.username,
                    globalName = updated.globalName,
                    avatarUrl = updated.avatarUrl,
                    latencyMs = diagnostic.latencyMs
                )
            }
        }
    }

    /** True when this build carries a Discord application id and a redirect URI to call back to. */
    fun isConfigured(): Boolean =
        BuildConfig.DISCORD_APPLICATION_ID.isNotBlank() && BuildConfig.DISCORD_REDIRECT_URI.isNotBlank()

    /**
     * The Discord authorization page for Derbis, or null when this build has no application id.
     *
     * Authorization code + PKCE, not the old implicit grant: Discord's current mobile flow is
     * code + PKCE with a custom-scheme redirect, and the Discord app is the component that
     * completes it. The verifier stays on the device, so no client secret ships in the APK.
     * Callers open this in the Discord app first and fall back to a browser, so signing in happens
     * where the user is already logged in.
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
        lastHandledRedirectSignature = ""
        val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val verifier = newCodeVerifier()
        // The redirect is a custom scheme, which any app on the device could claim, so the
        // callback carries a nonce this app generated and nobody else has seen. Persisted before
        // the hand-off, along with the PKCE verifier, so a process death during the Discord
        // round-trip does not strand the callback with nothing to check it against.
        val state = newCodeVerifier()
        preferences
            .edit()
            .putString(PKCE_VERIFIER_KEY, verifier)
            .putString(OAUTH_STATE_KEY, state)
            .apply()
        val includeSdkScopes = BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE &&
            !preferences.getBoolean(IDENTIFY_ONLY_SCOPE_KEY, false)
        val sdkScopes = if (includeSdkScopes && ensureNativeInitialized()) {
            runCatching { nativeDefaultScopes() }.getOrDefault("")
        } else {
            ""
        }
        val scopeParam = mergeDiscordOAuthScopes(sdkScopes, includeSdkScopes)
        signInStartedAtMs = android.os.SystemClock.elapsedRealtime()
        _connectionState.value = DerbisDiscordConnectionState(
            DerbisDiscordConnectionStatus.SigningIn,
            "Approve Derbis in Discord to finish signing in."
        )
        return Uri.parse(AUTHORIZE_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", BuildConfig.DISCORD_APPLICATION_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", scopeParam)
            .appendQueryParameter("redirect_uri", BuildConfig.DISCORD_REDIRECT_URI)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codeChallenge(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    /**
     * Deep link to the same authorization page that the Discord app itself understands, so the
     * consent screen opens inside Discord instead of a browser tab.
     */
    fun authorizationDeepLink(authorization: Uri): Uri? = runCatching {
        val path = authorization.encodedPath?.removePrefix("/").orEmpty()
        val query = authorization.encodedQuery.orEmpty()
        if (path.isBlank()) return@runCatching null
        Uri.parse("discord://-/$path" + if (query.isBlank()) "" else "?$query")
    }.getOrNull()

    private fun newCodeVerifier(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
    }

    private fun codeChallenge(verifier: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
    }

    /**
     * Tells the user what the return trip did.
     *
     * The callback usually relaunches the app on whatever screen it left, not on the Discord
     * panel, so a status that only lives in that panel is a status nobody sees: the phone
     * bounces back to Derbis, nothing appears to change, and the sign-in reads as broken even
     * when it succeeded.
     */
    private fun announce(message: String) {
        val context = applicationContext ?: return
        runCatching {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Whether the Discord app can take the authorization link on this device.
     *
     * Launching does not need this answer - startActivity() is not restricted by package
     * visibility - but saying "Open Discord" when Discord is not installed sends the listener
     * looking for an app that is not there, so the panel asks before it promises.
     */
    fun isDiscordAppAvailable(context: Context): Boolean = runCatching {
        context.packageManager.queryIntentActivities(
            android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("discord://-/")),
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        ).isNotEmpty()
    }.getOrDefault(false)

    /**
     * Gives up on a sign-in that was started but never finished, and says so. The PKCE verifier is
     * dropped with the state, so a callback that arrives late cannot complete a flow the listener
     * already walked away from.
     */
    fun cancelAuthorization(context: Context) {
        signInStartedAtMs = 0L
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .remove(PKCE_VERIFIER_KEY)
                .remove(OAUTH_STATE_KEY)
                .apply()
        }
        _connectionState.value = DerbisDiscordConnectionState(
            DerbisDiscordConnectionStatus.Disconnected,
            "Discord sign-in was cancelled."
        )
    }

    /**
     * True when a sign-in has been waiting longer than [SIGN_IN_TIMEOUT_MS]. Called from the panel
     * so the state it renders is never one nobody can leave.
     */
    fun hasStaleSignIn(): Boolean {
        val started = signInStartedAtMs
        if (started == 0L) return false
        return android.os.SystemClock.elapsedRealtime() - started > SIGN_IN_TIMEOUT_MS
    }

    /** Whether a deep link is the redirect this app is waiting for. */
    fun receives(uri: Uri?): Boolean =
        matchesDiscordRedirect(uri?.toString(), BuildConfig.DISCORD_REDIRECT_URI)

    /**
     * Completes the authorization started by [beginAuthorization].
     */
    fun handleRedirect(context: Context, uri: Uri?) {
        if (!receives(uri) || uri == null) return
        val rawUri = uri.toString()
        if (rawUri.isNotBlank() && rawUri == lastHandledRedirectSignature) {
            return
        }
        lastHandledRedirectSignature = rawUri
        signInStartedAtMs = 0L
        applicationContext = context.applicationContext
        val parsedParams = parseDiscordCallbackParams(rawUri)
        fun read(key: String): String = parsedParams[key].orEmpty()

        // State check first: if this callback is not the one this app started, none of the rest
        // of it can be trusted, including its error field.
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val expectedState = preferences.getString(OAUTH_STATE_KEY, null).orEmpty()
        val returnedState = read("state")
        if (expectedState.isNotBlank()) {
            preferences.edit().remove(OAUTH_STATE_KEY).apply()
            if (returnedState != expectedState) {
                _connectionState.value = DerbisDiscordConnectionState(
                    DerbisDiscordConnectionStatus.Error,
                    "Discord sign-in did not come back to the session that started it. Try again."
                )
                announce("Discord sign-in did not finish. Try again from Settings → Discord.")
                return
            }
        }
        val error = read("error")
        if (error.isNotBlank()) {
            if (error.equals("invalid_scope", ignoreCase = true)) {
                preferences.edit().putBoolean(IDENTIFY_ONLY_SCOPE_KEY, true).apply()
            }
            val description = read("error_description")
            val message = when {
                error.equals("invalid_scope", ignoreCase = true) ->
                    "Discord rejected the requested Rich Presence scope. Tap Reconnect Discord to sign in with basic account access."
                description.isNotBlank() -> description
                else -> "Discord sign-in was cancelled or denied ($error)."
            }
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Error,
                message
            )
            announce(message)
            return
        }
        val accessToken = read("access_token")
        if (accessToken.isNotBlank()) {
            // Legacy fragment response, kept so an old pending sign-in still completes.
            completeAuthorization(context, accessToken)
            return
        }
        val code = read("code")
        if (code.isBlank()) {
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Error,
                "Discord did not return an authorization code. Try signing in again."
            )
            announce("Discord did not finish sign-in. Try again.")
            return
        }
        exchangeCode(context, code)
    }

    /** Swaps the authorization code for a token, then verifies it like any other session. */
    private fun exchangeCode(context: Context, code: String) {
        val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val verifier = preferences.getString(PKCE_VERIFIER_KEY, null).orEmpty()
        if (verifier.isBlank()) {
            // If an earlier callback already saved a valid session, do not clobber Connected with Error.
            val existing = DerbisDiscordAccountStore.read(context)
            if (existing != null && existing.hasValidToken) {
                initialize(context)
                return
            }
            _connectionState.value = DerbisDiscordConnectionState(
                DerbisDiscordConnectionStatus.Error,
                "The sign-in challenge expired. Start the Discord sign-in again."
            )
            announce("Discord sign-in expired. Start it again from Settings → Discord.")
            return
        }
        _connectionState.value = DerbisDiscordConnectionState(
            DerbisDiscordConnectionStatus.Connecting,
            "Finishing Discord sign-in..."
        )
        bridgeScope.launch {
            val exchange = DiscordRpcClient.exchangeAuthorizationCode(
                clientId = BuildConfig.DISCORD_APPLICATION_ID,
                code = code,
                redirectUri = BuildConfig.DISCORD_REDIRECT_URI,
                codeVerifier = verifier
            )
            if (!exchange.success) {
                _connectionState.value = DerbisDiscordConnectionState(
                    DerbisDiscordConnectionStatus.Error,
                    exchange.message
                )
                announce(exchange.message)
                return@launch
            }
            preferences.edit().remove(PKCE_VERIFIER_KEY).apply()
            completeAuthorization(context, exchange.accessToken, exchange.refreshToken)
        }
    }

    private fun completeAuthorization(context: Context, accessToken: String, refreshToken: String = "") {
        _connectionState.value = DerbisDiscordConnectionState(
            DerbisDiscordConnectionStatus.Connecting,
            "Verifying your Discord account..."
        )
        bridgeScope.launch {
            val diagnostic = DiscordRpcClient.testConnection(accessToken, preferBearer = true)
            if (!diagnostic.success) {
                _connectionState.value = DerbisDiscordConnectionState(
                    status = DerbisDiscordConnectionStatus.Error,
                    detail = diagnostic.message
                )
                announce("Discord sign-in failed: ${diagnostic.message}")
                return@launch
            }
            val saved = DerbisDiscordSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = 1,
                username = diagnostic.username,
                globalName = diagnostic.globalName,
                avatarUrl = diagnostic.avatarUrl
            )
            DerbisDiscordAccountStore.save(context, saved)
            restoreAttempted = true
            _connectionState.value = DerbisDiscordConnectionState(
                status = DerbisDiscordConnectionStatus.Connected,
                detail = "Connected as @${diagnostic.username}",
                username = diagnostic.username,
                globalName = diagnostic.globalName,
                avatarUrl = diagnostic.avatarUrl,
                latencyMs = diagnostic.latencyMs
            )
            // An OAuth access token authenticates the REST API and Social SDK, not the raw user gateway.
            if (BuildConfig.DISCORD_SOCIAL_SDK_AVAILABLE) {
                connectNativeSession(saved)
                setEnabled(context, true)
                publishStored(context)
                announce("Discord connected as @${diagnostic.username}.")
            } else {
                _connectionState.value = _connectionState.value.copy(
                    detail = "Signed in as @${diagnostic.username}. Rich Presence needs the " +
                        "Discord Social SDK, which this build does not carry."
                )
                announce(
                    "Signed in as @${diagnostic.username}. Rich Presence needs the Discord " +
                        "Social SDK, which this build does not carry."
                )
            }
        }
    }

    /**
     * Connection tester: runs 4-phase diagnostics to troubleshoot any connection issues.
     */
    fun testConnection(context: Context, onResult: (DiscordDiagnosticResult) -> Unit) {
        val appContext = context.applicationContext
        bridgeScope.launch {
            val session = DerbisDiscordAccountStore.read(appContext)
            if (session == null || !session.hasValidToken) {
                onResult(
                    DiscordDiagnosticResult(
                        success = false,
                        message = "No Discord account is signed in yet. Tap Connect Discord first."
                    )
                )
                return@launch
            }
            val isUserToken = session.userToken.isNotBlank()
            var activeToken = if (isUserToken) session.userToken else session.accessToken
            var diagnostic = DiscordRpcClient.testConnection(activeToken, preferBearer = !isUserToken)
            if (!diagnostic.success && !isUserToken && diagnostic.httpCode == 401 && session.refreshToken.isNotBlank() && isConfigured()) {
                val refreshed = DiscordRpcClient.refreshAuthorizationToken(
                    clientId = BuildConfig.DISCORD_APPLICATION_ID,
                    refreshToken = session.refreshToken
                )
                if (refreshed.success) {
                    activeToken = refreshed.accessToken
                    diagnostic = DiscordRpcClient.testConnection(activeToken, preferBearer = true)
                    if (diagnostic.success) {
                        val updated = session.copy(
                            accessToken = refreshed.accessToken,
                            refreshToken = refreshed.refreshToken,
                            tokenType = 1,
                            username = diagnostic.username.ifBlank { session.username },
                            globalName = diagnostic.globalName.ifBlank { session.globalName },
                            avatarUrl = diagnostic.avatarUrl.ifBlank { session.avatarUrl }
                        )
                        DerbisDiscordAccountStore.save(appContext, updated)
                        connectNativeSession(updated)
                    }
                }
            }
            if (diagnostic.success) {
                _connectionState.value = _connectionState.value.copy(
                    status = DerbisDiscordConnectionStatus.Connected,
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

        // Both transports are driven from one schedule: Discord counts a presence update once per
        // session, so the gateway and the Social SDK share the same budget and the same layout.
        enqueuePresence(effective)
    }

    /**
     * Sends a presence update immediately when the 5-per-20-seconds budget allows, otherwise keeps
     * only the newest snapshot and flushes it once the window reopens. Without this a rapidly
     * changing lyric line would trip Discord's presence rate limit.
     */
    private fun enqueuePresence(presence: DerbisDiscordPresenceSnapshot, forceNative: Boolean = false) {
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
            dispatchPresence(presence, forceNative)
        } else {
            scheduleTrailingPresence(forceNative)
        }
    }

    private fun scheduleTrailingPresence(forceNative: Boolean) {
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
                if (latest != null) dispatchPresence(latest, forceNative)
            }
        }
    }

    /**
     * Hands one snapshot to every transport that is available: the Discord Gateway client and,
     * when the Social SDK is loaded, the native bridge. Each transport draws the same two activity
     * lines, so the lyric line travels to whichever one the listener's build can actually use.
     */
    private fun dispatchPresence(presence: DerbisDiscordPresenceSnapshot, forceNative: Boolean = false) {
        DiscordRpcClient.updatePresence(presence)
        if (loadBridge()) {
            publishNative(presence, force = forceNative)
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
        // Forced so a transport that just reconnected publishes again instead of trusting the
        // dedupe key from before the reconnect.
        enqueuePresence(lastPresence, forceNative = true)
    }

    private fun publishNative(presence: DerbisDiscordPresenceSnapshot, force: Boolean = false) {
        if (!force && nativePresenceActive && lastPublishedPresenceKey == presence.syncKey) return
        // Where this track began, so the SDK renders the same clock as the gateway.
        val startedAtEpochSeconds = (
            System.currentTimeMillis() - presence.positionMs.coerceAtLeast(0L)
        ).coerceAtLeast(0L) / 1_000L
        // Discord draws a progress bar only when it knows where the activity ends; a start on its
        // own is a counting-up clock, which is all this card used to show.
        val durationMs = presence.durationMs.coerceAtLeast(0L)
        // The Social SDK activity has the same two text lines as the gateway activity, so the
        // lyric line takes the first one and the track stays on the second, exactly as it does
        // there. Without a lyric line the track and artist take those two lines instead.
        val lyricLine = presence.lyricLine
        val hasLyric = lyricLine.isNotBlank()
        val details = (if (hasLyric) lyricLine else presence.title).discordPresenceText()
        val state = (
            if (hasLyric) presence.artistTitleLine.ifBlank { DISCORD_ACTIVITY_NAME }
            else presence.artist.ifBlank { DISCORD_UNKNOWN_ARTIST }
        ).discordPresenceText()
        val largeText = listOf(presence.title, presence.artist)
            .filter(String::isNotBlank)
            .joinToString(separator = " — ")
            .ifBlank { DISCORD_ACTIVITY_NAME }
            .discordPresenceText()
        runCatching {
            nativeUpdate(
                details = details,
                state = state,
                largeText = largeText,
                artworkUrl = presence.artworkUrl,
                startedAtEpochSeconds = startedAtEpochSeconds,
                durationMs = durationMs,
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
        val current = _connectionState.value
        val previous = current.status
        val storedSession = DerbisDiscordAccountStore.read(context)
        val hasSavedAccount = storedSession != null && storedSession.hasValidToken
        val nativeStatus = runCatching { nativeConnectionState() }.getOrDefault(NATIVE_DISCONNECTED)
        when (nativeStatus) {
            NATIVE_DISCONNECTED -> {
                if (!DiscordRpcClient.isConnected && !hasSavedAccount &&
                    previous != DerbisDiscordConnectionStatus.SigningIn &&
                    previous != DerbisDiscordConnectionStatus.Connecting
                ) {
                    _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Disconnected)
                }
            }
            NATIVE_SIGNING_IN -> {
                if (!hasSavedAccount) {
                    _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.SigningIn)
                }
            }
            NATIVE_CONNECTING -> {
                if (!hasSavedAccount && previous != DerbisDiscordConnectionStatus.Connected) {
                    _connectionState.value = DerbisDiscordConnectionState(DerbisDiscordConnectionStatus.Connecting)
                }
            }
            NATIVE_CONNECTED -> {
                val accessToken = runCatching { nativeTakeAccessToken() }.getOrDefault("")
                val refreshToken = runCatching { nativeTakeRefreshToken() }.getOrDefault("")
                val tokenType = runCatching { nativeTakeTokenType() }.getOrDefault(1)
                val preservedUsername = storedSession?.username?.ifBlank { current.username } ?: current.username
                val preservedGlobal = storedSession?.globalName?.ifBlank { current.globalName } ?: current.globalName
                val preservedAvatar = storedSession?.avatarUrl?.ifBlank { current.avatarUrl } ?: current.avatarUrl
                if (accessToken.isNotBlank()) {
                    DerbisDiscordAccountStore.save(
                        context,
                        DerbisDiscordSession(
                            accessToken = accessToken,
                            refreshToken = refreshToken.ifBlank { storedSession?.refreshToken.orEmpty() },
                            tokenType = tokenType,
                            userToken = storedSession?.userToken.orEmpty(),
                            username = preservedUsername,
                            globalName = preservedGlobal,
                            avatarUrl = preservedAvatar
                        )
                    )
                }
                _connectionState.value = DerbisDiscordConnectionState(
                    status = DerbisDiscordConnectionStatus.Connected,
                    detail = if (preservedUsername.isNotBlank()) "Connected as @$preservedUsername" else "Connected",
                    username = preservedUsername,
                    globalName = preservedGlobal,
                    avatarUrl = preservedAvatar,
                    latencyMs = current.latencyMs
                )
            }
            NATIVE_ERROR -> {
                // Keep a verified signed-in Discord account in Connected state even if the optional
                // Social SDK activity channel has not been granted presence scopes for this app id,
                // so the Settings card never falls back to "Reconnect Discord" right after approval.
                if (!DiscordRpcClient.isConnected && !hasSavedAccount) {
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
    private external fun nativeDefaultScopes(): String
    private external fun nativeBeginSignIn(): Boolean
    private external fun nativeApplyToken(accessToken: String, refreshToken: String, tokenType: Int): Boolean
    private external fun nativeRestoreSession(refreshToken: String): Boolean
    private external fun nativeDisconnect()
    private external fun nativeConnectionState(): Int
    private external fun nativeConnectionError(): String
    private external fun nativeTakeAccessToken(): String
    private external fun nativeTakeRefreshToken(): String
    private external fun nativeTakeTokenType(): Int
    private external fun nativeUpdate(
        details: String,
        state: String,
        largeText: String,
        artworkUrl: String,
        startedAtEpochSeconds: Long,
        durationMs: Long,
        playing: Boolean
    )
    private external fun nativeClear()
    private external fun nativeRelease()
}
