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

        val incompleteSession = DerbisDiscordSession(accessToken = "oauth_token_123")
        assertFalse(incompleteSession.hasValidToken)

        val oauthSession = DerbisDiscordSession(accessToken = "oauth_token_123", refreshToken = "refresh_123")
        assertTrue(oauthSession.hasValidToken)
        assertEquals("oauth_token_123", oauthSession.accessToken)
        assertEquals("refresh_123", oauthSession.refreshToken)
    }

    @Test
    fun authorizationRequiresBothAnAuthorizedSdkAndApplicationId() {
        assertFalse(discordAuthorizationAvailable(false, 123L))
        assertFalse(discordAuthorizationAvailable(true, 0L))
        assertFalse(discordAuthorizationAvailable(true, -1L))
        assertTrue(discordAuthorizationAvailable(true, 123L))
    }

    @Test
    fun refreshTokenAloneIsNotReportedAsAConnectedSession() {
        assertFalse(DerbisDiscordSession(refreshToken = "refresh_123").hasValidToken)
    }

    @Test
    fun presenceTextNormalizesWhitespaceAndKeepsTheExistingLimit() {
        val normalized = derbisDiscordPresenceSnapshot(
            sampleTrack(title = "  Never   Gonna\nGive You Up  ", artist = "Rick\tAstley"), 0L, true
        )
        assertEquals("Never Gonna Give You Up", normalized.title)
        assertEquals("Rick Astley", normalized.artist)
        assertEquals(128, derbisDiscordPresenceSnapshot(sampleTrack(title = "x".repeat(200)), 0L, true).title.length)
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
