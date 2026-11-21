package com.yasuda.derbis.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The numbers in these tests are the reason the filter was rewritten. A one-pole low band at
 * 160 Hz only removed about 14 dB of a centre vocal, which still reads as "the singer is there";
 * the fourth-order low band at 110 Hz takes more than 30 dB off the same signal while leaving the
 * kick and the bass fundamental within a fraction of a dB.
 */
class VocalCancelAudioProcessorTest {

    private val sampleRate = 44_100
    private val seconds = 2
    private val frameCount = sampleRate * seconds
    /** The depth ramp is 0.18 s; measure only after it has settled. */
    private val skipFrames = (sampleRate * 0.5).toInt()

    @Test
    fun cancelsACentrePannedVocal() {
        val vocal = formantVocal()

        val processed = process(stereo(vocal, vocal), channels = 2, encoding = C.ENCODING_PCM_FLOAT, enabled = true)

        val reductionDb = 20 * log10(rms(processed, channels = 2) / rms(vocal, channels = 1))
        assertTrue(
            "centre vocal should drop by at least 25 dB, was $reductionDb dB",
            reductionDb < -25.0
        )
    }

    @Test
    fun keepsTheBassAndKickBody() {
        val bass = FloatArray(frameCount) { index ->
            val t = index.toDouble() / sampleRate
            (0.5 * sin(2 * PI * 60.0 * t) + 0.3 * sin(2 * PI * 80.0 * t)).toFloat()
        }

        val processed = process(stereo(bass, bass), channels = 2, encoding = C.ENCODING_PCM_FLOAT, enabled = true)

        val before = rms(bass, channels = 1)
        val after = rms(processed, channels = 2)
        val deltaDb = 20 * log10(after / before)
        assertTrue("60/80 Hz body should survive, changed by ${deltaDb} dB", abs(deltaDb) < 1.0)
    }

    @Test
    fun leavesSideContentUntouched() {
        val side = FloatArray(frameCount) { index ->
            val t = index.toDouble() / sampleRate
            (0.4 * sin(2 * PI * 220.0 * t) + 0.2 * sin(2 * PI * 1_500.0 * t)).toFloat()
        }
        val input = stereo(side, FloatArray(side.size) { -side[it] })

        val processed = process(input, channels = 2, encoding = C.ENCODING_PCM_FLOAT, enabled = true)

        var worst = 0f
        for (index in skipFrames until frameCount) {
            worst = maxOf(worst, abs(processed[index * 2] - input[index * 2]))
            worst = maxOf(worst, abs(processed[index * 2 + 1] - input[index * 2 + 1]))
        }
        assertEquals("anti-phase side content must pass through exactly", 0f, worst, 1e-6f)
    }

    @Test
    fun isABitExactBypassWhenDisabled() {
        val vocal = FloatArray(frameCount) { index ->
            val t = index.toDouble() / sampleRate
            (0.3 * sin(2 * PI * 200.0 * t) + 0.4 * sin(2 * PI * 700.0 * t)).toFloat()
        }
        val input = stereo(vocal, vocal)

        val processed = process(input, channels = 2, encoding = C.ENCODING_PCM_FLOAT, enabled = false)

        assertTrue(input.contentEquals(processed))
    }

    @Test
    fun passesMonoThroughWithoutTouchingIt() {
        val mono = FloatArray(frameCount) { index ->
            val t = index.toDouble() / sampleRate
            (0.3 * sin(2 * PI * 200.0 * t) + 0.4 * sin(2 * PI * 700.0 * t)).toFloat()
        }

        val processed = process(mono, channels = 1, encoding = C.ENCODING_PCM_FLOAT, enabled = true)

        assertTrue(mono.contentEquals(processed))
    }

    @Test
    fun cancelsOnTheSixteenBitPathToo() {
        val vocal = formantVocal()

        val processed = process(stereo(vocal, vocal), channels = 2, encoding = C.ENCODING_PCM_16BIT, enabled = true)

        val reductionDb = 20 * log10(rms(processed, channels = 2) / rms(vocal, channels = 1))
        assertTrue(
            "centre vocal should drop on the 16-bit path too, was $reductionDb dB",
            reductionDb < -25.0
        )
    }

    /**
     * A 130 Hz fundamental with the energy sitting in the formants, which is what a mixed lead
     * vocal actually looks like. A lone 200 Hz sine would be a much easier target than a real one.
     */
    private fun formantVocal(): FloatArray {
        val partials = listOf(
            1 to 0.04,
            2 to 0.10,
            3 to 0.16,
            5 to 0.30,
            6 to 0.42,
            9 to 0.34,
            12 to 0.20,
            20 to 0.10
        )
        val raw = FloatArray(frameCount) { index ->
            val t = index.toDouble() / sampleRate
            var value = 0.0
            for ((multiple, amplitude) in partials) {
                value += amplitude * sin(2 * PI * 130.0 * multiple * t)
            }
            value.toFloat()
        }
        val peak = raw.maxOf { abs(it) }
        return FloatArray(frameCount) { (raw[it] / peak * 0.5f) }
    }

    private fun process(input: FloatArray, channels: Int, encoding: Int, enabled: Boolean): FloatArray {
        val processor = VocalCancelAudioProcessor()
        processor.enabled = enabled
        processor.configure(AudioFormat(sampleRate, channels, encoding))
        val bytes = if (encoding == C.ENCODING_PCM_FLOAT) {
            encodeFloat(input, channels)
        } else {
            encodeShort(input, channels)
        }
        processor.queueInput(bytes)
        val output = processor.getOutput()
        return if (encoding == C.ENCODING_PCM_FLOAT) decodeFloat(output) else decodeShort(output)
    }

    private fun encodeFloat(samples: FloatArray, channels: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(samples.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { buffer.putFloat(it) }
        buffer.flip()
        return buffer
    }

    private fun decodeFloat(buffer: ByteBuffer): FloatArray {
        val floats = buffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        return FloatArray(floats.remaining()) { floats.get() }
    }

    private fun encodeShort(samples: FloatArray, channels: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(samples.size * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { buffer.putShort((it * 32_767f).toInt().toShort()) }
        buffer.flip()
        return buffer
    }

    private fun decodeShort(buffer: ByteBuffer): FloatArray {
        val shorts = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return FloatArray(shorts.remaining()) { shorts.get() / 32_767f }
    }

    private fun stereo(left: FloatArray, right: FloatArray): FloatArray {
        val interleaved = FloatArray(left.size * 2)
        for (index in left.indices) {
            interleaved[index * 2] = left[index]
            interleaved[index * 2 + 1] = right[index]
        }
        return interleaved
    }

    private fun rms(samples: FloatArray, channels: Int): Double {
        val first = skipFrames * channels
        if (first >= samples.size) return 0.0
        var sum = 0.0
        for (index in first until samples.size) {
            val value = samples[index].toDouble()
            sum += value * value
        }
        return sqrt(sum / (samples.size - first))
    }
}
