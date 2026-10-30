@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.yasuda.derbis.ui.lyrics.studio

import com.yasuda.derbis.domain.LyricsRendererMode
import com.yasuda.derbis.ui.lyrics.spicy.SpicyStudioLivePreview
import com.yasuda.derbis.ui.lyrics.spicy.SpicyLyricsIcons
import com.yasuda.derbis.ui.lyrics.spicy.SpicyAccentCoral
import com.yasuda.derbis.ui.lyrics.spicy.SpicyAccentGold
import androidx.compose.material.icons.rounded.MusicNote

import com.yasuda.derbis.ui.components.DerbisBackButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yasuda.derbis.domain.DemoLyrics
import com.yasuda.derbis.domain.DerbisFontPreset
import com.yasuda.derbis.domain.DerbisLyricsStudioSettings
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.domain.LyricsActiveColorMode
import com.yasuda.derbis.domain.LyricsAlignment
import com.yasuda.derbis.domain.LyricsBackgroundMode
import com.yasuda.derbis.domain.LyricsFontWeightPreset
import com.yasuda.derbis.domain.LyricsGlowEffect
import com.yasuda.derbis.domain.LyricsLineSpacing
import com.yasuda.derbis.domain.LyricsStylePreset
import com.yasuda.derbis.domain.LyricsTextSize
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.KaraokeWordTimedText
import com.yasuda.derbis.ui.LocalAnimationsEnabled
import com.yasuda.derbis.ui.lyrics.buildTimedLyricText
import com.yasuda.derbis.ui.lyrics.wordHighlightTimings
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisInk
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.DerbisText
import com.yasuda.derbis.ui.theme.DerbisThemeAccents
import com.yasuda.derbis.ui.theme.DerbisTypeRhythm
import com.yasuda.derbis.ui.theme.DerbisViolet
import com.yasuda.derbis.ui.theme.derbisFontFamilyFor
import kotlinx.coroutines.delay

@Composable
internal fun LyricsStudioOverlay(
    settings: DerbisLyricsStudioSettings,
    currentTrack: Track?,
    liveLyrics: List<LyricLine>,
    lyricsSynced: Boolean,
    positionMs: Long,
    isPlaying: Boolean,
    onUpdateSettings: (DerbisLyricsStudioSettings) -> Unit,
    onResetSettings: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    var showResetDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DerbisInk)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = DerbisPlayerDesign.GutterCompact,
                end = DerbisPlayerDesign.GutterCompact,
                top = DerbisPlayerDesign.SpaceMd,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.SpaceLg)
        ) {
            // Header
            item(contentType = "studio-header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DerbisBackButton(onClick = onClose, contentDescription = strings.back)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = DerbisPlayerDesign.SpaceXs)
                    ) {
                        Text(
                            text = strings.lyricsStudio,
                            color = DerbisText,
                            fontSize = 26.sp,
                            lineHeight = DerbisTypeRhythm.lineHeight(26f),
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Choose between Default (Apple Music 13) and Spicy lyrics",
                            color = DerbisMuted,
                            fontSize = 13.sp,
                            lineHeight = DerbisTypeRhythm.lineHeight(13f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.size(DerbisPlayerDesign.MinimumTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = strings.lyricsStudioReset,
                            tint = DerbisMuted
                        )
                    }
                }
            }

            // Two-Way Option Selection (Default vs Spicy)
            item(contentType = "studio-renderer-options") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Lyrics Renderer",
                        color = DerbisText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Option 1: Default (Apple Music 13)
                    LyricsRendererOptionCard(
                        title = strings.lyricsRendererDefault,
                        badge = "DEFAULT",
                        description = strings.lyricsRendererDefaultDesc,
                        icon = Icons.Rounded.MusicNote,
                        selected = settings.isDefaultLyrics,
                        accentColor = DerbisCyan,
                        onClick = {
                            onUpdateSettings(settings.withRendererMode(LyricsRendererMode.DEFAULT))
                        }
                    )

                    // Option 2: Spicy (Spicy Lyrics)
                    LyricsRendererOptionCard(
                        title = strings.lyricsRendererSpicy,
                        badge = "SPICY",
                        description = strings.lyricsRendererSpicyDesc,
                        icon = SpicyLyricsIcons.Flame,
                        selected = settings.isSpicyLyrics,
                        accentColor = SpicyAccentCoral,
                        onClick = {
                            onUpdateSettings(settings.withRendererMode(LyricsRendererMode.SPICY))
                        }
                    )
                }
            }

            // Synchronized Live Preview Card
            item(contentType = "studio-preview") {
                LyricsStudioLivePreview(
                    settings = settings,
                    currentTrack = currentTrack,
                    liveLyrics = liveLyrics,
                    lyricsSynced = lyricsSynced,
                    playbackPositionMs = positionMs,
                    isPlaying = isPlaying
                )
            }

            // Tuning Options
            item(contentType = "studio-tuning") {
                StudioCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Renderer Options",
                            color = DerbisText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        StudioToggleRow(
                            title = "Word-by-Word Karaoke Sync",
                            subtitle = "Progressive real-time word highlight fill on synced verses",
                            checked = settings.progressiveKaraokeEnabled,
                            onCheckedChange = {
                                onUpdateSettings(settings.copy(progressiveKaraokeEnabled = it))
                            }
                        )

                        StudioToggleRow(
                            title = "Dynamic Depth Blur",
                            subtitle = "Progressive depth-of-field blur on background and inactive verses",
                            checked = settings.blurEnabled,
                            onCheckedChange = {
                                onUpdateSettings(settings.copy(blurEnabled = it))
                            }
                        )
                    }
                }
            }

            // Reset Button
            item(contentType = "studio-reset-action") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DerbisCyan
                        ),
                        border = BorderStroke(1.dp, DerbisCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = strings.lyricsStudioReset,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = strings.lyricsStudioReset,
                    fontWeight = FontWeight.Black,
                    color = DerbisText
                )
            },
            text = {
                Text(
                    text = strings.lyricsStudioResetConfirm,
                    color = DerbisMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetSettings()
                        showResetDialog = false
                    }
                ) {
                    Text(
                        text = strings.lyricsStudioReset,
                        color = DerbisCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(
                        text = strings.cancel,
                        color = DerbisMuted
                    )
                }
            },
            containerColor = Color(0xFF1E1E24),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun StudioSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = DerbisPlayerDesign.SpaceXs, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DerbisCyan,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            color = DerbisText,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
private fun PresetCard(
    preset: LyricsStylePreset,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val borderColor by animateColorAsState(
        targetValue = if (selected) DerbisCyan else Color.White.copy(alpha = 0.08f),
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(200)),
        label = "preset-border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) DerbisCyan.copy(alpha = 0.12f) else Color(0xFF16161A),
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(200)),
        label = "preset-bg"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .semantics {
                this.role = Role.RadioButton
                this.selected = selected
            },
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (selected) DerbisCyan else DerbisText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = DerbisMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(DerbisCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = DerbisInk,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF16161A),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun StudioChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) DerbisCyan else Color.White.copy(alpha = 0.06f),
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(180)),
        label = "chip-bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) DerbisInk else DerbisText,
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(180)),
        label = "chip-text"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StudioIconChoiceChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) DerbisCyan else Color.White.copy(alpha = 0.06f),
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(180)),
        label = "chip-icon-bg"
    )
    val tintColor by animateColorAsState(
        targetValue = if (selected) DerbisInk else DerbisText,
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(180)),
        label = "chip-icon-tint"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = tintColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun StudioToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DerbisText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = DerbisMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DerbisInk,
                checkedTrackColor = DerbisCyan,
                uncheckedThumbColor = DerbisMuted,
                uncheckedTrackColor = Color.White.copy(alpha = 0.10f)
            )
        )
    }
}

@Composable
private fun LyricsRendererOptionCard(
    title: String,
    badge: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) accentColor else Color.White.copy(alpha = 0.12f)
    val borderWidth = if (selected) 2.dp else 1.dp
    val cardBackground = if (selected) accentColor.copy(alpha = 0.12f) else Color(0xFF16161A)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
            .background(cardBackground)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (selected) accentColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accentColor else DerbisMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = DerbisText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                if (badge == "SPICY") {
                                    Brush.horizontalGradient(listOf(SpicyAccentCoral, SpicyAccentGold))
                                } else {
                                    Brush.horizontalGradient(listOf(DerbisCyan.copy(alpha = 0.35f), DerbisViolet.copy(alpha = 0.35f)))
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Text(
                    text = description,
                    color = DerbisMuted,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (selected) accentColor else Color.White.copy(alpha = 0.35f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsStudioLivePreview(
    settings: DerbisLyricsStudioSettings,
    currentTrack: Track?,
    liveLyrics: List<LyricLine>,
    lyricsSynced: Boolean,
    playbackPositionMs: Long,
    isPlaying: Boolean
) {
    val strings = LocalDerbisStrings.current
    val animationsEnabled = LocalAnimationsEnabled.current
    val useLive = currentTrack != null && liveLyrics.isNotEmpty() && lyricsSynced
    val previewLines = if (useLive) liveLyrics else DemoLyrics

    val simPositionMs = rememberSimulatedPlaybackPosition(useLive = useLive, isPlaying = isPlaying)
    val activePos = if (useLive) playbackPositionMs else simPositionMs

    val activeIndex = remember(previewLines, activePos) {
        val index = previewLines.indexOfLast { it.startMs <= activePos }
        if (index >= 0) index else 0
    }

    var compareMode by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioPreviewHeader(
                useLive = useLive,
                currentTrack = currentTrack,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier
                    .heightIn(min = DerbisPlayerDesign.MinimumTouchTarget)
                    .widthIn(max = 164.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { compareMode = !compareMode }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = if (compareMode) DerbisCyan else DerbisMuted,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (compareMode) strings.lyricsRendererCompare else strings.lyricsRendererSelectedStyle,
                    color = if (compareMode) DerbisCyan else DerbisMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (compareMode) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Default (Apple Music 13)",
                        color = DerbisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    AppleMusic13StudioLivePreview(
                        currentTrack = currentTrack,
                        previewLines = previewLines,
                        activeIndex = activeIndex,
                        playbackPositionMs = activePos,
                        animationsEnabled = animationsEnabled
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.lyricsRendererSpicy,
                        color = SpicyAccentCoral,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    SpicyStudioLivePreview(
                        currentTrack = currentTrack,
                        previewLines = previewLines,
                        activeIndex = activeIndex,
                        playbackPositionMs = activePos,
                        animationsEnabled = animationsEnabled
                    )
                }
            }
        } else {
            if (settings.isDefaultLyrics) {
                AppleMusic13StudioLivePreview(
                    currentTrack = currentTrack,
                    previewLines = previewLines,
                    activeIndex = activeIndex,
                    playbackPositionMs = activePos,
                    animationsEnabled = animationsEnabled
                )
            } else {
                SpicyStudioLivePreview(
                    currentTrack = currentTrack,
                    previewLines = previewLines,
                    activeIndex = activeIndex,
                    playbackPositionMs = activePos,
                    animationsEnabled = animationsEnabled
                )
            }
        }
    }
}

/** A compact, faithful studio preview of the dedicated Apple Music 13 lyrics surface. */
@Composable
private fun AppleMusic13StudioLivePreview(
    currentTrack: Track?,
    previewLines: List<LyricLine>,
    activeIndex: Int,
    playbackPositionMs: Long,
    animationsEnabled: Boolean
) {
    val accentStart = currentTrack?.let { Color(it.accentStart) } ?: DerbisCyan
    val accentEnd = currentTrack?.let { Color(it.accentEnd) } ?: DerbisViolet
    val shape = RoundedCornerShape(26.dp)
    val visibleIndices = (activeIndex - 1..activeIndex + 1)
        .map { it.coerceIn(0, previewLines.lastIndex) }
        .distinct()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
    ) {
        // This intentionally uses the same direct album-color ambience as the full screen view,
        // instead of the Studio's generic blurred-artwork card.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accentStart.copy(alpha = 0.86f),
                            accentEnd.copy(alpha = 0.62f),
                            Color(0xFF00151B)
                        )
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentStart.copy(alpha = 0.50f), Color.Transparent),
                        radius = 560f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.02f),
                        0.56f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.56f)
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 14.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val artworkUrl = currentTrack?.largeThumbnailUrl?.ifBlank { currentTrack.thumbnailUrl }.orEmpty()
            if (artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(artworkUrl).build(),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(5.dp))
                )
            } else {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.linearGradient(listOf(accentStart, accentEnd)))
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = currentTrack?.title ?: "Now Playing",
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentTrack?.artist.orEmpty(),
                    color = Color.White.copy(alpha = 0.68f),
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("•••", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 20.dp, top = 37.dp, bottom = 35.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            visibleIndices.forEach { index ->
                AppleMusic13StudioPreviewLine(
                    line = previewLines[index],
                    playbackPositionMs = playbackPositionMs,
                    isLineActive = index == activeIndex,
                    accent = accentEnd,
                    animationsEnabled = animationsEnabled
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f))) {
                Box(Modifier.fillMaxWidth(0.42f).height(2.dp).background(Color.White))
            }
            Text(
                text = "‹      ●      ›",
                color = Color.White.copy(alpha = 0.94f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AppleMusic13StudioPreviewLine(
    line: LyricLine,
    playbackPositionMs: Long,
    isLineActive: Boolean,
    accent: Color,
    animationsEnabled: Boolean
) {
    val lineAlpha by animateFloatAsState(
        targetValue = if (isLineActive) 1f else 0.34f,
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(160)),
        label = "apple-studio-preview-line-alpha"
    )
    val lineScale by animateFloatAsState(
        targetValue = if (isLineActive) 1f else 0.94f,
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(160)),
        label = "apple-studio-preview-line-scale"
    )
    val textStyle = TextStyle(
        color = Color.White,
        fontFamily = FontFamily.SansSerif,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold
    )
    val highlightWords = remember(line.text, line.startMs, line.endMs, line.words) {
        wordHighlightTimings(line)
    }
    val timedText = remember(line.text, highlightWords) { buildTimedLyricText(line.text, highlightWords) }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = lineAlpha
                scaleX = lineScale
                scaleY = lineScale
            },
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (!isLineActive || timedText.words.isEmpty()) {
            Text(line.text, style = textStyle)
        } else {
            var consumedCharacters = 0
            timedText.words.forEach { word ->
                val end = (word.startIndex + word.length).coerceAtMost(timedText.text.length)
                val segment = timedText.text.substring(consumedCharacters.coerceAtMost(end), end)
                consumedCharacters = end
                if (segment.isNotEmpty()) {
                    val currentWord = playbackPositionMs in word.startMs until word.endMs
                    val completedWord = playbackPositionMs >= word.endMs
                    val wordColor by animateColorAsState(
                        targetValue = when {
                            currentWord -> Color.White
                            completedWord -> Color.White.copy(alpha = 0.92f)
                            else -> Color.White.copy(alpha = 0.38f)
                        },
                        animationSpec = DerbisMotion.spec(animationsEnabled, tween(115)),
                        label = "apple-studio-preview-word"
                    )
                    Text(
                        text = segment,
                        color = wordColor,
                        style = textStyle.copy(
                            shadow = if (currentWord) Shadow(
                                color = accent.copy(alpha = 0.84f),
                                blurRadius = 18f,
                                offset = Offset.Zero
                            ) else null
                        )
                    )
                }
            }
            if (consumedCharacters < timedText.text.length) {
                Text(
                    text = timedText.text.substring(consumedCharacters),
                    color = Color.White.copy(alpha = 0.38f),
                    style = textStyle
                )
            }
        }
    }
}

@Composable
private fun rememberSimulatedPlaybackPosition(useLive: Boolean, isPlaying: Boolean): Long {
    var simPositionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying, useLive) {
        if (!useLive) {
            val totalDuration = 14000L
            while (true) {
                delay(60L)
                simPositionMs = (simPositionMs + 60L) % totalDuration
            }
        }
    }
    return simPositionMs
}

private fun resolveStudioAccentColor(settings: DerbisLyricsStudioSettings, currentTrack: Track?): Color =
    when (settings.activeColorMode) {
        LyricsActiveColorMode.WHITE -> Color.White
        LyricsActiveColorMode.AUTOMATIC -> if (currentTrack != null) Color(currentTrack.accentEnd) else DerbisCyan
        LyricsActiveColorMode.CUSTOM -> Color(settings.customAccentColor)
    }

private fun resolveStudioGlowShadow(settings: DerbisLyricsStudioSettings, accentColor: Color): Shadow? =
    when (settings.glowEffect) {
        LyricsGlowEffect.OFF -> null
        LyricsGlowEffect.SUBTLE -> Shadow(
            color = accentColor.copy(alpha = 0.55f),
            blurRadius = 14f,
            offset = Offset.Zero
        )
        LyricsGlowEffect.VIBRANT -> Shadow(
            color = accentColor.copy(alpha = 0.85f),
            blurRadius = 26f,
            offset = Offset.Zero
        )
    }

@Composable
private fun StudioPreviewHeader(
    useLive: Boolean,
    currentTrack: Track?,
    modifier: Modifier = Modifier
) {
    val strings = LocalDerbisStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.lyricsStudioPreview,
            color = DerbisMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (useLive) DerbisCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (useLive && currentTrack != null) currentTrack.title.ifBlank { strings.nowPlaying } else "Demo Timeline",
                color = if (useLive) DerbisCyan else DerbisMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudioPreviewLyricsLine(
    line: LyricLine,
    playbackPositionMs: Long,
    isLineActive: Boolean,
    distance: Int,
    settings: DerbisLyricsStudioSettings,
    accentColor: Color,
    fontFamily: FontFamily,
    baseFontSize: Float,
    lineHeight: TextUnit,
    glowShadow: Shadow?,
    animationsEnabled: Boolean
) {
    val lineAlpha by animateFloatAsState(
        targetValue = if (isLineActive) 1f else settings.inactiveOpacity,
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(180)),
        label = "preview-alpha"
    )
    val lineScale by animateFloatAsState(
        targetValue = if (isLineActive) 1.03f else settings.inactiveScale,
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(200)),
        label = "preview-scale"
    )
    val lineBlur = previewLineBlur(isLineActive, settings.blurEnabled, animationsEnabled, distance)
    val textColor = if (isLineActive) accentColor else Color.White
    val resolvedWeight = if (isLineActive) settings.fontWeight.weight else FontWeight.Medium
    val activeShadow = if (isLineActive) glowShadow else null
    val highlightWords = remember(line.text, line.startMs, line.endMs, line.words) {
        wordHighlightTimings(line)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = settings.lineSpacing.spacingDp / 2)
            .graphicsLayer {
                scaleX = lineScale
                scaleY = lineScale
                alpha = lineAlpha
            }
            .then(if (lineBlur > 0.dp) Modifier.blur(lineBlur) else Modifier),
        contentAlignment = previewAlignment(settings.alignment)
    ) {
        StudioPreviewLineText(
            line = line,
            words = highlightWords,
            playbackPositionMs = playbackPositionMs,
            progressiveKaraokeEnabled = settings.progressiveKaraokeEnabled,
            isLineActive = isLineActive,
            textColor = textColor,
            fontFamily = fontFamily,
            baseFontSize = baseFontSize,
            lineHeight = lineHeight,
            fontWeight = resolvedWeight,
            textAlign = settings.alignment.textAlign,
            glowShadow = activeShadow
        )
    }
}

@Composable
private fun StudioPreviewLineText(
    line: LyricLine,
    words: List<com.yasuda.derbis.domain.LyricWord>,
    playbackPositionMs: Long,
    progressiveKaraokeEnabled: Boolean,
    isLineActive: Boolean,
    textColor: Color,
    fontFamily: FontFamily,
    baseFontSize: Float,
    lineHeight: TextUnit,
    fontWeight: FontWeight,
    textAlign: TextAlign,
    glowShadow: Shadow?
) {
    if (progressiveKaraokeEnabled && words.isNotEmpty()) {
        KaraokeWordTimedText(
            lineText = line.text,
            words = words,
            positionProvider = { playbackPositionMs },
            isActive = isLineActive,
            fontSize = baseFontSize.sp,
            lineHeight = lineHeight,
            textAlign = textAlign,
            inactiveColor = textColor,
            completedColor = textColor,
            activeColor = textColor,
            pendingColor = textColor.copy(alpha = 0.35f),
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            shadow = glowShadow
        )
        return
    }
    Text(
        text = line.text,
        color = textColor,
        fontFamily = fontFamily,
        fontSize = baseFontSize.sp,
        lineHeight = lineHeight,
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = TextStyle(shadow = glowShadow)
    )
}

private fun previewAlignment(alignment: LyricsAlignment): Alignment = when (alignment) {
    LyricsAlignment.START -> Alignment.CenterStart
    LyricsAlignment.CENTER -> Alignment.Center
    LyricsAlignment.END -> Alignment.CenterEnd
}

private fun previewLineBlur(
    isLineActive: Boolean,
    blurEnabled: Boolean,
    animationsEnabled: Boolean,
    distance: Int
): Dp {
    if (isLineActive || !blurEnabled || !animationsEnabled) return 0.dp
    return (distance * 1.5f).dp
}

@Composable
private fun StudioPreviewBackground(
    mode: LyricsBackgroundMode,
    track: Track?,
    accent: Color
) {
    val context = LocalContext.current
    when (mode) {
        LyricsBackgroundMode.ORIGINAL -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF141418))
            )
        }
        LyricsBackgroundMode.SOLID -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16161A))
            )
        }
        LyricsBackgroundMode.AMOLED -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        LyricsBackgroundMode.GRADIENT -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.28f),
                                Color(0xFF0F0F12),
                                Color(0xFF0A0A0C)
                            )
                        )
                    )
            )
        }
        LyricsBackgroundMode.BLURRED_ARTWORK -> {
            val artworkUrl = track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl }.orEmpty()
            if (artworkUrl.isNotBlank()) {
                val request = remember(artworkUrl) {
                    ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(DerbisCyan.copy(alpha = 0.22f), DerbisViolet.copy(alpha = 0.22f))
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
            )
        }
        LyricsBackgroundMode.CINEMATIC -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.24f), Color(0xFF08080A)),
                            radius = 600f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.70f))
                        )
                    )
            )
        }
    }
}
