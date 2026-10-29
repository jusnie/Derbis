package com.yasuda.derbis.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal val DerbisHorizontalSafeInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)

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
