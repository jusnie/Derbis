@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.yasuda.derbis.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.player.PlaybackDiagnosticSnapshot
import com.yasuda.derbis.player.PlaybackDiagnosticStatus
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisText
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF11131C),
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
                DiagnosticsBody(snapshot = current, strings = strings)
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
private fun DiagnosticsBody(snapshot: PlaybackDiagnosticSnapshot, strings: DerbisStrings) {
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
                DiagnosticSection(title = title, rows = rows)
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
private fun DiagnosticSection(title: String, rows: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
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
