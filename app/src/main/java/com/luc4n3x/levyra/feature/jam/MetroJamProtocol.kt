package com.luc4n3x.levyra.feature.jam

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Small protobuf implementation for the documented Metroserver Listen Together wire format.
 * It deliberately implements only the messages Derbis needs and does not use Metrolist source,
 * generated classes, branding, or a Metrolist User-Agent.
 */
internal object MetroJamProtocol {
    const val CLIENT_CAPABILITIES = "client_capabilities"
    const val SERVER_CAPABILITIES = "server_capabilities"
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val LEAVE_ROOM = "leave_room"
    const val APPROVE_JOIN = "approve_join"
    const val REJECT_JOIN = "reject_join"
    const val PLAYBACK_ACTION = "playback_action"
    const val SUGGEST_TRACK = "suggest_track"
    const val PING = "ping"

    const val ROOM_CREATED = "room_created"
    const val JOIN_REQUEST = "join_request"
    const val JOIN_APPROVED = "join_approved"
    const val JOIN_REJECTED = "join_rejected"
    const val USER_JOINED = "user_joined"
    const val USER_LEFT = "user_left"
    const val SYNC_PLAYBACK = "sync_playback"
    const val SYNC_STATE = "sync_state"
    const val SUGGESTION_RECEIVED = "suggestion_received"
    const val ERROR = "error"
    const val PONG = "pong"

    const val ACTION_PLAY = "play"
    const val ACTION_PAUSE = "pause"
    const val ACTION_SEEK = "seek"
    const val ACTION_CHANGE_TRACK = "change_track"
    const val ACTION_SYNC_QUEUE = "sync_queue"

    data class Envelope(
        val type: String,
        val payload: ByteArray,
        val compressed: Boolean = false,
        val requestId: Long = 0L
    )

    data class Track(
        val id: String,
        val title: String,
        val artist: String,
        val album: String = "",
        val durationMs: Long = 0L,
        val thumbnail: String = "",
        val suggestedBy: String = ""
    )

    data class User(val id: String, val name: String, val host: Boolean, val connected: Boolean = true)

    data class RoomState(
        val code: String,
        val hostId: String,
        val users: List<User>,
        val current: Track? = null,
        val playing: Boolean = false,
        val positionMs: Long = 0L,
        val updatedAtMs: Long = 0L,
        val queue: List<Track> = emptyList(),
        val revision: Long = 0L,
        val queueTitle: String = ""
    )

    data class Playback(
        val action: String,
        val trackId: String = "",
        val positionMs: Long = 0L,
        val track: Track? = null,
        val queue: List<Track> = emptyList(),
        val playingRevision: Long = 0L,
        val updatedAtMs: Long = 0L,
        val queueTitle: String = ""
    )

    data class RoomCreated(val roomCode: String, val userId: String, val sessionToken: String)
    data class JoinRequest(val userId: String, val userName: String)
    data class JoinApproved(val roomCode: String, val userId: String, val sessionToken: String, val state: RoomState?)
    data class JoinRejected(val reason: String)
    data class Error(val code: String, val message: String)
    data class Suggestion(val id: String, val fromUserId: String, val fromName: String, val track: Track?)

    fun encodeCapabilities(): ByteArray = envelope(CLIENT_CAPABILITIES, ProtoWriter().apply {
        bool(1, true)
        bool(2, true)
        string(3, "Derbis/1.0.3")
        string(4, "replace_queue")
        string(4, "host_sequence")
        string(4, "track_revision")
    }.bytes())

    fun encodeCreateRoom(name: String): ByteArray = envelope(CREATE_ROOM, ProtoWriter().string(1, name).bytes())

    fun encodeJoinRoom(roomCode: String, name: String): ByteArray = envelope(
        JOIN_ROOM,
        ProtoWriter().string(1, roomCode.uppercase()).string(2, name).bytes()
    )

    fun encodeLeave(): ByteArray = envelope(LEAVE_ROOM, byteArrayOf())

    fun encodeApprove(userId: String): ByteArray = envelope(APPROVE_JOIN, ProtoWriter().string(1, userId).bytes())

    fun encodeReject(userId: String, reason: String): ByteArray = envelope(
        REJECT_JOIN,
        ProtoWriter().string(1, userId).string(2, reason).bytes()
    )

    fun encodeSuggestion(track: Track): ByteArray = envelope(
        SUGGEST_TRACK,
        ProtoWriter().message(1, encodeTrack(track)).bytes()
    )

    fun encodePing(clientTimeMs: Long, sequence: Long): ByteArray = envelope(
        PING,
        ProtoWriter().int64(1, clientTimeMs).int64(2, sequence).bytes()
    )

    fun encodePlayback(playback: Playback): ByteArray = envelope(PLAYBACK_ACTION, ProtoWriter().apply {
        string(1, playback.action)
        string(2, playback.trackId)
        int64(3, playback.positionMs)
        playback.track?.let { message(4, encodeTrack(it)) }
        playback.queue.forEach { message(6, encodeTrack(it)) }
        string(7, playback.queueTitle)
        int64(9, playback.updatedAtMs)
        int64(10, playback.playingRevision)
        int64(11, playback.updatedAtMs)
        bool(12, playback.action == ACTION_CHANGE_TRACK)
    }.bytes())

    fun decodeEnvelope(data: ByteArray): Envelope? = runCatching {
        val fields = ProtoReader(data)
        var type = ""
        var payload = byteArrayOf()
        var compressed = false
        var requestId = 0L
        while (fields.hasNext()) {
            when (fields.fieldNumber()) {
                1 -> type = fields.string()
                2 -> payload = fields.bytes()
                3 -> compressed = fields.bool()
                4 -> requestId = fields.int64()
                else -> fields.skip()
            }
        }
        if (type.isBlank()) return@runCatching null
        Envelope(type, if (compressed) decompress(payload) else payload, compressed, requestId)
    }.getOrNull()

    fun decodeRoomCreated(data: ByteArray): RoomCreated? = runCatching {
        val input = ProtoReader(data)
        var code = ""
        var userId = ""
        var token = ""
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> code = input.string()
            2 -> userId = input.string()
            3 -> token = input.string()
            else -> input.skip()
        }
        RoomCreated(code, userId, token).takeIf { it.roomCode.isNotBlank() && it.userId.isNotBlank() }
    }.getOrNull()

    fun decodeJoinRequest(data: ByteArray): JoinRequest? = runCatching {
        val input = ProtoReader(data)
        var id = ""
        var name = ""
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> id = input.string()
            2 -> name = input.string()
            else -> input.skip()
        }
        JoinRequest(id, name).takeIf { it.userId.isNotBlank() }
    }.getOrNull()

    fun decodeJoinApproved(data: ByteArray): JoinApproved? = runCatching {
        val input = ProtoReader(data)
        var room = ""
        var user = ""
        var token = ""
        var state: RoomState? = null
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> room = input.string()
            2 -> user = input.string()
            3 -> token = input.string()
            4 -> state = decodeRoomState(input.bytes())
            else -> input.skip()
        }
        JoinApproved(room, user, token, state).takeIf { it.roomCode.isNotBlank() && it.userId.isNotBlank() }
    }.getOrNull()

    fun decodeJoinRejected(data: ByteArray): JoinRejected = runCatching {
        val input = ProtoReader(data)
        var reason = ""
        while (input.hasNext()) if (input.fieldNumber() == 1) reason = input.string() else input.skip()
        JoinRejected(reason)
    }.getOrDefault(JoinRejected(""))

    fun decodeUser(data: ByteArray): User? = runCatching {
        val input = ProtoReader(data)
        var id = ""
        var name = ""
        var host = false
        var connected = true
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> id = input.string()
            2 -> name = input.string()
            3 -> host = input.bool()
            4 -> connected = input.bool()
            else -> input.skip()
        }
        User(id, name, host, connected).takeIf { it.id.isNotBlank() }
    }.getOrNull()

    fun decodeUserEvent(data: ByteArray): JoinRequest? = runCatching {
        val input = ProtoReader(data)
        var id = ""
        var name = ""
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> id = input.string()
            2 -> name = input.string()
            else -> input.skip()
        }
        JoinRequest(id, name).takeIf { it.userId.isNotBlank() }
    }.getOrNull()

    fun decodeSyncPlayback(data: ByteArray): Playback? = decodePlayback(data)

    fun decodeSyncState(data: ByteArray): Playback? = runCatching {
        val input = ProtoReader(data)
        var current: Track? = null
        var playing = false
        var position = 0L
        var updated = 0L
        val queue = mutableListOf<Track>()
        var revision = 0L
        var title = ""
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> current = decodeTrack(input.bytes())
            2 -> playing = input.bool()
            3 -> position = input.int64()
            4 -> updated = input.int64()
            5 -> decodeTrack(input.bytes())?.let(queue::add)
            7 -> revision = input.int64()
            8 -> title = input.string()
            else -> input.skip()
        }
        Playback(if (playing) ACTION_PLAY else ACTION_PAUSE, current?.id.orEmpty(), position, current, queue, revision, updated, title)
    }.getOrNull()

    fun decodeSuggestion(data: ByteArray): Suggestion? = runCatching {
        val input = ProtoReader(data)
        var id = ""
        var userId = ""
        var name = ""
        var track: Track? = null
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> id = input.string()
            2 -> userId = input.string()
            3 -> name = input.string()
            4 -> track = decodeTrack(input.bytes())
            else -> input.skip()
        }
        Suggestion(id, userId, name, track).takeIf { it.fromUserId.isNotBlank() }
    }.getOrNull()

    fun decodeError(data: ByteArray): Error? = runCatching {
        val input = ProtoReader(data)
        var code = ""
        var message = ""
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> code = input.string()
            2 -> message = input.string()
            else -> input.skip()
        }
        Error(code, message)
    }.getOrNull()

    fun roomStateFrom(playback: Playback, previous: RoomState): RoomState {
        val queue = playback.queue.ifEmpty { previous.queue }
        val current = playback.track ?: queue.firstOrNull { it.id == playback.trackId } ?: previous.current
        return previous.copy(
            current = current,
            playing = playback.action != ACTION_PAUSE,
            positionMs = playback.positionMs.coerceAtLeast(0L),
            updatedAtMs = playback.updatedAtMs.coerceAtLeast(previous.updatedAtMs),
            queue = queue,
            revision = maxOf(previous.revision, playback.playingRevision),
            queueTitle = playback.queueTitle.ifBlank { previous.queueTitle }
        )
    }

    private fun decodePlayback(data: ByteArray): Playback? = runCatching {
        val input = ProtoReader(data)
        var action = ""
        var id = ""
        var position = 0L
        var track: Track? = null
        val queue = mutableListOf<Track>()
        var revision = 0L
        var serverTime = 0L
        var title = ""
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> action = input.string()
            2 -> id = input.string()
            3 -> position = input.int64()
            4 -> track = decodeTrack(input.bytes())
            6 -> decodeTrack(input.bytes())?.let(queue::add)
            7 -> title = input.string()
            9, 11 -> serverTime = input.int64()
            10 -> revision = input.int64()
            else -> input.skip()
        }
        Playback(action, id, position, track, queue, revision, serverTime, title).takeIf { it.action.isNotBlank() }
    }.getOrNull()

    private fun decodeRoomState(data: ByteArray): RoomState? = runCatching {
        val input = ProtoReader(data)
        var code = ""
        var host = ""
        val users = mutableListOf<User>()
        var current: Track? = null
        var playing = false
        var position = 0L
        var update = 0L
        val queue = mutableListOf<Track>()
        var revision = 0L
        var title = ""
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> code = input.string()
            2 -> host = input.string()
            3 -> decodeUser(input.bytes())?.let(users::add)
            4 -> current = decodeTrack(input.bytes())
            5 -> playing = input.bool()
            6 -> position = input.int64()
            7 -> update = input.int64()
            9 -> decodeTrack(input.bytes())?.let(queue::add)
            10 -> revision = input.int64()
            11 -> title = input.string()
            else -> input.skip()
        }
        RoomState(code, host, users, current, playing, position, update, queue, revision, title)
            .takeIf { it.code.isNotBlank() && it.hostId.isNotBlank() }
    }.getOrNull()

    private fun encodeTrack(track: Track): ByteArray = ProtoWriter().apply {
        string(1, track.id)
        string(2, track.title)
        string(3, track.artist)
        string(4, track.album)
        int64(5, track.durationMs.coerceAtLeast(0L))
        string(6, track.thumbnail)
        string(7, track.suggestedBy)
    }.bytes()

    private fun decodeTrack(data: ByteArray): Track? = runCatching {
        val input = ProtoReader(data)
        var id = ""
        var title = ""
        var artist = ""
        var album = ""
        var duration = 0L
        var thumbnail = ""
        var suggested = ""
        while (input.hasNext()) when (input.fieldNumber()) {
            1 -> id = input.string()
            2 -> title = input.string()
            3 -> artist = input.string()
            4 -> album = input.string()
            5 -> duration = input.int64()
            6 -> thumbnail = input.string()
            7 -> suggested = input.string()
            else -> input.skip()
        }
        Track(id, title, artist, album, duration, thumbnail, suggested).takeIf { it.id.isNotBlank() }
    }.getOrNull()

    private fun envelope(type: String, payload: ByteArray): ByteArray = ProtoWriter().apply {
        string(1, type)
        bytes(2, payload)
        bool(3, false)
    }.bytes()

    private fun decompress(input: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(input)).use { it.readBytes() }

    internal fun compressForTest(input: ByteArray): ByteArray = ByteArrayOutputStream().use { output ->
        GZIPOutputStream(output).use { it.write(input) }
        output.toByteArray()
    }
}

internal class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun string(field: Int, value: String): ProtoWriter = bytes(field, value.toByteArray(Charsets.UTF_8))
    fun bool(field: Int, value: Boolean): ProtoWriter = int64(field, if (value) 1L else 0L)
    fun int64(field: Int, value: Long): ProtoWriter {
        tag(field, 0)
        varint(value)
        return this
    }
    fun bytes(field: Int, value: ByteArray): ProtoWriter {
        tag(field, 2)
        varint(value.size.toLong())
        out.write(value)
        return this
    }
    fun message(field: Int, value: ByteArray): ProtoWriter = bytes(field, value)
    fun bytes(): ByteArray = out.toByteArray()

    private fun tag(field: Int, wire: Int) = varint(((field shl 3) or wire).toLong())
    private fun varint(value: Long) {
        var remaining = value
        while (remaining and -128L != 0L) {
            out.write(((remaining and 127L) or 128L).toInt())
            remaining = remaining ushr 7
        }
        out.write(remaining.toInt())
    }
}

internal class ProtoReader(private val data: ByteArray) {
    private var index = 0
    private var wire = -1

    fun hasNext(): Boolean = index < data.size
    fun fieldNumber(): Int {
        val tag = varint().toInt()
        wire = tag and 7
        return tag ushr 3
    }
    fun int64(): Long = requireWire(0).let { varint() }
    fun bool(): Boolean = int64() != 0L
    fun string(): String = bytes().toString(Charsets.UTF_8)
    fun bytes(): ByteArray {
        requireWire(2)
        val length = varint().toInt()
        require(length >= 0 && length <= MAX_LENGTH && index + length <= data.size)
        return data.copyOfRange(index, index + length).also { index += length }
    }
    fun skip() {
        when (wire) {
            0 -> varint()
            1 -> advance(8)
            2 -> advance(varint().toInt())
            5 -> advance(4)
            else -> throw IllegalArgumentException("Unsupported protobuf wire type $wire")
        }
    }
    private fun requireWire(expected: Int) { require(wire == expected) { "Expected wire $expected, got $wire" } }
    private fun advance(amount: Int) {
        require(amount >= 0 && index + amount <= data.size)
        index += amount
    }
    private fun varint(): Long {
        var value = 0L
        var shift = 0
        while (shift < 64) {
            require(index < data.size)
            val next = data[index++].toInt() and 255
            value = value or ((next and 127).toLong() shl shift)
            if (next and 128 == 0) return value
            shift += 7
        }
        throw IllegalArgumentException("Invalid protobuf varint")
    }
    private companion object { const val MAX_LENGTH = 512 * 1024 }
}
