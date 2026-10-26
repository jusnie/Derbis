package com.luc4n3x.derbis.ui.i18n

import com.luc4n3x.derbis.domain.DerbisLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateReleaseLocalizationTest {
    @Test
    fun everySupportedLanguageShipsUpdateScreenCopy() {
        val catalogCodes = DerbisLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(41, catalogCodes.size)
        assertEquals(catalogCodes, updateReleaseLocalizationCodes())
        assertEquals(catalogCodes, update2512HighlightLocalizationCodes())

        catalogCodes.forEach { code ->
            val strings = DerbisStrings.forCode(code)
            val copy = strings.updateReleaseCopy()
            val highlights = strings.updateReleaseHighlights("2.5.12")
            assertTrue("releaseReady is blank for $code", copy.releaseReady.isNotBlank())
            assertTrue("releaseFrom is blank for $code", copy.releaseFrom.isNotBlank())
            assertTrue("releaseTo is blank for $code", copy.releaseTo.isNotBlank())
            assertTrue("releaseHighlights is blank for $code", copy.releaseHighlights.isNotBlank())
            assertTrue("releaseProtection is blank for $code", copy.releaseProtection.isNotBlank())
            assertTrue("releaseProtectionDetail is blank for $code", copy.releaseProtectionDetail.isNotBlank())
            assertEquals("unexpected highlight count for $code", 4, highlights.size)
            assertTrue("blank highlight for $code", highlights.all { it.isNotBlank() })
        }
    }

    @Test
    fun russianUpdateScreenUsesRussianCopy() {
        val strings = DerbisStrings.forCode("ru")
        val copy = strings.updateReleaseCopy()
        val highlights = strings.updateReleaseHighlights("v2.5.12")

        assertEquals("Обновление готово", copy.releaseReady)
        assertEquals("С версии", copy.releaseFrom)
        assertEquals("До версии", copy.releaseTo)
        assertEquals("Основные изменения", copy.releaseHighlights)
        assertEquals("Защищённое обновление", copy.releaseProtection)
        assertTrue(copy.releaseProtectionDetail.contains("Официальный APK"))
        assertTrue(copy.releaseProtectionDetail.contains("перед установкой"))
        assertTrue(highlights.first().contains("Синхронизированный"))
        assertFalse(highlights.any { it.contains("Synced lyrics", ignoreCase = true) })
    }

    @Test
    fun rtlAndTraditionalChineseUpdateCopyStayNative() {
        val arabic = DerbisStrings.forCode("ar").updateReleaseCopy()
        val persian = DerbisStrings.forCode("fa").updateReleaseCopy()
        val hebrew = DerbisStrings.forCode("he").updateReleaseCopy()
        val traditionalChinese = DerbisStrings.forCode("zh-Hant").updateReleaseCopy()

        assertEquals("التحديث جاهز", arabic.releaseReady)
        assertEquals("به‌روزرسانی آماده است", persian.releaseReady)
        assertEquals("העדכון מוכן", hebrew.releaseReady)
        assertEquals("更新已就緒", traditionalChinese.releaseReady)
    }

    @Test
    fun unknownReleaseFallsBackToSelectedLanguageCopy() {
        assertEquals(
            listOf(DerbisStrings.forCode("ru").updateDescription),
            DerbisStrings.forCode("ru").updateReleaseHighlights("9.9.9")
        )
        assertEquals(
            listOf(DerbisStrings.forCode("ja").updateDescription),
            DerbisStrings.forCode("ja").updateReleaseHighlights("9.9.9")
        )
    }
}
