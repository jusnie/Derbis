package com.yasuda.derbis.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.DerbisMixTransitionPreset
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.viewmodel.DerbisMixStudioController
import com.yasuda.derbis.viewmodel.DerbisMixStudioSession

private val MixInk = Color.Black
private val MixPanel = Color(0xFF121212)
private val MixLine = Color(0xFF2B2B2B)
private val MixText = Color.White
private val MixMuted = Color(0xFFAAAAAA)
private val MixAccent = Color(0xFFEDEDED)

/** Original playlist transition editor; it intentionally has no third-party visual language. */
@Composable
internal fun DerbisMixStudioScreen(
    session: DerbisMixStudioSession,
    controller: DerbisMixStudioController,
    beatSyncAvailable: Boolean,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val playlist = session.playlist
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MixInk)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "mix-header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DERBIS MIX STUDIO", color = MixMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.3.sp)
                    Text(playlist.name, color = MixText, fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (session.dirty || session.failed) {
                    Button(
                        onClick = controller::save,
                        enabled = !session.saving,
                        colors = ButtonDefaults.buttonColors(containerColor = MixAccent, contentColor = MixInk)
                    ) { Text(if (session.saving) "Saving" else "Save", fontWeight = FontWeight.Bold) }
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close Mix Studio", tint = MixText) }
            }
        }
        item(key = "mix-status") {
            Surface(color = MixPanel, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MixLine)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mixed playback", color = MixText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                if (session.draft.enabled) "Saved transitions will run for this playlist." else "Off — this playlist plays normally.",
                                color = MixMuted,
                                fontSize = 13.sp
                            )
                        }
                        Switch(checked = session.draft.enabled, onCheckedChange = controller::setEnabled, enabled = !session.saving)
                    }
                    Text(
                        if (beatSyncAvailable) {
                            "Derbis analyzes PCM locally for BPM and a beat-grid candidate while tracks play. Beat Sync only engages when both grids are confident; EQ and filter curves are not claimed here."
                        } else {
                            "Beat Sync is off in Audio Settings. Turn off DJ Crossfade, then enable Beat Sync to use verified BPM and beat-grid alignment."
                        },
                        color = MixMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
        item(key = "mix-auto") {
            Button(
                onClick = controller::applyAutoMix,
                enabled = playlist.tracks.size > 1 && !session.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MixAccent, contentColor = MixInk),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, null)
                Spacer(Modifier.width(10.dp))
                Text("Auto Mix this playlist", fontWeight = FontWeight.Bold)
            }
        }
        item(key = "mix-explainer") {
            Text(
                if (session.transitionCount > 0) "${session.transitionCount} transition points · choose a character and duration for each handoff."
                else "Add at least two tracks to create a transition plan.",
                color = MixMuted,
                fontSize = 13.sp
            )
        }
        itemsIndexed(playlist.tracks.zipWithNext(), key = { index, _ -> "mix-$index" }) { index, pair ->
            MixTransitionCard(
                index = index + 1,
                outgoing = pair.first,
                incoming = pair.second,
                setting = session.draft.settingFor(pair.first, pair.second),
                enabled = !session.saving,
                beatSyncAvailable = beatSyncAvailable,
                onPreset = { controller.setPreset(pair.first, pair.second, it) },
                onDuration = { controller.adjustDuration(pair.first, pair.second, it) },
                onBeatSync = { controller.setBeatSync(pair.first, pair.second, it) }
            )
        }
    }
}

@Composable
private fun MixTransitionCard(
    index: Int,
    outgoing: Track,
    incoming: Track,
    setting: com.yasuda.derbis.domain.DerbisMixTransitionSetting?,
    enabled: Boolean,
    beatSyncAvailable: Boolean,
    onPreset: (DerbisMixTransitionPreset) -> Unit,
    onDuration: (Long) -> Unit,
    onBeatSync: (Boolean) -> Unit
) {
    val preset = setting?.preset ?: DerbisMixTransitionPreset.Auto
    val durationSeconds = (setting?.durationMs ?: 6_000L) / 1_000L
    val beatSyncEnabled = setting?.beatSyncEnabled == true
    Surface(color = MixPanel, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MixLine)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TRANSITION ${index.toString().padStart(2, '0')}", color = MixMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            MixTrackLabel("OUT", outgoing)
            MixTrackLabel("IN", incoming)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BEAT ALIGN", color = MixText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        if (beatSyncAvailable) "Uses local BPM and beat-grid analysis when both tracks are ready."
                        else "Enable Beat Sync in Audio Settings after turning off DJ Crossfade.",
                        color = MixMuted,
                        fontSize = 11.sp
                    )
                }
                Switch(checked = beatSyncEnabled, onCheckedChange = onBeatSync, enabled = enabled && beatSyncAvailable)
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DerbisMixTransitionPreset.entries.forEach { option ->
                    TextButton(onClick = { onPreset(option) }, enabled = enabled) {
                        Text(
                            option.name,
                            color = if (option == preset) MixText else MixMuted,
                            fontWeight = if (option == preset) FontWeight.Black else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("OVERLAP", color = MixMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onDuration(-1_000L) },
                    enabled = enabled && preset != DerbisMixTransitionPreset.Cut && durationSeconds > 1L
                ) {
                    Icon(Icons.Rounded.Remove, "Shorten overlap", tint = MixText)
                }
                Text(
                    if (preset == DerbisMixTransitionPreset.Cut) "Instant" else "${durationSeconds}s",
                    color = MixText,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.widthIn(min = 50.dp)
                )
                IconButton(
                    onClick = { onDuration(1_000L) },
                    enabled = enabled && preset != DerbisMixTransitionPreset.Cut && durationSeconds < 30L
                ) {
                    Icon(Icons.Rounded.Add, "Lengthen overlap", tint = MixText)
                }
            }
        }
    }
}

@Composable
private fun MixTrackLabel(label: String, track: Track) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MixMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = MixText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = MixMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
