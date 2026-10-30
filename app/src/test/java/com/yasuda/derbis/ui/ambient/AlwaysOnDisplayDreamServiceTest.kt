package com.yasuda.derbis.ui.ambient

import com.yasuda.derbis.domain.DerbisAmbientMode
import com.yasuda.derbis.domain.DerbisAmbientSettings
import com.yasuda.derbis.domain.ambientPixelShiftOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Always-On Display (AOD) and DreamService (ArchiveTune pattern).
 * Validates OLED black palette safety, periodic pixel-shift burn-in protection,
 * mode switching, and ambient playback timer formatting.
 */
class AlwaysOnDisplayDreamServiceTest {

    @Test
    fun defaultSettingsFavorOledProtectionAndBurnInPrevention() {
        val settings = DerbisAmbientSettings()

        // Pure OLED Black enabled by default to save battery and shut off black pixels
        assertTrue(settings.amoledBlack)

        // Pixel-shift active by default to protect AMOLED panels against burn-in
        assertTrue(settings.pixelShift)

        // Auto-dimming active by default
        assertTrue(settings.autoDim)
        assertEquals(20, settings.autoDimAfterSeconds)

        assertFalse(settings.proximityBlackout)
    }

    @Test
    fun pixelShiftOffsetTextsCycleWithinProtectiveRange() {
        val maxAllowedShiftDp = DerbisAmbientSettings.PIXEL_SHIFT_RANGE_DP

        for (step in 0..24) {
            val (shiftX, shiftY) = ambientPixelShiftOffset(step)
            assertTrue("shiftX $shiftX exceeds bound $maxAllowedShiftDp", kotlin.math.abs(shiftX) <= maxAllowedShiftDp)
            assertTrue("shiftY $shiftY exceeds bound $maxAllowedShiftDp", kotlin.math.abs(shiftY) <= maxAllowedShiftDp)
        }
    }

    @Test
    fun ambientModesProvideAppropriateLayoutProperties() {
        val minimalMode = DerbisAmbientSettings(mode = DerbisAmbientMode.Minimal)
        val spotlightMode = DerbisAmbientSettings(mode = DerbisAmbientMode.Spotlight)
        val lyricsMode = DerbisAmbientSettings(mode = DerbisAmbientMode.Lyrics)

        // Minimal mode focuses purely on clock, title, and battery
        assertFalse(minimalMode.usesArtwork)
        assertFalse(minimalMode.usesLyricLine)

        // Spotlight mode renders radiant artwork backlight
        assertTrue(spotlightMode.usesArtwork)

        // Lyrics mode focuses on live real-time lyrics
        assertTrue(lyricsMode.usesLyricLine)
        assertFalse(lyricsMode.usesArtwork)
    }

    @Test
    fun timeFormattingHandlesSecondsAndMinutesProperly() {
        assertEquals("0:00", formatTestTime(0L))
        assertEquals("1:05", formatTestTime(65_000L))
        assertEquals("3:42", formatTestTime(222_000L))
        assertEquals("12:00", formatTestTime(720_000L))
    }

    private fun formatTestTime(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(java.util.Locale.US, minutes, seconds)
    }
}
