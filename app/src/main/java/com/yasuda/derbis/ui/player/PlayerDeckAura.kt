package com.yasuda.derbis.ui.player

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

private val AuraInk = Color.White
private val AuraInkSoft = Color.White.copy(alpha = 0.64f)
private val AuraInkFaint = Color.White.copy(alpha = 0.24f)
private val AuraBadge = Color.White.copy(alpha = 0.14f)
private val AuraGutter = 22.dp
private val AuraBarIdle = 4.dp
private val AuraBarActive = 9.dp
private const val VelvetSleeveWidth = 0.86f

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
    visualsEnabled: Boolean,
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
    val beatSyncVisuals = rememberBeatSyncVisualState(track.id, isPlaying, visualsEnabled)
    val velvetTop = remember(track.id, track.accentStart) {
        androidx.compose.ui.graphics.lerp(Color(track.accentStart), Color.Black, 0.72f)
    }
    val velvetMid = remember(track.id, track.accentEnd) {
        androidx.compose.ui.graphics.lerp(Color(track.accentEnd), Color.Black, 0.86f)
    }
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
                AuraBeatSyncArtwork(
                    visuals = beatSyncVisuals,
                    artworkUrl = artworkUrl,
                    motionArtwork = motionArtwork,
                    motionEnabled = motionEnabled,
                    isPlaying = isPlaying,
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
            BeatSyncProgressBar(
                fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                enabled = durationMs > 0L,
                visuals = beatSyncVisuals,
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
private fun AuraBeatSyncArtwork(
    visuals: BeatSyncVisualState,
    artworkUrl: String,
    motionArtwork: MotionArtwork?,
    motionEnabled: Boolean,
    isPlaying: Boolean,
    canvasQuality: DerbisCanvasQuality,
    modifier: Modifier = Modifier
) {
    BeatSyncArtworkFrame(visuals = visuals, modifier = modifier) { artworkModifier ->
        Box(modifier = artworkModifier.background(Color.Black.copy(alpha = 0.30f))) {
            MotionArtworkLayer(
                artwork = motionArtwork,
                enabled = motionEnabled,
                isPlaying = isPlaying,
                cornerRadius = 0.dp,
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
