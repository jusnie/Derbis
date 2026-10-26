package com.luc4n3x.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisAmbientSettingsTest {

    @Test
    fun defaultsKeepTheStandbyScreenReadableAndSafe() {
        val settings = DerbisAmbientSettings()

        assertEquals(DerbisAmbientMode.Artwork, settings.mode)
        assertTrue(settings.showClock)
        assertTrue(settings.showTitle)
        assertTrue(settings.showProgress)
        assertTrue(settings.amoledBlack)
        assertTrue(settings.pixelShift)
    }

    @Test
    fun normalizationClampsBrightnessAndAutoDimWindow() {
        val tooDark = DerbisAmbientSettings(brightness = -2f, autoDimAfterSeconds = 1).normalized()
        val tooBright = DerbisAmbientSettings(brightness = 9f, autoDimAfterSeconds = 9_999).normalized()

        assertEquals(DerbisAmbientSettings.MIN_BRIGHTNESS, tooDark.brightness, 0.0001f)
        assertEquals(DerbisAmbientSettings.MIN_AUTO_DIM_SECONDS, tooDark.autoDimAfterSeconds)
        assertEquals(DerbisAmbientSettings.MAX_BRIGHTNESS, tooBright.brightness, 0.0001f)
        assertEquals(DerbisAmbientSettings.MAX_AUTO_DIM_SECONDS, tooBright.autoDimAfterSeconds)
        assertEquals(
            DerbisAmbientSettings.MAX_AUTO_DIM_SECONDS * 1_000L,
            tooBright.autoDimAfterMs
        )
    }

    @Test
    fun unknownStoredModeFallsBackToArtwork() {
        assertEquals(DerbisAmbientMode.Artwork, DerbisAmbientMode.from(null))
        assertEquals(DerbisAmbientMode.Artwork, DerbisAmbientMode.from("not-a-mode"))
        assertEquals(DerbisAmbientMode.Lyrics, DerbisAmbientMode.from("lyrics"))
        assertEquals(DerbisAmbientMode.Spotlight, DerbisAmbientMode.from("  SPOTLIGHT "))
        assertEquals(DerbisAmbientMode.Minimal, DerbisAmbientMode.from("minimal"))
    }

    @Test
    fun storedModeIdsStayStableForPersistence() {
        assertEquals(
            listOf("minimal", "artwork", "spotlight", "lyrics"),
            DerbisAmbientMode.entries.map { it.id }
        )
    }

    @Test
    fun artworkAndLyricUsageFollowTheSelectedMode() {
        val minimal = DerbisAmbientSettings(mode = DerbisAmbientMode.Minimal)
        val artwork = DerbisAmbientSettings(mode = DerbisAmbientMode.Artwork)
        val spotlight = DerbisAmbientSettings(mode = DerbisAmbientMode.Spotlight)
        val lyrics = DerbisAmbientSettings(mode = DerbisAmbientMode.Lyrics)

        assertFalse(minimal.usesArtwork)
        assertTrue(artwork.usesArtwork)
        assertTrue(spotlight.usesArtwork)
        assertFalse(lyrics.usesArtwork)

        assertFalse(minimal.usesLyricLine)
        assertTrue(artwork.usesLyricLine)
        assertTrue(lyrics.usesLyricLine)
        assertFalse(lyrics.copy(showLyrics = false).usesLyricLine)
    }

    @Test
    fun pixelShiftWalksABoundedPatternAndReturnsToTheOrigin() {
        val offsets = (0 until 12).map { ambientPixelShiftOffset(it) }

        offsets.forEach { (x, y) ->
            assertTrue(kotlin.math.abs(x) <= DerbisAmbientSettings.PIXEL_SHIFT_RANGE_DP)
            assertTrue(kotlin.math.abs(y) <= DerbisAmbientSettings.PIXEL_SHIFT_RANGE_DP)
        }
        assertEquals(0f to 0f, ambientPixelShiftOffset(0))
        assertEquals(offsets[0], offsets[6])
        assertNotEquals(offsets[0], offsets[1])
        assertEquals(0f to 0f, ambientPixelShiftOffset(-5))
        assertEquals(0f to 0f, ambientPixelShiftOffset(3, rangeDp = 0f))
    }
}
