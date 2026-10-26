package com.luc4n3x.derbis.data.spotify

import android.content.Context
import com.luc4n3x.derbis.data.network.DerbisHttpClientFactory
import com.luc4n3x.derbis.domain.PlaylistImportFailureKind
import com.luc4n3x.derbis.nexus.playlistimport.ImportedPlaylistDescriptor
import com.luc4n3x.derbis.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.derbis.nexus.playlistimport.IncompleteReason
import com.luc4n3x.derbis.nexus.playlistimport.PlaylistImportCompleteness
import com.luc4n3x.derbis.nexus.playlistimport.PlaylistImportSource
import com.luc4n3x.derbis.nexus.playlistimport.PlaylistTextParsers
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** A Spotify playlist result that may be displayed before an import begins. */
data class SpotifyPlaylistSearchResult(
    val id: String,
    val title: String,
    val owner: String,
    val description: String,
    val artworkUrl: String,
    val trackCount: Int?,
    val spotifyUrl: String
)

/** A page of playlist metadata returned by Spotify for the signed-in listener. */
data class SpotifyPlaylistPage(
    val playlists: List<SpotifyPlaylistSearchResult>,
    val total: Int?,
    val nextOffset: Int?
)

/** Metadata for one Spotify track, used only for the signed-in user's playlist preview/import. */
data class SpotifyPlaylistTrackPreview(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUrl: String,
    val explicit: Boolean
)

data class SpotifyPlaylistDetail(
    val playlist: SpotifyPlaylistSearchResult,
    val tracks: List<SpotifyPlaylistTrackPreview>,
    val identities: List<ImportedTrackIdentity>,
    val completeness: PlaylistImportCompleteness
) {
    val descriptor: ImportedPlaylistDescriptor
        get() = ImportedPlaylistDescriptor(
            source = PlaylistImportSource.SPOTIFY,
            sourceId = playlist.id,
            title = playlist.title,
            owner = playlist.owner,
            description = playlist.description,
            artworkUrl = playlist.artworkUrl,
            declaredTrackCount = playlist.trackCount
        )
}

class SpotifyPlaylistBrowserException(
    val kind: PlaylistImportFailureKind,
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

/**
 * Official, signed-in Spotify playlist catalog. It intentionally uses only Spotify Web API
 * endpoints and never reads browser cookies or private Spotify web-player calls.
 */
class SpotifyPlaylistBrowser(
    private val context: Context,
    private val client: OkHttpClient = DerbisHttpClientFactory.media(context.applicationContext)
) {
    /**
     * Lists the signed-in listener's Spotify library. Spotify returns playlists the listener
     * owns or follows; opening one fetches track items only when Spotify grants item access.
     */
    suspend fun library(offset: Int = 0): SpotifyPlaylistPage {
        require(offset >= 0) { "Spotify playlist library offset must not be negative" }
        val token = accessToken()
        // Keep Spotify's ordinary library representation rather than a sparse field projection.
        // Playlist metadata is small, and this keeps the reader compatible with Spotify's
        // evolving optional playlist fields.
        val url = "$API_BASE/me/playlists".toHttpUrl().newBuilder()
            .addQueryParameter("limit", LIBRARY_PAGE_SIZE.toString())
            .addQueryParameter("offset", offset.toString())
            .build()
        return parseSpotifyPlaylistLibraryPage(
            json = getJson(url.toString(), token),
            requestedOffset = offset,
            requestedLimit = LIBRARY_PAGE_SIZE
        )
    }

    /**
     * Fetches tracks only with the signed-in user's token. Spotify currently returns playlist
     * items solely for playlists the user owns or collaborates on; a 403 is surfaced plainly.
     */
    suspend fun playlist(id: String): SpotifyPlaylistDetail {
        require(SPOTIFY_ID.matches(id)) { "Invalid Spotify playlist id" }
        val token = accessToken()
        val headerUrl = "$API_BASE/playlists/$id".toHttpUrl().newBuilder()
            .addQueryParameter("fields", "id,name,description,owner(display_name),images(url),items(total),external_urls(spotify)")
            .addQueryParameter("market", "from_token")
            .build()
        val header = parseSpotifyPlaylistHeader(getJson(headerUrl.toString(), token), id)
        val identities = ArrayList<ImportedTrackIdentity>()
        val previews = ArrayList<SpotifyPlaylistTrackPreview>()
        var offset = 0
        var declared = header.trackCount
        var hasNext = true
        while (hasNext && offset < MAX_IMPORT_TRACKS) {
            val url = "$API_BASE/playlists/$id/items".toHttpUrl().newBuilder()
                .addQueryParameter("limit", ITEM_PAGE_SIZE.toString())
                .addQueryParameter("offset", offset.toString())
                .addQueryParameter("market", "from_token")
                .addQueryParameter("additional_types", "track")
                .build()
            val page = parseSpotifyPlaylistItems(getJson(url.toString(), token))
            declared = declared ?: page.total
            val pageTracks = page.tracks
            pageTracks.forEach { track ->
                if (identities.size >= MAX_IMPORT_TRACKS) return@forEach
                val position = identities.size
                identities += ImportedTrackIdentity(
                    position = position,
                    title = track.title,
                    artists = PlaylistTextParsers.splitArtists(track.artist),
                    album = track.album,
                    durationMs = track.durationMs,
                    sourceTrackId = track.id,
                    explicit = track.explicit,
                    artworkUrl = track.artworkUrl,
                    originalUrl = track.spotifyUrl
                )
                previews += SpotifyPlaylistTrackPreview(
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMs = track.durationMs,
                    artworkUrl = track.artworkUrl,
                    explicit = track.explicit
                )
            }
            offset += page.itemCount
            hasNext = page.hasNext && page.itemCount > 0 && identities.size < MAX_IMPORT_TRACKS
        }
        if (identities.isEmpty() && (declared ?: 0) > 0) {
            throw SpotifyPlaylistBrowserException(
                PlaylistImportFailureKind.NO_USABLE_TRACKS,
                "This Spotify playlist does not contain importable music tracks."
            )
        }
        val finalHeader = header.copy(trackCount = declared ?: identities.size)
        return SpotifyPlaylistDetail(
            playlist = finalHeader,
            tracks = previews,
            identities = identities,
            completeness = if (!hasNext) {
                PlaylistImportCompleteness.Complete
            } else {
                PlaylistImportCompleteness.Incomplete(
                    retrieved = identities.size,
                    declared = declared,
                    reason = IncompleteReason.PAGINATION_STOPPED
                )
            }
        )
    }

    private suspend fun accessToken(): String = DerbisSpotifyConnection.accessToken(context)
        ?: throw SpotifyPlaylistBrowserException(
            PlaylistImportFailureKind.AUTH_REQUIRED,
            "Connect Spotify before searching playlists."
        )

    private suspend fun getJson(url: String, token: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                if (response.code == 401) DerbisSpotifyConnection.invalidateAccess(context)
                throw spotifyApiException(response.code, body)
            }
            runCatching { JSONObject(body) }.getOrElse { error ->
                throw SpotifyPlaylistBrowserException(
                    PlaylistImportFailureKind.PROVIDER_CHANGED,
                    "Spotify returned an unreadable playlist response.",
                    error
                )
            }
        }
    }

    private fun spotifyApiException(code: Int, body: String): SpotifyPlaylistBrowserException {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        return when (code) {
            401 -> SpotifyPlaylistBrowserException(PlaylistImportFailureKind.AUTH_REQUIRED, "Spotify sign-in expired. Connect Spotify again.")
            403 -> SpotifyPlaylistBrowserException(
                PlaylistImportFailureKind.AUTH_REQUIRED,
                "Spotify only makes playlist songs available for playlists you own or collaborate on."
            )
            404 -> SpotifyPlaylistBrowserException(PlaylistImportFailureKind.NOT_AVAILABLE, "This Spotify playlist is no longer available.")
            429 -> SpotifyPlaylistBrowserException(PlaylistImportFailureKind.RATE_LIMITED, "Spotify is busy. Please wait a moment and search again.")
            else -> SpotifyPlaylistBrowserException(
                PlaylistImportFailureKind.PROVIDER_CHANGED,
                detail.ifBlank { "Spotify could not load this playlist ($code)." }
            )
        }
    }

    private companion object {
        const val API_BASE = "https://api.spotify.com/v1"
        const val LIBRARY_PAGE_SIZE = 50
        const val ITEM_PAGE_SIZE = 50
        const val MAX_IMPORT_TRACKS = 10_000
        val SPOTIFY_ID = Regex("^[A-Za-z0-9]{22}$")
    }
}

internal fun parseSpotifyPlaylistLibraryPage(
    json: JSONObject,
    requestedOffset: Int = 0,
    requestedLimit: Int = 50
): SpotifyPlaylistPage {
    val responseOffset = json.optInt("offset", requestedOffset).coerceAtLeast(0)
    val responseLimit = json.optInt("limit", requestedLimit).takeIf { it > 0 } ?: requestedLimit
    val hasNext = !json.optString("next").isNullOrBlank()
    return SpotifyPlaylistPage(
        playlists = json.optJSONArray("items").spotifyPlaylistResults(),
        total = json.optInt("total", -1).takeIf { it >= 0 },
        nextOffset = if (hasNext) responseOffset + responseLimit else null
    )
}

internal fun parseSpotifyPlaylistHeader(json: JSONObject, fallbackId: String = ""): SpotifyPlaylistSearchResult {
    val result = json.toSpotifyPlaylistResult()
    return result.copy(id = result.id.ifBlank { fallbackId })
}

internal data class SpotifyPlaylistItemPage(
    val tracks: List<SpotifyPlaylistTrackData>,
    val total: Int?,
    val itemCount: Int,
    val hasNext: Boolean
)

internal data class SpotifyPlaylistTrackData(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUrl: String,
    val explicit: Boolean,
    val spotifyUrl: String
)

internal fun parseSpotifyPlaylistItems(json: JSONObject): SpotifyPlaylistItemPage {
    val items = json.optJSONArray("items") ?: JSONArray()
    val tracks = buildList {
        repeat(items.length()) { index ->
            val row = items.optJSONObject(index) ?: return@repeat
            // Spotify renamed track rows to item in 2026. Accept the previous name only so old
            // cached/API responses fail gracefully during the transition.
            val item = row.optJSONObject("item") ?: row.optJSONObject("track") ?: return@repeat
            if (!item.optString("type", "track").equals("track", ignoreCase = true)) return@repeat
            item.toSpotifyTrackData()?.let(::add)
        }
    }
    return SpotifyPlaylistItemPage(
        tracks = tracks,
        total = json.optInt("total", -1).takeIf { it >= 0 },
        itemCount = items.length(),
        hasNext = !json.optString("next").isNullOrBlank()
    )
}

private fun JSONArray?.spotifyPlaylistResults(): List<SpotifyPlaylistSearchResult> = buildList {
    this@spotifyPlaylistResults ?: return@buildList
    repeat(length()) { index ->
        optJSONObject(index)?.toSpotifyPlaylistResult()?.takeIf { it.id.isNotBlank() }?.let(::add)
    }
}

private fun JSONObject.toSpotifyPlaylistResult(): SpotifyPlaylistSearchResult {
    val id = optString("id").trim()
    val items = optJSONObject("items") ?: optJSONObject("tracks")
    return SpotifyPlaylistSearchResult(
        id = id,
        title = optString("name").trim().ifBlank { "Spotify playlist" },
        owner = optJSONObject("owner")?.optString("display_name").orEmpty().trim(),
        description = optString("description").trim(),
        artworkUrl = optJSONArray("images").firstImageUrl(),
        trackCount = items?.optInt("total", -1)?.takeIf { it >= 0 },
        spotifyUrl = optJSONObject("external_urls")?.optString("spotify").orEmpty().ifBlank {
            if (id.isBlank()) "" else "https://open.spotify.com/playlist/$id"
        }
    )
}

private fun JSONObject.toSpotifyTrackData(): SpotifyPlaylistTrackData? {
    val id = optString("id").trim()
    val title = optString("name").trim()
    if (id.isBlank() || title.isBlank()) return null
    val artists = optJSONArray("artists")
        ?.let { array -> List(array.length()) { array.optJSONObject(it)?.optString("name").orEmpty().trim() } }
        ?.filter(String::isNotBlank)
        .orEmpty()
    val album = optJSONObject("album")
    return SpotifyPlaylistTrackData(
        id = id,
        title = title,
        artist = artists.joinToString(", "),
        album = album?.optString("name").orEmpty().trim(),
        durationMs = optLong("duration_ms").coerceAtLeast(0L),
        artworkUrl = album?.optJSONArray("images").firstImageUrl(),
        explicit = optBoolean("explicit"),
        spotifyUrl = optJSONObject("external_urls")?.optString("spotify").orEmpty().ifBlank {
            "https://open.spotify.com/track/$id"
        }
    )
}

private fun JSONArray?.firstImageUrl(): String {
    this ?: return ""
    repeat(length()) { index ->
        val url = optJSONObject(index)?.optString("url").orEmpty().trim()
        if (url.isNotBlank()) return url
    }
    return ""
}
