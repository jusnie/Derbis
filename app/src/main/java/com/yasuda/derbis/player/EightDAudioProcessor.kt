package com.yasuda.derbis.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.yasuda.derbis.domain.Derbis360Experience
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class EightDAudioProcessor : AudioProcessor {
    @Volatile var enabled: Boolean = false
    @Volatile var experience: Derbis360Experience = Derbis360Experience.EIGHT_D
    @Volatile var headphoneOutputAvailable: Boolean = false

    private var format = AudioFormat.NOT_SET
    private var output = AudioProcessor.EMPTY_BUFFER
    private var storage = AudioProcessor.EMPTY_BUFFER
    private var ended = false
    private var phase = 0.0
    private var leftGain = 1f
    private var rightGain = 1f

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
        if (size <= 0) {
            output = AudioProcessor.EMPTY_BUFFER
            return
        }
        val out = replace(size)
        val input = inputBuffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        out.order(ByteOrder.LITTLE_ENDIAN)
        val activeExperience = experience
        val applyOrbit = enabled && headphoneOutputAvailable && format.channelCount == 2
        if (!applyOrbit) {
            out.put(input)
        } else if (format.encoding == C.ENCODING_PCM_FLOAT) {
            floatOrbit(input, out, activeExperience)
        } else {
            shortOrbit(input, out, activeExperience)
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
        phase = 0.0
    }

    override fun reset() {
        ended = false
        output = AudioProcessor.EMPTY_BUFFER
        phase = 0.0
        format = AudioFormat.NOT_SET
        storage = AudioProcessor.EMPTY_BUFFER
    }

    private fun replace(size: Int): ByteBuffer {
        if (storage.capacity() < size) storage = ByteBuffer.allocateDirect(size) else storage.clear()
        output = storage
        return storage
    }

    private fun advanceGains(experience: Derbis360Experience) {
        val sixteenD = experience == Derbis360Experience.SIXTEEN_D
        val basePan = sin(phase)
        val pan = if (sixteenD) {
            (basePan * 0.72 + sin(phase * 2.0 + PI / 2.0) * 0.28).coerceIn(-1.0, 1.0)
        } else {
            basePan
        }.toFloat()
        val rateHz = if (sixteenD) 0.105 else 0.055
        phase += (2.0 * PI * rateHz) / format.sampleRate.coerceAtLeast(1)
        if (phase >= 2.0 * PI) phase -= 2.0 * PI
        leftGain = sqrt((1f - pan) * 0.5f)
        rightGain = sqrt((1f + pan) * 0.5f)
    }

    private fun shortOrbit(input: ByteBuffer, out: ByteBuffer, experience: Derbis360Experience) {
        val sideMix = if (experience == Derbis360Experience.SIXTEEN_D) 0.34f else 0.22f
        while (input.remaining() >= 4) {
            advanceGains(experience)
            val left = input.short.toFloat()
            val right = input.short.toFloat()
            val mid = (left + right) * 0.5f
            val side = (left - right) * sideMix
            out.putShort((mid * leftGain + side).roundToInt().coerceIn(-32768, 32767).toShort())
            out.putShort((mid * rightGain - side).roundToInt().coerceIn(-32768, 32767).toShort())
        }
        while (input.hasRemaining()) out.put(input.get())
    }

    private fun floatOrbit(input: ByteBuffer, out: ByteBuffer, experience: Derbis360Experience) {
        val sideMix = if (experience == Derbis360Experience.SIXTEEN_D) 0.34f else 0.22f
        while (input.remaining() >= 8) {
            advanceGains(experience)
            val left = input.float
            val right = input.float
            val mid = (left + right) * 0.5f
            val side = (left - right) * sideMix
            out.putFloat(mid * leftGain + side)
            out.putFloat(mid * rightGain - side)
        }
        while (input.hasRemaining()) out.put(input.get())
    }
}
