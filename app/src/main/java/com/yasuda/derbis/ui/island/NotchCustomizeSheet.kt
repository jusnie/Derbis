package com.yasuda.derbis.ui.island

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yasuda.derbis.domain.DerbisNotchSettings
import com.yasuda.derbis.domain.DerbisNotchStyle
import com.yasuda.derbis.domain.NotchAlignment
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisInk
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisText

private val NotchSheetPanel = Color.White.copy(alpha = 0.04f)
private val NotchSheetLine = Color.White.copy(alpha = 0.10f)

/**
 * The Derbis Notch customize screen.
 *
 * Every switch here edits one thing the island shows for the music that is playing, and the
 * preview above them is the real island — same composable, same settings — so nothing has to be
 * imagined. The Pill / Card chips exist because the island has two shapes and one preview box.
 */
@Composable
internal fun NotchCustomizeSheet(
    settings: DerbisNotchSettings,
    onSettings: (DerbisNotchSettings) -> Unit,
    preview: NotchPreview?,
    onDismiss: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    var previewExpanded by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = DerbisInk, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.close, tint = DerbisText)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.derbisNotchTitle,
                            color = DerbisText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = strings.derbisNotchCustomizeHint,
                            color = DerbisMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NotchPreviewPanel(
                        preview = preview,
                        settings = settings,
                        expanded = previewExpanded,
                        onExpanded = { previewExpanded = it }
                    )

                    NotchToggleRow(
                        title = strings.derbisNotchArtwork,
                        subtitle = strings.derbisNotchArtworkHint,
                        checked = settings.showArtwork,
                        onCheckedChange = { onSettings(settings.copy(showArtwork = it)) }
                    )
                    NotchToggleRow(
                        title = strings.derbisNotchLyrics,
                        subtitle = strings.derbisNotchLyricsHint,
                        checked = settings.showLyrics,
                        onCheckedChange = { onSettings(settings.copy(showLyrics = it)) }
                    )
                    if (settings.showLyrics) {
                        NotchStyleRow(
                            style = settings.style,
                            onStyle = { onSettings(settings.copy(style = it)) }
                        )
                    }
                    NotchPositionRow(
                        alignment = settings.alignment,
                        offsetYDp = settings.offsetYDp,
                        onAlignment = { onSettings(settings.copy(alignment = it)) },
                        onOffset = { onSettings(settings.copy(offsetYDp = it)) }
                    )
                    NotchToggleRow(
                        title = strings.derbisNotchTransport,
                        subtitle = strings.derbisNotchTransportHint,
                        checked = settings.showTransport,
                        onCheckedChange = { onSettings(settings.copy(showTransport = it)) }
                    )
                    NotchToggleRow(
                        title = strings.derbisNotchProgress,
                        subtitle = strings.derbisNotchProgressHint,
                        checked = settings.showProgress,
                        onCheckedChange = { onSettings(settings.copy(showProgress = it)) }
                    )
                    NotchToggleRow(
                        title = strings.derbisNotchWaveform,
                        subtitle = strings.derbisNotchWaveformHint,
                        checked = settings.showWaveform,
                        onCheckedChange = { onSettings(settings.copy(showWaveform = it)) }
                    )
                    NotchToggleRow(
                        title = strings.derbisNotchHaptics,
                        subtitle = strings.derbisNotchHapticsHint,
                        checked = settings.haptics,
                        onCheckedChange = { onSettings(settings.copy(haptics = it)) }
                    )

                    Text(
                        text = strings.derbisNotchMusicOnlyNote,
                        color = DerbisMuted.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotchPreviewPanel(
    preview: NotchPreview?,
    settings: DerbisNotchSettings,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit
) {
    val strings = LocalDerbisStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NotchSheetPanel)
            .border(1.dp, NotchSheetLine, RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            PreviewChip(strings.derbisNotchPreviewPill, !expanded) { onExpanded(false) }
            PreviewChip(strings.derbisNotchPreviewCard, expanded) { onExpanded(true) }
        }
        if (preview == null) {
            Box(
                modifier = Modifier
                    .height(if (expanded) 190.dp else 56.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.derbisNotchPreviewEmpty,
                    color = DerbisMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // The island itself, not a drawing of it. Nothing inside reacts: a preview that took
            // the switch tap you just made would be a bug, not a feature. It is placed exactly the
            // way the running island is, so the position chips do something visible.
            val align = when (settings.alignment) {
                NotchAlignment.Start -> Alignment.TopStart
                NotchAlignment.End -> Alignment.TopEnd
                NotchAlignment.Center -> Alignment.TopCenter
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                DerbisMusicNotch(
                    preview = preview,
                    settings = settings,
                    onTogglePlay = {},
                    onPrevious = {},
                    onNext = {},
                    onOpen = {},
                    interactive = false,
                    startExpanded = expanded,
                    modifier = Modifier
                        .align(align)
                        .padding(
                            top = settings.offsetYDp.dp,
                            start = if (settings.alignment == NotchAlignment.Start) NotchSideInsetDp.dp else 0.dp,
                            end = if (settings.alignment == NotchAlignment.End) NotchSideInsetDp.dp else 0.dp
                        )
                )
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun PreviewChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) DerbisInk else DerbisMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) DerbisText else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

@Composable
private fun NotchToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NotchSheetPanel)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DerbisText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = DerbisMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DerbisInk,
                checkedTrackColor = DerbisCyan,
                uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                uncheckedThumbColor = DerbisMuted,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

/**
 * Placement. Three chips for the horizontal slot, then a nudge for how far below the status bar the
 * island sits — the fix for phones whose cutout is a punch-hole rather than a centred notch.
 */
@Composable
private fun NotchPositionRow(
    alignment: NotchAlignment,
    offsetYDp: Int,
    onAlignment: (NotchAlignment) -> Unit,
    onOffset: (Int) -> Unit
) {
    val strings = LocalDerbisStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NotchSheetPanel)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.derbisNotchPositionTitle, color = DerbisText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(strings.derbisNotchPositionHint, color = DerbisMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StyleChip(strings.derbisNotchPositionStart, alignment == NotchAlignment.Start) {
                    onAlignment(NotchAlignment.Start)
                }
                StyleChip(strings.derbisNotchPositionCenter, alignment == NotchAlignment.Center) {
                    onAlignment(NotchAlignment.Center)
                }
                StyleChip(strings.derbisNotchPositionEnd, alignment == NotchAlignment.End) {
                    onAlignment(NotchAlignment.End)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(strings.derbisNotchOffset, color = DerbisMuted, fontSize = 11.sp)
            Slider(
                value = offsetYDp.toFloat(),
                onValueChange = { onOffset(it.toInt()) },
                valueRange = 0f..NotchMaxOffsetYDp.toFloat(),
                steps = NotchMaxOffsetYDp - 1,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = DerbisText,
                    activeTrackColor = DerbisText,
                    inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                )
            )
            Text("${offsetYDp}dp", color = DerbisMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NotchStyleRow(style: DerbisNotchStyle, onStyle: (DerbisNotchStyle) -> Unit) {
    val strings = LocalDerbisStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NotchSheetPanel)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(strings.derbisNotchStyleTitle, color = DerbisText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(strings.derbisNotchStyleHint, color = DerbisMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StyleChip(strings.derbisNotchStyleDefault, style == DerbisNotchStyle.Default) {
                onStyle(DerbisNotchStyle.Default)
            }
            StyleChip(strings.derbisNotchStyleSpicy, style == DerbisNotchStyle.Spicy) {
                onStyle(DerbisNotchStyle.Spicy)
            }
        }
    }
}

@Composable
private fun StyleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) DerbisInk else DerbisMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) DerbisText else Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/** One-line summary of what the island is currently allowed to show, for the settings row. */
internal fun notchSummary(settings: DerbisNotchSettings, strings: com.yasuda.derbis.ui.i18n.DerbisStrings): String {
    val parts = buildList {
        if (settings.showArtwork) add(strings.derbisNotchArtwork)
        if (settings.showLyrics) add(strings.derbisNotchLyrics)
        if (settings.showTransport) add(strings.derbisNotchTransport)
        if (settings.showProgress) add(strings.derbisNotchProgress)
        if (settings.showWaveform) add(strings.derbisNotchWaveform)
        if (settings.haptics) add(strings.derbisNotchHaptics)
    }
    return if (parts.isEmpty()) strings.derbisNotchNothingShown else parts.joinToString(" · ")
}
