package com.yasuda.derbis.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

/**
 * Karaoke mode: removes the centre of the stereo image, which is where a lead vocal is mixed.
 *
 * The trick is the one every karaoke machine has used for decades. A stereo mix splits into a
 * middle channel `M = (L + R) / 2`, which carries everything panned to the centre — lead vocal,
 * kick, bass, snare — and a side channel `S = (L - R) / 2`, which carries only what differs
 * between the speakers: guitars, keys, reverb tails, doubled backing vocals.
 *
 * A plain `L - R` removes the vocal but also guts the low end, because the kick and bass live in
 * the centre too; the result sounds like a phone speaker at arm's length. So this processor keeps
 * the centre's low band intact and cancels everything above it:
 *
 * ```
 * low  = lowpass4(M, ~110 Hz)    // keep the rhythm section's body
 * hub  = M - low                 // vocals, snare crack, most of the mix
 * L'   = L - hub * depth
 * R'   = R - hub * depth
 * ```
 *
 * The low band is a **fourth-order** Butterworth (two biquads, 24 dB/oct), not a one-pole. That
 * distinction is what makes the vocal actually go away: a one-pole at the same corner only takes
 * ~14 dB off the centre over the vocal's range, which leaves a plainly audible, slightly dull
 * singer. At 24 dB/oct the corner sits at 110 Hz, so the kick and the bass fundamental stay put
 * while the vocal's fundamental and every formant above ~200 Hz are pulled down by 20 dB to 80 dB.
 * It is a minimum-phase IIR filter, so it is not phase-linear, but the roll-off is smooth: no
 * ringing and no pre-echo. And because the exact quantity subtracted from both speakers is the
 * same, side content passes through bit-for-bit unchanged and the transition band cannot ring.
 *
 * It is honest about its limits: it cannot touch a mono master (a single channel is passed through
 * untouched), a vocal printed with heavy reverb and delay only dips, and anything doubled wide will
 * survive in `S`. It is not stem separation and it does not pretend to be.
 *
 * Everything is smoothed per sample, so switching karaoke on or off mid-song ramps over
 * [RampSeconds] instead of clicking. Float and 16-bit PCM are both handled: the lossless path runs
 * float, the ordinary path runs short.
 */
class VocalCancelAudioProcessor : AudioProcessor {

    @Volatile
    var enabled: Boolean = false

    /**
     * How much centre content survives, `0f..1f`. Zero is full karaoke (vocal gone, band thinner
     * in the middle), one is a bypass. The settings toggle drives this through [enabled]; the two
     * intermediate values exist so the processor can be given a "softer" mode later without a
     * second class.
     */
    @Volatile
    var centreAmount: Float = 0f

    private var format = AudioFormat.NOT_SET
    private var output = AudioProcessor.EMPTY_BUFFER
    private var storage = AudioProcessor.EMPTY_BUFFER
    private var ended = false

    /** Current smoothed depth, `0` = untouched, `1` = full cancel. */
    private var depth = 0f
    private var rampStepPerSample = 0f

    /** Two cascaded biquads making up the fourth-order Butterworth low band of M. */
    private val lowBandFirst = Biquad()
    private val lowBandSecond = Biquad()

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        format = inputAudioFormat
        val sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(1).toFloat()
        // 110 Hz keeps the kick and the bass fundamental audible, which is the difference between a
        // karaoke track and a thin one. Fourth order is what actually gets the vocal out: the same
        // corner as a one-pole leaves the singer intact.
        val corner = LowBandHz.coerceAtMost(sampleRate * 0.45f)
        lowBandFirst.setLowPass(corner, ButterworthQFirst, sampleRate)
        lowBandSecond.setLowPass(corner, ButterworthQSecond, sampleRate)
        rampStepPerSample = 1f / (sampleRate * RampSeconds).coerceAtLeast(1f)
        return inputAudioFormat
    }

    override fun isActive(): Boolean = format != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size <= 0) {
            output = AudioProcessor.EMPTY_BUFFER
            return
        }
        val out = replace(size)
        val input = inputBuffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        out.order(ByteOrder.LITTLE_ENDIAN)
        // A mono stream has no centre to cancel: every sample is the centre.
        if (format.channelCount != 2) {
            out.put(input)
        } else if (format.encoding == C.ENCODING_PCM_FLOAT) {
            floatCancel(input, out)
        } else {
            shortCancel(input, out)
        }
        out.flip()
        inputBuffer.position(inputBuffer.limit())
    }

    override fun queueEndOfStream() {
        ended = true
    }

    override fun getOutput(): ByteBuffer = output.also { output = AudioProcessor.EMPTY_BUFFER }

    override fun isEnded(): Boolean = ended && !output.hasRemaining()

    override fun flush(streamMetadata: AudioProcessor.StreamMetadata) {
        ended = false
        output = AudioProcessor.EMPTY_BUFFER
        lowBandFirst.reset()
        lowBandSecond.reset()
    }

    override fun reset() {
        ended = false
        output = AudioProcessor.EMPTY_BUFFER
        storage = AudioProcessor.EMPTY_BUFFER
        format = AudioFormat.NOT_SET
        depth = 0f
        rampStepPerSample = 0f
        lowBandFirst.reset()
        lowBandSecond.reset()
    }

    /** Target depth for this buffer, and one ramp step per sample frame. */
    private fun targetDepth(): Float = if (enabled) 1f - centreAmount.coerceIn(0f, 1f) else 0f

    private fun advanceDepth(target: Float): Float {
        depth = when {
            depth < target -> (depth + rampStepPerSample).coerceAtMost(target)
            depth > target -> (depth - rampStepPerSample).coerceAtLeast(target)
            else -> target
        }
        return depth
    }

    private fun replace(size: Int): ByteBuffer {
        if (storage.capacity() < size) storage = ByteBuffer.allocateDirect(size) else storage.clear()
        output = storage
        return storage
    }

    /** Low band of the mid channel for this sample frame. */
    private fun lowBand(mid: Float): Float = lowBandSecond.process(lowBandFirst.process(mid))

    private fun shortCancel(input: ByteBuffer, out: ByteBuffer) {
        val target = targetDepth()
        while (input.remaining() >= 4) {
            val d = advanceDepth(target)
            val left = input.short.toFloat()
            val right = input.short.toFloat()
            val mid = (left + right) * 0.5f
            val hub = mid - lowBand(mid)
            // `d` is how much of the centre is removed: 0 leaves the mix exactly as it is, 1 takes
            // the whole hub away. The side channel is already inside left and right, so
            // subtracting the same centre from both leaves it intact without a second mix stage —
            // the band keeps its width, the vocal does not.
            val centre = hub * d
            out.putShort((left - centre).roundToShort())
            out.putShort((right - centre).roundToShort())
        }
        while (input.hasRemaining()) out.put(input.get())
    }

    private fun floatCancel(input: ByteBuffer, out: ByteBuffer) {
        val target = targetDepth()
        while (input.remaining() >= 8) {
            val d = advanceDepth(target)
            val left = input.float
            val right = input.float
            val mid = (left + right) * 0.5f
            val hub = mid - lowBand(mid)
            val centre = hub * d
            out.putFloat(left - centre)
            out.putFloat(right - centre)
        }
        while (input.hasRemaining()) out.put(input.get())
    }

    private fun Float.roundToShort(): Short =
        kotlin.math.round(this).toInt().coerceIn(-32768, 32767).toShort()

    /**
     * Transposed direct form II biquad — two multiply-adds per sample, no allocation per buffer.
     * Coefficients come from the standard RBJ low-pass cookbook, normalised by `a0`.
     */
    private class Biquad {
        private var b0 = 0f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private var z1 = 0f
        private var z2 = 0f

        fun setLowPass(cornerHz: Float, q: Float, sampleRate: Float) {
            val w0 = 2.0 * kotlin.math.PI * cornerHz / sampleRate
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q)
            val a0 = 1.0 + alpha
            val b0 = (1.0 - cosW0) * 0.5
            val b1 = 1.0 - cosW0
            val b2 = b0
            val a1 = -2.0 * cosW0
            val a2 = 1.0 - alpha
            this.b0 = (b0 / a0).toFloat()
            this.b1 = (b1 / a0).toFloat()
            this.b2 = (b2 / a0).toFloat()
            this.a1 = (a1 / a0).toFloat()
            this.a2 = (a2 / a0).toFloat()
            reset()
        }

        fun reset() {
            z1 = 0f
            z2 = 0f
        }

        fun process(x: Float): Float {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b2 * x - a2 * y
            return y
        }
    }

    private companion object {
        /** Corner of the low band that survives. Below ~100 Hz nothing is touched. */
        const val LowBandHz = 110.0f

        /** Butterworth fourth-order Q pair: low section first, high section second. */
        const val ButterworthQFirst = 0.5411961f
        const val ButterworthQSecond = 1.3065630f

        const val RampSeconds = 0.18f
    }
}
