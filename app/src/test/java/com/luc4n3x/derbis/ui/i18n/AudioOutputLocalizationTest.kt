package com.luc4n3x.derbis.ui.i18n

import com.luc4n3x.derbis.domain.DerbisLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOutputLocalizationTest {
    @Test
    fun everySupportedLanguageShipsItsOwnAaudioOutputCopy() {
        val catalogCodes = DerbisLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, audioOutputLocalizationCodes())
        val english = DerbisStrings.forCode("en")
        DerbisStrings.all().forEach { strings ->
            assertTrue(strings.code, strings.audioOutputAaudio.contains("AAudio"))
            assertTrue(strings.code, strings.audioOutputAaudioSubtitle.isNotBlank())
            if (strings.code != "en") {
                assertNotEquals(strings.code, english.audioOutputAaudioSubtitle, strings.audioOutputAaudioSubtitle)
            }
        }
    }

    @Test
    fun subtitleNamesTheOutputPathWithoutLatencyOrBatteryClaims() {
        val latencyOrBatteryTerms = listOf("latency", "latenza", "battery", "batteria", "低延迟", "低遅延", "저지연")
        DerbisStrings.all().forEach { strings ->
            val subtitle = strings.audioOutputAaudioSubtitle
            assertTrue(strings.code, subtitle.contains("AAudio") && subtitle.contains("Oboe"))
            assertTrue(strings.code, latencyOrBatteryTerms.none { subtitle.contains(it, ignoreCase = true) })
        }
    }

    @Test
    fun italianCopyIsTranslated() {
        assertEquals("Uscita AAudio (Oboe)", DerbisStrings.forCode("it").audioOutputAaudio)
        assertEquals("Uscita audio nativa tramite AAudio e Oboe", DerbisStrings.forCode("it").audioOutputAaudioSubtitle)
    }
}
