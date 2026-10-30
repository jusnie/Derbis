package com.yasuda.derbis.feature.discord

import com.yasuda.derbis.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordClickablePresenceTest {
    private fun presence() = DerbisDiscordPresenceSnapshot(
        trackId = "dQw4w9WgXcQ",
        title = "Never Gonna Give You Up",
        artist = "Rick Astley",
        artworkUrl = "https://example.com/album.jpg",
        artistThumbnailUrl = "https://example.com/artist.jpg",
        positionMs = 45_000L,
        durationMs = 213_000L,
        songUrl = "https://derbis-web.vercel.app/t/dQw4w9WgXcQ",
        artistUrl = "https://music.youtube.com/channel/UCuAXFkgsw1L7xaCfnd5JJOw",
        playing = true
    )

    @Test
    fun activityPayloadKeepsTheClickableSongArtistAndArtworkData() {
        val source = presence()
        val payload = derbisDiscordActivityPayload(source, 1_700_000_000_000L)

        assertEquals(source.songUrl, payload.songUrl)
        assertEquals(source.artistUrl, payload.artistUrl)
        assertEquals(source.artworkUrl, payload.artworkUrl)
        assertEquals(source.artistThumbnailUrl, payload.artistThumbnailUrl)
        assertEquals("Never Gonna Give You Up — Rick Astley", payload.artworkText)
    }

    @Test
    fun sdkProgressUsesEpochMillisecondsAndAnEndTimestamp() {
        val now = 1_700_000_000_000L
        val payload = derbisDiscordActivityPayload(presence(), now)

        assertEquals(now - 45_000L, payload.startedAtEpochMs)
        assertEquals(now - 45_000L + 213_000L, payload.endsAtEpochMs)
        assertEquals(213_000L, payload.endsAtEpochMs - payload.startedAtEpochMs)
    }

    @Test
    fun unknownDurationHasNoInventedEndTimestamp() {
        val payload = derbisDiscordActivityPayload(presence().copy(durationMs = 0L), 1_700_000_000_000L)

        assertEquals(0L, payload.endsAtEpochMs)
        assertTrue(payload.startedAtEpochMs > 0L)
    }

    @Test
    fun positionPastTheEndDoesNotCreateANegativeRemainingTime() {
        val now = 1_700_000_000_000L
        val payload = derbisDiscordActivityPayload(presence().copy(positionMs = 999_000L), now)

        assertEquals(now, payload.endsAtEpochMs)
    }

    @Test
    fun negativeAndOverflowingTimingIsBounded() {
        val negative = derbisDiscordActivityPayload(presence().copy(positionMs = -1L, durationMs = -1L), 100_000L)
        val overflow = derbisDiscordActivityPayload(presence().copy(positionMs = 0L, durationMs = Long.MAX_VALUE), Long.MAX_VALUE)

        assertEquals(100_000L, negative.startedAtEpochMs)
        assertEquals(0L, negative.endsAtEpochMs)
        assertEquals(0L, overflow.endsAtEpochMs)
    }

    @Test
    fun privateOrUntrustedUrlsNeverBecomeClickablePresenceLinks() {
        val payload = derbisDiscordActivityPayload(
            presence().copy(songUrl = "file:///private/song.mp3", artistUrl = "https://other.example/artist", artworkUrl = "content://cover"),
            1_700_000_000_000L
        )

        assertTrue(payload.songUrl.isEmpty())
        assertTrue(payload.artistUrl.isEmpty())
        assertTrue(payload.artworkUrl.isEmpty())
    }

    @Test
    fun localTrackIdentifiersAreNotPublishedAsPublicSongLinks() {
        val track = Track(
            id = "privateFile", title = "Local title", artist = "Local artist", album = "", durationMs = 100_000L,
            streamUrl = "content://audio/1", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "Local library",
            moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0,
            artistBrowseIds = listOf("private-artist")
        )
        val snapshot = derbisDiscordPresenceSnapshot(track, 0L, true)

        assertTrue(snapshot.songUrl.isEmpty())
        assertTrue(snapshot.artistUrl.isEmpty())
    }

    @Test
    fun resolvedYoutubeCounterpartSuppliesThePublicLinkWithoutReplacingTrackIdentity() {
        val track = Track(
            id = "1234567890123456789012", title = "Track", artist = "Artist", album = "", durationMs = 100_000L,
            streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "Spotify",
            moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0,
            counterpartVideoId = "dQw4w9WgXcQ"
        )
        val snapshot = derbisDiscordPresenceSnapshot(track, 0L, true)

        assertEquals(track.id, snapshot.trackId)
        assertEquals("https://derbis-web.vercel.app/t/dQw4w9WgXcQ", snapshot.songUrl)
    }

    @Test
    fun changedClickableLinksAreNotSuppressedByThePresenceSyncKey() {
        val first = presence()
        val second = first.copy(songUrl = "https://derbis-web.vercel.app/t/abcdefghijk")
        assertNotEquals(first.syncKey, second.syncKey)
    }

    @Test
    fun sdkTextAndHoverLabelsKeepTheExistingLengthLimit() {
        val payload = derbisDiscordActivityPayload(presence().copy(title = "x".repeat(250), artist = "y".repeat(250)), 1_700_000_000_000L)

        assertEquals(128, payload.title.length)
        assertEquals(128, payload.artist.length)
        assertEquals(128, payload.artworkText.length)
    }
}
