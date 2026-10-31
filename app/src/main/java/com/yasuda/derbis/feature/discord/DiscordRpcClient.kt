package com.yasuda.derbis.feature.discord

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pure Kotlin Discord Gateway & RPC client (OuterTune / KizzyRPC pattern).
 *
 * Implements Discord Rich Presence over Discord Gateway v10 WebSocket, supporting:
 *  1. Dual assets: high-res song album cover (`large_image`) and artist avatar icon (`small_image`)
 *  2. Real-time Connection Tester with 4-phase diagnostics (Network, REST API, Profile, Gateway)
 *  3. Direct token login and improved normal login session management
 *
 * The activity deliberately carries no buttons, timestamps or lyric line: only the track
 * title, the artist and the two artwork assets.
 */
object DiscordRpcClient {
    private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
    private const val REST_API_USER_ME = "https://discord.com/api/v10/users/@me"
    private const val DEFAULT_APP_ID = "1199347898516045884"

    /** Discord caps both `details` and `state` at 128 characters. */
    private const val ACTIVITY_TEXT_LIMIT = 128

    private fun String.activityText(): String =
        trim().replace(Regex("\\s+"), " ").take(ACTIVITY_TEXT_LIMIT)

    // Gateway Opcodes
    private const val OP_DISPATCH = 0
    private const val OP_HEARTBEAT = 1
    private const val OP_IDENTIFY = 2
    private const val OP_PRESENCE_UPDATE = 3
    private const val OP_HELLO = 10
    private const val OP_HEARTBEAT_ACK = 11

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var currentToken: String = ""
    @Volatile private var lastPresence: DerbisDiscordPresenceSnapshot? = null
    @Volatile private var isGatewayConnected = AtomicBoolean(false)
    private val sequenceNumber = AtomicInteger(-1)
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    val isConnected: Boolean
        get() = isGatewayConnected.get()

    /**
     * Connect to Discord Gateway with the authenticated token.
     */
    fun connect(token: String) {
        if (token.isBlank()) return
        if (currentToken == token && isGatewayConnected.get()) return
        currentToken = token
        startWebSocket()
    }

    fun disconnect() {
        currentToken = ""
        stopWebSocket()
    }

    private fun startWebSocket() {
        stopWebSocket()
        val request = Request.Builder()
            .url(GATEWAY_URL)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) DerbisMusic/1.1")
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("Discord Gateway WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleGatewayMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("Discord Gateway closed: $code - $reason")
                isGatewayConnected.set(false)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.w(t, "Discord Gateway error: ${response?.message}")
                isGatewayConnected.set(false)
                scheduleReconnect()
            }
        })
    }

    private fun stopWebSocket() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        isGatewayConnected.set(false)
        webSocket?.close(1000, "Derbis Disconnected")
        webSocket = null
    }

    private fun scheduleReconnect() {
        if (currentToken.isBlank() || reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(5_000L)
            if (currentToken.isNotBlank() && !isGatewayConnected.get()) {
                startWebSocket()
            }
        }
    }

    private fun handleGatewayMessage(payload: String) {
        runCatching {
            val json = JSONObject(payload)
            val op = json.optInt("op", -1)
            val s = json.optInt("s", -1)
            if (s >= 0) sequenceNumber.set(s)

            when (op) {
                OP_HELLO -> {
                    val d = json.optJSONObject("d")
                    val interval = d?.optLong("heartbeat_interval", 41250L) ?: 41250L
                    startHeartbeat(interval)
                    sendIdentify()
                }
                OP_DISPATCH -> {
                    val event = json.optString("t")
                    if (event == "READY" || event == "READY_SUPPLEMENTAL") {
                        isGatewayConnected.set(true)
                        Timber.d("Discord Gateway READY dispatch received")
                        lastPresence?.let { updatePresence(it) }
                    }
                }
                OP_HEARTBEAT -> sendHeartbeat()
                OP_HEARTBEAT_ACK -> {
                    // Heartbeat acknowledged by Discord
                }
            }
        }.onFailure { Timber.e(it, "Failed to parse Gateway message") }
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                sendHeartbeat()
            }
        }
    }

    private fun sendHeartbeat() {
        val seq = sequenceNumber.get()
        val payload = JSONObject().apply {
            put("op", OP_HEARTBEAT)
            put("d", if (seq >= 0) seq else JSONObject.NULL)
        }
        webSocket?.send(payload.toString())
    }

    private fun sendIdentify() {
        val payload = JSONObject().apply {
            put("op", OP_IDENTIFY)
            put("d", JSONObject().apply {
                put("token", currentToken)
                put("capabilities", 16381)
                put("properties", JSONObject().apply {
                    put("os", "Android")
                    put("browser", "Discord Android")
                    put("device", "Android")
                    put("system_locale", "en-US")
                    put("client_version", "210.0")
                    put("release_channel", "googleRelease")
                })
                put("presence", JSONObject().apply {
                    put("status", "online")
                    put("since", 0)
                    put("activities", JSONArray())
                    put("afk", false)
                })
            })
        }
        webSocket?.send(payload.toString())
    }

    /**
     * Updates Discord Rich Presence with the track title, artist and dual artwork assets.
     */
    internal fun updatePresence(presence: DerbisDiscordPresenceSnapshot) {
        lastPresence = presence
        if (!isGatewayConnected.get()) return

        val activities = JSONArray()
        if (presence.playing && presence.title.isNotBlank()) {
            val activity = JSONObject().apply {
                put("name", "Derbis")
                put("type", 2) // 2 = Listening to
                put("details", presence.title.activityText())
                put("state", presence.artist.ifBlank { "Unknown Artist" }.activityText())
                put("application_id", DEFAULT_APP_ID)

                // Song cover and artist icon (dual-asset)
                put("assets", JSONObject().apply {
                    if (presence.artworkUrl.isNotBlank()) {
                        put("large_image", presence.artworkUrl)
                        put("large_text", presence.title)
                    } else {
                        put("large_image", "derbis_listening")
                        put("large_text", "Listening on Derbis")
                    }
                    if (presence.artistThumbnailUrl.isNotBlank()) {
                        put("small_image", presence.artistThumbnailUrl)
                        put("small_text", presence.artist.ifBlank { "Derbis Artist" })
                    } else {
                        put("small_image", "derbis_listening")
                        put("small_text", "Derbis Music")
                    }
                })
            }
            activities.put(activity)
        }

        val presencePayload = JSONObject().apply {
            put("op", OP_PRESENCE_UPDATE)
            put("d", JSONObject().apply {
                put("since", 0)
                put("activities", activities)
                put("status", "online")
                put("afk", false)
            })
        }
        webSocket?.send(presencePayload.toString())
    }

    /**
     * Clears presence from user profile.
     */
    fun clearPresence() {
        lastPresence = null
        if (!isGatewayConnected.get()) return
        val clearPayload = JSONObject().apply {
            put("op", OP_PRESENCE_UPDATE)
            put("d", JSONObject().apply {
                put("since", 0)
                put("activities", JSONArray())
                put("status", "online")
                put("afk", false)
            })
        }
        webSocket?.send(clearPayload.toString())
    }

    /**
     * 4-phase Connection Tester (OuterTune diagnostic pattern).
     */
    suspend fun testConnection(token: String): DiscordDiagnosticResult = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext DiscordDiagnosticResult(
                success = false,
                message = "Token is empty. Please log in or paste a valid Discord token."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            // Stage 1 & 2: REST API Call to /users/@me
            val authHeader = if (token.startsWith("Bot ", ignoreCase = true) || token.startsWith("Bearer ", ignoreCase = true)) {
                token
            } else {
                token.trim()
            }

            val request = Request.Builder()
                .url(REST_API_USER_ME)
                .header("Authorization", authHeader)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) DerbisMusic/1.1")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    401 -> "Invalid or expired Discord token (HTTP 401 Unauthorized)."
                    429 -> "Discord rate limit exceeded (HTTP 429). Please wait a moment."
                    403 -> "Access forbidden (HTTP 403). Check account permissions."
                    else -> "Discord returned HTTP ${response.code}: ${response.message}"
                }
                return@withContext DiscordDiagnosticResult(
                    success = false,
                    latencyMs = latency,
                    httpCode = response.code,
                    message = errorMsg
                )
            }

            // Stage 3: Parse User Profile
            val profile = JSONObject(body)
            val username = profile.optString("username", "Discord User")
            val globalName = profile.optString("global_name", username)
            val userId = profile.optString("id", "")
            val avatarHash = profile.optString("avatar", "")
            val avatarUrl = if (userId.isNotBlank() && avatarHash.isNotBlank()) {
                "https://cdn.discordapp.com/avatars/$userId/$avatarHash.png?size=128"
            } else ""

            // Stage 4: Gateway ping status
            val gatewayStatus = if (isGatewayConnected.get()) "Active & Synced" else "Connecting"

            return@withContext DiscordDiagnosticResult(
                success = true,
                latencyMs = latency,
                httpCode = response.code,
                username = username,
                globalName = globalName,
                userId = userId,
                avatarUrl = avatarUrl,
                gatewayStatus = gatewayStatus,
                message = "Connected to Discord as @$username ($latency ms). Gateway is $gatewayStatus."
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            return@withContext DiscordDiagnosticResult(
                success = false,
                latencyMs = latency,
                message = "Network connection failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}

/**
 * Diagnostic result returned by the Connection Tester.
 */
data class DiscordDiagnosticResult(
    val success: Boolean,
    val latencyMs: Long = 0L,
    val httpCode: Int = 0,
    val username: String = "",
    val globalName: String = "",
    val userId: String = "",
    val avatarUrl: String = "",
    val gatewayStatus: String = "",
    val message: String = ""
)
