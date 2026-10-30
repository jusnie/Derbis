package com.yasuda.derbis.feature.search

import com.yasuda.derbis.data.ArtistRepository
import com.yasuda.derbis.data.YoutubeMusicRepository
import com.yasuda.derbis.data.spotify.SpotifySearchClient
import com.yasuda.derbis.domain.ArtistHit
import com.yasuda.derbis.domain.LyricsSearchMatch
import com.yasuda.derbis.domain.SearchArtistDiscography
import com.yasuda.derbis.domain.SearchFilter
import com.yasuda.derbis.domain.SearchResults
import com.yasuda.derbis.domain.SearchSuggestionBundle
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.feature.providers.DerbisProviderRouter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

internal class YoutubeMusicSearchBackend(
    private val repository: YoutubeMusicRepository,
    private val artistRepository: ArtistRepository,
    private val providerRouter: DerbisProviderRouter,
    private val spotifyClient: SpotifySearchClient = SpotifySearchClient(),
    private val lrclibClient: LrcLibLyricsSearchClient = LrcLibLyricsSearchClient(),
    private val localCandidateSource: () -> List<LocalSearchCandidate>,
    private val playableTrack: (Track) -> Boolean
) : SearchBackend {
    override fun localCandidates(): List<LocalSearchCandidate> = localCandidateSource()

    override fun acceptsLocalTrack(track: Track): Boolean = playableTrack(track)

    override suspend fun suggestions(query: String, languageCode: String): SearchSuggestionBundle =
        withContext(Dispatchers.IO) { repository.searchSuggestionBundle(query, languageCode) }

    override suspend fun overview(query: String, languageCode: String): SearchResults =
        providerRouter.searchEverything(query, languageCode)

    override suspend fun spotifyOverview(query: String): SearchResults? =
        withContext(Dispatchers.IO) {
            try {
                spotifyClient.search(query).copy(playlists = emptyList())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                null
            }
        }

    override suspend fun cachedSpotifyOverview(query: String): SearchResults = spotifyClient.cachedSearch(query)

    override suspend fun section(
        filter: SearchFilter,
        query: String,
        languageCode: String,
        continuation: String
    ): SearchSectionPage = when (filter) {
        SearchFilter.Songs -> repository.searchSongsPage(query, languageCode, continuation)
            .let { page -> SearchSectionPage(songs = page.items, continuation = page.continuation) }
        SearchFilter.Videos -> repository.searchVideosPage(query, languageCode, continuation)
            .let { page -> SearchSectionPage(videos = page.items, continuation = page.continuation) }
        SearchFilter.Albums -> repository.searchAlbumsPage(query, languageCode, continuation)
            .let { page -> SearchSectionPage(albums = page.items, continuation = page.continuation) }
        SearchFilter.Artists -> repository.searchArtistsPage(query, languageCode, continuation)
            .let { page -> SearchSectionPage(artists = page.items, continuation = page.continuation) }
        SearchFilter.Playlists -> repository.searchPlaylistsPage(query, languageCode, continuation)
            .let { page -> SearchSectionPage(playlists = page.items, continuation = page.continuation) }
        SearchFilter.Lyrics -> searchLyricsSection(query, languageCode, continuation)
        SearchFilter.All -> SearchSectionPage()
    }

    private suspend fun searchLyricsSection(
        query: String,
        languageCode: String,
        continuation: String
    ): SearchSectionPage = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext SearchSectionPage()

        val ytmLyricsQuery = if (trimmed.contains("lyrics", ignoreCase = true)) trimmed else "$trimmed lyrics"
        val ytmDeferred = async {
            runCatching { repository.searchSongsPage(ytmLyricsQuery, languageCode, continuation) }.getOrNull()
        }
        val lrclibDeferred = async {
            runCatching { lrclibClient.search(trimmed) }.getOrDefault(emptyList())
        }

        val ytmPage = ytmDeferred.await()
        val lrclibMatches = lrclibDeferred.await()
        val ytmSongs = ytmPage?.items.orEmpty()

        val matches = buildList {
            val matchedYtmIds = mutableSetOf<String>()

            // 1. YouTube Music songs mapped to exact LRCLIB matched line if found
            ytmSongs.forEach { track ->
                val lrcMatch = lrclibMatches.firstOrNull {
                    track.title.contains(it.title, ignoreCase = true) || it.title.contains(track.title, ignoreCase = true)
                }
                val line = lrcMatch?.matchedLine?.ifBlank { trimmed } ?: trimmed
                matchedYtmIds.add(track.id)
                add(LyricsSearchMatch(track = track, matchedLine = line))
            }

            // 2. High-confidence LRCLIB matches that weren't in YTM's top page, resolved via repository.searchOne
            lrclibMatches.take(4).forEach { lrc ->
                val alreadyAdded = any { it.track.title.contains(lrc.title, ignoreCase = true) }
                if (!alreadyAdded) {
                    val foundTrack = runCatching { repository.searchOne("${lrc.title} ${lrc.artist}", languageCode) }.getOrNull()
                    if (foundTrack != null && matchedYtmIds.add(foundTrack.id)) {
                        add(LyricsSearchMatch(track = foundTrack, matchedLine = lrc.matchedLine.ifBlank { trimmed }))
                    }
                }
            }
        }

        SearchSectionPage(
            songs = matches.map { it.track },
            lyricsMatches = matches,
            continuation = ytmPage?.continuation.orEmpty()
        )
    }

    override suspend fun exactArtist(query: String): ArtistHit? = artistRepository.artistHitFor(query)

    override suspend fun officialArtists(candidates: List<ArtistHit>): List<ArtistHit> =
        artistRepository.officialArtistHits(candidates)

    override suspend fun officialDiscography(artist: ArtistHit): SearchArtistDiscography? =
        officialSearchDiscography(artistRepository.profile(artist.browseId, artist.name), artist)

    override fun overviewHedgeDelayMs(): Long {
        val averageLatencyMs = providerRouter.health()
            .firstOrNull { it.providerId == YOUTUBE_MUSIC_PROVIDER_ID }
            ?.averageLatencyMs
            ?: 0L
        if (averageLatencyMs <= 0L) return DEFAULT_HEDGE_DELAY_MS
        return (averageLatencyMs * 2L).coerceIn(MIN_HEDGE_DELAY_MS, MAX_HEDGE_DELAY_MS)
    }

    private companion object {
        const val YOUTUBE_MUSIC_PROVIDER_ID = "youtube_music"
        const val DEFAULT_HEDGE_DELAY_MS = 1_800L
        const val MIN_HEDGE_DELAY_MS = 1_200L
        const val MAX_HEDGE_DELAY_MS = 3_500L
    }
}
