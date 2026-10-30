package com.yasuda.derbis.ui.sharing

import androidx.compose.runtime.Immutable
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.ModernAspectCardVector
import com.yasuda.derbis.ui.components.ModernAspectSquareVector
import com.yasuda.derbis.ui.components.ModernAspectStoryVector
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Aspect ratio configurations for export and preview of Track Story Cards.
 */
enum class TrackStoryFormat(
    val id: String,
    val label: String,
    val ratioLabel: String,
    val widthPx: Int,
    val heightPx: Int,
    val aspectRatio: Float
) {
    STORY_9_16("story_9_16", "Story", "9:16", 1080, 1920, 9f / 16f),
    CARD_4_5("card_4_5", "Post", "4:5", 1080, 1350, 4f / 5f),
    SQUARE_1_1("square_1_1", "Square", "1:1", 1080, 1080, 1f);

    fun icon(): ImageVector = when (this) {
        STORY_9_16 -> ModernAspectStoryVector
        CARD_4_5 -> ModernAspectCardVector
        SQUARE_1_1 -> ModernAspectSquareVector
    }
}

/**
 * Visual background and design themes for the Track Story Card.
 */
enum class TrackStoryTheme(
    val id: String,
    val label: String,
    val subtitle: String
) {
    AURA_GLOW(
        id = "aura_glow",
        label = "Aura Glow",
        subtitle = "Dynamic chromatic blur & living radiance"
    ),
    OLED_BLACK(
        id = "oled_black",
        label = "OLED Pure",
        subtitle = "Pitch black luxury with luminous accents"
    ),
    VINYL_STUDIO(
        id = "vinyl_studio",
        label = "Vinyl Studio",
        subtitle = "Grooved vinyl record sleeve presentation"
    )
}

/**
 * Palette colors extracted from the album artwork.
 */
@Immutable
data class TrackStoryPalette(
    val dominant: Int,
    val accent: Int,
    val darkBase: Int = 0xFF08080C.toInt(),
    val lightAccent: Int = 0xFFFFFFFF.toInt()
)

/**
 * Snapshot containing all metadata needed to render a rich story card.
 */
@Immutable
data class TrackStorySnapshot(
    val track: Track,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val qualityTier: String = "LOSSLESS OPUS • 48 kHz",
    val engineLabel: String = "DERBIS AUDIO ENGINE",
    val watchUrl: String = ""
) {
    val formattedPosition: String get() = formatTime(positionMs)
    val formattedDuration: String get() = formatTime(durationMs)
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0.42f

    companion object {
        private fun formatTime(ms: Long): String {
            if (ms <= 0L) return "0:00"
            val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            return "%d:%02d".format(minutes, seconds)
        }
    }
}
