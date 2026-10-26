package com.luc4n3x.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DerbisAudioQualityTest {
    @Test
    fun defaultIsHigh() {
        assertEquals("High", DerbisAudioQuality.DEFAULT)
    }

    @Test
    fun explicitChoicesArePreservedRegardlessOfCaseAndSpacing() {
        assertEquals("High", DerbisAudioQuality.normalize("High"))
        assertEquals("Auto", DerbisAudioQuality.normalize("Auto"))
        assertEquals("Low", DerbisAudioQuality.normalize("Low"))
        assertEquals("Auto", DerbisAudioQuality.normalize(" auto "))
        assertEquals("Low", DerbisAudioQuality.normalize("LOW"))
        assertEquals("High", DerbisAudioQuality.normalize("high"))
    }

    @Test
    fun missingOrBlankValuesUseTheDefault() {
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize(null))
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize(""))
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize("   "))
    }

    @Test
    fun corruptOrUnknownValuesFallBackToTheDefault() {
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize("Medium"))
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize("null"))
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize("320"))
    }
}
