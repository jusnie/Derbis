package com.luc4n3x.derbis.ui.update

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.derbis.domain.AppUpdateInfo
import com.luc4n3x.derbis.ui.i18n.DerbisStrings
import com.luc4n3x.derbis.ui.i18n.updateReleaseCopy
import com.luc4n3x.derbis.ui.i18n.updateReleaseHighlights
import com.luc4n3x.derbis.ui.theme.DerbisCyan
import com.luc4n3x.derbis.ui.theme.DerbisGlassBorder
import com.luc4n3x.derbis.ui.theme.DerbisMuted
import com.luc4n3x.derbis.ui.theme.DerbisPanel
import com.luc4n3x.derbis.ui.theme.DerbisPanelSoft
import com.luc4n3x.derbis.ui.theme.DerbisText
import com.luc4n3x.derbis.ui.theme.DerbisViolet
import com.luc4n3x.derbis.update.formatUpdateDuration
import com.luc4n3x.derbis.update.formatUpdateTransferLine
import com.luc4n3x.derbis.update.updateUnitsFor
import com.luc4n3x.derbis.update.updateProgressPercent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

sealed interface DerbisUpdatePhase {
    data object Idle : DerbisUpdatePhase

    data class Available(val update: AppUpdateInfo) : DerbisUpdatePhase

    data class Preparing(val versionName: String) : DerbisUpdatePhase

    data class Downloading(
        val versionName: String,
        val downloadedBytes: Long,
        val totalBytes: Long?,
        val bytesPerSecond: Double?,
        val elapsedMs: Long
    ) : DerbisUpdatePhase

    data class Ready(val versionName: String) : DerbisUpdatePhase

    data class Installing(val versionName: String) : DerbisUpdatePhase

    data class PermissionRequired(val versionName: String) : DerbisUpdatePhase

    data class Failed(val versionName: String) : DerbisUpdatePhase
}

@Composable
fun DerbisUpdateBanner(
    phase: DerbisUpdatePhase,
    strings: DerbisStrings,
    onUpdate: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (phase is DerbisUpdatePhase.Idle) return
    var notesExpanded by rememberSaveable(phaseIdentity(phase)) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = BannerShape)
            .clip(BannerShape)
            .background(DerbisPanel)
            .background(
                Brush.linearGradient(
                    listOf(
                        DerbisCyan.copy(alpha = if (phase is DerbisUpdatePhase.Available) 0.16f else 0.10f),
                        DerbisViolet.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )
            .border(width = 1.dp, color = DerbisGlassBorder, shape = BannerShape)
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 12.dp)) {
            BannerHeader(
                phase = phase,
                title = phaseTitle(phase, strings),
                version = phaseVersion(phase),
                dismissLabel = strings.close,
                onDismiss = onDismiss.takeIf { isDismissible(phase) }
            )
            BannerBody(phase = phase, strings = strings, notesExpanded = notesExpanded)
            BannerActions(
                phase = phase,
                strings = strings,
                notesExpanded = notesExpanded,
                onToggleNotes = { notesExpanded = !notesExpanded },
                onUpdate = onUpdate,
                onCancel = onCancel,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun BannerHeader(
    phase: DerbisUpdatePhase,
    title: String,
    version: String?,
    dismissLabel: String,
    onDismiss: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(updateBadgeShape())
                .background(if (phase is DerbisUpdatePhase.Failed) DerbisGlassBorder else DerbisCyan),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SystemUpdateAlt,
                contentDescription = null,
                tint = if (phase is DerbisUpdatePhase.Failed) DerbisMuted else UpdateOnAccent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DerbisText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!version.isNullOrBlank()) {
                Text(
                    text = version,
                    color = DerbisCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(CircleShape)
                        .background(DerbisCyan.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        if (onDismiss != null) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = dismissLabel,
                    tint = DerbisMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}

@Composable
private fun BannerBody(
    phase: DerbisUpdatePhase,
    strings: DerbisStrings,
    notesExpanded: Boolean
) {
    when (phase) {
        is DerbisUpdatePhase.Available -> {
            val notes = remember(strings.code, phase.update.latestVersionName) {
                strings.updateReleaseHighlights(phase.update.latestVersionName)
                    .joinToString(separator = "\n")
            }
            if (notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notes,
                    color = DerbisMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = if (notesExpanded) Int.MAX_VALUE else 2,
                    overflow = if (notesExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = if (notesExpanded) {
                        Modifier
                            .heightIn(max = 176.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(end = 10.dp)
                    } else {
                        Modifier.padding(end = 10.dp)
                    }
                )
            }
            val protection = strings.updateReleaseCopy()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 10.dp)
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = DerbisCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = protection.releaseProtection,
                    color = DerbisMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        is DerbisUpdatePhase.Downloading -> {
            val percent = updateProgressPercent(phase.downloadedBytes, phase.totalBytes)
            Spacer(modifier = Modifier.height(10.dp))
            if (percent != null) DeterminateTrack(fraction = percent / 100f) else IndeterminateTrack()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatUpdateTransferLine(
                        downloadedBytes = phase.downloadedBytes,
                        totalBytes = phase.totalBytes,
                        bytesPerSecond = phase.bytesPerSecond,
                        units = updateUnitsFor(strings.code)
                    ),
                    color = DerbisMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                val trailing = percent?.let { "$it%" } ?: formatUpdateDuration(phase.elapsedMs, updateUnitsFor(strings.code))
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = trailing,
                        color = DerbisText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        is DerbisUpdatePhase.PermissionRequired -> {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.updateAllowInstalls,
                color = DerbisMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(end = 10.dp)
            )
        }

        is DerbisUpdatePhase.Preparing,
        is DerbisUpdatePhase.Installing,
        is DerbisUpdatePhase.Ready -> {
            Spacer(modifier = Modifier.height(10.dp))
            IndeterminateTrack()
        }

        is DerbisUpdatePhase.Failed,
        DerbisUpdatePhase.Idle -> Unit
    }
}

@Composable
private fun BannerActions(
    phase: DerbisUpdatePhase,
    strings: DerbisStrings,
    notesExpanded: Boolean,
    onToggleNotes: () -> Unit,
    onUpdate: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (phase) {
            is DerbisUpdatePhase.Available -> {
                QuietAction(
                    label = if (notesExpanded) strings.close else strings.whatsNew,
                    onClick = onToggleNotes
                )
                PrimaryAction(label = strings.update, onClick = onUpdate)
            }

            is DerbisUpdatePhase.Preparing,
            is DerbisUpdatePhase.Downloading -> QuietAction(label = strings.cancel, onClick = onCancel)

            is DerbisUpdatePhase.PermissionRequired -> PrimaryAction(label = strings.update, onClick = onRetry)

            is DerbisUpdatePhase.Failed -> PrimaryAction(label = strings.updateRetry, onClick = onRetry)

            is DerbisUpdatePhase.Ready,
            is DerbisUpdatePhase.Installing,
            DerbisUpdatePhase.Idle -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuietAction(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = DerbisPanelSoft,
            contentColor = DerbisText
        ),
        shapes = ButtonDefaults.shapes()
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = DerbisCyan, contentColor = UpdateOnAccent),
        shapes = ButtonDefaults.shapes()
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeterminateTrack(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 10.dp)
            .height(6.dp)
            .clip(TrackShape)
            .background(DerbisText.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(TrackShape)
                .background(Brush.horizontalGradient(listOf(DerbisCyan, DerbisViolet)))
        )
    }
}

@Composable
private fun IndeterminateTrack() {
    val transition = rememberInfiniteTransition(label = "derbis-update-track")
    val offset by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "derbis-update-sweep"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 10.dp)
            .height(6.dp)
            .clip(TrackShape)
            .background(DerbisText.copy(alpha = 0.12f))
    ) {
        Layout(
            content = {
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .clip(TrackShape)
                        .background(Brush.horizontalGradient(listOf(DerbisCyan, DerbisViolet)))
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { measurables, constraints ->
            val trackWidth = constraints.maxWidth
            val segmentWidth = (trackWidth * 0.35f).toInt().coerceAtLeast(1)
            val placeable = measurables.first().measure(
                constraints.copy(minWidth = segmentWidth, maxWidth = segmentWidth)
            )
            layout(trackWidth, placeable.height) {
                placeable.placeRelative(x = (trackWidth * offset).toInt(), y = 0)
            }
        }
    }
}

internal data class DerbisUpdateNoteSection(
    val title: String,
    val entries: List<String>
)

/** Preserves authored release-note sections so the update screen can show Fixed before New. */
internal fun derbisUpdateNoteSections(notes: String, version: String): List<DerbisUpdateNoteSection> {
    val versionKey = version.trim().lowercase()
    val sections = mutableListOf<Pair<String, MutableList<String>>>()
    val unsectioned = mutableListOf<String>()
    var currentEntries: MutableList<String>? = null

    notes.lineSequence().forEach { raw ->
        val trimmed = raw.trim()
        val line = normalizedUpdateNoteLine(trimmed)
        if (line.isBlank() || line == "---" || !line.any { it.isLetterOrDigit() }) return@forEach
        val lower = line.lowercase()
        if (isRedundantUpdateHeading(lower, versionKey)) {
            currentEntries = null
            return@forEach
        }
        if (trimmed.startsWith('#')) {
            val entries = mutableListOf<String>()
            sections += line to entries
            currentEntries = entries
        } else {
            (currentEntries ?: unsectioned).add(line)
        }
    }

    val result = buildList {
        if (unsectioned.isNotEmpty()) add(DerbisUpdateNoteSection("", unsectioned.distinct()))
        sections.forEach { (title, entries) ->
            val distinct = entries.distinct()
            if (distinct.isNotEmpty()) add(DerbisUpdateNoteSection(title, distinct))
        }
    }
    return result.take(3).map { section -> section.copy(entries = section.entries.take(12)) }
}

private fun normalizedUpdateNoteLine(raw: String): String = raw
    .replace(Regex("^#{1,6}\\s*"), "")
    .replace(Regex("^[-*•]+\\s*"), "")
    .replace(Regex("\\*\\*([^*]+)\\*\\*")) { match -> match.groupValues[1] }
    .replace(Regex("`([^`]+)`")) { match -> match.groupValues[1] }
    .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)")) { match -> match.groupValues[1] }
    .replace("__", "")
    .replace("**", "")
    .trim()

private fun isRedundantUpdateHeading(lower: String, versionKey: String): Boolean =
    lower == "novità" ||
        lower == "changelog" ||
        lower.startsWith("derbis v$versionKey") ||
        lower.startsWith("derbis $versionKey") ||
        lower.startsWith("versione $versionKey")

internal fun derbisUpdateNoteLines(notes: String, version: String): List<String> {
    val versionKey = version.trim().lowercase()
    return notes
        .lineSequence()
        .map { it.trim() }
        .map(::normalizedUpdateNoteLine)
        .filter { line -> line.isNotBlank() && line != "---" && line.any { it.isLetterOrDigit() } }
        .filterNot { line -> isRedundantUpdateHeading(line.lowercase(), versionKey) }
        .distinct()
        .take(12)
        .toList()
}

private val BannerShape = RoundedCornerShape(28.dp)
private val UpdateOnAccent = Color(0xFF00201F)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun updateBadgeShape(): Shape = MaterialShapes.Cookie9Sided.toShape()
private val TrackShape = RoundedCornerShape(2.dp)

private fun phaseIdentity(phase: DerbisUpdatePhase): String = when (phase) {
    DerbisUpdatePhase.Idle -> "idle"
    is DerbisUpdatePhase.Available -> "available:${phase.update.latestVersionName}"
    is DerbisUpdatePhase.Preparing -> "preparing:${phase.versionName}"
    is DerbisUpdatePhase.Downloading -> "downloading:${phase.versionName}"
    is DerbisUpdatePhase.Ready -> "ready:${phase.versionName}"
    is DerbisUpdatePhase.Installing -> "installing:${phase.versionName}"
    is DerbisUpdatePhase.PermissionRequired -> "permission:${phase.versionName}"
    is DerbisUpdatePhase.Failed -> "failed:${phase.versionName}"
}

private fun phaseTitle(phase: DerbisUpdatePhase, strings: DerbisStrings): String = when (phase) {
    DerbisUpdatePhase.Idle -> ""
    is DerbisUpdatePhase.Available -> strings.newUpdate
    is DerbisUpdatePhase.Preparing -> strings.updatePreparing
    is DerbisUpdatePhase.Downloading -> strings.updateDownloading
    is DerbisUpdatePhase.Ready -> strings.updateReadyToInstall
    is DerbisUpdatePhase.Installing -> strings.updateInstalling
    is DerbisUpdatePhase.PermissionRequired -> strings.updateReadyToInstall
    is DerbisUpdatePhase.Failed -> strings.updateFailed
}

private fun phaseVersion(phase: DerbisUpdatePhase): String? = when (phase) {
    DerbisUpdatePhase.Idle -> null
    is DerbisUpdatePhase.Available -> phase.update.latestVersionName
    is DerbisUpdatePhase.Preparing -> phase.versionName
    is DerbisUpdatePhase.Downloading -> phase.versionName
    is DerbisUpdatePhase.Ready -> phase.versionName
    is DerbisUpdatePhase.Installing -> phase.versionName
    is DerbisUpdatePhase.PermissionRequired -> phase.versionName
    is DerbisUpdatePhase.Failed -> phase.versionName
}?.takeIf { it.isNotBlank() }

private fun isDismissible(phase: DerbisUpdatePhase): Boolean = when (phase) {
    is DerbisUpdatePhase.Available,
    is DerbisUpdatePhase.Failed,
    is DerbisUpdatePhase.PermissionRequired -> true
    else -> false
}
