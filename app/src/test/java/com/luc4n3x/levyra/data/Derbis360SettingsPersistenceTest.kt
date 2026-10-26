package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Derbis360Experience
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Derbis360SettingsPersistenceTest {
    @Test
    fun `selected derbis 360 experience is preserved by backup and restore`() {
        val restored = backupAudioSettingsFromJson(
            backupAudioSettingsToJson(
                LevyraAudioSettings(
                    eightDAudioEnabled = true,
                    derbis360Experience = Derbis360Experience.SIXTEEN_D
                )
            )
        )

        assertTrue(restored.eightDAudioEnabled)
        assertEquals(Derbis360Experience.SIXTEEN_D, restored.derbis360Experience)
    }

    @Test
    fun `legacy eight d backups retain their enabled master toggle and default experience`() {
        val restored = backupAudioSettingsFromJson(JSONObject().put("eightDAudioEnabled", true))

        assertTrue(restored.eightDAudioEnabled)
        assertEquals(Derbis360Experience.EIGHT_D, restored.derbis360Experience)
    }

    @Test
    fun `unknown experiences safely normalize to eight d`() {
        val restored = backupAudioSettingsFromJson(JSONObject().put("derbis360Experience", "invalid"))

        assertFalse(restored.eightDAudioEnabled)
        assertEquals(Derbis360Experience.EIGHT_D, restored.derbis360Experience)
    }
}
