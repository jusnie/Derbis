package com.yasuda.derbis.domain

import com.yasuda.derbis.ui.formatMiniHexColor
import com.yasuda.derbis.ui.i18n.miniPlayerStudioCompleteLocalizationCodes
import com.yasuda.derbis.ui.i18n.miniPlayerStudioLocalizationCodes
import com.yasuda.derbis.ui.parseMiniHexColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisMiniPlayerStudioSettingsTest {

    @Test
    fun defaultSettingsKeepOriginalStyle() {
        val settings = DerbisMiniPlayerStudioSettings()
        assertEquals(MiniPlayerStyle.Original, settings.style)
        assertTrue(settings.tunings.isEmpty())
        assertTrue(DerbisMiniPlayerStudioSettings.spec(MiniPlayerStyle.Original).options.isEmpty())
    }

    @Test
    fun missingOrCorruptStorageFallsBackToOriginal() {
        assertEquals(DerbisMiniPlayerStudioSettings(), DerbisMiniPlayerStudioSettings.decode(null))
        assertEquals(DerbisMiniPlayerStudioSettings(), DerbisMiniPlayerStudioSettings.decode(""))
        assertEquals(DerbisMiniPlayerStudioSettings(), DerbisMiniPlayerStudioSettings.decode("{not json"))
    }

    @Test
    fun unknownStyleAndValuesFallBackSafely() {
        val decoded = DerbisMiniPlayerStudioSettings.decode(
            """{"version":9,"style":"holographic","tunings":{"floating":{"height":999,"progress":"spiral","buttons":"neon"},"mystery":{}}}"""
        )
        assertEquals(MiniPlayerStyle.Original, decoded.style)
        val floating = decoded.tuningFor(MiniPlayerStyle.Floating)
        val spec = DerbisMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating)
        assertEquals(spec.height.last, floating.heightDp)
        assertEquals(spec.defaults.progress, floating.progress)
        assertEquals(spec.defaults.buttons, floating.buttons)
    }

    @Test
    fun everyStyleDefaultIsAlreadyNormalized() {
        MiniPlayerStyle.entries.forEach { style ->
            val defaults = DerbisMiniPlayerStudioSettings.spec(style).defaults
            assertEquals(style.name, defaults, defaults.normalized(style))
        }
    }

    @Test
    fun customizationRoundTripsThroughStorage() {
        val settings = DerbisMiniPlayerStudioSettings()
            .withStyle(MiniPlayerStyle.LiquidGlass)
            .updateTuning(MiniPlayerStyle.LiquidGlass) {
                it.copy(opacity = 70, cornerDp = 20, colorMode = MiniColorMode.Custom, customColor = 0xFF112233.toInt())
            }
        val decoded = DerbisMiniPlayerStudioSettings.decode(settings.encode())
        assertEquals(settings, decoded)
        assertEquals(70, decoded.activeTuning().opacity)
    }

    @Test
    fun originalIsNeverCustomized() {
        val settings = DerbisMiniPlayerStudioSettings().updateTuning(MiniPlayerStyle.Original) { it.copy(heightDp = 80) }
        assertTrue(settings.tunings.isEmpty())
    }

    @Test
    fun tuningMatchingDefaultsIsDropped() {
        val style = MiniPlayerStyle.Floating
        val settings = DerbisMiniPlayerStudioSettings()
            .updateTuning(style) { it.copy(insetDp = 30) }
            .updateTuning(style) { it.copy(insetDp = DerbisMiniPlayerStudioSettings.spec(style).defaults.insetDp) }
        assertTrue(settings.tunings.isEmpty())
    }

    @Test
    fun valuesAreClampedToSafeLimits() {
        val style = MiniPlayerStyle.UltraCompact
        val tuning = DerbisMiniPlayerStudioSettings.spec(style).defaults
            .copy(heightDp = 10, artworkDp = 200, cornerDp = 200, controlScale = 999)
            .normalized(style)
        assertEquals(48, tuning.heightDp)
        assertTrue(tuning.artworkDp <= tuning.heightDp - 12)
        assertTrue(tuning.cornerDp <= tuning.heightDp / 2)
        assertEquals(MiniPlayerTuning.CONTROL_SCALE_RANGE.last, tuning.controlScale)
    }

    @Test
    fun restoringDefaultClearsEverything() {
        val customized = DerbisMiniPlayerStudioSettings()
            .withStyle(MiniPlayerStyle.ArtworkFocus)
            .updateTuning(MiniPlayerStyle.ArtworkFocus) { it.copy(heightDp = 84) }
        assertEquals(DerbisMiniPlayerStudioSettings(), DerbisMiniPlayerStudioSettings.decode(DerbisMiniPlayerStudioSettings().encode()))
        assertTrue(customized != DerbisMiniPlayerStudioSettings())
    }

    @Test
    fun hexColorsParseStrictly() {
        assertEquals(0xFF7C8CFF.toInt(), parseMiniHexColor("#7c8cff"))
        assertEquals(0xFF000000.toInt(), parseMiniHexColor("000000"))
        assertNull(parseMiniHexColor("12345"))
        assertNull(parseMiniHexColor("GGGGGG"))
        assertEquals("7C8CFF", formatMiniHexColor(0xFF7C8CFF.toInt()))
    }

    @Test
    fun miniPlayerStudioCopyCoversEveryLanguage() {
        val expected = DerbisLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(expected, miniPlayerStudioLocalizationCodes())
        assertEquals(expected, miniPlayerStudioCompleteLocalizationCodes())
    }
}
