package com.yasuda.derbis.data

import com.yasuda.derbis.domain.DerbisAmbientMode
import com.yasuda.derbis.domain.DerbisAmbientSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupAmbientSettingsTest {

    @Test
    fun ambientSettingsRoundTripThroughBackupJson() {
        val settings = DerbisAmbientSettings(
            brightness = 0.72f,
            autoDim = false,
            autoDimAfterSeconds = 45,
            pixelShift = false,
            proximityBlackout = true,
            showLyrics = false,
            showCanvas = false,
            mode = DerbisAmbientMode.Spotlight,
            showClock = false,
            showTitle = false,
            showProgress = false,
            amoledBlack = false
        )

        val restored = backupAmbientSettingsFromJson(backupAmbientSettingsToJson(settings))

        assertEquals(settings, restored)
    }

    @Test
    fun nonFiniteBrightnessFallsBackToDefault() {
        val restored = backupAmbientSettingsFromJson(
            JSONObject().put("brightness", "NaN")
        )

        assertEquals(DerbisAmbientSettings().brightness, restored.brightness)
    }

    @Test
    fun missingAmbientSettingsUseCurrentDefaults() {
        assertEquals(DerbisAmbientSettings(), backupAmbientSettingsFromJson(null))
        assertEquals(DerbisAmbientSettings(), backupAmbientSettingsFromJson(JSONObject()))
    }
}
