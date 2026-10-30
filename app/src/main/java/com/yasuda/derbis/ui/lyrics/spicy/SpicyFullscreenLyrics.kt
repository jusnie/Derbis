package com.yasuda.derbis.ui.lyrics.spicy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.centerLyricsItem
import com.yasuda.derbis.ui.components.DerbisIonicons
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.i18n.lyricsSyncCopy
import com.yasuda.derbis.ui.lyrics.LYRICS_SHARE_MAX_LINES
import com.yasuda.derbis.ui.lyrics.isLyricsShareSelectable
import com.yasuda.derbis.ui.lyrics.lyricsOffsetPosition
import com.yasuda.derbis.ui.lyrics.lyricsShareSelectedLines
import com.yasuda.derbis.ui.lyrics.rememberLyricsPlaybackClock
import com.yasuda.derbis.ui.lyrics.toggleLyricsShareSelection
import com.yasuda.derbis.ui.theme.OutfitFont
import com.yasuda.derbis.viewmodel.DerbisUiState
import kotlinx.coroutines.delay

@Composable
fun SpicyFullscreenLyrics(
    state: DerbisUiState,
    accentStart: Color,
    accentEnd: Color,
    onSeekToMs: (Long) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onShareLyrics: (List<LyricLine>) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenOptions: () -> Unit,
    onClose: () -> Unit,
    lyricsOffsetMs: Long,
    showTranslation: Boolean,
    showRomanization: Boolean,
    showFurigana: Boolean = true,
    calibrateMode: Boolean,
    onCalibrateLine: (Long) -> Unit
) {
    val strings = LocalDerbisStrings.current
    val track = state.currentTrack
    val lyrics = state.lyrics
    val listState = key(track?.id) { rememberLazyListState() }
    val clock = rememberLyricsPlaybackClock(
        trackKey = track?.id.orEmpty(),
        reportedPositionMs = state.positionMs,
        isPlaying = state.isPlaying,
        speed = state.audioSettings.playbackSpeed,
        smoothingEnabled = state.lyricsSynced && state.animationsEnabled
    )
    val latestLyricsOffsetMs by rememberUpdatedState(lyricsOffsetMs)
    val lyricPositionProvider: () -> Long = remember(clock) {
        { lyricsOffsetPosition(clock.positionMs, latestLyricsOffsetMs) }
    }
    val audioPositionProvider: () -> Long = remember(clock) { { clock.positionMs } }
    val activeIndex by remember(lyrics, state.lyricsSynced, lyricPositionProvider) {
        derivedStateOf { spicyFocusedLyricIndex(lyricPositionProvider(), lyrics, state.lyricsSynced) }
    }
    var selectionMode by rememberSaveable(track?.id) { mutableStateOf(false) }
    var shareRange by remember(track?.id, lyrics) { mutableStateOf<IntRange?>(null) }
    val selectedLines = remember(lyrics, shareRange) { lyricsShareSelectedLines(shareRange, lyrics) }
    val canShare = remember(track?.id, lyrics) { track != null && lyrics.any(::isLyricsShareSelectable) }
    var followingLyrics by rememberSaveable(track?.id) { mutableStateOf(true) }
    var scrollGeneration by remember(track?.id) { mutableIntStateOf(0) }
    val scrollConnection = remember(track?.id) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && consumed.y != 0f) {
                    followingLyrics = false
                    scrollGeneration++
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(scrollGeneration, listState.isScrollInProgress, selectionMode, followingLyrics) {
        if (!followingLyrics && !listState.isScrollInProgress && !selectionMode) {
            delay(5_000L)
            followingLyrics = true
        }
    }
    LaunchedEffect(track?.id, activeIndex, lyrics.size, followingLyrics, selectionMode) {
        if (activeIndex >= 0 && followingLyrics && !selectionMode) {
            centerLyricsItem(listState, activeIndex, state.animationsEnabled, anchorFraction = 0.38f)
        }
    }
    fun beginSelection() {
        selectionMode = true
        shareRange = activeIndex
            .takeIf { lyrics.getOrNull(it)?.let(::isLyricsShareSelectable) == true }
            ?.let { toggleLyricsShareSelection(null, it, lyrics) }
    }
    fun finishSelection() {
        selectionMode = false
        shareRange = null
    }

    Box(Modifier.fillMaxSize()) {
        SpicyLyricsBackdrop(accentStart, accentEnd)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val compact = maxHeight < 480.dp
            val wideDock = compact && maxWidth >= 600.dp
            Column(
                Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .padding(horizontal = if (wideDock) 24.dp else 16.dp)
            ) {
                SpicyTrackHeader(
                    track = track,
                    favorite = track?.id in state.favoriteIds,
                    compact = wideDock,
                    selectionMode = selectionMode,
                    onFavorite = { track?.let(onToggleFavorite) },
                    onOptions = onOpenOptions,
                    onClose = { if (selectionMode) finishSelection() else onClose() }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 7.dp, end = 8.dp, top = if (compact) 2.dp else 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            selectionMode -> strings.selectVerses
                            calibrateMode -> lyricsSyncCopy(strings.code).title
                            else -> strings.lyrics
                        },
                        color = SpicyAccentGold,
                        fontFamily = OutfitFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (selectionMode) "${selectedLines.size}/$LYRICS_SHARE_MAX_LINES ${strings.linesLabel}"
                            else if (activeIndex >= 0) "${(activeIndex + 1).toString().padStart(2, '0')} / ${lyrics.size.toString().padStart(2, '0')}"
                            else if (state.lyricsSynced) strings.lyricsSyncedStatus else strings.lyricsUnsyncedStatus,
                        color = SpicyMuted,
                        fontFamily = if (activeIndex >= 0 && !selectionMode) FontFamily.Monospace else OutfitFont,
                        fontSize = 11.sp,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(SpicyPaper.copy(alpha = 0.07f)))
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val listHeight = maxHeight
                    if (lyrics.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(DerbisIonicons.Lyrics, null, Modifier.size(30.dp), tint = SpicyAccentCoral)
                            Text(
                                text = if (state.lyricsLoading) strings.searchingLyrics else strings.lyricsUnavailable,
                                color = SpicyMuted,
                                fontFamily = OutfitFont,
                                fontSize = 18.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().nestedScroll(scrollConnection),
                            contentPadding = PaddingValues(
                                top = (listHeight * 0.15f).coerceIn(12.dp, 72.dp),
                                bottom = (listHeight * 0.60f).coerceAtLeast(64.dp)
                            ),
                            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
                        ) {
                            itemsIndexed(lyrics, key = { index, line -> "spicy-${line.startMs}-${line.role}-$index" }) { index, line ->
                                val selectable = isLyricsShareSelectable(line)
                                SpicyVerseCard(
                                    line = line,
                                    lineNumber = index + 1,
                                    isActive = !selectionMode && index == activeIndex,
                                    isPast = activeIndex >= 0 && index < activeIndex,
                                    isSelected = shareRange?.contains(index) == true && selectable,
                                    selectionMode = selectionMode,
                                    positionProvider = lyricPositionProvider,
                                    synced = state.lyricsSynced,
                                    progressiveKaraoke = state.lyricsStudioSettings.progressiveKaraokeEnabled,
                                    showRomanization = showRomanization,
                                    showTranslation = showTranslation,
                                    showFurigana = showFurigana,
                                    animationsEnabled = state.animationsEnabled,
                                    compact = compact,
                                    onClick = if (selectable && (selectionMode || state.lyricsSynced)) {
                                        {
                                            when {
                                                selectionMode -> shareRange = toggleLyricsShareSelection(shareRange, index, lyrics)
                                                calibrateMode -> onCalibrateLine(line.startMs)
                                                else -> {
                                                    followingLyrics = true
                                                    onSeekToMs((line.startMs + lyricsOffsetMs).coerceAtLeast(0L))
                                                }
                                            }
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                    if (!followingLyrics && !selectionMode && state.lyricsSynced) {
                        OutlinedButton(
                            onClick = { followingLyrics = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SpicySurface, contentColor = SpicyPaper),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).height(48.dp)
                        ) {
                            Icon(SpicyLyricsIcons.Focus, null, Modifier.size(18.dp), tint = SpicyAccentCoral)
                            Text(strings.spicyLyricsBackToLive, fontFamily = OutfitFont, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                if (selectionMode) {
                    Button(
                        onClick = { onShareLyrics(selectedLines); finishSelection() },
                        enabled = selectedLines.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SpicyAccentCoral, contentColor = SpicyInk),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(52.dp)
                    ) {
                        Text(strings.shareLyricsContinue, fontFamily = OutfitFont, fontWeight = FontWeight.Bold)
                    }
                } else {
                    SpicyPlaybackDock(
                        trackKey = track?.id.orEmpty(),
                        durationMs = state.durationMs,
                        positionProvider = audioPositionProvider,
                        isPlaying = state.isPlaying,
                        compact = wideDock,
                        shareEnabled = canShare,
                        onSeekToMs = onSeekToMs,
                        onPrevious = onPrevious,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onOpenQueue = onOpenQueue,
                        onShare = { beginSelection() },
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpicyTrackHeader(
    track: Track?,
    favorite: Boolean,
    compact: Boolean,
    selectionMode: Boolean,
    onFavorite: () -> Unit,
    onOptions: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val metadata: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = track?.title ?: strings.lyrics,
                color = SpicyPaper,
                fontFamily = OutfitFont,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track?.artist.orEmpty(),
                color = SpicyMuted,
                fontFamily = OutfitFont,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    val artwork: @Composable () -> Unit = {
        val artworkUrl = track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl }.orEmpty()
        Box(
            Modifier
                .size(if (compact) 40.dp else 48.dp)
                .clip(RoundedCornerShape(14.dp, 5.dp, 14.dp, 5.dp))
                .background(SpicySurface),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUrl.isNotBlank()) {
                AsyncImage(model = artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(DerbisIonicons.Lyrics, null, Modifier.size(22.dp), tint = SpicyAccentCoral)
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        if (!compact) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SpicyBrandBadge(Modifier.padding(start = 6.dp))
                Spacer(Modifier.weight(1f))
                SpicyIconAction(SpicyLyricsIcons.Close, if (selectionMode) strings.cancel else strings.closeLyrics, onClose)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = if (compact) 0.dp else 4.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (compact) SpicyBrandBadge()
            artwork()
            metadata(Modifier.weight(1f).padding(start = 4.dp))
            SpicyIconAction(
                icon = if (favorite) DerbisIonicons.Heart else DerbisIonicons.HeartOutline,
                label = if (favorite) strings.removeFromFavorites else strings.addToFavorites,
                onClick = onFavorite,
                enabled = track != null,
                selected = favorite
            )
            SpicyIconAction(DerbisIonicons.Equalizer, strings.options, onOptions)
            if (compact) SpicyIconAction(SpicyLyricsIcons.Close, if (selectionMode) strings.cancel else strings.closeLyrics, onClose)
        }
    }
}
