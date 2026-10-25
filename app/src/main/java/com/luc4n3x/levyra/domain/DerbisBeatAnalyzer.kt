package com.luc4n3x.levyra.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Lightweight, on-device onset analysis for PCM supplied by the audio path. It returns a beat-grid
 * candidate only when its periodicity confidence is high enough. A grid origin is a downbeat
 * candidate (bar phase is not guessed when the music does not contain reliable accent evidence).
 */
object DerbisBeatAnalyzer {
    private const val MIN_BPM = 70f
    private const val MAX_BPM = 190f
    private const val ANALYSIS_FRAME = 1_024
    private const val MAX_ANALYSIS_SECONDS = 36
    private const val MIN_ANALYSIS_SECONDS = 8

    fun analyzeInterleaved(
        samples: FloatArray,
        sampleRate: Int,
        channels: Int
    ): DerbisBeatGrid {
        if (sampleRate !in 8_000..192_000 || channels !in 1..8 || samples.isEmpty()) return DerbisBeatGrid()
        val availableFrames = samples.size / channels
        val maxFrames = minOf(availableFrames, sampleRate * MAX_ANALYSIS_SECONDS)
        if (maxFrames < sampleRate * MIN_ANALYSIS_SECONDS) return DerbisBeatGrid()
        val hop = (ANALYSIS_FRAME / 2).coerceAtLeast(256)
        val envelope = onsetEnvelope(samples, maxFrames, channels, hop)
        if (envelope.size < 16) return DerbisBeatGrid()
        return estimateGrid(envelope, sampleRate, hop)
    }

    private fun onsetEnvelope(
        samples: FloatArray,
        frameCount: Int,
        channels: Int,
        hop: Int
    ): FloatArray {
        val result = ArrayList<Float>()
        var previousEnergy = 0f
        var frameStart = 0
        while (frameStart + ANALYSIS_FRAME <= frameCount) {
            var energy = 0f
            var sampleOffset = frameStart * channels
            repeat(ANALYSIS_FRAME) {
                var mono = 0f
                repeat(channels) { channel -> mono += samples[sampleOffset + channel] }
                mono /= channels.toFloat()
                energy += abs(mono)
                sampleOffset += channels
            }
            energy /= ANALYSIS_FRAME.toFloat()
            result += max(0f, energy - previousEnergy)
            previousEnergy = energy
            frameStart += hop
        }
        val values = result.toFloatArray()
        val mean = values.average().toFloat()
        var variance = 0f
        values.forEach { variance += (it - mean) * (it - mean) }
        val spread = sqrt(variance / values.size.coerceAtLeast(1)).coerceAtLeast(0.000001f)
        // Suppress the noise floor while retaining relative onset strength for correlation.
        return FloatArray(values.size) { index -> ((values[index] - mean - spread * .15f) / spread).coerceAtLeast(0f) }
    }

    private fun estimateGrid(envelope: FloatArray, sampleRate: Int, hop: Int): DerbisBeatGrid {
        val framesPerSecond = sampleRate.toFloat() / hop.toFloat()
        val minLag = (framesPerSecond * 60f / MAX_BPM).roundToInt().coerceAtLeast(1)
        val maxLag = (framesPerSecond * 60f / MIN_BPM).roundToInt().coerceAtMost(envelope.lastIndex)
        if (minLag >= maxLag) return DerbisBeatGrid()

        var bestLag = -1
        var bestScore = 0f
        var secondScore = 0f
        for (lag in minLag..maxLag) {
            var cross = 0f
            var leftPower = 0f
            var rightPower = 0f
            for (index in lag until envelope.size) {
                val left = envelope[index]
                val right = envelope[index - lag]
                cross += left * right
                leftPower += left * left
                rightPower += right * right
            }
            val score = if (leftPower > 0f && rightPower > 0f) cross / sqrt(leftPower * rightPower) else 0f
            if (score > bestScore) {
                secondScore = bestScore
                bestScore = score
                bestLag = lag
            } else if (score > secondScore) {
                secondScore = score
            }
        }
        if (bestLag <= 0 || bestScore <= 0f) return DerbisBeatGrid()

        val bpm = (60f * framesPerSecond / bestLag.toFloat())
        val periodicity = bestScore.coerceIn(0f, 1f)
        val separation = ((bestScore - secondScore) / bestScore.coerceAtLeast(.0001f)).coerceIn(0f, 1f)
        val confidence = (periodicity * .72f + separation * .28f).coerceIn(0f, 1f)
        if (confidence < DerbisBeatGrid.MIN_CONFIDENCE) return DerbisBeatGrid(confidence = confidence)

        var bestPhase = 0
        var phaseScore = -1f
        for (phase in 0 until bestLag) {
            var score = 0f
            var index = phase
            while (index < envelope.size) {
                score += envelope[index]
                index += bestLag
            }
            if (score > phaseScore) {
                phaseScore = score
                bestPhase = phase
            }
        }
        // Snap to the strongest local onset near the periodic phase, retaining a non-negative origin.
        val snapRadius = (bestLag / 3).coerceAtLeast(1)
        var snapped = bestPhase
        var snappedValue = envelope.getOrElse(bestPhase) { 0f }
        for (index in (bestPhase - snapRadius).coerceAtLeast(0)..(bestPhase + snapRadius).coerceAtMost(envelope.lastIndex)) {
            if (envelope[index] > snappedValue) {
                snapped = index
                snappedValue = envelope[index]
            }
        }
        val downbeatMs = (snapped.toLong() * hop.toLong() * 1_000L / sampleRate.toLong()).coerceAtLeast(0L)
        return DerbisBeatGrid(bpm = bpm, downbeatMs = downbeatMs, confidence = confidence)
    }
}
