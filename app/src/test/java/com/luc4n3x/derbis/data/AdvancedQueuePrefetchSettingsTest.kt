package com.luc4n3x.derbis.data

import com.luc4n3x.derbis.domain.ADVANCED_QUEUE_PREFETCH_OFF
import com.luc4n3x.derbis.domain.DerbisAudioSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class AdvancedQueuePrefetchSettingsTest {
    @Test
    fun advancedPrefetchIsOffByDefault() {
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, DerbisAudioSettings().advancedQueuePrefetchTracks)
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, DerbisAudioSettings().normalized().advancedQueuePrefetchTracks)
    }

    @Test
    fun supportedWindowsSurviveNormalization() {
        listOf(0, 5, 10, 15).forEach { tracks ->
            assertEquals(tracks, DerbisAudioSettings(advancedQueuePrefetchTracks = tracks).normalized().advancedQueuePrefetchTracks)
        }
    }

    @Test
    fun unsupportedWindowsFallBackToOff() {
        listOf(-1, 1, 3, 7, 20, Int.MAX_VALUE).forEach { tracks ->
            assertEquals(
                ADVANCED_QUEUE_PREFETCH_OFF,
                DerbisAudioSettings(advancedQueuePrefetchTracks = tracks).normalized().advancedQueuePrefetchTracks
            )
        }
    }

    @Test
    fun windowRoundTripsThroughBackup() {
        val restored = backupAudioSettingsFromJson(
            backupAudioSettingsToJson(DerbisAudioSettings(advancedQueuePrefetchTracks = 10))
        )
        assertEquals(10, restored.advancedQueuePrefetchTracks)
    }

    @Test
    fun mobileDataChoiceIsOffByDefaultAndRoundTripsThroughBackup() {
        assertEquals(false, DerbisAudioSettings().advancedQueuePrefetchOnMobileData)
        val restored = backupAudioSettingsFromJson(
            backupAudioSettingsToJson(DerbisAudioSettings(advancedQueuePrefetchTracks = 15, advancedQueuePrefetchOnMobileData = true))
        )
        assertEquals(true, restored.advancedQueuePrefetchOnMobileData)
        assertEquals(false, backupAudioSettingsFromJson(JSONObject()).advancedQueuePrefetchOnMobileData)
    }

    @Test
    fun legacyBackupWithoutTheFieldRestoresOff() {
        val legacy = JSONObject().put("preloadNextTrack", true).put("gaplessEnabled", true)
        val restored = backupAudioSettingsFromJson(legacy)
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, restored.advancedQueuePrefetchTracks)
        assertEquals(true, restored.preloadNextTrack)
    }

    @Test
    fun corruptedBackupValueRestoresOff() {
        val corrupted = JSONObject().put("advancedQueuePrefetchTracks", 12)
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, backupAudioSettingsFromJson(corrupted).advancedQueuePrefetchTracks)
        val wrongType = JSONObject().put("advancedQueuePrefetchTracks", "many")
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, backupAudioSettingsFromJson(wrongType).advancedQueuePrefetchTracks)
    }
}
