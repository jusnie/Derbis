package com.yasuda.derbis.ui.lyrics.spicy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.ui.theme.OutfitFont

internal val SpicyInk = Color(0xFF121015)
internal val SpicySurface = Color(0xFF211B22)
internal val SpicyPaper = Color(0xFFFFF2E8)
internal val SpicyMuted = Color(0xFFB6A7B0)
internal val SpicyAccentCoral = Color(0xFFFF876E)
internal val SpicyAccentGold = Color(0xFFECC58E)
internal val SpicyVerseShape = RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp)
internal val SpicyDockShape = RoundedCornerShape(26.dp, 10.dp, 26.dp, 10.dp)
internal val SpicyGradientBrush = Brush.horizontalGradient(listOf(SpicyAccentCoral, SpicyAccentGold))

internal object SpicyLyricsIcons {
    val Flame = lineIcon(
        "Flame",
        "M13,2.5 C14,6.5 10,7.5 10.5,11 C8.5,10 8,8.5 8,7 C4,10 3.5,13 5,16.5 C7,21 14,23 18,18.5 C22,14 18,8.5 16,7 C16.5,11 14.5,11.5 14,10 C13.5,8 16,6 13,2.5 Z"
    )
    val Close = lineIcon("Close", "M6,6 L18,18 M18,6 L6,18")
    val Focus = lineIcon(
        "Focus",
        "M8,3 L3,3 L3,8 M16,3 L21,3 L21,8 M3,16 L3,21 L8,21 M21,16 L21,21 L16,21 M7,12 L10,12 L12,8 L15,16 L17,12"
    )

    private fun lineIcon(name: String, path: String): ImageVector = ImageVector.Builder(
        name = "Spicy.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(path).toNodes(),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

@Composable
internal fun SpicyBrandBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(SpicyLyricsIcons.Flame, null, Modifier.size(22.dp), tint = SpicyAccentCoral)
        Text(
            text = "SPICY",
            color = SpicyPaper,
            fontFamily = OutfitFont,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
internal fun SpicyLyricsBackdrop(
    accentStart: Color,
    accentEnd: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .background(SpicyInk)
            .drawWithCache {
                val tint = lerp(SpicyAccentCoral, accentStart, 0.16f)
                val paths = List(5) { index ->
                    val shift = index * size.width * 0.09f
                    Path().apply {
                        moveTo(size.width * 0.45f + shift, -size.height * 0.05f)
                        cubicTo(
                            size.width * 0.05f + shift, size.height * 0.26f,
                            size.width * 1.28f + shift, size.height * 0.15f,
                            size.width * 0.90f + shift, size.height * 0.62f
                        )
                    }
                }
                val wash = Brush.linearGradient(
                    colors = listOf(lerp(SpicyInk, tint, 0.035f), SpicyInk, lerp(SpicyInk, accentEnd, 0.02f)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                onDrawBehind {
                    drawRect(wash)
                    paths.forEach { drawPath(it, tint.copy(alpha = 0.075f), style = Stroke(1.dp.toPx())) }
                }
            }
    )
}

@Composable
internal fun SpicyTickTimeline(
    progress: Float,
    modifier: Modifier = Modifier,
    showCursor: Boolean = false,
    enabled: Boolean = true
) {
    Canvas(modifier.height(22.dp)) {
        val fraction = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        val count = (size.width / 6.dp.toPx()).toInt().coerceIn(12, 72)
        val step = size.width / count
        val tickWidth = minOf(2.dp.toPx(), step * 0.38f)
        for (index in 0 until count) {
            val x = step * (index + 0.5f)
            val tickHeight = if (index % 5 == 0) 11.dp.toPx() else 6.dp.toPx()
            val filled = enabled && index.toFloat() / count < fraction
            drawRoundRect(
                color = if (filled) lerp(SpicyAccentCoral, SpicyAccentGold, index.toFloat() / count)
                    else SpicyPaper.copy(alpha = if (enabled) 0.18f else 0.08f),
                topLeft = Offset(x - tickWidth / 2f, (size.height - tickHeight) / 2f),
                size = Size(tickWidth, tickHeight),
                cornerRadius = CornerRadius(tickWidth / 2f)
            )
        }
        if (showCursor && enabled) {
            val cursorWidth = 3.dp.toPx()
            val x = (size.width * fraction).coerceIn(cursorWidth / 2f, (size.width - cursorWidth / 2f).coerceAtLeast(cursorWidth / 2f))
            drawRoundRect(
                color = SpicyPaper,
                topLeft = Offset(x - cursorWidth / 2f, (size.height - 19.dp.toPx()) / 2f),
                size = Size(cursorWidth, 19.dp.toPx()),
                cornerRadius = CornerRadius(cursorWidth / 2f)
            )
        }
    }
}
