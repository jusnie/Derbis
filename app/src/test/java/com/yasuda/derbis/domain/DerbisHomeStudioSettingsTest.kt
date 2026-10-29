package com.yasuda.derbis.domain

import com.yasuda.derbis.ui.i18n.homeStudioLocalizationCodes
import com.yasuda.derbis.ui.reorderStep
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisHomeStudioSettingsTest {
    @Test
    fun untouchedOriginalKeepsTheClassicHomeRenderer() {
        assertTrue(DerbisHomeStudioSettings().usesClassicOriginalHome())
        assertTrue(DerbisHomeStudioSettings.decode(null).usesClassicOriginalHome())
        val resetAfterEdit = DerbisHomeStudioSettings()
            .updateLayout(HomeStyle.Original) { it.withVisibility(HomeSectionId.PersonalOrbit, false) }
            .resetStyle(HomeStyle.Original)
        assertTrue(resetAfterEdit.usesClassicOriginalHome())
    }

    @Test
    fun customizedOriginalUsesTheRendererThatHonoursHiddenAndReorderedSections() {
        val hidden = DerbisHomeStudioSettings()
            .updateLayout(HomeStyle.Original) { it.withVisibility(HomeSectionId.PersonalOrbit, false) }
        val reordered = DerbisHomeStudioSettings()
            .updateLayout(HomeStyle.Original) { it.moved(2, 0) }

        assertFalse(hidden.usesClassicOriginalHome())
        assertFalse(reordered.usesClassicOriginalHome())
        assertFalse(DerbisHomeStudioSettings(style = HomeStyle.ExpressiveMax).usesClassicOriginalHome())
    }

    @Test
    fun freshInstallDefaultsToOriginalWithCurrentHomeOrder() {
        val settings = DerbisHomeStudioSettings.decode(null)
        val layout = settings.activeLayout()

        assertEquals(HomeStyle.Original, settings.style)
        assertEquals(HomeSectionId.entries.toList(), layout.order)
        assertTrue(layout.hidden.isEmpty())
        assertTrue(layout.layouts.isEmpty())
        assertEquals(HomeDensity.Balanced, layout.density)
        assertEquals(HomeMotion.Normal, layout.motion)
        assertTrue(layout.dynamicPalette)
    }

    @Test
    fun corruptPayloadFallsBackToOriginal() {
        assertEquals(DerbisHomeStudioSettings(), DerbisHomeStudioSettings.decode("{not json"))
        assertEquals(HomeStyle.Original, DerbisHomeStudioSettings.decode("""{"style":"retired_style"}""").style)
    }

    @Test
    fun everyStyleRoundTripsThroughStorage() {
        HomeStyle.entries.forEach { style ->
            val settings = DerbisHomeStudioSettings()
                .withStyle(style)
                .updateLayout(style) { layout ->
                    layout.moved(0, 3)
                        .withVisibility(HomeSectionId.Charts, visible = false)
                        .withLayout(HomeSectionId.QuickPicks, HomeSectionLayout.Grid)
                        .withFeatured(HomeSectionId.AlbumsForYou, featuredOn = true)
                        .copy(density = HomeDensity.Spacious, dynamicPalette = false)
                }

            val restored = DerbisHomeStudioSettings.decode(settings.encode())

            assertEquals(settings, restored)
            assertEquals(style, restored.style)
        }
    }

    @Test
    fun eachStyleKeepsItsOwnCustomization() {
        val settings = DerbisHomeStudioSettings()
            .updateLayout(HomeStyle.Flow) { it.withVisibility(HomeSectionId.Resonance, visible = false) }
            .updateLayout(HomeStyle.Editorial) { it.withLayout(HomeSectionId.QuickPicks, HomeSectionLayout.Grid) }
            .withStyle(HomeStyle.Editorial)
            .withStyle(HomeStyle.Flow)

        assertTrue(settings.layoutFor(HomeStyle.Flow).isHidden(HomeSectionId.Resonance))
        assertEquals(HomeSectionLayout.Grid, settings.layoutFor(HomeStyle.Editorial).layoutFor(HomeSectionId.QuickPicks))
        assertFalse(settings.layoutFor(HomeStyle.Editorial).isHidden(HomeSectionId.Resonance))
    }

    @Test
    fun newSectionsFromFutureReleasesAreMergedNearTheirDefaultNeighbour() {
        val stored = JSONObject()
            .put("style", "original")
            .put(
                "layouts",
                JSONObject().put(
                    "original",
                    JSONObject()
                        .put("order", JSONArray(listOf("charts", "genre_chips", "unknown_section", "quick_picks")))
                        .put("hidden", JSONArray(listOf("unknown_section")))
                )
            )
            .toString()

        val layout = DerbisHomeStudioSettings.decode(stored).activeLayout()

        assertEquals(HomeSectionId.entries.size, layout.order.size)
        assertEquals(HomeSectionId.entries.toSet(), layout.order.toSet())
        assertEquals(HomeSectionId.Charts, layout.order.first())
        assertEquals(HomeSectionId.SpeedDial, layout.order[layout.order.indexOf(HomeSectionId.GenreChips) + 1])
    }

    @Test
    fun fixedSectionsIgnoreLayoutOverrides() {
        val layout = DerbisHomeStudioSettings.defaultLayout(HomeStyle.Original)
            .withLayout(HomeSectionId.PersonalOrbit, HomeSectionLayout.Grid)

        assertEquals(HomeSectionLayout.Auto, layout.layoutFor(HomeSectionId.PersonalOrbit))
    }

    @Test
    fun featuredSectionsAreCappedAndRenderedFirst() {
        val layout = DerbisHomeStudioSettings.defaultLayout(HomeStyle.Original)
            .withFeatured(HomeSectionId.Charts, featuredOn = true)
            .withFeatured(HomeSectionId.Resonance, featuredOn = true)
            .withFeatured(HomeSectionId.QuickPicks, featuredOn = true)

        assertEquals(listOf(HomeSectionId.Resonance, HomeSectionId.QuickPicks), layout.featured)
        assertEquals(listOf(HomeSectionId.Resonance, HomeSectionId.QuickPicks), layout.renderOrder().take(2))
    }

    @Test
    fun resettingRestoresDefaultsWithoutTouchingOtherStyles() {
        val customized = DerbisHomeStudioSettings()
            .updateLayout(HomeStyle.Minimal) { it.copy(density = HomeDensity.Spacious) }
            .updateLayout(HomeStyle.Immersive) { it.copy(motion = HomeMotion.Reduced) }
            .withStyle(HomeStyle.Minimal)

        val reset = customized.resetStyle(HomeStyle.Minimal)

        assertEquals(DerbisHomeStudioSettings.defaultLayout(HomeStyle.Minimal), reset.layoutFor(HomeStyle.Minimal))
        assertEquals(HomeMotion.Reduced, reset.layoutFor(HomeStyle.Immersive).motion)
        assertEquals(HomeStyle.Minimal, reset.style)
    }

    @Test
    fun defaultLayoutsAreNotPersisted() {
        val untouched = DerbisHomeStudioSettings().updateLayout(HomeStyle.Flow) { it }

        assertTrue(untouched.styleLayouts.isEmpty())
        assertFalse(JSONObject(untouched.encode()).getJSONObject("layouts").has("flow"))
    }

    @Test
    fun originalNeverUsesExpressiveMotion() {
        val layout = DerbisHomeStudioSettings()
            .updateLayout(HomeStyle.Original) { it.copy(motion = HomeMotion.Expressive) }
            .layoutFor(HomeStyle.Original)

        assertEquals(HomeMotion.Normal, layout.motion)
    }

    @Test
    fun everyStyleDefaultCoversEverySectionOnce() {
        HomeStyle.entries.forEach { style ->
            val order = DerbisHomeStudioSettings.defaultLayout(style).order
            assertEquals(style.name, HomeSectionId.entries.size, order.size)
            assertEquals(style.name, HomeSectionId.entries.toSet(), order.toSet())
        }
    }

    @Test
    fun dragReorderSwapsOnlyPastHalfOfTheNeighbour() {
        val order = listOf(HomeSectionId.QuickPicks, HomeSectionId.Charts, HomeSectionId.Resonance)
        val heights = order.associateWith { 100 }

        assertEquals(null, reorderStep(order, HomeSectionId.Charts, 40f, heights))
        assertEquals(
            listOf(HomeSectionId.QuickPicks, HomeSectionId.Resonance, HomeSectionId.Charts) to -40f,
            reorderStep(order, HomeSectionId.Charts, 60f, heights)
        )
        assertEquals(
            listOf(HomeSectionId.Charts, HomeSectionId.QuickPicks, HomeSectionId.Resonance) to 40f,
            reorderStep(order, HomeSectionId.Charts, -60f, heights)
        )
    }

    @Test
    fun homeStudioCopyCoversEveryLanguage() {
        assertEquals(
            DerbisLanguageCatalog.languages.map { it.code }.toSet(),
            homeStudioLocalizationCodes()
        )
    }
}
