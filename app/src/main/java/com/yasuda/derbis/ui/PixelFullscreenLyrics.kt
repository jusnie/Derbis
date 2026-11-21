package com.yasuda.derbis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.DerbisPlayPauseGlyph
import com.yasuda.derbis.ui.components.PremiumSeekbar
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.components.formatSeekbarMillis
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.lyrics.activeLyricIndex
import com.yasuda.derbis.ui.lyrics.isLyricsShareSelectable
import com.yasuda.derbis.ui.lyrics.lyricsOffsetPosition
import com.yasuda.derbis.ui.lyrics.lyricsShareSelectedLines
import com.yasuda.derbis.ui.lyrics.rememberLyricsPlaybackClock
import com.yasuda.derbis.ui.lyrics.toggleLyricsShareSelection
import com.yasuda.derbis.ui.theme.PixelDesign
import com.yasuda.derbis.ui.theme.PixelIcons
import com.yasuda.derbis.ui.theme.PixelPalette
import com.yasuda.derbis.ui.theme.PixelifyFont

/**
 * Opaque blush-paper fullscreen lyric page for the Pixel skin.
 *
 * Standing Rule 48: the lyric page is opaque, never transparent over Now Playing.
 * No blurred cover backdrop or dark scrim is painted; every verse sits on #FFEDF1 paper
 * with deep plum (#5B2338) ink and a 3dp rose (#9E1B44) active rule.
 */
@Composable
internal fun PixelFullscreenLyrics(
    state: DerbisUiState,
    fullscreen: Boolean,
    onClose: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onShareLyrics: (List<LyricLine>) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenOptions: () -> Unit,
    lyricsOffsetMs: Long,
    showTranslation: Boolean,
    showRomanization: Boolean,
    calibrateMode: Boolean,
    onCalibrateLine: (Long) -> Unit
) {
    val strings = LocalDerbisStrings.current
    val p = PixelPalette
    val track = state.currentTrack
    val lyrics = remember(state.lyrics) { state.lyrics.filter { !it.isMetadata } }
    val listState = rememberLazyListState()
    val clock = rememberLyricsPlaybackClock(
        trackKey = track?.id.orEmpty(),
        reportedPositionMs = state.positionMs,
        isPlaying = state.isPlaying,
        speed = state.audioSettings.playbackSpeed,
        smoothingEnabled = state.lyricsSynced && state.animationsEnabled
    )
    val latestOffsetMs by rememberUpdatedState(lyricsOffsetMs)
    val activeIndex by remember(lyrics, state.lyricsSynced, clock) {
        derivedStateOf {
            if (state.lyricsSynced) {
                activeLyricIndex(lyricsOffsetPosition(clock.positionMs, latestOffsetMs), lyrics)
            } else {
                -1
            }
        }
    }

    var sharePickerMode by rememberSaveable(track?.id) { mutableStateOf(false) }
    var shareRange by remember(track?.id, lyrics) { mutableStateOf<IntRange?>(null) }
    val selectedLines = remember(lyrics, shareRange) {
        lyricsShareSelectedLines(shareRange, lyrics)
    }
    BackHandler(enabled = sharePickerMode) {
        sharePickerMode = false
        shareRange = null
    }

    LaunchedEffect(activeIndex, lyrics.size, sharePickerMode) {
        if (!sharePickerMode && activeIndex >= 0 && lyrics.isNotEmpty()) {
            runCatching {
                centerLyricsItem(
                    listState = listState,
                    index = activeIndex,
                    animate = state.animationsEnabled,
                    anchorFraction = 0.36f
                )
            }
        }
    }

    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val canShare = track != null && lyrics.any(::isLyricsShareSelectable)
    val isFavorite = track?.let { state.favoriteIds.contains(it.id) } == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.black)
            .pixelPaperGrid()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top header band (#FFD9E2) with 2dp bottom rule
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(p.panel)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (track != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.5.dp, p.text, RectangleShape)
                        ) {
                            CoverImage(track, Modifier.fillMaxSize())
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: strings.lyrics,
                            color = p.text,
                            fontFamily = PixelifyFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!track?.artist.isNullOrBlank()) {
                            Text(
                                text = track?.artist.orEmpty(),
                                color = p.muted,
                                fontFamily = PixelifyFont,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (canShare) {
                        PixelLyricHeaderButton(
                            icon = Icons.Rounded.Share,
                            label = strings.shareLyrics,
                            active = sharePickerMode,
                            onClick = {
                                if (sharePickerMode) {
                                    sharePickerMode = false
                                    shareRange = null
                                } else {
                                    sharePickerMode = true
                                    shareRange = activeIndex
                                        .takeIf { idx -> lyrics.getOrNull(idx)?.let(::isLyricsShareSelectable) == true }
                                        ?.let { idx -> toggleLyricsShareSelection(null, idx, lyrics) }
                                }
                            }
                        )
                    }
                    PixelLyricHeaderButton(
                        icon = PixelIcons.Queue,
                        label = strings.queue,
                        active = false,
                        onClick = onOpenQueue
                    )
                    PixelLyricHeaderButton(
                        icon = Icons.Rounded.Tune,
                        label = strings.options,
                        active = calibrateMode,
                        onClick = onOpenOptions
                    )
                    PixelLyricHeaderButton(
                        icon = Icons.Rounded.Close,
                        label = strings.closeLyrics,
                        active = false,
                        onClick = onClose
                    )
                }
                HorizontalDivider(color = p.outline, thickness = PixelDesign.border)
            }

            if (lyrics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.lyricsLoading) strings.playerLyricsSearching else strings.playerLyricsUnavailable,
                        color = p.muted,
                        fontFamily = PixelifyFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(lyrics, key = { index, line -> "$index:${line.startMs}" }) { index, line ->
                        val isActive = !sharePickerMode && (!state.lyricsSynced || index == activeIndex)
                        val isPast = !sharePickerMode && state.lyricsSynced && activeIndex >= 0 && index < activeIndex
                        val isSelected = sharePickerMode && shareRange?.contains(index) == true
                        val selectable = isLyricsShareSelectable(line)

                        val lineColor = when {
                            isSelected -> p.text
                            sharePickerMode && !selectable -> p.muted.copy(alpha = 0.5f)
                            sharePickerMode -> p.muted
                            isActive -> p.text
                            isPast -> p.muted
                            else -> p.muted.copy(alpha = 0.78f)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .background(p.ink, RectangleShape)
                                            .border(PixelDesign.border, p.cyan, RectangleShape)
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    } else if (isActive && state.lyricsSynced) {
                                        Modifier
                                            .drawBehind {
                                                drawRect(
                                                    color = p.cyan,
                                                    topLeft = Offset(0f, 0f),
                                                    size = Size(3.dp.toPx(), size.height)
                                                )
                                            }
                                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                                    } else {
                                        Modifier.padding(vertical = 2.dp)
                                    }
                                )
                                .clickable(
                                    enabled = if (sharePickerMode) selectable else (calibrateMode || state.lyricsSynced)
                                ) {
                                    when {
                                        sharePickerMode -> shareRange = toggleLyricsShareSelection(shareRange, index, lyrics)
                                        calibrateMode -> onCalibrateLine(line.startMs)
                                        state.lyricsSynced -> onSeekToMs((line.startMs + latestOffsetMs).coerceAtLeast(0L))
                                    }
                                }
                        ) {
                            Text(
                                text = if (line.isInstrumental) "♪ · · ·" else line.text,
                                color = lineColor,
                                fontFamily = PixelifyFont,
                                fontWeight = if (isActive || isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (isActive) 22.sp else 18.sp,
                                lineHeight = if (isActive) 29.sp else 25.sp
                            )
                            if (showRomanization && line.romanized.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = line.romanized,
                                    color = p.muted,
                                    fontFamily = PixelifyFont,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            if (showTranslation && line.translated.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = line.translated,
                                    color = if (isActive) p.cyan else p.muted,
                                    fontFamily = PixelifyFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }

            if (sharePickerMode) {
                HorizontalDivider(color = p.outline, thickness = PixelDesign.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(p.panel)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${selectedLines.size} / 5",
                        color = p.text,
                        fontFamily = PixelifyFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .background(if (selectedLines.isNotEmpty()) p.cyan else p.panelSoft, RectangleShape)
                            .border(PixelDesign.border, p.text, RectangleShape)
                            .derbisPressable(
                                enabled = selectedLines.isNotEmpty(),
                                onClick = {
                                    val chosen = selectedLines
                                    sharePickerMode = false
                                    shareRange = null
                                    onShareLyrics(chosen)
                                },
                                role = Role.Button
                            )
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.shareLyricsContinue.uppercase(),
                            color = if (selectedLines.isNotEmpty()) p.ink else p.muted,
                            fontFamily = PixelifyFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            } else if (!fullscreen) {
                HorizontalDivider(color = p.outline, thickness = PixelDesign.border)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(p.panel)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    PremiumSeekbar(
                        progress = progress,
                        durationMs = state.durationMs,
                        onSeekTo = onSeekToMs,
                        activeColor = p.cyan,
                        inactiveColor = p.panelSoft,
                        thumbColor = p.cyan,
                        isPlaying = state.isPlaying,
                        animated = state.animationsEnabled
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatSeekbarMillis(state.positionMs),
                            color = p.muted,
                            fontFamily = PixelifyFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.durationMs > 0L) formatSeekbarMillis(state.durationMs) else "--:--",
                            color = p.muted,
                            fontFamily = PixelifyFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelLyricTransportPlate(
                            icon = PixelIcons.Previous,
                            label = strings.previous,
                            primary = false,
                            onClick = onPrevious
                        )
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .pixelHardShadow(offset = PixelDesign.shadowOffset, color = p.outline)
                                .background(p.cyan, RectangleShape)
                                .border(PixelDesign.border, p.text, RectangleShape)
                                .derbisPressable(onClick = onTogglePlay, role = Role.Button)
                                .semantics { contentDescription = if (state.isPlaying) strings.pause else strings.play },
                            contentAlignment = Alignment.Center
                        ) {
                            DerbisPlayPauseGlyph(
                                playing = state.isPlaying,
                                color = p.ink,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        PixelLyricTransportPlate(
                            icon = PixelIcons.Next,
                            label = strings.next,
                            primary = false,
                            onClick = onNext
                        )
                        if (track != null) {
                            PixelLyricTransportPlate(
                                icon = if (isFavorite) PixelIcons.Heart else PixelIcons.HeartBorder,
                                label = strings.favoritesPlain,
                                primary = false,
                                active = isFavorite,
                                onClick = { onToggleFavorite(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelLyricHeaderButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val p = PixelPalette
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(if (active) p.cyan else p.ink, RectangleShape)
            .border(PixelDesign.border, if (active) p.text else p.outline, RectangleShape)
            .derbisPressable(onClick = onClick, onClickLabel = label, role = Role.Button)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = PixelIcons.resolve(icon),
            contentDescription = null,
            tint = if (active) p.ink else p.text,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PixelLyricTransportPlate(
    icon: ImageVector,
    label: String,
    primary: Boolean,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val p = PixelPalette
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(if (primary || active) p.panelSoft else p.ink, RectangleShape)
            .border(PixelDesign.border, if (active) p.cyan else p.outline, RectangleShape)
            .derbisPressable(onClick = onClick, onClickLabel = label, role = Role.Button)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) p.cyan else p.text,
            modifier = Modifier.size(20.dp)
        )
    }
}
