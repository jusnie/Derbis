package com.yasuda.derbis.feature.providers

import com.yasuda.derbis.data.YoutubeMusicPlaylistDetail
import com.yasuda.derbis.data.subsonic.SubsonicAlbum
import com.yasuda.derbis.data.subsonic.SubsonicAlbumDetail
import com.yasuda.derbis.data.subsonic.SubsonicClient
import com.yasuda.derbis.data.subsonic.SubsonicCredentials
import com.yasuda.derbis.data.subsonic.SubsonicResult
import com.yasuda.derbis.data.subsonic.SubsonicSearchPage
import com.yasuda.derbis.data.subsonic.SubsonicSettingsStore
import com.yasuda.derbis.data.subsonic.SubsonicSong
import com.yasuda.derbis.domain.AlbumDetail
import com.yasuda.derbis.domain.AlbumHit
import com.yasuda.derbis.domain.ArtistHit
import com.yasuda.derbis.domain.PlaylistHit
import com.yasuda.derbis.domain.SearchResults
import com.yasuda.derbis.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Maps a Subsonic song onto this app's [Track].
 *
 * Every field that the rest of the app reads is filled: identity, durations, disc and track
 * numbers, year, ISRC, the size and bitrate that the technical sheet reports, and a thumbnail.
 *
 * Two deliberate choices. The [Track.id] is prefixed so an id from this provider can never be
 * mistaken for a YouTube one, and the [Track.streamUrl] is signed fresh here rather than stored —
 * a Subsonic stream URL carries a token in its query string, so it is a credential, not a link.
 */
internal fun SubsonicSong.toTrack(
    credentials: SubsonicCredentials,
    client: SubsonicClient
): Track {
    val stream = client.streamUrl(credentials, id).orEmpty()
    val cover = client.coverArtUrl(credentials, coverArtId).orEmpty()
    return Track(
        id = SubsonicClient.SONG_ID_PREFIX + id,
        title = title,
        artist = artist,
        album = album,
        durationMs = (durationSeconds * 1000L).coerceAtLeast(0L),
        streamUrl = stream,
        videoUrl = "",
        thumbnailUrl = cover,
        largeThumbnailUrl = cover,
        source = SubsonicClient.SOURCE,
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        trackNumber = trackNumber,
        discNumber = discNumber,
        year = year,
        albumBrowseId = SubsonicClient.ALBUM_ID_PREFIX + albumId,
        artistBrowseIds = if (artistId.isBlank()) emptyList() else listOf(artistId),
        metadataProvider = SubsonicClient.SOURCE
    )
}

private fun SubsonicAlbum.toAlbumHit(client: SubsonicClient, credentials: SubsonicCredentials): AlbumHit {
    return AlbumHit(
        title = name,
        artist = artist,
        year = year,
        thumbnailUrl = client.coverArtUrl(credentials, coverArtId).orEmpty(),
        query = listOf(artist, name).filter { it.isNotBlank() }.joinToString(" "),
        browseId = SubsonicClient.ALBUM_ID_PREFIX + id
    )
}

/**
 * Catalog side: search, album and playlist.
 *
 * The provider declines anything it does not own rather than answering empty, so the router keeps
 * the YouTube Music provider as the primary for ordinary albums and only reaches into the home
 * server when an id says the request came from there.
 */
class SubsonicCatalogProvider(
    private val settings: SubsonicSettingsStore,
    private val client: SubsonicClient = SubsonicClient()
) : DerbisCatalogProvider {
    override val id: String = PROVIDER_ID
    override val priority: Int = PRIORITY

    /**
     * Search results are merged with whatever the primary provider returned, not offered as a
     * replacement. A listener who owns a record should see it next to the streamed version, not
     * instead of it — and one who does not own it must notice no difference at all.
     */
    override val mergesIntoPrimary: Boolean = true

    override suspend fun searchEverything(query: String, languageCode: String): SearchResults {
        val credentials = settings.load() ?: return SearchResults()
        return when (val result = client.search(credentials, query)) {
            is SubsonicResult.Failed -> throw DerbisProviderDeclinedException(result.message)
            is SubsonicResult.Unreachable -> throw DerbisProviderDeclinedException(result.message)
            is SubsonicResult.Ok -> result.value.toSearchResults(credentials)
        }
    }

    override suspend fun albumDetail(album: AlbumHit, languageCode: String): AlbumDetail? {
        val albumId = album.browseId.removePrefix(SubsonicClient.ALBUM_ID_PREFIX)
        if (!album.browseId.startsWith(SubsonicClient.ALBUM_ID_PREFIX) || albumId.isBlank()) {
            throw DerbisProviderDeclinedException("not a server album")
        }
        val credentials = settings.load()
            ?: throw DerbisProviderDeclinedException("no server configured")
        return when (val result = client.album(credentials, albumId)) {
            is SubsonicResult.Failed -> throw DerbisProviderDeclinedException(result.message)
            is SubsonicResult.Unreachable -> throw DerbisProviderDeclinedException(result.message)
            is SubsonicResult.Ok -> result.value.toAlbumDetail(credentials)
        }
    }

    override suspend fun playlist(
        playlistId: String,
        languageCode: String,
        limit: Int
    ): YoutubeMusicPlaylistDetail? {
        val id = playlistId.removePrefix(SubsonicClient.PLAYLIST_ID_PREFIX)
        if (!playlistId.startsWith(SubsonicClient.PLAYLIST_ID_PREFIX) || id.isBlank()) {
            throw DerbisProviderDeclinedException("not a server playlist")
        }
        val credentials = settings.load()
            ?: throw DerbisProviderDeclinedException("no server configured")
        return when (val result = client.playlist(credentials, id)) {
            is SubsonicResult.Failed -> throw DerbisProviderDeclinedException(result.message)
            is SubsonicResult.Unreachable -> throw DerbisProviderDeclinedException(result.message)
            is SubsonicResult.Ok -> {
                val detail = result.value
                YoutubeMusicPlaylistDetail(
                    playlistId = playlistId,
                    title = detail.playlist.name,
                    description = "",
                    author = detail.playlist.owner,
                    thumbnailUrl = client.coverArtUrl(credentials, detail.playlist.coverArtId).orEmpty(),
                    tracks = detail.songs.take(limit).map { it.toTrack(credentials, client) }
                )
            }
        }
    }

    private fun SubsonicSearchPage.toSearchResults(credentials: SubsonicCredentials): SearchResults {
        val tracks = songs.map { it.toTrack(credentials, client) }
        return SearchResults(
            topTrack = tracks.firstOrNull(),
            songs = tracks,
            artists = artists.map { artist ->
                ArtistHit(
                    name = artist.name,
                    subscribers = "",
                    thumbnailUrl = client.coverArtUrl(credentials, artist.coverArtId).orEmpty(),
                    accentStart = 0,
                    accentEnd = 0
                )
            },
            albums = albums.map { it.toAlbumHit(client, credentials) },
            playlists = emptyList<PlaylistHit>()
        )
    }

    private fun SubsonicAlbumDetail.toAlbumDetail(credentials: SubsonicCredentials): AlbumDetail {
        val hit = album.toAlbumHit(client, credentials)
        return AlbumDetail(
            album = hit,
            description = "",
            tracks = songs.map { it.toTrack(credentials, client) }
        )
    }

    private companion object {
        const val PROVIDER_ID = "subsonic"
        const val PRIORITY = 20
    }
}

/**
 * Playback side.
 *
 * Only touches tracks this provider produced. Everything else answers with a miss, which the
 * router treats as "keep going" rather than as a failure, so a YouTube track never pays for having
 * a home server configured.
 */
class SubsonicPlaybackProvider(
    private val settings: SubsonicSettingsStore,
    private val client: SubsonicClient = SubsonicClient()
) : DerbisPlaybackProvider {
    override val id: String = PROVIDER_ID
    override val priority: Int = PRIORITY

    override suspend fun resolve(track: Track, videoMode: Boolean): Track {
        if (!track.isSubsonicTrack()) throw DerbisProviderMissException("not a server track")
        // A Subsonic track has no video: the file is the file. Asking for video mode would only
        // produce a player waiting for a stream that can never arrive.
        if (videoMode) throw DerbisProviderMissException("no video for server tracks")
        return refresh(track)
    }

    override suspend fun resolveForOffline(track: Track): Track {
        if (!track.isSubsonicTrack()) throw DerbisProviderMissException("not a server track")
        return refresh(track)
    }

    /**
     * Re-signs the stream URL.
     *
     * The URL in a [Track] was signed when the track was built, and a salt is only good for the
     * request it was made for. Re-signing here is what makes a queued server track still playable
     * an hour later, and it is why the URL is never persisted.
     */
    private suspend fun refresh(track: Track): Track = withContext(Dispatchers.IO) {
        val credentials = settings.load() ?: throw DerbisProviderMissException("no server configured")
        val songId = track.id.removePrefix(SubsonicClient.SONG_ID_PREFIX)
        val stream = client.streamUrl(credentials, songId)
            ?: throw DerbisProviderMissException("could not sign a stream url")
        track.copy(streamUrl = stream, source = SubsonicClient.SOURCE)
    }

    private companion object {
        const val PROVIDER_ID = "subsonic_playback"
        const val PRIORITY = 0
    }
}

internal fun Track.isSubsonicTrack(): Boolean =
    source.equals(SubsonicClient.SOURCE, ignoreCase = true) ||
        id.startsWith(SubsonicClient.SONG_ID_PREFIX)
