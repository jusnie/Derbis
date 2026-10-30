package com.yasuda.derbis.ui

import com.yasuda.derbis.domain.LyricsSearchMatch
import com.yasuda.derbis.domain.SearchFilter
import com.yasuda.derbis.domain.SearchResults
import com.yasuda.derbis.feature.search.searchTestTrack
import com.yasuda.derbis.feature.search.LyricsSearchContent
import com.yasuda.derbis.feature.search.lyricsSearchContent
import com.yasuda.derbis.feature.search.lyricsSearchMatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsSearchPresentationTest {
    @Test
    fun lyricsModeDoesNotReclassifyOrdinarySongsAsLyricsMatches() {
        val songs = listOf(searchTestTrack("catalog-song", "Song title", "Artist"))
        val entries = youTubeMusicFilteredEntries(
            SearchFilter.Lyrics, songs, SearchResults(songs = songs), topArtist = null
        )

        assertTrue(entries.isEmpty())
    }

    @Test
    fun lyricsModeUsesOnlyDedicatedLyricsMatchesEvenWhenCatalogSongsExist() {
        val song = searchTestTrack("catalog-song", "Song title", "Artist")
        val match = LyricsSearchMatch(searchTestTrack("lyrics-song", "Matched song", "Artist"), "remembered words")
        val entries = youTubeMusicFilteredEntries(
            SearchFilter.Lyrics, listOf(song), SearchResults(songs = listOf(song), lyricsMatches = listOf(match)), null
        )

        assertEquals(listOf(YouTubeMusicSearchEntry.Lyrics(match)), entries)
    }

    @Test
    fun lyricsRowsStayDeduplicatedByTheirOwnTrackIdentity() {
        val match = LyricsSearchMatch(searchTestTrack("lyrics-song", "Matched song", "Artist"), "remembered words")
        val entries = youTubeMusicFilteredEntries(
            SearchFilter.Lyrics, emptyList(), SearchResults(lyricsMatches = listOf(match, match)), null
        )

        assertEquals(listOf(YouTubeMusicSearchEntry.Lyrics(match)), entries)
    }

    @Test
    fun choosingSongsExplicitlyStillShowsOrdinarySongs() {
        val song = searchTestTrack("catalog-song", "Song title", "Artist")
        val entries = youTubeMusicFilteredEntries(SearchFilter.Songs, listOf(song), SearchResults(songs = listOf(song)), null)

        assertEquals(listOf(YouTubeMusicSearchEntry.Song(song)), entries)
    }

    @Test
    fun pendingLyricsNeverUsesCatalogSongsWhileWaitingForItsOwnPage() {
        assertEquals(LyricsSearchContent.Loading, content(pending = true))
    }

    @Test
    fun lyricsPageLoadingIsIndependentOfOrdinarySearchLoading() {
        assertEquals(LyricsSearchContent.Loading, content(loading = true, pending = false))
    }

    @Test
    fun aLoadedEmptyLyricsPageStaysEmptyDespiteOrdinarySongResults() {
        val songs = listOf(searchTestTrack("catalog-song", "Song title", "Artist"))
        val matches = lyricsSearchMatches(SearchResults(songs = songs))

        assertTrue(matches.isEmpty())
        assertEquals(LyricsSearchContent.Empty, content(matches = matches.size, loaded = true))
    }

    @Test
    fun failedLyricsUsesItsOwnErrorEvenWhenOtherSearchWorkIsPending() {
        assertEquals(LyricsSearchContent.Failed, content(failed = true, pending = true))
    }

    @Test
    fun carriedLyricsFromThePreviousQueryAreHiddenUntilTheCurrentPageLoads() {
        val old = LyricsSearchMatch(searchTestTrack("old-match", "Old title", "Artist"), "old words")
        val visible = lyricsSearchMatches(SearchResults(lyricsMatches = listOf(old)), loaded = false)

        assertTrue(visible.isEmpty())
        assertEquals(LyricsSearchContent.Loading, content(matches = visible.size, pending = true))
    }

    @Test
    fun emptyAndShortLyricsInputShowsAPromptNotTheNormalDiscoveryList() {
        for (query in listOf("", " ", "a")) {
            assertEquals(LyricsSearchContent.Prompt, content(query = query, pending = true, loading = true))
        }
    }

    @Test
    fun existingLyricsMatchesStayVisibleDuringPaginationOrALaterPageFailure() {
        assertEquals(LyricsSearchContent.Matches, content(matches = 1, loaded = true, loading = true))
        assertEquals(LyricsSearchContent.Matches, content(matches = 1, loaded = true, failed = true))
    }

    @Test
    fun readyLyricsKeepTheMatchedWordsAndStayDeduplicated() {
        val match = LyricsSearchMatch(searchTestTrack("match", "Title", "Artist"), "remembered words", "lyric snippet")
        val visible = lyricsSearchMatches(SearchResults(lyricsMatches = listOf(match, match)), loaded = true)

        assertEquals(listOf(match), visible)
        assertEquals("remembered words", visible.single().matchedLine)
    }

    @Test
    fun aFinishedLyricsPageDoesNotWaitForAnUnrelatedSongOverview() {
        assertEquals(LyricsSearchContent.Empty, content(loaded = true, pending = true))
    }

    @Test
    fun retryingALyricsFailureShowsItsLoadingState() {
        assertEquals(LyricsSearchContent.Loading, content(loading = true, failed = true, pending = true))
    }

    @Test
    fun theAllFilterStillContainsOrdinarySongs() {
        val song = searchTestTrack("catalog-song", "Song title", "Artist")
        val entries = youTubeMusicFilteredEntries(SearchFilter.All, listOf(song), SearchResults(songs = listOf(song)), null)

        assertEquals(listOf(YouTubeMusicSearchEntry.Song(song)), entries)
    }

    private fun content(
        query: String = "remembered words",
        matches: Int = 0,
        loaded: Boolean = false,
        loading: Boolean = false,
        failed: Boolean = false,
        pending: Boolean = false
    ): LyricsSearchContent = lyricsSearchContent(query, matches, loaded, loading, failed, pending)

}
