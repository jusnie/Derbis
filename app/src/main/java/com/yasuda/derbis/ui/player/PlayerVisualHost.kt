package com.yasuda.derbis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yasuda.derbis.ui.PlayerAmbience

/**
 * The deck's background. There is no longer a choice to make here: the cover, blurred and drifting,
 * with its own colours moving over it. Kept as a host so a skin that owns its own atmosphere
 * (Apple Music, Prism, Nocturne) can short-circuit in one place.
 */
@Composable
internal fun PlayerVisualHost(
    artworkUrl: String,
    ambience: PlayerAmbience,
    animationsEnabled: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    fixedColors: Boolean = false,
    backdropFocus: Float = 0.34f
) {
    Box(modifier = modifier) {
        PlayerBackdrop(
            artworkUrl = artworkUrl,
            ambience = ambience,
            isPlaying = isPlaying,
            animationsEnabled = animationsEnabled,
            fixedColors = fixedColors,
            focusY = backdropFocus,
            modifier = Modifier.fillMaxSize()
        )
    }
}
