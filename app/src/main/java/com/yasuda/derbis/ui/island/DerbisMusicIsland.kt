package com.yasuda.derbis.ui.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.formatSeekbarMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * In-app dynamic island for the current track.
 *
 * Collapsed it is a black capsule hugging the status bar. Tapping it grows into a two column card:
 * artwork, title and artist on the left, the synced lyric sheet on the right, with a blurred copy
 * of the artwork behind everything. Nothing here needs a system overlay permission.
 */
enum class IslandLyricStyle { Default, Spicy }

private val IslandInk = Color(0xFF0A0A0C)
private val IslandText = Color.White
private val IslandMuted = Color.White.copy(alpha = 0.62f)
private val IslandPill = Color(0xFF050506)

@Composable
internal fun DerbisMusicIsland(
    track: Track?,
    lyrics: List<LyricLine>,
    lyricsSynced: Boolean,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    style: IslandLyricStyle,
    onStyleChange: (IslandLyricStyle) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (track == null) return
    var expanded by rememberSaveable(track.id) { mutableStateOf(false) }
    val activeIndex = remember(lyrics, positionMs, lyricsSynced) {
        if (!lyricsSynced) {
            -1
        } else {
            var found = -1
            for (index in lyrics.indices) {
                if (lyrics[index].startMs <= positionMs) found = index else break
            }
            found
        }
    }

    val shape = if (expanded) RoundedCornerShape(30.dp) else RoundedCornerShape(50)
    Box(
        modifier = modifier
            .width(if (expanded) 356.dp else 168.dp)
            .animateContentSize()
            .clip(shape)
            .background(IslandPill)
            .border(1.dp, Color.White.copy(alpha = if (expanded) 0.10f else 0.06f), shape)
            .clickable { expanded = !expanded }
    ) {
        // Blurred artwork only exists in the expanded card; the pill stays pitch black so it reads
        // like the cutout it is imitating.
        if (expanded) {
            AsyncImage(
                model = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(36.dp)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                IslandInk.copy(alpha = 0.74f),
                                IslandInk.copy(alpha = 0.90f)
                            )
                        )
                    )
            )
        }

        if (!expanded) {
            IslandPillRow(track = track, isPlaying = isPlaying)
        } else {
            IslandCard(
                track = track,
                lyrics = lyrics,
                lyricsSynced = lyricsSynced,
                activeIndex = activeIndex,
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                style = style,
                onStyleChange = onStyleChange,
                onTogglePlay = onTogglePlay,
                onPrevious = onPrevious,
                onNext = onNext,
                onOpen = onOpen
            )
        }
    }
}

@Composable
private fun IslandPillRow(track: Track, isPlaying: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = track.title,
            color = IslandText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        IslandWaveform(isPlaying = isPlaying, color = Color(0xFF7C4DFF))
    }
}

/** Three bars that breathe while audio plays and flatten when it stops. */
@Composable
private fun IslandWaveform(isPlaying: Boolean, color: Color) {
    val transition = rememberInfiniteTransition(label = "island-wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "island-wave-phase"
    )
    Canvas(modifier = Modifier.size(width = 18.dp, height = 14.dp)) {
        val bars = 3
        val slot = size.width / bars
        val amplitude = if (isPlaying) 1f else 0.18f
        for (index in 0 until bars) {
            val wobble = 0.45f + 0.55f * kotlin.math.abs(kotlin.math.sin(phase + index * 1.1f))
            val barHeight = size.height * wobble * amplitude
            val left = slot * index + slot * 0.28f
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(left, (size.height - barHeight) / 2f),
                size = androidx.compose.ui.geometry.Size(slot * 0.44f, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(slot * 0.22f)
            )
        }
    }
}

@Composable
private fun IslandCard(
    track: Track,
    lyrics: List<LyricLine>,
    lyricsSynced: Boolean,
    activeIndex: Int,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    style: IslandLyricStyle,
    onStyleChange: (IslandLyricStyle) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit
) {
    Column(modifier = Modifier.padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(
                modifier = Modifier.width(112.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = track.title,
                    color = IslandText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = IslandMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IslandStyleChip(
                        label = "Default",
                        selected = style == IslandLyricStyle.Default,
                        onClick = { onStyleChange(IslandLyricStyle.Default) }
                    )
                    Spacer(Modifier.width(6.dp))
                    IslandStyleChip(
                        label = "Spicy",
                        selected = style == IslandLyricStyle.Spicy,
                        onClick = { onStyleChange(IslandLyricStyle.Spicy) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                IslandLyricSheet(
                    lyrics = lyrics,
                    synced = lyricsSynced,
                    activeIndex = activeIndex,
                    style = style
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        IslandProgress(positionMs = positionMs, durationMs = durationMs)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = IslandText, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = IslandText,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = IslandText, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = formatSeekbarMillis(positionMs),
                color = IslandMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(text = " / ", color = IslandMuted, fontSize = 10.sp)
            Text(
                text = if (durationMs > 0L) formatSeekbarMillis(durationMs) else "--:--",
                color = IslandMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Open",
                color = IslandText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable { onOpen() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun IslandStyleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(160),
        label = "island-chip"
    )
    Text(
        text = label,
        color = if (selected) IslandInk else IslandText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

/**
 * Lyric sheet for the expanded island. Default keeps the list calm and single-coloured; Spicy
 * scales the active line up and gives it the accent treatment used by the spicy lyric deck.
 */
@Composable
private fun IslandLyricSheet(
    lyrics: List<LyricLine>,
    synced: Boolean,
    activeIndex: Int,
    style: IslandLyricStyle
) {
    if (lyrics.isEmpty()) {
        Box(modifier = Modifier.height(112.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "No lyrics for this track",
                color = IslandMuted,
                fontSize = 11.sp
            )
        }
        return
    }
    val listState = rememberLazyListState()
    val target = if (synced) activeIndex else -1
    LaunchedEffect(target, lyrics.size) {
        if (target >= 0) {
            runCatching {
                listState.animateScrollToItem(index = (target - 1).coerceAtLeast(0))
            }
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.height(112.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == target
            val weight = if (isActive) FontWeight.Bold else FontWeight.Medium
            val scale = if (style == IslandLyricStyle.Spicy) 1.16f else 1f
            val color = when {
                isActive && style == IslandLyricStyle.Spicy -> Color(0xFFFF2D95)
                isActive -> IslandText
                else -> IslandMuted
            }
            Text(
                text = line.text.ifBlank { "♪" },
                color = color,
                fontSize = (if (isActive) 13.sp else 11.sp) * scale,
                fontWeight = weight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IslandProgress(positionMs: Long, durationMs: Long) {
    val fraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
