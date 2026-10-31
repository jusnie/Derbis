package com.yasuda.derbis.ui.player

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.VolumeDown
import com.yasuda.derbis.ui.icons.CheckCircle
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import com.yasuda.derbis.ui.icons.GraphicEq
import com.yasuda.derbis.ui.icons.MoreVert
import com.yasuda.derbis.ui.icons.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.yasuda.derbis.player.VisualizerAudioProcessor
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.data.DerbisBeatGridStore
import com.yasuda.derbis.domain.DerbisBeatGrid
import com.yasuda.derbis.domain.DerbisCanvasQuality
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.feature.cast.CastRouteButton
import com.yasuda.derbis.feature.motion.MotionArtwork
import com.yasuda.derbis.ui.artwork.SeamlessArtworkImage
import com.yasuda.derbis.ui.InstantArtworkPlaceholder
import com.yasuda.derbis.ui.MotionArtworkLayer
import com.yasuda.derbis.ui.MotionArtworkPresentation
import com.yasuda.derbis.ui.components.DerbisLoadingIndicator
import com.yasuda.derbis.ui.components.DerbisPlayPauseGlyph
import com.yasuda.derbis.ui.components.DerbisPressScale
import com.yasuda.derbis.ui.components.PlayerControlLabels
import com.yasuda.derbis.ui.components.formatSeekbarMillis
import com.yasuda.derbis.ui.components.derbisMarquee
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.exp
import kotlin.math.floor

private val AuraInk = Color.White
private val AuraInkSoft = Color.White.copy(alpha = 0.64f)
private val AuraInkFaint = Color.White.copy(alpha = 0.24f)
private val AuraBadge = Color.White.copy(alpha = 0.14f)
private val AuraGutter = 22.dp
private val AuraBarIdle = 4.dp
private val AuraBarActive = 9.dp
private const val VelvetSleeveWidth = 0.86f
private val VelvetSleeveCorner = 24.dp

@Composable
internal fun PlayerAuraDeck(
    track: Track,
    slots: PlayerDeckSlots,
    artworkUrl: String,
    motionArtwork: MotionArtwork?,
    motionEnabled: Boolean,
    canvasQuality: DerbisCanvasQuality,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    positionMs: Long,
    durationMs: Long,
    labels: PlayerControlLabels,
    animated: Boolean,
    onCollapse: () -> Unit,
    onMore: () -> Unit,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onSeek: (Float) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val velvetTop = remember(track.id, track.accentStart) {
        androidx.compose.ui.graphics.lerp(Color(track.accentStart), Color.Black, 0.72f)
    }
    val velvetMid = remember(track.id, track.accentEnd) {
        androidx.compose.ui.graphics.lerp(Color(track.accentEnd), Color.Black, 0.86f)
    }
    // Swap: Velvet now carries the rounded sleeve that previously belonged to the Apple Music 13
    // deck. It remains static by default; an explicitly enabled verified Canvas loop plays inside
    // the sleeve without changing Velvet's layout or adding a palette/ink effect.
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(0f to velvetTop, 0.50f to velvetMid, 1f to Color.Black)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AuraGutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuraGrabber(onCollapse = onCollapse)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val sleeveSize = minOf(maxWidth * VelvetSleeveWidth, maxHeight - 16.dp)
                    .coerceAtLeast(152.dp)
                AuraVelvetSleeve(
                    track = track,
                    artworkUrl = artworkUrl,
                    motionArtwork = motionArtwork,
                    motionEnabled = motionEnabled,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    animated = animated,
                    canvasQuality = canvasQuality,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .size(sleeveSize)
                )
                slots.gestures(track, Modifier.matchParentSize())
            }
            AuraHeadline(
                track = track,
                isFavorite = isFavorite,
                isDownloaded = isDownloaded,
                animated = animated,
                onArtistClick = onArtistClick,
                onToggleFavorite = onToggleFavorite,
                onDownload = onDownload,
                onMore = onMore,
                modifier = headlineModifier
            )
            Spacer(Modifier.height(14.dp))
            AuraLine(
                fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                enabled = durationMs > 0L,
                isPlaying = isPlaying,
                animated = animated,
                onCommit = onSeek
            )
            AuraTimes(positionMs = positionMs, durationMs = durationMs)
            Spacer(Modifier.height(10.dp))
            AuraTransport(
                isPlaying = isPlaying,
                isResolving = isResolving,
                labels = labels,
                onPrevious = onPrevious,
                onTogglePlay = onTogglePlay,
                onNext = onNext
            )
            Spacer(Modifier.height(10.dp))
            PlayerOutputVolume()
            Spacer(Modifier.height(8.dp))
            AuraUtilityRow(actions = slots.dockActions(track))
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AuraVelvetSleeve(
    track: Track,
    artworkUrl: String,
    motionArtwork: MotionArtwork?,
    motionEnabled: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    animated: Boolean,
    canvasQuality: DerbisCanvasQuality,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val grid = remember(track.id) {
        runCatching { DerbisBeatGridStore(context).gridFor(track.id) }.getOrNull()?.takeIf { it.isUsable }
    }
    val beats = rememberBeatPosition(
        grid = grid,
        positionMs = positionMs,
        isPlaying = isPlaying,
        animated = animated
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        BeatAura(
            beats = beats,
            hasGrid = grid?.validBpm != null,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.84f)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.30f))
        ) {
            MotionArtworkLayer(
                artwork = motionArtwork,
                enabled = motionEnabled,
                isPlaying = isPlaying,
                cornerRadius = 999.dp,
                presentation = MotionArtworkPresentation.Card,
                quality = canvasQuality,
                staticArtworkMotionEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) {
                SeamlessArtworkImage(
                    url = artworkUrl,
                    modifier = Modifier.fillMaxSize(),
                    fallback = { InstantArtworkPlaceholder(modifier = Modifier.fillMaxSize()) }
                )
            }
        }
    }
}

/**
 * Continuous beat position of the playing track, restarted from the real playback position on
 * every update so the glow lands on the beat instead of on an arbitrary clock. Drawing reads it
 * inside the aura Canvas, which keeps the pulse in the draw phase.
 */
@Composable
private fun rememberBeatPosition(
    grid: DerbisBeatGrid?,
    positionMs: Long,
    isPlaying: Boolean,
    animated: Boolean
): MutableFloatState {
    val beats = remember { mutableFloatStateOf(0f) }
    val bpm = grid?.validBpm
    LaunchedEffect(grid, positionMs, isPlaying, animated) {
        if (bpm == null) {
            beats.floatValue = 0f
            return@LaunchedEffect
        }
        val anchor = positionMs.coerceAtLeast(0L)
        if (!isPlaying || !animated) {
            beats.floatValue = anchor * bpm / 60_000f
            return@LaunchedEffect
        }
        var startNanos = -1L
        while (true) {
            withFrameNanos { now ->
                if (startNanos < 0L) startNanos = now
                val advanced = anchor + (now - startNanos) / 1_000_000L
                beats.floatValue = advanced * bpm / 60_000f
            }
        }
    }
    return beats
}

/**
 * Ring of light behind the circular artwork. Every beat swells the ring and pushes the RGB sweep
 * outward; between beats it settles. Without a beat grid it keeps a slow accent glow only.
 */
@Composable
private fun BeatAura(beats: MutableFloatState, hasGrid: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.415f

        val beat = beats.floatValue
        val phase = beat - floor(beat)
        val hit = if (hasGrid) exp(-phase * 3.4f) else 0.32f
        val rotation = (beat * 26f) % 360f

        val radius = baseRadius + size.minDimension * 0.055f * hit
        val sweep = Brush.sweepGradient(
            colors = listOf(
                AuraCyan,
                AuraViolet,
                AuraMagenta,
                AuraAmber,
                AuraCyan
            ),
            center = center
        )

        // Wide bloom that carries most of the light.
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.62f to Color.Transparent,
                    0.84f to AuraViolet.copy(alpha = 0.10f + 0.26f * hit),
                    0.96f to AuraMagenta.copy(alpha = 0.06f + 0.18f * hit),
                    1f to Color.Transparent
                ),
                center = center,
                radius = radius * 1.42f
            ),
            radius = radius * 1.42f,
            center = center
        )

        rotate(degrees = rotation, pivot = center) {
            drawCircle(
                brush = sweep,
                radius = radius,
                center = center,
                style = Stroke(width = size.minDimension * 0.020f, cap = StrokeCap.Round),
                alpha = 0.55f + 0.45f * hit
            )
        }
        rotate(degrees = -rotation * 0.6f, pivot = center) {
            drawCircle(
                brush = sweep,
                radius = radius * 1.075f,
                center = center,
                style = Stroke(width = size.minDimension * 0.008f, cap = StrokeCap.Round),
                alpha = 0.30f + 0.50f * hit
            )
        }
        rotate(degrees = rotation * 1.4f, pivot = center) {
            drawCircle(
                brush = sweep,
                radius = radius * 1.16f,
                center = center,
                style = Stroke(width = size.minDimension * 0.004f, cap = StrokeCap.Round),
                alpha = 0.18f + 0.34f * hit
            )
        }
    }
}

private val AuraCyan = Color(0xFF00E5FF)
private val AuraViolet = Color(0xFF7C4DFF)
private val AuraMagenta = Color(0xFFFF2D95)
private val AuraAmber = Color(0xFFFFD400)

@Composable
private fun AuraGrabber(onCollapse: () -> Unit) {
    val strings = LocalDerbisStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .derbisPressable(onClick = onCollapse, role = Role.Button, pressedScale = DerbisPressScale.Control)
            .semantics { contentDescription = strings.collapsePlayer },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.46f))
        )
    }
}

@Composable
private fun AuraHeadline(
    track: Track,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    animated: Boolean,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDerbisStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = track,
            modifier = Modifier.weight(1f),
            transitionSpec = { DerbisMotion.contentSwap(animated) },
            contentKey = { it.id },
            label = "player-aura-headline"
        ) { shown ->
            Column {
                Text(
                    text = shown.title,
                    color = AuraInk,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .derbisMarquee(enabled = animated, repeatDelayMillis = 3_200)
                        .semantics { heading() }
                )
                PlayerArtistText(
                    track = shown,
                    color = AuraInkSoft,
                    style = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
                    onClickLabel = strings.openArtist,
                    onArtistClick = onArtistClick,
                    enabled = shown.id == track.id
                )
            }
        }
        AuraIconButton(
            icon = if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
            label = if (isDownloaded) strings.downloaded else strings.download,
            size = 24.dp,
            onClick = onDownload
        )
        AuraIconButton(
            icon = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            label = strings.favoritesPlain,
            size = 26.dp,
            onClick = onToggleFavorite
        )
        AuraIconButton(icon = Icons.Rounded.MoreVert, label = strings.more, size = 22.dp, onClick = onMore)
    }
}

@Composable
private fun AuraIconButton(icon: ImageVector, label: String, size: Dp, tint: Color = AuraInk, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .derbisPressable(onClick = onClick, role = Role.Button, pressedScale = DerbisPressScale.Control)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun AuraLine(
    fraction: Float,
    enabled: Boolean,
    isPlaying: Boolean = false,
    animated: Boolean = true,
    onCommit: (Float) -> Unit,
    onLive: ((Float) -> Unit)? = null
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val latestCommit by rememberUpdatedState(onCommit)
    val latestLive by rememberUpdatedState(onLive)
    val shown = dragFraction ?: fraction

    var waveTime by remember { mutableFloatStateOf(0f) }
    var waveBass by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, animated) {
        if (!animated) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    waveTime += dt * (if (isPlaying) 1.6f else 0.35f)

                    val waveform = if (isPlaying) VisualizerAudioProcessor.waveformState.value else FloatArray(0)
                    var bassSample = 0f
                    val sampleCount = minOf(8, waveform.size)
                    for (i in 0 until sampleCount) {
                        bassSample += waveform[i]
                    }
                    val targetBass = if (sampleCount > 0) (bassSample / sampleCount) * 1.5f else 0f
                    waveBass += (targetBass - waveBass) * (if (targetBass > waveBass) 0.35f else 0.12f)
                }
                lastNanos = now
            }
        }
    }

    val wavePlayedPath = remember { Path() }
    val waveUnplayedPath = remember { Path() }
    val waveFillPath = remember { Path() }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                setProgress { target ->
                    latestCommit(target.coerceIn(0f, 1f))
                    true
                }
            }
            .pointerInput(enabled) {
                detectTapGestures { offset ->
                    if (enabled) latestCommit((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(enabled) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (enabled) dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragFraction?.let(latestCommit)
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        if (enabled) {
                            val next = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragFraction = next
                            latestLive?.invoke(next)
                        }
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val midY = h * 0.52f
        val amp = (h * 0.34f + waveBass * 3.5.dp.toPx()).coerceAtMost(h * 0.46f)
        val xHead = (w * shown.coerceIn(0f, 1f))

        wavePlayedPath.reset()
        waveUnplayedPath.reset()
        waveFillPath.reset()

        val steps = 54
        val dx = w / steps

        fun computeY(x: Float): Float {
            val progressPhase = (x / w) * 9.5f
            val wave1 = kotlin.math.sin(progressPhase - waveTime * 1.3f) * 0.72f
            val wave2 = kotlin.math.cos(progressPhase * 1.5f + waveTime * 0.85f) * 0.28f
            return midY - (wave1 + wave2) * amp
        }

        var startedUnplayed = false
        wavePlayedPath.moveTo(0f, computeY(0f))
        waveFillPath.moveTo(0f, h)
        waveFillPath.lineTo(0f, computeY(0f))

        for (i in 1..steps) {
            val x = i * dx
            val y = computeY(x)

            if (x <= xHead) {
                wavePlayedPath.lineTo(x, y)
                waveFillPath.lineTo(x, y)
            } else {
                if (!startedUnplayed) {
                    val yAtHead = computeY(xHead)
                    wavePlayedPath.lineTo(xHead, yAtHead)
                    waveFillPath.lineTo(xHead, yAtHead)
                    waveFillPath.lineTo(xHead, h)
                    waveFillPath.close()

                    waveUnplayedPath.moveTo(xHead, yAtHead)
                    startedUnplayed = true
                }
                waveUnplayedPath.lineTo(x, y)
            }
        }

        if (!startedUnplayed) {
            waveFillPath.lineTo(w, h)
            waveFillPath.close()
        }

        // 1. Draw soft vertical gradient fill underneath the played wave
        drawPath(
            path = waveFillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF80DEEA).copy(alpha = 0.34f),
                    Color(0xFF26C6DA).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                startY = midY - amp,
                endY = h
            )
        )

        // 2. Draw unplayed wave line (dim translucent)
        drawPath(
            path = waveUnplayedPath,
            color = Color.White.copy(alpha = 0.20f),
            style = Stroke(
                width = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 3. Draw played wave line (vibrant cyan/white glow)
        // Outer soft glow
        drawPath(
            path = wavePlayedPath,
            color = Color(0xFF4DD0E1).copy(alpha = 0.35f),
            style = Stroke(
                width = 6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        // Core bright line
        drawPath(
            path = wavePlayedPath,
            color = Color(0xFFE0F7FA),
            style = Stroke(
                width = 2.8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 4. Draw moving scrubber stick ("move this stick on progress bar")
        val isDragging = dragFraction != null
        val stickWidth = if (isDragging) 4.5.dp.toPx() else 3.dp.toPx()
        val stickHeight = if (isDragging) 34.dp.toPx() else 28.dp.toPx()
        val stickTop = (h - stickHeight) / 2f
        val stickRadius = CornerRadius(stickWidth / 2f)

        // Outer glow on stick
        drawRoundRect(
            color = Color(0xFF4DD0E1).copy(alpha = 0.55f),
            topLeft = Offset(xHead - stickWidth * 1.5f, stickTop - 2.dp.toPx()),
            size = Size(stickWidth * 3f, stickHeight + 4.dp.toPx()),
            cornerRadius = CornerRadius(stickWidth * 1.5f)
        )
        // Core white stick
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(xHead - stickWidth / 2f, stickTop),
            size = Size(stickWidth, stickHeight),
            cornerRadius = stickRadius
        )
    }
}

@Composable
private fun AuraTimes(positionMs: Long, durationMs: Long) {
    val spec by rememberPlayerAudioSpec()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuraTime(formatSeekbarMillis(positionMs), Modifier.weight(1f))
        if (spec.codec.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AuraBadge)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = AuraInkSoft, modifier = Modifier.size(12.dp))
                Text(spec.codec, color = AuraInkSoft, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
            }
        }
        val remaining = if (durationMs > 0L) "-" + formatSeekbarMillis((durationMs - positionMs).coerceAtLeast(0L)) else "--:--"
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            AuraTime(remaining)
        }
    }
}

@Composable
private fun AuraTime(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = AuraInkSoft,
        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
        modifier = modifier
    )
}

@Composable
private fun AuraTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuraTransportButton(Icons.Rounded.FastRewind, labels.previous, 38.dp) {
            haptics.perform(DerbisHapticAction.Transport)
            onPrevious()
        }
        PlayerAuraPlayPauseButton(
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            onTogglePlay = onTogglePlay
        )
        AuraTransportButton(Icons.Rounded.FastForward, labels.next, 38.dp) {
            haptics.perform(DerbisHapticAction.Transport)
            onNext()
        }
    }
}

@Composable
internal fun PlayerAuraPlayPauseButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    onTogglePlay: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    Box(
        modifier = Modifier
            .size(72.dp)
            .derbisPressable(
                onClick = {
                    haptics.perform(DerbisHapticAction.Transport)
                    onTogglePlay()
                },
                role = Role.Button,
                pressedScale = DerbisPressScale.Control
            )
            .semantics { contentDescription = if (isPlaying) labels.pause else labels.play },
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            DerbisLoadingIndicator(color = AuraInk, modifier = Modifier.size(42.dp))
        } else {
            DerbisPlayPauseGlyph(
                playing = isPlaying,
                color = AuraInk,
                modifier = Modifier.size(42.dp),
                animateMorph = false
            )
        }
    }
}

@Composable
private fun AuraTransportButton(icon: ImageVector, label: String, glyph: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .derbisPressable(onClick = onClick, role = Role.Button, pressedScale = DerbisPressScale.Control)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = AuraInk, modifier = Modifier.size(glyph))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerOutputVolume(
    contentColor: Color = AuraInk,
    mutedColor: Color = AuraInkSoft,
    activeColor: Color = AuraInk,
    trackColor: Color = AuraBadge,
    condensed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val label = LocalDerbisStrings.current.volume
    val context = LocalContext.current
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember(audio) { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var level by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max) }
    DisposableEffect(audio) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                level = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val apply: (Float) -> Unit = { fraction ->
        level = fraction
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (fraction * max).toInt().coerceIn(0, max), 0)
    }
    val sliderColors = SliderDefaults.colors(
        thumbColor = activeColor,
        activeTrackColor = activeColor,
        inactiveTrackColor = trackColor
    )
    val sliderState = rememberSliderState(value = level.coerceIn(0f, 1f), trackRange = 0f..1f)
    LaunchedEffect(level) {
        sliderState.value = level.coerceIn(0f, 1f)
    }
    if (condensed) {
        Row(
            modifier = modifier.fillMaxWidth().height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.VolumeDown, contentDescription = null, tint = mutedColor, modifier = Modifier.size(19.dp))
            Slider(
                state = sliderState,
                onValueChange = { value ->
                    sliderState.value = value
                    apply(value)
                },
                colors = sliderColors,
                thumb = { Box(Modifier.size(8.dp).background(activeColor, CircleShape)) },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        colors = sliderColors,
                        modifier = Modifier.height(6.dp),
                        drawStopIndicator = null,
                        thumbTrackGapSize = 4.dp,
                        trackInsideCornerSize = 3.dp
                    )
                },
                modifier = Modifier.weight(1f).height(DerbisPlayerDesign.MinimumTouchTarget)
                    .semantics { contentDescription = label }
            )
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = mutedColor, modifier = Modifier.size(19.dp))
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = mutedColor, modifier = Modifier.size(20.dp))
                Text(label, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${(level * 100).toInt()}%", color = mutedColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                state = sliderState,
                onValueChange = { value ->
                    sliderState.value = value
                    apply(value)
                },
                colors = sliderColors,
                thumb = {
                    Box(Modifier.size(width = 4.dp, height = 40.dp).background(activeColor, CircleShape))
                },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        colors = sliderColors,
                        modifier = Modifier.height(32.dp),
                        drawStopIndicator = null,
                        thumbTrackGapSize = 4.dp,
                        trackInsideCornerSize = 4.dp
                    )
                },
                modifier = Modifier.fillMaxWidth().height(DerbisPlayerDesign.MinimumTouchTarget)
                    .semantics { contentDescription = label }
            )
        }
    }
}

@Composable
private fun AuraUtilityRow(actions: List<PlayerDockAction>) {
    val byKey = actions.associateBy { it.key }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        byKey["lyrics"]?.let { action ->
            AuraIconButton(action.icon, action.label, 22.dp, if (action.active) AuraInk else AuraInkSoft, action.onClick)
        }
        CastRouteButton(modifier = Modifier.size(44.dp))
        byKey["visual-mode"]?.let { action ->
            AuraIconButton(action.icon, action.label, 22.dp, AuraInkSoft, action.onClick)
        }
        byKey["queue"]?.let { action ->
            AuraIconButton(action.icon, action.label, 22.dp, AuraInkSoft, action.onClick)
        }
    }
}
