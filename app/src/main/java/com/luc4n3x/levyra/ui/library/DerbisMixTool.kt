package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.DerbisMixTransitionPreset
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText

/** Original two-song transition tool. It does not expose or claim Beat Sync. */
@Composable
internal fun DerbisMixToolDialog(
    tracks: List<Track>,
    onDismiss: () -> Unit,
    onStartMix: (Track, Track, DerbisMixTransitionPreset, Long) -> Unit
) {
    var music1 by remember { mutableStateOf<Track?>(tracks.firstOrNull()) }
    var music2 by remember { mutableStateOf<Track?>(tracks.drop(1).firstOrNull()) }
    var selectingSlot by remember { mutableStateOf<Int?>(null) }
    var preset by remember { mutableStateOf(DerbisMixTransitionPreset.Auto) }
    var durationMs by remember { mutableStateOf(6_000L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LevyraInk,
        titleContentColor = LevyraText,
        textContentColor = LevyraText,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Tune, null, tint = LevyraCyan)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Derbis Mix", fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("Two-song transition tool", color = LevyraMuted, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close", tint = LevyraText) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Choose Music 1 and Music 2, then play a real transition. Beat Sync remains a separate Mix Studio feature.",
                    color = LevyraMuted,
                    fontSize = 13.sp
                )
                MixToolSongButton("Music 1", music1, onClick = { selectingSlot = 1 })
                MixToolSongButton("Music 2", music2, onClick = { selectingSlot = 2 })
                Text("Transition", color = LevyraText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(DerbisMixTransitionPreset.Auto, DerbisMixTransitionPreset.Fade, DerbisMixTransitionPreset.Rise).forEach { option ->
                        FilterChip(
                            selected = preset == option,
                            onClick = { preset = option },
                            label = { Text(option.name) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(DerbisMixTransitionPreset.Blend, DerbisMixTransitionPreset.Cut).forEach { option ->
                        FilterChip(
                            selected = preset == option,
                            onClick = { preset = option },
                            label = { Text(option.name) }
                        )
                    }
                }
                Text(
                    if (preset == DerbisMixTransitionPreset.Cut) "Cut uses a fixed, quick 0.8-second handoff."
                    else "Handoff length",
                    color = LevyraMuted,
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3_000L, 6_000L, 10_000L).forEach { value ->
                        FilterChip(
                            selected = durationMs == value,
                            enabled = preset != DerbisMixTransitionPreset.Cut,
                            onClick = { durationMs = value },
                            label = { Text("${value / 1_000}s") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = music1 != null && music2 != null && music1 != music2,
                onClick = {
                    onStartMix(
                        music1!!,
                        music2!!,
                        preset,
                        if (preset == DerbisMixTransitionPreset.Cut) 800L else durationMs
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = Color.Black)
            ) { Text("Play transition", fontWeight = FontWeight.Black) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )

    val slot = selectingSlot
    if (slot != null) {
        AlertDialog(
            onDismissRequest = { selectingSlot = null },
            containerColor = LevyraInk,
            title = { Text(if (slot == 1) "Choose Music 1" else "Choose Music 2", color = LevyraText, fontWeight = FontWeight.Black) },
            text = {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(tracks, key = { "mix-tool-${it.id}-${it.videoUrl}" }) { track ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (slot == 1) music1 = track else music2 = track
                                selectingSlot = null
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = LevyraPanel, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(34.dp)) {}
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = LevyraText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = { OutlinedButton(onClick = { selectingSlot = null }) { Text("Done") } }
        )
    }
}

@Composable
private fun MixToolSongButton(label: String, track: Track?, onClick: () -> Unit) {
    Surface(
        color = LevyraPanel,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, color = LevyraCyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(track?.title ?: "Choose a song", color = LevyraText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track?.artist.orEmpty(), color = LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun DerbisMixToolCard(trackCount: Int, onClick: () -> Unit) {
    Surface(
        color = LevyraPanel,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = LevyraCyan.copy(alpha = .16f), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Tune, null, tint = LevyraCyan, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Derbis Mix", color = LevyraText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    if (trackCount >= 2) "Choose two songs and shape their transition" else "Add at least two songs to use this tool",
                    color = LevyraMuted,
                    fontSize = 12.sp
                )
            }
            Text("Open", color = LevyraCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
