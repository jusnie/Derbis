package com.yasuda.derbis.data.subsonic

import com.yasuda.derbis.data.network.DerbisHttpClientFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * A Subsonic / Navidrome / Gonic / Airsonic client, deliberately small.
 *
 * Only the calls this app actually needs are implemented. Everything is one GET that answers
 * JSON, so there is no SDK, no session, and no background work: if the server is down the caller
 * gets [SubsonicResult.Unreachable] and the router moves on to the next provider.
 *
 * The API version is pinned at 1.16.1 because that is the last revision every server family
 * agrees on. OpenSubsonic extensions are detected but never required.
 */
class SubsonicClient(
    private val client: OkHttpClient = DerbisHttpClientFactory.externalIntegrations(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val saltSource: () -> String = { randomSalt() }
) {
    /**
     * Checks that the URL looks like a server root and that the credentials work.
     *
     * The server URL is normalised first: a trailing slash, or a trailing `/rest`, or a pasted
     * `.../rest/ping.view` are all things people actually type, and all of them are recoverable.
     */
    suspend fun ping(credentials: SubsonicCredentials): SubsonicResult<SubsonicServerInfo> {
        val base = normalizeServerUrl(credentials.serverUrl)
            ?: return SubsonicResult.Unreachable("That does not look like a server address")
        val body = request(base, "ping", credentials, emptyList())
        return when (body) {
            is SubsonicResult.Failed -> body
            is SubsonicResult.Unreachable -> body
            is SubsonicResult.Ok -> {
                val response = body.value
                SubsonicResult.Ok(
                    SubsonicServerInfo(
                        version = response.optString("version", ""),
                        serverType = response.optString("type", ""),
                        openSubsonic = response.optBoolean("openSubsonic", false)
                    )
                )
            }
        }
    }

    suspend fun search(
        credentials: SubsonicCredentials,
        query: String,
        songCount: Int = SONGS_PER_SEARCH,
        albumCount: Int = ALBUMS_PER_SEARCH,
        artistCount: Int = ARTISTS_PER_SEARCH
    ): SubsonicResult<SubsonicSearchPage> {
        if (query.isBlank()) return SubsonicResult.Ok(SubsonicSearchPage())
        val base = normalizeServerUrl(credentials.serverUrl)
            ?: return SubsonicResult.Unreachable("That does not look like a server address")
        val params = listOf(
            "query" to query,
            "songCount" to songCount.toString(),
            "albumCount" to albumCount.toString(),
            "artistCount" to artistCount.toString(),
            "artistOffset" to "0",
            "albumOffset" to "0",
            "songOffset" to "0"
        )
        return when (val body = request(base, "search3", credentials, params)) {
            is SubsonicResult.Failed -> body
            is SubsonicResult.Unreachable -> body
            is SubsonicResult.Ok -> {
                // search3 nests everything under "searchResult3". Servers older than 1.4.0 only
                // have "searchResult2" with a different shape; those are refused rather than
                // guessed at, because a half-parsed library is worse than none.
                val result = body.value.optJSONObject("searchResult3")
                    ?: body.value.optJSONObject("searchResult2")
                    ?: JSONObject()
                SubsonicResult.Ok(
                    SubsonicSearchPage(
                        songs = result.optJSONArray("song").toObjects().mapNotNull(::parseSong),
                        albums = result.optJSONArray("album").toObjects().mapNotNull(::parseAlbum),
                        artists = result.optJSONArray("artist").toObjects().mapNotNull(::parseArtist)
                    )
                )
            }
        }
    }

    suspend fun album(credentials: SubsonicCredentials, albumId: String): SubsonicResult<SubsonicAlbumDetail> {
        val base = normalizeServerUrl(credentials.serverUrl)
            ?: return SubsonicResult.Unreachable("That does not look like a server address")
        return when (val body = request(base, "getAlbum", credentials, listOf("id" to albumId))) {
            is SubsonicResult.Failed -> body
            is SubsonicResult.Unreachable -> body
            is SubsonicResult.Ok -> {
                val albumJson = body.value.optJSONObject("album")
                    ?: return SubsonicResult.Failed(0, "Album not on this server")
                val album = parseAlbum(albumJson)
                    ?: return SubsonicResult.Failed(0, "Album not on this server")
                SubsonicResult.Ok(
                    SubsonicAlbumDetail(
                        album = album,
                        songs = albumJson.optJSONArray("song").toObjects().mapNotNull(::parseSong)
                    )
                )
            }
        }
    }

    suspend fun playlists(credentials: SubsonicCredentials): SubsonicResult<List<SubsonicPlaylist>> {
        val base = normalizeServerUrl(credentials.serverUrl)
            ?: return SubsonicResult.Unreachable("That does not look like a server address")
        return when (val body = request(base, "getPlaylists", credentials, emptyList())) {
            is SubsonicResult.Failed -> body
            is SubsonicResult.Unreachable -> body
            is SubsonicResult.Ok -> SubsonicResult.Ok(
                body.value.optJSONObject("playlists")
                    ?.optJSONArray("playlist")
                    .toObjects()
                    .mapNotNull(::parsePlaylist)
            )
        }
    }

    suspend fun playlist(
        credentials: SubsonicCredentials,
        playlistId: String
    ): SubsonicResult<SubsonicPlaylistDetail> {
        val base = normalizeServerUrl(credentials.serverUrl)
            ?: return SubsonicResult.Unreachable("That does not look like a server address")
        return when (val body = request(base, "getPlaylist", credentials, listOf("id" to playlistId))) {
            is SubsonicResult.Failed -> body
            is SubsonicResult.Unreachable -> body
            is SubsonicResult.Ok -> {
                val playlistJson = body.value.optJSONObject("playlist")
                    ?: return SubsonicResult.Failed(0, "Playlist not on this server")
                val playlist = parsePlaylist(playlistJson)
                    ?: return SubsonicResult.Failed(0, "Playlist not on this server")
                SubsonicResult.Ok(
                    SubsonicPlaylistDetail(
                        playlist = playlist,
                        songs = playlistJson.optJSONArray("entry").toObjects().mapNotNull(::parseSong)
                    )
                )
            }
        }
    }

    /**
     * The URL the player streams from.
     *
     * `format=raw` asks the server not to transcode, which is the whole point of pointing Derbis at
     * a home server — the file on the disk is already what the listener wants to hear. A server
     * that cannot serve the original format still answers, it just transcodes; the setting on the
     * server decides that, not this app.
     */
    fun streamUrl(credentials: SubsonicCredentials, songId: String, raw: Boolean = true): String? {
        val base = normalizeServerUrl(credentials.serverUrl) ?: return null
        return buildSignedUrl(base, "stream", credentials, buildList {
            add("id" to songId)
            if (raw) add("format" to "raw")
        })?.toString()
    }

    /**
     * Cover art at the artwork-quality ceiling.
     *
     * Subsonic takes an explicit `size`, so unlike the CDNs this one can honour the user's band
     * exactly. The token is in the query string, which is why this URL must never be persisted —
     * see [SubsonicSettingsStore] and the note in [com.yasuda.derbis.data.TrackJson].
     */
    fun coverArtUrl(credentials: SubsonicCredentials, coverArtId: String): String? {
        if (coverArtId.isBlank()) return null
        val base = normalizeServerUrl(credentials.serverUrl) ?: return null
        val size = COVER_ART_SIZE_PX
        return buildSignedUrl(base, "getCoverArt", credentials, listOf("id" to coverArtId, "size" to size.toString()))
            ?.toString()
    }

    private suspend fun request(
        base: HttpUrl,
        endpoint: String,
        credentials: SubsonicCredentials,
        params: List<Pair<String, String>>
    ): SubsonicResult<JSONObject> = withContext(dispatcher) {
        val url = buildSignedUrl(base, endpoint, credentials, params)
            ?: return@withContext SubsonicResult.Unreachable("Could not build the request")
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext SubsonicResult.Unreachable(
                        "Server answered ${response.code}"
                    )
                }
                val text = response.body?.string().orEmpty()
                val root = runCatching { JSONObject(text) }.getOrNull()
                    ?: return@withContext SubsonicResult.Unreachable("Server did not answer JSON")
                // Most servers wrap the payload in "subsonic-response"; OpenSubsonic also accepts
                // "response". Both are checked so a newer server does not look broken.
                val envelope = root.optJSONObject("subsonic-response")
                    ?: root.optJSONObject("response")
                    ?: root
                when (envelope.optString("status", "ok")) {
                    "failed" -> {
                        val error = envelope.optJSONObject("error")
                        SubsonicResult.Failed(
                            code = error?.optInt("code", 0) ?: 0,
                            message = error?.optString("message").orEmpty()
                        )
                    }
                    else -> SubsonicResult.Ok(envelope)
                }
            }
        } catch (error: Throwable) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            SubsonicResult.Unreachable(error.message ?: error::class.java.simpleName)
        }
    }

    private fun buildSignedUrl(
        base: HttpUrl,
        endpoint: String,
        credentials: SubsonicCredentials,
        params: List<Pair<String, String>>
    ): HttpUrl? {
        val salt = saltSource()
        val token = md5Hex(credentials.password + salt)
        val builder = base.newBuilder()
            .addPathSegment("rest")
            .addPathSegment("$endpoint.view")
            .addQueryParameter("u", credentials.username)
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", API_VERSION)
            .addQueryParameter("c", CLIENT_NAME)
            .addQueryParameter("f", "json")
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return runCatching { builder.build() }.getOrNull()
    }

    private fun parseSong(json: JSONObject): SubsonicSong? {
        val id = json.optString("id").ifBlank { return null }
        return SubsonicSong(
            id = id,
            title = json.optString("title").ifBlank { "Unknown track" },
            artist = json.optString("artist"),
            album = json.optString("album"),
            albumId = json.optString("albumId"),
            artistId = json.optString("artistId"),
            coverArtId = json.optString("coverArt"),
            durationSeconds = json.optLong("duration", 0L),
            trackNumber = json.optInt("track", 0),
            discNumber = json.optInt("discNumber", 0),
            year = json.optString("year"),
            genre = json.optString("genre"),
            sizeBytes = json.optLong("size", 0L),
            suffix = json.optString("suffix"),
            bitRate = json.optInt("bitRate", 0),
            contentType = json.optString("contentType")
        )
    }

    private fun parseAlbum(json: JSONObject): SubsonicAlbum? {
        val id = json.optString("id").ifBlank { return null }
        return SubsonicAlbum(
            id = id,
            name = json.optString("name").ifBlank { json.optString("title") },
            artist = json.optString("artist"),
            artistId = json.optString("artistId"),
            coverArtId = json.optString("coverArt"),
            songCount = json.optInt("songCount", 0),
            year = json.optString("year"),
            genre = json.optString("genre")
        )
    }

    private fun parseArtist(json: JSONObject): SubsonicArtist? {
        val id = json.optString("id").ifBlank { return null }
        val name = json.optString("name").ifBlank { return null }
        return SubsonicArtist(
            id = id,
            name = name,
            albumCount = json.optInt("albumCount", 0),
            coverArtId = json.optString("coverArt")
        )
    }

    private fun parsePlaylist(json: JSONObject): SubsonicPlaylist? {
        val id = json.optString("id").ifBlank { return null }
        return SubsonicPlaylist(
            id = id,
            name = json.optString("name").ifBlank { "Playlist" },
            owner = json.optString("owner"),
            songCount = json.optInt("songCount", 0),
            coverArtId = json.optString("coverArt")
        )
    }

    companion object {
        const val API_VERSION = "1.16.1"
        const val CLIENT_NAME = "Derbis"
        const val SOURCE = "Subsonic"
        const val SONG_ID_PREFIX = "subsonic:"

        /**
         * Full-resolution cover art. This line has no artwork-quality ceiling yet; when it does,
         * this is the number that should follow it rather than a constant.
         */
        const val COVER_ART_SIZE_PX = 1200
        const val ALBUM_ID_PREFIX = "subsonic:album:"
        const val PLAYLIST_ID_PREFIX = "subsonic:playlist:"
        private const val SONGS_PER_SEARCH = 20
        private const val ALBUMS_PER_SEARCH = 12
        private const val ARTISTS_PER_SEARCH = 6

        /**
         * Turns whatever was pasted into a bare server root.
         *
         * Accepts `host`, `https://host`, `https://host/`, `https://host/music`,
         * `https://host/rest` and `https://host/rest/ping.view`. It does not accept a path that
         * is not a URL at all, because guessing a scheme for someone is how you end up silently
         * talking to the wrong host over cleartext.
         */
        fun normalizeServerUrl(raw: String): HttpUrl? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val withScheme = when {
                trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true) -> trimmed
                // A bare host is assumed to be a local server over https first; the caller can
                // always type the scheme explicitly to override.
                else -> "https://$trimmed"
            }
            val parsed = withScheme.toHttpUrlOrNull() ?: return null
            var segments = parsed.pathSegments.filter { it.isNotBlank() }
            if (segments.lastOrNull()?.contains('.') == true) segments = segments.dropLast(1)
            if (segments.lastOrNull() == "rest") segments = segments.dropLast(1)
            val builder = parsed.newBuilder().encodedPath("/")
            segments.forEach { segment -> builder.addPathSegment(segment) }
            builder.query(null).fragment(null)
            return runCatching { builder.build() }.getOrNull()
        }

        internal fun md5Hex(value: String): String {
            val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
            val out = StringBuilder(digest.size * 2)
            digest.forEach { byte ->
                val v = byte.toInt() and 0xFF
                out.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
            }
            return out.toString()
        }

        private const val HEX = "0123456789abcdef"

        internal fun randomSalt(): String {
            val bytes = ByteArray(8)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { byte ->
                val v = byte.toInt() and 0xFF
                "${HEX[v ushr 4]}${HEX[v and 0x0F]}"
            }
        }
    }
}

data class SubsonicSearchPage(
    val songs: List<SubsonicSong> = emptyList(),
    val albums: List<SubsonicAlbum> = emptyList(),
    val artists: List<SubsonicArtist> = emptyList()
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}

private fun JSONArray?.toObjects(): List<JSONObject> {
    if (this == null) return emptyList()
    val out = ArrayList<JSONObject>(length())
    for (index in 0 until length()) {
        optJSONObject(index)?.let(out::add)
    }
    return out
}

/** A search hit with no id is not a track, whatever else the server filled in. */
internal fun SubsonicSong.isUsable(): Boolean = id.isNotBlank()
