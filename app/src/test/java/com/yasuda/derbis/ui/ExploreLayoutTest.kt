package com.yasuda.derbis.ui

import com.yasuda.derbis.domain.ExploreCatalog
import com.yasuda.derbis.domain.ExploreCategory
import com.yasuda.derbis.domain.ExploreZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreLayoutTest {
    @Test
    fun discoveryContentPrecedesAdvancedMixControls() {
        val rows = buildExploreRows(zones(4))

        assertTrue(rows.indexOfFirst { it is ExploreRow.MoodRail } < rows.indexOf(ExploreRow.MixTools))
        assertEquals(ExploreRow.MixTools, rows.last())
    }

    @Test
    fun mixControlsRemainAvailableWhenDiscoveryIsEmpty() {
        val rows = buildExploreRows(emptyList())

        assertEquals(listOf(ExploreRow.Shortcuts, ExploreRow.MixTools), rows)
        assertEquals(1, rows.count { it == ExploreRow.MixTools })
    }

    @Test
    fun shortcutAnchorsResolveToTheirHeaders() {
        val rows = buildExploreRows(zones(3))
        val anchored = ExploreShortcut.entries.filter { it.anchor != null }

        assertEquals(anchored.mapNotNull { it.anchor }.toSet(), exploreAvailableAnchors(rows))
        anchored.forEach { shortcut ->
            val anchor = requireNotNull(shortcut.anchor)
            val index = exploreAnchorIndex(rows, anchor)
            assertTrue("Missing header for ${shortcut.name}", index >= 0)
            assertEquals(ExploreRow.Header(anchor), rows[index])
        }
    }

    @Test
    fun exploreRowsExposeNeitherRemovedFreshCurrentsNorSamples() {
        for (count in 0..5) {
            val rows = buildExploreRows(zones(count))
            assertTrue(rows.none { it.key.contains("fresh", ignoreCase = true) })
            assertTrue(rows.none { it.key.contains("samples", ignoreCase = true) })
        }
        assertEquals(setOf(ExploreShortcut.NewReleases, ExploreShortcut.Moods), ExploreShortcut.entries.toSet())
        assertEquals(listOf(ExploreAnchor.Moods), ExploreAnchor.entries)
    }

    @Test
    fun newReleasesShortcutKeepsItsDestinationWithoutARemovedCarouselAnchor() {
        assertEquals(ExploreCatalog.NEW_RELEASES_ZONE_ID, ExploreShortcut.NewReleases.zoneId)
        assertNull(ExploreShortcut.NewReleases.anchor)
        assertNull(ExploreShortcut.Moods.zoneId)
    }

    @Test
    fun noCarouselLoadingOrEmptyPlaceholderCanReturn() {
        val empty = buildExploreRows(emptyList())
        val loaded = buildExploreRows(zones(2))

        assertEquals(listOf("explore-shortcuts", "explore-mix-tools"), empty.map { it.key })
        assertEquals(
            listOf("explore-shortcuts", "explore-header-Moods", "explore-mood-rail", "explore-mix-tools"),
            loaded.map { it.key }
        )
    }

    @Test
    fun availableAnchorsMatchOnlyRenderedSections() {
        assertTrue(exploreAvailableAnchors(buildExploreRows(emptyList())).isEmpty())
        assertEquals(setOf(ExploreAnchor.Moods), exploreAvailableAnchors(buildExploreRows(zones(2))))
    }

    @Test
    fun moodRailKeepsTheCompleteCatalogInSourceOrder() {
        val rows = buildExploreRows(zones(5))
        val rail = rows.filterIsInstance<ExploreRow.MoodRail>().single()

        assertEquals(listOf("zone-0", "zone-1", "zone-2", "zone-3", "zone-4"), rail.zones.map { it.id })
        assertEquals("explore-mood-rail", rail.key)
    }

    @Test
    fun duplicatedZonesNeverProduceDuplicatedLazyKeys() {
        val rows = buildExploreRows(zones(3) + zones(3))
        val keys = rows.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
        assertEquals(3, rows.filterIsInstance<ExploreRow.MoodRail>().single().zones.size)
    }

    @Test
    fun missingMoodCatalogStillRendersTheRestOfTheScreen() {
        val rows = buildExploreRows(emptyList())

        assertEquals(-1, exploreAnchorIndex(rows, ExploreAnchor.Moods))
        assertTrue(rows.contains(ExploreRow.Shortcuts))
        assertTrue(rows.contains(ExploreRow.MixTools))
        assertTrue(rows.none { it is ExploreRow.Header || it is ExploreRow.MoodRail })
    }

    @Test
    fun providerSectionsUseStructuralOrderAndStableParamsIdentity() {
        val categories = listOf(
            ExploreCategory("Focus", "mood-focus", "Moods & moments", 0),
            ExploreCategory("Focus duplicate", "mood-focus", "Moods & moments", 0),
            ExploreCategory("Pop", "genre-pop", "Genres", 1),
            ExploreCategory("", "malformed", "Genres", 1),
            ExploreCategory("Rock", "", "Genres", 1)
        )
        val sections = buildExploreCategorySections(categories)

        assertEquals(2, sections.size)
        assertEquals(ExploreCategoryPresentation.Atmospheric, sections[0].presentation)
        assertEquals(ExploreCategoryPresentation.Structured, sections[1].presentation)
        assertEquals(listOf("mood-focus", "genre-pop"), sections.flatMap { it.categories }.map { it.params })
    }

    @Test
    fun ungroupedProviderCategoriesRemainUsableWithoutTitleGuessing() {
        val sections = buildExploreCategorySections(
            listOf(ExploreCategory("未知", "opaque-params", section = "", sectionIndex = -1))
        )

        assertEquals(ExploreCategoryPresentation.Mixed, sections.single().presentation)
        assertEquals("opaque-params", sections.single().categories.single().params)
    }

    @Test
    fun fallbackGenresExcludeNonGenreEditorialShortcuts() {
        val template = zones(1).single()
        val ids = exploreFallbackGenres(
            listOf(
                template.copy(id = ExploreCatalog.NEW_RELEASES_ZONE_ID),
                template.copy(id = ExploreCatalog.LOCAL_WAVE_ZONE_ID),
                template.copy(id = "rap-drill")
            )
        ).map { it.id }

        assertFalse(ExploreCatalog.NEW_RELEASES_ZONE_ID in ids)
        assertFalse(ExploreCatalog.LOCAL_WAVE_ZONE_ID in ids)
        assertTrue("rap-drill" in ids)
    }

    private fun zones(count: Int): List<ExploreZone> = List(count) { index ->
        ExploreZone(
            id = "zone-$index",
            label = "Zone $index",
            emoji = "🎧",
            query = "query $index",
            accentStart = 0xFF00E5FF.toInt(),
            accentEnd = 0xFF2979FF.toInt()
        )
    }
}
