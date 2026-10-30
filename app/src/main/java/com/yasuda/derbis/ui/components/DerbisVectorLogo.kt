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
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import timber.log.Timber

private fun safeParsePathNodes(rawPath: String): List<PathNode> = runCatching {
    PathParser().parsePathString(rawPath).toNodes()
}.getOrElse { error ->
    Timber.w(error, "Vector path parse error for: %s", rawPath.take(32))
    emptyList()
}

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
        pathData = safeParsePathNodes(
            "M 3,9 v 6 M 6,6 v 12 M 9,4 v 16 M 12,2 v 20 M 15,5 v 14 M 18,7 v 10 M 21,10 v 4"
        ),
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
        pathData = safeParsePathNodes(
            "M 5,4 h 14 a 3,3 0 0 1 3,3 v 9 a 3,3 0 0 1 -3,3 h -4 l -3.5,2.5 a 0.8,0.8 0 0 1 -1.3,-0.6 V 19 H 5 a 3,3 0 0 1 -3,-3 V 7 a 3,3 0 0 1 3,-3 z"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Synced rhythmic lyric bars
        pathData = safeParsePathNodes(
            "M 8,8.5 h 8 M 8,12.5 h 5 M 8,15.5 h 7"
        ),
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
        pathData = safeParsePathNodes(
            "M 12,4 v 10 M 7.5,10.5 L 12,15 l 4.5,-4.5"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.1f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Storage cradle
        pathData = safeParsePathNodes(
            "M 5,16.5 v 1.5 a 2.5,2.5 0 0 0 2.5,2.5 h 9 a 2.5,2.5 0 0 0 2.5,-2.5 v -1.5"
        ),
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
        pathData = safeParsePathNodes(
            "M 7.5,4.5 l 5.5,5.5 l -5.5,5.5"
        ),
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
        pathData = safeParsePathNodes(
            "M 7,3 h 10 a 3,3 0 0 1 3,3 v 12 a 3,3 0 0 1 -3,3 H 7 a 3,3 0 0 1 -3,-3 V 6 a 3,3 0 0 1 3,-3 z"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Audio wave bars inside card
        pathData = safeParsePathNodes(
            "M 9,12 v 2 M 12,10 v 6 M 15,11 v 4"
        ),
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
        pathData = safeParsePathNodes(
            "M 7,3 h 10 a 4,4 0 0 1 4,4 v 10 a 4,4 0 0 1 -4,4 H 7 a 4,4 0 0 1 -4,-4 V 7 a 4,4 0 0 1 4,-4 z"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Center lens circle
        pathData = safeParsePathNodes(
            "M 12,8.5 a 3.5,3.5 0 1 0 0,7 a 3.5,3.5 0 0 0 0,-7 z"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f
    ).addPath(
        // Top right flash dot
        pathData = safeParsePathNodes(
            "M 17.5,6.5 h 0.01"
        ),
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
        pathData = safeParsePathNodes(
            "M 12,4 v 10 M 7.5,10.5 L 12,15 l 4.5,-4.5"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.1f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Gallery tray
        pathData = safeParsePathNodes(
            "M 5,16.5 v 1.5 a 2.5,2.5 0 0 0 2.5,2.5 h 9 a 2.5,2.5 0 0 0 2.5,-2.5 v -1.5"
        ),
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
        pathData = safeParsePathNodes(
            "M 12,3 c 0,4.5 -3.5,8 -8,8 c 4.5,0 8,3.5 8,8 c 0,-4.5 3.5,-8 8,-8 c -4.5,0 -8,-3.5 -8,-8 z"
        ),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        // Small accent sparkle
        pathData = safeParsePathNodes(
            "M 19,4 c 0,1.5 -1,2.5 -2.5,2.5 c 1.5,0 2.5,1 2.5,2.5 c 0,-1.5 1,-2.5 2.5,-2.5 c -1.5,0 -2.5,-1 -2.5,-2.5 z"
        ),
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
        pathData = safeParsePathNodes(
            "M 10,13 a 5,5 0 0 0 7.54,0.54 l 3,-3 a 5,5 0 0 0 -7.07,-7.07 l -1.72,1.71"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).addPath(
        pathData = safeParsePathNodes(
            "M 14,11 a 5,5 0 0 0 -7.54,-0.54 l -3,3 a 5,5 0 0 0 7.07,7.07 l 1.71,-1.71"
        ),
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
        pathData = safeParsePathNodes(
            "M 6,2.5 h 8 a 2,2 0 0 1 2,2 v 11 a 2,2 0 0 1 -2,2 H 6 a 2,2 0 0 1 -2,-2 v -11 a 2,2 0 0 1 2,-2 z"
        ),
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
        pathData = safeParsePathNodes(
            "M 4.5,3 h 11 a 2,2 0 0 1 2,2 v 10 a 2,2 0 0 1 -2,2 h -11 a 2,2 0 0 1 -2,-2 V 5 a 2,2 0 0 1 2,-2 z"
        ),
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
        pathData = safeParsePathNodes(
            "M 3.5,3.5 h 13 a 2,2 0 0 1 2,2 v 13 a 2,2 0 0 1 -2,2 h -13 a 2,2 0 0 1 -2,-2 v -13 a 2,2 0 0 1 2,-2 z"
        ),
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
        pathData = safeParsePathNodes(
            "M 8,11 a 3,3 0 1 0 0,-6 a 3,3 0 0 0 0,6 z"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f
    ).addPath(
        // Left listener shoulders
        pathData = safeParsePathNodes(
            "M 3.5,19.5 c 0,-2.8 2.2,-4.5 4.5,-4.5 s 4.5,1.7 4.5,4.5"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round
    ).addPath(
        // Right listener head
        pathData = safeParsePathNodes(
            "M 16,11 a 3,3 0 1 0 0,-6 a 3,3 0 0 0 0,6 z"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f
    ).addPath(
        // Right listener shoulders
        pathData = safeParsePathNodes(
            "M 11.5,19.5 c 0,-2.8 2.2,-4.5 4.5,-4.5 s 4.5,1.7 4.5,4.5"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round
    ).addPath(
        // Synchronized sound wave arc overhead
        pathData = safeParsePathNodes(
            "M 9.5,3.5 a 3.5,3.5 0 0 1 5,0"
        ),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.0f,
        strokeLineCap = StrokeCap.Round
    ).build()
}
