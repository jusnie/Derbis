package com.yasuda.derbis.ui.player

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

internal class BeatSyncSignal {
    private val audio = ReactiveAuraSignal()
    private val input = FloatArray(SampleCount)
    private val envelope = FloatArray(SampleCount)

    var phase = 0f
        private set
    val energy: Float get() = audio.loudness
    val impact: Float get() = audio.onset
    val settled: Boolean get() = audio.settled

    fun advance(samples: FloatArray, dtSeconds: Float) {
        val dt = if (dtSeconds.isFinite()) dtSeconds.coerceIn(0f, 0.1f) else 0f
        if (dt <= 0f) return
        var peak = 0f
        for (index in input.indices) {
            val position = index.toFloat() / input.lastIndex
            val value = sampleBeatSyncWaveform(samples, position)
            input[index] = value
            peak = maxOf(peak, value)
            val target = (value * 3f).coerceAtMost(1f)
            val rate = if (target > envelope[index]) 22f else 9f
            envelope[index] += (target - envelope[index]) * (1f - exp(-rate * dt))
        }
        audio.advance(if (samples.isEmpty()) NoSamples else input, dt)
        if (peak > 0.0001f) {
            phase = (phase + dt * (1.5f + energy * 2f + impact * 2f)) % TwoPi
        }
        if (settled && peak == 0f) envelope.fill(0f)
    }

    fun ringOffset(fraction: Float, phase: Float = this.phase): Float {
        val position = beatSyncFraction(fraction)
        val angle = position * TwoPi
        val mirrored = abs(position * 2f - 1f)
        val sample = sampleBeatSyncWaveform(envelope, mirrored)
        val ripple = (sin(angle * 12f + phase) + 1f) * 0.5f
        val upper = cos(angle).coerceAtLeast(0f)
        val side = sin(angle)
        val shoulders = upper * upper * side * side * 4f
        return (energy * (0.003f + sample * (0.023f + ripple * 0.018f)) +
            impact * shoulders * 0.055f).coerceIn(0f, 0.075f)
    }

    fun progressOffset(fraction: Float, phase: Float = this.phase): Float {
        val position = beatSyncFraction(fraction)
        val angle = position * TwoPi
        val taper = sin(position * PI.toFloat()).coerceAtLeast(0f)
        val sample = sampleBeatSyncWaveform(envelope, position)
        val carrier = sin(angle * 3f - phase) * 0.72f + sin(angle * 7f + phase * 2f) * 0.28f
        return (carrier * taper * (energy * 0.72f + impact * 0.28f) * (0.4f + sample * 0.6f))
            .coerceIn(-1f, 1f)
    }

    fun reset() {
        audio.reset()
        input.fill(0f)
        envelope.fill(0f)
        phase = 0f
    }

    private companion object {
        const val SampleCount = 48
        const val TwoPi = (PI * 2.0).toFloat()
        val NoSamples = FloatArray(0)
    }
}

internal fun beatSyncFraction(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else 0f

internal fun beatSyncSeekFraction(x: Float, width: Float, rtl: Boolean): Float {
    if (!x.isFinite() || !width.isFinite() || width <= 0f) return 0f
    val fraction = beatSyncFraction(x / width)
    return if (rtl) 1f - fraction else fraction
}

internal fun sampleBeatSyncWaveform(samples: FloatArray, fraction: Float): Float {
    if (samples.isEmpty()) return 0f
    val location = beatSyncFraction(fraction) * samples.lastIndex
    val start = location.toInt().coerceIn(samples.indices)
    val end = (start + 1).coerceAtMost(samples.lastIndex)
    val mix = location - start
    val first = samples[start].let { if (it.isFinite()) abs(it).coerceAtMost(1f) else 0f }
    val second = samples[end].let { if (it.isFinite()) abs(it).coerceAtMost(1f) else 0f }
    return first + (second - first) * mix
}
