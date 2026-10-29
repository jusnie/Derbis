package com.yasuda.derbis.domain

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisBeatAnalyzerTest {
    @Test
    fun findsBpmAndBeatOriginFromRegularPcmOnsets() {
        val result = DerbisBeatAnalyzer.analyzeInterleaved(
            samples = clickTrack(sampleRate = 44_100, seconds = 20, bpm = 120f, firstBeatMs = 240L),
            sampleRate = 44_100,
            channels = 1
        )

        assertTrue(result.isUsable)
        assertEquals(120f, result.bpm ?: 0f, 4.5f)
        assertTrue(abs((result.downbeatMs ?: -1L) - 240L) < 80L)
    }

    @Test
    fun doesNotInventAGridForTooShortOrSilentInput() {
        val tooShort = DerbisBeatAnalyzer.analyzeInterleaved(FloatArray(4_000), 44_100, 1)
        val silence = DerbisBeatAnalyzer.analyzeInterleaved(FloatArray(44_100 * 12), 44_100, 1)

        assertFalse(tooShort.isUsable)
        assertFalse(silence.isUsable)
    }

    @Test
    fun beatAlignmentUsesAnalyzedGridAndRefusesLargeTempoCorrection() {
        val reference = DerbisBeatGrid(bpm = 120f, downbeatMs = 240L, confidence = .9f)
        val compatible = DerbisBeatGrid(bpm = 124f, downbeatMs = 120L, confidence = .9f)
        val incompatible = DerbisBeatGrid(bpm = 160f, downbeatMs = 120L, confidence = .9f)

        val aligned = DerbisMixStudio.alignIncomingToReference(10_240L, reference, compatible)
        val rejected = DerbisMixStudio.alignIncomingToReference(10_240L, reference, incompatible)

        assertTrue(aligned.aligned)
        assertEquals(120f / 124f, aligned.incomingTempo, .001f)
        assertFalse(rejected.aligned)
    }

    private fun clickTrack(sampleRate: Int, seconds: Int, bpm: Float, firstBeatMs: Long): FloatArray {
        val samples = FloatArray(sampleRate * seconds)
        val interval = (sampleRate * 60f / bpm).toInt()
        var start = (firstBeatMs * sampleRate / 1_000L).toInt()
        while (start < samples.size) {
            repeat(300) { index ->
                val location = start + index
                if (location < samples.size) samples[location] = 1f - index / 300f
            }
            start += interval
        }
        return samples
    }
}
