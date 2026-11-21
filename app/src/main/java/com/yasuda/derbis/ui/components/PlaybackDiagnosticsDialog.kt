@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.yasuda.derbis.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.yasuda.derbis.player.PlaybackDiagnosticSnapshot
import com.yasuda.derbis.player.PlaybackDiagnosticStatus
import com.yasuda.derbis.ui.DerbisIsLight
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.pixelHardShadow
import com.yasuda.derbis.ui.pixelPaperGrid
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisInk
import com.yasuda.derbis.ui.theme.DerbisIsPixel
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisPanel
import com.yasuda.derbis.ui.theme.DerbisText
import com.yasuda.derbis.ui.theme.PixelDesign
import com.yasuda.derbis.ui.theme.PixelPalette
import com.yasuda.derbis.ui.theme.PixelifyFont
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val DIAGNOSTICS_REFRESH_MS = 1_000L

@Composable
internal fun PlaybackDiagnosticsDialog(
    capture: suspend () -> PlaybackDiagnosticSnapshot,
    onDismiss: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<PlaybackDiagnosticSnapshot?>(null) }

    LaunchedEffect(Unit) {
        while (isActive) {
            snapshot = capture()
            delay(DIAGNOSTICS_REFRESH_MS)
        }
    }

    val current = snapshot
    if (DerbisIsPixel) {
        PixelPlaybackDiagnosticsDialog(
            snapshot = current,
            strings = strings,
            onCopy = { current?.let { copyDiagnostics(context, it, strings.diagnosticsCopied) } },
            onDismiss = onDismiss
        )
        return
    }

    val light = DerbisIsLight
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (light) DerbisInk else Color(0xFF11131C),
        title = {
            Column {
                Text(strings.playbackDiagnostics, color = DerbisText, fontWeight = FontWeight.Black)
                Text(
                    text = current?.status?.let { strings.diagnosticStatusLabel(it) }
                        ?: strings.playbackDiagnosticsSubtitle,
                    color = DerbisMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            if (current == null) {
                Text(strings.playbackDiagnosticsSubtitle, color = DerbisMuted, fontWeight = FontWeight.SemiBold)
            } else {
                DiagnosticsBody(snapshot = current, strings = strings, light = light)
            }
        },
        confirmButton = {
            TextButton(
                enabled = current != null,
                onClick = { current?.let { copyDiagnostics(context, it, strings.diagnosticsCopied) } }
            ) {
                Text(strings.diagnosticsCopyReport, color = DerbisCyan, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close, color = DerbisMuted, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PixelPlaybackDiagnosticsDialog(
    snapshot: PlaybackDiagnosticSnapshot?,
    strings: DerbisStrings,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    val p = PixelPalette
    val statusText = snapshot?.status?.let { strings.diagnosticStatusLabel(it) }
        ?: strings.playbackDiagnosticsSubtitle
    val statusSolid = snapshot?.status == PlaybackDiagnosticStatus.HEALTHY ||
        snapshot?.status == PlaybackDiagnosticStatus.ERROR

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0.24f)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(end = PixelDesign.shadowOffset, bottom = PixelDesign.shadowOffset)
                .pixelHardShadow(offset = PixelDesign.shadowOffset, color = p.outline)
                .background(p.black, RectangleShape)
                .pixelPaperGrid()
                .border(PixelDesign.border, p.text, RectangleShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(p.panel, RectangleShape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer { rotationZ = 45f }
                        .background(p.pink, RectangleShape)
                        .border(2.dp, p.text, RectangleShape)
                )
                Text(
                    text = strings.playbackDiagnostics,
                    color = p.text,
                    fontFamily = PixelifyFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(if (statusSolid) p.cyan else p.ink, RectangleShape)
                        .border(1.dp, if (statusSolid) p.text else p.outline, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText.uppercase(Locale.ROOT),
                        color = if (statusSolid) p.ink else p.text,
                        fontFamily = PixelifyFont,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
            HorizontalDivider(thickness = PixelDesign.border, color = p.outline)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                if (snapshot == null) {
                    Text(
                        text = strings.playbackDiagnosticsSubtitle,
                        color = p.muted,
                        fontFamily = PixelifyFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    PixelDiagnosticsBody(snapshot = snapshot, strings = strings)
                }
            }

            HorizontalDivider(thickness = PixelDesign.border, color = p.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(p.panel, RectangleShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelDialogButton(
                    label = strings.close,
                    solid = false,
                    enabled = true,
                    onClick = onDismiss
                )
                PixelDialogButton(
                    label = strings.diagnosticsCopyReport,
                    solid = true,
                    enabled = snapshot != null,
                    onClick = onCopy
                )
            }
        }
    }
}

@Composable
private fun PixelDialogButton(
    label: String,
    solid: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val p = PixelPalette
    val bg = when {
        !enabled -> p.panelSoft
        solid -> p.cyan
        else -> p.ink
    }
    val fg = when {
        !enabled -> p.muted
        solid -> p.ink
        else -> p.text
    }
    val borderColor = if (solid && enabled) p.text else p.outline
    Box(
        modifier = Modifier
            .padding(end = 2.dp, bottom = 2.dp)
            .pixelHardShadow(offset = 2.dp, color = p.outline)
            .background(bg, RectangleShape)
            .border(PixelDesign.border, borderColor, RectangleShape)
            .then(
                if (enabled) {
                    Modifier.derbisPressable(onClick = onClick, onClickLabel = label, role = Role.Button)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            fontFamily = PixelifyFont,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun PixelDiagnosticsBody(snapshot: PlaybackDiagnosticSnapshot, strings: DerbisStrings) {
    val p = PixelPalette
    val sections = listOf(
        strings.diagnosticsSectionPlayback to snapshot.playbackRows(),
        strings.diagnosticsSectionFormats to snapshot.formatRows(),
        strings.diagnosticsSectionNetwork to snapshot.networkRows(),
        strings.diagnosticsSectionResolver to snapshot.resolverRows()
    )
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (snapshot.status == PlaybackDiagnosticStatus.IDLE && snapshot.trackId.isBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(p.ink, RectangleShape)
                        .border(1.dp, p.outline, RectangleShape)
                        .padding(10.dp)
                ) {
                    Text(
                        text = strings.diagnosticsNoPlayback,
                        color = p.muted,
                        fontFamily = PixelifyFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        sections.forEach { (title, rows) ->
            item(key = title) {
                PixelDiagnosticSection(title = title, rows = rows)
            }
        }
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = strings.diagnosticsPrivacyNote,
                color = p.muted,
                fontFamily = PixelifyFont,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PixelDiagnosticSection(title: String, rows: List<Pair<String, String>>) {
    val p = PixelPalette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(p.ink, RectangleShape)
            .border(1.dp, p.outline, RectangleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(p.panel, RectangleShape)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = title.uppercase(Locale.ROOT),
                color = p.text,
                fontFamily = PixelifyFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        }
        HorizontalDivider(thickness = 1.dp, color = p.outline)
        rows.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = label,
                    color = p.muted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontFamily = PixelifyFont,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(0.44f)
                )
                Text(
                    text = value,
                    color = p.text,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontFamily = PixelifyFont,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.56f)
                )
            }
            if (index != rows.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = p.panel)
            }
        }
    }
}

@Composable
private fun DiagnosticsBody(snapshot: PlaybackDiagnosticSnapshot, strings: DerbisStrings, light: Boolean) {
    val sections = listOf(
        strings.diagnosticsSectionPlayback to snapshot.playbackRows(),
        strings.diagnosticsSectionFormats to snapshot.formatRows(),
        strings.diagnosticsSectionNetwork to snapshot.networkRows(),
        strings.diagnosticsSectionResolver to snapshot.resolverRows()
    )
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (snapshot.status == PlaybackDiagnosticStatus.IDLE && snapshot.trackId.isBlank()) {
            item {
                Text(strings.diagnosticsNoPlayback, color = DerbisMuted, fontWeight = FontWeight.SemiBold)
            }
        }
        sections.forEach { (title, rows) ->
            item(key = title) {
                DiagnosticSection(title = title, rows = rows, light = light)
            }
        }
        item {
            Text(
                text = strings.diagnosticsPrivacyNote,
                color = DerbisMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DiagnosticSection(title: String, rows: List<Pair<String, String>>, light: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (light) DerbisPanel else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, color = DerbisText, fontSize = 13.sp, fontWeight = FontWeight.Black)
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = label,
                    color = DerbisMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(0.44f)
                )
                Text(
                    text = value,
                    color = DerbisText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.56f)
                )
            }
        }
    }
}

private fun DerbisStrings.diagnosticStatusLabel(status: PlaybackDiagnosticStatus): String = when (status) {
    PlaybackDiagnosticStatus.HEALTHY -> diagnosticsStatusHealthy
    PlaybackDiagnosticStatus.FALLBACK_HISTORY -> diagnosticsStatusFallback
    PlaybackDiagnosticStatus.ERROR -> diagnosticsStatusError
    PlaybackDiagnosticStatus.IDLE -> diagnosticsStatusIdle
}

private fun copyDiagnostics(context: Context, snapshot: PlaybackDiagnosticSnapshot, copiedMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Derbis playback diagnostics", snapshot.safeReport()))
    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
}
