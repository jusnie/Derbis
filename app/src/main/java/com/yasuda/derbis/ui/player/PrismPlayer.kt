package com.yasuda.derbis.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.domain.PrismPlayerLayout
import com.yasuda.derbis.domain.RepeatMode
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.PrismPlayDisc
import com.yasuda.derbis.ui.LocalDerbisGlassAccessibility
import com.yasuda.derbis.ui.LocalDerbisGlassStyle
import com.yasuda.derbis.ui.LocalGlassBackdrop
import com.yasuda.derbis.ui.glassBackdropSource
import com.yasuda.derbis.ui.rememberGlassBackdropState
import com.yasuda.derbis.ui.rememberGlassBlurAllowed
import com.yasuda.derbis.ui.components.PlayerControlLabels
import com.yasuda.derbis.ui.components.DerbisGlassIntensity
import com.yasuda.derbis.ui.components.LocalDerbisLiquidGlassEnabled
import com.yasuda.derbis.ui.components.derbisGlass
import com.yasuda.derbis.ui.components.formatSeekbarMillis
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.*

@Composable
internal fun PrismPlayerScene(
    track: Track, slots: PlayerDeckSlots, artworkCap: Dp,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit, modifier: Modifier = Modifier,
    playing: Boolean = true,
    activeLyric: LyricLine? = null,
    lyricPositionMs: Long = 0L
) {
    if (DerbisIsYouTubeMusic) {
        YouTubeMusicPlayerScene(track, slots, artworkCap, playing, metadata, transport, modifier)
        return
    }
    if (DerbisIsAppleMusic) {
        AppleMusicPlayerScene(
            track, slots, artworkCap, playing, metadata, transport, modifier,
            activeLyric = activeLyric,
            lyricPositionMs = lyricPositionMs
        )
        return
    }
    if (DerbisIsTorque) {
        TorquePlayerScene(track, slots, metadata, transport, modifier)
        return
    }
    if (DerbisIsSpotify) {
        SpotifyPlayerScene(track, slots, artworkCap, metadata, transport, modifier)
        return
    }
    if (DerbisIsIvory) {
        IvoryPlayerScene(track, slots, artworkCap, metadata, transport, modifier)
        return
    }
    val focus = activePrismSettings.playerLayout == PrismPlayerLayout.Focus
    // Real blur is scoped to one player backdrop. Low-RAM, battery saver, High text contrast,
    // and the low-transparency setting resolve to the same lightweight opaque material instead
    // of leaving an unused expensive layer alive. Reduced Motion remains independent.
    val glassAccessibility = LocalDerbisGlassAccessibility.current
    val glassStyle = LocalDerbisGlassStyle.current
    val playerGlassEnabled = LocalDerbisLiquidGlassEnabled.current &&
        !glassAccessibility.prefersOpaqueMaterials && !glassStyle.prefersOpaqueMaterials
    val playerGlass = rememberGlassBackdropState(playerGlassEnabled && rememberGlassBlurAllowed())
    CompositionLocalProvider(LocalGlassBackdrop provides playerGlass) {
        BoxWithConstraints(modifier.statusBarsPadding().navigationBarsPadding()) {
            Box(
                Modifier
                    .matchParentSize()
                    .glassBackdropSource(playerGlass)
            ) {
                PrismPlayerAtmosphere(Modifier.fillMaxSize())
            }
        val artSize = minOf(maxWidth - PrismDesign.gutter * 2, maxHeight * if (focus) 0.5f else 0.44f, artworkCap)
        val viewport = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PrismDesign.gutter)) {
            Column(Modifier.fillMaxWidth().heightIn(min = viewport), verticalArrangement = Arrangement.SpaceBetween) {
                slots.header()
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    slots.stage(track, Modifier.size(artSize), PrismDesign.artworkCorner)
                }
                val infoShape = RoundedCornerShape(28.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .derbisGlass(
                            shape = infoShape,
                            baseTint = DerbisPanelSoft,
                            fallbackColor = DerbisPanel,
                            fallbackBorderColor = DerbisText.copy(alpha = 0.18f),
                            intensity = DerbisGlassIntensity.Subtle
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(if (focus) 14.dp else 6.dp)
                ) {
                    if (focus) {
                        slots.progress(PlayerProgressStyle.Classic)
                        metadata()
                    } else {
                        metadata()
                        slots.engagement(track)
                        slots.progress(PlayerProgressStyle.Classic)
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .derbisGlass(
                            shape = RoundedCornerShape(32.dp),
                            baseTint = DerbisPanelSoft,
                            fallbackColor = DerbisPanel,
                            fallbackBorderColor = DerbisText.copy(alpha = 0.18f),
                            intensity = DerbisGlassIntensity.Standard
                        )
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { transport() }
                }
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    slots.dock(track)
                    if (focus) slots.engagement(track)
                }
            }
        }
        }
    }
}

@Composable
internal fun PrismTransport(
    playing: Boolean, resolving: Boolean, shuffle: Boolean, repeat: RepeatMode,
    labels: PlayerControlLabels, onShuffle: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier
) {
    if (DerbisIsTorque) {
        TorqueTransport(playing, resolving, shuffle, repeat, labels, onShuffle, onPrevious, onToggle, onNext, onRepeat, modifier)
        return
    }
    if (DerbisIsYouTubeMusic) {
        YouTubeMusicTransport(playing, resolving, shuffle, repeat, labels, onShuffle, onPrevious, onToggle, onNext, onRepeat, modifier)
        return
    }
    if (PlayerAppleMusicStyle) {
        AppleMusicTransport(playing, resolving, shuffle, repeat, labels, onShuffle, onPrevious, onToggle, onNext, onRepeat, modifier)
        return
    }
    if (DerbisIsSpotify) {
        SpotifyTransport(playing, resolving, shuffle, repeat, labels, onShuffle, onPrevious, onToggle, onNext, onRepeat, modifier)
        return
    }
    if (DerbisIsIvory) {
        IvoryTransport(playing, resolving, shuffle, repeat, labels, onShuffle, onPrevious, onToggle, onNext, onRepeat, modifier)
        return
    }
    Row(modifier.fillMaxWidth().heightIn(min = 88.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        PrismModeButton(Icons.Rounded.Shuffle, labels.shuffle, shuffle, onShuffle)
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, labels.previous, tint = DerbisText, modifier = Modifier.size(38.dp))
        }
        PrismPlayDisc(
            playing, resolving, 76.dp,
            Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onToggle)
                .semantics { contentDescription = if (playing) labels.pause else labels.play }
        )
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, labels.next, tint = DerbisText, modifier = Modifier.size(38.dp))
        }
        PrismModeButton(if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off, onRepeat)
    }
}

@Composable
private fun PrismModeButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .derbisGlass(
                shape = CircleShape,
                baseTint = if (active) DerbisCyan else DerbisPanelSoft,
                fallbackColor = if (active) DerbisCyan.copy(alpha = 0.82f) else DerbisPanel,
                fallbackBorderColor = DerbisText.copy(alpha = 0.18f),
                intensity = DerbisGlassIntensity.Subtle
            )
            .toggleable(value = active, role = Role.Switch, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = if (active) DerbisCyan else DerbisMuted, modifier = Modifier.size(24.dp))
        if (active) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(4.dp).clip(CircleShape).background(DerbisCyan))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrismSeekbar(positionMs: Long, bufferedMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    if (DerbisIsTorque) {
        TorqueStrip(positionMs, durationMs, onSeek, modifier)
        return
    }
    if (DerbisIsYouTubeMusic) {
        YouTubeMusicSeekbar(positionMs, bufferedMs, durationMs, onSeek, modifier)
        return
    }
    if (PlayerAppleMusicStyle) {
        AppleMusicSeekbar(positionMs, bufferedMs, durationMs, onSeek, modifier)
        return
    }
    if (DerbisIsSpotify) {
        SpotifySeekbar(positionMs, bufferedMs, durationMs, onSeek, modifier)
        return
    }
    if (DerbisIsIvory) {
        IvorySeekbar(positionMs, bufferedMs, durationMs, onSeek, modifier)
        return
    }
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val sliderState = rememberSliderState(value = dragging ?: fraction, trackRange = 0f..1f)
    val seekLabel = LocalDerbisStrings.current.gestureActionSeek
    val trackShape = RoundedCornerShape(PrismDesign.seekbarHeight)
    SideEffect { sliderState.value = dragging ?: fraction }
    Column(modifier.fillMaxWidth()) {
        Slider(
            state = sliderState, onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let(onSeek); dragging = null },
            enabled = durationMs > 0L, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = seekLabel },
            thumb = { Box(Modifier.size(if (dragging != null) 18.dp else 12.dp).clip(CircleShape).background(DerbisText)) },
            track = { state ->
                Box(Modifier.fillMaxWidth().height(PrismDesign.seekbarHeight).clip(trackShape).background(PrismAccent.Hairline)) {
                    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                    Box(Modifier.fillMaxWidth(buffered).fillMaxHeight().background(DerbisText.copy(alpha = 0.10f)))
                    Box(Modifier.fillMaxWidth(state.value.coerceIn(0f, 1f)).fillMaxHeight().clip(trackShape).background(PrismAccent.gradient()))
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatSeekbarMillis(dragging?.let { (durationMs * it).toLong() } ?: positionMs), style = MaterialTheme.typography.labelMedium, color = DerbisMuted)
            Text(formatSeekbarMillis(durationMs), style = MaterialTheme.typography.labelMedium, color = DerbisMuted)
        }
    }
}
