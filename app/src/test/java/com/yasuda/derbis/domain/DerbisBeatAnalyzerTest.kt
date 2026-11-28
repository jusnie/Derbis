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

    /**
     * The gate used to be calibrated on click tracks: on ordinary mixed music (kick, snare,
     * off-beat hats, a pad) the old score read ~0.26 against a 0.5 threshold, so no grid was ever
     * stored — and the beat visuals and Beat Sync had nothing to run on. This material is what
     * that gate has to accept.
     */
    @Test
    fun findsTheGridInMixedMusicNotJustAClickTrack() {
        val result = DerbisBeatAnalyzer.analyzeInterleaved(
            samples = mixedTrack(sampleRate = 12_000, seconds = 14, bpm = 120f),
            sampleRate = 12_000,
            channels = 1
        )

        assertTrue("mixed music should produce a usable grid", result.isUsable)
        assertEquals(120f, result.bpm ?: 0f, 3f)
    }

    /** ...and it still refuses to invent one for material with no pulse at all. */
    @Test
    fun doesNotInventAGridForBroadbandNoise() {
        val random = kotlin.random.Random(7)
        val noise = FloatArray(12_000 * 14) { random.nextFloat() * 2f - 1f }

        assertFalse(DerbisBeatAnalyzer.analyzeInterleaved(noise, 12_000, 1).isUsable)
    }

    private fun mixedTrack(sampleRate: Int, seconds: Int, bpm: Float): FloatArray {
        val samples = FloatArray(sampleRate * seconds)
        val beat = 60f / bpm
        fun hit(startSeconds: Float, lengthSeconds: Float, amplitude: Float, shape: (Float) -> Float) {
            val start = (startSeconds * sampleRate).toInt()
            val length = (lengthSeconds * sampleRate).toInt()
            for (index in 0 until length) {
                val position = start + index
                if (position !in samples.indices) continue
                samples[position] += amplitude * shape(index / sampleRate.toFloat())
            }
        }
        var bar = 0
        while (bar * 4 * beat < seconds) {
            val barStart = bar * 4 * beat
            hit(barStart, .22f, .55f) { t -> kotlin.math.sin(2f * Math.PI.toFloat() * (55f + 90f * kotlin.math.exp(-t * 30f)) * t) * kotlin.math.exp(-t * 14f) }
            hit(barStart + 2 * beat, .22f, .55f) { t -> kotlin.math.sin(2f * Math.PI.toFloat() * (55f + 90f * kotlin.math.exp(-t * 30f)) * t) * kotlin.math.exp(-t * 14f) }
            for (step in intArrayOf(1, 3)) {
                hit(barStart + step * beat, .18f, .30f) { t ->
                    kotlin.math.sin(2f * Math.PI.toFloat() * 190f * t) * kotlin.math.exp(-t * 22f)
                }
            }
            for (eighth in 1 until 8 step 2) {
                hit(barStart + eighth * beat / 2f, .05f, .10f) { t -> kotlin.math.exp(-t * 90f) }
            }
            hit(barStart, beat * 1.5f, .25f) { t -> kotlin.math.sin(2f * Math.PI.toFloat() * 55f * t) }
            hit(barStart + 2 * beat, beat * 1.2f, .22f) { t -> kotlin.math.sin(2f * Math.PI.toFloat() * 73f * t) }
            bar++
        }
        // A steady pad: energy the analyser has to ignore rather than count as an onset.
        for (index in samples.indices) {
            val t = index / sampleRate.toFloat()
            val chord = (kotlin.math.sin(2f * Math.PI.toFloat() * 220f * t) +
                kotlin.math.sin(2f * Math.PI.toFloat() * 277.2f * t) +
                kotlin.math.sin(2f * Math.PI.toFloat() * 330f * t)) / 3f
            samples[index] += .07f * chord
        }
        val peak = samples.maxOf { kotlin.math.abs(it) }.coerceAtLeast(.0001f)
        for (index in samples.indices) samples[index] = samples[index] / peak * .9f
        return samples
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

    @Test
    fun halfTimeTempoLockMatchesDoubleSpeedPairs() {
        val reference = DerbisBeatGrid(bpm = 128f, downbeatMs = 0L, confidence = .9f)
        val halfTime = DerbisBeatGrid(bpm = 62f, downbeatMs = 0L, confidence = .9f)
        val doubled = DerbisBeatGrid(bpm = 256f, downbeatMs = 0L, confidence = .9f)

        val paired = DerbisMixStudio.beatSync(reference, halfTime)
        val tooFast = DerbisMixStudio.beatSync(reference, doubled)

        assertTrue(paired.aligned)
        // 62 BPM doubled is 124 BPM, so the deck only has to run 3% fast.
        assertEquals(128f / 124f, paired.incomingTempo, .02f)
        // 256 BPM needs no stretch at all: every second incoming beat lands on the grid.
        assertTrue(tooFast.aligned)
        assertEquals(1f, tooFast.incomingTempo, .001f)
    }

    @Test
    fun longOverlapsStartOnABarLineAndShortOnesOnABeat() {
        val grid = DerbisBeatGrid(bpm = 120f, downbeatMs = 0L, confidence = .9f)

        val longStart = DerbisMixStudio.beatAlignedTransitionStartMs(
            durationMs = 239_300L,
            desiredTransitionMs = 8_000L,
            grid = grid
        )
        val shortStart = DerbisMixStudio.beatAlignedTransitionStartMs(
            durationMs = 239_300L,
            desiredTransitionMs = 1_200L,
            grid = grid
        )

        // 120 BPM: 500 ms per beat, 2 s per bar. A long fade may only start on a bar line,
        // while a 1.2 s overlap is snapped to the nearest beat so it is not stretched by a bar.
        assertEquals(230_000L, longStart ?: -1L)
        assertEquals(238_000L, shortStart ?: -1L)
    }

    @Test
    fun incomingDeckOpensOnItsFirstBarLine() {
        val reference = DerbisBeatGrid(bpm = 120f, downbeatMs = 240L, confidence = .9f)
        val incoming = DerbisBeatGrid(bpm = 122f, downbeatMs = 1_480L, confidence = .9f)

        val plan = DerbisMixStudio.alignIncomingToReference(120_000L, reference, incoming)

        assertTrue(plan.aligned)
        assertEquals(1_480L, plan.incomingSeekMs)
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
