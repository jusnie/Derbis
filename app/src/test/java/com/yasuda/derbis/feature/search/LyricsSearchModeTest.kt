package com.yasuda.derbis.feature.search

import com.yasuda.derbis.domain.ArtistHit
import com.yasuda.derbis.domain.LyricsSearchMatch
import com.yasuda.derbis.domain.SearchArtistDiscography
import com.yasuda.derbis.domain.SearchFilter
import com.yasuda.derbis.domain.SearchResults
import com.yasuda.derbis.domain.SearchSuggestionBundle
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsSearchModeTest {
    private val dispatcher: ExecutorCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val backend = LyricsModeBackend()

    @After
    fun tearDown() {
        scope.cancel()
        dispatcher.close()
    }

    private fun engine(timing: SearchTiming = SearchTiming(typingDebounceMs = 0L, suggestionWaitCapMs = 5_000L)) =
        DerbisSearchEngine(scope, backend, computeDispatcher = dispatcher, timing = timing, ioDispatcher = dispatcher)

    private suspend fun DerbisSearchEngine.awaitQuery(query: String): SearchSessionSnapshot =
        withTimeout(4_000L) { state.first { it.queryKey == searchQueryKey(query) } }

    private suspend fun DerbisSearchEngine.awaitLyrics(query: String): SearchSessionSnapshot {
        assertEquals(SearchFilter.Lyrics, awaitQuery(query).filter)
        return withTimeout(4_000L) {
            state.first {
                it.queryKey == searchQueryKey(query) &&
                    SearchFilter.Lyrics in it.sectionContinuations && it.settled
            }
        }
    }

    private suspend fun awaitCondition(condition: () -> Boolean) {
        withTimeout(4_000L) { while (!condition()) delay(5L) }
    }

    @Test
    fun lyricsSelectedBeforeTypingSurvivesTheFirstQueryAndFetchesLyrics() = runBlocking {
        val engine = engine()
        engine.selectFilter(SearchFilter.Lyrics)
        engine.submit("golden lines", "en")

        val result = engine.awaitLyrics("golden lines")

        assertEquals(listOf("lyrics-golden lines"), result.results.lyricsMatches.map { it.track.id })
        assertEquals(1, backend.lyricsCalls.count { it == "golden lines" })
    }

    @Test
    fun replacingTheQueryAndSearchingAgainDoesNotNeedAnotherLyricsTap() = runBlocking {
        val engine = engine()
        engine.selectFilter(SearchFilter.Lyrics)
        engine.submit("golden lines", "en")
        engine.awaitLyrics("golden lines")

        engine.onQueryChanged("midnight voices", "en")
        assertEquals(SearchFilter.Lyrics, engine.awaitQuery("midnight voices").filter)
        engine.submit("midnight voices", "en")
        val result = engine.awaitLyrics("midnight voices")

        assertEquals(listOf("lyrics-midnight voices"), result.results.lyricsMatches.map { it.track.id })
        assertTrue(backend.lyricsCalls.containsAll(listOf("golden lines", "midnight voices")))
    }

    @Test
    fun clearingOrShorteningTheQueryDoesNotTurnLyricsOff() = runBlocking {
        val engine = engine()
        engine.selectFilter(SearchFilter.Lyrics)
        engine.submit("golden lines", "en")
        val first = engine.awaitLyrics("golden lines")
        engine.onQueryChanged("", "en")
        val cleared = withTimeout(4_000L) {
            engine.state.first { it.generation > first.generation && it.queryKey.isEmpty() }
        }
        assertEquals(SearchFilter.Lyrics, cleared.filter)
        assertTrue(cleared.results.isEmpty)
        assertTrue(cleared.sectionLoading.isEmpty())
        engine.onQueryChanged("a", "en")
        delay(30L)
        assertEquals(SearchFilter.Lyrics, engine.state.value.filter)
        engine.submit("midnight voices", "en")
        assertEquals(SearchFilter.Lyrics, engine.awaitLyrics("midnight voices").filter)
    }

    @Test
    fun returningToCachedResultsKeepsLyricsAndUsesTheCachedLyricsPage() = runBlocking {
        val engine = engine()
        engine.selectFilter(SearchFilter.Lyrics)
        engine.submit("golden lines", "en")
        engine.awaitLyrics("golden lines")
        engine.submit("midnight voices", "en")
        engine.awaitLyrics("midnight voices")
        engine.submit("golden lines", "en")

        val result = engine.awaitLyrics("golden lines")

        assertEquals("lyrics-golden lines", result.results.lyricsMatches.single().track.id)
        assertEquals(1, backend.lyricsCalls.count { it == "golden lines" })
    }

    @Test
    fun explicitlyTurningLyricsOffKeepsTheFollowingSearchNormal() = runBlocking {
        val engine = engine()
        engine.selectFilter(SearchFilter.Lyrics)
        engine.submit("golden lines", "en")
        engine.awaitLyrics("golden lines")
        engine.selectFilter(SearchFilter.All)
        engine.submit("midnight voices", "en")
        val result = withTimeout(4_000L) {
            engine.state.first { it.queryKey == "midnight voices" && it.settled }
        }

        assertEquals(SearchFilter.All, result.filter)
        assertFalse("midnight voices" in backend.lyricsCalls)
    }

    @Test
    fun selectingAnotherFilterExplicitlyLeavesLyricsModeWithoutChangingNormalFilterPolicy() = runBlocking {
        val engine = engine()
        engine.selectFilter(SearchFilter.Lyrics)
        engine.submit("golden lines", "en")
        engine.awaitLyrics("golden lines")
        engine.selectFilter(SearchFilter.Songs)
        engine.submit("midnight voices", "en")
        val result = withTimeout(4_000L) {
            engine.state.first { it.queryKey == "midnight voices" && it.settled }
        }

        assertEquals(SearchFilter.All, result.filter)
        assertFalse("midnight voices" in backend.lyricsCalls)
    }

    @Test
    fun retainedLyricsSearchStillWaitsForTheExistingTypingOrSubmitTrigger() = runBlocking {
        backend.suggestionsGate = CompletableDeferred()
        val engine = engine(SearchTiming(typingDebounceMs = 300L, suggestionWaitCapMs = 5_000L))
        engine.selectFilter(SearchFilter.Lyrics)
        engine.onQueryChanged("golden lines", "en")
        assertEquals(SearchFilter.Lyrics, engine.awaitQuery("golden lines").filter)
        delay(60L)
        assertTrue(backend.lyricsCalls.isEmpty())

        engine.submit("golden lines", "en")
        engine.awaitLyrics("golden lines")
        assertEquals(1, backend.lyricsCalls.count { it == "golden lines" })
    }

    @Test
    fun lateLyricsFromTheOldQueryCannotReplaceTheNewQuery() = runBlocking {
        val oldPage = CompletableDeferred<SearchSectionPage>()
        backend.gates["golden lines"] = oldPage
        val engine = engine()
        engine.selectFilter(SearchFilter.Lyrics)
        engine.submit("golden lines", "en")
        assertEquals(SearchFilter.Lyrics, engine.awaitQuery("golden lines").filter)
        awaitCondition { "golden lines" in backend.lyricsCalls }
        engine.submit("midnight voices", "en")
        engine.awaitLyrics("midnight voices")
        oldPage.complete(backend.lyricsPage("golden lines"))
        delay(50L)

        assertEquals("midnight voices", engine.state.value.queryKey)
        assertEquals(SearchFilter.Lyrics, engine.state.value.filter)
        assertEquals("lyrics-midnight voices", engine.state.value.results.lyricsMatches.single().track.id)
    }

    @Test
    fun turningLyricsOffWhileItsPageLoadsIsNotUndoneByTheLateResponse() = runBlocking {
        val page = CompletableDeferred<SearchSectionPage>()
        backend.gates["golden lines"] = page
        val engine = engine()
        engine.selectFilter(SearchFilter.Lyrics)
        engine.submit("golden lines", "en")
        assertEquals(SearchFilter.Lyrics, engine.awaitQuery("golden lines").filter)
        awaitCondition { "golden lines" in backend.lyricsCalls }
        engine.selectFilter(SearchFilter.All)
        page.complete(backend.lyricsPage("golden lines"))
        awaitCondition { SearchFilter.Lyrics in engine.state.value.sectionContinuations }
        assertEquals(SearchFilter.All, engine.state.value.filter)

        engine.submit("midnight voices", "en")
        withTimeout(4_000L) { engine.state.first { it.queryKey == "midnight voices" && it.settled } }
        assertEquals(SearchFilter.All, engine.state.value.filter)
        assertFalse("midnight voices" in backend.lyricsCalls)
    }

    @Test
    fun failedSearchAndRetryKeepLyricsSelected() = runBlocking {
        backend.failing += "golden lines"
        val engine = engine()
        engine.selectFilter(SearchFilter.Lyrics)
        engine.submit("golden lines", "en")
        val failed = withTimeout(4_000L) { engine.state.first { it.failure == SearchFailure.Failed } }
        assertEquals(SearchFilter.Lyrics, failed.filter)
        backend.failing -= "golden lines"
        engine.submit("golden lines", "en")

        val retried = engine.awaitLyrics("golden lines")

        assertEquals(SearchFailure.None, retried.failure)
        assertEquals("lyrics-golden lines", retried.results.lyricsMatches.single().track.id)
    }

    @Test
    fun anEngineResetClearsResultsWithoutSilentlyTurningLyricsOff() = runBlocking {
        val engine = engine()
        engine.selectFilter(SearchFilter.Lyrics)
        engine.submit("golden lines", "en")
        val first = engine.awaitLyrics("golden lines")
        engine.reset()
        val reset = withTimeout(4_000L) {
            engine.state.first { it.generation > first.generation && it.queryKey.isEmpty() }
        }

        assertEquals(SearchFilter.Lyrics, reset.filter)
        assertTrue(reset.results.isEmpty)
    }

    @Test
    fun laterOverviewResultsDoNotEraseAnAlreadyLoadedLyricsMatch() {
        val match = LyricsSearchMatch(searchTestTrack("lyrics-one", "Golden Lines", "Artist"), "golden lines", "matched lyric snippet")
        val merged = mergeFastSearchResults(
            SearchResults(lyricsMatches = listOf(match)),
            SearchResults(songs = listOf(searchTestTrack("song-one", "Golden Lines", "Artist"))),
            "golden lines"
        )

        assertEquals(listOf(match), merged.lyricsMatches)
        assertEquals("song-one", merged.songs.single().id)
    }

    @Test
    fun richSuggestionsKeepLyricsMatchesAndDuplicateTracksAreNotAddedTwice() {
        val match = LyricsSearchMatch(searchTestTrack("lyrics-one", "Golden Lines", "Artist"), "golden lines")
        val base = SearchResults(lyricsMatches = listOf(match))
        val suggestions = mergeRichSuggestionResults(
            base,
            SearchSuggestionBundle(songs = listOf(searchTestTrack("song-one", "Golden Lines", "Artist"))),
            "golden lines"
        )
        val repeated = mergeFastSearchResults(suggestions, base, "golden lines")

        assertEquals(listOf(match), suggestions.lyricsMatches)
        assertEquals(listOf(match), repeated.lyricsMatches)
    }

    @Test
    fun aNewRelatedQueryNeverKeepsThePreviousQuerysLyricsMatches() {
        val match = LyricsSearchMatch(searchTestTrack("old-lyrics", "Old Song", "Artist"), "old line")
        val carried = SearchSessionSnapshot(
            query = "golden lines extended",
            queryKey = "golden lines extended",
            results = SearchResults(lyricsMatches = listOf(match)),
            carriedOver = true,
            filter = SearchFilter.Lyrics
        )
        val current = carried.withOverview(
            SearchResults(songs = listOf(searchTestTrack("current-song", "New Song", "Artist"))),
            "golden lines extended"
        )

        assertTrue(current.results.lyricsMatches.isEmpty())
        assertEquals(SearchFilter.Lyrics, current.filter)
    }

    @Test
    fun ordinarySearchStillStartsInAllWithoutMakingALyricsRequest() = runBlocking {
        val engine = engine()
        engine.submit("golden lines", "en")
        val result = withTimeout(4_000L) {
            engine.state.first { it.queryKey == "golden lines" && it.settled }
        }

        assertEquals(SearchFilter.All, result.filter)
        assertTrue(backend.lyricsCalls.isEmpty())
    }
}

private class LyricsModeBackend : SearchBackend {
    val lyricsCalls = CopyOnWriteArrayList<String>()
    val gates = ConcurrentHashMap<String, CompletableDeferred<SearchSectionPage>>()
    val failing: MutableSet<String> = ConcurrentHashMap.newKeySet()
    var suggestionsGate: CompletableDeferred<SearchSuggestionBundle>? = null

    fun lyricsPage(query: String) = SearchSectionPage(
        lyricsMatches = listOf(LyricsSearchMatch(searchTestTrack("lyrics-$query", query, "Matched artist"), query))
    )

    override fun localCandidates(): List<LocalSearchCandidate> = emptyList()
    override suspend fun suggestions(query: String, languageCode: String): SearchSuggestionBundle =
        suggestionsGate?.await() ?: SearchSuggestionBundle()

    override suspend fun overview(query: String, languageCode: String): SearchResults {
        if (query in failing) throw IOException("Search failed")
        return SearchResults(songs = List(3) { index -> searchTestTrack("$query-$index", "$query $index", "Artist $index") })
    }

    override suspend fun section(filter: SearchFilter, query: String, languageCode: String, continuation: String): SearchSectionPage {
        if (filter == SearchFilter.Lyrics) lyricsCalls += query
        if (query in failing) throw IOException("Section failed")
        return if (filter == SearchFilter.Lyrics) gates[query]?.await() ?: lyricsPage(query) else SearchSectionPage()
    }

    override suspend fun exactArtist(query: String): ArtistHit? = null
    override suspend fun officialArtists(candidates: List<ArtistHit>): List<ArtistHit> = candidates
    override suspend fun officialDiscography(artist: ArtistHit): SearchArtistDiscography? = null
    override fun overviewHedgeDelayMs(): Long = 60_000L
}
