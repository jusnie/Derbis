package com.yasuda.derbis.feature.search

import com.yasuda.derbis.domain.LyricsSearchMatch
import com.yasuda.derbis.domain.SearchFilter
import com.yasuda.derbis.domain.SearchResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsSearchCarryOverTest {
    private val oldMatch = LyricsSearchMatch(searchTestTrack("old-lyrics", "Old title", "Artist"), "old words")
    private val oldSong = searchTestTrack("old-song", "Old title", "Artist")

    private fun carried(filter: SearchFilter) = SearchSessionSnapshot(
        generation = 2L,
        query = "new remembered words",
        queryKey = "new remembered words",
        results = SearchResults(songs = listOf(oldSong), lyricsMatches = listOf(oldMatch)),
        carriedOver = true,
        filter = filter
    )

    @Test
    fun emptyCurrentLyricsPageClearsOldQueryMatchesInLyricsMode() {
        val current = carried(SearchFilter.Lyrics).withSectionPage(SearchFilter.Lyrics, SearchSectionPage(), "")
        val matches = lyricsSearchMatches(current.results, loaded = SearchFilter.Lyrics in current.sectionContinuations)

        assertFalse(current.carriedOver)
        assertTrue(current.results.lyricsMatches.isEmpty())
        assertTrue(matches.isEmpty())
        assertEquals(
            LyricsSearchContent.Empty,
            lyricsSearchContent(current.query, matches.size, loaded = true, loading = false, failed = false, pending = true)
        )
    }

    @Test
    fun emptyLyricsPageDoesNotChangeOrdinaryAllModeCarryOver() {
        val current = carried(SearchFilter.All).withSectionPage(SearchFilter.Lyrics, SearchSectionPage(), "")

        assertTrue(current.carriedOver)
        assertEquals(listOf(oldSong), current.results.songs)
    }

    @Test
    fun emptyUnrelatedSectionDoesNotDecideTheLyricsSearchOutcome() {
        val current = carried(SearchFilter.Lyrics).withSectionPage(SearchFilter.Videos, SearchSectionPage(), "")

        assertTrue(current.carriedOver)
        assertFalse(SearchFilter.Lyrics in current.sectionContinuations)
        assertTrue(lyricsSearchMatches(current.results, loaded = false).isEmpty())
    }

    @Test
    fun nonemptyCurrentLyricsPageReplacesPreviousQueryMatches() {
        val match = LyricsSearchMatch(searchTestTrack("new-lyrics", "Current title", "Artist"), "new words")
        val current = carried(SearchFilter.Lyrics).withSectionPage(
            SearchFilter.Lyrics, SearchSectionPage(lyricsMatches = listOf(match)), ""
        )

        assertFalse(current.carriedOver)
        assertEquals(listOf(match), current.results.lyricsMatches)
        assertFalse(current.results.lyricsMatches.any { it.track.id == oldMatch.track.id })
    }
}
