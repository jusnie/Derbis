package com.yasuda.derbis.ui.i18n

import com.yasuda.derbis.domain.DerbisLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchLocalizationTest {
    @Test
    fun everyCatalogLanguageShipsSettingsSearchCopy() {
        val codes = DerbisLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(codes, settingsSearchLocalizationCodes())
        DerbisStrings.all().forEach { strings ->
            assertTrue(strings.code, strings.settingsSearchPlaceholder.isNotBlank())
            assertTrue(strings.code, strings.settingsSearchEmpty.isNotBlank())
        }
    }

    @Test
    fun nonEnglishLanguagesDoNotFallBackToEnglish() {
        val english = DerbisStrings.forCode("en")
        DerbisStrings.all().filterNot { it.code == "en" }.forEach { strings ->
            assertNotEquals(strings.code, english.settingsSearchPlaceholder, strings.settingsSearchPlaceholder)
            assertNotEquals(strings.code, english.settingsSearchEmpty, strings.settingsSearchEmpty)
        }
    }

    @Test
    fun englishAndItalianCopyMatchTheProductWording() {
        assertEquals("Search settings", DerbisStrings.forCode("en").settingsSearchPlaceholder)
        assertEquals("No settings found", DerbisStrings.forCode("en").settingsSearchEmpty)
        assertEquals("Cerca nelle impostazioni", DerbisStrings.forCode("it").settingsSearchPlaceholder)
        assertEquals("Nessuna impostazione trovata", DerbisStrings.forCode("it").settingsSearchEmpty)
    }
}
