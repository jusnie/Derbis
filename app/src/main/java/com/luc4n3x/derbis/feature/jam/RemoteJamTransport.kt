package com.luc4n3x.derbis.feature.jam

import com.luc4n3x.derbis.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import timber.log.Timber

/**
 * Production relay settings are injected at build time. A remote share code deliberately uses a
 * reserved private-address marker so it keeps the established QR/deep-link format without leaking
 * a real device address. The relay only keys the room by its high-entropy secret.
 */
internal object DerbisJamRelay {
    private const val REMOTE_ADDRESS = "10.254.254.254"
    private const val REMOTE_PORT = 54_443
    // Render's free service can need roughly a minute to wake after an idle spin-down.
    // Only creating a room needs that longer budget; guests normally join an already-warm relay.
    private const val HOST_CONNECT_TIMEOUT_MS = 75_000L
    private const val GUEST_CONNECT_TIMEOUT_MS = 15_000L
    private const val APPROVAL_TIMEOUT_MS = 120_000L
    private const val EVENT_BUFFER = 64

    val endpoint: String
        get() = BuildConfig.JAM_RELAY_URL.trim()

    val isConfigured: Boolean
        get() = endpoint.startsWith("wss://", ignoreCase = true) || endpoint.startsWith("ws://", ignoreCase = true)

    fun codeFor(secret: String): JamSessionCode = JamSessionCode(REMOTE_ADDRESS, REMOTE_PORT, secret)

    fun isRemoteCode(code: JamSessionCode): Boolean =
        code.hostAddress == REMOTE_ADDRESS && code.port == REMOTE_PORT

    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(GUEST_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    fun request(): Request? = endpoint
        .takeIf { isConfigured }
        ?.let { Request.Builder().url(it).build() }

    fun relayFailure(reason: String?): JamFailure = when (reason) {
        "room_full" -> JamFailure.SessionFull
        "host_unavailable", "host_exists" -> JamFailure.ConnectionFailed
        else -> JamFailure.ConnectionFailed
    }

    fun envelope(type: String, room: String? = null, payload: String? = null, target: String? = null): String =
        JSONObject().apply {
            put("type", type)
            if (room != null) put("room", room)
            if (payload != null) put("payload", payload)
            if (target != null) put("to", target)
        }.toString()

    fun hello(room: String, role: String): String = JSONObject().apply {
        put("type", "hello")
        put("room", room)
        put("role", role)
    }.toString()

    fun parse(raw: String): JSONObject? = runCatching { JSONObject(raw) }.getOrNull()

    const val HOST_CONNECT_TIMEOUT: Long = HOST_CONNECT_TIMEOUT_MS
    const val GUEST_CONNECT_TIMEOUT: Long = GUEST_CONNECT_TIMEOUT_MS
    const val APPROVAL_TIMEOUT: Long = APPROVAL_TIMEOUT_MS
    const val BUFFER: Int = EVENT_BUFFER
}

/** A remote host still owns authorization, playback state, and the cryptographic guest challenge. */
internal class RemoteJamHostTransport : JamHostTransport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<JamHostEvent>(extraBufferCapacity = DerbisJamRelay.BUFFER)
    override val events: Flow<JamHostEvent> = _events.asSharedFlow()

    private val pending = linkedMapOf<String, PendingGuest>()
    private val connected = linkedSetOf<String>()
    private var client: OkHttpClient? = null
    private var socket: WebSocket? = null
    private var ready: CompletableDeferred<Boolean>? = null
    private var secret: String = ""
    private var stopping = false

    private data class PendingGuest(
        val guestId: String,
        val name: String,
        val hostProof: String,
        val expiry: Job
    )

    override suspend fun start(secret: String): JamSessionCode? {
        if (!JamSessionCode.isValidSecret(secret) || !DerbisJamRelay.isConfigured) return null
        stop()
        this.secret = secret
        stopping = false
        val request = DerbisJamRelay.request() ?: return null
        val opened = CompletableDeferred<Boolean>()
        ready = opened
        val httpClient = DerbisJamRelay.client()
        client = httpClient
        socket = httpClient.newWebSocket(request, listener())
        val accepted = withTimeoutOrNull(DerbisJamRelay.HOST_CONNECT_TIMEOUT) { opened.await() } == true
        if (!accepted) {
            stop()
            return null
        }
        return DerbisJamRelay.codeFor(secret)
    }

    override suspend fun notifyPending(participantId: String, message: JamMessage.Pending) {
        if (participantId !in pending) return
        sendTo(participantId, JamProtocol.encode(message))
    }

    override suspend fun admit(participantId: String, welcome: JamMessage.Welcome): Boolean {
        val entry = pending.remove(participantId) ?: return false
        entry.expiry.cancel()
        val sent = sendTo(participantId, JamProtocol.encode(welcome))
        if (sent) connected += participantId else _events.tryEmit(JamHostEvent.GuestLeft(participantId))
        return sent
    }

    override suspend fun reject(participantId: String, failure: JamFailure) {
        pending.remove(participantId)?.expiry?.cancel()
        val wasKnown = connected.remove(participantId)
        sendTo(participantId, JamProtocol.encode(JamMessage.Failure(failure)))
        if (wasKnown) _events.tryEmit(JamHostEvent.GuestLeft(participantId))
    }

    override suspend fun broadcast(message: JamMessage) {
        val payload = JamProtocol.encode(message)
        connected.toList().forEach { participantId -> sendTo(participantId, payload) }
    }

    override suspend fun send(participantId: String, message: JamMessage) {
        if (participantId in pending || participantId in connected) sendTo(participantId, JamProtocol.encode(message))
    }

    override suspend fun disconnect(participantId: String) {
        val wasPending = pending.remove(participantId)?.also { it.expiry.cancel() } != null
        val wasConnected = connected.remove(participantId)
        if (wasPending || wasConnected) {
            sendTo(participantId, JamProtocol.encode(JamMessage.Bye("removed")))
            _events.tryEmit(JamHostEvent.GuestLeft(participantId))
        }
    }

    override fun stop() {
        stopping = true
        pending.values.forEach { it.expiry.cancel() }
        pending.clear()
        connected.clear()
        ready?.complete(false)
        ready = null
        socket?.close(1000, "leaving")
        socket = null
        client?.dispatcher?.executorService?.shutdown()
        client?.connectionPool?.evictAll()
        client = null
        secret = ""
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun listener(): WebSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (!webSocket.send(DerbisJamRelay.hello(room = secret, role = "host"))) {
                ready?.complete(false)
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val message = DerbisJamRelay.parse(text) ?: return
            when (message.optString("type")) {
                "ready" -> ready?.complete(true)
                "failure" -> {
                    ready?.complete(false)
                    _events.tryEmit(JamHostEvent.Failed(DerbisJamRelay.relayFailure(message.optString("reason"))))
                }
                "peer_joined" -> {
                    message.optString("clientId").takeIf { it.isNotBlank() }?.let(::beginGuestHandshake)
                }
                "peer_left" -> {
                    message.optString("clientId").takeIf { it.isNotBlank() }?.let(::removePeer)
                }
                "message" -> receiveGuestPayload(
                    participantId = message.optString("from"),
                    payload = message.optString("payload")
                )
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!stopping) {
                Timber.w(t, "Derbis Jam relay host disconnected")
                ready?.complete(false)
                _events.tryEmit(JamHostEvent.Failed(JamFailure.ConnectionFailed))
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!stopping) {
                ready?.complete(false)
                _events.tryEmit(JamHostEvent.Failed(JamFailure.ConnectionFailed))
            }
        }
    }

    private fun beginGuestHandshake(participantId: String) {
        if (participantId in pending || participantId in connected || pending.size + connected.size >= JamSessionState.MAX_PARTICIPANTS - 1) return
        val hostNonce = JamAuth.generateNonce()
        if (!sendTo(participantId, JamProtocol.encode(JamMessage.Challenge(hostNonce)))) return
        handshakeNonces[participantId] = hostNonce
    }

    private val handshakeNonces = linkedMapOf<String, String>()

    private fun receiveGuestPayload(participantId: String, payload: String) {
        if (participantId.isBlank() || payload.isBlank()) return
        when (val message = JamProtocol.decode(payload)) {
            is JamMessage.Authenticate -> authenticateGuest(participantId, message)
            is JamMessage.Action -> if (participantId in connected) {
                _events.tryEmit(JamHostEvent.ActionReceived(participantId, message.action))
            }
            is JamMessage.Bye -> removePeer(participantId)
            else -> Unit
        }
    }

    private fun authenticateGuest(participantId: String, message: JamMessage.Authenticate) {
        val hostNonce = handshakeNonces.remove(participantId) ?: return
        val expectedProof = JamAuth.computeGuestProof(secret, hostNonce, message.guestNonce)
        if (!JamAuth.verifyProof(expectedProof, message.proof)) {
            sendTo(participantId, JamProtocol.encode(JamMessage.Failure(JamFailure.NotAuthorized)))
            return
        }
        val hostProof = JamAuth.computeHostProof(secret, hostNonce, message.guestNonce)
        val expiry = scope.launch {
            delay(DerbisJamRelay.APPROVAL_TIMEOUT)
            val stale = pending.remove(participantId) ?: return@launch
            sendTo(participantId, JamProtocol.encode(JamMessage.Failure(JamFailure.Rejected)))
            _events.tryEmit(JamHostEvent.GuestLeft(participantId))
            stale.expiry.cancel()
        }
        pending[participantId] = PendingGuest(
            guestId = JamIdentity.sanitize(message.guestId),
            name = JamProtocol.sanitizeName(message.name),
            hostProof = hostProof,
            expiry = expiry
        )
        _events.tryEmit(
            JamHostEvent.GuestPending(
                participantId = participantId,
                guestId = JamIdentity.sanitize(message.guestId),
                name = JamProtocol.sanitizeName(message.name),
                hostProof = hostProof
            )
        )
    }

    private fun removePeer(participantId: String) {
        handshakeNonces.remove(participantId)
        val wasPending = pending.remove(participantId)?.also { it.expiry.cancel() } != null
        val wasConnected = connected.remove(participantId)
        if (wasPending || wasConnected) _events.tryEmit(JamHostEvent.GuestLeft(participantId))
    }

    private fun sendTo(participantId: String, payload: String): Boolean =
        socket?.send(DerbisJamRelay.envelope(type = "message", payload = payload, target = participantId)) == true
}

/** Remote guest transport; no media is routed through the relay, only synchronized Jam messages. */
internal class RemoteJamGuestTransport : JamGuestTransport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<JamGuestEvent>(extraBufferCapacity = DerbisJamRelay.BUFFER)
    override val events: Flow<JamGuestEvent> = _events.asSharedFlow()

    private var client: OkHttpClient? = null
    private var socket: WebSocket? = null
    private var connectedDeferred: CompletableDeferred<Boolean>? = null
    private var expectedHostProof: String = ""
    private var guestNonce: String = ""
    private var sessionId: String = ""
    private var participantId: String = ""
    private var stopping = false
    private var joined = false

    override suspend fun connect(code: JamSessionCode, name: String, guestId: String): Boolean {
        if (!DerbisJamRelay.isConfigured || !DerbisJamRelay.isRemoteCode(code) || !JamSessionCode.isValidSecret(code.secret)) {
            _events.tryEmit(JamGuestEvent.Failed(JamFailure.InvalidCode))
            return false
        }
        stop()
        stopping = false
        expectedHostProof = ""
        guestNonce = ""
        sessionId = ""
        participantId = ""
        joined = false
        val request = DerbisJamRelay.request() ?: return false
        val waiting = CompletableDeferred<Boolean>()
        connectedDeferred = waiting
        val httpClient = DerbisJamRelay.client()
        client = httpClient
        socket = httpClient.newWebSocket(request, guestListener(code.secret, name, guestId))
        val connected = withTimeoutOrNull(DerbisJamRelay.GUEST_CONNECT_TIMEOUT) { waiting.await() } == true
        if (!connected) stop()
        return connected
    }

    override suspend fun send(message: JamMessage) {
        socket?.send(DerbisJamRelay.envelope(type = "message", payload = JamProtocol.encode(message)))
    }

    override fun stop() {
        stopping = true
        connectedDeferred?.complete(false)
        connectedDeferred = null
        socket?.close(1000, "leaving")
        socket = null
        client?.dispatcher?.executorService?.shutdown()
        client?.connectionPool?.evictAll()
        client = null
        joined = false
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun guestListener(secret: String, name: String, guestId: String): WebSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (!webSocket.send(DerbisJamRelay.hello(room = secret, role = "guest"))) {
                connectedDeferred?.complete(false)
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val envelope = DerbisJamRelay.parse(text) ?: return
            when (envelope.optString("type")) {
                "failure" -> fail(DerbisJamRelay.relayFailure(envelope.optString("reason")))
                "message" -> receiveHostPayload(envelope.optString("payload"), secret, name, guestId)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!stopping) {
                Timber.w(t, "Derbis Jam relay guest disconnected")
                fail(JamFailure.ConnectionFailed)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!stopping) fail(JamFailure.ConnectionFailed)
        }
    }

    private fun receiveHostPayload(payload: String, secret: String, name: String, guestId: String) {
        when (val message = JamProtocol.decode(payload)) {
            is JamMessage.Challenge -> {
                guestNonce = JamAuth.generateNonce()
                expectedHostProof = JamAuth.computeHostProof(secret, message.hostNonce, guestNonce)
                val proof = JamAuth.computeGuestProof(secret, message.hostNonce, guestNonce)
                socket?.send(
                    DerbisJamRelay.envelope(
                        type = "message",
                        payload = JamProtocol.encode(
                            JamMessage.Authenticate(
                                guestNonce = guestNonce,
                                name = JamProtocol.sanitizeName(name),
                                proof = proof,
                                guestId = JamIdentity.sanitize(guestId)
                            )
                        )
                    )
                )
            }
            is JamMessage.Pending -> {
                if (!JamAuth.verifyProof(expectedHostProof, message.hostProof)) {
                    fail(JamFailure.NotAuthorized)
                    return
                }
                joined = true
                connectedDeferred?.complete(true)
                _events.tryEmit(JamGuestEvent.AwaitingApproval)
            }
            is JamMessage.Welcome -> {
                if (!JamAuth.verifyProof(expectedHostProof, message.hostProof)) {
                    fail(JamFailure.NotAuthorized)
                    return
                }
                joined = true
                sessionId = message.sessionId
                participantId = message.participantId
                connectedDeferred?.complete(true)
                _events.tryEmit(JamGuestEvent.Connected(sessionId, participantId))
            }
            is JamMessage.State -> if (joined) _events.tryEmit(JamGuestEvent.StateReceived(message))
            is JamMessage.Failure -> fail(message.failure)
            is JamMessage.Bye -> fail(JamFailure.HostEnded)
            null -> fail(JamFailure.ProtocolError)
            else -> Unit
        }
    }

    private fun fail(failure: JamFailure) {
        val wasJoined = joined
        joined = false
        connectedDeferred?.complete(false)
        _events.tryEmit(JamGuestEvent.Failed(failure))
        if (wasJoined) _events.tryEmit(JamGuestEvent.Disconnected)
    }
}

/** Uses remote rooms whenever a relay URL is injected, while retaining LAN Jam as a safe fallback. */
internal object DerbisJamTransportFactory : JamTransportFactory {
    override fun host(): JamHostTransport = if (DerbisJamRelay.isConfigured) RemoteJamHostTransport() else LanJamHostTransport()

    override fun guest(): JamGuestTransport = AdaptiveJamGuestTransport()
}

private class AdaptiveJamGuestTransport : JamGuestTransport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<JamGuestEvent>(extraBufferCapacity = DerbisJamRelay.BUFFER)
    override val events: Flow<JamGuestEvent> = _events.asSharedFlow()
    private var delegate: JamGuestTransport? = null
    private var eventJob: Job? = null

    override suspend fun connect(code: JamSessionCode, name: String, guestId: String): Boolean {
        stop()
        val selected = if (DerbisJamRelay.isConfigured && DerbisJamRelay.isRemoteCode(code)) {
            RemoteJamGuestTransport()
        } else {
            LanJamGuestTransport()
        }
        delegate = selected
        eventJob = scope.launch { selected.events.collect { _events.emit(it) } }
        return selected.connect(code, name, guestId)
    }

    override suspend fun send(message: JamMessage) {
        delegate?.send(message)
    }

    override fun stop() {
        eventJob?.cancel()
        eventJob = null
        delegate?.stop()
        delegate = null
    }
}
