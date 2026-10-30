package com.yasuda.derbis.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.yasuda.derbis.ui.icons.Repeat
import com.yasuda.derbis.ui.icons.RepeatOne
import com.yasuda.derbis.ui.icons.Shuffle
import com.yasuda.derbis.ui.icons.SkipNext
import com.yasuda.derbis.ui.icons.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.RepeatMode
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.PlayerDarkSurface
import com.yasuda.derbis.ui.components.DerbisLoadingIndicator
import com.yasuda.derbis.ui.components.DerbisPressScale
import com.yasuda.derbis.ui.components.PlayerControlLabels
import com.yasuda.derbis.ui.components.PlayerIcon
import com.yasuda.derbis.ui.components.derbisCookieLandingShape
import com.yasuda.derbis.ui.components.derbisExpressiveCorner
import com.yasuda.derbis.ui.components.derbisExpressiveToggleCorner
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.playerMix
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics

private val FolioScrollingStage = 300.dp
private val FolioStageCorner = 0.dp
private val FolioControlHeight = 80.dp
private val FolioControlHeightCompact = 66.dp
private const val FolioPaperDepth = 0.74f
private const val FolioArtistAlpha = 0.78f
private const val FolioFixedActionCount = 3
private const val FolioCoverLobeDegrees = 30f
private const val FolioCoverLobeMillis = 4_000

@Composable
internal fun PlayerFolioDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
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
    onToggleFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val metrics = folioDeckMetrics(compact)
    val paper by animateColorAsState(
        targetValue = surfaces.glow.copy(alpha = 1f).playerMix(PlayerDarkSurface, FolioPaperDepth),
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.paletteTween()),
        label = "player-folio-paper"
    )
    val ink by animateColorAsState(
        targetValue = surfaces.hero,
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.paletteTween()),
        label = "player-folio-ink"
    )
    val inkContent by animateColorAsState(
        targetValue = surfaces.heroContent,
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.paletteTween()),
        label = "player-folio-ink-content"
    )
    Column(
        modifier = modifier
            .background(paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(
                start = gutter,
                end = gutter,
                top = DerbisPlayerDesign.SpaceXs,
                bottom = metrics.bottom
            )
    ) {
        slots.header()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.height(FolioScrollingStage) else Modifier.weight(1f))
                .padding(vertical = metrics.stagePadding),
            contentAlignment = Alignment.Center
        ) {
            val side = minOf(maxWidth, maxHeight, artworkCap)
            val turn = rememberFolioCoverTurn(isPlaying, animated)
            slots.stage(
                track,
                Modifier
                    .size(side)
                    .graphicsLayer {
                        shape = derbisCookieLandingShape(1f, turn.value)
                        clip = true
                    },
                FolioStageCorner
            )
            slots.gestures(track, Modifier.matchParentSize())
        }
        FolioHeadline(
            track = track,
            ink = ink,
            animated = animated,
            compact = compact,
            onArtistClick = onArtistClick,
            modifier = headlineModifier
        )
        Spacer(Modifier.height(metrics.headlineGap))
        FolioTransport(
            slots = slots,
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            metrics = metrics,
            ink = ink,
            inkContent = inkContent,
            onPrevious = onPrevious,
            onTogglePlay = onTogglePlay,
            onNext = onNext
        )
        Spacer(Modifier.height(metrics.actionGap))
        FolioActionRow(
            track = track,
            slots = slots,
            surfaces = surfaces,
            isFavorite = isFavorite,
            shuffleOn = shuffleOn,
            repeatMode = repeatMode,
            labels = labels,
            animated = animated,
            onToggleFavorite = onToggleFavorite,
            onShuffle = onShuffle,
            onRepeat = onRepeat
        )
    }
}

@Composable
private fun rememberFolioCoverTurn(isPlaying: Boolean, animated: Boolean): Animatable<Float, AnimationVector1D> {
    val turn = remember { Animatable(0f) }
    LaunchedEffect(isPlaying, animated) {
        if (!isPlaying || !animated) return@LaunchedEffect
        while (true) {
            turn.animateTo(turn.value + FolioCoverLobeDegrees, tween(FolioCoverLobeMillis, easing = LinearEasing))
            turn.snapTo(0f)
        }
    }
    return turn
}

@Composable
private fun FolioTransport(
    slots: PlayerDeckSlots,
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    metrics: FolioDeckMetrics,
    ink: Color,
    inkContent: Color,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FolioPlayPill(
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            height = metrics.controlHeight,
            ink = ink,
            inkContent = inkContent,
            onClick = onTogglePlay
        )
        FolioRoundButton(Icons.Rounded.SkipNext, labels.next, metrics.controlHeight, ink, inkContent, onNext)
    }
    Spacer(Modifier.height(metrics.transportGap))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FolioRoundButton(Icons.Rounded.SkipPrevious, labels.previous, metrics.controlHeight, ink, inkContent, onPrevious)
        Box(modifier = Modifier.weight(1f)) {
            slots.progress(PlayerProgressStyle.Wavy)
        }
    }
}

private class FolioDeckMetrics(
    val controlHeight: Dp,
    val bottom: Dp,
    val stagePadding: Dp,
    val headlineGap: Dp,
    val transportGap: Dp,
    val actionGap: Dp
)

private val FolioRegularMetrics = FolioDeckMetrics(
    controlHeight = FolioControlHeight,
    bottom = DerbisPlayerDesign.SpaceMd,
    stagePadding = DerbisPlayerDesign.SpaceLg,
    headlineGap = DerbisPlayerDesign.SpaceLg,
    transportGap = DerbisPlayerDesign.SpaceMd,
    actionGap = DerbisPlayerDesign.SpaceSm
)

private val FolioCompactMetrics = FolioDeckMetrics(
    controlHeight = FolioControlHeightCompact,
    bottom = DerbisPlayerDesign.SpaceSm,
    stagePadding = DerbisPlayerDesign.SpaceSm,
    headlineGap = DerbisPlayerDesign.SpaceMd,
    transportGap = DerbisPlayerDesign.SpaceSm,
    actionGap = DerbisPlayerDesign.SpaceXs
)

private fun folioDeckMetrics(compact: Boolean): FolioDeckMetrics =
    if (compact) FolioCompactMetrics else FolioRegularMetrics

@Composable
private fun FolioHeadline(
    track: Track,
    ink: Color,
    animated: Boolean,
    compact: Boolean,
    onArtistClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDerbisStrings.current
    AnimatedContent(
        targetState = track,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { DerbisMotion.contentSwap(animated) },
        contentKey = { it.id },
        label = "player-folio-headline"
    ) { shown ->
        Column(modifier = Modifier.fillMaxWidth()) {
            val titleSize = folioTitleSize(shown.title, compact)
            Text(
                text = shown.title,
                color = ink,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black,
                fontSize = titleSize,
                lineHeight = titleSize,
                letterSpacing = (-1.2).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(DerbisPlayerDesign.SpaceSm))
            PlayerArtistText(
                track = shown,
                color = ink.copy(alpha = FolioArtistAlpha),
                style = TextStyle(
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.6.sp
                ),
                onClickLabel = strings.openArtist,
                onArtistClick = onArtistClick,
                enabled = shown.id == track.id
            )
        }
    }
}

internal fun folioTitleSize(title: String, compact: Boolean): TextUnit {
    val base = when {
        title.length <= 10 -> 60f
        title.length <= 18 -> 46f
        title.length <= 30 -> 38f
        else -> 32f
    }
    return (if (compact) base * 0.82f else base).sp
}

@Composable
private fun RowScope.FolioPlayPill(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    height: Dp,
    ink: Color,
    inkContent: Color,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val restCorner = derbisExpressiveToggleCorner(
        checked = isPlaying,
        unchecked = height * 0.3f,
        checkedCorner = height / 2,
        label = "player-folio-pill-corner"
    )
    val corner = derbisExpressiveCorner(
        interactionSource = interaction,
        rest = restCorner,
        pressed = height * 0.2f,
        label = "player-folio-pill-press"
    )
    val label = if (isPlaying) labels.pause else labels.play
    Box(
        modifier = Modifier
            .weight(1f)
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(ink)
            .derbisPressable(
                onClick = {
                    haptics.perform(DerbisHapticAction.Transport)
                    onClick()
                },
                interactionSource = interaction,
                pressedScale = DerbisPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label }
            .padding(horizontal = DerbisPlayerDesign.SpaceLg),
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            DerbisLoadingIndicator(color = inkContent, modifier = Modifier.size(height * 0.44f))
        } else {
            Text(
                text = label.uppercase(),
                color = inkContent,
                fontSize = if (height < FolioControlHeight) 21.sp else 25.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FolioRoundButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    ink: Color,
    inkContent: Color,
    onClick: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val corner = derbisExpressiveCorner(
        interactionSource = interaction,
        rest = size / 2,
        pressed = size * 0.28f,
        label = "player-folio-round-press"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(ink)
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
        PlayerIcon(icon, inkContent, Modifier.size(size * 0.38f))
    }
}

@Composable
private fun FolioActionRow(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    animated: Boolean,
    onToggleFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val dockActions = slots.dockActions(track)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val controlCount = FolioFixedActionCount + dockActions.size
        val fits = maxWidth >= DerbisPlayerDesign.MinimumTouchTarget * controlCount
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fits) Modifier else Modifier.horizontalScroll(rememberScrollState())),
            horizontalArrangement = if (fits) Arrangement.SpaceEvenly else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerFavoriteButton(
                trackId = track.id,
                isFavorite = isFavorite,
                surfaces = surfaces,
                label = strings.favoritesPlain,
                animated = animated,
                onToggle = onToggleFavorite
            )
            PlayerToggleControl(
                icon = Icons.Rounded.Shuffle,
                label = labels.shuffle,
                active = shuffleOn,
                toggle = true,
                surfaces = surfaces,
                animated = animated,
                glyph = DerbisPlayerDesign.TransportModeGlyph,
                onClick = onShuffle
            )
            PlayerToggleControl(
                icon = if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                label = labels.repeat,
                active = repeatMode != RepeatMode.Off,
                toggle = true,
                surfaces = surfaces,
                animated = animated,
                glyph = DerbisPlayerDesign.TransportModeGlyph,
                onClick = onRepeat
            )
            dockActions.forEach { action ->
                PlayerToggleControl(
                    icon = action.icon,
                    label = action.label,
                    active = action.active,
                    toggle = action.toggle,
                    surfaces = surfaces,
                    animated = animated,
                    glyph = DerbisPlayerDesign.TransportModeGlyph,
                    onClick = action.onClick,
                    enabled = action.enabled,
                    busy = action.busy,
                    stateDescription = action.stateDescription
                )
            }
        }
    }
}
