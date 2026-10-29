package com.yasuda.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisAudioPresetsTest {

    private val addedPresetIds = listOf(
        DerbisAudioPresets.ROCK,
        DerbisAudioPresets.POP,
        DerbisAudioPresets.ELECTRONIC,
        DerbisAudioPresets.JAZZ,
        DerbisAudioPresets.ACOUSTIC,
        DerbisAudioPresets.CLASSICAL,
        DerbisAudioPresets.AIRPODS_PRO,
        DerbisAudioPresets.SONY_XM4,
        DerbisAudioPresets.SONY_XM5,
        DerbisAudioPresets.SENNHEISER_HD600
    )

    @Test
    fun presetIdsAreUnique() {
        val ids = DerbisAudioPresets.presets.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyPresetKeepsTheTenBandContract() {
        DerbisAudioPresets.presets.forEach { preset ->
            assertEquals(preset.id, DerbisAudioPresets.bandCount, preset.levels.size)
        }
    }

    @Test
    fun addedPresetIdsResolveThroughCatalogHelpers() {
        addedPresetIds.forEach { id ->
            val preset = DerbisAudioPresets.preset(id)

            assertEquals(id, DerbisAudioPresets.normalizePreset(id))
            assertEquals(preset.levels, DerbisAudioPresets.levelsFor(id))
            assertEquals(preset.fallbackLabel, DerbisAudioPresets.labelFor(id))
            assertTrue(DerbisAudioPresets.labelFor(id).isNotBlank())
        }
    }
}
