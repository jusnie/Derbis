package com.yasuda.derbis.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import com.yasuda.derbis.ui.components.derbisGlass
import com.yasuda.derbis.ui.components.derbisLightweightGlass
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.playerMix
import androidx.compose.foundation.shape.CutCornerShape
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisIsPixel
import com.yasuda.derbis.ui.theme.DerbisIsTorque
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics
import com.yasuda.derbis.ui.theme.PixelDesign
import com.yasuda.derbis.ui.theme.PixelPalette

/**
 * The Flow studio chip bar, ported from iOS (`DerbisFlowStudioChipBar`).
 *
 * Four compact capsules under the transport: Endless Listening, playback speed, the
 * centre-channel vocal cut, and a menu with the sleep timer, similar songs and the audio
 * studio. On the Liquid Glass skin the active capsules are real glass with the artwork
 * accent as their tint; elsewhere they fall back to the shared lightweight material so the
 * row still reads as a raised control strip.
 */
@Composable
internal fun PlayerStudioChipBar(
    radioEnabled: Boolean,
    playbackSpeed: Float,
    karaokeMode: Boolean,
    /** Non-null while a sleep timer runs (already rounded up), e.g. "23m" or the end-of-track marker. */
    sleepTimerLabel: String?,
    accent: Color,
    accentContent: Color,
    onToggleEndless: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleKaraoke: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenSimilarSongs: () -> Unit,
    onOpenAudioStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDerbisStrings.current
    val haptics = LocalDerbisHaptics.current
    val sleepActive = sleepTimerLabel != null
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioChip(
            icon = Icons.Rounded.Radio,
            label = strings.continuousRadio,
            active = radioEnabled,
            accent = accent,
            accentContent = accentContent,
            description = strings.continuousRadio,
            onClick = {
                haptics.perform(DerbisHapticAction.SeekSnap)
                onToggleEndless()
            },
            modifier = Modifier.weight(1f)
        )
        StudioChip(
            icon = Icons.Rounded.Speed,
            label = "${formatPlaybackSpeed(playbackSpeed)}x",
            active = playbackSpeed != 1f,
            accent = accent,
            accentContent = accentContent,
            description = "${strings.tempo}: ${formatPlaybackSpeed(playbackSpeed)}x",
            onClick = {
                haptics.perform(DerbisHapticAction.SeekSnap)
                onCycleSpeed()
            },
            modifier = Modifier.weight(1f)
        )
        StudioChip(
            icon = Icons.Rounded.Mic,
            label = strings.karaokeTitle,
            active = karaokeMode,
            accent = accent,
            accentContent = accentContent,
            description = strings.karaokeTitle,
            onClick = {
                haptics.perform(DerbisHapticAction.SeekSnap)
                onToggleKaraoke()
            },
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.weight(1f)) {
            StudioChip(
                icon = if (sleepActive) Icons.Rounded.Bedtime else Icons.Rounded.Tune,
                label = sleepTimerLabel ?: strings.audioQuality,
                active = sleepActive,
                accent = accent,
                accentContent = accentContent,
                description = if (sleepActive) "${strings.sleepTimer}: $sleepTimerLabel" else strings.audioQuality,
                onClick = {
                    haptics.perform(DerbisHapticAction.Confirm)
                    menuExpanded = true
                },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.widthIn(min = 220.dp)
            ) {
                DropdownMenuItem(
                    text = { Text(strings.sleepTimer) },
                    leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null) },
                    trailingIcon = sleepTimerLabel?.let { left ->
                        { Text(left, style = TextStyle(fontWeight = FontWeight.Bold)) }
                    },
                    onClick = {
                        menuExpanded = false
                        onOpenSleepTimer()
                    }
                )
                DropdownMenuItem(
                    text = { Text(strings.youMightAlsoLike) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onOpenSimilarSongs()
                    }
                )
                DropdownMenuItem(
                    text = { Text(strings.audioQuality) },
                    leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onOpenAudioStudio()
                    }
                )
            }
        }
    }
}

@Composable
private fun StudioChip(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    accentContent: Color,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pixel = DerbisIsPixel
    val torque = DerbisIsTorque
    val capsule = when {
        pixel -> RectangleShape
        torque -> CutCornerShape(6.dp)
        else -> DerbisPlayerDesign.ShapePill
    }
    val container: Modifier = if (active) {
        if (pixel) {
            Modifier.background(accent, capsule)
        } else {
            Modifier.derbisGlass(
                shape = capsule,
                baseTint = accent,
                fallbackColor = accent.playerMix(Color.White, 0.10f).copy(alpha = 0.92f),
                fallbackBorderColor = Color.White.copy(alpha = 0.34f),
                artworkTint = accent
            )
        }
    } else if (pixel) {
        Modifier
            .background(Color.White.copy(alpha = 0.08f), capsule)
            .border(PixelDesign.border, PixelPalette.outline, capsule)
    } else {
        // White-on-dark film with the edge light: quiet glass for an always-visible strip.
        Modifier.derbisLightweightGlass(
            shape = capsule,
            fill = Color.White.copy(alpha = 0.09f),
            rim = true
        )
    }
    val contentColor = if (active) accentContent else DerbisPlayerDesign.TextPrimary
    Row(
        modifier = modifier
            .heightIn(min = 38.dp)
            .then(container)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = description }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
