package com.luc4n3x.derbis.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisThemeStudioTest {

    @Test
    fun curatedPresetsLeadTheCatalogWithoutLosingLegacyThemes() {
        val ids = DerbisThemes.presets.map { it.id }

        assertEquals(
            listOf(
                DerbisThemes.DERBIS_AURA,
                DerbisThemes.COVER_FLOW,
                DerbisThemes.AMOLED,
                DerbisThemes.FROST,
                DerbisThemes.OCEAN,
                DerbisThemes.EMBER
            ),
            ids.take(6)
        )
        listOf(
            DerbisThemes.COSMIC,
            DerbisThemes.NEON_CYAN,
            DerbisThemes.PURPLE_GLASS,
            DerbisThemes.MINIMAL_WHITE,
            DerbisThemes.MOOD_FLOW
        ).forEach { legacy ->
            assertTrue("Missing legacy preset $legacy", legacy in ids)
        }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun storedPresetIdsStillResolveAfterTheCatalogGrew() {
        DerbisThemes.presets.forEach { preset ->
            assertEquals(preset.id, DerbisThemes.byId(preset.id).id)
            assertEquals(preset.id, DerbisThemes.normalize(preset.id))
        }
        assertEquals(DerbisThemes.DERBIS_AURA, DerbisThemes.byId("apple_music").id)
        assertEquals(DerbisThemes.DERBIS_AURA, DerbisThemes.byId("").id)
        assertEquals(DerbisThemes.DERBIS_AURA, DerbisThemes.byId("removed_theme").id)
    }

    @Test
    fun newPresetsKeepReadableForegroundAndDistinctSurfaces() {
        listOf(DerbisThemes.frost, DerbisThemes.ocean, DerbisThemes.ember).forEach { palette ->
            assertNotEquals(palette.black, palette.panel)
            assertNotEquals(palette.panel, palette.panelSoft)
            assertTrue(
                "Insufficient text contrast in ${palette.id}",
                luminanceGap(palette.text, palette.black) > 0.4f
            )
            assertTrue(
                "Muted text too close to the surface in ${palette.id}",
                luminanceGap(palette.muted, palette.panel) > 0.1f
            )
        }
        assertTrue(DerbisThemes.frost.isLight)
        assertFalse(DerbisThemes.ocean.isLight)
        assertFalse(DerbisThemes.ember.isLight)
    }

    @Test
    fun accentOverrideOnlyRetintsAccentRoles() {
        val base = DerbisThemes.ocean
        val accent = Color(0xFFFF375F)
        val tinted = DerbisThemeController.withAccent(base, accent)

        assertEquals(base.black, tinted.black)
        assertEquals(base.panel, tinted.panel)
        assertEquals(base.text, tinted.text)
        assertEquals(base.muted, tinted.muted)
        assertEquals(accent, tinted.blue)
        assertEquals(accent, tinted.pink)
        assertNotEquals(base.cyan, tinted.cyan)
    }

    @Test
    fun accentOverrideStaysReadableOnLightAndDarkPresets() {
        val accent = Color(0xFF64D2FF)
        val onDark = DerbisThemeController.withAccent(DerbisThemes.ocean, accent)
        val onLight = DerbisThemeController.withAccent(DerbisThemes.frost, accent)

        assertTrue(luminance(onDark.cyan) > luminance(accent))
        assertTrue(luminance(onLight.cyan) < luminance(accent))
    }

    @Test
    fun pureBlackOnlyDarkensDarkPresets() {
        val dark = DerbisThemeController.asPureBlack(DerbisThemes.ember)
        assertEquals(Color.Black, dark.black)
        assertEquals(Color.Black, dark.ink)
        assertEquals(DerbisThemes.ember.text, dark.text)

        val light = DerbisThemeController.asPureBlack(DerbisThemes.frost)
        assertEquals(DerbisThemes.frost, light)
    }

    @Test
    fun studioPreviewMirrorsTheResolvedPalette() {
        val plain = themeStudioPaletteFor(DerbisThemes.OCEAN, accent = 0, pureBlack = false)
        assertEquals(DerbisThemes.ocean.black, plain.background)
        assertEquals(DerbisThemes.ocean.panel, plain.surface)
        assertEquals(DerbisThemes.ocean.cyan, plain.accent)
        assertEquals(DerbisThemes.ocean.black, plain.onAccent)

        val accentValue = 0xFFBF5AF2.toInt()
        val accented = themeStudioPaletteFor(DerbisThemes.OCEAN, accentValue, pureBlack = false)
        assertNotEquals(plain.accent, accented.accent)
        assertEquals(plain.background, accented.background)

        val blackened = themeStudioPaletteFor(DerbisThemes.OCEAN, accent = 0, pureBlack = true)
        assertEquals(Color.Black, blackened.background)

        val light = themeStudioPaletteFor(DerbisThemes.FROST, accent = 0, pureBlack = true)
        assertEquals(DerbisThemes.frost.black, light.background)
        assertEquals(Color.White, light.onAccent)
    }

    @Test
    fun accentPaletteIsBoundedAndOpaque() {
        assertEquals(DerbisThemeAccents.size, DerbisThemeAccents.toSet().size)
        assertTrue(DerbisThemeAccents.size <= 12)
        DerbisThemeAccents.forEach { value ->
            assertEquals(1f, Color(value).alpha, 0.0001f)
            assertTrue(value != 0)
        }
    }

    private fun luminance(color: Color): Float =
        0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

    private fun luminanceGap(first: Color, second: Color): Float =
        kotlin.math.abs(luminance(first) - luminance(second))
}
