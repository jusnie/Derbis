package com.luc4n3x.levyra.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EightDAudioSettingsTest {
    @Test fun `eight d audio is opt in and survives normalization`() {
        assertFalse(LevyraAudioSettings().normalized().eightDAudioEnabled)
        assertTrue(LevyraAudioSettings(eightDAudioEnabled = true).normalized().eightDAudioEnabled)
    }
}
