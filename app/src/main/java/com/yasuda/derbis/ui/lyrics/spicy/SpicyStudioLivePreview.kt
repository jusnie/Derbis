package com.yasuda.derbis.ui.lyrics.spicy

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.OutfitFont

@Composable
fun SpicyStudioLivePreview(
    currentTrack: Track?,
    previewLines: List<LyricLine>,
    activeIndex: Int,
    playbackPositionMs: Long,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalDerbisStrings.current
    val shape = RoundedCornerShape(24.dp, 8.dp, 24.dp, 8.dp)
    val durationMs = previewLines.maxOfOrNull { it.endMs } ?: 0L
    val latestPositionMs by rememberUpdatedState(playbackPositionMs)
    val positionProvider: () -> Long = remember { { latestPositionMs } }
    val indices = remember(previewLines.size, activeIndex) { spicyPreviewIndices(previewLines.size, activeIndex) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, SpicyAccentCoral.copy(alpha = 0.22f), shape)
    ) {
        SpicyLyricsBackdrop(
            accentStart = currentTrack?.let { Color(it.accentStart) } ?: SpicyAccentCoral,
            accentEnd = currentTrack?.let { Color(it.accentEnd) } ?: SpicyAccentGold,
            modifier = Modifier.matchParentSize()
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SpicyBrandBadge()
                Text(
                    text = currentTrack?.title ?: strings.lyricsRendererPreviewTitle,
                    color = SpicyMuted,
                    fontFamily = OutfitFont,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            indices.forEach { index ->
                SpicyVerseCard(
                    line = previewLines[index],
                    lineNumber = index + 1,
                    isActive = index == activeIndex,
                    isPast = index < activeIndex,
                    positionProvider = positionProvider,
                    synced = true,
                    animationsEnabled = animationsEnabled,
                    compact = true
                )
            }
            if (previewLines.isEmpty()) {
                Text(strings.lyricsUnavailable, color = SpicyMuted, fontFamily = OutfitFont, modifier = Modifier.padding(16.dp))
            }
            Column(Modifier.fillMaxWidth().padding(start = 34.dp, end = 12.dp, top = 3.dp)) {
                SpicyTickTimeline(
                    progress = spicyTimelineProgress(playbackPositionMs, durationMs),
                    showCursor = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatSpicySeekMillis(playbackPositionMs), color = SpicyPaper, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text(formatSpicySeekMillis(durationMs), color = SpicyMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
