package com.yasuda.derbis.ui.ambient

import androidx.compose.runtime.Immutable
import com.yasuda.derbis.domain.DerbisAmbientSettings
import com.yasuda.derbis.domain.DerbisCanvasQuality
import com.yasuda.derbis.feature.motion.MotionArtwork

@Immutable
internal data class AmbientUiState(
    val hasTrack: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lyricLine: String = "",
    val motionArtwork: MotionArtwork? = null,
    val animationsEnabled: Boolean = true,
    val accentStart: Int = 0,
    val accentEnd: Int = 0,
    val canvasQuality: DerbisCanvasQuality = DerbisCanvasQuality.Auto,
    val settings: DerbisAmbientSettings = DerbisAmbientSettings()
) {
    val hasAccent: Boolean get() = accentStart != 0 && accentEnd != 0
}
