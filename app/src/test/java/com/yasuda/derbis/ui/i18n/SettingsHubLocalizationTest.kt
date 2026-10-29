package com.yasuda.derbis.ui.i18n

import com.yasuda.derbis.domain.DerbisLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsHubLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteSettingsHubCopy() {
        val catalogCodes = DerbisLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, settingsHubLocalizationCodes())
        assertTrue(settingsHubIncompleteLocalizationCodes().toString(), settingsHubIncompleteLocalizationCodes().isEmpty())
    }

    @Test
    fun translatedLanguagesDoNotFallBackToEnglishGroupTitles() {
        val english = DerbisStrings.forCode("en").settingsHubCopy
        DerbisStrings.all().filter { it.code != "en" }.forEach { strings ->
            assertNotEquals(strings.code, english[SettingsHubText.StudiosHint], strings.settingsHubCopy[SettingsHubText.StudiosHint])
        }
    }
}
