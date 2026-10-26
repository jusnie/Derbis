package com.luc4n3x.derbis.ui.i18n

import com.luc4n3x.derbis.domain.DerbisContentLocales
import com.luc4n3x.derbis.domain.DerbisLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class AudioAndRegionLocalizationRegressionTest {
    @Test
    fun truePeakLimiterHasLocalizedCopyForEverySupportedLanguage() {
        val labels = DerbisLanguageCatalog.languages.associate { language ->
            language.code to DerbisStrings.forCode(language.code).truePeakLimiter
        }

        assertEquals(DerbisLanguageCatalog.languages.size, labels.size)
        assertTrue(labels.values.all(String::isNotBlank))
        assertEquals("Limitador true-peak", labels.getValue("pt"))
        assertEquals("トゥルーピークリミッター", labels.getValue("ja"))
        assertEquals("محدد الذروة الحقيقية", labels.getValue("ar"))
        assertEquals("מגביל שיא אמיתי", labels.getValue("he"))
        assertTrue(labels.values.toSet().size >= 20)
    }

    @Test
    fun portugueseDiscoveryDefaultsToPortugal() {
        val locale = DerbisContentLocales.forLanguage("pt-PT")
        val quickSearches = DerbisContentLocales.quickSearches("pt-PT")
        val artists = DerbisContentLocales.artistSuggestions("pt-PT")

        assertEquals("pt", locale.chartRegionId)
        assertEquals("pt", locale.chartCountry)
        assertEquals("PT", locale.gl)
        assertTrue(locale.localSectionTitle.contains("Portugal", ignoreCase = true))
        assertTrue(locale.homeQueries.any { it.contains("Portugal", ignoreCase = true) })
        assertFalse(locale.homeQueries.any { it.contains("Brasil", ignoreCase = true) })
        assertTrue(quickSearches.any { it.contains("Portugal", ignoreCase = true) })
        assertFalse(quickSearches.any { it.contains("Brasil", ignoreCase = true) })
        assertTrue(artists.contains("Dillaz"))
    }

    @Test
    fun changingUiLanguagePreservesSelectedContentRegion() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/derbis/viewmodel/DerbisViewModel.kt"),
            Path.of("src/main/java/com/luc4n3x/derbis/viewmodel/DerbisViewModel.kt")
        ).firstOrNull(Files::exists) ?: error("DerbisViewModel.kt not found")
        val content = Files.readString(source)
        val start = content.indexOf("private fun applyLanguageContent")
        val end = content.indexOf("fun restartOnboarding", start)
        assertTrue(start >= 0 && end > start)
        val block = content.substring(start, end)

        assertTrue(block.contains("val selectedChartRegion = ChartsCatalog.regions"))
        assertTrue(block.contains("firstOrNull { it.id == _state.value.selectedChartId }"))
        assertTrue(block.contains("selectedChartId = selectedChartRegion.id"))
        assertFalse(block.contains("selectedChartId = defaultChartRegion.id"))
    }
}
