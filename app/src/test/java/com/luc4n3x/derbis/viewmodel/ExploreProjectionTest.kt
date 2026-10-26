package com.luc4n3x.derbis.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreProjectionTest {
    @Test
    fun samplesRuntimeStateChangesProjection() {
        val baseline = exploreProjection(DerbisUiState())

        assertFalse(baseline.isResolving)
        assertFalse(baseline.isVideoMode)
        assertFalse(baseline.isFreshCurrentsLoading)

        assertTrue(exploreProjection(DerbisUiState(isResolving = true)).isResolving)
        assertTrue(exploreProjection(DerbisUiState(isVideoMode = true)).isVideoMode)
        assertTrue(exploreProjection(DerbisUiState(isFreshCurrentsLoading = true)).isFreshCurrentsLoading)
    }

    @Test
    fun mixRuntimeStateChangesProjectionImmediately() {
        val baseline = exploreProjection(DerbisUiState())
        val dragging = exploreProjection(DerbisUiState(mixFamiliarity = 0.82f))
        val loading = exploreProjection(DerbisUiState(mixLoading = true))
        val failed = exploreProjection(DerbisUiState(mixMessage = "mix-error"))

        assertEquals(0.5f, baseline.mixFamiliarity, 0.0001f)
        assertEquals(0.82f, dragging.mixFamiliarity, 0.0001f)
        assertTrue(loading.mixLoading)
        assertEquals("mix-error", failed.mixMessage)
    }
}
