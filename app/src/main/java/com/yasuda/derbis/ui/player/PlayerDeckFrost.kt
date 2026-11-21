package com.yasuda.derbis.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * Frost — the glass deck.
 *
 * One idea: the record is the light. The artwork's two accents bleed into a near-black ground, the
 * sleeve floats on that glow, and every control lives inside a single glass panel beneath it. One
 * translucent layer, one specular line, no stacked blur: the ground is already a gradient, so the
 * panel only has to be lit, not blurred.
 */

private val FrostGround = Color(0xFF07100F)
private val FrostInk = Color(0xFFF2F4F1)

private const val FrostInkStrong = 0.78f
private const val FrostInkMuted = 0.62f
private const val FrostInkSoft = 0.50f
private const val FrostInkFaint = 0.44f
private const val FrostPanelTopAlpha = 0.15f
private const val FrostPanelBottomAlpha = 0.06f
private const val FrostPanelBorderAlpha = 0.16f
private const val FrostTrackAlpha = 0.18f

private val FrostCoverCorner = 20.dp
private val FrostPanelCorner = 26.dp
private val FrostSeekHeight = 6.dp
private val FrostSeekThumb = 11.dp
private val FrostPlaySize = 56.dp
private val FrostPlaySizeCompact = 48.dp
private val FrostControlSize = 46.dp
private val FrostGlyphSize = 21.dp
private val FrostModeGlyphSize = 18.dp

@Composable
internal fun PlayerFrostDeck(
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
    Box(modifier = modifier.background(FrostGround)) {
        FrostBackdrop(accent = accent)
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
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            slots.header()
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceSm))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.height(FrostScrollStage) else Modifier.weight(1f)),
                contentAlignment = Alignment.Center
            ) {
                val side = minOf(maxWidth, maxHeight, artworkCap)
                if (side > 0.dp) {
                    slots.stage(
                        track,
                        Modifier
                            .size(side)
                            .shadow(
                                elevation = 26.dp,
                                shape = RoundedCornerShape(FrostCoverCorner),
                                clip = false,
                                ambientColor = accent.copy(alpha = 0.6f),
                                spotColor = accent.copy(alpha = 0.6f)
                            ),
                        FrostCoverCorner
                    )
                    slots.gestures(track, Modifier.size(side))
                }
            }
            Spacer(Modifier.height(if (compact) DerbisPlayerDesign.SpaceMd else DerbisPlayerDesign.SpaceLg))
            FrostPanel(
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
            FrostLyricsStrip(
                track = track,
                playback = playback,
                dockActions = slots.dockActions(track)
            )
        }
    }
}

private val FrostScrollStage = 300.dp

@Composable
private fun FrostBackdrop(accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(FrostGround)
        drawRect(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * 0.24f, size.height * 0.16f),
                radius = size.minDimension * 1.05f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.34f), Color.Transparent),
                center = Offset(size.width * 0.86f, size.height * 0.34f),
                radius = size.minDimension * 0.92f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * 0.46f, size.height * 0.92f),
                radius = size.minDimension * 1.0f
            )
        )
        drawRect(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, FrostGround.copy(alpha = 0.88f)),
                startY = size.height * 0.42f,
                endY = size.height
            )
        )
    }
}

@Composable
private fun FrostPanel(
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
    val shape = RoundedCornerShape(FrostPanelCorner)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = FrostPanelTopAlpha),
                        Color.White.copy(alpha = FrostPanelBottomAlpha)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = FrostPanelBorderAlpha), shape)
    ) {
        FrostSpecular()
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)) {
            FrostHeadline(
                track = track,
                compact = compact,
                animated = animated,
                onArtistClick = onArtistClick
            )
            Spacer(Modifier.height(if (compact) DerbisPlayerDesign.SpaceSm else 14.dp))
            FrostSeek(
                playback = playback,
                accent = accent
            )
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceXxs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FrostTime(formatOneTime(playback.positionMs))
                FrostTime(if (playback.hasDuration) formatOneTime(playback.durationMs) else "--:--")
            }
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceMd))
            FrostTransport(
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
}

/** The lit top edge of the glass: one hairline that fades out at both ends. */
@Composable
private fun FrostSpecular(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 26.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0f)
                    )
                )
            )
    )
}

@Composable
private fun FrostHeadline(
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
        label = "player-frost-headline"
    ) { shown ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = shown.title,
                color = FrostInk,
                fontSize = if (compact) 19.sp else 22.sp,
                lineHeight = (if (compact) 19.sp else 22.sp) * 1.16f,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.26).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (shown.artist.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = shown.artist.uppercase(),
                    color = FrostInk.copy(alpha = FrostInkMuted),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.4.sp,
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
private fun FrostSeek(
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
                .height(FrostSeekHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = FrostTrackAlpha))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(FrostSeekHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accent, Color.White)
                        )
                    )
            )
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(FrostSeekHeight)) {
            val x = size.width * fraction
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = (FrostSeekThumb / 2 + 3.dp).toPx(),
                center = Offset(x, size.height / 2f)
            )
            drawCircle(
                color = Color.White,
                radius = (FrostSeekThumb / 2).toPx(),
                center = Offset(x, size.height / 2f)
            )
        }
    }
}

@Composable
private fun FrostTime(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = FrostInk.copy(alpha = FrostInkSoft),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.6.sp,
        style = FrostTabular,
        maxLines = 1,
        modifier = modifier
    )
}

private val FrostTabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun FrostTransport(
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
        FrostModeButton(
            icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            label = labels.repeat,
            active = repeatMode != RepeatMode.Off,
            onClick = onRepeat
        )
        FrostIconButton(
            icon = Icons.Rounded.SkipPrevious,
            label = labels.previous,
            onClick = onPrevious
        )
        FrostPlayButton(
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            compact = compact,
            onClick = onTogglePlay
        )
        FrostIconButton(
            icon = Icons.Rounded.SkipNext,
            label = labels.next,
            onClick = onNext
        )
        FrostModeButton(
            icon = Icons.Rounded.Shuffle,
            label = labels.shuffle,
            active = shuffleOn,
            onClick = onShuffle
        )
    }
}

@Composable
private fun FrostModeButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    Box(
        modifier = Modifier
            .size(FrostControlSize)
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
            tint = FrostInk.copy(alpha = if (active) FrostInkStrong else FrostInkFaint),
            modifier = Modifier.size(FrostModeGlyphSize)
        )
    }
}

@Composable
private fun FrostIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    Box(
        modifier = Modifier
            .size(FrostControlSize)
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
            tint = FrostInk.copy(alpha = 0.88f),
            modifier = Modifier.size(FrostGlyphSize)
        )
    }
}

@Composable
private fun FrostPlayButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    compact: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val label = if (isPlaying) labels.pause else labels.play
    val size = if (compact) FrostPlaySizeCompact else FrostPlaySize
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f))
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
            DerbisLoadingIndicator(color = FrostGround, modifier = Modifier.size(size * 0.36f))
        } else {
            DerbisPlayPauseGlyph(
                playing = isPlaying,
                color = FrostGround,
                modifier = Modifier.size(if (compact) 18.dp else 21.dp)
            )
        }
    }
}

@Composable
private fun FrostLyricsStrip(
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
                .background(Color.White.copy(alpha = 0.12f))
        )
        Spacer(Modifier.height(DerbisPlayerDesign.SpaceMd))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = playback.lyricsLabel.uppercase(),
                color = FrostInk.copy(alpha = FrostInkFaint),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.9.sp,
                maxLines = 1
            )
            FrostDockRow(actions = dockActions)
        }
        val body = line.ifEmpty { "" }
        if (body.isNotEmpty()) {
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceSm))
            Text(
                text = body,
                color = FrostInk.copy(alpha = FrostInkStrong),
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
                    .semantics { contentDescription = "${playback.lyricsLabel}: $body" }
            )
        } else if (track.title.isNotBlank()) {
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceSm))
            Text(
                text = playback.lyricsLabel,
                color = FrostInk.copy(alpha = FrostInkFaint),
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
private fun FrostDockRow(actions: List<PlayerDockAction>) {
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
                        color = FrostInk.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                } else {
                    PlayerIcon(
                        icon = action.icon,
                        tint = FrostInk.copy(alpha = if (action.active) 0.85f else FrostInkFaint),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
