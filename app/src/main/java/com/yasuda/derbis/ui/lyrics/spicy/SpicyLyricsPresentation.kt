package com.yasuda.derbis.ui.lyrics.spicy

import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.ui.lyrics.activeLyricIndex
import kotlin.math.roundToLong

internal fun spicyFocusedLyricIndex(positionMs: Long, lines: List<LyricLine>, synced: Boolean): Int {
    if (!synced) return -1
    val index = activeLyricIndex(positionMs, lines)
    val line = lines.getOrNull(index) ?: return -1
    return if (
        !line.isMetadata && !line.isInstrumental && line.text.isNotBlank() &&
        positionMs >= line.startMs && positionMs - line.endMs <= 320L
    ) index else -1
}

internal fun spicyPreviewIndices(lineCount: Int, activeIndex: Int): IntRange {
    if (lineCount <= 0) return IntRange.EMPTY
    val center = activeIndex.coerceIn(0, lineCount - 1)
    return (center - 1).coerceAtLeast(0)..(center + 1).coerceAtMost(lineCount - 1)
}

internal fun spicyTimelineProgress(positionMs: Long, durationMs: Long): Float =
    if (durationMs > 0L) (positionMs.toDouble() / durationMs).toFloat().coerceIn(0f, 1f) else 0f

internal fun spicySeekPosition(progress: Float, durationMs: Long): Long =
    if (durationMs > 0L && progress.isFinite()) (durationMs * progress.coerceIn(0f, 1f).toDouble()).roundToLong().coerceIn(0L, durationMs) else 0L

internal fun formatSpicySeekMillis(ms: Long): String {
    val seconds = (ms / 1_000L).coerceAtLeast(0L)
    return "${seconds / 60L}:${(seconds % 60L).toString().padStart(2, '0')}"
}
