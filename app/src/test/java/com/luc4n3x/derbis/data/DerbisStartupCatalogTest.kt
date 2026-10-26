package com.luc4n3x.derbis.data

import com.luc4n3x.derbis.domain.HomeSection
import com.luc4n3x.derbis.domain.DerbisContentLocales
import com.luc4n3x.derbis.domain.DerbisLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisStartupCatalogTest {
    @Test
    fun homeSectionsDoNotExposeLegacyEnergyShelfInAnyLanguage() {
        DerbisLanguageCatalog.languages.forEach { language ->
            val locale = DerbisContentLocales.forLanguage(language.code)
            val sections = DerbisStartupCatalog.homeSections(language.code)

            assertFalse(
                "Legacy energy shelf still visible for ${language.code}",
                sections.any { section ->
                    section.title.trim().equals(locale.energySectionTitle.trim(), ignoreCase = true)
                }
            )
        }
    }

    @Test
    fun startupHomeKeepsFullLegacySeedPoolWithoutEnergyShelf() {
        DerbisLanguageCatalog.languages.forEach { language ->
            val tracks = DerbisStartupCatalog.homeSections(language.code)
                .flatMap { it.tracks }
                .distinctBy { it.title.lowercase() to it.artist.lowercase() }
            val titles = tracks.mapTo(mutableSetOf()) { it.title }

            assertEquals("Startup seed count changed for ${language.code}", 20, tracks.size)
            assertTrue("Midnight City missing for ${language.code}", "Midnight City" in titles)
            assertTrue("Starboy missing for ${language.code}", "Starboy" in titles)
            assertTrue("Believer missing for ${language.code}", "Believer" in titles)
        }
    }

    @Test
    fun repairHomeSectionsMovesPersistedLegacyEnergyTracksIntoQuickPicks() {
        DerbisLanguageCatalog.languages.forEach { language ->
            val locale = DerbisContentLocales.forLanguage(language.code)
            val startupTracks = DerbisStartupCatalog.homeSections(language.code).flatMap { it.tracks }
            val quickTrack = startupTracks.first { it.title == "Bohemian Rhapsody" }
            val energyTrack = startupTracks.first { it.title == "Midnight City" }

            val repaired = DerbisStartupCatalog.repairHomeSections(
                listOf(
                    HomeSection(locale.quickSectionTitle, listOf(quickTrack)),
                    HomeSection(locale.energySectionTitle, listOf(energyTrack))
                ),
                language.code
            )

            assertFalse(
                "Legacy energy shelf still restored for ${language.code}",
                repaired.any { section ->
                    section.title.trim().equals(locale.energySectionTitle.trim(), ignoreCase = true)
                }
            )
            val quick = repaired.single { section ->
                section.title.trim().equals(locale.quickSectionTitle.trim(), ignoreCase = true)
            }
            assertEquals(
                setOf("Bohemian Rhapsody", "Midnight City"),
                quick.tracks.mapTo(mutableSetOf()) { it.title }
            )
        }
    }

    @Test
    fun repairHomeSectionsMigratesLegacyEnergyShelfAfterLanguageChange() {
        val allEnergyTitles = DerbisLanguageCatalog.languages
            .map { language -> DerbisContentLocales.forLanguage(language.code).energySectionTitle.trim() }
            .filter { it.isNotEmpty() }

        DerbisLanguageCatalog.languages.forEach { savedLanguage ->
            val targetLanguageCode = if (savedLanguage.code == "en") "it" else "en"
            val savedLocale = DerbisContentLocales.forLanguage(savedLanguage.code)
            val startupTracks = DerbisStartupCatalog.homeSections(savedLanguage.code).flatMap { it.tracks }
            val quickTrack = startupTracks.first { it.title == "Bohemian Rhapsody" }
            val energyTrack = startupTracks.first { it.title == "Midnight City" }

            val repaired = DerbisStartupCatalog.repairHomeSections(
                listOf(
                    HomeSection(savedLocale.quickSectionTitle, listOf(quickTrack)),
                    HomeSection(savedLocale.energySectionTitle, listOf(energyTrack))
                ),
                targetLanguageCode
            )

            assertFalse(
                "Legacy ${savedLanguage.code} energy shelf survived after switching to $targetLanguageCode",
                repaired.any { section ->
                    allEnergyTitles.any { title -> section.title.trim().equals(title, ignoreCase = true) }
                }
            )
            val quick = repaired.single { section ->
                section.title.trim().equals(savedLocale.quickSectionTitle.trim(), ignoreCase = true)
            }
            assertEquals(
                setOf("Bohemian Rhapsody", "Midnight City"),
                quick.tracks.mapTo(mutableSetOf()) { it.title }
            )
        }
    }

    @Test
    fun chartFallbackStillContainsTwentyStartupSeeds() {
        val tracks = DerbisStartupCatalog.chartTracks("en")
        val titles = tracks.mapTo(mutableSetOf()) { it.title }

        assertEquals(20, tracks.size)
        assertTrue("Midnight City" in titles)
        assertTrue("Starboy" in titles)
        assertTrue("Believer" in titles)
    }

    @Test
    fun `startup seeds open with the official album cover instead of a video frame`() {
        val seed = DerbisStartupCatalog.homeSections("it").flatMap { it.tracks }
            .first { it.title == "Blinding Lights" && it.artist == "The Weeknd" }

        assertTrue(seed.thumbnailUrl.startsWith("https://i.scdn.co/image/"))
        assertTrue(seed.largeThumbnailUrl.startsWith("https://i.scdn.co/image/"))
        assertTrue(seed.videoUrl.contains("4NRXx6U8ABQ"))
    }
}
