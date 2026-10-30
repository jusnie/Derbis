package com.yasuda.derbis.ui

import com.yasuda.derbis.domain.ExploreCatalog
import com.yasuda.derbis.domain.ExploreCategory
import com.yasuda.derbis.domain.ExploreZone

internal enum class ExploreCategoryPresentation {
    Atmospheric,
    Structured,
    Mixed
}

internal data class ExploreCategorySection(
    val key: String,
    val providerTitle: String,
    val presentation: ExploreCategoryPresentation,
    val categories: List<ExploreCategory>
)

internal enum class ExploreAnchor {
    Moods
}

internal enum class ExploreShortcut(val anchor: ExploreAnchor?, val zoneId: String?) {
    NewReleases(null, ExploreCatalog.NEW_RELEASES_ZONE_ID),
    Moods(ExploreAnchor.Moods, null)
}

internal sealed interface ExploreRow {
    val key: String

    data object Shortcuts : ExploreRow {
        override val key: String = "explore-shortcuts"
    }

    data class Header(val anchor: ExploreAnchor) : ExploreRow {
        override val key: String = "explore-header-${anchor.name}"
    }

    data object MixTools : ExploreRow {
        override val key: String = "explore-mix-tools"
    }

    data class MoodRail(val zones: List<ExploreZone>) : ExploreRow {
        override val key: String = "explore-mood-rail"
    }
}

internal fun buildExploreRows(zones: List<ExploreZone>): List<ExploreRow> {
    val rows = mutableListOf<ExploreRow>()
    rows += ExploreRow.Shortcuts

    val distinctZones = zones.distinctBy { it.id }
    if (distinctZones.isNotEmpty()) {
        rows += ExploreRow.Header(ExploreAnchor.Moods)
        rows += ExploreRow.MoodRail(distinctZones)
    }

    rows += ExploreRow.MixTools
    return rows
}

internal fun exploreAnchorIndex(rows: List<ExploreRow>, anchor: ExploreAnchor): Int =
    rows.indexOfFirst { row -> row is ExploreRow.Header && row.anchor == anchor }

internal fun exploreAvailableAnchors(rows: List<ExploreRow>): Set<ExploreAnchor> =
    rows.asSequence()
        .filterIsInstance<ExploreRow.Header>()
        .map { row -> row.anchor }
        .toSet()

internal fun buildExploreCategorySections(categories: List<ExploreCategory>): List<ExploreCategorySection> {
    val validCategories = LinkedHashMap<String, ExploreCategory>()
    categories.forEach { category ->
        val title = category.title.trim()
        if (title.isNotBlank() && category.params.isNotBlank()) {
            validCategories.putIfAbsent(
                category.params,
                category.copy(title = title, section = category.section.trim())
            )
        }
    }

    return validCategories.values
        .groupBy { category -> category.sectionIndex to category.section }
        .map { (sectionIdentity, sectionCategories) ->
            val (sectionIndex, providerTitle) = sectionIdentity
            ExploreCategorySection(
                key = "provider-section-$sectionIndex-${sectionCategories.first().params}",
                providerTitle = providerTitle,
                presentation = when {
                    sectionIndex < 0 -> ExploreCategoryPresentation.Mixed
                    sectionIndex == 0 -> ExploreCategoryPresentation.Atmospheric
                    else -> ExploreCategoryPresentation.Structured
                },
                categories = sectionCategories
            )
        }
}

internal fun exploreFallbackGenres(zones: List<ExploreZone>): List<ExploreZone> = zones
    .asSequence()
    .filterNot { zone ->
        zone.id == ExploreCatalog.NEW_RELEASES_ZONE_ID || zone.id == ExploreCatalog.LOCAL_WAVE_ZONE_ID
    }
    .distinctBy { zone -> zone.id }
    .toList()
