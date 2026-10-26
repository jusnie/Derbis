package com.luc4n3x.derbis.ui.ambient

import androidx.compose.runtime.Immutable
import com.luc4n3x.derbis.domain.DerbisAmbientSettings
import com.luc4n3x.derbis.domain.DerbisCanvasQuality
import com.luc4n3x.derbis.feature.motion.MotionArtwork

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
