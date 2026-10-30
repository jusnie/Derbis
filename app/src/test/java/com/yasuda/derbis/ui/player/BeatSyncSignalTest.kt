package com.yasuda.derbis.ui.player

import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatSyncSignalTest {
    @Test
    fun silenceNeverInventsAProgressWaveOrBeat() {
        val signal = BeatSyncSignal()
        repeat(180) { signal.advance(FloatArray(0), 1f / 30f) }

        assertEquals(0f, signal.energy, 0f)
        assertEquals(0f, signal.impact, 0f)
        assertEquals(0f, signal.phase, 0f)
        for (index in 0..100) {
            assertEquals(0f, signal.ringOffset(index / 100f), 0f)
            assertEquals(0f, signal.progressOffset(index / 100f), 0f)
        }
    }

    @Test
    fun actualAudioEnergyDrivesBothArtworkAndProgress() {
        val signal = BeatSyncSignal()
        val samples = FloatArray(60) { if (it % 9 < 3) 0.65f else 0.08f }
        repeat(24) { signal.advance(samples, 1f / 30f) }

        assertTrue(signal.energy > 0.1f)
        assertTrue(signal.phase > 0f)
        assertFalse(signal.settled)
        assertTrue((0..100).any { signal.ringOffset(it / 100f) > 0.005f })
        assertTrue((0..100).any { kotlin.math.abs(signal.progressOffset(it / 100f)) > 0.01f })
    }

    @Test
    fun aSuddenAudioTransientProducesAnOnset() {
        val signal = BeatSyncSignal()
        val quiet = FloatArray(60) { 0.01f }
        repeat(240) { signal.advance(quiet, 1f / 30f) }
        val quietImpact = signal.impact
        repeat(8) { signal.advance(FloatArray(60) { 0.8f }, 1f / 30f) }

        assertTrue(signal.impact > quietImpact + 0.1f)
    }

    @Test
    fun missingPcmSettlesInsteadOfRunningADecorativeLoop() {
        val signal = BeatSyncSignal()
        repeat(20) { signal.advance(FloatArray(60) { 0.5f }, 1f / 30f) }
        val phase = signal.phase
        repeat(300) { signal.advance(FloatArray(0), 1f / 30f) }

        assertEquals(phase, signal.phase, 0f)
        assertEquals(0f, signal.energy, 0f)
        assertEquals(0f, signal.impact, 0f)
        assertTrue(signal.settled)
    }

    @Test
    fun trackOrPauseResetClearsAllVisualEnergy() {
        val signal = BeatSyncSignal()
        repeat(20) { signal.advance(FloatArray(60) { 0.5f }, 1f / 30f) }
        signal.reset()

        assertEquals(0f, signal.energy, 0f)
        assertEquals(0f, signal.impact, 0f)
        assertEquals(0f, signal.phase, 0f)
        assertEquals(0f, signal.ringOffset(0.2f), 0f)
        assertEquals(0f, signal.progressOffset(0.2f), 0f)
    }

    @Test
    fun visualSamplingDoesNotMutateTheSharedAudioFeed() {
        val samples = floatArrayOf(0.1f, 0.7f, 0.3f, 0.4f)
        val original = samples.copyOf()
        BeatSyncSignal().advance(samples, 1f / 30f)

        assertTrue(original.contentEquals(samples))
    }

    @Test
    fun malformedSamplesAndFrameTimesRemainFinite() {
        val signal = BeatSyncSignal()
        val samples = floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -0.7f, 3f)
        for (dt in floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0f, 0.03f, 5f)) {
            signal.advance(samples, dt)
        }
        assertTrue(signal.energy.isFinite())
        assertTrue(signal.impact.isFinite())
        assertTrue(signal.phase.isFinite())
        for (index in 0..100) {
            assertTrue(signal.ringOffset(index / 100f) in 0f..0.075f)
            assertTrue(signal.progressOffset(index / 100f) in -1f..1f)
        }
    }

    @Test
    fun ringSeamClosesAndProgressEndsStayAnchored() {
        val signal = BeatSyncSignal()
        repeat(30) { signal.advance(FloatArray(60) { 0.3f }, 1f / 30f) }

        assertEquals(signal.ringOffset(0f), signal.ringOffset(1f), 0.00001f)
        assertEquals(0f, signal.progressOffset(0f), 0.00001f)
        assertEquals(0f, signal.progressOffset(1f), 0.00001f)
    }

    @Test
    fun longPlaybackKeepsPhaseAndGeometryBounded() {
        val signal = BeatSyncSignal()
        val samples = FloatArray(60) { 0.25f }
        repeat(60_000) { index ->
            signal.advance(samples, 1f / 30f)
            if (index % 1_000 == 0) {
                assertTrue(signal.phase >= 0f && signal.phase < (PI * 2).toFloat())
                assertTrue(signal.ringOffset(0.3f) in 0f..0.075f)
                assertTrue(signal.progressOffset(0.3f) in -1f..1f)
            }
        }
    }

    @Test
    fun tapAndDragFractionsPreserveLtrAndRtlSeeking() {
        assertEquals(0.25f, beatSyncSeekFraction(25f, 100f, rtl = false), 0f)
        assertEquals(0.75f, beatSyncSeekFraction(25f, 100f, rtl = true), 0f)
        assertEquals(0f, beatSyncSeekFraction(-50f, 100f, rtl = false), 0f)
        assertEquals(1f, beatSyncSeekFraction(150f, 100f, rtl = false), 0f)
        assertEquals(1f, beatSyncSeekFraction(-50f, 100f, rtl = true), 0f)
        assertEquals(0f, beatSyncSeekFraction(150f, 100f, rtl = true), 0f)
    }

    @Test
    fun invalidSeekGeometryNeverProducesNaNOrAnOutOfRangePosition() {
        assertEquals(0f, beatSyncSeekFraction(20f, 0f, rtl = false), 0f)
        assertEquals(0f, beatSyncSeekFraction(Float.NaN, 100f, rtl = false), 0f)
        assertEquals(0f, beatSyncSeekFraction(20f, Float.POSITIVE_INFINITY, rtl = true), 0f)
        assertEquals(0f, beatSyncFraction(Float.NaN), 0f)
        assertEquals(1f, beatSyncFraction(2f), 0f)
    }

    @Test
    fun smallAndEmptyWaveformsCanBeInterpolatedSafely() {
        assertEquals(0f, sampleBeatSyncWaveform(FloatArray(0), 0.4f), 0f)
        assertEquals(0.4f, sampleBeatSyncWaveform(floatArrayOf(0.4f), 0.9f), 0f)
        assertEquals(0.5f, sampleBeatSyncWaveform(floatArrayOf(0f, 1f), 0.5f), 0f)
        assertEquals(0f, sampleBeatSyncWaveform(floatArrayOf(Float.NaN), 0.2f), 0f)
    }
}
