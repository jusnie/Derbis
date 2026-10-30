package com.yasuda.derbis.feature.motion

import com.yasuda.derbis.domain.SearchResults
import com.yasuda.derbis.domain.Track
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyCanvasProviderTest {
    private val clients = mutableListOf<OkHttpClient>()

    @After
    fun tearDown() {
        clients.forEach {
            it.dispatcher.executorService.shutdownNow()
            it.connectionPool.evictAll()
        }
    }

    @Test
    fun spotifyIdentityFetchesPublicCanvasWithoutSearchingOrSigningIn() = runBlocking {
        val fixture = fixture()
        val identity = MotionTrackIdentity.from(track())

        val result = fixture.provider.find(identity) as MotionArtworkProviderResult.Found

        assertEquals(1, fixture.calls.get())
        assertEquals("/provider/canvaslist/$SPOTIFY_ID", fixture.path.get())
        assertEquals(identity, result.candidates.single().identity)
        assertEquals(CANVAS_URL, result.candidates.single().url)
    }

    @Test
    fun youtubeIdentityResolvesVerifiedSpotifyMetadata() = runBlocking {
        val queries = mutableListOf<String>()
        val fixture = fixture(search = { query, limit ->
            queries += query
            assertEquals(6, limit)
            SearchResults(songs = listOf(track()))
        })
        val identity = MotionTrackIdentity.from(track().copy(id = "abcdefghijk", source = "YouTube Music"))

        val result = fixture.provider.find(identity) as MotionArtworkProviderResult.Found

        assertEquals(listOf("Espresso Sabrina Carpenter"), queries)
        assertEquals("spotify:$SPOTIFY_ID", result.candidates.single().identity.trackId)
        assertEquals("/provider/canvaslist/$SPOTIFY_ID", fixture.path.get())
        assertTrue(CanonicalTrackMatcher.match(identity, result.candidates.single()).accepted)
    }

    @Test
    fun wrongTopResultCannotReplaceTheMatchingRecording() = runBlocking {
        val wrong = track().copy(id = "spotify:$OTHER_ID", artist = "Another Artist")
        val fixture = fixture(search = { _, _ ->
            SearchResults(topTrack = wrong, songs = listOf(wrong, track()))
        })
        val identity = MotionTrackIdentity.from(track().copy(id = "abcdefghijk"))

        val result = fixture.provider.find(identity) as MotionArtworkProviderResult.Found

        assertEquals("spotify:$SPOTIFY_ID", result.candidates.single().identity.trackId)
        assertEquals("Sabrina Carpenter", result.candidates.single().identity.artists.single())
    }

    @Test
    fun isrcMissFallsBackToTitleAndArtistSearch() = runBlocking {
        val queries = mutableListOf<String>()
        val fixture = fixture(search = { query, _ ->
            queries += query
            if (query.startsWith("isrc:")) SearchResults() else SearchResults(songs = listOf(track()))
        })
        val identity = MotionTrackIdentity.from(track().copy(id = "abcdefghijk", isrc = "USUM72403305"))

        val result = fixture.provider.find(identity)

        assertTrue(result is MotionArtworkProviderResult.Found)
        assertEquals(listOf("isrc:USUM72403305", "Espresso Sabrina Carpenter"), queries)
    }

    @Test
    fun opaqueCatalogIdsAreNotMistakenForSpotifyIds() = runBlocking {
        val fixture = fixture(search = { _, _ ->
            SearchResults(songs = listOf(track().copy(id = "spotify:derbis-f9f6f8fac4ee25604a42")))
        })
        val identity = MotionTrackIdentity.from(track().copy(id = "abcdefghijk"))

        assertEquals(MotionArtworkProviderResult.NoMatch, fixture.provider.find(identity))
        assertEquals(0, fixture.calls.get())
    }

    @Test
    fun conflictingRecordingIsRejectedBeforeCanvasRequest() = runBlocking {
        val fixture = fixture(search = { _, _ ->
            SearchResults(songs = listOf(track().copy(isrc = "USUM72499999")))
        })
        val identity = MotionTrackIdentity.from(track().copy(id = "abcdefghijk", isrc = "USUM72403305"))

        assertEquals(MotionArtworkProviderResult.NoMatch, fixture.provider.find(identity))
        assertEquals(0, fixture.calls.get())
    }

    @Test
    fun unavailableCanvasRemainsARealMiss() = runBlocking {
        val fixture = fixture(body = """{"data":{"$SPOTIFY_ID":null}}""")

        assertEquals(
            MotionArtworkProviderResult.NoMatch,
            fixture.provider.find(MotionTrackIdentity.from(track()))
        )
    }

    @Test
    fun insecureMediaIsNotReturnedAsArtwork() = runBlocking {
        val fixture = fixture(body = """{"data":{"$SPOTIFY_ID":{"url":"http://canvaz.scdn.co/loop.mp4"}}}""")

        assertEquals(
            MotionArtworkProviderResult.NoMatch,
            fixture.provider.find(MotionTrackIdentity.from(track()))
        )
    }

    @Test
    fun cancellingSearchCancelsLookupInsteadOfProducingAMiss() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val fixture = fixture(search = { _, _ ->
            started.complete(Unit)
            awaitCancellation()
        })
        var completed = false
        val lookup = launch {
            fixture.provider.find(MotionTrackIdentity.from(track().copy(id = "abcdefghijk")))
            completed = true
        }

        withTimeout(2_000L) { started.await() }
        lookup.cancelAndJoin()

        assertTrue(lookup.isCancelled)
        assertFalse(completed)
        assertEquals(0, fixture.calls.get())
    }

    @Test
    fun publicServiceFailureDoesNotBecomeFakeArtwork() = runBlocking {
        val fixture = fixture(code = 503)

        assertTrue(
            fixture.provider.find(MotionTrackIdentity.from(track())) is MotionArtworkProviderResult.Failed
        )
    }

    private data class Fixture(
        val provider: SpotifyCanvasProvider,
        val calls: AtomicInteger,
        val path: AtomicReference<String>
    )

    private fun fixture(
        body: String = """{"data":{"$SPOTIFY_ID":{"url":"$CANVAS_URL"}}}""",
        code: Int = 200,
        search: suspend (String, Int) -> SearchResults = { _, _ -> error("Unexpected catalog search") }
    ): Fixture {
        val calls = AtomicInteger()
        val path = AtomicReference("")
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls.incrementAndGet()
            path.set(chain.request().url.encodedPath)
            assertEquals("application/json", chain.request().header("Accept"))
            assertEquals(null, chain.request().header("Authorization"))
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }.build().also { clients += it }
        return Fixture(SpotifyCanvasProvider(client, search), calls, path)
    }

    private fun track(): Track = Track(
        id = "spotify:$SPOTIFY_ID",
        title = "Espresso",
        artist = "Sabrina Carpenter",
        album = "Short n' Sweet",
        durationMs = 175_459L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "spotify",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )

    companion object {
        private const val SPOTIFY_ID = "2qSkIjg1o9h3YT9RAgYN75"
        private const val OTHER_ID = "4LRPiXqCikLlN15c3yImP7"
        private const val CANVAS_URL = "https://canvaz.scdn.co/upload/artist/test/video/loop.cnvs.mp4"
    }
}
