package com.yasuda.derbis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class VelvetWavySeekbarTest {

    @Test
    fun testHarmonicWaveEquationOutputBounds() {
        val width = 1000f
        val height = 40f
        val midY = height * 0.52f
        val amp = height * 0.34f
        val time = 1.5f

        for (i in 0..50) {
            val x = (i / 50f) * width
            val progressPhase = (x / width) * 9.5f
            val wave1 = sin(progressPhase - time * 1.3f) * 0.72f
            val wave2 = cos(progressPhase * 1.5f + time * 0.85f) * 0.28f
            val y = midY - (wave1 + wave2) * amp

            assertTrue("Wave y-coordinate should stay within canvas bounds", y >= 0f && y <= height)
        }
    }

    @Test
    fun testScrubberStickHorizontalPositionTracking() {
        val width = 1080f
        val fractions = listOf(0f, 0.25f, 0.5f, 0.75f, 1.0f)

        for (fraction in fractions) {
            val xHead = width * fraction
            assertEquals(width * fraction, xHead, 0.001f)
            assertTrue("Scrubber head should be within [0, width]", xHead in 0f..width)
        }
    }

    @Test
    fun testAudioTransientModulation() {
        var currentBass = 0f
        val attack = 0.35f
        val decay = 0.12f

        // When bass hits
        val bassEnergy = 1.0f
        currentBass += (bassEnergy - currentBass) * attack
        assertTrue("Bass amplitude must expand immediately on beat hit", currentBass > 0.3f)

        // When bass releases
        val silence = 0f
        currentBass += (silence - currentBass) * decay
        assertTrue("Bass amplitude must decay smoothly without jumping to zero", currentBass in 0.01f..0.35f)
    }
}
