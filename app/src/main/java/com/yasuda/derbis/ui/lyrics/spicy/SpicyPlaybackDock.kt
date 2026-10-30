@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yasuda.derbis.ui.lyrics.spicy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.ui.components.DerbisIonicons
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings

@Composable
internal fun SpicyPlaybackDock(
    trackKey: String,
    durationMs: Long,
    positionProvider: () -> Long,
    isPlaying: Boolean,
    compact: Boolean,
    shareEnabled: Boolean,
    onSeekToMs: (Long) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDerbisStrings.current
    val transport: @Composable () -> Unit = {
        SpicyIconAction(DerbisIonicons.SkipPrevious, strings.previous, onPrevious)
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .size(if (compact) 48.dp else 56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SpicyGradientBrush)
        ) {
            Icon(
                imageVector = if (isPlaying) DerbisIonicons.Pause else DerbisIonicons.Play,
                contentDescription = if (isPlaying) strings.pause else strings.play,
                tint = SpicyInk,
                modifier = Modifier.size(23.dp)
            )
        }
        SpicyIconAction(DerbisIonicons.SkipNext, strings.next, onNext)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SpicyDockShape)
            .background(SpicySurface)
            .border(1.dp, SpicyPaper.copy(alpha = 0.08f), SpicyDockShape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpicyIconAction(DerbisIonicons.Share, strings.shareLyrics, onShare, enabled = shareEnabled)
                transport()
                SpicyPlaybackProgress(
                    trackKey, durationMs, positionProvider, onSeekToMs,
                    Modifier.weight(1f).padding(horizontal = 16.dp)
                )
                SpicyIconAction(DerbisIonicons.Queue, strings.queue, onOpenQueue)
            }
        } else {
            SpicyPlaybackProgress(trackKey, durationMs, positionProvider, onSeekToMs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpicyIconAction(DerbisIonicons.Share, strings.shareLyrics, onShare, enabled = shareEnabled)
                transport()
                SpicyIconAction(DerbisIonicons.Queue, strings.queue, onOpenQueue)
            }
        }
    }
}

@Composable
private fun SpicyPlaybackProgress(
    trackKey: String,
    durationMs: Long,
    positionProvider: () -> Long,
    onSeekToMs: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDerbisStrings.current
    var dragProgress by remember(trackKey) { mutableStateOf<Float?>(null) }
    val shownProgress = dragProgress ?: spicyTimelineProgress(positionProvider(), durationMs)
    val shownPosition = if (durationMs > 0L) spicySeekPosition(shownProgress, durationMs) else positionProvider()
    val sliderState = rememberSliderState(value = shownProgress, trackRange = 0f..1f)
    LaunchedEffect(trackKey, shownProgress) {
        val next = shownProgress.coerceIn(0f, 1f)
        if (sliderState.value != next) sliderState.value = next
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = formatSpicySeekMillis(shownPosition),
                    color = SpicyPaper,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (durationMs > 0L) formatSpicySeekMillis(durationMs) else "--:--",
                    color = SpicyMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
            Slider(
                state = sliderState,
                onValueChange = { value ->
                    sliderState.value = value
                    dragProgress = value
                },
                onValueChangeFinished = {
                    dragProgress?.let { onSeekToMs(spicySeekPosition(it, durationMs)) }
                    dragProgress = null
                },
                enabled = durationMs > 0L,
                thumb = {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(21.dp)
                            .background(if (durationMs > 0L) SpicyPaper else SpicyMuted, RoundedCornerShape(2.dp))
                    )
                },
                track = { slider ->
                    SpicyTickTimeline(
                        progress = slider.value,
                        enabled = durationMs > 0L,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = strings.playback }
            )
        }
    }
}

@Composable
internal fun SpicyIconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) SpicyAccentCoral.copy(alpha = 0.12f) else Color.Transparent)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (!enabled) SpicyMuted.copy(alpha = 0.4f) else if (selected) SpicyAccentCoral else SpicyPaper,
            modifier = Modifier.size(22.dp)
        )
    }
}
