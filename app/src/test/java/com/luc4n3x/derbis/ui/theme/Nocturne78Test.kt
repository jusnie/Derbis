package com.luc4n3x.derbis.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.luc4n3x.derbis.domain.DerbisSkin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Nocturne78Test {
    @After
    fun restoreDefault() {
        DerbisThemeController.apply(DerbisThemes.DERBIS_AURA)
    }

    @Test
    fun fixedSkinIgnoresArtworkAccentPureBlackAndLightPalette() {
        DerbisThemeController.apply(
            presetId = DerbisThemes.MINIMAL_WHITE,
            coverAccentStart = 0xFFFF0000.toInt(), coverAccentEnd = 0xFF00FF00.toInt(),
            pureBlack = true, accentOverride = 0xFF0000FF.toInt(), skin = DerbisSkin.Nocturne78
        )
        assertEquals(Nocturne78Palette, DerbisActivePalette)
        assertFalse(DerbisActivePalette.isLight)
        assertEquals(Color.Black, DerbisInk)
    }

    @Test
    fun returningToDefaultRestoresTheSelectedPaletteAndAccent() {
        val accent = 0xFF60A5FA.toInt()
        DerbisThemeController.apply(DerbisThemes.FROST, accentOverride = accent)
        val original = DerbisActivePalette
        DerbisThemeController.apply(DerbisThemes.FROST, accentOverride = accent, skin = DerbisSkin.Nocturne78)
        assertTrue(DerbisIsNocturne)
        DerbisThemeController.apply(DerbisThemes.FROST, accentOverride = accent, skin = DerbisSkin.Default)
        assertEquals(original, DerbisActivePalette)
        assertFalse(DerbisIsNocturne)
    }

    @Test
    fun textAndInteractiveColorsMeetContrastOnAllNocturneSurfaces() {
        val p = Nocturne78Palette
        val surfaces = listOf(p.black, p.ink, p.panel, p.panelSoft, Nocturne78Design.variant)
        surfaces.forEach { surface ->
            listOf(p.text, p.muted, p.cyan).forEach { foreground ->
                val contrast = (foreground.luminance() + 0.05f) / (surface.luminance() + 0.05f)
                assertTrue("Contrast $contrast on $surface", contrast >= 4.5f)
            }
        }
        val scheme = derbisColorScheme(p)
        assertTrue((scheme.primary.luminance() + 0.05f) / (scheme.onPrimary.luminance() + 0.05f) >= 4.5f)
    }

    @Test
    fun missingOrUnknownSkinUsesDefault() {
        assertEquals(DerbisSkin.Default, DerbisSkin.from(""))
        assertEquals(DerbisSkin.Default, DerbisSkin.from("future_skin"))
    }
}
