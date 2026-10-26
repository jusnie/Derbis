package com.luc4n3x.derbis.ui.i18n

import com.luc4n3x.derbis.domain.DerbisLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisHomeVideoStringsTest {

    @Test
    fun `every supported language has its own video shelf title`() {
        val english = DerbisStrings.forCode("en").homeVideoShelfTitle
        DerbisLanguageCatalog.languages.forEach { language ->
            val title = DerbisStrings.forCode(language.code).homeVideoShelfTitle
            assertTrue(language.code, title.isNotBlank())
            if (language.code != "en") assertTrue(language.code, title != english || language.code in sharedWithEnglish)
        }
    }

    @Test
    fun `english and italian describe the shelf plainly`() {
        assertEquals("Music videos", DerbisStrings.forCode("en").homeVideoShelfTitle)
        assertEquals("Videoclip musicali", DerbisStrings.forCode("it").homeVideoShelfTitle)
    }

    private val sharedWithEnglish = emptySet<String>()
}
