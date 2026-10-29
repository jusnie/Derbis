package com.yasuda.derbis.player.enhanced

import com.yasuda.derbis.data.backupAudioSettingsFromJson
import com.yasuda.derbis.data.backupAudioSettingsToJson
import com.yasuda.derbis.domain.DerbisAudioSettings
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancedAudioSettingsPersistenceTest {

    @Test
    fun defaultEnhancedAudio_isEnabledByDefault() {
        val settings = DerbisAudioSettings()
        assertTrue("Derbis Enhanced Audio must be enabled by default", settings.enhancedAudioEnabled)
    }

    @Test
    fun toggleEnhancedAudio_updatesSettingsCorrectly() {
        val original = DerbisAudioSettings(enhancedAudioEnabled = true)
        assertTrue(original.enhancedAudioEnabled)

        val disabled = original.copy(enhancedAudioEnabled = false)
        assertFalse(disabled.enhancedAudioEnabled)

        val reEnabled = disabled.copy(enhancedAudioEnabled = true)
        assertTrue(reEnabled.enhancedAudioEnabled)
    }

    @Test
    fun backupRoundTrip_preservesEnhancedAudioState() {
        val enabledSettings = DerbisAudioSettings(enhancedAudioEnabled = true)
        val restoredEnabled = backupAudioSettingsFromJson(backupAudioSettingsToJson(enabledSettings))
        assertTrue(restoredEnabled.enhancedAudioEnabled)

        val disabledSettings = DerbisAudioSettings(enhancedAudioEnabled = false)
        val restoredDisabled = backupAudioSettingsFromJson(backupAudioSettingsToJson(disabledSettings))
        assertFalse(restoredDisabled.enhancedAudioEnabled)
    }

    @Test
    fun legacyBackup_defaultsEnhancedAudioToTrue() {
        val legacyJson = JSONObject()
        val restored = backupAudioSettingsFromJson(legacyJson)
        assertTrue("Legacy backups without enhancedAudioEnabled must default to true", restored.enhancedAudioEnabled)
    }
}
