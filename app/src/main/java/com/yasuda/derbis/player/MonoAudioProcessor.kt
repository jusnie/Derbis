package com.yasuda.derbis.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Folds a stereo stream down to one channel, then copies it to both sides.
 *
 * Why not just play "one channel": a phone with a single earbud in, a mono Bluetooth speaker, or a
 * listener with hearing loss in one ear otherwise loses whichever half of the mix sits on the
 * silent side — a lead vocal, a guitar, a whole hi-hat. Downmixing to (L+R)/2 and sending that to
 * both outputs means nothing in the recording is unavailable to either ear.
 *
 * It is a straight average rather than a weighted one: a -3 dB pan law would make the result
 * quieter than the source and there is already a limiter and a normaliser downstream that are
 * entitled to change level. Summing without halving would clip on anything loud, so the halve is
 * the whole point.
 *
 * Mono cancels the stereo image, so it is mutually exclusive with the spatial effects in spirit —
 * 8D and the virtualiser both move channels around, and there is nothing left to move. The switch
 * for those stays where it is; this processor runs after them, which is exactly why it silences
 * their movement rather than fighting it.
 */
class MonoAudioProcessor : AudioProcessor {
    @Volatile
    var enabled: Boolean = false

    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        // Only the two encodings the rest of the chain speaks; anything else is a format the sink
        // asked for that this app never produces, and guessing would be worse than declining.
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        outputAudioFormat = inputAudioFormat
        return outputAudioFormat
    }

    override fun isActive(): Boolean = outputAudioFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val size = limit - inputBuffer.position()
        if (size <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }
        val output = replaceOutputBuffer(size)
        // A mono source is already what this processor produces, and a surround layout is not
        // something it can meaningfully fold; both pass through untouched.
        if (enabled && inputAudioFormat.channelCount == 2) {
            if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
                foldFloat(inputBuffer.asReadOnlyBuffer(), output)
            } else {
                foldShort(inputBuffer.asReadOnlyBuffer(), output)
            }
        } else {
            output.put(inputBuffer)
        }
        output.flip()
        inputBuffer.position(limit)
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && !outputBuffer.hasRemaining()

    override fun flush(streamMetadata: AudioProcessor.StreamMetadata) {
        inputEnded = false
        outputBuffer = AudioProcessor.EMPTY_BUFFER
    }

    override fun reset() {
        inputEnded = false
        buffer = AudioProcessor.EMPTY_BUFFER
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }

    private fun foldFloat(input: ByteBuffer, output: ByteBuffer) {
        input.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        while (input.remaining() >= 8) {
            val left = input.float
            val right = input.float
            val mono = (left + right) * 0.5f
            output.putFloat(mono)
            output.putFloat(mono)
        }
        drainRemainder(input, output)
    }

    private fun foldShort(input: ByteBuffer, output: ByteBuffer) {
        input.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        while (input.remaining() >= 4) {
            val left = input.short.toInt()
            val right = input.short.toInt()
            // Sum in Int before halving, so the average of two samples near full scale cannot wrap
            // the way a Short average would.
            val mono = ((left + right) / 2).toShort()
            output.putShort(mono)
            output.putShort(mono)
        }
        drainRemainder(input, output)
    }

    /**
     * A trailing byte shorter than one stereo frame is not audio.
     *
     * It is copied rather than dropped so the byte count the sink is expecting is preserved; a
     * partial frame at the very end of a stream is a container artefact, not a sample.
     */
    private fun drainRemainder(input: ByteBuffer, output: ByteBuffer) {
        while (input.hasRemaining()) output.put(input.get())
    }

    /** Grows [buffer] to at least [size] bytes and hands out a cleared buffer of exactly that size. */
    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN)
        } else {
            buffer.clear()
        }
        buffer.limit(size)
        return buffer
    }
}
