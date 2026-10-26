package com.luc4n3x.derbis.feature.motion

import com.luc4n3x.derbis.domain.DerbisCanvasSource
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
                DerbisCanvasSource.Auto
            )
        )
    }

    @Test
    fun legacyStoredSourceValuesMigrateToOwnedCatalogAutoMode() {
        assertEquals(DerbisCanvasSource.Auto, DerbisCanvasSource.from("community"))
        assertEquals(DerbisCanvasSource.Auto, DerbisCanvasSource.from("apple"))
        assertEquals(DerbisCanvasSource.Auto, DerbisCanvasSource.from("tidal"))
        assertEquals(DerbisCanvasSource.Auto, DerbisCanvasSource.from("anything-else"))
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
