package com.yasuda.derbis.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.zIndex
import com.yasuda.derbis.domain.RepeatMode
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.DerbisLoadingIndicator
import com.yasuda.derbis.ui.components.DerbisPlayPauseGlyph
import com.yasuda.derbis.ui.components.DerbisPressScale
import com.yasuda.derbis.ui.components.PlayerControlLabels
import com.yasuda.derbis.ui.components.PlayerIcon
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisIsPixel
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics

/*
 * Vinyl — the deck whose stage is the queue.
 *
 * One idea: the cover, not the song, is what moves. The track playing sits forward and flat in
 * the centre, its two neighbours are turned away on the Y axis, scaled down and dimmed, and a skip
 * turns the whole row rather than swapping two pictures. The turn is one float — the cards read
 * their position off it — so they move as a wheel instead of three things that happen to animate
 * at once, and with animations off it snaps in one frame like the rest of the player.
 *
 * The name is older than the stage: this was the turntable deck, and the deck is still Vinyl, so a
 * saved choice still lands here.
 *
 * No image assets: every card is the artwork slot every deck already gets, drawn through the same
 * renderer the centre card uses.
 */

private val VinylGround = Color(0xFF08080A)
private val VinylInk = Color(0xFFF3F1EE)

private const val VinylInkStrong = 0.78f
private const val VinylInkMuted = 0.60f
private const val VinylInkSoft = 0.50f
private const val VinylInkFaint = 0.44f

/**
 * The arc.
 *
 * A card's distance `d` from the playhead sets its whole transform: it shifts sideways by
 * `d x 0.58` of a card, turns away by `d x 52°`, shrinks by `|d| x 0.12` and is dimmed by
 * `|d| x 0.26`. One rule for three cards, so they cannot disagree about where the arc is.
 */
private const val VinylArcShift = 0.58f
private const val VinylArcTurnDegrees = 52f
private const val VinylArcScaleFalloff = 0.12f
private const val VinylArcScrim = 0.26f

/** How long the row takes to turn one place. */
private const val VinylTurnMs = 420

/**
 * Camera distance as a multiple of the card's own width.
 *
 * Compose measures this in pixels and defaults to 8, which is smaller than the card and so
 * distorts a 52° turn into a trapezoid; its own guidance is to use at least the size of the
 * view. Five card widths gives the shallow, even perspective the preview was drawn with, and it
 * holds at any card size instead of drifting with screen density.
 */
private const val VinylCameraFactor = 5f

/**
 * Past this distance a card is off the stage, so it is not drawn at all.
 *
 * A turn only ever carries a card between one and two places out, so this is where a neighbour
 * fades in rather than appears: cutting it at a hard edge would drop a sliver of cover mid-turn.
 */
private const val VinylNeighbourLimit = 1.8f

/** Card side as a fraction of the stage width: narrow enough to leave the neighbours room. */
private const val VinylCardWidthFactor = 0.66f

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
    // The row's one degree of freedom. It rests at 0; a track change sets it to +1 or -1 (which
    // way the queue moved) and it settles back, carrying all three cards with it.
    val turn = remember { Animatable(0f) }
    val trackIdentity = track.playlistEntryId.ifBlank { track.id }
    val currentQueueIndex = slots.queue?.index ?: -1
    var settledIdentity by remember { mutableStateOf(trackIdentity) }
    var settledQueueIndex by remember { mutableIntStateOf(currentQueueIndex) }
    val pendingTurn = if (
        settledIdentity != trackIdentity &&
        animated &&
        currentQueueIndex >= 0 &&
        settledQueueIndex >= 0
    ) {
        (currentQueueIndex - settledQueueIndex).coerceIn(-1, 1).toFloat()
    } else {
        0f
    }
    LaunchedEffect(trackIdentity, currentQueueIndex) {
        if (settledIdentity == trackIdentity) {
            settledQueueIndex = currentQueueIndex
            return@LaunchedEffect
        }
        val direction = if (currentQueueIndex >= 0 && settledQueueIndex >= 0) {
            (currentQueueIndex - settledQueueIndex).coerceIn(-1, 1)
        } else {
            0
        }
        if (direction == 0) {
            turn.snapTo(0f)
            settledQueueIndex = currentQueueIndex
            settledIdentity = trackIdentity
            return@LaunchedEffect
        }
        turn.snapTo(direction.toFloat())
        settledQueueIndex = currentQueueIndex
        settledIdentity = trackIdentity
        turn.animateTo(
            targetValue = 0f,
            animationSpec = if (animated) {
                tween(durationMillis = VinylTurnMs, easing = FastOutSlowInEasing)
            } else {
                snap()
            }
        )
    }
    val stageTurn = if (settledIdentity != trackIdentity) pendingTurn else turn.value
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
                contentAlignment = Alignment.Center
            ) {
                // A card takes two thirds of the stage, so the neighbours have somewhere to
                // turn into and the outermost one runs off the edge rather than being framed.
                val side = minOf(maxWidth * VinylCardWidthFactor, maxHeight, artworkCap)
                if (side > 0.dp) {
                    VinylStage(
                        track = track,
                        slots = slots,
                        side = side,
                        turn = stageTurn
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
            slots.error()
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
        // The artwork's light, low behind the row: two washes, no blur, no image.
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
 * The stage is the queue.
 *
 * The current cover sits forward and flat; the two beside it are turned away on the Y axis,
 * scaled down and dimmed. Pressing next or previous does not swap two pictures — the whole row
 * turns, and the card that lands in the centre straightens up.
 *
 * The turn is one float, not one animation per card. On a track change it starts at ±1 and
 * settles to 0 over [VinylTurnMs], and every card reads its position off that single value, so
 * the three of them cannot drift apart the way three independent tweens would. With animations
 * off it snaps in one frame, which is the same rule the rest of the player follows.
 */
@Composable
private fun VinylStage(
    track: Track,
    slots: PlayerDeckSlots,
    side: Dp,
    turn: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sidePx = with(density) { side.toPx() }
    // Measured against the card, not the screen: Compose takes this in pixels, and anything
    // under the card's own width fans a turned cover into a trapezoid.
    val cameraDistance = sidePx * VinylCameraFactor
    val corner = if (DerbisIsPixel) 0.dp else VinylSleeveCorner
    val queue = slots.queue

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Drawn back to front: the neighbours first, then the card in the playhead, so the centre
        // one is never cut by a neighbour that happens to be nearer during a turn.
        queue?.previous?.let { previous ->
            key(previous.playlistEntryId.ifBlank { previous.id }) {
                VinylNeighbour(
                    track = previous,
                    slots = slots,
                    side = side,
                    distance = -1f + turn,
                    cameraDistance = cameraDistance,
                    corner = corner
                )
            }
        }
        queue?.next?.let { next ->
            key(next.playlistEntryId.ifBlank { next.id }) {
                VinylNeighbour(
                    track = next,
                    slots = slots,
                    side = side,
                    distance = 1f + turn,
                    cameraDistance = cameraDistance,
                    corner = corner
                )
            }
        }
        Box(
            modifier = Modifier
                .size(side)
                .zIndex(1f)
                .graphicsLayer {
                    translationX = turn * VinylArcShift * sidePx
                    rotationY = -turn * VinylArcTurnDegrees
                    scaleX = 1f - kotlin.math.abs(turn) * VinylArcScaleFalloff
                    scaleY = scaleX
                    this.cameraDistance = cameraDistance
                }
        ) {
            slots.stage(track, Modifier.size(side), corner)
            slots.gestures(track, Modifier.size(side))
        }
    }
}

/** One card beside the playhead: same artwork loader, transformed by its distance from centre. */
@Composable
private fun VinylNeighbour(
    track: Track,
    slots: PlayerDeckSlots,
    side: Dp,
    distance: Float,
    cameraDistance: Float,
    corner: Dp
) {
    val away = kotlin.math.abs(distance)
    // Given up on once it has travelled far enough out to be off the stage: drawing it there
    // costs a cover decode for nothing.
    if (away > VinylNeighbourLimit) return
    val density = LocalDensity.current
    val sidePx = with(density) { side.toPx() }
    val scrim = away * VinylArcScrim
    // Full strength at rest, gone by the limit, and ramping in between so a card arriving from
    // two places out does not appear as a hard edge on the way in.
    val presence = ((VinylNeighbourLimit - away) / (VinylNeighbourLimit - 1f)).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .size(side)
            .graphicsLayer {
                translationX = distance * VinylArcShift * sidePx
                rotationY = -distance * VinylArcTurnDegrees
                scaleX = 1f - away * VinylArcScaleFalloff
                scaleY = scaleX
                alpha = presence
                this.cameraDistance = cameraDistance
            }
    ) {
        slots.cover(track, side, corner)
        if (scrim > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = scrim))
            )
        }
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
