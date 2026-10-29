package com.yasuda.derbis.ui.theme

import com.yasuda.derbis.domain.DerbisVisualPerformance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisVisualPerformanceTest {

    @Test
    fun fullKeepsEveryEffectEvenOnConstrainedDevices() {
        val capabilities = resolveVisualCapabilities(
            selected = DerbisVisualPerformance.Full,
            animationsEnabled = true,
            lowRamDevice = true,
            powerSaveMode = true
        )
        assertEquals(DerbisVisualCapabilities.Full, capabilities)
    }

    @Test
    fun compositionDefaultMatchesFull() {
        assertEquals(
            DerbisVisualCapabilities.Full,
            resolveVisualCapabilities(DerbisVisualPerformance.Full, true, false, false)
        )
    }

    @Test
    fun smoothDropsDecorativeCostButNeverDependsOnDeviceSignals() {
        listOf(false, true).forEach { lowRam ->
            listOf(false, true).forEach { powerSave ->
                val capabilities = resolveVisualCapabilities(DerbisVisualPerformance.Smooth, true, lowRam, powerSave)
                assertFalse(capabilities.decorativeMotion)
                assertFalse(capabilities.depthTransitions)
                assertFalse(capabilities.microMotion)
                assertFalse(capabilities.heavyBlur)
            }
        }
    }

    @Test
    fun autoResolvesFromStableDeviceSignalsOnly() {
        assertEquals(
            DerbisVisualPerformance.Full,
            resolveVisualPerformance(DerbisVisualPerformance.Auto, lowRamDevice = false, powerSaveMode = false)
        )
        assertEquals(
            DerbisVisualPerformance.Smooth,
            resolveVisualPerformance(DerbisVisualPerformance.Auto, lowRamDevice = true, powerSaveMode = false)
        )
        assertEquals(
            DerbisVisualPerformance.Smooth,
            resolveVisualPerformance(DerbisVisualPerformance.Auto, lowRamDevice = false, powerSaveMode = true)
        )
    }

    @Test
    fun autoIsDeterministicForTheSameSignals() {
        val first = resolveVisualCapabilities(DerbisVisualPerformance.Auto, true, false, true)
        repeat(20) {
            assertEquals(first, resolveVisualCapabilities(DerbisVisualPerformance.Auto, true, false, true))
        }
    }

    @Test
    fun reducedMotionTurnsOffMotionButKeepsFullMaterial() {
        val capabilities = resolveVisualCapabilities(DerbisVisualPerformance.Full, false, false, false)
        assertFalse(capabilities.decorativeMotion)
        assertFalse(capabilities.depthTransitions)
        assertFalse(capabilities.microMotion)
        assertTrue(capabilities.heavyBlur)
    }
}
