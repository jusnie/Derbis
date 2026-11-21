package com.yasuda.derbis.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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
 * `d x 0.56` of a card, turns away smoothly up to `48°`, shrinks by `|d| x 0.14` and is dimmed
 * along a smooth curve. One rule for every visible cover so the row moves as a single wheel.
 */
private const val VinylArcShift = 0.56f
private const val VinylArcTurnDegrees = 48f
private const val VinylArcScaleFalloff = 0.14f
private const val VinylArcScrim = 0.30f

/** Spring tuning for settling the cover wheel smoothly without harsh snap or bounce. */
private const val VinylSpringDamping = 0.88f
private const val VinylSpringStiffness = 230f
private const val VinylBackdropColorMs = 560
private const val VinylHeroSettleThreshold = 0.018f

/**
 * Camera distance as a multiple of the card's own width.
 *
 * Compose measures this in pixels and defaults to 8, which is smaller than the card and so
 * distorts a 48° turn into a trapezoid; its own guidance is to use at least the size of the
 * view. 5.5 card widths gives a clean, balanced perspective at every screen density.
 */
private const val VinylCameraFactor = 5.5f

/**
 * Past this distance a card is off the stage, so it is not drawn at all.
 *
 * Keeping two neighbours on each side in the queue window (`farPrevious` / `farNext`) lets the
 * outer card glide smoothly to `±1.85` and fade away instead of disappearing on the first frame.
 */
private const val VinylNeighbourLimit = 1.85f

/**
 * Full-bleed stage card sizing: the carousel sits outside horizontal panel gutters so the hero
 * sleeve has generous presence in the centre while angled neighbours still frame both edges.
 */
private const val VinylCardWidthFactor = 0.70f
private const val VinylCardWidthFactorCompact = 0.66f
private val VinylStageVerticalInset = 10.dp

private val VinylSleeveCorner = 14.dp
private val VinylPanelCorner = 24.dp
private val VinylSeekHeight = 5.dp
private val VinylSeekThumb = 11.dp
private val VinylPlaySize = 56.dp
private val VinylPlaySizeCompact = 48.dp
private val VinylControlSize = 46.dp
private val VinylGlyphSize = 21.dp
private val VinylModeGlyphSize = 18.dp
private val VinylScrollStage = 300.dp

@Immutable
private data class VinylArcSlot(
    val key: String,
    val track: Track,
    val slot: Int
)

internal fun vinylTrackStepDirection(
    previousQueueIndex: Int,
    currentQueueIndex: Int,
    queueTotal: Int
): Int {
    if (previousQueueIndex < 0 || currentQueueIndex < 0 || previousQueueIndex == currentQueueIndex) {
        return 0
    }
    if (queueTotal > 2) {
        if (previousQueueIndex == queueTotal - 1 && currentQueueIndex == 0) return 1
        if (previousQueueIndex == 0 && currentQueueIndex == queueTotal - 1) return -1
    }
    return (currentQueueIndex - previousQueueIndex).coerceIn(-1, 1)
}

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
    headlineModifier: Modifier = Modifier,
    swipeOffsetPx: () -> Float = { 0f }
) {
    val glow = surfaces.glow
    val rawAccent = if (glow.alpha > 0f) glow.copy(alpha = 1f) else surfaces.hero
    val accent by animateColorAsState(
        targetValue = rawAccent,
        animationSpec = if (animated) {
            tween(durationMillis = VinylBackdropColorMs, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "player-vinyl-accent"
    )
    val turn = remember { Animatable(0f) }
    var stageDragSpanPx by remember { mutableFloatStateOf(1f) }
    var lastCommittedSwipeTurn by remember { mutableFloatStateOf(0f) }
    val liveSwipePx = swipeOffsetPx()
    val liveSwipeTurn = if (stageDragSpanPx > 1f) {
        val raw = liveSwipePx / stageDragSpanPx
        val hasPrev = slots.queue?.previous != null
        val hasNext = slots.queue?.next != null
        val damped = when {
            raw > 0f && !hasPrev -> raw * 0.22f
            raw < 0f && !hasNext -> raw * 0.22f
            else -> raw
        }
        damped.coerceIn(-1.15f, 1.15f)
    } else {
        0f
    }
    val trackIdentity = track.playlistEntryId.ifBlank { track.id }
    val currentQueueIndex = slots.queue?.index ?: -1
    val queueTotal = slots.queue?.total ?: 0
    var settledTrack by remember { mutableStateOf(track) }
    var settledIdentity by remember { mutableStateOf(trackIdentity) }
    var settledQueueIndex by remember { mutableIntStateOf(currentQueueIndex) }
    var outgoingGhost by remember { mutableStateOf<VinylArcSlot?>(null) }

    if (settledIdentity == trackIdentity && liveSwipeTurn != 0f) {
        lastCommittedSwipeTurn = liveSwipeTurn
    } else if (settledIdentity == trackIdentity && liveSwipePx == 0f) {
        lastCommittedSwipeTurn = 0f
    }

    val stepDirection = if (settledIdentity != trackIdentity) {
        vinylTrackStepDirection(settledQueueIndex, currentQueueIndex, queueTotal)
            .let { if (it != 0) it else 1 }
    } else {
        0
    }
    val stageTurn = if (settledIdentity != trackIdentity) {
        if (animated) {
            (turn.value + lastCommittedSwipeTurn + stepDirection.toFloat()).coerceIn(-1.75f, 1.75f)
        } else {
            0f
        }
    } else {
        turn.value + liveSwipeTurn
    }

    LaunchedEffect(trackIdentity, currentQueueIndex) {
        if (settledIdentity == trackIdentity) {
            settledQueueIndex = currentQueueIndex
            return@LaunchedEffect
        }
        val direction = vinylTrackStepDirection(settledQueueIndex, currentQueueIndex, queueTotal)
            .let { if (it != 0) it else 1 }
        val startTurn = (turn.value + lastCommittedSwipeTurn + direction.toFloat()).coerceIn(-1.75f, 1.75f)
        val outgoing = settledTrack
        val outgoingKey = settledIdentity
        lastCommittedSwipeTurn = 0f
        settledTrack = track
        settledIdentity = trackIdentity
        settledQueueIndex = currentQueueIndex
        if (!animated) {
            outgoingGhost = null
            turn.snapTo(0f)
            return@LaunchedEffect
        }
        outgoingGhost = VinylArcSlot(
            key = outgoingKey,
            track = outgoing,
            slot = -direction
        )
        turn.snapTo(startTurn)
        turn.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = VinylSpringDamping,
                stiffness = VinylSpringStiffness
            )
        )
        outgoingGhost = null
    }

    Box(modifier = modifier.background(VinylGround)) {
        VinylBackdrop(accent = accent)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(
                    top = DerbisPlayerDesign.SpaceXs,
                    bottom = if (compact) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceXl
                )
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = gutter)) {
                slots.header()
            }
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
                val widthFactor = if (compact) VinylCardWidthFactorCompact else VinylCardWidthFactor
                val availableHeight = (maxHeight - VinylStageVerticalInset * 2).coerceAtLeast(0.dp)
                val side = minOf(maxWidth * widthFactor, availableHeight, artworkCap)
                if (side > 0.dp) {
                    val density = LocalDensity.current
                    val spanPx = with(density) { (side * 0.78f).toPx() }.coerceAtLeast(1f)
                    if (stageDragSpanPx != spanPx) stageDragSpanPx = spanPx
                    VinylStage(
                        track = track,
                        slots = slots,
                        side = side,
                        turn = stageTurn,
                        outgoingGhost = outgoingGhost,
                        animated = animated,
                        onSelectNeighbour = { slot ->
                            if (slot < 0) onPrevious() else if (slot > 0) onNext()
                        }
                    )
                }
            }
            Spacer(Modifier.height(if (compact) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceLg))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = gutter)
            ) {
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
 * Every visible sleeve (`farPrevious`, `previous`, `current`, `next`, `farNext`) lives in the same
 * keyed composition loop so stepping forward or backward moves the existing card nodes continuously
 * along the 3D arc — no bitmap reload, no cross-fade between two songs on the same card, and no
 * z-order pop. Once the wheel settles in the centre, [PlayerDeckSlots.stage] fades in seamlessly at
 * zero offset so Canvas motion artwork, Lyrics Flip and the mini-player morph anchor remain active.
 */
@Composable
private fun VinylStage(
    track: Track,
    slots: PlayerDeckSlots,
    side: Dp,
    turn: Float,
    outgoingGhost: VinylArcSlot?,
    animated: Boolean,
    onSelectNeighbour: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sidePx = with(density) { side.toPx() }.coerceAtLeast(1f)
    val cameraDistance = sidePx * VinylCameraFactor
    val corner = if (DerbisIsPixel) 0.dp else VinylSleeveCorner
    val queue = slots.queue
    val currentKey = track.playlistEntryId.ifBlank { track.id }
    val cards = remember(queue, track.id, track.playlistEntryId, outgoingGhost) {
        buildList {
            val seen = HashSet<String>(6)
            fun append(candidate: Track?, slot: Int) {
                if (candidate == null) return
                val candidateKey = candidate.playlistEntryId.ifBlank { candidate.id }
                if (seen.add(candidateKey)) {
                    add(VinylArcSlot(key = candidateKey, track = candidate, slot = slot))
                }
            }
            append(queue?.farPrevious, -2)
            append(queue?.previous, -1)
            append(track, 0)
            append(queue?.next, 1)
            append(queue?.farNext, 2)
            if (outgoingGhost != null && seen.add(outgoingGhost.key)) {
                add(outgoingGhost)
            }
        }
    }

    val settled = abs(turn) < VinylHeroSettleThreshold
    val heroOverlayAlpha by animateFloatAsState(
        targetValue = if (settled) 1f else 0f,
        animationSpec = if (animated) {
            tween(durationMillis = if (settled) 150 else 60, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "vinyl-hero-overlay-alpha"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        for (entry in cards) {
            key(entry.key) {
                val distance = entry.slot.toFloat() + turn
                VinylArcCard(
                    track = entry.track,
                    slots = slots,
                    side = side,
                    sidePx = sidePx,
                    distance = distance,
                    cameraDistance = cameraDistance,
                    corner = corner,
                    isNeighbour = entry.key != currentKey && (entry.slot == -1 || entry.slot == 1),
                    onTapNeighbour = { onSelectNeighbour(entry.slot) }
                )
            }
        }

        // Settled centre stage: keeps Canvas motion, Lyrics Flip and mini-player morph anchor
        // working at rest while staying invisible whenever the 3D cover wheel is turning.
        Box(
            modifier = Modifier
                .size(side)
                .zIndex(3f)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = heroOverlayAlpha
                        this.cameraDistance = cameraDistance
                    }
            ) {
                slots.stage(track, Modifier.size(side), corner)
            }
            slots.gestures(track, Modifier.size(side))
        }
    }
}

/** One sleeve on the 3D Cover Flow arc, continuously positioned from [distance]. */
@Composable
private fun VinylArcCard(
    track: Track,
    slots: PlayerDeckSlots,
    side: Dp,
    sidePx: Float,
    distance: Float,
    cameraDistance: Float,
    corner: Dp,
    isNeighbour: Boolean,
    onTapNeighbour: () -> Unit
) {
    val away = abs(distance)
    if (away > VinylNeighbourLimit) return
    // Smooth eased tilt near the playhead so landing in the centre settles softly to 0° instead
    // of hinging abruptly across the origin.
    val clampedUnit = distance.coerceIn(-1f, 1f)
    val easedAngleFactor = 0.65f * sin(clampedUnit * (PI.toFloat() / 2f)) + 0.35f * distance.coerceIn(-1.6f, 1.6f)
    val scrimProgress = away.coerceIn(0f, 1.4f)
    val scrim = (scrimProgress * scrimProgress / (1.4f * 1.4f).coerceAtLeast(1f) * 0.45f + away * VinylArcScrim * 0.65f)
        .coerceIn(0f, 0.72f)
    val presence = ((VinylNeighbourLimit - away) / (VinylNeighbourLimit - 1f)).coerceIn(0f, 1f)
    val easedPresence = presence * presence * (3f - 2f * presence)
    val cardScale = (1f - away * VinylArcScaleFalloff).coerceAtLeast(0.68f)

    Box(
        modifier = Modifier
            .size(side)
            .zIndex(2f - away)
            .graphicsLayer {
                translationX = distance * VinylArcShift * sidePx
                rotationY = -easedAngleFactor * VinylArcTurnDegrees
                scaleX = cardScale
                scaleY = cardScale
                alpha = easedPresence
                this.cameraDistance = cameraDistance
            }
            .then(
                if (isNeighbour) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { onTapNeighbour() })
                    }
                } else {
                    Modifier
                }
            )
    ) {
        slots.cover(track, side, corner)
        if (scrim > 0.004f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(corner))
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
