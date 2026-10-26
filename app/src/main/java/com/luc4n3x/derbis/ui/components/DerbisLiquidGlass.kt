package com.luc4n3x.derbis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.derbis.ui.GlassBackdropState
import com.luc4n3x.derbis.ui.DerbisGlassStyle
import com.luc4n3x.derbis.ui.LocalGlassBackdrop
import com.luc4n3x.derbis.ui.LocalDerbisGlassStyle
import com.luc4n3x.derbis.ui.glassSurface
import com.luc4n3x.derbis.ui.playerMix
import com.luc4n3x.derbis.ui.theme.DerbisPlayerDesign

val LocalDerbisLiquidGlassEnabled = staticCompositionLocalOf { false }

private const val GlassSheenShare = 0.5f

enum class DerbisGlassIntensity {
    Subtle,
    Standard,
    Strong
}

@Immutable
data class DerbisGlassTokens(
    val blurRadius: Dp,
    val tintAlpha: Float,
    val fallbackTintAlpha: Float,
    val borderAlpha: Float,
    val highlightAlpha: Float
)

object DerbisGlassDefaults {
    val Subtle = DerbisGlassTokens(
        blurRadius = 18.dp,
        tintAlpha = 0.16f,
        fallbackTintAlpha = 0.24f,
        borderAlpha = 0.26f,
        highlightAlpha = 0.10f
    )
    val Standard = DerbisGlassTokens(
        blurRadius = 24.dp,
        tintAlpha = 0.20f,
        fallbackTintAlpha = 0.30f,
        borderAlpha = 0.34f,
        highlightAlpha = 0.14f
    )
    val Strong = DerbisGlassTokens(
        blurRadius = 30.dp,
        tintAlpha = 0.26f,
        fallbackTintAlpha = 0.38f,
        borderAlpha = 0.42f,
        highlightAlpha = 0.18f
    )

    fun tokens(intensity: DerbisGlassIntensity): DerbisGlassTokens = when (intensity) {
        DerbisGlassIntensity.Subtle -> Subtle
        DerbisGlassIntensity.Standard -> Standard
        DerbisGlassIntensity.Strong -> Strong
    }
}

fun Modifier.derbisGlass(
    shape: Shape,
    baseTint: Color,
    fallbackColor: Color,
    fallbackBorderColor: Color,
    intensity: DerbisGlassIntensity = DerbisGlassIntensity.Standard,
    artworkTint: Color? = null,
    borderWidth: Dp = DerbisPlayerDesign.Hairline
): Modifier = composed {
    if (!LocalDerbisLiquidGlassEnabled.current) {
        return@composed this
            .background(fallbackColor, shape)
            .border(borderWidth, fallbackBorderColor, shape)
    }
    this.resolvedDerbisGlass(
        backdrop = LocalGlassBackdrop.current,
        shape = shape,
        baseTint = baseTint,
        fallbackBorderColor = fallbackBorderColor,
        intensity = intensity,
        artworkTint = artworkTint,
        borderWidth = borderWidth,
        style = LocalDerbisGlassStyle.current
    )
}

internal fun Modifier.resolvedDerbisGlass(
    backdrop: GlassBackdropState?,
    shape: Shape,
    baseTint: Color,
    fallbackBorderColor: Color,
    intensity: DerbisGlassIntensity,
    artworkTint: Color?,
    borderWidth: Dp,
    style: DerbisGlassStyle = DerbisGlassStyle()
): Modifier {
    val tokens = DerbisGlassDefaults.tokens(intensity)
    val harmonizedTint = artworkTint
        ?.let { baseTint.playerMix(it, style.tintMix) }
        ?: baseTint
    val glassTint = harmonizedTint.copy(alpha = (tokens.tintAlpha * style.tintAlphaScale).coerceIn(0.06f, 0.6f))
    val lightweightFallback = harmonizedTint.copy(alpha = tokens.fallbackTintAlpha)
    val highlight = Color.White.copy(alpha = tokens.highlightAlpha)
    val border = fallbackBorderColor.playerMix(highlight, 0.45f)

    return if (backdrop?.enabled == true && backdrop.layer != null) {
        this.glassSurface(
            state = backdrop,
            shape = shape,
            tint = glassTint,
            fallbackColor = lightweightFallback,
            borderColor = border.copy(alpha = tokens.borderAlpha.coerceAtLeast(border.alpha)),
            blurRadius = tokens.blurRadius,
            borderWidth = borderWidth,
            highlightAlpha = tokens.highlightAlpha * GlassSheenShare,
            refraction = true
        )
    } else {
        this
            .background(lightweightFallback, shape)
            .border(borderWidth, border, shape)
    }
}

@Composable
fun DerbisGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    baseTint: Color,
    fallbackColor: Color,
    fallbackBorderColor: Color,
    intensity: DerbisGlassIntensity = DerbisGlassIntensity.Standard,
    artworkTint: Color? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.derbisGlass(
            shape = shape,
            baseTint = baseTint,
            fallbackColor = fallbackColor,
            fallbackBorderColor = fallbackBorderColor,
            intensity = intensity,
            artworkTint = artworkTint
        ),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun DerbisGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = DerbisPlayerDesign.HeaderButton,
    iconSize: Dp = 21.dp,
    tint: Color = DerbisPlayerDesign.TextPrimary,
    baseTint: Color = DerbisPlayerDesign.GlassFill,
    fallbackColor: Color = DerbisPlayerDesign.GlassFill,
    fallbackBorderColor: Color = DerbisPlayerDesign.GlassBorderTop
        .playerMix(DerbisPlayerDesign.GlassBorderBottom, 0.5f),
    artworkTint: Color? = null,
    intensity: DerbisGlassIntensity = DerbisGlassIntensity.Strong,
    shape: Shape = CircleShape,
    enabled: Boolean = true
) {
    SpringIconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            minWidth = maxOf(size, 40.dp),
            minHeight = DerbisPlayerDesign.MinimumTouchTarget
        ),
        enabled = enabled,
        pressedScale = 0.94f,
        contentDescription = contentDescription
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .derbisGlass(
                    shape = shape,
                    baseTint = baseTint,
                    fallbackColor = fallbackColor,
                    fallbackBorderColor = fallbackBorderColor,
                    intensity = intensity,
                    artworkTint = artworkTint
                ),
            contentAlignment = Alignment.Center
        ) {
            PlayerIcon(icon = icon, tint = tint, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun DerbisGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape,
    baseTint: Color,
    fallbackColor: Color,
    fallbackBorderColor: Color,
    artworkTint: Color? = null,
    intensity: DerbisGlassIntensity = DerbisGlassIntensity.Standard,
    content: @Composable BoxScope.() -> Unit
) {
    DerbisGlassSurface(
        modifier = modifier,
        shape = shape,
        baseTint = baseTint,
        fallbackColor = fallbackColor,
        fallbackBorderColor = fallbackBorderColor,
        intensity = intensity,
        artworkTint = artworkTint,
        contentAlignment = Alignment.Center,
        content = content
    )
}
