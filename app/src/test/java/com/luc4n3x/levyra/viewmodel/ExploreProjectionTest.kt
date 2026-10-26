package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreProjectionTest {
    @Test
    fun samplesRuntimeStateChangesProjection() {
        val baseline = exploreProjection(LevyraUiState())

        assertFalse(baseline.isResolving)
        assertFalse(baseline.isVideoMode)
        assertFalse(baseline.isFreshCurrentsLoading)

        assertTrue(exploreProjection(LevyraUiState(isResolving = true)).isResolving)
        assertTrue(exploreProjection(LevyraUiState(isVideoMode = true)).isVideoMode)
        assertTrue(exploreProjection(LevyraUiState(isFreshCurrentsLoading = true)).isFreshCurrentsLoading)
    }

    @Test
    fun mixRuntimeStateChangesProjectionImmediately() {
        val baseline = exploreProjection(LevyraUiState())
        val dragging = exploreProjection(LevyraUiState(mixFamiliarity = 0.82f))
        val loading = exploreProjection(LevyraUiState(mixLoading = true))
        val failed = exploreProjection(LevyraUiState(mixMessage = "mix-error"))

        assertEquals(0.5f, baseline.mixFamiliarity, 0.0001f)
        assertEquals(0.82f, dragging.mixFamiliarity, 0.0001f)
        assertTrue(loading.mixLoading)
        assertEquals("mix-error", failed.mixMessage)
    }
}
