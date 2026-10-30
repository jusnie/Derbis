package com.yasuda.derbis.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pristine, mathematically exact modern vector rendering of the Derbis 3-diamond mark.
 * Renders anti-aliased with smooth rounded corners at any resolution.
 */
@Composable
fun DerbisVectorLogo(
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val squareSide = s * 0.1768f
        val cornerRadius = CornerRadius(s * 0.022f, s * 0.022f)

        val centers = listOf(
            Offset(s * 0.4134f, s * 0.3380f),
            Offset(s * 0.5833f, s * 0.4988f),
            Offset(s * 0.4134f, s * 0.6597f)
        )

        for (center in centers) {
            rotate(degrees = 45f, pivot = center) {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(center.x - squareSide / 2f, center.y - squareSide / 2f),
                    size = Size(squareSide, squareSide),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}

/**
 * Modern SVG 7-bar audio wave icon for audio engine / equalizer feature presentation.
 */
internal val ModernAudioEngineVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernAudioEngine",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M3 9v6 M6 6v12 M9 4v16 M12 2v20 M15 5v14 M18 7v10 M21 10v4"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round
    ).build()
}

/**
 * Modern SVG synced lyrics / word-by-word active highlight vector.
 */
internal val ModernSyncedLyricsVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernSyncedLyrics",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        // Modern rounded dialogue lyrics card
        pathData = PathParser().parsePathString(
            "M5 4h14a3 3 0 0 1 3 3v9a3 3 0 0 1-3 3h-4l-3.5 2.5a.8.8 0 0 1-1.3-.6V19H5a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Synced rhythmic lyric bars
        pathData = PathParser().parsePathString(
            "M8 8.5h8 M8 12.5h5 M8 15.5h7"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round
    ).build()
}

/**
 * Modern SVG offline download arrow into cradle with rounded linecaps.
 */
internal val ModernOfflineDownloadVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernOfflineDownload",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        // Modern downward arrow
        pathData = PathParser().parsePathString(
            "M12 4v10 M7.5 10.5L12 15l4.5-4.5"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.1f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Storage cradle
        pathData = PathParser().parsePathString(
            "M5 16.5v1.5a2.5 2.5 0 0 0 2.5 2.5h9a2.5 2.5 0 0 0 2.5-2.5v-1.5"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.1f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

/**
 * Modern SVG forward chevron arrow for primary action buttons.
 */
internal val ModernChevronRightVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernChevronRight",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M7.5 4.5l5.5 5.5-5.5 5.5"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

/**
 * Modern SVG story card icon with soundwaves for social sharing.
 */
internal val ModernStoryCardVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernStoryCard",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        // Phone / vertical story frame
        pathData = PathParser().parsePathString(
            "M7 3h10a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Audio wave bars inside card
        pathData = PathParser().parsePathString(
            "M9 12v2 M12 10v6 M15 11v4"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.1f,
        strokeLineCap = StrokeCap.Round
    ).build()
}

/**
 * Modern SVG Instagram camera outline for direct Instagram Stories sharing.
 */
internal val ModernInstagramStoryVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernInstagramStory",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        // Rounded camera square
        pathData = PathParser().parsePathString(
            "M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Center lens circle
        pathData = PathParser().parsePathString(
            "M12 8.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f
    ).addPath(
        // Top right flash dot
        pathData = PathParser().parsePathString(
            "M17.5 6.5h.01"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.6f,
        strokeLineCap = StrokeCap.Round
    ).build()
}

/**
 * Modern SVG Save to Gallery / Download icon.
 */
internal val ModernSaveGalleryVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernSaveGallery",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        // Downward arrow
        pathData = PathParser().parsePathString(
            "M12 4v10 M7.5 10.5L12 15l4.5-4.5"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.1f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Gallery tray
        pathData = PathParser().parsePathString(
            "M5 16.5v1.5a2.5 2.5 0 0 0 2.5 2.5h9a2.5 2.5 0 0 0 2.5-2.5v-1.5"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.1f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

/**
 * Modern SVG Share Sparkle icon for universal share action.
 */
internal val ModernShareSparkleVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernShareSparkle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        // 4-point star sparkle
        pathData = PathParser().parsePathString(
            "M12 3c0 4.5-3.5 8-8 8 4.5 0 8 3.5 8 8 0-4.5 3.5-8 8-8-4.5 0-8-3.5-8-8z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Small accent sparkle
        pathData = PathParser().parsePathString(
            "M19 4c0 1.5-1 2.5-2.5 2.5 1.5 0 2.5 1 2.5 2.5 0-1.5 1-2.5 2.5-2.5-1.5 0-2.5-1-2.5-2.5z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.5f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

/**
 * Modern SVG Copy Link icon.
 */
internal val ModernCopyLinkVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernCopyLink",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        pathData = PathParser().parsePathString(
            "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

/**
 * Modern SVG Aspect ratio icons for 9:16 Story, 4:5 Card, and 1:1 Square.
 */
internal val ModernAspectStoryVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernAspectStory",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M6 2.5h8a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

internal val ModernAspectCardVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernAspectCard",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M4.5 3h11a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-11a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

internal val ModernAspectSquareVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernAspectSquare",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M3.5 3.5h13a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

/**
 * Modern SVG Listen Together vector icon: dual listeners connected with synchronized sound waves.
 */
internal val ModernListenTogetherVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "ModernListenTogether",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        // Left listener head
        pathData = PathParser().parsePathString(
            "M8 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f
    ).addPath(
        // Left listener shoulders
        pathData = PathParser().parsePathString(
            "M3.5 19.5c0-2.8 2.2-4.5 4.5-4.5s4.5 1.7 4.5 4.5"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round
    ).addPath(
        // Right listener head
        pathData = PathParser().parsePathString(
            "M16 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f
    ).addPath(
        // Right listener shoulders
        pathData = PathParser().parsePathString(
            "M11.5 19.5c0-2.8 2.2-4.5 4.5-4.5s4.5 1.7 4.5 4.5"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round
    ).addPath(
        // Synchronized sound wave arc overhead
        pathData = PathParser().parsePathString(
            "M9.5 3.5a3.5 3.5 0 0 1 5 0"
        ).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round
    ).build()
}


