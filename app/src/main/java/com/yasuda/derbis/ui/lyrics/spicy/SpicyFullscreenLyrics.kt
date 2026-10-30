package com.yasuda.derbis.ui.lyrics.spicy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import com.yasuda.derbis.data.KanjiFurigana
import com.yasuda.derbis.ui.lyrics.FuriganaRubyLine
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.domain.LyricWord
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.DerbisUiState
import com.yasuda.derbis.ui.LocalDerbisStrings
import com.yasuda.derbis.ui.i18n.lyricsSyncCopy
import com.yasuda.derbis.ui.lyrics.activeLyricIndex
import com.yasuda.derbis.ui.lyrics.centerLyricsItem
import com.yasuda.derbis.ui.lyrics.isLyricsShareSelectable
import com.yasuda.derbis.ui.lyrics.lyricsOffsetPosition
import com.yasuda.derbis.ui.lyrics.lyricsShareSelectedLines
import com.yasuda.derbis.ui.lyrics.rememberLyricsPlaybackClock
import com.yasuda.derbis.ui.lyrics.toggleLyricsShareSelection
import com.yasuda.derbis.ui.theme.DerbisMotion
import kotlinx.coroutines.delay

private const val SPICY_SHARE_MAX_LINES = 5

val SpicyGradientBrush: Brush = Brush.horizontalGradient(
    listOf(
        Color(0xFFFF2A68),
        Color(0xFFFF5E3A),
        Color(0xFFFF9500)
    )
)

val SpicyFlameColor: Color = Color(0xFFFF4500)
val SpicyAccentCoral: Color = Color(0xFFFF2A68)
val SpicyAccentGold: Color = Color(0xFFFF9500)

/**
 * Spicy Lyrics Brand Badge: distinctive fiery pill highlighting the Spicy renderer.
 */
@Composable
fun SpicyBrandBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFF2A68),
                        Color(0xFFFF6200)
                    )
                )
            )
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Whatshot,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "SPICY",
            color = Color.White,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp
        )
    }
}

/**
 * Immersive dynamic ambient mesh backdrop for Spicy Lyrics.
 * Combines obsidian deeps with warm spicy ember blooms and track cover highlights.
 */
@Composable
fun SpicyLyricsBackdrop(
    track: Track?,
    accentStart: Color,
    accentEnd: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090E))
    ) {
        val artworkUrl = track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl }.orEmpty()
        if (artworkUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUrl)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(64.dp)
                    .graphicsLayer { alpha = 0.22f }
            )
        }

        // Primary vibrant fiery orb (top-left)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SpicyAccentCoral.copy(alpha = 0.36f),
                            Color.Transparent
                        ),
                        center = Offset(180f, 280f),
                        radius = 820f
                    )
                )
        )

        // Secondary track-accent orb (bottom-right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentStart.copy(alpha = 0.32f),
                            accentEnd.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(840f, 1300f),
                        radius = 960f
                    )
                )
        )

        // Ember warm glow (center-bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SpicyAccentGold.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(500f, 950f),
                        radius = 700f
                    )
                )
        )

        // Contrast vignette ensuring maximum lyric legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF09090E).copy(alpha = 0.45f),
                        0.48f to Color.Transparent,
                        1f to Color(0xFF09090E).copy(alpha = 0.78f)
                    )
                )
        )
    }
}

/**
 * Spicy Fullscreen Lyrics: Bold typography, active verse magnification (1.16x),
 * progressive depth blur on past/upcoming verses, dynamic fiery glow, and fluid auto-scroll.
 */
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
    val listState = rememberLazyListState()

    val clock = rememberLyricsPlaybackClock(
        trackKey = track?.id.orEmpty(),
        reportedPositionMs = state.positionMs,
        isPlaying = state.isPlaying,
        speed = state.audioSettings.playbackSpeed,
        smoothingEnabled = state.lyricsSynced && state.animationsEnabled
    )

    val latestLyricsOffsetMs by rememberUpdatedState(lyricsOffsetMs)
    val activeIndex by remember(lyrics, state.lyricsSynced, clock) {
        derivedStateOf {
            if (state.lyricsSynced) {
                activeLyricIndex(lyricsOffsetPosition(clock.positionMs, latestLyricsOffsetMs), lyrics)
            } else {
                -1
            }
        }
    }

    LaunchedEffect(activeIndex, lyrics.size) {
        if (activeIndex >= 0 && lyrics.isNotEmpty()) {
            centerLyricsItem(
                listState = listState,
                index = activeIndex,
                animate = state.animationsEnabled,
                anchorFraction = 0.36f
            )
        }
    }

    var showCluster by rememberSaveable(track?.id) { mutableStateOf(true) }
    var interactionTick by remember(track?.id) { mutableIntStateOf(0) }
    LaunchedEffect(showCluster, interactionTick) {
        if (showCluster) {
            delay(8_000L)
            showCluster = false
        }
    }
    val wakeCluster: () -> Unit = {
        showCluster = true
        interactionTick++
    }
    val scrollWakesCluster = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f) wakeCluster()
                return Offset.Zero
            }
        }
    }

    var dragProgress by remember(track?.id) { mutableStateOf<Float?>(null) }
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shownProgress = dragProgress ?: progress
    val shownPosition = (state.durationMs * shownProgress).toLong()

    var imageShareSelectionMode by rememberSaveable(track?.id) { mutableStateOf(false) }
    var imageShareRange by remember(track?.id, lyrics) { mutableStateOf<IntRange?>(null) }
    val selectedImageShareLines = remember(lyrics, imageShareRange) {
        lyricsShareSelectedLines(imageShareRange, lyrics)
    }
    fun beginImageShareSelection() {
        imageShareSelectionMode = true
        imageShareRange = activeIndex
            .takeIf { index -> lyrics.getOrNull(index)?.let(::isLyricsShareSelectable) == true }
            ?.let { index -> toggleLyricsShareSelection(null, index, lyrics) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                showCluster = !showCluster
                interactionTick++
            }
    ) {
        // Dynamic Backdrop
        SpicyLyricsBackdrop(
            track = track,
            accentStart = accentStart,
            accentEnd = accentEnd
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = if (imageShareSelectionMode) 44.dp else 0.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (track != null) {
                    val thumbUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = track?.title ?: strings.lyrics,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        SpicyBrandBadge()
                    }
                    Text(
                        text = track?.artist.orEmpty(),
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        wakeCluster()
                        track?.let(onToggleFavorite)
                    },
                    enabled = track != null
                ) {
                    Icon(
                        imageVector = if (track?.id in state.favoriteIds) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = strings.favorite,
                        tint = if (track?.id in state.favoriteIds) SpicyAccentCoral else Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }

                IconButton(onClick = { wakeCluster(); onOpenOptions() }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = strings.options,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Lyric Verses List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(scrollWakesCluster)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = if (imageShareSelectionMode) 20.dp else 36.dp,
                        bottom = if (imageShareSelectionMode) 100.dp else 36.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    if (lyrics.isEmpty()) {
                        item {
                            Text(
                                text = if (state.lyricsLoading) strings.searchingLyrics else strings.lyricsUnavailable,
                                color = Color.White.copy(alpha = 0.58f),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        itemsIndexed(
                            lyrics,
                            key = { index, line -> "spicy-lyrics-${line.startMs}-${line.role}-$index" }
                        ) { index, line ->
                            val isLineActive = !imageShareSelectionMode && index == activeIndex
                            val imageShareSelected = imageShareRange?.contains(index) == true && isLyricsShareSelectable(line)
                            val imageSelectionActive = imageShareSelectionMode && imageShareSelected

                            SpicyLyricLineRow(
                                line = line,
                                isActive = isLineActive,
                                isImageSelection = imageSelectionActive,
                                playbackPositionMs = lyricsOffsetPosition(clock.positionMs, latestLyricsOffsetMs),
                                showRomanization = showRomanization,
                                showTranslation = showTranslation,
                                showFurigana = showFurigana,
                                accentColor = accentEnd,
                                animationsEnabled = state.animationsEnabled,
                                onClick = {
                                    wakeCluster()
                                    when {
                                        imageShareSelectionMode -> {
                                            if (isLyricsShareSelectable(line)) {
                                                imageShareRange = toggleLyricsShareSelection(imageShareRange, index, lyrics)
                                            }
                                        }
                                        calibrateMode -> onCalibrateLine(line.startMs)
                                        else -> onSeekToMs((line.startMs + lyricsOffsetMs).coerceAtLeast(0L))
                                    }
                                }
                            )
                        }
                    }
                }

                // Floating Actions (Share, Queue, Sync)
                AnimatedVisibility(
                    visible = showCluster && !imageShareSelectionMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Column(
                        modifier = Modifier.padding(end = 20.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (track != null && lyrics.any(::isLyricsShareSelectable)) {
                            SpicyFloatingButton(
                                icon = Icons.Rounded.Image,
                                label = if (imageShareSelectionMode) {
                                    "${selectedImageShareLines.size}/$SPICY_SHARE_MAX_LINES ${strings.linesLabel}"
                                } else {
                                    strings.shareLyrics
                                },
                                onClick = {
                                    wakeCluster()
                                    if (imageShareSelectionMode) {
                                        imageShareSelectionMode = false
                                        imageShareRange = null
                                    } else {
                                        beginImageShareSelection()
                                    }
                                }
                            )
                        }
                        if (state.lyricsSynced) {
                            SpicyFloatingButton(
                                icon = Icons.Rounded.Timer,
                                label = lyricsSyncCopy(strings.code).title,
                                onClick = { wakeCluster(); onOpenOptions() }
                            )
                        }
                        SpicyFloatingButton(
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            label = strings.queue,
                            onClick = { wakeCluster(); onOpenQueue() }
                        )
                    }
                }

                // Share Sheet Continue Button
                AnimatedVisibility(
                    visible = imageShareSelectionMode,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Button(
                        onClick = {
                            onShareLyrics(selectedImageShareLines)
                            imageShareSelectionMode = false
                            imageShareRange = null
                        },
                        enabled = selectedImageShareLines.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpicyAccentCoral,
                            contentColor = Color.White,
                            disabledContainerColor = SpicyAccentCoral.copy(alpha = 0.34f),
                            disabledContentColor = Color.White.copy(alpha = 0.55f)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .widthIn(min = 180.dp)
                    ) {
                        Text(
                            text = strings.shareLyricsContinue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Idle Progress Strip
                AnimatedVisibility(
                    visible = !showCluster && !imageShareSelectionMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, end = 32.dp, bottom = 22.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = shownProgress,
                                onValueChange = { dragProgress = it },
                                onValueChangeFinished = {
                                    dragProgress?.let { onSeekToMs((state.durationMs * it).toLong()) }
                                    dragProgress = null
                                },
                                enabled = state.durationMs > 0L,
                                colors = SliderDefaults.colors(
                                    thumbColor = SpicyAccentCoral,
                                    activeTrackColor = SpicyAccentCoral,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatSpicySeekMillis(shownPosition),
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (state.durationMs > 0L) formatSpicySeekMillis(state.durationMs) else "--:--",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Expanded Bottom Playback Cluster
            AnimatedVisibility(
                visible = showCluster && !imageShareSelectionMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 6.dp, bottom = 14.dp)
                ) {
                    Slider(
                        value = shownProgress,
                        onValueChange = {
                            dragProgress = it
                            wakeCluster()
                        },
                        onValueChangeFinished = {
                            dragProgress?.let { onSeekToMs((state.durationMs * it).toLong()) }
                            dragProgress = null
                            wakeCluster()
                        },
                        enabled = state.durationMs > 0L,
                        colors = SliderDefaults.colors(
                            thumbColor = SpicyAccentCoral,
                            activeTrackColor = SpicyAccentCoral,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatSpicySeekMillis(shownPosition),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.durationMs > 0L) formatSpicySeekMillis(state.durationMs) else "--:--",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { wakeCluster(); onPrevious() },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = strings.previous,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SpicyGradientBrush)
                                .clickable { wakeCluster(); onTogglePlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (state.isPlaying) strings.pause else strings.play,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = { wakeCluster(); onNext() },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = strings.next,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Lyric Verse in Spicy Lyrics style:
 * Active: 1.16x magnification, bold black font, pure white text with dynamic glowing shadow.
 * Inactive: 0.92x scale, dimmed opacity (0.32f), progressive depth blur (2.5.dp).
 */
@Composable
fun SpicyLyricLineRow(
    line: LyricLine,
    isActive: Boolean,
    isImageSelection: Boolean,
    playbackPositionMs: Long,
    showRomanization: Boolean,
    showTranslation: Boolean,
    showFurigana: Boolean = true,
    accentColor: Color,
    animationsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lineScale by animateFloatAsState(
        targetValue = if (isActive) 1.16f else 0.92f,
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(240, easing = FastOutSlowInEasing)),
        label = "spicy-scale"
    )

    val lineAlpha by animateFloatAsState(
        targetValue = when {
            isImageSelection -> 1f
            isActive -> 1f
            else -> 0.32f
        },
        animationSpec = DerbisMotion.spec(animationsEnabled, tween(200)),
        label = "spicy-alpha"
    )

    val blurRadius = if (!isActive && !isImageSelection) 2.5.dp else 0.dp

    val glowShadow = if (isActive) {
        Shadow(
            color = SpicyAccentCoral.copy(alpha = 0.65f),
            blurRadius = 22f,
            offset = Offset(0f, 2f)
        )
    } else {
        Shadow.None
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = lineScale
                scaleY = lineScale
                transformOrigin = TransformOrigin(0f, 0.5f)
                alpha = lineAlpha
            }
            .blur(blurRadius)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isImageSelection) Color.White.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val hasKanji = remember(line.text) { KanjiFurigana.containsKanji(line.text) }
        if (hasKanji) {
            FuriganaRubyLine(
                lineText = line.text,
                baseColor = Color.White,
                rubyColor = Color.White.copy(alpha = 0.85f),
                fontSize = 28.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                shadow = glowShadow
            )
        } else {
            // High-impact bold verse
            Text(
                text = line.text,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 28.sp,
                    lineHeight = 35.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    shadow = glowShadow
                )
            )
        }

        if (showRomanization && line.romanized.isNotBlank()) {
            Text(
                text = line.romanized,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        if (showTranslation && line.translated.isNotBlank()) {
            Text(
                text = line.translated,
                color = SpicyAccentGold.copy(alpha = 0.88f),
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Compact Studio live preview of Spicy Lyrics.
 * Renders the exact same demo verses or live track with Spicy bold typography,
 * verse magnification, depth blur, and fiery accent.
 */
@Composable
fun SpicyStudioLivePreview(
    currentTrack: Track?,
    previewLines: List<LyricLine>,
    activeIndex: Int,
    playbackPositionMs: Long,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val accentStart = currentTrack?.let { Color(it.accentStart) } ?: SpicyAccentCoral
    val accentEnd = currentTrack?.let { Color(it.accentEnd) } ?: SpicyAccentGold
    val shape = RoundedCornerShape(26.dp)

    val visibleIndices = (activeIndex - 1..activeIndex + 1)
        .map { it.coerceIn(0, previewLines.lastIndex) }
        .distinct()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .background(Color(0xFF09090E))
    ) {
        // Spicy radial blooms
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SpicyAccentCoral.copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        center = Offset(140f, 160f),
                        radius = 420f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentStart.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(520f, 480f),
                        radius = 480f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.10f),
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f)
                    )
                )
        )

        // Top Preview Header
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
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SpicyGradientBrush)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = currentTrack?.title ?: "Now Playing",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SpicyBrandBadge()
                }
                Text(
                    text = currentTrack?.artist.orEmpty().ifBlank { "Yasuda Music" },
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Lyric Verses
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 20.dp, top = 44.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            visibleIndices.forEach { index ->
                val line = previewLines[index]
                val isActive = index == activeIndex
                val lineScale by animateFloatAsState(
                    targetValue = if (isActive) 1.15f else 0.92f,
                    animationSpec = DerbisMotion.spec(animationsEnabled, tween(180)),
                    label = "preview-spicy-scale"
                )
                val lineAlpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.32f,
                    animationSpec = DerbisMotion.spec(animationsEnabled, tween(180)),
                    label = "preview-spicy-alpha"
                )

                Text(
                    text = line.text,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                        shadow = if (isActive) Shadow(SpicyAccentCoral.copy(alpha = 0.7f), blurRadius = 14f) else Shadow.None
                    ),
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = lineScale
                            scaleY = lineScale
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            alpha = lineAlpha
                        }
                        .blur(if (isActive) 0.dp else 2.dp)
                )
            }
        }

        // Bottom progress bar strip
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .height(2.5.dp)
                        .background(SpicyGradientBrush)
                )
            }
        }
    }
}

@Composable
private fun SpicyFloatingButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .border(1.dp, SpicyAccentCoral.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatSpicySeekMillis(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
