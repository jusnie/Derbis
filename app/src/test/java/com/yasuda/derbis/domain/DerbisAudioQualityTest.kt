package com.yasuda.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisAudioQualityTest {
    @Test
    fun defaultIsHigh() {
        assertEquals("High", DerbisAudioQuality.DEFAULT)
    }

    @Test
    fun explicitChoicesArePreservedRegardlessOfCaseAndSpacing() {
        assertEquals("High", DerbisAudioQuality.normalize("High"))
        assertEquals("Medium", DerbisAudioQuality.normalize(" medium "))
        assertEquals("Low", DerbisAudioQuality.normalize("LOW"))
        assertEquals("Lossless", DerbisAudioQuality.normalize("Lossless"))
        assertEquals("Lossless", DerbisAudioQuality.normalize("  LOSSLESS  "))
    }

    @Test
    fun legacyAutoReadsAsHighBecauseThatIsTheBranchItTook() {
        assertEquals("High", DerbisAudioQuality.normalize("Auto"))
        assertEquals("High", DerbisAudioQuality.normalize(" auto "))
    }

    @Test
    fun missingOrBlankValuesUseTheDefault() {
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize(null))
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize(""))
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize("   "))
    }

    @Test
    fun corruptOrUnknownValuesFallBackToTheDefault() {
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize("null"))
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize("320"))
        assertEquals(DerbisAudioQuality.DEFAULT, DerbisAudioQuality.normalize("Ultra"))
    }

    @Test
    fun onlyTheLosslessStepAsksForLossless() {
        assertTrue(DerbisAudioQuality.prefersLossless("Lossless"))
        assertTrue(DerbisAudioQuality.prefersLossless("lossless"))
        assertFalse(DerbisAudioQuality.prefersLossless("High"))
        assertFalse(DerbisAudioQuality.prefersLossless("Low"))
        assertFalse(DerbisAudioQuality.prefersLossless(null))
    }
}
