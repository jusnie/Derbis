package com.yasuda.derbis.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.yasuda.derbis.ui.icons.AutoAwesome
import com.yasuda.derbis.ui.icons.Close
import androidx.compose.material.icons.rounded.Remove
import com.yasuda.derbis.ui.icons.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.data.DerbisBeatGridStore
import com.yasuda.derbis.domain.DerbisBeatGrid
import com.yasuda.derbis.domain.DerbisMixStudio
import com.yasuda.derbis.domain.DerbisMixTransitionPreset
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.LocalAnimationsEnabled
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import com.yasuda.derbis.viewmodel.DerbisMixStudioController
import com.yasuda.derbis.viewmodel.DerbisMixStudioSession

private val MixInk = Color.Black
private val MixPanel = Color(0xFF121212)
private val MixLine = Color(0xFF2B2B2B)
private val MixText = Color.White
private val MixMuted = Color(0xFFAAAAAA)
private val MixAccent = Color(0xFFEDEDED)
private val MixInner = Color(0xFF181818)
private val MixSync = Color(0xFF4BD08B)
private val MixSyncWarn = Color(0xFFE3A857)

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
    val context = LocalContext.current
    val gridStore = remember(context) { DerbisBeatGridStore(context) }
    // Grids are written while tracks play, so the editor keeps re-reading them instead of
    // showing a one-shot snapshot that stays stale for the rest of the session.
    var grids by remember(playlist.id) { mutableStateOf<Map<String, DerbisBeatGrid>>(emptyMap()) }
    LaunchedEffect(playlist.id, playlist.tracks) {
        while (true) {
            val loaded = buildMap {
                playlist.tracks.forEach { track ->
                    gridStore.gridFor(track.id)?.takeIf { it.isUsable }?.let { put(track.id, it) }
                }
            }
            if (loaded != grids) grids = loaded
            delay(2_000L)
        }
    }
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
                outgoingGrid = grids[pair.first.id],
                incomingGrid = grids[pair.second.id],
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
    outgoingGrid: DerbisBeatGrid? = null,
    incomingGrid: DerbisBeatGrid? = null,
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
            BeatSyncPanel(
                outgoingGrid = outgoingGrid,
                incomingGrid = incomingGrid,
                enabled = beatSyncEnabled,
                available = beatSyncAvailable,
                interactive = enabled,
                durationMs = setting?.durationMs ?: 6_000L,
                onToggle = onBeatSync
            )
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

/**
 * Beat Sync readout for one handoff. It shows the two detected tempos, the correction the engine
 * will apply and whether the pair can actually lock, so the switch never claims a sync it cannot
 * deliver.
 */
@Composable
private fun BeatSyncPanel(
    outgoingGrid: DerbisBeatGrid?,
    incomingGrid: DerbisBeatGrid?,
    enabled: Boolean,
    available: Boolean,
    interactive: Boolean,
    durationMs: Long,
    onToggle: (Boolean) -> Unit
) {
    val sync = remember(outgoingGrid, incomingGrid) {
        if (outgoingGrid != null && incomingGrid != null) {
            DerbisMixStudio.beatSync(outgoingGrid, incomingGrid)
        } else null
    }
    val outgoingBpm = outgoingGrid?.validBpm
    val incomingBpm = incomingGrid?.validBpm
    val aligned = sync?.aligned == true
    val ratio = sync?.takeIf { it.aligned }?.incomingTempo
    val animationsEnabled = LocalAnimationsEnabled.current
    val clock = rememberBeatClock(animationsEnabled)
    val statusColor = when {
        !available -> MixMuted
        aligned -> MixSync
        enabled -> MixSyncWarn
        else -> MixMuted
    }
    val status = when {
        !available -> "Off in Audio Settings"
        aligned && enabled -> "Locked · bar-aligned handoff"
        aligned -> "Grids ready · switch on to lock"
        enabled && (outgoingBpm == null || incomingBpm == null) -> "Listening for beat grids…"
        enabled -> "Tempo gap too wide to match"
        else -> "Off for this pair"
    }
    val detail = when {
        !available -> "Turn Beat Sync on in Audio Settings, then disable DJ Crossfade to use it here."
        aligned -> {
            val bpm = outgoingBpm ?: 0f
            val barMs = if (bpm > 0f) 60_000f / bpm * DerbisMixStudio.BEATS_PER_BAR else 0f
            val bars = if (barMs > 0f) (durationMs / barMs).roundToInt().coerceAtLeast(1) else 0
            "$bars-bar fade · both decks open on a bar line"
        }
        enabled && (outgoingBpm == null || incomingBpm == null) ->
            "Derbis analyzes each track's PCM while it plays; a grid lands after about 12 seconds."
        enabled -> "The two tempos are more than ±8% apart, so a matched handoff would sound wrong."
        else -> "Each pair can lock independently once both grids exist."
    }

    Surface(
        color = MixInner,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MixLine)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(statusColor)
                        .size(7.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "BEAT SYNC",
                        color = MixText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Text(status, color = statusColor, fontSize = 11.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = interactive && available
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                BpmReadout("OUT", outgoingBpm)
                Text(
                    "→",
                    color = MixMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
                BpmReadout("IN", incomingBpm)
                Spacer(Modifier.weight(1f))
                TempoDeltaChip(ratio = ratio, aligned = aligned, enabled = enabled)
            }
            if (aligned) {
                TempoRail(ratio = ratio ?: 1f, accent = if (enabled) MixSync else MixMuted)
                // The two decks drawn as two rows of beats: they tick together only when the
                // tempo really locks, so the sync is visible instead of just claimed.
                BeatLanes(
                    outgoingBpm = outgoingBpm,
                    incomingBpm = incomingBpm?.takeIf { it > 0f }?.times(ratio ?: 1f),
                    clock = clock,
                    outgoingAccent = MixText,
                    incomingAccent = if (enabled) MixSync else MixMuted
                )
            }
            Text(detail, color = MixMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

/**
 * Frame clock for the beat lanes. Reading it inside a Canvas draw keeps the animation in the
 * draw phase, so a pulsing bar never recomposes the sheet.
 */
@Composable
private fun rememberBeatClock(enabled: Boolean): MutableFloatState {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            clock.floatValue = 0f
            return@LaunchedEffect
        }
        var previous = 0L
        var elapsed = clock.floatValue
        while (true) {
            withFrameNanos { now ->
                if (previous != 0L) elapsed += (now - previous) / 1_000_000_000f
                previous = now
                clock.floatValue = elapsed
            }
        }
    }
    return clock
}

@Composable
private fun BeatLanes(
    outgoingBpm: Float?,
    incomingBpm: Float?,
    clock: MutableFloatState,
    outgoingAccent: Color,
    incomingAccent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BeatLane(
            label = "OUT",
            bpm = outgoingBpm,
            clock = clock,
            accent = outgoingAccent
        )
        BeatLane(
            label = "IN",
            bpm = incomingBpm,
            clock = clock,
            accent = incomingAccent
        )
    }
}

/** One bar of four beats; the pip under the playhead swells and fades on every beat. */
@Composable
private fun BeatLane(label: String, bpm: Float?, clock: MutableFloatState, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = MixMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.width(26.dp)
        )
        Canvas(modifier = Modifier.weight(1f).height(16.dp)) {
            val beatsPerSecond = (bpm ?: 0f) / 60f
            val continuous = clock.floatValue * beatsPerSecond
            val phase = continuous - floor(continuous)
            val activeSlot = (floor(continuous).toInt() % SLOTS + SLOTS) % SLOTS
            val slotWidth = size.width / SLOTS
            val centerY = size.height / 2f
            for (index in 0 until SLOTS) {
                val isActive = index == activeSlot && bpm != null
                val pulse = if (isActive) (1f - phase).coerceIn(0f, 1f) else 0f
                drawCircle(
                    color = if (isActive) accent.copy(alpha = .30f + .70f * pulse) else MixLine,
                    radius = (2.4f + 3.4f * pulse) * density,
                    center = Offset(slotWidth * index + slotWidth / 2f, centerY)
                )
            }
        }
    }
}

private const val SLOTS = 4

@Composable
private fun BpmReadout(label: String, bpm: Float?) {
    Column {
        Text(
            label,
            color = MixMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = bpm?.let { ((it * 10f).roundToInt() / 10f).toString() } ?: "—",
                color = if (bpm != null) MixText else MixMuted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "BPM",
                color = MixMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

@Composable
private fun TempoDeltaChip(ratio: Float?, aligned: Boolean, enabled: Boolean) {
    val tint = when {
        ratio == null || !aligned -> MixMuted
        !enabled -> MixMuted
        abs(ratio - 1f) < 0.0005f -> MixSync
        else -> MixSync
    }
    val text = when {
        ratio == null || !aligned -> "—"
        abs(ratio - 1f) < 0.0005f -> "MATCH"
        else -> {
            val percent = (abs(ratio - 1f) * 1_000f).roundToInt() / 10f
            (if (ratio > 1f) "+" else "−") + percent + "%"
        }
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Small ±10% rail with a tick at 1.00x so the correction is readable at a glance. */
@Composable
private fun TempoRail(ratio: Float, accent: Color) {
    val normalized = ((ratio - 0.90f) / 0.20f).coerceIn(0f, 1f)
    Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
        val y = size.height / 2f
        drawLine(
            color = MixLine,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = MixText.copy(alpha = 0.28f),
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = 2f
        )
        drawCircle(
            color = accent,
            radius = 5f,
            center = Offset(size.width * normalized, y)
        )
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
