package com.yasuda.derbis.ui.theme

import com.yasuda.derbis.domain.*
import com.yasuda.derbis.ui.playerContrastRatio
import com.yasuda.derbis.ui.player.playerSheetColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class PrismThemeTest {
    @After fun restore() { DerbisThemeController.apply(DerbisThemes.DERBIS_AURA) }

    @Test fun legacyPrismPreferenceOpensTheLiquidGlassSkin() {
        assertEquals(DerbisSkin.LiquidGlass, DerbisSkin.from("Prism"))
        assertEquals(DerbisSkin.LiquidGlass, DerbisSkin.from("LiquidGlass"))
        DerbisThemeController.apply(DerbisThemes.COSMIC, skin = DerbisSkin.LiquidGlass)
        assertTrue(DerbisIsLiquidGlass)
    }

    @Test fun appearancesHaveReadableContent() {
        listOf(PrismAppearance.Night, PrismAppearance.Light).forEach { appearance ->
            val p = prismPalette(PrismSettings(appearance = appearance))
            listOf(p.black, p.ink, p.panel, p.panelSoft).forEach { surface ->
                assertTrue("Text $appearance", playerContrastRatio(p.text, surface) >= 4.5f)
                assertTrue("Secondary $appearance", playerContrastRatio(p.muted, surface) >= 4.5f)
                assertTrue("Accent $appearance", playerContrastRatio(p.cyan, surface) >= 3f)
            }
            val scheme = derbisColorScheme(p)
            assertTrue(playerContrastRatio(scheme.primary, scheme.onPrimary) >= 4.5f)
        }
    }

    @Test fun liquidGlassUsesADeepNeutralBaseAndCoolSpectralAccents() {
        val night = prismPalette()
        assertEquals("Liquid Glass", night.label)
        assertEquals(Color(0xFF080A0F), night.black)
        listOf(PrismAppearance.Night, PrismAppearance.Light).forEach { appearance ->
            val p = prismPalette(PrismSettings(appearance = appearance))
            listOf(p.cyan, p.blue, p.violet, p.pink, p.orange).forEach { accent ->
                val hue = Hct.fromInt(accent.toArgb()).hue
                assertTrue("Accent hue $hue $appearance", hue in 165.0..285.0)
            }
            assertEquals(p, prismPalette(PrismSettings(appearance = appearance)))
        }
    }

    @Test fun torqueBandAndAccentStayReadableForAnyCover() {
        val covers = listOf(0xFFFFFF00, 0xFF0000FF, 0xFF00FF00, 0xFF222222, 0xFFFFFFFF, 0xFF8B5A2B, 0xFFFF0000)
        covers.forEach { cover ->
            val p = torquePalette(TorqueSettings(TorqueAccent.Cover), cover.toInt())
            assertTrue("Band $cover", playerContrastRatio(Color.White, p.blue) >= 3f)
            listOf(p.black, p.ink, p.panel, p.panelSoft).forEach { surface ->
                assertTrue("Accent $cover", playerContrastRatio(p.cyan, surface) >= 3f)
                assertTrue("Text $cover", playerContrastRatio(p.text, surface) >= 4.5f)
                assertTrue("Muted $cover", playerContrastRatio(p.muted, surface) >= 4.5f)
            }
        }
    }

    @Test fun torqueFallsBackToRedAndRedIgnoresCover() {
        assertEquals(Color(TorqueRed), torquePalette().blue)
        assertEquals(torquePalette(TorqueSettings(TorqueAccent.Red), 0xFF00FF00.toInt()), torquePalette(TorqueSettings(TorqueAccent.Red)))
        DerbisThemeController.apply(DerbisThemes.FROST, skin = DerbisSkin.Torque)
        assertTrue(DerbisIsTorque)
        assertTrue(DerbisIsPrism)
        assertTrue(DerbisSkinManagesLayout)
    }

    @Test fun ivoryDefaultsToLightAndKeepsInkReadableInBothAppearances() {
        assertTrue(ivoryPalette().isLight)
        assertFalse(ivoryPalette(IvorySettings(IvoryAppearance.Dark)).isLight)
        assertTrue(ivoryPalette(IvorySettings(IvoryAppearance.Automatic), systemDark = false).isLight)
        assertFalse(ivoryPalette(IvorySettings(IvoryAppearance.Automatic), systemDark = true).isLight)
        IvoryAppearance.entries.forEach { appearance ->
            val p = ivoryPalette(IvorySettings(appearance), systemDark = true)
            assertTrue(playerContrastRatio(p.text, p.black) >= 7f)
            assertTrue(playerContrastRatio(p.muted, p.black) >= 4.5f)
            assertTrue(playerContrastRatio(p.black, p.blue) >= 7f)
        }
        DerbisThemeController.apply(DerbisThemes.FROST, skin = DerbisSkin.Ivory)
        assertTrue(DerbisIsIvory)
        assertTrue(DerbisIsPrism)
        assertFalse(DerbisIsTorque)
        assertTrue(DerbisSkinManagesLayout)
    }

    @Test fun spotifyStyleIsDarkWithOneGreenAccentAndReadableSurfaces() {
        val p = spotifyPalette()
        assertFalse(p.isLight)
        assertEquals(SpotifyDesign.green, p.cyan)
        listOf(p.black, p.ink, p.panel, p.panelSoft).forEach { surface ->
            assertTrue(playerContrastRatio(p.text, surface) >= 7f)
            assertTrue(playerContrastRatio(p.muted, surface) >= 4.5f)
        }
        val scheme = derbisColorScheme(p)
        assertTrue(playerContrastRatio(scheme.primary, scheme.onPrimary) >= 7f)
        listOf(Color(0xFFFFE4E1), Color(0xFF101010), Color(0xFF1ED760), Color(0xFF3050F0)).forEach { cover ->
            listOf(SpotifyDesign.miniSurface(cover), SpotifyDesign.heroSurface(cover)).forEach { surface ->
                assertTrue(playerContrastRatio(Color.White, surface) >= 4.5f)
            }
            val lyrics = SpotifyDesign.lyricsSurface(cover)
            assertTrue(playerContrastRatio(Color.White, lyrics) >= 3f)
            assertTrue(playerContrastRatio(Color.Black, lyrics) >= 3f)
        }
        DerbisThemeController.apply(DerbisThemes.FROST, skin = DerbisSkin.SpotifyStyle)
        assertTrue(DerbisIsSpotify)
        assertTrue(DerbisIsPrism)
        assertFalse(DerbisIsIvory)
        assertTrue(DerbisSkinManagesLayout)
        assertEquals(DerbisSkin.SpotifyStyle, DerbisSkin.from(DerbisSkin.SpotifyStyle.name))
    }

    @Test fun playerSheetsKeepReadableContentInBothAppearances() {
        listOf(PrismAppearance.Light, PrismAppearance.Night).forEach { appearance ->
            val settings = PrismSettings(appearance = appearance)
            DerbisThemeController.apply(DerbisThemes.COSMIC, skin = DerbisSkin.LiquidGlass, prism = settings)
            val surfaces = prismPlayerSurfaces(DerbisActivePalette)
            assertTrue(playerContrastRatio(surfaces.content, playerSheetColor(surfaces)) >= 4.5f)
            assertTrue(playerContrastRatio(surfaces.contentMuted, playerSheetColor(surfaces)) >= 4.5f)
        }
    }

    @Test fun automaticFollowsSystemAndNightDoesNot() {
        assertTrue(prismPalette(PrismSettings(appearance = PrismAppearance.Automatic), systemDark = false).isLight)
        assertFalse(prismPalette(PrismSettings(appearance = PrismAppearance.Automatic), systemDark = true).isLight)
        assertFalse(prismPalette(systemDark = false).isLight)
    }

    @Test fun switchingSkinsRestoresDefaultAndNocturneExactly() {
        DerbisThemeController.apply(DerbisThemes.FROST, accentOverride = 0xFF3355AA.toInt())
        val saved = DerbisActivePalette
        DerbisThemeController.apply(DerbisThemes.FROST, skin = DerbisSkin.LiquidGlass)
        assertTrue(DerbisSkinManagesLayout)
        assertTrue(DerbisIsPrism)
        DerbisThemeController.apply(DerbisThemes.FROST, skin = DerbisSkin.Nocturne78)
        assertEquals(Nocturne78Palette, DerbisActivePalette)
        assertFalse(DerbisIsPrism)
        DerbisThemeController.apply(DerbisThemes.FROST, accentOverride = 0xFF3355AA.toInt())
        assertEquals(saved, DerbisActivePalette)
        assertFalse(DerbisSkinManagesLayout)
    }

    @Test fun projectedDockSettingsKeepSavedCustomizationIntact() {
        val mini = DerbisMiniPlayerStudioSettings()
        val nav = DerbisNavigationStudioSettings()
        assertEquals(mini.activeTuning().controls, prismMiniSettings(mini).activeTuning().controls)
        assertEquals(nav.activeTuning().order, prismNavSettings(nav).activeTuning().order)
        assertEquals(nav.activeTuning().hidden, prismNavSettings(nav).activeTuning().hidden)
        assertEquals(DerbisMiniPlayerStudioSettings(), mini)
        assertEquals(DerbisNavigationStudioSettings(), nav)
    }

    @Test fun liquidGlassSkinProjectsGlassMiniPlayerAndNavigation() {
        DerbisThemeController.apply(DerbisThemes.COSMIC, skin = DerbisSkin.LiquidGlass)
        val mini = prismMiniSettings(DerbisMiniPlayerStudioSettings())
        val nav = prismNavSettings(DerbisNavigationStudioSettings())

        assertEquals(MiniPlayerStyle.LiquidGlass, mini.style)
        assertEquals(NavStyle.LiquidGlass, nav.style)
        assertEquals(NavBackground.Translucent, nav.activeTuning().background)
    }

    @Test fun switchingSkinsUpdatesMiniPlayerCornerAfterDesignerNormalization() {
        val saved = DerbisMiniPlayerStudioSettings()
        DerbisThemeController.apply(DerbisThemes.COSMIC, skin = DerbisSkin.LiquidGlass)
        assertEquals(30, skinMiniPlayerCorner(prismMiniSettings(saved).activeTuning()).value.toInt())
        DerbisThemeController.apply(DerbisThemes.COSMIC, skin = DerbisSkin.Torque)
        assertEquals(2, skinMiniPlayerCorner(prismMiniSettings(saved).activeTuning()).value.toInt())
        DerbisThemeController.apply(DerbisThemes.COSMIC, skin = DerbisSkin.LiquidGlass)
        assertEquals(30, skinMiniPlayerCorner(prismMiniSettings(saved).activeTuning()).value.toInt())
        DerbisThemeController.apply(DerbisThemes.COSMIC, skin = DerbisSkin.Nocturne78)
        val nocturneTuning = nocturneMiniSettings(saved).activeTuning()
        assertEquals(10, skinMiniPlayerCorner(nocturneTuning).value.toInt())
        DerbisThemeController.apply(DerbisThemes.COSMIC, skin = DerbisSkin.Default)
        assertEquals(saved.activeTuning().cornerDp, skinMiniPlayerCorner(saved.activeTuning()).value.toInt())
        assertEquals(DerbisMiniPlayerStudioSettings(), saved)
    }
}
