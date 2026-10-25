package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.theme.TidalDesign

/**
 * An original Derbis editorial deck using the local TIDAL Style palette. It deliberately uses
 * only Derbis playback state and controls; it does not imply a TIDAL account or service link.
 */
@Composable
internal fun TidalMusicPlayerScene(
    track: Track,
    slots: PlayerDeckSlots,
    artworkCap: Dp,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF262626), TidalDesign.black)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val gutter = 24.dp
        val artSize = minOf(maxWidth - gutter * 2, maxHeight * .48f, artworkCap)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = gutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            slots.header()
            Text(
                text = "DERBIS PLAYING",
                color = TidalDesign.muted,
                fontSize = 11.sp,
                letterSpacing = 1.4.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = artSize)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                slots.stage(track, Modifier.size(artSize), 20.dp)
                slots.gestures(track, Modifier.matchParentSize())
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(track.title, color = TidalDesign.white, fontSize = 25.sp, maxLines = 1)
                Text(track.artist, color = TidalDesign.muted, fontSize = 18.sp, maxLines = 1)
            }
            slots.engagement(track)
            slots.progress(PlayerProgressStyle.Classic)
            // This direct transport mirrors the deck's large, high-contrast stage without adding
            // any third-party controls.
            TidalMusicTransport(
                isPlaying = isPlaying,
                isResolving = isResolving,
                shuffleOn = shuffleOn,
                repeatMode = repeatMode,
                labels = labels,
                onShuffle = onShuffle,
                onPrevious = onPrevious,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onRepeat = onRepeat
            )
            slots.dock(track)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TidalMusicTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: RepeatMode,
    labels: PlayerControlLabels,
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
        TidalModeButton(Icons.Rounded.Shuffle, labels.shuffle, shuffleOn, onShuffle)
        IconButton(onClick = onPrevious, modifier = Modifier.size(54.dp)) {
            Icon(Icons.Rounded.SkipPrevious, labels.previous, tint = TidalDesign.white, modifier = Modifier.size(36.dp))
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(TidalDesign.white)
                .clickable(role = Role.Button, onClick = onTogglePlay)
                .semantics { contentDescription = if (isPlaying) labels.pause else labels.play },
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) {
                LevyraLoadingIndicator(color = TidalDesign.black, modifier = Modifier.size(28.dp))
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = TidalDesign.black,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(54.dp)) {
            Icon(Icons.Rounded.SkipNext, labels.next, tint = TidalDesign.white, modifier = Modifier.size(36.dp))
        }
        TidalModeButton(
            if (repeatMode == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            labels.repeat,
            repeatMode != RepeatMode.Off,
            onRepeat
        )
    }
}

@Composable
private fun TidalModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, label, tint = if (active) TidalDesign.accent else TidalDesign.white, modifier = Modifier.size(25.dp))
    }
}
