@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.yasuda.derbis.feature.cast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.cast.MediaRouteButton as Media3RouteButton

/**
 * The Cast action, drawn as the glyph alone.
 *
 * It used to sit in a glass circle - a fill plus a hairline ring - which put a second, smaller
 * circle next to the player's round glass buttons and, over artwork, read as a ring pasted onto
 * the cover. The player's rule is no light edges or rings on artwork, so the button keeps its
 * size and its touch target and drops the chrome: the icon is the whole control.
 */
@Composable
fun CastRouteButton(modifier: Modifier = Modifier) {
    if (!CastRuntimeInitializer.available) return

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White.copy(alpha = 0.86f)
        ) {
            Media3RouteButton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }
    }
}
