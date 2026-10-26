package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.luc4n3x.levyra.domain.Derbis360Experience
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EightDAudioProcessorTest {
    @Test
    fun `processor is inactive until configured and toggle defaults off`() {
        val processor = EightDAudioProcessor()
        assertFalse(processor.enabled)
        assertFalse(processor.isActive)
    }

    @Test
    fun `enabled processor bypasses the orbit until a headphone output is available`() {
        val processor = EightDAudioProcessor().apply {
            enabled = true
            experience = Derbis360Experience.SIXTEEN_D
            configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        }
        val source = intArrayOf(12_000, -8_000, 10_000, -6_000)

        processor.queueInput(pcm16(source))
        assertArrayEquals(source, readPcm16(processor.output))

        processor.headphoneOutputAvailable = true
        processor.queueInput(pcm16(source))
        assertFalse(source.contentEquals(readPcm16(processor.output)))
    }

    @Test
    fun `eight and sixteen d experiences produce distinct headphone processing`() {
        val source = IntArray(96_000) { index -> if (index % 2 == 0) 9_000 else -7_000 }
        fun render(experience: Derbis360Experience): IntArray = EightDAudioProcessor().apply {
            enabled = true
            headphoneOutputAvailable = true
            this.experience = experience
            configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
            queueInput(pcm16(source))
        }.let { readPcm16(it.output) }

        assertFalse(render(Derbis360Experience.EIGHT_D).contentEquals(render(Derbis360Experience.SIXTEEN_D)))
    }

    private fun pcm16(samples: IntArray): ByteBuffer = ByteBuffer.allocateDirect(samples.size * Short.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .apply {
            samples.forEach { putShort(it.toShort()) }
            flip()
        }

    private fun readPcm16(buffer: ByteBuffer): IntArray = buffer.order(ByteOrder.LITTLE_ENDIAN).let { output ->
        IntArray(output.remaining() / Short.SIZE_BYTES) { output.short.toInt() }
    }
}
