package com.luc4n3x.derbis.ui

enum class DerbisLayoutMode {
    Compact,
    Medium,
    Expanded
}

enum class DerbisPlayerPane {
    Stacked,
    SideBySide
}

const val DerbisMediumWidthDp: Float = 600f
const val DerbisExpandedWidthDp: Float = 840f
const val DerbisCompactLandscapeMaxHeightDp: Float = 480f

fun isDerbisCompactLandscape(widthDp: Float, heightDp: Float): Boolean =
    heightDp > 0f && heightDp < DerbisCompactLandscapeMaxHeightDp && widthDp > heightDp

fun derbisCompactLandscapeVideoPaneWeight(widthDp: Float): Float =
    if (widthDp >= 760f) 1.5f else 1f

fun derbisCompactLandscapeHeightDp(
    preferredDp: Float,
    widthDp: Float,
    heightDp: Float,
    viewportShare: Float,
    minimumDp: Float
): Float {
    if (!isDerbisCompactLandscape(widthDp, heightDp)) return preferredDp
    val floor = minOf(minimumDp, preferredDp)
    return (heightDp * viewportShare).coerceIn(floor, preferredDp)
}

fun resolveDerbisLayoutMode(widthDp: Float, heightDp: Float): DerbisLayoutMode {
    val safeWidth = widthDp.coerceAtLeast(0f)
    val safeHeight = heightDp.coerceAtLeast(0f)
    return when {
        safeWidth >= DerbisExpandedWidthDp -> DerbisLayoutMode.Expanded
        safeWidth >= DerbisMediumWidthDp -> DerbisLayoutMode.Medium
        safeHeight > 0f && safeHeight < 400f && safeWidth >= 480f -> DerbisLayoutMode.Medium
        else -> DerbisLayoutMode.Compact
    }
}

fun resolvePlayerPane(widthDp: Float, heightDp: Float): DerbisPlayerPane {
    val landscape = widthDp > heightDp
    val wideEnough = widthDp >= DerbisExpandedWidthDp || (landscape && widthDp >= DerbisMediumWidthDp)
    val tallEnough = heightDp >= 320f
    return if (wideEnough && tallEnough) DerbisPlayerPane.SideBySide else DerbisPlayerPane.Stacked
}

fun resolveNowPlayingPane(widthDp: Float, heightDp: Float, isVideoMode: Boolean): DerbisPlayerPane =
    if (isVideoMode && !isDerbisCompactLandscape(widthDp, heightDp)) {
        DerbisPlayerPane.Stacked
    } else {
        resolvePlayerPane(widthDp, heightDp)
    }

fun derbisContentMaxWidthDp(mode: DerbisLayoutMode): Float = when (mode) {
    DerbisLayoutMode.Compact -> 560f
    DerbisLayoutMode.Medium -> 720f
    DerbisLayoutMode.Expanded -> 1080f
}

fun derbisPlayerArtworkMaxWidthDp(pane: DerbisPlayerPane, mode: DerbisLayoutMode): Float = when {
    pane == DerbisPlayerPane.SideBySide -> 460f
    mode == DerbisLayoutMode.Compact -> 520f
    else -> 560f
}

fun derbisMiniPlayerMaxWidthDp(mode: DerbisLayoutMode): Float = when (mode) {
    DerbisLayoutMode.Compact -> Float.POSITIVE_INFINITY
    DerbisLayoutMode.Medium -> 640f
    DerbisLayoutMode.Expanded -> 760f
}

fun derbisFoldAwareGutterDp(mode: DerbisLayoutMode, compactPlayer: Boolean): Float = when {
    mode == DerbisLayoutMode.Expanded -> 34f
    mode == DerbisLayoutMode.Medium -> 28f
    compactPlayer -> 18f
    else -> 22f
}
