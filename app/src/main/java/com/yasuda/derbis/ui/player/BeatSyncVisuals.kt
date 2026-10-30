package com.yasuda.derbis.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.yasuda.derbis.player.VisualizerAudioProcessor
import com.yasuda.derbis.ui.theme.LocalDerbisVisualCapabilities
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val BeatSyncRgb = listOf(
    Color(0xFF58F4FF), Color(0xFF6480FF), Color(0xFFD867FF),
    Color(0xFFFF6BBD), Color(0xFFFFBA78), Color(0xFF75F2CF), Color(0xFF58F4FF)
)
private val BeatSyncNoSamples = FloatArray(0)
private const val BeatSyncFrameNanos = 33_333_333L
private const val BeatSyncFreshSeconds = 0.25f

@Stable
internal class BeatSyncVisualState {
    val signal = BeatSyncSignal()
    var phase by mutableFloatStateOf(0f)
        private set
    var energy by mutableFloatStateOf(0f)
        private set
    var impact by mutableFloatStateOf(0f)
        private set

    fun advance(samples: FloatArray, dtSeconds: Float) {
        signal.advance(samples, dtSeconds)
        phase = signal.phase
        energy = signal.energy
        impact = signal.impact
    }

    fun reset() {
        signal.reset()
        phase = 0f
        energy = 0f
        impact = 0f
    }
}

@Composable
internal fun rememberBeatSyncVisualState(
    trackKey: String,
    isPlaying: Boolean,
    enabled: Boolean
): BeatSyncVisualState {
    val state = remember(trackKey) { BeatSyncVisualState() }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val decorative = LocalDerbisVisualCapabilities.current.decorativeMotion
    val active = enabled && decorative && lifecycle.isAtLeast(Lifecycle.State.STARTED)
    LaunchedEffect(state, active, isPlaying) {
        if (!active || !isPlaying) {
            state.reset()
            return@LaunchedEffect
        }
        var lastNanos = 0L
        var elapsedNanos = 0L
        var previous = VisualizerAudioProcessor.waveformState.value
        var sampleAge = BeatSyncFreshSeconds
        while (isActive) {
            withFrameNanos { now ->
                if (lastNanos != 0L) elapsedNanos += (now - lastNanos).coerceAtLeast(0L)
                lastNanos = now
                if (elapsedNanos < BeatSyncFrameNanos) return@withFrameNanos
                val dt = (elapsedNanos / 1_000_000_000f).coerceAtMost(0.1f)
                elapsedNanos = 0L
                val samples = VisualizerAudioProcessor.waveformState.value
                sampleAge = if (samples !== previous) 0f else sampleAge + dt
                previous = samples
                state.advance(if (sampleAge < BeatSyncFreshSeconds) samples else BeatSyncNoSamples, dt)
            }
            if (state.signal.settled) delay(80L)
        }
    }
    return state
}

@Composable
internal fun rememberBeatSyncPreviewState(): BeatSyncVisualState = remember {
    val samples = FloatArray(60) { index ->
        (0.11f + 0.13f * (1f + sin(index * 0.29f)) * 0.5f)
    }
    BeatSyncVisualState().apply {
        repeat(12) { advance(samples, 1f / 30f) }
    }
}

@Composable
internal fun BeatSyncArtworkFrame(
    visuals: BeatSyncVisualState,
    modifier: Modifier = Modifier,
    artwork: @Composable (Modifier) -> Unit
) {
    Box(
        modifier = modifier.drawWithCache {
            val ring = Path()
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val dimension = size.minDimension
            val brush = Brush.sweepGradient(BeatSyncRgb, center)
            val unit = dimension * 0.009f
            val farGlow = Stroke(unit * 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val nearGlow = Stroke(unit * 3.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val rim = Stroke(unit * 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val core = Stroke(unit * 0.68f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val shine = Stroke(unit * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val segments = 160
            val cosine = FloatArray(segments + 1) { index ->
                cos(index * 2f * PI.toFloat() / segments - PI.toFloat() * 0.5f)
            }
            val sine = FloatArray(segments + 1) { index ->
                sin(index * 2f * PI.toFloat() / segments - PI.toFloat() * 0.5f)
            }
            onDrawBehind {
                val phase = visuals.phase
                val energy = visuals.energy
                val impact = visuals.impact
                ring.reset()
                for (index in 0..segments) {
                    val offset = visuals.signal.ringOffset(index.toFloat() / segments, phase)
                    val radius = dimension * (0.405f + offset)
                    val x = center.x + cosine[index] * radius
                    val y = center.y + sine[index] * radius
                    if (index == 0) ring.moveTo(x, y) else ring.lineTo(x, y)
                }
                ring.close()
                drawPath(ring, brush, alpha = 0.035f + energy * 0.04f, style = farGlow)
                drawPath(ring, brush, alpha = 0.09f + energy * 0.09f + impact * 0.08f, style = nearGlow)
                drawPath(ring, brush, alpha = 0.34f + impact * 0.2f, style = rim)
                drawPath(ring, brush, style = core)
                drawPath(ring, Color.White.copy(alpha = 0.35f + impact * 0.3f), style = shine)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        artwork(
            Modifier
                .fillMaxSize(0.76f)
                .graphicsLayer {
                    val pulse = 1f + visuals.impact * 0.012f
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
        )
    }
}

@Composable
internal fun BeatSyncProgressBar(
    fraction: Float,
    enabled: Boolean,
    visuals: BeatSyncVisualState,
    onCommit: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragFraction by remember(visuals) { mutableStateOf<Float?>(null) }
    val latestCommit by rememberUpdatedState(onCommit)
    val shown = beatSyncFraction(dragFraction ?: fraction)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val full = remember { Path() }
    val played = remember { Path() }
    val brush = remember { Brush.horizontalGradient(BeatSyncRgb) }
    Box(modifier = modifier.fillMaxWidth().height(38.dp), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(48.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                    if (enabled) {
                        setProgress { target ->
                            if (!target.isFinite()) false else {
                                latestCommit(beatSyncFraction(target))
                                true
                            }
                        }
                    } else {
                        disabled()
                    }
                }
                .pointerInput(enabled, rtl, visuals) {
                    detectTapGestures { offset ->
                        if (enabled) latestCommit(beatSyncSeekFraction(offset.x, size.width.toFloat(), rtl))
                    }
                }
                .pointerInput(enabled, rtl, visuals) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (enabled) dragFraction = beatSyncSeekFraction(offset.x, size.width.toFloat(), rtl)
                        },
                        onDragEnd = {
                            if (enabled) dragFraction?.let(latestCommit)
                            dragFraction = null
                        },
                        onDragCancel = { dragFraction = null },
                        onHorizontalDrag = { change, _ ->
                            if (enabled) {
                                change.consume()
                                dragFraction = beatSyncSeekFraction(change.position.x, size.width.toFloat(), rtl)
                            }
                        }
                    )
                }
                .drawWithCache {
                    val trackStroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val glowStroke = Stroke(8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val edgeStroke = Stroke(4.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val coreStroke = Stroke(2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    onDrawBehind {
                        val width = size.width
                        val height = size.height
                        if (width <= 0f || height <= 0f) return@onDrawBehind
                        val phase = visuals.phase
                        val energy = visuals.energy
                        val impact = visuals.impact
                        val head = width * shown
                        val amplitude = minOf(15.dp.toPx(), height * 0.33f)
                        fun yAt(x: Float): Float = height * 0.5f -
                            visuals.signal.progressOffset(x / width, phase) * amplitude
                        full.reset()
                        played.reset()
                        fun screenX(x: Float): Float = if (rtl) width - x else x
                        full.moveTo(screenX(0f), yAt(0f))
                        played.moveTo(screenX(0f), yAt(0f))
                        for (index in 1..96) {
                            val x = width * index / 96f
                            val y = yAt(x)
                            full.lineTo(screenX(x), y)
                            if (x < head) played.lineTo(screenX(x), y)
                        }
                        played.lineTo(screenX(head), yAt(head))
                        drawPath(full, Color.White.copy(alpha = 0.20f), style = trackStroke)
                        if (shown > 0f) {
                            drawPath(played, brush, alpha = 0.08f + energy * 0.08f + impact * 0.1f, style = glowStroke)
                            drawPath(played, brush, alpha = 0.26f, style = edgeStroke)
                            drawPath(played, brush, style = coreStroke)
                        }
                    }
                }
        ) {}
    }
}
