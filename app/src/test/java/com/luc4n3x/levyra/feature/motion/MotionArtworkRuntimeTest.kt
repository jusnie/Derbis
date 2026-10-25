package com.luc4n3x.levyra.feature.motion

import com.luc4n3x.levyra.domain.LevyraCanvasSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkRuntimeTest {
    @Test
    fun configurationAllowsOnlyTheOwnedCatalogProvider() {
        val normalized = MotionArtworkConfig(
            providerOrder = listOf("untrusted-provider", DerbisCanvasCatalogProvider.PROVIDER_ID, DerbisCanvasCatalogProvider.PROVIDER_ID),
            minimumConfidence = 500,
            requestTimeoutMs = 100L,
            positiveTtlMs = Long.MAX_VALUE,
            negativeTtlMs = 1L
        ).normalized()

        assertEquals(listOf(DerbisCanvasCatalogProvider.PROVIDER_ID), normalized.providerOrder)
        assertEquals(100, normalized.minimumConfidence)
        assertEquals(2_500L, normalized.requestTimeoutMs)
        assertTrue(normalized.positiveTtlMs <= 7L * 24L * 60L * 60L * 1000L)
        assertEquals(10L * 60L * 1000L, normalized.negativeTtlMs)
    }

    @Test
    fun autoResolutionUsesOnlyTheOwnedCatalog() {
        assertEquals(
            listOf(DerbisCanvasCatalogProvider.PROVIDER_ID),
            MotionArtworkConfig().providerOrder
        )
        assertEquals(
            listOf(DerbisCanvasCatalogProvider.PROVIDER_ID),
            motionArtworkProviderOrder(
                listOf("untrusted-provider", DerbisCanvasCatalogProvider.PROVIDER_ID),
                LevyraCanvasSource.Auto
            )
        )
    }

    @Test
    fun legacyStoredSourceValuesMigrateToOwnedCatalogAutoMode() {
        assertEquals(LevyraCanvasSource.Auto, LevyraCanvasSource.from("community"))
        assertEquals(LevyraCanvasSource.Auto, LevyraCanvasSource.from("apple"))
        assertEquals(LevyraCanvasSource.Auto, LevyraCanvasSource.from("tidal"))
        assertEquals(LevyraCanvasSource.Auto, LevyraCanvasSource.from("anything-else"))
    }

    @Test
    fun runtimeEpochChangesOnlyForARealConfigurationChange() {
        val initial = MotionArtworkRuntime.snapshot()
        assertTrue(initial.epoch >= MOTION_ARTWORK_CACHE_SCHEMA_EPOCH)
        try {
            val changed = MotionArtworkRuntime.update(initial.value.copy(requestTimeoutMs = 7_000L))
            val repeated = MotionArtworkRuntime.update(changed.value)

            assertEquals(initial.epoch + 1L, changed.epoch)
            assertEquals(changed.epoch, repeated.epoch)
        } finally {
            MotionArtworkRuntime.update(initial.value)
        }
    }
}
