package com.luc4n3x.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisLyricsStudioSettingsTest {

    @Test
    fun defaultSettingsMatchesDerbisOriginal() {
        val settings = DerbisLyricsStudioSettings()
        assertEquals(LyricsStylePreset.DERBIS_ORIGINAL, settings.stylePreset)
        assertEquals(LyricsBackgroundMode.ORIGINAL, settings.backgroundMode)
        assertEquals(LyricsAlignment.START, settings.alignment)
        assertEquals(LyricsTextSize.STANDARD, settings.textSize)
        assertEquals(LyricsLineSpacing.STANDARD, settings.lineSpacing)
        assertEquals(LyricsFontWeightPreset.BOLD, settings.fontWeight)
        assertEquals(LyricsActiveColorMode.WHITE, settings.activeColorMode)
        assertEquals(LyricsGlowEffect.OFF, settings.glowEffect)
        assertTrue(settings.progressiveKaraokeEnabled)
        assertTrue(settings.blurEnabled)
        assertFalse(settings.appleMusic13LyricsEnabled)
    }

    @Test
    fun applyPresetConfiguresDistinctStyles() {
        val defaultSettings = DerbisLyricsStudioSettings()

        val original = defaultSettings.applyPreset(LyricsStylePreset.DERBIS_ORIGINAL)
        assertEquals(LyricsStylePreset.DERBIS_ORIGINAL, original.stylePreset)
        assertEquals(LyricsBackgroundMode.ORIGINAL, original.backgroundMode)
        assertEquals(LyricsAlignment.START, original.alignment)

        val expressive = defaultSettings.applyPreset(LyricsStylePreset.EXPRESSIVE_KARAOKE)
        assertEquals(LyricsStylePreset.EXPRESSIVE_KARAOKE, expressive.stylePreset)
        assertEquals(LyricsTextSize.LARGE, expressive.textSize)
        assertEquals(LyricsFontWeightPreset.EXTRA_BOLD, expressive.fontWeight)
        assertEquals(LyricsGlowEffect.SUBTLE, expressive.glowEffect)
        assertTrue(expressive.progressiveKaraokeEnabled)

        val minimal = defaultSettings.applyPreset(LyricsStylePreset.MINIMAL)
        assertEquals(LyricsStylePreset.MINIMAL, minimal.stylePreset)
        assertEquals(LyricsBackgroundMode.AMOLED, minimal.backgroundMode)
        assertEquals(LyricsAlignment.CENTER, minimal.alignment)
        assertEquals(LyricsTextSize.STANDARD, minimal.textSize)
        assertEquals(LyricsFontWeightPreset.SEMI_BOLD, minimal.fontWeight)
        assertEquals(LyricsGlowEffect.OFF, minimal.glowEffect)
        assertFalse(minimal.progressiveKaraokeEnabled)
        assertFalse(minimal.blurEnabled)

        val focus = defaultSettings.applyPreset(LyricsStylePreset.FOCUS)
        assertEquals(LyricsStylePreset.FOCUS, focus.stylePreset)
        assertEquals(LyricsTextSize.LARGE, focus.textSize)
        assertEquals(LyricsFontWeightPreset.BLACK, focus.fontWeight)
        assertEquals(LyricsGlowEffect.SUBTLE, focus.glowEffect)
        assertEquals(0.22f, focus.inactiveOpacity, 0.01f)
        assertEquals(0.82f, focus.inactiveScale, 0.01f)

        val cinematic = defaultSettings.applyPreset(LyricsStylePreset.CINEMATIC)
        assertEquals(LyricsStylePreset.CINEMATIC, cinematic.stylePreset)
        assertEquals(LyricsBackgroundMode.CINEMATIC, cinematic.backgroundMode)
        assertEquals(LyricsTextSize.LARGE, cinematic.textSize)
        assertEquals(LyricsGlowEffect.VIBRANT, cinematic.glowEffect)
        assertTrue(cinematic.blurEnabled)
    }

    @Test
    fun jsonRoundTripPreservesAllFields() {
        val original = DerbisLyricsStudioSettings(
            stylePreset = LyricsStylePreset.CINEMATIC,
            fontPreset = DerbisFontPreset.Outfit,
            textSize = LyricsTextSize.LARGE,
            lineSpacing = LyricsLineSpacing.RELAXED,
            fontWeight = LyricsFontWeightPreset.EXTRA_BOLD,
            alignment = LyricsAlignment.CENTER,
            inactiveScale = 0.85f,
            activeColorMode = LyricsActiveColorMode.CUSTOM,
            customAccentColor = 0xFFE91E63.toInt(),
            inactiveOpacity = 0.35f,
            glowEffect = LyricsGlowEffect.VIBRANT,
            backgroundMode = LyricsBackgroundMode.CINEMATIC,
            progressiveKaraokeEnabled = true,
            blurEnabled = true
        )

        val json = original.encode()
        val decoded = DerbisLyricsStudioSettings.decode(json)

        assertEquals(original, decoded)
    }

    @Test
    fun appleMusic13RendererRoundTripsAndRemainsExclusiveOfPresetChanges() {
        val enabled = DerbisLyricsStudioSettings(
            stylePreset = LyricsStylePreset.CINEMATIC,
            appleMusic13LyricsEnabled = true
        )

        assertTrue(DerbisLyricsStudioSettings.decode(enabled.encode()).appleMusic13LyricsEnabled)
        assertTrue(enabled.applyPreset(LyricsStylePreset.MINIMAL).appleMusic13LyricsEnabled)
        val rendered = enabled.settingsForActiveLyricsRenderer()
        assertEquals(LyricsBackgroundMode.BLURRED_ARTWORK, rendered.backgroundMode)
        assertEquals(LyricsAlignment.CENTER, rendered.alignment)
        assertFalse(enabled.copy(appleMusic13LyricsEnabled = false).appleMusic13LyricsEnabled)
    }

    @Test
    fun decodingLegacyLyricsSettingsDefaultsAppleMusic13RendererOff() {
        val legacy = DerbisLyricsStudioSettings.decode("""{"stylePreset":"CINEMATIC"}""")
        assertFalse(legacy.appleMusic13LyricsEnabled)
    }

    @Test
    fun decodingInvalidOrEmptyJsonReturnsDefaultSafely() {
        val emptyDecoded = DerbisLyricsStudioSettings.decode("")
        assertEquals(DerbisLyricsStudioSettings(), emptyDecoded)

        val nullDecoded = DerbisLyricsStudioSettings.decode(null)
        assertEquals(DerbisLyricsStudioSettings(), nullDecoded)

        val malformedDecoded = DerbisLyricsStudioSettings.decode("{invalid_json: 123}")
        assertEquals(DerbisLyricsStudioSettings(), malformedDecoded)

        val partialJson = """{"stylePreset":"MINIMAL","alignment":"CENTER"}"""
        val partialDecoded = DerbisLyricsStudioSettings.decode(partialJson)
        assertEquals(LyricsStylePreset.MINIMAL, partialDecoded.stylePreset)
        assertEquals(LyricsAlignment.CENTER, partialDecoded.alignment)
        assertEquals(DerbisLyricsStudioSettings().textSize, partialDecoded.textSize)
    }

    @Test
    fun normalizationClampsExtremeValues() {
        val outOfBounds = DerbisLyricsStudioSettings(
            inactiveScale = 5.0f,
            inactiveOpacity = -0.5f
        ).normalized()

        assertTrue(outOfBounds.inactiveScale in 0.70f..1.0f)
        assertTrue(outOfBounds.inactiveOpacity in 0.15f..0.90f)
    }

    @Test
    fun demoLyricsHasValidLinesAndWordTiming() {
        val lines = DemoLyrics
        assertTrue("Demo lyrics should not be empty", lines.isNotEmpty())
        lines.forEach { line ->
            assertTrue("Line text should not be empty", line.text.isNotBlank())
            assertTrue("Line startMs should be non-negative", line.startMs >= 0)
        }
        val wordTimedLines = lines.filter { !it.words.isNullOrEmpty() }
        assertTrue("Demo lyrics should include word-timed lines", wordTimedLines.isNotEmpty())
    }
}
