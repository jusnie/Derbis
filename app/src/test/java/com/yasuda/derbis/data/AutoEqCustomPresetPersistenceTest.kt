package com.yasuda.derbis.data

import com.yasuda.derbis.domain.DerbisAudioPreset
import com.yasuda.derbis.domain.DerbisAudioPresets
import com.yasuda.derbis.domain.DerbisAudioSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEqCustomPresetPersistenceTest {

    @Test
    fun twelvePresetsSurviveRepeatedPersistenceAndBackupRoundTrips() {
        val presets = (1..12).map { index ->
            DerbisAudioPreset("custom_$index", "Headphones variant $index", List(10) { index }, 0, 0)
        }
        var restored = DerbisAudioSettings(customPresets = presets)
        repeat(3) {
            restored = backupAudioSettingsFromJson(backupAudioSettingsToJson(restored)).copy(
                customPresets = customPresetsFromJson(customPresetsToJson(restored.customPresets))
            ).normalized()
        }
        assertEquals(presets, restored.customPresets)
    }

    private val preset = DerbisAudioPreset(
        id = "custom_abc123",
        fallbackLabel = "My HD600 tune",
        levels = listOf(10, 8, 4, 0, -2, 0, 4, 8, 6, 2),
        bassBoost = 0,
        virtualizer = 0,
        preampDb = -4.5f
    )

    @Test
    fun customPresetsJsonRoundTrip() {
        val json = customPresetsToJson(listOf(preset))
        val restored = customPresetsFromJson(json)

        assertEquals(1, restored.size)
        assertEquals(preset, restored[0])
    }

    @Test
    fun customPresetsJsonRejectsBuiltInIds() {
        val json = customPresetsToJson(
            listOf(
                preset,
                preset.copy(id = DerbisAudioPresets.FLAT)
            )
        )

        assertEquals(listOf(preset), customPresetsFromJson(json))
    }

    @Test
    fun customPresetsJsonRejectsWrongBandCount() {
        val json = customPresetsToJson(listOf(preset.copy(levels = listOf(0, 0))))

        assertTrue(customPresetsFromJson(json).isEmpty())
    }

    @Test
    fun backupRoundTripPreservesCustomPresets() {
        val settings = DerbisAudioSettings(customPresets = listOf(preset))
        val restored = backupAudioSettingsFromJson(backupAudioSettingsToJson(settings))

        assertEquals(listOf(preset), restored.customPresets)
    }

    @Test
    fun legacyBackupWithoutCustomPresetsRemainsCompatible() {
        val legacy = JSONObject()
            .put("equalizerEnabled", true)
            .put("presetId", "rock")
            .put("bandLevels", org.json.JSONArray(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
            .put("preampDb", -2.0)

        val restored = backupAudioSettingsFromJson(legacy)

        assertTrue(restored.customPresets.isEmpty())
        assertEquals(-2f, restored.preampDb)
    }

    @Test
    fun normalizedSettingsBoundCustomPresets() {
        val levels = listOf(150, -150, 0, 0, 0, 0, 0, 0, 0, 0)
        val many = (0 until 30).map { index ->
            DerbisAudioPreset(
                id = "custom_$index",
                fallbackLabel = "Preset $index",
                levels = levels,
                bassBoost = 200,
                virtualizer = -10,
                preampDb = 9f
            )
        }
        val settings = DerbisAudioSettings(customPresets = many).normalized()

        assertEquals(DerbisAudioPresets.MAX_CUSTOM_PRESETS, settings.customPresets.size)
        settings.customPresets.forEach { custom ->
            assertEquals(listOf(100, -100, 0, 0, 0, 0, 0, 0, 0, 0), custom.levels)
            assertEquals(100, custom.bassBoost)
            assertEquals(0, custom.virtualizer)
            assertEquals(3f, custom.preampDb)
        }
    }

    @Test
    fun normalizedSettingsKeepSelectedCustomPresetId() {
        val settings = DerbisAudioSettings(
            presetId = preset.id,
            bandLevels = preset.levels,
            preampDb = preset.preampDb,
            customPresets = listOf(preset)
        ).normalized()

        assertEquals(preset.id, settings.presetId)
        assertEquals(preset.levels, settings.bandLevels)
        assertEquals(preset.preampDb, settings.preampDb)
    }

    @Test
    fun normalizedSettingsDropUnknownCustomPresetId() {
        val settings = DerbisAudioSettings(
            presetId = "custom_missing",
            customPresets = listOf(preset)
        ).normalized()

        assertEquals(DerbisAudioPresets.FLAT, settings.presetId)
    }

    @Test
    fun normalizedSettingsDropBuiltInIdsAndDuplicates() {
        val settings = DerbisAudioSettings(
            customPresets = listOf(
                preset,
                preset.copy(id = DerbisAudioPresets.ROCK),
                preset.copy(fallbackLabel = "Duplicate")
            )
        ).normalized()

        assertEquals(listOf(preset), settings.customPresets)
    }
}
