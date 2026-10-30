package com.yasuda.derbis.ui.sharing

import com.yasuda.derbis.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackStoryModelTest {

    @Test
    fun formatsCalculateExpectedHeights() {
        assertEquals(1920, TrackStoryCard.heightFor(1080, TrackStoryFormat.STORY_9_16))
        assertEquals(960, TrackStoryCard.heightFor(540, TrackStoryFormat.STORY_9_16))

        assertEquals(1350, TrackStoryCard.heightFor(1080, TrackStoryFormat.CARD_4_5))
        assertEquals(675, TrackStoryCard.heightFor(540, TrackStoryFormat.CARD_4_5))

        assertEquals(1080, TrackStoryCard.heightFor(1080, TrackStoryFormat.SQUARE_1_1))
        assertEquals(540, TrackStoryCard.heightFor(540, TrackStoryFormat.SQUARE_1_1))
    }

    @Test
    fun snapshotCalculatesProgressAndFormattedTimes() {
        val track = track(
            id = "test-track-1",
            title = "Starboy",
            artist = "The Weeknd",
            durationMs = 230_000L
        )
        val snapshot = TrackStorySnapshot(
            track = track,
            positionMs = 115_000L,
            durationMs = 230_000L
        )

        assertEquals("1:55", snapshot.formattedPosition)
        assertEquals("3:50", snapshot.formattedDuration)
        assertEquals(0.5f, snapshot.progress, 0.001f)
    }

    @Test
    fun snapshotClampsProgress() {
        val track = track(id = "test-2", title = "Blinding Lights", artist = "The Weeknd")
        val overshot = TrackStorySnapshot(track = track, positionMs = 500_000L, durationMs = 200_000L)
        assertEquals(1.0f, overshot.progress, 0.001f)

        val negative = TrackStorySnapshot(track = track, positionMs = -50_000L, durationMs = 200_000L)
        assertEquals(0.0f, negative.progress, 0.001f)
    }

    @Test
    fun paletteFallbackPreservesColors() {
        val track = track(
            id = "test-3",
            title = "Midnight City",
            artist = "M83",
            accentStart = 0xFF123456.toInt(),
            accentEnd = 0xFF654321.toInt()
        )
        val palette = TrackStoryCard.resolvePalette(track, null)
        assertEquals(0xFF123456.toInt(), palette.dominant)
        assertEquals(0xFF654321.toInt(), palette.accent)
        assertEquals(0xFF08080C.toInt(), palette.darkBase)
    }

    @Test
    fun allFormatsAndThemesHaveLabels() {
        TrackStoryFormat.entries.forEach { format ->
            assertTrue(format.label.isNotBlank())
            assertTrue(format.ratioLabel.isNotBlank())
            assertTrue(format.widthPx > 0)
            assertTrue(format.heightPx > 0)
        }

        TrackStoryTheme.entries.forEach { theme ->
            assertTrue(theme.label.isNotBlank())
            assertTrue(theme.subtitle.isNotBlank())
        }
    }

    private fun track(
        id: String,
        title: String,
        artist: String,
        durationMs: Long = 0L,
        accentStart: Int = 0,
        accentEnd: Int = 0
    ): Track = Track(
        id = id, title = title, artist = artist, album = "", durationMs = durationMs,
        streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "test",
        moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0,
        accentStart = accentStart, accentEnd = accentEnd
    )

}
