package com.yasuda.derbis.ui.lyrics.spicy

import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.ui.lyrics.lyricsOffsetPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpicyLyricsPresentationTest {
    private val lines = listOf(
        LyricLine(1_000L, 2_000L, "First verse"),
        LyricLine(8_000L, 9_000L, "Next verse")
    )

    @Test
    fun focusUsesActualLineTimingAndClearsDuringInstrumentalGaps() {
        assertEquals(-1, spicyFocusedLyricIndex(999L, lines, true))
        assertEquals(0, spicyFocusedLyricIndex(1_000L, lines, true))
        assertEquals(0, spicyFocusedLyricIndex(2_320L, lines, true))
        assertEquals(-1, spicyFocusedLyricIndex(2_321L, lines, true))
        assertEquals(-1, spicyFocusedLyricIndex(7_999L, lines, true))
        assertEquals(1, spicyFocusedLyricIndex(8_000L, lines, true))
        assertEquals(-1, spicyFocusedLyricIndex(10_000L, lines, true))
    }

    @Test
    fun unsyncedEmptyMetadataAndInstrumentalLyricsDoNotInventAnActiveVerse() {
        assertEquals(-1, spicyFocusedLyricIndex(1_500L, lines, false))
        assertEquals(-1, spicyFocusedLyricIndex(1_500L, emptyList(), true))
        assertEquals(-1, spicyFocusedLyricIndex(1_500L, listOf(lines.first().copy(isMetadata = true)), true))
        assertEquals(-1, spicyFocusedLyricIndex(1_500L, listOf(lines.first().copy(isInstrumental = true)), true))
        assertEquals(-1, spicyFocusedLyricIndex(1_500L, listOf(lines.first().copy(text = "  ")), true))
    }

    @Test
    fun lyricCalibrationIsAppliedOnceAndLineSeekingIsItsInverse() {
        val offset = 600L
        val audioPosition = lines.first().startMs + offset
        assertEquals(1_000L, lyricsOffsetPosition(audioPosition, offset))
        assertEquals(0, spicyFocusedLyricIndex(lyricsOffsetPosition(audioPosition, offset), lines, true))
        assertEquals(-1, spicyFocusedLyricIndex(lyricsOffsetPosition(audioPosition - 1L, offset), lines, true))
    }

    @Test
    fun previewRangesHandleEmptyShortAndOutOfRangeSamples() {
        assertTrue(spicyPreviewIndices(0, -1).isEmpty())
        assertTrue(spicyPreviewIndices(-1, 0).isEmpty())
        assertEquals(listOf(0), spicyPreviewIndices(1, -1).toList())
        assertEquals(listOf(0, 1), spicyPreviewIndices(4, -1).toList())
        assertEquals(listOf(0, 1, 2), spicyPreviewIndices(4, 1).toList())
        assertEquals(listOf(2, 3), spicyPreviewIndices(4, 99).toList())
    }

    @Test
    fun timelineIsContinuousAndClampedWithoutQuantizingToTheDecorativeTicks() {
        assertEquals(0f, spicyTimelineProgress(50L, 0L), 0f)
        assertEquals(0f, spicyTimelineProgress(-1L, 10_000L), 0f)
        assertEquals(1f, spicyTimelineProgress(20_000L, 10_000L), 0f)
        assertEquals(0.4321f, spicyTimelineProgress(4_321L, 10_000L), 0.00001f)
        assertEquals(4_321L, spicySeekPosition(0.4321f, 10_000L))
        assertEquals(0L, spicySeekPosition(Float.NaN, 10_000L))
        assertEquals(0L, spicySeekPosition(Float.POSITIVE_INFINITY, 10_000L))
        assertEquals(0L, spicySeekPosition(1f, -1L))
        assertEquals(10_000L, spicySeekPosition(2f, 10_000L))
    }

    @Test
    fun timesAreStableAndNeverNegative() {
        assertEquals("0:00", formatSpicySeekMillis(-1L))
        assertEquals("1:01", formatSpicySeekMillis(61_999L))
        assertEquals("60:00", formatSpicySeekMillis(3_600_000L))
    }
}
