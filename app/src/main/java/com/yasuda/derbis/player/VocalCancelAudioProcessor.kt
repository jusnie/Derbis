package com.yasuda.derbis.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Karaoke mode: cancels the centre of the stereo image, which is where a lead vocal is mixed.
 *
 * The trick is the one every karaoke machine has used for decades. A stereo mix can be split into
 * a middle channel `M = (L + R) / 2`, which carries everything panned to the centre — lead vocal,
 * kick, bass, snare — and a side channel `S = (L - R) / 2`, which carries only what differs
 * between the speakers: guitars, keys, reverb tails, doubled backing vocals.
 *
 * A plain `L - R` removes the vocal but also guts the low end, because the kick and bass live in
 * the centre too; the result sounds like a phone speaker at arm's length. So this processor keeps
 * the centre's low band intact and only cancels its mid and high content:
 *
 * ```
 * low  = lowpass(M, ~160 Hz)     // keep the rhythm section's body
 * hub  = M - low                 // vocals, snare crack, most of the mix
 * L'   = low + hub * cancel + S
 * R'   = low + hub * cancel - S
 * ```
 *
 * With [cancelAmount] near zero the lead vocal disappears and the band stays. It is honest about
 * its limits: it cannot touch a mono master (single channel is passed through untouched), a vocal
 * printed with heavy reverb and delay only dips, and anything doubled wide will survive in `S`.
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

    /** One-pole state for the shared low band of M. */
    private var lowState = 0f
    private var lowCoefficient = 0f

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        format = inputAudioFormat
        // 160 Hz keeps the kick and the bass guitar audible, which is the difference between a
        // karaoke track and a thin one. A one-pole is enough: this is a body guard, not a filter.
        val sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(1)
        lowCoefficient = 1f - kotlin.math.exp(-2.0 * kotlin.math.PI * LowBandHz / sampleRate).toFloat()
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
        lowState = 0f
    }

    override fun reset() {
        ended = false
        output = AudioProcessor.EMPTY_BUFFER
        storage = AudioProcessor.EMPTY_BUFFER
        format = AudioFormat.NOT_SET
        depth = 0f
        lowState = 0f
        lowCoefficient = 0f
        rampStepPerSample = 0f
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

    private fun shortCancel(input: ByteBuffer, out: ByteBuffer) {
        val target = targetDepth()
        while (input.remaining() >= 4) {
            val d = advanceDepth(target)
            val left = input.short.toFloat()
            val right = input.short.toFloat()
            val mid = (left + right) * 0.5f
            lowState += (mid - lowState) * lowCoefficient
            val hub = mid - lowState
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
            lowState += (mid - lowState) * lowCoefficient
            val hub = mid - lowState
            val centre = hub * d
            out.putFloat(left - centre)
            out.putFloat(right - centre)
        }
        while (input.hasRemaining()) out.put(input.get())
    }

    private fun Float.roundToShort(): Short =
        kotlin.math.round(this).toInt().coerceIn(-32768, 32767).toShort()

    private companion object {
        const val LowBandHz = 160.0
        const val RampSeconds = 0.18f
    }
}
