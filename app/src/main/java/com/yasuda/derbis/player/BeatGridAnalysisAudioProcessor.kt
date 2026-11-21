package com.yasuda.derbis.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.yasuda.derbis.domain.DerbisBeatAnalyzer
import com.yasuda.derbis.domain.DerbisBeatGrid
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt
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
    @Volatile private var capturedFromMs: Long = 0L
    private var collected = FloatArray(0)
    private var collectedSize = 0
    private var decimation = 1

    /**
     * The sample rate the decimated stream actually runs at. This is *not* always
     * [ANALYSIS_SAMPLE_RATE]: decimation is a whole number of input frames, so a 48 kHz decoder
     * (AAC and Opus both usually are) decimates by 4 to 12 000 Hz, not to 11 025. Pretending it
     * was 11 025 made every measured tempo 8.8% fast and walked the downbeat off the beat — the
     * grid was there but wrong, which is worse than no grid at all.
     */
    private var analysisSampleRate = ANALYSIS_SAMPLE_RATE
    private var frameCursor = 0L
    private var analysisStarted = false

    /**
     * Starts (or restarts) the capture for [id]. [fromPositionMs] is the playback position the
     * first captured sample corresponds to, so a track that starts or resumes mid-way still gets
     * a beat origin measured against the real timeline instead of against the capture window.
     */
    fun beginTrack(id: String, fromPositionMs: Long = 0L) {
        val origin = fromPositionMs.coerceAtLeast(0L)
        if (id == trackId && origin == capturedFromMs) return
        trackId = id
        capturedFromMs = origin
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
        decimation = (inputAudioFormat.sampleRate.toFloat() / ANALYSIS_SAMPLE_RATE)
            .roundToInt()
            .coerceAtLeast(1)
        analysisSampleRate = (inputAudioFormat.sampleRate / decimation).coerceAtLeast(1)
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
            val capturedOrigin = capturedFromMs
            val capturedRate = analysisSampleRate
            scope.launch {
                val grid = DerbisBeatAnalyzer.analyzeInterleaved(snapshot, capturedRate, 1)
                if (trackId == capturedTrack && grid.isUsable) {
                    onGrid(capturedTrack, grid.copy(capturedFromMs = capturedOrigin))
                }
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
        capturedFromMs = 0L
        analysisSampleRate = ANALYSIS_SAMPLE_RATE
        frameCursor = 0L
        analysisStarted = false
    }

    private companion object {
        /** Target analysis rate. Decimation rounds to the nearest whole frame count. */
        const val ANALYSIS_SAMPLE_RATE = 11_025
        const val MIN_CAPTURED_SAMPLES = ANALYSIS_SAMPLE_RATE * 12
        const val MAX_CAPTURED_SAMPLES = ANALYSIS_SAMPLE_RATE * 36
    }
}
