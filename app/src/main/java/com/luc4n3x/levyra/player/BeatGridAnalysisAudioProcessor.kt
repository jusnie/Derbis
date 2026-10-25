package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.luc4n3x.levyra.domain.DerbisBeatAnalyzer
import com.luc4n3x.levyra.domain.DerbisBeatGrid
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Pass-through PCM tap used for local beat-grid analysis. It samples only the first part of a
 * normally played track, does no work on the playback buffer beyond decimation, and never stores
 * or transmits PCM. Analysis runs off the audio thread.
 */
class BeatGridAnalysisAudioProcessor(
    private val onGrid: (trackId: String, grid: DerbisBeatGrid) -> Unit
) : AudioProcessor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var inputFormat = AudioFormat.NOT_SET
    private var active = false
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var passthrough = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    @Volatile private var trackId: String = ""
    private var collected = FloatArray(0)
    private var collectedSize = 0
    private var decimation = 1
    private var frameCursor = 0L
    private var analysisStarted = false

    fun beginTrack(id: String) {
        if (id == trackId) return
        trackId = id
        collected = FloatArray(0)
        collectedSize = 0
        frameCursor = 0L
        analysisStarted = false
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if ((inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) ||
            inputAudioFormat.channelCount <= 0 || inputAudioFormat.sampleRate <= 0
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        inputFormat = inputAudioFormat
        decimation = (inputAudioFormat.sampleRate / ANALYSIS_SAMPLE_RATE).coerceAtLeast(1)
        active = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = active

    override fun queueInput(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val bytes = inputBuffer.remaining()
        if (bytes <= 0) {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return
        }
        capture(inputBuffer.asReadOnlyBuffer())
        val output = replaceBuffer(bytes)
        output.put(inputBuffer)
        output.flip()
        inputBuffer.position(limit)
    }

    private fun capture(source: ByteBuffer) {
        val id = trackId
        if (id.isBlank() || analysisStarted) return
        val channels = inputFormat.channelCount
        val bytesPerSample = if (inputFormat.encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val frames = source.remaining() / (channels * bytesPerSample)
        if (frames <= 0) return
        source.order(ByteOrder.LITTLE_ENDIAN)
        ensureCapacity(collectedSize + (frames / decimation + 2))
        repeat(frames) {
            var mono = 0f
            repeat(channels) {
                mono += if (inputFormat.encoding == C.ENCODING_PCM_FLOAT) source.float else source.short / 32768f
            }
            if (frameCursor % decimation == 0L && collectedSize < MAX_CAPTURED_SAMPLES) {
                collected[collectedSize++] = mono / channels.toFloat()
            }
            frameCursor++
        }
        if (collectedSize >= MIN_CAPTURED_SAMPLES && !analysisStarted) {
            analysisStarted = true
            val snapshot = collected.copyOf(collectedSize)
            val capturedTrack = id
            scope.launch {
                val grid = DerbisBeatAnalyzer.analyzeInterleaved(snapshot, ANALYSIS_SAMPLE_RATE, 1)
                if (trackId == capturedTrack && grid.isUsable) onGrid(capturedTrack, grid)
            }
        }
    }

    private fun ensureCapacity(required: Int) {
        if (collected.size >= required) return
        collected = collected.copyOf(minOf(MAX_CAPTURED_SAMPLES, maxOf(required, collected.size.coerceAtLeast(8_192) * 2)))
    }

    private fun replaceBuffer(size: Int): ByteBuffer {
        if (passthrough.capacity() < size) passthrough = ByteBuffer.allocateDirect(size)
        else passthrough.clear()
        passthrough.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer = passthrough
        return passthrough
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer = outputBuffer.also { outputBuffer = AudioProcessor.EMPTY_BUFFER }

    override fun isEnded(): Boolean = inputEnded && !outputBuffer.hasRemaining()

    override fun flush(streamMetadata: AudioProcessor.StreamMetadata) {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    override fun reset() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        passthrough = AudioProcessor.EMPTY_BUFFER
        inputFormat = AudioFormat.NOT_SET
        active = false
        inputEnded = false
        collected = FloatArray(0)
        collectedSize = 0
        trackId = ""
        frameCursor = 0L
        analysisStarted = false
    }

    private companion object {
        const val ANALYSIS_SAMPLE_RATE = 11_025
        const val MIN_CAPTURED_SAMPLES = ANALYSIS_SAMPLE_RATE * 12
        const val MAX_CAPTURED_SAMPLES = ANALYSIS_SAMPLE_RATE * 36
    }
}
