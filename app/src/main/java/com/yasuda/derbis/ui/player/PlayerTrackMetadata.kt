package com.yasuda.derbis.ui.player

import com.yasuda.derbis.ui.theme.*
import com.yasuda.derbis.ui.theme.DerbisIsNocturne

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import com.yasuda.derbis.ui.icons.Favorite
import com.yasuda.derbis.ui.icons.FavoriteBorder
import androidx.compose.material.icons.rounded.AddCircleOutline
import com.yasuda.derbis.ui.icons.CheckCircle
import com.yasuda.derbis.ui.icons.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp as OutlinedThumbUp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.PlayerGlassIconButton
import com.yasuda.derbis.ui.components.derbisMarquee
import com.yasuda.derbis.ui.components.rememberDerbisFavoriteMorphShape
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.DerbisTypeRhythm
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics

private const val FavoritePopScale = 1.14f
private const val TitleMarqueeDelayMs = 3_200

@Composable
internal fun PlayerTrackMetadata(
    track: Track,
    isFavorite: Boolean,
    surfaces: PlayerSurfaceTokens,
    animationsEnabled: Boolean,
    stepDirection: Int,
    compact: Boolean,
    openArtistLabel: String,
    favoritesLabel: String,
    activeLyric: LyricLine?,
    lyricsLoading: Boolean,
    showLyricPreview: Boolean,
    lyricsLabel: String,
    onOpenLyrics: () -> Unit,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appleMusic = PlayerAppleMusicStyle
    val prism = PlayerPrismStyle
    val titleSize = metadataTitleSize(compact, appleMusic, prism)
    val artistSize = metadataArtistSize(compact, appleMusic)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = track,
                transitionSpec = { DerbisMotion.trackChange(animationsEnabled, stepDirection) },
                contentKey = { it.id },
                label = "player-metadata"
            ) { shown ->
                val isCurrent = shown.id == track.id
                Column {
                    Text(
                        text = shown.title,
                        color = surfaces.content,
                        fontSize = titleSize,
                        lineHeight = DerbisTypeRhythm.lineHeight(titleSize),
                        fontWeight = metadataTitleWeight(appleMusic, prism),
                        letterSpacing = if (DerbisIsSpotify) (-0.4).sp else if (DerbisIsYouTubeMusic) (-0.2).sp else if (appleMusic) (-0.3).sp else (-0.8).sp,
                        maxLines = if (animationsEnabled) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.derbisMarquee(
                            enabled = animationsEnabled,
                            repeatDelayMillis = TitleMarqueeDelayMs
                        )
                    )
                    PlayerArtistText(
                        track = shown,
                        color = surfaces.contentMuted,
                        style = TextStyle(
                            fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                            fontSize = artistSize,
                            lineHeight = DerbisTypeRhythm.lineHeight(artistSize),
                            fontWeight = if (DerbisIsSpotify || appleMusic || DerbisIsYouTubeMusic) FontWeight.Normal else FontWeight.Medium,
                            letterSpacing = (-0.1).sp
                        ),
                        onClickLabel = openArtistLabel,
                        onArtistClick = onArtistClick,
                        enabled = isCurrent
                    )
                }
            }
            if (showLyricPreview && activeLyric?.text?.isNotBlank() == true) {
                PlayerInlineLyricsLink(
                    line = activeLyric,
                    loading = lyricsLoading,
                    showLinePreview = showLyricPreview,
                    label = lyricsLabel,
                    color = surfaces.activeContent,
                    onOpenLyrics = onOpenLyrics
                )
            }
        }
        Spacer(modifier = Modifier.width(DerbisPlayerDesign.SpaceMd))
        PlayerFavoriteButton(
            trackId = track.id,
            isFavorite = isFavorite,
            surfaces = surfaces,
            label = favoritesLabel,
            animated = animationsEnabled,
            onToggle = onToggleFavorite
        )
    }
}

/**
 * Every Player Deck uses this compact lyric affordance directly beneath metadata. Text changes
 * fade rather than jump as the active lyric moves; tapping it opens the full lyrics page.
 */
internal fun playerInlineLyricsText(line: LyricLine?, label: String): String =
    line?.text?.trim()?.takeIf { it.isNotEmpty() }.orEmpty()

@Composable
private fun PlayerInlineLyricsLink(
    line: LyricLine?,
    loading: Boolean,
    showLinePreview: Boolean,
    label: String,
    color: Color,
    onOpenLyrics: () -> Unit
) {
    if (!showLinePreview) return
    val lyricText = line?.text?.trim()?.takeIf { it.isNotEmpty() } ?: return
    val accessibilityLabel = "$label: $lyricText"
    AnimatedContent(
        targetState = lyricText,
        transitionSpec = {
            (fadeIn(animationSpec = tween(190)) + scaleIn(initialScale = 0.985f, animationSpec = tween(190)))
                .togetherWith(fadeOut(animationSpec = tween(125)))
        },
        label = "player-inline-lyric"
    ) { shownLyric ->
        Text(
            text = shownLyric,
            color = color.copy(alpha = 0.96f),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.1).sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpenLyrics)
                .semantics { contentDescription = accessibilityLabel }
        )
    }
}

private fun metadataTitleSize(compact: Boolean, appleMusic: Boolean, prism: Boolean): TextUnit = when {
    DerbisIsYouTubeMusic -> 24.sp
    DerbisIsSpotify -> 22.sp
    appleMusic -> 20.sp
    prism -> 23.sp
    DerbisIsNocturne -> if (compact) 22.sp else 25.sp
    compact -> 23.sp
    else -> 27.sp
}

private fun metadataArtistSize(compact: Boolean, appleMusic: Boolean): TextUnit = when {
    DerbisIsYouTubeMusic -> 16.sp
    DerbisIsSpotify -> 16.sp
    appleMusic -> 20.sp
    compact -> 15.sp
    else -> 16.5.sp
}

private fun metadataTitleWeight(appleMusic: Boolean, prism: Boolean): FontWeight = when {
    DerbisIsYouTubeMusic -> FontWeight.Bold
    DerbisIsSpotify -> FontWeight.Bold
    appleMusic -> FontWeight.Medium
    DerbisIsNocturne || prism -> FontWeight.Medium
    else -> FontWeight.ExtraBold
}

@Composable
internal fun PlayerFavoriteButton(
    trackId: String,
    isFavorite: Boolean,
    surfaces: PlayerSurfaceTokens,
    label: String,
    animated: Boolean,
    onToggle: () -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    val pop = rememberFavoritePop(trackId, isFavorite, animated)
    val handleToggle = {
        haptics.perform(DerbisHapticAction.Favorite)
        onToggle()
    }
    when {
        DerbisIsSpotify -> SpotifyPlayerFavoriteButton(isFavorite, label, pop.value, handleToggle)
        DerbisIsYouTubeMusic -> YouTubeMusicPlayerFavoriteButton(isFavorite, label, pop.value, handleToggle)
        PlayerAppleMusicStyle -> AppleMusicPlayerFavoriteButton(isFavorite, label, pop.value, handleToggle)
        else -> DefaultPlayerFavoriteButton(isFavorite, surfaces, label, animated, pop.value, handleToggle)
    }
}

@Composable
private fun SpotifyPlayerFavoriteButton(
    isFavorite: Boolean,
    label: String,
    scale: Float,
    onToggle: () -> Unit
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(DerbisPlayerDesign.MinimumTouchTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { toggleableState = ToggleableState(isFavorite) }
    ) {
        Icon(
            if (isFavorite) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline, label,
            tint = if (isFavorite) SpotifyDesign.green else SpotifyDesign.text, modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun YouTubeMusicPlayerFavoriteButton(
    isFavorite: Boolean,
    label: String,
    scale: Float,
    onToggle: () -> Unit
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(DerbisPlayerDesign.MinimumTouchTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { toggleableState = ToggleableState(isFavorite) }
    ) {
        Icon(if (isFavorite) Icons.Rounded.ThumbUp else Icons.Outlined.OutlinedThumbUp, label, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun AppleMusicPlayerFavoriteButton(
    isFavorite: Boolean,
    label: String,
    scale: Float,
    onToggle: () -> Unit
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(DerbisPlayerDesign.MinimumTouchTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { toggleableState = ToggleableState(isFavorite) }
    ) {
        Box(
            Modifier.size(30.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (isFavorite) AppleMusicVibrant.primary else AppleMusicVibrant.quaternary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, label,
                tint = if (isFavorite) Color.Black.copy(alpha = 0.78f) else AppleMusicVibrant.primary, modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun DefaultPlayerFavoriteButton(
    isFavorite: Boolean,
    surfaces: PlayerSurfaceTokens,
    label: String,
    animated: Boolean,
    scale: Float,
    onToggle: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = surfaces.tintFor(isFavorite, surfaces.content),
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.standardTween(200)),
        label = "player-favorite-tint"
    )
    val fill by animateColorAsState(
        targetValue = surfaces.fillFor(isFavorite),
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.standardTween(200)),
        label = "player-favorite-fill"
    )
    val outline = surfaces.segmentOutline
    PlayerGlassIconButton(
        icon = favoriteIcon(isFavorite),
        contentDescription = label,
        size = 44.dp,
        iconSize = 23.dp,
        tint = tint,
        fill = fill,
        borderTop = outline,
        borderBottom = outline,
        shape = if (PlayerPrismStyle) androidx.compose.foundation.shape.CircleShape else rememberDerbisFavoriteMorphShape(favorite = isFavorite, animated = animated),
        modifier = Modifier
            .size(DerbisPlayerDesign.MinimumTouchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics { toggleableState = ToggleableState(isFavorite) },
        onClick = onToggle
    )
}

internal fun shouldTriggerFavoritePop(
    previousTrackId: String?,
    currentTrackId: String,
    wasFavorite: Boolean,
    isFavorite: Boolean,
    animated: Boolean
): Boolean =
    animated && previousTrackId == currentTrackId && !wasFavorite && isFavorite

@Composable
internal fun rememberFavoritePop(
    trackId: String,
    isFavorite: Boolean,
    animated: Boolean
): Animatable<Float, *> {
    val pop = remember(trackId) { Animatable(1f) }
    var wasFavorite by remember(trackId) { mutableStateOf(isFavorite) }

    LaunchedEffect(trackId, isFavorite, animated) {
        val shouldPop = shouldTriggerFavoritePop(
            previousTrackId = trackId,
            currentTrackId = trackId,
            wasFavorite = wasFavorite,
            isFavorite = isFavorite,
            animated = animated
        )
        wasFavorite = isFavorite

        if (!shouldPop) {
            pop.snapTo(1f)
            return@LaunchedEffect
        }
        pop.animateTo(FavoritePopScale, tween(DerbisMotion.Durations.Instant, easing = DerbisMotion.Easings.Decelerate))
        pop.animateTo(1f, DerbisMotion.feedback.spec())
    }
    return pop
}

private fun favoriteIcon(isFavorite: Boolean): ImageVector =
    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
