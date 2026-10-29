package com.yasuda.derbis.domain

import com.yasuda.derbis.ui.i18n.DerbisStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisContentLocalesTest {
    @Test
    fun everySupportedLanguageHasDistinctArtistSuggestions() {
        DerbisLanguageCatalog.languages.forEach { language ->
            val suggestions = DerbisContentLocales.artistSuggestions(language.code)
            assertTrue(suggestions.size >= 10)
            assertEquals(suggestions.size, suggestions.map(::artistIdentityKey).distinct().size)
            assertTrue(suggestions.all { it.isNotBlank() })
        }
    }

    @Test
    fun nonItalianLanguagesDoNotReuseItalianSuggestions() {
        val italian = DerbisContentLocales.artistSuggestions("it").map(::artistIdentityKey).toSet()
        DerbisLanguageCatalog.languages
            .filterNot { it.code == "it" }
            .forEach { language ->
                val localized = DerbisContentLocales.artistSuggestions(language.code).map(::artistIdentityKey).toSet()
                assertTrue(italian.intersect(localized).isEmpty())
            }
    }

    @Test
    fun everySupportedLanguageResolvesToAnExplicitChartRegion() {
        val regionIds = ChartsCatalog.regions.map { it.id }.toSet()
        DerbisLanguageCatalog.languages.forEach { language ->
            val locale = DerbisContentLocales.forLanguage(language.code)
            assertTrue("Missing chart region for ${language.code}: ${locale.chartRegionId}", locale.chartRegionId in regionIds)
            assertEquals(locale.chartRegionId, ChartsCatalog.defaultRegionForLanguage(language.code).id)
        }
    }

    @Test
    fun initialChartRegionFollowsUiLanguage() {
        assertEquals("it", ChartsCatalog.defaultRegionForLanguage("it").id)
        assertEquals("it", ChartsCatalog.defaultRegionForLanguage("it-IT").id)
        assertEquals("pt", ChartsCatalog.defaultRegionForLanguage("pt").id)
        assertEquals(DerbisContentLocales.forLanguage("ja").chartRegionId, ChartsCatalog.defaultRegionForLanguage("ja").id)
    }

    @Test
    fun animeJpopExploreZoneDoesNotReusePartyQuery() {
        listOf("en", "ja", "zh", "ko").forEach { code ->
            val strings = DerbisStrings.forCode(code)
            val locale = DerbisContentLocales.forLanguage(code)
            val zone = ExploreCatalog.getZones(strings).first { it.id == "anime-jpop" }
            assertFalse("Anime/J-pop should not reuse party query for $code", zone.query == locale.queryForTaste("party"))
            assertTrue("Anime/J-pop query should remain explicit for $code", zone.query.contains("J-POP", ignoreCase = true))
        }
    }

    @Test
    fun exploreMoodCatalogHasCompletePairsForEverySupportedLanguage() {
        DerbisLanguageCatalog.languages.forEach { language ->
            val strings = DerbisStrings.forCode(language.code)
            val moods = ExploreCatalog.getZones(strings)
                .filterNot { it.id == ExploreCatalog.NEW_RELEASES_ZONE_ID }

            assertEquals("Uneven mood grid for ${language.code}", 0, moods.size % 2)
            assertEquals("Duplicate mood for ${language.code}", moods.size, moods.map { it.id }.distinct().size)
            assertEquals(
                "Afrobeats label for ${language.code}",
                strings.exploreAfrobeats,
                moods.single { it.id == "afrobeats" }.label
            )
            assertTrue("Empty Afrobeats label for ${language.code}", strings.exploreAfrobeats.isNotBlank())
        }
    }

    @Test
    fun majorAsianLanguagesUseLocalizedDiscoveryData() {
        assertEquals("jp", DerbisContentLocales.forLanguage("ja-JP").chartRegionId)
        assertEquals("kr", DerbisContentLocales.forLanguage("ko-KR").chartRegionId)
        assertEquals("in", DerbisContentLocales.forLanguage("hi-IN").chartRegionId)
        assertEquals("id", DerbisContentLocales.forLanguage("in-ID").chartRegionId)
        assertEquals("vn", DerbisContentLocales.forLanguage("vi-VN").chartRegionId)
        assertEquals("th", DerbisContentLocales.forLanguage("th-TH").chartRegionId)
        assertEquals("ph", DerbisContentLocales.forLanguage("fil-PH").chartRegionId)
        assertEquals("ph", DerbisContentLocales.forLanguage("tl-PH").chartRegionId)
        assertEquals("il", DerbisContentLocales.forLanguage("he-IL").chartRegionId)
        assertEquals("il", DerbisContentLocales.forLanguage("iw-IL").chartRegionId)
        assertTrue(DerbisContentLocales.artistSuggestions("ja").contains("YOASOBI"))
        assertTrue(DerbisContentLocales.artistSuggestions("ko").contains("BTS"))
        assertTrue(DerbisContentLocales.artistSuggestions("vi").contains("Sơn Tùng M-TP"))
        assertTrue(DerbisContentLocales.artistSuggestions("fil").contains("Cup of Joe"))
        assertTrue(DerbisContentLocales.artistSuggestions("he").contains("נועה קירל"))
    }

    @Test
    fun artistSuggestionMatchingUsesNormalizedLanguageAndPrimaryArtist() {
        assertTrue(DerbisContentLocales.isArtistSuggestionForLanguage("Bad Bunny feat. Feid", "es-MX"))
        assertTrue(DerbisContentLocales.isArtistSuggestionForLanguage("周杰伦", "zh-CN"))
        assertTrue(DerbisContentLocales.isArtistSuggestionForLanguage("MiyaGi & Andy Panda", "ru-RU"))
        assertFalse(DerbisContentLocales.isArtistSuggestionForLanguage("Sfera Ebbasta", "fr-FR"))
    }
}
