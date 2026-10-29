package com.yasuda.derbis.feature.jam

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import timber.log.Timber

/** Direct Metroserver protocol client used by the opt-in Metrolist Clients — Remote path. */
internal class MetroserverRemoteClient(
    endpoint: String,
    private val clientName: String = "com.derbis.app/1.1.1"
) {
    sealed interface Event {
        data object Connected : Event
        data class RoomCreated(val value: MetroJamProtocol.RoomCreated) : Event
        data class JoinRequested(val value: MetroJamProtocol.JoinRequest) : Event
        data class JoinApproved(val value: MetroJamProtocol.JoinApproved) : Event
        data class JoinRejected(val value: MetroJamProtocol.JoinRejected) : Event
        data class UserJoined(val value: MetroJamProtocol.JoinRequest) : Event
        data class UserLeft(val value: MetroJamProtocol.JoinRequest) : Event
        data class Sync(val value: MetroJamProtocol.Playback) : Event
        data class SyncState(val value: MetroJamProtocol.Playback) : Event
        data class Suggestion(val value: MetroJamProtocol.Suggestion) : Event
        data class Failed(val reason: String) : Event
        data object Disconnected : Event
    }

    private val url = normalizeEndpoint(endpoint)
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = EVENT_BUFFER)
    val events: Flow<Event> = _events.asSharedFlow()

    private var socket: WebSocket? = null
    private var open: CompletableDeferred<Boolean>? = null
    private var closedByCaller = false
    private var pingSequence = 0L

    val isValid: Boolean get() = url != null

    suspend fun connect(): Boolean {
        if (socket != null) return true
        val endpoint = url ?: return false
        closedByCaller = false
        val connected = CompletableDeferred<Boolean>()
        open = connected
        socket = client.newWebSocket(
            Request.Builder().url(endpoint).header("User-Agent", clientName).build(),
            listener()
        )
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) { connected.await() } == true
    }

    fun createRoom(displayName: String): Boolean = send(MetroJamProtocol.encodeCreateRoom(displayName))

    fun joinRoom(roomCode: String, displayName: String): Boolean =
        send(MetroJamProtocol.encodeJoinRoom(roomCode, displayName))

    fun approve(userId: String): Boolean = send(MetroJamProtocol.encodeApprove(userId))

    fun reject(userId: String, reason: String = "Rejected by host"): Boolean =
        send(MetroJamProtocol.encodeReject(userId, reason))

    fun publish(playback: MetroJamProtocol.Playback): Boolean = send(MetroJamProtocol.encodePlayback(playback))

    fun suggest(track: MetroJamProtocol.Track): Boolean = send(MetroJamProtocol.encodeSuggestion(track))

    fun ping(nowMs: Long): Boolean = send(MetroJamProtocol.encodePing(nowMs, ++pingSequence))

    fun leave() {
        send(MetroJamProtocol.encodeLeave())
        close()
    }

    fun close() {
        closedByCaller = true
        open?.complete(false)
        open = null
        socket?.close(1000, "leaving")
        socket = null
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun send(bytes: ByteArray): Boolean = socket?.send(ByteString.of(*bytes)) == true

    private fun listener(): WebSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (!webSocket.send(ByteString.of(*MetroJamProtocol.encodeCapabilities()))) {
                open?.complete(false)
                return
            }
            open?.complete(true)
            _events.tryEmit(Event.Connected)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            val envelope = MetroJamProtocol.decodeEnvelope(bytes.toByteArray()) ?: run {
                _events.tryEmit(Event.Failed("Invalid Metroserver envelope"))
                return
            }
            when (envelope.type) {
                MetroJamProtocol.ROOM_CREATED -> MetroJamProtocol.decodeRoomCreated(envelope.payload)?.let { _events.tryEmit(Event.RoomCreated(it)) }
                MetroJamProtocol.JOIN_REQUEST -> MetroJamProtocol.decodeJoinRequest(envelope.payload)?.let { _events.tryEmit(Event.JoinRequested(it)) }
                MetroJamProtocol.JOIN_APPROVED -> MetroJamProtocol.decodeJoinApproved(envelope.payload)?.let { _events.tryEmit(Event.JoinApproved(it)) }
                MetroJamProtocol.JOIN_REJECTED -> _events.tryEmit(Event.JoinRejected(MetroJamProtocol.decodeJoinRejected(envelope.payload)))
                MetroJamProtocol.USER_JOINED -> MetroJamProtocol.decodeUserEvent(envelope.payload)?.let { _events.tryEmit(Event.UserJoined(it)) }
                MetroJamProtocol.USER_LEFT -> MetroJamProtocol.decodeUserEvent(envelope.payload)?.let { _events.tryEmit(Event.UserLeft(it)) }
                MetroJamProtocol.SYNC_PLAYBACK -> MetroJamProtocol.decodeSyncPlayback(envelope.payload)?.let { _events.tryEmit(Event.Sync(it)) }
                MetroJamProtocol.SYNC_STATE -> MetroJamProtocol.decodeSyncState(envelope.payload)?.let { _events.tryEmit(Event.SyncState(it)) }
                MetroJamProtocol.SUGGESTION_RECEIVED -> MetroJamProtocol.decodeSuggestion(envelope.payload)?.let { _events.tryEmit(Event.Suggestion(it)) }
                MetroJamProtocol.ERROR -> MetroJamProtocol.decodeError(envelope.payload)?.let { _events.tryEmit(Event.Failed("${it.code}: ${it.message}")) }
                MetroJamProtocol.SERVER_CAPABILITIES, MetroJamProtocol.PONG -> Unit
                else -> Timber.d("Ignoring unsupported Metroserver event %s", envelope.type)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!closedByCaller) {
                open?.complete(false)
                _events.tryEmit(Event.Failed(t.message ?: "Metroserver connection failed"))
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!closedByCaller) {
                open?.complete(false)
                _events.tryEmit(Event.Disconnected)
            }
        }
    }

    companion object {
        const val CONNECT_TIMEOUT_MS = 8_000L
        private const val EVENT_BUFFER = 64

        fun normalizeEndpoint(value: String): String? {
            val trimmed = value.trim()
            if (trimmed.isBlank() || trimmed.length > 2_048) return null
            val normalized = if (trimmed.startsWith("ws://") || trimmed.startsWith("wss://")) trimmed else "wss://$trimmed"
            return normalized.takeIf { it.startsWith("ws://") || it.startsWith("wss://") }
        }
    }
}
