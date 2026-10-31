package com.yasuda.derbis.feature.discord

import com.yasuda.derbis.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordRichPresenceTest {

    private fun sampleTrack(
        id: String = "dQw4w9WgXcQ",
        title: String = "Never Gonna Give You Up",
        artist: String = "Rick Astley",
        durationMs: Long = 213_000L,
        artistBrowseId: String = "UCuAXFkgsw1L7xaCfnd5JJOw"
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Whenever You Need Somebody",
        durationMs = durationMs,
        streamUrl = "https://example.com/audio.opus",
        videoUrl = "https://youtube.com/watch?v=$id",
        thumbnailUrl = "https://lh3.googleusercontent.com/thumbnail.jpg",
        largeThumbnailUrl = "https://lh3.googleusercontent.com/large_thumbnail.jpg",
        source = "youtube",
        moodTags = emptySet(),
        energy = 80,
        vocal = 90,
        replayScore = 10,
        cacheScore = 10,
        accentStart = 0,
        accentEnd = 0,
        artistBrowseIds = listOf(artistBrowseId)
    )

    @Test
    fun testDerbisDiscordPresenceSnapshotCreation() {
        val track = sampleTrack()
        val snapshot = derbisDiscordPresenceSnapshot(
            track = track,
            positionMs = 45_000L,
            durationMs = 213_000L,
            playing = true
        )

        assertEquals("dQw4w9WgXcQ", snapshot.trackId)
        assertEquals("Never Gonna Give You Up", snapshot.title)
        assertEquals("Rick Astley", snapshot.artist)
        assertEquals("UCuAXFkgsw1L7xaCfnd5JJOw", snapshot.artistId)
        assertEquals("https://lh3.googleusercontent.com/large_thumbnail.jpg", snapshot.artworkUrl)
        assertEquals(45_000L, snapshot.positionMs)
        assertEquals(213_000L, snapshot.durationMs)
        assertEquals("https://derbis-web.vercel.app/t/dQw4w9WgXcQ", snapshot.songUrl)
        assertEquals("https://music.youtube.com/channel/UCuAXFkgsw1L7xaCfnd5JJOw", snapshot.artistUrl)
        assertTrue(snapshot.playing)
    }

    @Test
    fun testProgressBarTimestampMath() {
        val now = 1_700_000_000_000L
        val positionMs = 30_000L
        val durationMs = 180_000L

        val startMs = now - positionMs
        val endMs = startMs + durationMs

        assertEquals(now - 30_000L, startMs)
        assertEquals(startMs + 180_000L, endMs)
        assertEquals(durationMs, endMs - startMs)
        assertTrue("End timestamp must be strictly greater than start timestamp", endMs > startMs)
    }

    @Test
    fun testDerbisDiscordSessionValidity() {
        val emptySession = DerbisDiscordSession()
        assertFalse(emptySession.hasValidToken)

        val oauthSession = DerbisDiscordSession(accessToken = "oauth_token_123")
        assertTrue(oauthSession.hasValidToken)
        assertEquals("oauth_token_123", oauthSession.effectiveToken)

        val userTokenSession = DerbisDiscordSession(
            accessToken = "oauth_token_123",
            userToken = "direct_user_token_456"
        )
        assertTrue(userTokenSession.hasValidToken)
        assertEquals("direct_user_token_456", userTokenSession.effectiveToken)
    }

    @Test
    fun testSnapshotSyncKeyDistinctness() {
        val track1 = sampleTrack(id = "track_1", title = "Song A")
        val track2 = sampleTrack(id = "track_2", title = "Song B")

        val snap1 = derbisDiscordPresenceSnapshot(track1, 10_000L, 200_000L, true)
        val snap2 = derbisDiscordPresenceSnapshot(track2, 10_000L, 200_000L, true)

        assertTrue(snap1.syncKey != snap2.syncKey)
    }

    @Test
    fun testLyricLineIsSanitizedAndCappedAtDiscordLimit() {
        val noisy = "  I   never   wanna\tbe  " + "x".repeat(200)
        val snapshot = derbisDiscordPresenceSnapshot(
            track = sampleTrack(),
            positionMs = 12_000L,
            durationMs = 213_000L,
            playing = true,
            lyricLine = noisy
        )

        assertTrue(snapshot.lyricLine.startsWith("I never wanna be"))
        assertFalse(snapshot.lyricLine.contains("  "))
        assertEquals(128, snapshot.lyricLine.length)
    }

    @Test
    fun testLyricLineClearedWhenPausedOrTrackMissing() {
        val playing = derbisDiscordPresenceSnapshot(sampleTrack(), 1_000L, 213_000L, true, "hello")
        assertEquals("hello", playing.lyricLine)

        val paused = derbisDiscordPresenceSnapshot(sampleTrack(), 1_000L, 213_000L, false, "hello")
        assertEquals("", paused.lyricLine)

        val noTrack = derbisDiscordPresenceSnapshot(null, 1_000L, 0L, true, "hello")
        assertEquals("", noTrack.lyricLine)
    }

    @Test
    fun testSyncKeyTracksLyricLineChanges() {
        val track = sampleTrack()
        val first = derbisDiscordPresenceSnapshot(track, 5_000L, 213_000L, true, "line one")
        val second = derbisDiscordPresenceSnapshot(track, 5_000L, 213_000L, true, "line two")

        assertTrue(first.syncKey != second.syncKey)
    }

    @Test
    fun testArtistTitleLineKeepsTrackVisibleDuringLyrics() {
        val snapshot = derbisDiscordPresenceSnapshot(sampleTrack(), 0L, 213_000L, true, "line")

        assertEquals("Rick Astley — Never Gonna Give You Up", snapshot.artistTitleLine)
    }

    @Test
    fun testLyricPositionHonoursLatencyOffset() {
        // Positive offset = lyrics should trail the audio clock (Bluetooth lag).
        assertEquals(28_000L, discordLyricPositionMs(positionMs = 30_000L, offsetMs = 2_000L))
        // Negative offset = lyrics should lead it.
        assertEquals(32_000L, discordLyricPositionMs(positionMs = 30_000L, offsetMs = -2_000L))
        // Never resolves a line before the track starts.
        assertEquals(0L, discordLyricPositionMs(positionMs = 500L, offsetMs = 5_000L))
        assertEquals(30_000L, discordLyricPositionMs(positionMs = 30_000L, offsetMs = 0L))
    }

    @Test
    fun testPresenceRateGateHonoursDiscordLimit() {
        // Nothing sent yet, so the first update goes out immediately.
        assertTrue(shouldFlushPresenceNow(nowElapsedMs = 90_000L, lastSentElapsedMs = 0L))
        // Too soon after the last update: coalesce into the trailing flush instead.
        assertFalse(shouldFlushPresenceNow(nowElapsedMs = 100_000L, lastSentElapsedMs = 97_000L))
        // Exactly at the interval boundary: allowed.
        assertTrue(shouldFlushPresenceNow(nowElapsedMs = 101_000L, lastSentElapsedMs = 97_000L))
        // 4 s spacing yields at most five updates per 20 s window.
        assertEquals(4_000L, DISCORD_PRESENCE_MIN_INTERVAL_MS)
        assertEquals(5, (20_000L / DISCORD_PRESENCE_MIN_INTERVAL_MS).toInt())
    }

    @Test
    fun testSnapshotNullTrackSafety() {
        val snapshot = derbisDiscordPresenceSnapshot(
            track = null,
            positionMs = 0L,
            durationMs = 0L,
            playing = false
        )

        assertEquals("", snapshot.title)
        assertEquals("", snapshot.artist)
        assertEquals("", snapshot.artworkUrl)
        assertFalse(snapshot.playing)
    }
}
