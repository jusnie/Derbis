package com.yasuda.derbis.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal val DerbisHorizontalSafeInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)

/**
 * Full-screen (immersive chrome) top inset for every skin.
 *
 * Derbis hides the system status bar and navigation bar in all skins, video-app style, and
 * brings them back on scroll (see ChromeImmersion). While hidden, Compose reports a 0dp status
 * bar inset, which would pin headers to the physical screen edge or under the camera cutout.
 * safeDrawing is the union of the system bars and the display cutout, so it keeps headers clear
 * of the hole-punch while the bars are gone, and collapses back to the real status bar height
 * when the user scrolls the bars back in. A small floor keeps content off the edge on bar-less
 * devices without a cutout.
 */
@Composable
internal fun derbisSafeTopInset(): Dp =
    WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding().coerceAtLeast(8.dp)

/**
 * Drop-in replacement for `statusBarsPadding()` that stays correct in immersive mode:
 * clears the camera cutout (or a small floor) when the status bar is hidden.
 */
@Composable
internal fun Modifier.derbisSafeTopPadding(): Modifier =
    this.padding(top = derbisSafeTopInset())

@Composable
private fun derbisWindowSize(): DpSize {
    val containerSize = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) {
        DpSize(containerSize.width.toDp(), containerSize.height.toDp())
    }
}

@Composable
internal fun derbisWindowIsCompactLandscape(): Boolean {
    val size = derbisWindowSize()
    return isDerbisCompactLandscape(size.width.value, size.height.value)
}

@Composable
internal fun derbisCompactLandscapeHeight(preferred: Dp, viewportShare: Float, minimum: Dp): Dp {
    val size = derbisWindowSize()
    return derbisCompactLandscapeHeightDp(
        preferredDp = preferred.value,
        widthDp = size.width.value,
        heightDp = size.height.value,
        viewportShare = viewportShare,
        minimumDp = minimum.value
    ).dp
}
