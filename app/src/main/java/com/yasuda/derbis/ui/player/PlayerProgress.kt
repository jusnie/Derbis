@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.yasuda.derbis.ui.player

import com.yasuda.derbis.ui.theme.*
import com.yasuda.derbis.ui.theme.DerbisIsNocturne

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yasuda.derbis.player.PlaybackService
import com.yasuda.derbis.player.waveseek.WaveSeekStore
import com.yasuda.derbis.ui.components.PremiumSeekbar
import com.yasuda.derbis.ui.components.SeekbarThumbShape
import com.yasuda.derbis.ui.components.DerbisWavySeekbar
import com.yasuda.derbis.ui.components.formatSeekbarMillis
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign

internal enum class PlayerProgressStyle {
    Classic,
    Wavy,
    Expressive
}

@Composable
internal fun PlayerProgress(
    positionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    activeColor: Color,
    secondaryColor: Color,
    surfaces: PlayerSurfaceTokens,
    isPlaying: Boolean,
    playbackSpeed: Float,
    animationsEnabled: Boolean,
    motionActive: Boolean,
    compact: Boolean,
    crossfadeSeconds: Int,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    style: PlayerProgressStyle = PlayerProgressStyle.Classic
) {
    if (PlayerPrismStyle) {
        PrismSeekbar(positionMs, bufferedPositionMs, durationMs, onSeek, modifier)
        return
    }
    val context = LocalContext.current.applicationContext
    val activePlayer by PlaybackService.activePlayerFlow.collectAsStateWithLifecycle()
    val crossfade by PlaybackService.crossfadeProgressFlow.collectAsStateWithLifecycle()
    val revision by WaveSeekStore.revision.collectAsStateWithLifecycle()
    val crossfadeStartFraction = remember(durationMs, crossfadeSeconds) {
        val fadeMs = crossfadeSeconds.coerceIn(0, 30) * 1_000L
        if (durationMs > 0L && fadeMs > 0L && fadeMs < durationMs) {
            ((durationMs - fadeMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
    }
    val rgbMotion = crossfade.active && animationsEnabled && motionActive &&
        LocalDerbisVisualCapabilities.current.decorativeMotion
    val rgb = if (rgbMotion) {
        val rainbow = rememberInfiniteTransition(label = "player-crossfade-position-rgb")
        val hue by rainbow.animateFloat(
            initialValue = 285f,
            targetValue = 645f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 1_350, easing = LinearEasing)),
            label = "player-crossfade-position-hue"
        )
        Color.hsv(hue % 360f, saturation = 0.90f, value = 1f)
    } else {
        secondaryColor
    }
    val mediaId = activePlayer?.currentMediaItem?.mediaId.orEmpty()
    val waveform = remember(context, mediaId, durationMs, compact, revision) {
        WaveSeekStore.load(context, mediaId, durationMs)
            ?.bars(if (compact) 72 else 96)
    }

    val appearance = remember(style, activeColor, secondaryColor, motionActive, waveform, DerbisIsNocturne) {
        playerProgressAppearance(style, activeColor, secondaryColor, motionActive, waveform)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (appearance.wavy) {
            DerbisWavySeekbar(
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                animated = animationsEnabled && motionActive,
                activeColor = activeColor,
                inactiveColor = DerbisPlayerDesign.TrackInactive,
                crossfadeStartFraction = crossfadeStartFraction,
                crossfadeActive = crossfade.active,
                crossfadeColor = rgb,
                onSeek = onSeek
            )
        } else {
            PremiumSeekbar(
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                onSeekTo = { seekMs ->
                    if (durationMs > 0L) {
                        onSeek((seekMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f))
                    }
                },
                activeColor = activeColor,
                trailingColor = appearance.trailing,
                thumbColor = appearance.thumb,
                inactiveColor = DerbisPlayerDesign.TrackInactive,
                animated = animationsEnabled,
                motionActive = appearance.motion,
                waveform = appearance.waveform,
                interactionKey = mediaId,
                thumbShape = appearance.thumbShape,
                crossfadeStartFraction = crossfadeStartFraction,
                crossfadeActive = crossfade.active,
                crossfadeColor = rgb
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -DerbisPlayerDesign.SpaceXs)
                .padding(horizontal = DerbisPlayerDesign.SpaceXxs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerTimeLabel(formatSeekbarMillis(positionMs), surfaces.contentMuted, compact)
            if (crossfade.active) {
                Text(
                    text = "Crossfading",
                    color = rgb,
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    maxLines = 1
                )
            }
            PlayerTimeLabel(
                if (durationMs > 0L) formatSeekbarMillis(durationMs) else "--:--",
                surfaces.contentFaint,
                compact
            )
        }
    }
}

private val TabularTimeStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun PlayerTimeLabel(text: String, color: Color, compact: Boolean) {
    Text(
        text = text,
        color = color,
        fontSize = if (compact) 11.sp else 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
        style = TabularTimeStyle
    )
}

private class PlayerProgressAppearance(
    val wavy: Boolean, val trailing: Color, val thumb: Color, val motion: Boolean,
    val waveform: FloatArray?, val thumbShape: SeekbarThumbShape
)

private fun playerProgressAppearance(style: PlayerProgressStyle, active: Color, secondary: Color, motion: Boolean, waveform: FloatArray?): PlayerProgressAppearance {
    val thumb = if (style == PlayerProgressStyle.Expressive) SeekbarThumbShape.Notched else SeekbarThumbShape.Round
    return if (DerbisIsNocturne) PlayerProgressAppearance(false, active, active, false, null, thumb)
    else PlayerProgressAppearance(style == PlayerProgressStyle.Wavy, secondary, Color.White, motion, waveform, thumb)
}
