package com.yasuda.derbis.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

internal val TorqueRiseShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    moveTo(0f, size.height * 0.56f)
    lineTo(size.width, size.height * 0.34f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

internal val TorqueStripShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    moveTo(0f, size.height * 0.74f)
    lineTo(size.width, size.height * 0.62f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

internal object SkinChrome {
    val active: Boolean get() = DerbisSkinManagesLayout
    val outerCorner: Dp get() = if (DerbisIsYouTubeMusic) YouTubeMusicDesign.chipCorner else if (DerbisIsAppleMusic) AppleMusicDesign.cornerL else if (DerbisIsPrism) PrismDesign.surfaceCorner else Nocturne78Design.cardCorner
    val innerCorner: Dp get() = if (DerbisIsAppleMusic) 0.dp else if (DerbisIsYouTubeMusic) YouTubeMusicDesign.artworkCorner else if (DerbisIsPrism) PrismDesign.controlCorner.coerceAtMost(6.dp) else 2.dp
    val fieldShape: Shape get() = RoundedCornerShape(if (DerbisIsPrism) PrismDesign.controlCorner else Nocturne78Design.controlCorner)
    val chipShape: Shape get() = when {
        DerbisIsYouTubeMusic -> YouTubeMusicDesign.chipShape
        DerbisIsTorque -> RoundedCornerShape(2.dp)
        DerbisIsPrism -> CircleShape
        else -> RoundedCornerShape(Nocturne78Design.controlCorner)
    }
    val iconShape: Shape get() = if (DerbisIsAppleMusic) RoundedCornerShape(AppleMusicDesign.cornerM) else if (DerbisIsPrism) PrismDesign.cut(PrismDesign.controlCorner) else RoundedCornerShape(Nocturne78Design.controlCorner)
    val iconBackground: Color get() = when {
        DerbisIsYouTubeMusic -> Color.Transparent
        DerbisIsAppleMusic -> AppleMusicDesign.accent
        DerbisIsTorque || DerbisIsIvory || DerbisIsSpotify -> Color.Transparent
        DerbisIsPrism -> PrismAccent.Tint
        else -> DerbisPanelSoft
    }
    val iconTint: Color get() = if (DerbisIsAppleMusic || DerbisIsYouTubeMusic) Color.White else if (DerbisIsTorque || DerbisIsIvory || DerbisIsSpotify) DerbisText else DerbisCyan
    val chipSelectedContainer: Color get() = when {
        DerbisIsYouTubeMusic -> Color.White
        DerbisIsAppleMusic -> AppleMusicDesign.accent
        DerbisIsTorque -> DerbisActivePalette.blue
        DerbisIsIvory -> IvoryDesign.ink
        DerbisIsSpotify -> SpotifyDesign.green
        DerbisIsPrism -> PrismAccent.Tint
        else -> Color.Transparent
    }
    val chipSelectedContent: Color get() = if (DerbisIsYouTubeMusic) Color.Black else if (DerbisIsTorque || DerbisIsAppleMusic) Color.White else if (DerbisIsSpotify) Color.Black else if (DerbisIsIvory) IvoryDesign.onInk else DerbisCyan
    val labelsUppercase: Boolean get() = DerbisIsNocturne || DerbisIsTorque

    fun label(text: String, locale: Locale): String = if (labelsUppercase) text.uppercase(locale) else text
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SkinSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val torque = DerbisIsTorque
    val travel by animateFloatAsState(if (checked) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "skin-switch")
    val accent = if (torque) DerbisActivePalette.blue else DerbisCyan
    val (width, height) = skinSwitchDimensions()
    Canvas(modifier.size(width = width, height = height)) {
        drawSkinSwitch(checked, travel, accent, DerbisMuted)
    }
}

private fun skinSwitchDimensions(): Pair<Dp, Dp> = when {
    DerbisIsAppleMusic -> 51.dp to 31.dp
    DerbisIsYouTubeMusic -> 40.dp to 24.dp
    else -> 52.dp to 26.dp
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSkinSwitch(
    checked: Boolean,
    travel: Float,
    accent: Color,
    idle: Color
) {
    val stroke = 1.5.dp.toPx()
    val inset = 3.dp.toPx()
    when {
        DerbisIsTorque -> drawTorqueSwitch(checked, travel, accent, idle, stroke, inset)
        DerbisIsIvory -> drawIvorySwitch(travel, IvoryDesign.ink, IvoryDesign.onInk, idle, stroke)
        DerbisIsSpotify -> drawSpotifySwitch(travel)
        DerbisIsAppleMusic -> drawAppleMusicSwitch(travel)
        DerbisIsYouTubeMusic -> drawYouTubeMusicSwitch(travel)
        else -> drawRectangularSwitch(checked, travel, accent, idle, stroke, inset)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTorqueSwitch(checked: Boolean, travel: Float, accent: Color, idle: Color, stroke: Float, inset: Float) {
    val w = size.width
    val h = size.height
    val slant = h * 0.45f
    val track = Path().apply {
        moveTo(slant, 0f); lineTo(w, 0f); lineTo(w - slant, h); lineTo(0f, h); close()
    }
    if (checked) drawPath(track, accent.copy(alpha = 0.25f))
    drawPath(track, if (checked) accent else idle, style = Stroke(stroke))
    val thumbWidth = w * 0.46f
    val x = slant * 0.25f + (w - thumbWidth - slant * 0.5f) * travel
    val thumb = Path().apply {
        moveTo(x + slant, inset); lineTo(x + thumbWidth, inset)
        lineTo(x + thumbWidth - slant, h - inset); lineTo(x, h - inset); close()
    }
    drawPath(thumb, if (checked) accent else idle)

}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIvorySwitch(travel: Float, ink: Color, onInk: Color, idle: Color, stroke: Float) {
    val radius = CornerRadius(size.height / 2)
    drawRoundRect(ink.copy(alpha = travel), cornerRadius = radius)
    drawRoundRect(androidx.compose.ui.graphics.lerp(idle.copy(alpha = 0.6f), ink, travel), cornerRadius = radius, style = Stroke(stroke))
    val dot = size.height * 0.16f + size.height * 0.06f * travel
    val x = size.height / 2 + (size.width - size.height) * travel
    drawCircle(androidx.compose.ui.graphics.lerp(idle, onInk, travel), dot, Offset(x, size.height / 2))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAppleMusicSwitch(travel: Float) {
    val trackHeight = size.height
    drawRoundRect(
        androidx.compose.ui.graphics.lerp(AppleMusicDesign.secondaryFill, AppleMusicSystemGreen, travel),
        cornerRadius = CornerRadius(trackHeight / 2)
    )
    val radius = trackHeight / 2 - 2.dp.toPx()
    val x = trackHeight / 2 + (size.width - trackHeight) * travel
    drawCircle(Color.Black.copy(alpha = 0.10f), radius + 0.6.dp.toPx(), Offset(x, size.height / 2 + 1.dp.toPx()))
    drawCircle(Color.White, radius, Offset(x, size.height / 2))
}

private val AppleMusicSystemGreen = Color(0xFF11D964)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawYouTubeMusicSwitch(travel: Float) {
    val trackHeight = 14.dp.toPx()
    val thumb = 10.dp.toPx()
    val top = (size.height - trackHeight) / 2f
    val trackColor = androidx.compose.ui.graphics.lerp(YouTubeMusicDesign.textTertiary, Color.White.copy(alpha = 0.5f), travel)
    val thumbColor = androidx.compose.ui.graphics.lerp(YouTubeMusicDesign.textSecondary, Color.White, travel)
    drawRoundRect(trackColor, androidx.compose.ui.geometry.Offset(thumb, top), androidx.compose.ui.geometry.Size(size.width - thumb * 2f, trackHeight), androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f))
    drawCircle(thumbColor, thumb, androidx.compose.ui.geometry.Offset(thumb + (size.width - thumb * 2f) * travel, size.height / 2f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpotifySwitch(travel: Float) {
    val trackHeight = size.height * 0.8f
    val top = (size.height - trackHeight) / 2
    drawRoundRect(
        androidx.compose.ui.graphics.lerp(SpotifyDesign.faint, SpotifyDesign.green, travel),
        Offset(0f, top), androidx.compose.ui.geometry.Size(size.width, trackHeight), CornerRadius(trackHeight / 2)
    )
    val radius = trackHeight / 2 - 2.dp.toPx()
    val x = trackHeight / 2 + (size.width - trackHeight) * travel
    drawCircle(androidx.compose.ui.graphics.lerp(SpotifyDesign.subdued, Color.White, travel), radius, Offset(x, size.height / 2))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRectangularSwitch(checked: Boolean, travel: Float, accent: Color, idle: Color, stroke: Float, inset: Float) {
    val w = size.width
    val h = size.height
    val radius = CornerRadius(3.dp.toPx())
    if (checked) drawRoundRect(accent.copy(alpha = 0.22f), cornerRadius = radius)
    drawRoundRect(if (checked) accent else idle, cornerRadius = radius, style = Stroke(stroke))
    val thumb = h - inset * 2
    val x = inset + (w - thumb - inset * 2) * travel
    drawRoundRect(if (checked) accent else idle, Offset(x, inset), Size(thumb, thumb), CornerRadius(2.dp.toPx()))
}
