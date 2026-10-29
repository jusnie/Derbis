package com.yasuda.derbis.viewmodel

import com.yasuda.derbis.domain.DerbisAudioSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSettingsPersistenceCoordinatorTest {
    @Test
    fun staleWriteCannotReplaceLatestSavedPresets() {
        val writes = mutableListOf<DerbisAudioSettings>()
        val coordinator = AudioSettingsPersistenceCoordinator(writes::add)
        val old = DerbisAudioSettings()
        val latest = old.copy(customPresets = (1..12).map { index ->
            com.yasuda.derbis.domain.DerbisAudioPreset(
                id = "custom_$index", fallbackLabel = "Headphones $index",
                levels = List(10) { 0 }, bassBoost = 0, virtualizer = 0
            )
        })
        coordinator.schedule(old)
        coordinator.schedule(latest)
        coordinator.persist(latest)
        coordinator.persist(old)
        coordinator.flush()

        assertEquals(listOf(latest), writes)
    }

    @Test
    fun cleanupFlushesLatestPendingAudioSettings() {
        val writes = mutableListOf<DerbisAudioSettings>()
        val coordinator = AudioSettingsPersistenceCoordinator(writes::add)
        val first = DerbisAudioSettings(preampDb = -2f)
        val latest = DerbisAudioSettings(preampDb = 1f)

        coordinator.schedule(first)
        coordinator.schedule(latest)
        coordinator.flush()
        coordinator.flush()

        assertEquals(listOf(latest), writes)
    }
}
