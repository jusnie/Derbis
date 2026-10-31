package com.yasuda.derbis.ui.player

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yasuda.derbis.data.DerbisArtworkCache
import com.yasuda.derbis.data.DerbisBeatGridStore
import com.yasuda.derbis.domain.DerbisBeatGrid
import com.yasuda.derbis.domain.DerbisCanvasQuality
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.feature.cast.CastRouteButton
import com.yasuda.derbis.feature.motion.MotionArtwork
import com.yasuda.derbis.ui.InstantArtworkPlaceholder
import com.yasuda.derbis.ui.MotionArtworkLayer
import com.yasuda.derbis.ui.MotionArtworkPresentation
import com.yasuda.derbis.ui.artwork.SeamlessArtworkImage
import com.yasuda.derbis.ui.components.DerbisLoadingIndicator
import com.yasuda.derbis.ui.components.DerbisPlayPauseGlyph
import com.yasuda.derbis.ui.components.DerbisPressScale
import com.yasuda.derbis.ui.components.PlayerControlLabels
import com.yasuda.derbis.ui.components.derbisMarquee
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.components.formatSeekbarMillis
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics
import kotlin.math.exp
import kotlin.math.floor

private val AuraInk = Color.White
private val AuraInkSoft = Color.White.copy(alpha = 0.64f)
private val AuraBadge = Color.White.copy(alpha = 0.14f)

/** Ink for glyphs drawn on the white play disc — not pure black, which reads as a hole. */
private val AuraOnInk = Color(0xFF07070A)
private val AuraGutter = 22.dp
private const val VelvetSleeveWidth = 0.86f

/** Touch target for the bar; the bar itself is [AuraBarHeight] tall inside it and centred, so the
    target is the full height even though the ink is a tenth of it. */
private val AuraProgressHeight = 40.dp
private val AuraBarHeight = 10.dp
private const val AURA_PROGRESS_MS = 260

/** Artwork is decoded tiny and then blurred, so 96 px is more than the blur can resolve. */
private const val ArtworkWashPx = 96

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
    // One beat clock for the whole deck: the ring around the artwork and the backdrop behind it
    // must swell on the same frame, and two independent clocks drift apart within a bar.
    val context = LocalContext.current
    val beatGrid = remember(track.id) {
        runCatching { DerbisBeatGridStore(context).gridFor(track.id) }.getOrNull()?.takeIf { it.isUsable }
    }
    val beats = rememberBeatPosition(
        grid = beatGrid,
        positionMs = positionMs,
        isPlaying = isPlaying,
        animated = animated
    )
    val hasBeatGrid = beatGrid?.validBpm != null
    val accentStart = remember(track.id, track.accentStart) { Color(track.accentStart) }
    val accentEnd = remember(track.id, track.accentEnd) { Color(track.accentEnd) }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(0f to velvetTop, 0.50f to velvetMid, 1f to Color.Black)
        )
    ) {
        AuraBackdrop(
            artworkUrl = artworkUrl,
            accent = accentStart,
            beats = beats,
            hasGrid = hasBeatGrid,
            isPlaying = isPlaying,
            modifier = Modifier.matchParentSize()
        )
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
                    artworkUrl = artworkUrl,
                    motionArtwork = motionArtwork,
                    motionEnabled = motionEnabled,
                    isPlaying = isPlaying,
                    beats = beats,
                    hasGrid = hasBeatGrid,
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
            Spacer(Modifier.height(6.dp))
            AuraProgress(
                fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                enabled = durationMs > 0L,
                accentStart = accentStart,
                accentEnd = accentEnd,
                animated = animated,
                onCommit = onSeek
            )
            AuraTimes(positionMs = positionMs, durationMs = durationMs)
            Spacer(Modifier.height(14.dp))
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
    artworkUrl: String,
    motionArtwork: MotionArtwork?,
    motionEnabled: Boolean,
    isPlaying: Boolean,
    beats: MutableFloatState,
    hasGrid: Boolean,
    animated: Boolean,
    canvasQuality: DerbisCanvasQuality,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        BeatAura(
            beats = beats,
            hasGrid = hasGrid,
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
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.38f))
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
                    fontSize = 19.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
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

/**
 * The blurred artwork behind the whole deck, at the opacity a background is allowed to have.
 *
 * It is why the page no longer reads as a flat black column: the sleeve's own colours bleed into
 * the room, but at 16–26 % and under a heavy scrim, so the title, the bar and the transport keep
 * contrast. The image is blurred from a 96 px request rather than a full-size one — the blur is
 * destructive anyway, and asking Coil for a thumbnail keeps the decode off the frame budget.
 *
 * The pulse is read inside `graphicsLayer` and `Canvas`, never in composition: on a beat grid the
 * backdrop breathes on the beat without recomposing a single node, and without a grid it settles
 * to a slow, static wash.
 */
@Composable
private fun AuraBackdrop(
    artworkUrl: String,
    accent: Color,
    beats: MutableFloatState,
    hasGrid: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Modifier.blur draws through a RenderEffect, which Android 12 introduced; older versions
    // fall through to the gradient and the glow, both of which still look deliberate.
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && artworkUrl.isNotBlank()
    val request = remember(context, artworkUrl) {
        ImageRequest.Builder(context)
            .data(DerbisArtworkCache.small(artworkUrl))
            .size(ArtworkWashPx, ArtworkWashPx)
            .crossfade(true)
            .build()
    }
    Box(modifier = modifier) {
        if (canBlur) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // Blur first, pulse second: the RenderEffect then wraps a still image and is
                // computed once, while the beat only re-composites the finished layer. Pulsing
                // inside the blur would re-run the effect on every frame of the track.
                modifier = Modifier
                    .fillMaxSize()
                    .blur(72.dp, BlurredEdgeTreatment.Unbounded)
                    .graphicsLayer {
                        val hit = auraBeatHit(beats, hasGrid, isPlaying)
                        alpha = 0.16f + 0.10f * hit
                        val scale = 1.16f + 0.025f * hit
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
        // One accent bloom behind the sleeve, swelling on the beat instead of strobing.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hit = auraBeatHit(beats, hasGrid, isPlaying)
            val center = Offset(size.width * 0.5f, size.height * 0.27f)
            val radius = size.minDimension * 0.92f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to accent.copy(alpha = 0.10f + 0.07f * hit),
                        0.55f to accent.copy(alpha = 0.04f + 0.03f * hit),
                        1f to Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        // Legibility scrim: heavy at the bottom where the transport lives, light at the top where
        // only the grabber sits, so the artwork is never the thing competing with the text.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.28f),
                            0.44f to Color.Black.copy(alpha = 0.52f),
                            0.78f to Color.Black.copy(alpha = 0.80f),
                            1f to Color.Black.copy(alpha = 0.94f)
                        )
                    )
                }
        )
    }
}

/**
 * The beat envelope, 1 on the beat and fading to 0 across the bar.
 *
 * Shared by everything that pulses so the deck cannot disagree with itself, and deliberately
 * allocation-free besides the `exp`: it is called from draw lambdas on every frame.
 */
private fun auraBeatHit(beats: MutableFloatState, hasGrid: Boolean, isPlaying: Boolean): Float {
    if (!hasGrid || !isPlaying) return 0.12f
    val beat = beats.floatValue
    val phase = beat - floor(beat)
    return exp(-phase * 3.4f)
}

/**
 * The seek bar, as a bar.
 *
 * The old one was a travelling sine wave with a stick dragged through it — two ideas of "where
 * you are" competing on the same line, and neither one comfortably tappable. This is a single
 * thick track: what you have heard is filled with the track's own gradient, what is left is a
 * flat lane, and the head is a round knob that only grows while your finger is on it.
 */
@Composable
private fun AuraProgress(
    fraction: Float,
    enabled: Boolean,
    accentStart: Color,
    accentEnd: Color,
    animated: Boolean,
    onCommit: (Float) -> Unit
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val latestCommit by rememberUpdatedState(onCommit)
    val dragging = dragFraction != null
    val shown = (dragFraction ?: fraction).coerceIn(0f, 1f)
    // Smooths the half-second tick of the playback position into a gliding head instead of a
    // stepping one; a drag bypasses it so the knob stays under the finger.
    val animatedShown by animateFloatAsState(
        targetValue = shown,
        animationSpec = if (animated && !dragging) tween(AURA_PROGRESS_MS) else tween(0),
        label = "aura-progress"
    )
    val head = if (dragging) shown else animatedShown
    val track = remember(accentStart, accentEnd) {
        Brush.horizontalGradient(listOf(accentStart, accentEnd))
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(AuraProgressHeight)
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
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    }
                )
            }
    ) {
        if (size.width <= 0f) return@Canvas
        val barHeight = AuraBarHeight.toPx()
        val top = (size.height - barHeight) / 2f
        val radius = CornerRadius(barHeight / 2f)
        val width = size.width
        val filled = width * head

        // The lane, so the bar is visible before anything has played.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f),
            topLeft = Offset(0f, top),
            size = Size(width, barHeight),
            cornerRadius = radius
        )
        if (filled > 0f) {
            // A wider, fainter copy underneath is the glow — cheaper than a blur and it keeps the
            // neon of the sleeve without turning the control into a light strip.
            drawRoundRect(
                color = accentStart.copy(alpha = 0.22f),
                topLeft = Offset(0f, top - barHeight * 0.35f),
                size = Size(filled, barHeight * 1.7f),
                cornerRadius = CornerRadius(barHeight * 0.85f)
            )
            drawRoundRect(
                brush = track,
                topLeft = Offset(0f, top),
                size = Size(filled.coerceAtLeast(barHeight), barHeight),
                cornerRadius = radius
            )
        }
        // The knob is drawn even at 0:00 — an empty lane with no head reads as a broken control.
        val knobRadius = if (dragging) barHeight * 0.95f else barHeight * 0.70f
        val knobX = filled.coerceIn(knobRadius, width - knobRadius)
        val knobY = size.height / 2f
        drawCircle(
            color = Color.Black.copy(alpha = 0.40f),
            radius = knobRadius + 2.5.dp.toPx(),
            center = Offset(knobX, knobY)
        )
        drawCircle(color = Color.White, radius = knobRadius, center = Offset(knobX, knobY))
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
        val qualityOrCodec = spec.quality ?: spec.codec
        if (qualityOrCodec.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AuraBadge)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = AuraInkSoft, modifier = Modifier.size(12.dp))
                Text(qualityOrCodec, color = AuraInkSoft, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
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
            // The one filled surface in the deck. Everything else is ink on glass; giving the
            // primary action a solid disc is what keeps the eye from having to hunt for it now
            // that the background actually has something in it.
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AuraInk),
                contentAlignment = Alignment.Center
            ) {
                DerbisPlayPauseGlyph(
                    playing = isPlaying,
                    color = AuraOnInk,
                    modifier = Modifier.size(26.dp)
                )
            }
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
