package com.yasuda.derbis.player

import com.yasuda.derbis.domain.AlternativeAudioSource
import com.yasuda.derbis.domain.AlternativeMatchVerdict
import com.yasuda.derbis.domain.ResolvedPlaybackManifest
import com.yasuda.derbis.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativeStreamCacheKeyTest {
    private fun track(url: String, kbps: Int?, providerTrackId: String = "pW-kkdqr"): Track = Track(
        id = "4NRXx6U8ABQ",
        title = "Blinding Lights",
        artist = "The Weeknd",
        album = "After Hours",
        durationMs = 200_000L,
        streamUrl = url,
        videoUrl = "https://www.youtube.com/watch?v=4NRXx6U8ABQ",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        playbackManifest = kbps?.let {
            ResolvedPlaybackManifest(
                sourceVideoId = "4NRXx6U8ABQ",
                provider = "Derbis HQ · JioSaavn",
                resolvedAtMs = 0L,
                expiresAtMs = 0L,
                durationMs = 200_000L,
                selectedAudioUrl = url,
                selectedVideoUrl = "",
                streams = emptyList(),
                alternativeSource = AlternativeAudioSource("jiosaavn", providerTrackId, it, AlternativeMatchVerdict.EXACT, 100)
            )
        }
    )

    @Test
    fun differentAlternativeTiersNeverShareCachedBytes() {
        val high = track("https://aac.saavncdn.com/820/hash_320.mp4", 320)
        val medium = track("https://aac.saavncdn.com/820/hash_160.mp4", 160)
        assertNotEquals(DerbisPlaybackCacheKey.stream(high), DerbisPlaybackCacheKey.stream(medium))
    }

    @Test
    fun alternativeNeverSharesCachedBytesWithNormalStream() {
        val alternative = track("https://aac.saavncdn.com/820/hash_320.mp4", 320)
        val normal = track("https://rr1---sn.googlevideo.com/videoplayback?itag=140", null)
        assertNotEquals(DerbisPlaybackCacheKey.stream(alternative), DerbisPlaybackCacheKey.stream(normal))
    }

    @Test
    fun sameAlternativeTierKeepsStableKeyAcrossUrls() {
        val signed = track("https://web.saavncdn.com/820/hash_320.mp4?Expires=1&Signature=a", 320)
        val open = track("https://aac.saavncdn.com/820/hash_320.mp4", 320)
        assertEquals(DerbisPlaybackCacheKey.stream(signed), DerbisPlaybackCacheKey.stream(open))
        assertTrue(DerbisPlaybackCacheKey.stream(open).startsWith("derbis:4NRXx6U8ABQ:stream-v2:alt-"))
    }

    @Test
    fun punctuationDistinctProviderIdsNeverCollide() {
        val slash = track("https://aac.saavncdn.com/820/hash_320.mp4", 320, providerTrackId = "song/a")
        val question = track("https://aac.saavncdn.com/820/hash_320.mp4", 320, providerTrackId = "song?a")
        assertNotEquals(DerbisPlaybackCacheKey.stream(slash), DerbisPlaybackCacheKey.stream(question))
    }
}
