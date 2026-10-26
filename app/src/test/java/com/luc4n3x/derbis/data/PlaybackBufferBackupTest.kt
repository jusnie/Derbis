package com.luc4n3x.derbis.data

import com.luc4n3x.derbis.domain.DerbisAudioSettings
import com.luc4n3x.derbis.domain.PlaybackBufferMode
import com.luc4n3x.derbis.domain.PlaybackBufferSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackBufferBackupTest {
    @Test
    fun customBufferRoundTripsThroughBackup() {
        val configured = PlaybackBufferSettings(
            mode = PlaybackBufferMode.CUSTOM,
            minBufferSeconds = 8f,
            maxBufferSeconds = 32f,
            playbackBufferSeconds = 0.7f,
            rebufferSeconds = 1.6f
        )

        val restored = backupAudioSettingsFromJson(
            backupAudioSettingsToJson(DerbisAudioSettings(playbackBuffer = configured))
        )

        assertEquals(configured, restored.playbackBuffer)
    }

    @Test
    fun legacyBackupDefaultsToAutomatic() {
        assertEquals(
            PlaybackBufferMode.AUTOMATIC,
            backupAudioSettingsFromJson(JSONObject()).playbackBuffer.mode
        )
    }
}
