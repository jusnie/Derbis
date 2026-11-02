package com.yasuda.derbis.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.RepeatMode
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.DerbisLoadingIndicator
import com.yasuda.derbis.ui.components.DerbisPlayPauseGlyph
import com.yasuda.derbis.ui.components.DerbisPressScale
import com.yasuda.derbis.ui.components.PlayerControlLabels
import com.yasuda.derbis.ui.components.PlayerIcon
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics

/*
 * Vinyl — the deck whose stage is a turntable.
 *
 * One idea: the sleeve holds the record. A square cover sits on the left, the disc slides out from
 * behind it while the track plays and glides back in when it pauses, and the disc's label is
 * pressed from the artwork's own two accents. The disc turns with the playhead at a 33⅓ rpm
 * relationship, so scrubbing lands it in the same place every time, and every move is a tween that
 * snaps when animations are off.
 *
 * No image assets: the sleeve is the artwork slot every deck already gets, and the disc, grooves,
 * label and spindle are drawn.
 */

private val VinylGround = Color(0xFF08080A)
private val VinylInk = Color(0xFFF3F1EE)
private val VinylDiscCore = Color(0xFF0B0B0D)
private val VinylDiscEdge = Color(0xFF17171A)

private const val VinylInkStrong = 0.78f
private const val VinylInkMuted = 0.60f
private const val VinylInkSoft = 0.50f
private const val VinylInkFaint = 0.44f

/** Disc diameter against the sleeve side. */
private const val VinylDiscRatio = 0.94f

/** How far the record travels out of its sleeve, against the sleeve side. */
private const val VinylSlideRatio = 0.42f

/** Stage width per sleeve side, so the disc's overhang is never clipped. */
private const val VinylStageWidthFactor = 1.42f

/** Degrees the disc turns per second of playback: 33⅓ rpm. */
private const val VinylDegreesPerMs = 0.2f

private val VinylSleeveCorner = 4.dp
private val VinylPanelCorner = 24.dp
private val VinylSeekHeight = 5.dp
private val VinylSeekThumb = 11.dp
private val VinylPlaySize = 56.dp
private val VinylPlaySizeCompact = 48.dp
private val VinylControlSize = 46.dp
private val VinylGlyphSize = 21.dp
private val VinylModeGlyphSize = 18.dp
private val VinylScrollStage = 240.dp

@Composable
internal fun PlayerVinylDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    playback: PlayerDeckPlayback,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    animated: Boolean,
    compact: Boolean,
    scrollable: Boolean,
    gutter: Dp,
    artworkCap: Dp,
    onArtistClick: (Int) -> Unit,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val glow = surfaces.glow
    val accent = if (glow.alpha > 0f) glow.copy(alpha = 1f) else surfaces.hero
    val discOut by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = if (animated) {
            tween(durationMillis = 520, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "vinyl-disc-out"
    )
    Box(modifier = modifier.background(VinylGround)) {
        VinylBackdrop(accent = accent)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(
                    start = gutter,
                    end = gutter,
                    top = DerbisPlayerDesign.SpaceXs,
                    bottom = if (compact) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceXl
                )
        ) {
            slots.header()
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceSm))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (scrollable) Modifier.height(VinylScrollStage)
                        else Modifier.weight(1f)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                val side = minOf(maxWidth / VinylStageWidthFactor, maxHeight, artworkCap)
                if (side > 0.dp) {
                    VinylStage(
                        track = track,
                        slots = slots,
                        playback = playback,
                        accent = accent,
                        side = side,
                        discOut = discOut
                    )
                }
            }
            Spacer(Modifier.height(if (compact) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceLg))
            VinylPanel(
                track = track,
                playback = playback,
                accent = accent,
                isPlaying = isPlaying,
                isResolving = isResolving,
                shuffleOn = shuffleOn,
                repeatMode = repeatMode,
                labels = labels,
                compact = compact,
                animated = animated,
                onArtistClick = onArtistClick,
                onShuffle = onShuffle,
                onPrevious = onPrevious,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onRepeat = onRepeat,
                modifier = headlineModifier
            )
            Spacer(Modifier.height(if (compact) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceLg))
            VinylLyricsStrip(
                track = track,
                playback = playback,
                dockActions = slots.dockActions(track)
            )
        }
    }
}

@Composable
private fun VinylBackdrop(accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(VinylGround)
        // The artwork's light, low behind the sleeve: two washes, no blur, no image.
        drawRect(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.42f), Color.Transparent),
                center = Offset(size.width * 0.20f, size.height * 0.18f),
                radius = size.minDimension * 1.15f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * 0.86f, size.height * 0.30f),
                radius = size.minDimension * 0.95f
            )
        )
        drawRect(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, VinylGround.copy(alpha = 0.92f)),
                startY = size.height * 0.40f,
                endY = size.height
            )
        )
    }
}

/**
 * The turntable: the sleeve is the artwork slot, the record is drawn behind it, and the record's
 * centre slides right as [discOut] goes from rest to playing.
 */
@Composable
private fun VinylStage(
    track: Track,
    slots: PlayerDeckSlots,
    playback: PlayerDeckPlayback,
    accent: Color,
    side: Dp,
    discOut: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(side),
        contentAlignment = Alignment.CenterStart
    ) {
        VinylDisc(
            accent = accent,
            playback = playback,
            side = side,
            discOut = discOut,
            modifier = Modifier.fillMaxSize()
        )
        slots.stage(track, Modifier.size(side), VinylSleeveCorner)
        slots.gestures(track, Modifier.size(side))
    }
}

@Composable
private fun VinylDisc(
    accent: Color,
    playback: PlayerDeckPlayback,
    side: Dp,
    discOut: Float,
    modifier: Modifier = Modifier
) {
    // 33⅓ rpm, read off the playhead: a seek lands the disc in the same place every time.
    val angle = (playback.positionMs * VinylDegreesPerMs) % 360f
    Canvas(modifier = modifier) {
        val sidePx = side.toPx()
        val diameter = sidePx * VinylDiscRatio
        val radius = diameter / 2f
        val centre = Offset(
            x = sidePx * 0.5f + sidePx * VinylSlideRatio * discOut,
            y = size.height / 2f
        )
        // A record has weight: a soft drop under it, then the vinyl itself.
        drawCircle(
            color = Color.Black.copy(alpha = 0.42f),
            radius = radius * 1.01f,
            center = centre + Offset(0f, 7.dp.toPx())
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(VinylDiscEdge, VinylDiscCore),
                center = centre + Offset(-radius * 0.34f, -radius * 0.38f),
                radius = radius * 1.5f
            ),
            radius = radius,
            center = centre
        )
        rotate(degrees = angle, pivot = centre) {
            // Grooves: rings, not a texture, so they stay crisp at any density.
            val grooveStroke = 0.7.dp.toPx()
            var groove = radius * 0.34f
            var index = 0
            while (groove < radius * 0.985f) {
                drawCircle(
                    color = Color.White.copy(alpha = if (index % 3 == 0) 0.055f else 0.032f),
                    radius = groove,
                    center = centre,
                    style = Stroke(width = grooveStroke)
                )
                groove += radius * 0.028f
                index++
            }
            // One sheen, so the disc reads as turning even on a still frame.
            drawCircle(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.08f to Color.White.copy(alpha = 0.085f),
                    0.20f to Color.Transparent,
                    0.52f to Color.Transparent,
                    0.58f to Color.White.copy(alpha = 0.045f),
                    0.70f to Color.Transparent,
                    1f to Color.Transparent,
                    center = centre
                ),
                radius = radius * 0.985f,
                center = centre
            )
        }
        // The pressed label: the artwork's two accents, mixed down so it reads as ink on vinyl.
        val labelRadius = radius * 0.31f
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(accent, accent.copy(alpha = 0.62f)),
                start = centre - Offset(labelRadius, labelRadius),
                end = centre + Offset(labelRadius, labelRadius)
            ),
            radius = labelRadius,
            center = centre
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.30f),
            radius = labelRadius,
            center = centre,
            style = Stroke(width = 1.dp.toPx())
        )
        // Spindle.
        drawCircle(color = VinylGround, radius = radius * 0.032f, center = centre)
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = radius * 0.032f,
            center = centre,
            style = Stroke(width = 0.8.dp.toPx())
        )
    }
}

@Composable
private fun VinylPanel(
    track: Track,
    playback: PlayerDeckPlayback,
    accent: Color,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    compact: Boolean,
    animated: Boolean,
    onArtistClick: (Int) -> Unit,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        VinylHeadline(
            track = track,
            compact = compact,
            animated = animated,
            onArtistClick = onArtistClick
        )
        Spacer(Modifier.height(if (compact) DerbisPlayerDesign.SpaceSm else DerbisPlayerDesign.SpaceMd))
        VinylSeek(playback = playback, accent = accent)
        Spacer(Modifier.height(DerbisPlayerDesign.SpaceXxs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VinylTime(formatOneTime(playback.positionMs))
            VinylTime(if (playback.hasDuration) formatOneTime(playback.durationMs) else "--:--")
        }
        Spacer(Modifier.height(if (compact) DerbisPlayerDesign.SpaceSm else DerbisPlayerDesign.SpaceMd))
        VinylTransport(
            isPlaying = isPlaying,
            isResolving = isResolving,
            shuffleOn = shuffleOn,
            repeatMode = repeatMode,
            labels = labels,
            compact = compact,
            onShuffle = onShuffle,
            onPrevious = onPrevious,
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onRepeat = onRepeat
        )
    }
}

@Composable
private fun VinylHeadline(
    track: Track,
    compact: Boolean,
    animated: Boolean,
    onArtistClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = track,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { DerbisMotion.contentSwap(animated) },
        contentKey = { it.id },
        label = "player-vinyl-headline"
    ) { shown ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = shown.title,
                color = VinylInk,
                fontSize = if (compact) 19.sp else 22.sp,
                lineHeight = (if (compact) 19.sp else 22.sp) * 1.16f,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (shown.artist.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = shown.artist.uppercase(),
                    color = VinylInk.copy(alpha = VinylInkMuted),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .derbisPressable(
                            onClick = { onArtistClick(0) },
                            pressedScale = DerbisPressScale.Row,
                            role = Role.Button
                        )
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun VinylSeek(
    playback: PlayerDeckPlayback,
    accent: Color,
    modifier: Modifier = Modifier
) {
    var scrub by remember { mutableStateOf<Float?>(null) }
    val liveFraction by rememberUpdatedState(playback.fraction())
    val fraction = (scrub ?: liveFraction).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 26.dp)
            .pointerInput(playback.hasDuration) {
                if (!playback.hasDuration) return@pointerInput
                detectTapGestures { offset ->
                    playback.onSeek((offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f))
                }
            }
            .pointerInput(playback.hasDuration) {
                if (!playback.hasDuration) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        scrub = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        scrub?.let(playback.onSeek)
                        scrub = null
                    },
                    onDragCancel = { scrub = null }
                ) { change, _ ->
                    change.consume()
                    scrub = (change.position.x / size.width.toFloat().coerceAtLeast(1f))
                        .coerceIn(0f, 1f)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(VinylSeekHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(VinylSeekHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Brush.horizontalGradient(colors = listOf(accent, Color.White)))
            )
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(VinylSeekHeight)) {
            val x = size.width * fraction
            drawCircle(
                color = accent.copy(alpha = 0.30f),
                radius = (VinylSeekThumb / 2 + 3.dp).toPx(),
                center = Offset(x, size.height / 2f)
            )
            drawCircle(
                color = Color.White,
                radius = (VinylSeekThumb / 2).toPx(),
                center = Offset(x, size.height / 2f)
            )
        }
    }
}

@Composable
private fun VinylTime(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = VinylInk.copy(alpha = VinylInkSoft),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.6.sp,
        style = VinylTabular,
        maxLines = 1,
        modifier = modifier
    )
}

private val VinylTabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun VinylTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    compact: Boolean,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VinylModeButton(
            icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            label = labels.repeat,
            active = repeatMode != RepeatMode.Off,
            onClick = onRepeat
        )
        VinylIconButton(
            icon = Icons.Rounded.SkipPrevious,
            label = labels.previous,
            onClick = onPrevious
        )
        VinylPlayButton(
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            compact = compact,
            onClick = onTogglePlay
        )
        VinylIconButton(
            icon = Icons.Rounded.SkipNext,
            label = labels.next,
            onClick = onNext
        )
        VinylModeButton(
            icon = Icons.Rounded.Shuffle,
            label = labels.shuffle,
            active = shuffleOn,
            onClick = onShuffle
        )
    }
}

@Composable
private fun VinylModeButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    Box(
        modifier = Modifier
            .size(VinylControlSize)
            .clip(CircleShape)
            .derbisPressable(
                onClick = {
                    haptics.perform(DerbisHapticAction.Transport)
                    onClick()
                },
                pressedScale = DerbisPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(
            icon = icon,
            tint = VinylInk.copy(alpha = if (active) VinylInkStrong else VinylInkFaint),
            modifier = Modifier.size(VinylModeGlyphSize)
        )
    }
}

@Composable
private fun VinylIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    Box(
        modifier = Modifier
            .size(VinylControlSize)
            .clip(CircleShape)
            .derbisPressable(
                onClick = {
                    haptics.perform(DerbisHapticAction.Transport)
                    onClick()
                },
                pressedScale = DerbisPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(
            icon = icon,
            tint = VinylInk.copy(alpha = 0.88f),
            modifier = Modifier.size(VinylGlyphSize)
        )
    }
}

@Composable
private fun VinylPlayButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    compact: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val label = if (isPlaying) labels.pause else labels.play
    val size = if (compact) VinylPlaySizeCompact else VinylPlaySize
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.94f))
            .derbisPressable(
                onClick = {
                    haptics.perform(DerbisHapticAction.Transport)
                    onClick()
                },
                interactionSource = interaction,
                pressedScale = DerbisPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            DerbisLoadingIndicator(color = VinylGround, modifier = Modifier.size(size * 0.36f))
        } else {
            DerbisPlayPauseGlyph(
                playing = isPlaying,
                color = VinylGround,
                modifier = Modifier.size(if (compact) 18.dp else 21.dp)
            )
        }
    }
}

@Composable
private fun VinylLyricsStrip(
    track: Track,
    playback: PlayerDeckPlayback,
    dockActions: List<PlayerDockAction>,
    modifier: Modifier = Modifier
) {
    val line = playerInlineLyricsText(playback.activeLyric, playback.lyricsLabel)
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.10f))
        )
        Spacer(Modifier.height(DerbisPlayerDesign.SpaceMd))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = playback.lyricsLabel.uppercase(),
                color = VinylInk.copy(alpha = VinylInkFaint),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.9.sp,
                maxLines = 1
            )
            VinylDockRow(actions = dockActions)
        }
        if (line.isNotEmpty()) {
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceSm))
            Text(
                text = line,
                color = VinylInk.copy(alpha = VinylInkStrong),
                fontSize = 12.5.sp,
                lineHeight = 12.5.sp * 1.6f,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .derbisPressable(
                        onClick = playback.onOpenLyrics,
                        pressedScale = DerbisPressScale.Row,
                        role = Role.Button
                    )
                    .semantics { contentDescription = "${playback.lyricsLabel}: $line" }
            )
        } else if (track.title.isNotBlank()) {
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceSm))
            Text(
                text = playback.lyricsLabel,
                color = VinylInk.copy(alpha = VinylInkFaint),
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .derbisPressable(
                        onClick = playback.onOpenLyrics,
                        pressedScale = DerbisPressScale.Row,
                        role = Role.Button
                    )
                    .semantics { contentDescription = playback.lyricsLabel }
            )
        }
    }
}

@Composable
private fun VinylDockRow(actions: List<PlayerDockAction>) {
    if (actions.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .derbisPressable(
                        onClick = action.onClick,
                        enabled = action.enabled,
                        pressedScale = DerbisPressScale.Control,
                        role = Role.Button
                    )
                    .semantics { contentDescription = action.label },
                contentAlignment = Alignment.Center
            ) {
                if (action.busy) {
                    DerbisLoadingIndicator(
                        color = VinylInk.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                } else {
                    PlayerIcon(
                        icon = action.icon,
                        tint = VinylInk.copy(alpha = if (action.active) 0.85f else VinylInkFaint),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
