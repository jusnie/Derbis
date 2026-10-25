package com.luc4n3x.levyra.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EightDAudioSettingsTest {
    @Test fun `eight d audio is opt in and survives normalization`() {
        assertFalse(LevyraAudioSettings().normalized().eightDAudioEnabled)
        assertTrue(LevyraAudioSettings(eightDAudioEnabled = true).normalized().eightDAudioEnabled)
    }

    @Test fun `beat sync takes priority over dj crossfade when conflicting settings are restored`() {
        val normalized = LevyraAudioSettings(djSoftMode = true, beatSyncEnabled = true).normalized()
        assertTrue(normalized.beatSyncEnabled)
        assertFalse(normalized.djSoftMode)
    }
}
