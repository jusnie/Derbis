package com.yasuda.derbis.domain

class LyricsEngine {
    fun currentLine(positionMs: Long, lines: List<LyricLine>): LyricLine? {
        if (lines.isEmpty()) return null
        var low = 0
        var high = lines.lastIndex
        var candidate = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (lines[middle].startMs <= positionMs) {
                candidate = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        if (candidate < 0) return null
        for (index in candidate downTo 0) {
            val line = lines[index]
            if (line.startMs > positionMs) continue
            // Allow a short 320ms transition window for natural fadeout
            if (positionMs <= line.endMs + 320L) return line
            if (candidate - index > 8) break
        }
        val candidateLine = lines[candidate]
        if (positionMs in candidateLine.startMs..(candidateLine.endMs + 320L)) {
            return candidateLine
        }
        return null
    }

    fun syntheticLyrics(track: Track): List<LyricLine> = emptyList()
}
