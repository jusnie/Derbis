package com.yasuda.derbis.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EightDAudioSettingsTest {
    @Test fun `eight d audio is opt in and survives normalization`() {
        assertFalse(DerbisAudioSettings().normalized().eightDAudioEnabled)
        assertTrue(DerbisAudioSettings(eightDAudioEnabled = true).normalized().eightDAudioEnabled)
    }

    @Test fun `beat sync takes priority over dj crossfade when conflicting settings are restored`() {
        val normalized = DerbisAudioSettings(djSoftMode = true, beatSyncEnabled = true).normalized()
        assertTrue(normalized.beatSyncEnabled)
        assertFalse(normalized.djSoftMode)
    }
}
