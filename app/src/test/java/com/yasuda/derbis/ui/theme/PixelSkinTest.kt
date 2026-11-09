package com.yasuda.derbis.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.domain.DerbisSkin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelSkinTest {
    @After
    fun restoreDefault() {
        DerbisThemeController.apply(DerbisThemes.DERBIS_AURA)
    }

    /**
     * Pixel is a light skin, so the fixed-palette assertion is the same shape as it was for
     * Nocturne 78 but the expected values are inverted: the palette ignores the preset, the
     * artwork accent and the pure-black switch, and reports itself as light.
     */
    @Test
    fun fixedSkinIgnoresArtworkAccentPureBlackAndLightPalette() {
        DerbisThemeController.apply(
            presetId = DerbisThemes.MINIMAL_WHITE,
            coverAccentStart = 0xFFFF0000.toInt(), coverAccentEnd = 0xFF00FF00.toInt(),
            pureBlack = true, accentOverride = 0xFF0000FF.toInt(), skin = DerbisSkin.Pixel
        )
        assertEquals(PixelPalette, DerbisActivePalette)
        assertTrue(DerbisActivePalette.isLight)
        assertEquals(PixelPalette.ink, DerbisInk)
    }

    @Test
    fun returningToDefaultRestoresTheSelectedPaletteAndAccent() {
        val accent = 0xFF60A5FA.toInt()
        DerbisThemeController.apply(DerbisThemes.FROST, accentOverride = accent)
        val original = DerbisActivePalette
        DerbisThemeController.apply(DerbisThemes.FROST, accentOverride = accent, skin = DerbisSkin.Pixel)
        assertTrue(DerbisIsPixel)
        DerbisThemeController.apply(DerbisThemes.FROST, accentOverride = accent, skin = DerbisSkin.Default)
        assertEquals(original, DerbisActivePalette)
        assertFalse(DerbisIsPixel)
    }

    /**
     * A light skin puts dark text on light surfaces, so the ratio has to be taken the right way
     * up. The old Nocturne test divided foreground by surface and silently assumed the skin was
     * dark; that only ever produced a number above 1 on a dark palette.
     */
    @Test
    fun textAndInteractiveColorsMeetContrastOnEveryPixelSurface() {
        val p = PixelPalette
        val surfaces = listOf(p.black, p.ink, p.panel, p.panelSoft, PixelDesign.variant)
        surfaces.forEach { surface ->
            listOf(p.text, p.muted, p.cyan).forEach { foreground ->
                val contrast = contrastRatio(foreground, surface)
                assertTrue("Contrast $contrast for $foreground on $surface", contrast >= 4.5f)
            }
        }
        val scheme = derbisColorScheme(p)
        assertTrue(contrastRatio(scheme.primary, scheme.onPrimary) >= 4.5f)
        assertTrue(contrastRatio(scheme.onSurface, scheme.surface) >= 4.5f)
    }

    @Test
    fun thePixelSkinHasNoCorners() {
        assertEquals(0.dp, PixelDesign.artworkCorner)
        assertEquals(0.dp, PixelDesign.cardCorner)
        assertEquals(0.dp, PixelDesign.panelCorner)
        assertEquals(0.dp, PixelDesign.controlCorner)
    }

    @Test
    fun missingOrUnknownSkinUsesDefault() {
        assertEquals(DerbisSkin.Default, DerbisSkin.from(""))
        assertEquals(DerbisSkin.Default, DerbisSkin.from("future_skin"))
    }

    /** The retired skin must not become an invisible choice for anyone who had it saved. */
    @Test
    fun theRetiredNocturneSkinLandsOnPixel() {
        assertEquals(DerbisSkin.Pixel, DerbisSkin.from("Nocturne78"))
        assertEquals(DerbisSkin.Pixel, DerbisSkin.from("nocturne_78"))
        assertEquals(DerbisSkin.Pixel, DerbisSkin.from("Nocturne 78"))
        assertEquals(DerbisSkin.Pixel, DerbisSkin.from("pixel"))
    }

    @Test
    fun prismStillLandsOnLiquidGlass() {
        assertEquals(DerbisSkin.LiquidGlass, DerbisSkin.from("Prism"))
        assertEquals(DerbisSkin.LiquidGlass, DerbisSkin.from("prism"))
    }

    private fun contrastRatio(a: Color, b: Color): Float {
        val first = a.luminance()
        val second = b.luminance()
        val lighter = maxOf(first, second)
        val darker = minOf(first, second)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
