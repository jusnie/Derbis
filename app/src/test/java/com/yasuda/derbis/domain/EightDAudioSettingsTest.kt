package com.yasuda.derbis.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EightDAudioSettingsTest {
    @Test fun `eight d audio is opt in and survives normalization`() {
        assertFalse(DerbisAudioSettings().normalized().eightDAudioEnabled)
        assertTrue(DerbisAudioSettings(eightDAudioEnabled = true).normalized().eightDAudioEnabled)
    }

    @Test fun `dj crossfade survives normalization now that the beat sync switch is gone`() {
        val normalized = DerbisAudioSettings(djSoftMode = true).normalized()
        assertTrue(normalized.djSoftMode)
        assertTrue(normalized.crossfadeSeconds >= 0)
    }
}
