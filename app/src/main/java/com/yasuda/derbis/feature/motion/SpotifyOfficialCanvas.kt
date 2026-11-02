package com.yasuda.derbis.feature.motion

import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * Spotify's own Canvas, fetched the way the web player fetches it.
 *
 * There is no public API. The web player posts a tiny protobuf to
 * `spclient.wg.spotify.com/canvaz-cache/v0/canvases` carrying the track URI and gets back a
 * `*.cnvs.mp4` on Spotify's CDN. That endpoint wants a bearer minted from the listener's session
 * ([SpotifyTokenProvider]) plus a client token, and it gates by user agent — the web player's own
 * UA is turned away, so the request wears a mobile client's instead.
 *
 * Field numbers are Spotify's, ported from BitChord (`data/canvas/SpotifyCanvas.kt`): get them
 * wrong and the response parses to nothing instead of failing loudly, which is why there is a
 * regex behind the structured parse. The wire format is hand-rolled — two strings in, a handful
 * out, and pulling in protoc for that is not worth a build dependency.
 */
internal object SpotifyOfficialCanvas {
    private const val CANVAS_URL = "https://spclient.wg.spotify.com/canvaz-cache/v0/canvases"
    private const val APP_UA = "Spotify/9.0.34.593 iOS/18.4 (iPhone15,3)"
    private const val MAX_ATTEMPTS = 2

    private val CNVS_REGEX = Regex("""https://[^"'\s\x00-\x1F]+\.cnvs\.mp4""")
    private val TRACK_ID = Regex("^[A-Za-z0-9]{22}$")

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(16, TimeUnit.SECONDS)
        .build()

    /** Canvas CDN links do not rotate, so one hit per track per process is enough. */
    private val urlCache = ConcurrentHashMap<String, String>()

    sealed interface Result {
        data class Found(val url: String) : Result
        data object NoCanvas : Result
        data class Failed(val reason: String) : Result
    }

    /** Convenience for providers: the URL, or null for every kind of miss. */
    suspend fun canvasUrl(trackId: String, forceRefresh: Boolean = false): String? {
        val id = normalizeTrackId(trackId) ?: return null
        if (!forceRefresh) urlCache[id]?.let { return it }
        return when (val result = probe(id)) {
            is Result.Found -> {
                urlCache[id] = result.url
                result.url
            }
            else -> null
        }
    }

    /**
     * The same lookup, but keeping the reason — the Spotify session screen's "Test session" uses
     * this to tell "signed in, this track has no Canvas" apart from "the session was rejected".
     */
    suspend fun probe(trackId: String): Result {
        val id = normalizeTrackId(trackId)
            ?: return Result.Failed("That is not a Spotify track id.")
        urlCache[id]?.let { return Result.Found(it) }

        var lastReason = "no session"
        repeat(MAX_ATTEMPTS) { attempt ->
            val token = SpotifyTokenProvider.accessToken()
                ?: return Result.Failed(
                    if (com.yasuda.derbis.data.spotify.DerbisSpotifySession.hasSession()) {
                        // The cookie is there; the web player just refused to mint a token from it.
                        "Spotify did not mint a token for that session. It has probably expired — sign in again."
                    } else {
                        "No Spotify session yet. Sign in or paste an sp_dc first."
                    }
                )
            val clientToken = SpotifyTokenProvider.clientToken()
            val trackUri = "spotify:track:$id"

            val response = runCatching {
                withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(CANVAS_URL)
                        .post(encodeRequest(trackUri).toRequestBody("application/protobuf".toMediaType()))
                        .header("Authorization", "Bearer $token")
                        .apply { clientToken?.let { header("Client-Token", it) } }
                        .header("Accept", "application/protobuf")
                        .header("Accept-Language", "en")
                        .header("User-Agent", APP_UA)
                        .build()
                    http.newCall(request).execute().use { result ->
                        result.code to if (result.isSuccessful) result.body?.bytes() else null
                    }
                }
            }.onFailure { Timber.d(it, "canvaz-cache request failed") }.getOrNull()
                ?: return Result.Failed("Could not reach Spotify.")

            val (code, bytes) = response
            when {
                code == 401 || code == 403 -> {
                    // The session is fine, the token was not (or was minted from an older cookie).
                    SpotifyTokenProvider.invalidate()
                    lastReason = if (attempt == 0) "rejected token, retried" else "Spotify rejected the session ($code)"
                }
                code == 404 || code == 204 -> return Result.NoCanvas
                code !in 200..299 -> {
                    lastReason = "Spotify answered $code"
                }
                bytes == null || bytes.isEmpty() -> return Result.NoCanvas
                else -> {
                    val url = decodeResponse(bytes, trackUri)
                    if (url == null) return Result.NoCanvas
                    urlCache[id] = url
                    return Result.Found(url)
                }
            }
        }
        return Result.Failed(lastReason)
    }

    fun invalidate() {
        urlCache.clear()
    }

    private fun normalizeTrackId(raw: String): String? {
        val cleaned = raw.trim().removePrefix("spotify:track:").removePrefix("spotify:")
        return cleaned.takeIf { TRACK_ID.matches(it) }
    }

    /** `CanvasRequest { repeated Track tracks = 1; }`, `Track { string track_uri = 1; }` */
    private fun encodeRequest(trackUri: String): ByteArray {
        val track = ByteArrayOutputStream().also { writeString(it, 1, trackUri) }
        val request = ByteArrayOutputStream().also { writeBytes(it, 1, track.toByteArray()) }
        return request.toByteArray()
    }

    /**
     * `CanvasResponse { repeated Canvas canvases = 1; }`,
     * `Canvas { id = 1; canvas_url = 2; …; track_uri = 5; … }`. Only the two fields this needs
     * are read; everything else is skipped by wire type.
     */
    private fun decodeResponse(bytes: ByteArray, wantedUri: String): String? {
        val entries = mutableListOf<Pair<String, String?>>()
        var index = 0
        while (index < bytes.size) {
            val header = readVarint(bytes, index) ?: break
            index = header.second
            val field = (header.first ushr 3).toInt()
            val wire = (header.first and 7L).toInt()
            if (field == 1 && wire == 2) {
                val length = readVarint(bytes, index) ?: break
                val start = length.second
                val end = start + length.first.toInt()
                if (end > bytes.size) break
                decodeCanvas(bytes.copyOfRange(start, end))?.let(entries::add)
                index = end
            } else {
                index = skipField(bytes, index, wire) ?: break
            }
        }
        entries.firstOrNull { it.second == wantedUri }?.first?.let { return it }
        entries.firstOrNull()?.first?.let { return it }
        // Last resort: the URL is sitting in the bytes as plain text even if a field number above
        // turned out to be stale.
        return CNVS_REGEX.find(String(bytes, Charsets.ISO_8859_1))?.value
    }

    private fun decodeCanvas(bytes: ByteArray): Pair<String, String?>? {
        var url: String? = null
        var trackUri: String? = null
        var index = 0
        while (index < bytes.size) {
            val header = readVarint(bytes, index) ?: break
            index = header.second
            val field = (header.first ushr 3).toInt()
            val wire = (header.first and 7L).toInt()
            if (wire == 2 && (field == 1 || field == 2 || field == 5)) {
                val length = readVarint(bytes, index) ?: break
                val start = length.second
                val end = start + length.first.toInt()
                if (end > bytes.size) break
                val text = String(bytes, start, end - start, Charsets.UTF_8)
                when (field) {
                    2 -> url = text
                    5 -> trackUri = text
                }
                index = end
            } else {
                index = skipField(bytes, index, wire) ?: break
            }
        }
        return url?.let { it to trackUri }
    }

    private fun readVarint(bytes: ByteArray, start: Int): Pair<Long, Int>? {
        var value = 0L
        var shift = 0
        var index = start
        while (index < bytes.size && shift <= 63) {
            val byte = bytes[index].toInt() and 0xFF
            value = value or ((byte and 0x7F).toLong() shl shift)
            index++
            if ((byte and 0x80) == 0) return value to index
            shift += 7
        }
        return null
    }

    private fun skipField(bytes: ByteArray, start: Int, wire: Int): Int? = when (wire) {
        0 -> readVarint(bytes, start)?.second
        1 -> (start + 8).takeIf { it <= bytes.size }
        2 -> readVarint(bytes, start)?.let { (length, next) ->
            (next + length.toInt()).takeIf { it <= bytes.size }
        }
        5 -> (start + 4).takeIf { it <= bytes.size }
        else -> null
    }

    private fun writeString(out: ByteArrayOutputStream, field: Int, value: String) {
        writeBytes(out, field, value.toByteArray(Charsets.UTF_8))
    }

    private fun writeBytes(out: ByteArrayOutputStream, field: Int, value: ByteArray) {
        writeVarint(out, (field shl 3) or 2)
        writeVarint(out, value.size)
        out.write(value)
    }

    private fun writeVarint(out: ByteArrayOutputStream, value: Int) {
        var remaining = value
        while ((remaining and 0x7F.inv()) != 0) {
            out.write((remaining and 0x7F) or 0x80)
            remaining = remaining ushr 7
        }
        out.write(remaining)
    }
}
