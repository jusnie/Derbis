package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Local headphone-oriented, slow equal-power stereo orbit. This is an effect, not spatial audio metadata. */
class EightDAudioProcessor : AudioProcessor {
    @Volatile var enabled: Boolean = false
    private var format = AudioFormat.NOT_SET
    private var output = AudioProcessor.EMPTY_BUFFER
    private var storage = AudioProcessor.EMPTY_BUFFER
    private var ended = false
    private var phase = 0.0

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        format = inputAudioFormat
        return inputAudioFormat
    }
    override fun isActive(): Boolean = format != AudioFormat.NOT_SET
    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size <= 0) { output = AudioProcessor.EMPTY_BUFFER; return }
        val out = replace(size)
        val input = inputBuffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        out.order(ByteOrder.LITTLE_ENDIAN)
        if (!enabled || format.channelCount != 2) out.put(input) else if (format.encoding == C.ENCODING_PCM_FLOAT) floatOrbit(input, out) else shortOrbit(input, out)
        out.flip(); inputBuffer.position(inputBuffer.limit())
    }
    override fun queueEndOfStream() { ended = true }
    override fun getOutput(): ByteBuffer = output.also { output = AudioProcessor.EMPTY_BUFFER }
    override fun isEnded(): Boolean = ended && !output.hasRemaining()
    override fun flush(streamMetadata: AudioProcessor.StreamMetadata) { ended = false; output = AudioProcessor.EMPTY_BUFFER; phase = 0.0 }
    override fun reset() { ended = false; output = AudioProcessor.EMPTY_BUFFER; phase = 0.0; format = AudioFormat.NOT_SET; storage = AudioProcessor.EMPTY_BUFFER }
    private fun replace(size: Int): ByteBuffer {
        if (storage.capacity() < size) storage = ByteBuffer.allocateDirect(size) else storage.clear()
        output = storage; return storage
    }
    private fun gains(): Pair<Float, Float> {
        val pan = sin(phase).toFloat()
        phase += (2.0 * PI * 0.055) / format.sampleRate.coerceAtLeast(1)
        if (phase >= 2.0 * PI) phase -= 2.0 * PI
        return sqrt((1f - pan) * .5f) to sqrt((1f + pan) * .5f)
    }
    private fun shortOrbit(input: ByteBuffer, out: ByteBuffer) {
        while (input.remaining() >= 4) { val (l, r) = gains(); val left=input.short.toFloat(); val right=input.short.toFloat(); val mid=(left+right)*.5f; val side=(left-right)*.22f; out.putShort((mid*l+side).roundToInt().coerceIn(-32768,32767).toShort()); out.putShort((mid*r-side).roundToInt().coerceIn(-32768,32767).toShort()) }
        while (input.hasRemaining()) out.put(input.get())
    }
    private fun floatOrbit(input: ByteBuffer, out: ByteBuffer) {
        while (input.remaining() >= 8) { val (l, r) = gains(); val left=input.float; val right=input.float; val mid=(left+right)*.5f; val side=(left-right)*.22f; out.putFloat(mid*l+side); out.putFloat(mid*r-side) }
        while (input.hasRemaining()) out.put(input.get())
    }
}
