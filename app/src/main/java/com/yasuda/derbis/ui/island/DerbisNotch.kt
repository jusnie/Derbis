package com.yasuda.derbis.ui.island

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yasuda.derbis.domain.DerbisNotchSettings
import com.yasuda.derbis.domain.DerbisNotchStyle
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.ui.LocalAnimationsEnabled
import com.yasuda.derbis.ui.components.formatSeekbarMillis
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.player.PlayerDragEvent
import com.yasuda.derbis.ui.player.PlayerSwipeResult
import com.yasuda.derbis.ui.player.playerAxisDragGestures
import com.yasuda.derbis.ui.player.rememberPlayerSwipeMotion
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics

/**
 * Everything the notch draws, flattened off the player state.
 *
 * The island is handed data, never a Track: the customize screen shows the same composable it is
 * configuring, without owning a player.
 */
internal data class NotchPreview(
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val lyrics: List<LyricLine>,
    /** False when the provider returned plain text: the sheet still lists lines, nothing highlights. */
    val lyricsSynced: Boolean = true,
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean
)

/**
 * Derbis Notch — the in-app dynamic island for the current track.
 *
 * Collapsed it is a black capsule hugging the status bar. Tapping it grows into a card: artwork,
 * title and artist on the left, the synced lyric sheet on the right, over a blurred copy of the
 * artwork. A hairline progress bar runs along the bottom edge, Apple style — no ring, no pill, just
 * a straight line. Nothing here needs a system overlay permission, and every part of it can be
 * turned off from its customize screen.
 *
 * Motion is part of the material, not decoration: the capsule and the card are one surface that
 * morphs (width, height and corner radius on the same spring), the blurred cover blooms behind it,
 * the two layouts cross-fade instead of swapping, and a horizontal swipe on the collapsed capsule
 * skips the track with the same carry/settle motion the mini player uses. Everything respects the
 * app's animations switch — off means snap, never a half-animated island.
 */
/** Distance below the status bar, and from the screen edge when the island is not centred. */
internal const val NotchBaseTopDp = 4
internal const val NotchSideInsetDp = 12
internal const val NotchMaxOffsetYDp = 28

private val NotchInk = Color(0xFF0A0A0C)
private val NotchText = Color.White
private val NotchMuted = Color.White.copy(alpha = 0.62f)
private val NotchPill = Color(0xFF050506)
private val NotchSpicy = Color(0xFFFF2D95)
private val NotchWave = Color(0xFF7C4DFF)

/** Width damping of the morph. Slightly under 1 so the island settles instead of gliding to a stop. */
private const val NotchMorphDamping = 0.76f
private const val NotchMorphStiffness = 320f

@Composable
internal fun DerbisMusicNotch(
    preview: NotchPreview,
    settings: DerbisNotchSettings,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    /** False in the customize screen: the island renders, but nothing inside it reacts. */
    interactive: Boolean = true,
    /** The customize screen drives this itself so it can show both states side by side of the toggles. */
    startExpanded: Boolean = false
) {
    var expanded by rememberSaveable(preview.title, startExpanded) { mutableStateOf(startExpanded) }
    val animated = LocalAnimationsEnabled.current
    val haptics = LocalDerbisHaptics.current
    val activeIndex = remember(preview.lyrics, preview.positionMs, preview.lyricsSynced) {
        if (!preview.lyricsSynced) {
            -1
        } else {
            var found = -1
            for (index in preview.lyrics.indices) {
                if (preview.lyrics[index].startMs <= preview.positionMs) found = index else break
            }
            found
        }
    }

    // One spring for the whole morph: the capsule widens, the corners relax and the content
    // cross-fades on the same curve, so it reads as one surface opening rather than three
    // separate animations.
    val morphSpring = if (expanded) DerbisMotion.expand else DerbisMotion.collapse
    val corner by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (expanded) 30.dp else 19.dp,
        animationSpec = DerbisMotion.physics(animated, morphSpring),
        label = "notch-corner"
    )
    val backdrop by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = DerbisMotion.physics(animated, morphSpring),
        label = "notch-backdrop"
    )
    val rim by animateFloatAsState(
        targetValue = if (expanded) 0.10f else 0.06f,
        animationSpec = DerbisMotion.physics(animated, morphSpring),
        label = "notch-rim"
    )

    val shape = RoundedCornerShape(corner)
    // A 356dp card is wider than a small phone's screen; clamp it so the island never runs off
    // the edge on a 320-360dp device.
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val expandedWidth = if (settings.compactCard) 248.dp else 356.dp
    val islandWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (expanded) {
            minOf(expandedWidth, screenWidth - 32.dp)
        } else {
            minOf(if (settings.showArtwork) 168.dp else 132.dp, screenWidth - 32.dp)
        },
        animationSpec = DerbisMotion.physics(animated, morphSpring),
        label = "notch-width"
    )

    // Swipe-to-skip lives on the collapsed capsule only: the expanded card owns vertical scrolling
    // for the lyric sheet and inside it there are real transport buttons. It reuses the same drag
    // engine and settle curve as the mini player, so a swipe feels identical everywhere.
    val swipe = rememberPlayerSwipeMotion(animated)
    val swipeEnabled = interactive && !expanded && settings.showTransport
    val swipeOffset = { swipe.offsetFor(preview.title) }
    val swipeModifier = if (swipeEnabled) {
        Modifier.playerAxisDragGestures(
            key = preview.title,
            enabled = true,
            rightToLeft = false,
            edgeZonesEnabled = false,
            verticalEnabled = false
        ) { event ->
            when (event) {
                is PlayerDragEvent.HorizontalOffset -> swipe.follow(preview.title, event.offsetPx)
                is PlayerDragEvent.HorizontalSettled -> {
                    val committed = event.result != PlayerSwipeResult.Settle
                    if (committed) {
                        if (settings.haptics) haptics.perform(DerbisHapticAction.TrackSwipe)
                        when (event.result) {
                            PlayerSwipeResult.Next -> onNext()
                            PlayerSwipeResult.Previous -> onPrevious()
                            PlayerSwipeResult.Settle -> Unit
                        }
                    }
                    swipe.release(committed = committed, carry = false)
                }
                else -> Unit
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .width(islandWidth)
            .graphicsLayer { translationX = swipeOffset() }
            .animateContentSize(
                animationSpec = if (animated) {
                    spring(dampingRatio = NotchMorphDamping, stiffness = NotchMorphStiffness)
                } else {
                    // Reduced motion: the island still has to change size, it just does not glide.
                    snap()
                },
                alignment = Alignment.TopCenter
            )
            .clip(shape)
            .background(NotchPill)
            .border(1.dp, Color.White.copy(alpha = rim), shape)
            .then(swipeModifier)
            .then(
                if (!interactive) {
                    Modifier
                } else {
                    Modifier.clickable {
                        // The tick follows the state, not the tap, so a card that closes itself
                        // buzzes the same way a tapped one does.
                        if (settings.haptics) haptics.perform(DerbisHapticAction.Confirm)
                        expanded = !expanded
                    }
                }
            )
    ) {
        // The blurred cover only exists in the expanded card; the pill stays pitch black so it
        // reads like the cutout it is imitating. It blooms in behind the card instead of popping.
        if (backdrop > 0.01f) {
            if (settings.showArtwork && preview.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = preview.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = backdrop
                            // Starts slightly wider than the card and settles: the cover is being
                            // revealed from behind the cutout, not faded on top of it.
                            val grow = 1.08f - 0.08f * backdrop
                            scaleX = grow
                            scaleY = grow
                        }
                        .blur(36.dp)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = backdrop }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                NotchInk.copy(alpha = 0.74f),
                                NotchInk.copy(alpha = 0.90f)
                            )
                        )
                    )
            )
        }

        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(animationSpec = tween(if (animated) 160 else 0)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(if (animated) 180 else 0)),
            exit = fadeOut(animationSpec = tween(if (animated) 90 else 0)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(if (animated) 90 else 0))
        ) {
            NotchPillRow(preview = preview, settings = settings)
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(if (animated) 220 else 0, delayMillis = if (animated) 60 else 0)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(if (animated) 240 else 0, delayMillis = if (animated) 60 else 0)),
            exit = fadeOut(animationSpec = tween(if (animated) 90 else 0)) +
                scaleOut(targetScale = 0.97f, animationSpec = tween(if (animated) 110 else 0))
        ) {
            Box {
                NotchCard(
                    preview = preview,
                    settings = settings,
                    activeIndex = activeIndex,
                    onTogglePlay = onTogglePlay,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onOpen = onOpen,
                    interactive = interactive
                )
                if (settings.showProgress) {
                    NotchProgressBar(
                        positionMs = preview.positionMs,
                        durationMs = preview.durationMs,
                        isPlaying = preview.isPlaying,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotchPillRow(preview: NotchPreview, settings: DerbisNotchSettings) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (settings.showArtwork && preview.artworkUrl.isNotBlank()) {
            AsyncImage(
                model = preview.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = preview.title,
            color = NotchText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (settings.showWaveform) {
            Spacer(Modifier.width(6.dp))
            NotchWaveform(isPlaying = preview.isPlaying)
        }
    }
}

/** Three bars that breathe while audio plays and flatten when it stops. */
@Composable
private fun NotchWaveform(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "notch-wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "notch-wave-phase"
    )
    Canvas(modifier = Modifier.size(width = 18.dp, height = 14.dp)) {
        val bars = 3
        val slot = size.width / bars
        val amplitude = if (isPlaying) 1f else 0.18f
        // Only subscribe to the animation while audio is playing: a paused notch should not
        // redraw sixty times a second for three flat bars.
        val wave = if (isPlaying) phase else 0f
        for (index in 0 until bars) {
            val wobble = 0.45f + 0.55f * kotlin.math.abs(kotlin.math.sin(wave + index * 1.1f))
            val barHeight = size.height * wobble * amplitude
            val left = slot * index + slot * 0.28f
            drawRoundRect(
                color = NotchWave,
                topLeft = androidx.compose.ui.geometry.Offset(left, (size.height - barHeight) / 2f),
                size = androidx.compose.ui.geometry.Size(slot * 0.44f, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(slot * 0.22f)
            )
        }
    }
}

@Composable
private fun NotchCard(
    preview: NotchPreview,
    settings: DerbisNotchSettings,
    activeIndex: Int,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    interactive: Boolean
) {
    Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp)) {
        if (settings.compactCard) {
            // No lyric sheet: the artwork sits beside the text instead of above it, so the card
            // stays a card rather than a tall empty box.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NotchArtwork(preview = preview)
                Column(modifier = Modifier.weight(1f)) {
                    NotchTitleBlock(preview = preview, centered = false)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(
                    modifier = Modifier.width(112.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NotchArtwork(preview = preview)
                    Spacer(Modifier.height(10.dp))
                    NotchTitleBlock(preview = preview, centered = true)
                }
                Column(modifier = Modifier.weight(1f)) {
                    NotchLyricSheet(
                        lyrics = preview.lyrics,
                        activeIndex = activeIndex,
                        style = settings.style
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (settings.showTransport) {
                NotchTransportButton(
                    icon = Icons.Rounded.SkipPrevious,
                    label = LocalDerbisStrings.current.previous,
                    size = 30.dp,
                    iconSize = 18.dp,
                    interactive = interactive,
                    onClick = onPrevious
                )
                NotchPlayButton(isPlaying = preview.isPlaying, interactive = interactive, onClick = onTogglePlay)
                NotchTransportButton(
                    icon = Icons.Rounded.SkipNext,
                    label = LocalDerbisStrings.current.next,
                    size = 30.dp,
                    iconSize = 18.dp,
                    interactive = interactive,
                    onClick = onNext
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = formatSeekbarMillis(preview.positionMs),
                color = NotchMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(text = " / ", color = NotchMuted, fontSize = 10.sp)
            Text(
                text = if (preview.durationMs > 0L) formatSeekbarMillis(preview.durationMs) else "--:--",
                color = NotchMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            if (interactive) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = LocalDerbisStrings.current.openPlayer,
                    color = NotchText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable { onOpen() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun NotchArtwork(preview: NotchPreview) {
    if (preview.artworkUrl.isBlank()) return
    AsyncImage(
        model = preview.artworkUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
    )
}

@Composable
private fun NotchTitleBlock(preview: NotchPreview, centered: Boolean) {
    Text(
        text = preview.title,
        color = NotchText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
    Text(
        text = preview.artist,
        color = NotchMuted,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
}

/**
 * One transport control. In the customize screen [interactive] is false, so the icon draws but is
 * not a button — a preview should never look like it is taking the tap you just made on a switch.
 */
@Composable
private fun NotchTransportButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    iconSize: Dp,
    interactive: Boolean,
    onClick: () -> Unit
) {
    if (interactive) {
        androidx.compose.material3.IconButton(onClick = onClick, modifier = Modifier.size(size)) {
            Icon(icon, contentDescription = label, tint = NotchText, modifier = Modifier.size(iconSize))
        }
    } else {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = NotchText, modifier = Modifier.size(iconSize))
        }
    }
}

/**
 * Play/pause. The glyph is the one control that changes meaning on tap, so the swap is punctuated
 * by a quick dip and spring back — the tap lands even before audio does. A preview (not
 * interactive) still draws full strength instead of Material's disabled alpha.
 */
@Composable
private fun NotchPlayButton(
    isPlaying: Boolean,
    interactive: Boolean,
    onClick: () -> Unit
) {
    val bump = remember { Animatable(1f) }
    val animated = LocalAnimationsEnabled.current
    var settledOnce by remember { mutableStateOf(false) }
    LaunchedEffect(isPlaying) {
        // The first composition is not a state change: the island is simply appearing.
        if (!settledOnce) {
            settledOnce = true
            return@LaunchedEffect
        }
        if (!animated) {
            bump.snapTo(1f)
        } else {
            bump.animateTo(
                targetValue = 0.82f,
                animationSpec = tween(DerbisMotion.Durations.Quick, easing = DerbisMotion.Easings.Accelerate)
            )
            bump.animateTo(targetValue = 1f, animationSpec = DerbisMotion.settle.spec())
        }
    }
    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) {
                LocalDerbisStrings.current.pause
            } else {
                LocalDerbisStrings.current.play
            },
            tint = NotchText,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = bump.value
                    scaleY = bump.value
                }
        )
    }
    if (interactive) {
        androidx.compose.material3.IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
            icon()
        }
    } else {
        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            icon()
        }
    }
}

/**
 * Lyric sheet for the expanded notch. Default keeps the list calm and single-coloured; Spicy scales
 * the active line up and gives it the accent treatment used by the spicy lyric deck.
 *
 * The active line now animates between states instead of snapping: colour, opacity and (in Spicy)
 * a small scale all ease on the same short tween, so the sheet reads as one line moving forward.
 */
@Composable
private fun NotchLyricSheet(
    lyrics: List<LyricLine>,
    activeIndex: Int,
    style: DerbisNotchStyle
) {
    val strings = LocalDerbisStrings.current
    val animated = LocalAnimationsEnabled.current
    if (lyrics.isEmpty()) {
        Box(modifier = Modifier.height(112.dp), contentAlignment = Alignment.Center) {
            Text(text = strings.lyricsUnavailable, color = NotchMuted, fontSize = 11.sp)
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex, lyrics.size) {
        if (activeIndex >= 0) {
            runCatching {
                if (animated) {
                    listState.animateScrollToItem(index = (activeIndex - 1).coerceAtLeast(0))
                } else {
                    listState.scrollToItem(index = (activeIndex - 1).coerceAtLeast(0))
                }
            }
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.height(112.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            val weight = if (isActive) FontWeight.Bold else FontWeight.Medium
            val targetColor = when {
                isActive && style == DerbisNotchStyle.Spicy -> NotchSpicy
                isActive -> NotchText
                else -> NotchMuted
            }
            val color by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(if (animated) 220 else 0),
                label = "notch-lyric-color"
            )
            val emphasis by animateFloatAsState(
                targetValue = if (isActive) 1f else 0f,
                animationSpec = tween(if (animated) 220 else 0),
                label = "notch-lyric-emphasis"
            )
            val scale = if (style == DerbisNotchStyle.Spicy) 1f + 0.16f * emphasis else 1f
            Text(
                text = line.text.ifBlank { "♪" },
                color = color,
                fontSize = (if (isActive) 13.sp else 11.sp) * scale,
                fontWeight = weight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer {
                    scaleX = 1f + (scale - 1f) * 0.6f
                    scaleY = 1f + (scale - 1f) * 0.6f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
            )
        }
    }
}

/**
 * Apple style playback hairline: a straight 2 dp line pinned to the bottom edge of the card, square
 * ends, no ring and no pill. The fill glides between player ticks instead of stepping, and it
 * brightens while audio plays so a paused island reads as paused at a glance.
 */
@Composable
internal fun NotchProgressBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    animated: Boolean = LocalAnimationsEnabled.current
) {
    val target = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = if (!animated) 0 else if (isPlaying) 900 else 260,
            easing = LinearEasing
        ),
        label = "notch-progress"
    )
    val fill by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.66f,
        animationSpec = tween(if (animated) 240 else 0),
        label = "notch-progress-fill"
    )
    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        drawRect(color = Color.White.copy(alpha = 0.18f))
        drawRect(
            color = Color.White.copy(alpha = fill),
            size = androidx.compose.ui.geometry.Size(size.width * fraction, size.height)
        )
    }
}
