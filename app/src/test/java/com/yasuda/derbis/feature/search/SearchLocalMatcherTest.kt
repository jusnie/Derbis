package com.yasuda.derbis.feature.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchLocalMatcherTest {
    @Test
    fun `exact title is a strong first match`() {
        val matches = matchLocalSearchTracks(
            query = "sirio",
            candidates = listOf(
                LocalSearchCandidate(searchTestTrack("r1", "Sirio Remix", "Other"), LocalSearchAffinity.RECENT),
                LocalSearchCandidate(searchTestTrack("a1", "Sirio", "Geolier"), LocalSearchAffinity.CACHE)
            )
        )

        assertEquals(listOf("a1", "r1"), matches.map { it.track.id })
        assertTrue(matches.first().strong)
        assertFalse(matches.last().strong)
    }

    @Test
    fun `partial last token matches while typing`() {
        val matches = matchLocalSearchTracks(
            query = "geoli",
            candidates = listOf(LocalSearchCandidate(searchTestTrack("g1", "I P' ME", "Geolier"), LocalSearchAffinity.HOME))
        )

        assertEquals(listOf("g1"), matches.map { it.track.id })
        assertFalse(matches.single().strong)
    }

    @Test
    fun `every token must match title or artist`() {
        val matches = matchLocalSearchTracks(
            query = "geolier adele",
            candidates = listOf(LocalSearchCandidate(searchTestTrack("g1", "I P' ME", "Geolier"), LocalSearchAffinity.HOME))
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `title plus artist query is strong`() {
        val matches = matchLocalSearchTracks(
            query = "hello adele",
            candidates = listOf(LocalSearchCandidate(searchTestTrack("h1", "Hello", "Adele"), LocalSearchAffinity.FAVORITE))
        )

        assertTrue(matches.single().strong)
    }

    @Test
    fun `duplicate identity keeps the highest affinity source`() {
        val track = searchTestTrack("x1", "Hello", "Adele")
        val matches = matchLocalSearchTracks(
            query = "hello",
            candidates = listOf(
                LocalSearchCandidate(track, LocalSearchAffinity.RECENT),
                LocalSearchCandidate(track.copy(album = "25"), LocalSearchAffinity.CACHE)
            )
        )

        assertEquals(1, matches.size)
        assertEquals(1_000 + LocalSearchAffinity.RECENT, matches.single().score)
    }

    @Test
    fun `rejected and blank identity tracks are skipped`() {
        val matches = matchLocalSearchTracks(
            query = "hello",
            candidates = listOf(
                LocalSearchCandidate(searchTestTrack("", "Hello", "Adele"), LocalSearchAffinity.RECENT),
                LocalSearchCandidate(searchTestTrack("blocked", "Hello There", "Someone"), LocalSearchAffinity.RECENT),
                LocalSearchCandidate(searchTestTrack("ok", "Hello World", "Someone Else"), LocalSearchAffinity.RECENT)
            ),
            accept = { it.id != "blocked" }
        )

        assertEquals(listOf("ok"), matches.map { it.track.id })
    }

    @Test
    fun `a single typo still finds the track`() {
        // "taylr" is one missing letter from the artist the listener meant. Before the typo pass
        // this searched a library and returned nothing.
        val candidates = listOf(
            LocalSearchCandidate(searchTestTrack("t1", "Anti-Hero", "Taylor Swift"), LocalSearchAffinity.HOME),
            LocalSearchCandidate(searchTestTrack("t2", "Someone Else", "Adele"), LocalSearchAffinity.HOME)
        )

        val matches = matchLocalSearchTracks("taylr", candidates)

        assertEquals(listOf("t1"), matches.map { it.track.id })
        // A corrected match is a suggestion, never a strong result.
        assertFalse(matches.single().strong)
    }

    @Test
    fun `a swapped pair of letters still finds the track`() {
        val candidates = listOf(
            LocalSearchCandidate(searchTestTrack("s1", "Love Story", "Taylor Swift"), LocalSearchAffinity.HOME)
        )

        assertEquals(listOf("s1"), matchLocalSearchTracks("swfit", candidates).map { it.track.id })
    }

    @Test
    fun `typo search never outranks an exact match`() {
        val candidates = listOf(
            LocalSearchCandidate(searchTestTrack("typo", "Jaz", "Someone"), LocalSearchAffinity.RECENT),
            LocalSearchCandidate(searchTestTrack("exact", "Jazz", "Other"), LocalSearchAffinity.CACHE)
        )

        val matches = matchLocalSearchTracks("jazz", candidates)

        // The strict pass answered, so the fuzzy pass never runs and the exact title leads.
        assertEquals(listOf("exact"), matches.map { it.track.id })
        assertTrue(matches.single().strong)
    }

    @Test
    fun `two edits away is not a typo`() {
        val candidates = listOf(
            LocalSearchCandidate(searchTestTrack("x1", "Hello", "Adele"), LocalSearchAffinity.HOME)
        )

        assertTrue(matchLocalSearchTracks("hxxlo", candidates).isEmpty())
    }

    @Test
    fun `short tokens are not corrected`() {
        // "ed" is one edit from too many words to be useful; correcting it would bury real results.
        val candidates = listOf(
            LocalSearchCandidate(searchTestTrack("x1", "Ad", "Al"), LocalSearchAffinity.HOME)
        )

        assertTrue(matchLocalSearchTracks("ed", candidates).isEmpty())
    }

    @Test
    fun `typo matching still honours the accept filter`() {
        val candidates = listOf(
            LocalSearchCandidate(searchTestTrack("blocked", "Anti-Hero", "Taylor Swift"), LocalSearchAffinity.HOME)
        )

        assertTrue(matchLocalSearchTracks("taylr", candidates, accept = { it.id != "blocked" }).isEmpty())
    }

    @Test
    fun `edit distance treats one substitution, deletion, insertion and swap as one`() {
        assertTrue(withinOneEdit("taylor", "taylro"))
        assertTrue(withinOneEdit("taylor", "taylr"))
        assertTrue(withinOneEdit("taylor", "taylors"))
        assertTrue(withinOneEdit("taylor", "tailor"))
        assertTrue(withinOneEdit("taylor", "taylor"))
        assertFalse(withinOneEdit("taylor", "taylorss"))
        assertFalse(withinOneEdit("taylor", "tylaro"))
        // A one-character substitution is one edit by definition; single-letter tokens are kept
        // out of the typo pass by length, not by this function.
        assertTrue(withinOneEdit("i", "a"))
        assertTrue(withinOneEdit("", "a"))
        assertFalse(withinOneEdit("", "ab"))
    }

    @Test
    fun `short queries and limits are respected`() {
        val candidates = (1..10).map { index ->
            LocalSearchCandidate(searchTestTrack("t$index", "Love $index", "Artist $index", durationMs = 100_000L + index), 0)
        }

        assertTrue(matchLocalSearchTracks("l", candidates).isEmpty())
        assertEquals(3, matchLocalSearchTracks("love", candidates, limit = 3).size)
    }
}
