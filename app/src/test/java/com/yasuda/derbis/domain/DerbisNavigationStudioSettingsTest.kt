package com.yasuda.derbis.domain

import androidx.compose.ui.unit.dp
import com.yasuda.derbis.ui.i18n.navigationStudioCompleteLocalizationCodes
import com.yasuda.derbis.ui.i18n.navigationStudioLocalizationCodes
import com.yasuda.derbis.ui.navDockExtraClearance
import com.yasuda.derbis.ui.navReorderStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisNavigationStudioSettingsTest {

    @Test
    fun defaultsKeepTheOriginalBar() {
        val settings = DerbisNavigationStudioSettings()
        assertEquals(NavStyle.Original, settings.style)
        assertTrue(settings.tunings.isEmpty())
        assertEquals(NavDestinations.defaultOrder, settings.activeTuning().visibleOrder())
    }

    @Test
    fun originalCannotBeCustomized() {
        val settings = DerbisNavigationStudioSettings().updateTuning(NavStyle.Original) { it.copy(heightDp = 60) }
        assertTrue(settings.tunings.isEmpty())
    }

    @Test
    fun corruptOrUnknownStorageFallsBack() {
        assertEquals(DerbisNavigationStudioSettings(), DerbisNavigationStudioSettings.decode("{oops"))
        val decoded = DerbisNavigationStudioSettings.decode(
            """{"style":"rainbow","tunings":{"minimal":{"order":["library","bogus","home"],"hidden":["home","explore"],"labels":"shout","height":999}}}"""
        )
        assertEquals(NavStyle.Original, decoded.style)
        val minimal = decoded.tuningFor(NavStyle.Minimal)
        assertEquals(listOf(DerbisTab.Library, DerbisTab.Home, DerbisTab.Search, DerbisTab.Explore), minimal.order)
        assertEquals(setOf(DerbisTab.Explore), minimal.hidden)
        assertEquals(DerbisNavigationStudioSettings.spec(NavStyle.Minimal).defaults.labels, minimal.labels)
        assertEquals(DerbisNavigationStudioSettings.spec(NavStyle.Minimal).height.last, minimal.heightDp)
    }

    @Test
    fun essentialDestinationsCannotBeHidden() {
        val tuning = NavTuning()
            .withVisibility(DerbisTab.Home, false)
            .withVisibility(DerbisTab.Search, false)
            .withVisibility(DerbisTab.Library, false)
        assertEquals(setOf(DerbisTab.Library), tuning.hidden)
        assertEquals(listOf(DerbisTab.Home, DerbisTab.Search, DerbisTab.Explore), tuning.visibleOrder())
    }

    @Test
    fun customizationRoundTripsAndDefaultsAreDropped() {
        val style = NavStyle.FloatingDock
        val settings = DerbisNavigationStudioSettings()
            .withStyle(style)
            .updateTuning(style) { it.moved(0, 3).copy(indicator = NavIndicator.Line) }
        assertEquals(settings, DerbisNavigationStudioSettings.decode(settings.encode()))
        val restored = settings.updateTuning(style) { DerbisNavigationStudioSettings.spec(style).defaults }
        assertTrue(restored.tunings.isEmpty())
    }

    @Test
    fun everyStyleDefaultIsNormalized() {
        NavStyle.entries.forEach { style ->
            val defaults = DerbisNavigationStudioSettings.spec(style).defaults
            assertEquals(style.name, defaults, defaults.normalized(style))
        }
    }

    @Test
    fun dragReorderMovesPastHalfARow() {
        val order = NavDestinations.defaultOrder
        val heights = order.associateWith { 100 }
        assertEquals(
            listOf(DerbisTab.Search, DerbisTab.Home, DerbisTab.Explore, DerbisTab.Library) to -40f,
            navReorderStep(order, DerbisTab.Home, 60f, heights)
        )
        assertEquals(null, navReorderStep(order, DerbisTab.Home, 40f, heights))
    }

    @Test
    fun legacyExpressiveFlagMigratesToMotion() {
        val decoded = DerbisNavigationStudioSettings.decode(
            """{"version":1,"style":"floating_dock","tunings":{"floating_dock":{"expressive":false,"height":70},"super_expressive":{"expressive":true,"height":84}}}"""
        )
        assertEquals(NavStyle.FloatingDock, decoded.style)
        assertEquals(NavMotion.Minimal, decoded.tuningFor(NavStyle.FloatingDock).motion)
        assertEquals(70, decoded.tuningFor(NavStyle.FloatingDock).heightDp)
        assertEquals(NavMotion.Expressive, decoded.tuningFor(NavStyle.SuperExpressive).motion)
    }

    @Test
    fun incompatibleChoicesFallBackToStyleDefaults() {
        val minimal = NavTuning(indicator = NavIndicator.Floating, cornerDp = 99, opacity = 5, spacingDp = -3)
            .normalized(NavStyle.Minimal)
        assertEquals(NavIndicator.Dot, minimal.indicator)
        assertEquals(DerbisNavigationStudioSettings.spec(NavStyle.Minimal).corner.last, minimal.cornerDp)
        assertEquals(NavTuning.OPACITY_RANGE.first, minimal.opacity)
        assertEquals(0, minimal.spacingDp)
        val glass = NavTuning(background = NavBackground.Solid).normalized(NavStyle.LiquidGlass)
        assertEquals(NavBackground.Translucent, glass.background)
    }

    @Test
    fun richCustomizationRoundTrips() {
        val style = NavStyle.LiquidGlass
        val settings = DerbisNavigationStudioSettings()
            .withStyle(style)
            .updateTuning(style) {
                it.copy(
                    labels = NavLabelMode.Compact,
                    indicator = NavIndicator.Circle,
                    motion = NavMotion.Off,
                    colorSource = NavColorSource.Custom,
                    accent = 0x00FF8A65,
                    surfaceTone = NavSurfaceTone.Amoled,
                    indicatorTone = NavIndicatorTone.Vivid,
                    opacity = 60,
                    inactiveAlpha = 70,
                    cornerDp = 28,
                    spacingDp = 8,
                    paddingDp = 12,
                    indicatorScale = 115,
                    haptics = false
                )
            }
        assertEquals(0xFFFF8A65.toInt(), settings.activeTuning().accent)
        assertEquals(settings, DerbisNavigationStudioSettings.decode(settings.encode()))
    }

    @Test
    fun onlyStyledBarsReserveExtraContentSpace() {
        assertEquals(0.dp, navDockExtraClearance(NavStyle.Original, NavTuning(heightDp = 120)))
        val tall = DerbisNavigationStudioSettings.spec(NavStyle.SuperExpressive).defaults.copy(heightDp = 88)
        assertEquals(12.dp, navDockExtraClearance(NavStyle.SuperExpressive, tall))
        val floating = DerbisNavigationStudioSettings.spec(NavStyle.FloatingDock).defaults
        assertEquals(0.dp, navDockExtraClearance(NavStyle.FloatingDock, floating))
    }

    @Test
    fun navigationStudioCopyCoversEveryLanguage() {
        val expected = DerbisLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(expected, navigationStudioLocalizationCodes())
        assertEquals(expected, navigationStudioCompleteLocalizationCodes())
    }

    @Test
    fun expandingLabelsAreTheDefaultOnlyForCustomizableStyles() {
        val expanding = listOf(
            NavStyle.SuperExpressive,
            NavStyle.FloatingDock,
            NavStyle.SplitDock,
            NavStyle.ConnectedDock,
            NavStyle.LiquidGlass,
            NavStyle.Minimal,
            NavStyle.ExpressiveCompact
        )
        expanding.forEach { style ->
            assertEquals(style.name, NavLabelMode.Expanding, DerbisNavigationStudioSettings().tuningFor(style).labels)
        }
        assertEquals(NavLabelMode.Always, DerbisNavigationStudioSettings().tuningFor(NavStyle.YtMusic).labels)
        assertEquals(NavLabelMode.Always, DerbisNavigationStudioSettings().tuningFor(NavStyle.Original).labels)
        assertEquals(NavColorSource.Theme, DerbisNavigationStudioSettings().tuningFor(NavStyle.FloatingDock).colorSource)
    }

    @Test
    fun explicitlyStoredLabelModesSurviveTheNewDefault() {
        val decoded = DerbisNavigationStudioSettings.decode(
            """{"version":2,"style":"floating_dock","tunings":{"floating_dock":{"labels":"active","height":70},"minimal":{"labels":"never","height":64},"liquid_glass":{"height":70}}}"""
        )
        assertEquals(NavLabelMode.ActiveOnly, decoded.tuningFor(NavStyle.FloatingDock).labels)
        assertEquals(NavLabelMode.Never, decoded.tuningFor(NavStyle.Minimal).labels)
        assertEquals(NavLabelMode.Expanding, decoded.tuningFor(NavStyle.LiquidGlass).labels)
        assertEquals(decoded, DerbisNavigationStudioSettings.decode(decoded.encode()))
    }

    @Test
    fun artworkColorSourceRoundTrips() {
        val style = NavStyle.SplitDock
        val settings = DerbisNavigationStudioSettings()
            .withStyle(style)
            .updateTuning(style) { it.copy(colorSource = NavColorSource.Artwork, labels = NavLabelMode.Always) }
        val restored = DerbisNavigationStudioSettings.decode(settings.encode())
        assertEquals(NavColorSource.Artwork, restored.activeTuning().colorSource)
        assertEquals(NavLabelMode.Always, restored.activeTuning().labels)
        assertEquals(settings, restored)
    }
}
