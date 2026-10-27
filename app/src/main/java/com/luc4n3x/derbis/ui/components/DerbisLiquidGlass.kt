package com.luc4n3x.derbis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
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
import com.luc4n3x.derbis.ui.LocalDerbisGlassAccessibility
import com.luc4n3x.derbis.ui.LocalGlassBackdrop
import com.luc4n3x.derbis.ui.LocalDerbisGlassStyle
import com.luc4n3x.derbis.ui.glassSurface
import com.luc4n3x.derbis.ui.playerMix
import com.luc4n3x.derbis.ui.theme.DerbisActivePalette
import com.luc4n3x.derbis.ui.theme.DerbisIsLiquidGlass
import com.luc4n3x.derbis.ui.theme.DerbisPanel
import com.luc4n3x.derbis.ui.theme.DerbisPlayerDesign

val LocalDerbisLiquidGlassEnabled = staticCompositionLocalOf { false }

/**
 * The dedicated Liquid Glass skin is a full-system material: screens, cards, grouped rows and
 * controls all participate in the shared frosted backdrop. Other skins remain unaffected.
 */
private const val GlassSheenShare = 0.62f

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
    val highlightAlpha: Float,
    /** Content containers use a quiet translucent material rather than sampling glass again. */
    val samplesBackdrop: Boolean,
    /** Lens distortion is reserved for the larger, directly manipulable chrome pieces. */
    val allowsRefraction: Boolean
)

object DerbisGlassDefaults {
    // Full-system Liquid Glass intentionally frosts content groups as well as chrome. This is
    // only selected by the Liquid Glass skin; other skins retain their ordinary content surfaces.
    val Subtle = DerbisGlassTokens(
        blurRadius = 16.dp,
        tintAlpha = 0.14f,
        fallbackTintAlpha = 0.62f,
        borderAlpha = 0.30f,
        highlightAlpha = 0.085f,
        samplesBackdrop = true,
        allowsRefraction = false
    )
    // Standard is for one primary functional layer: a dock, sheet header, player transport or
    // a floating search/control group.
    val Standard = DerbisGlassTokens(
        blurRadius = 22.dp,
        tintAlpha = 0.15f,
        fallbackTintAlpha = 0.66f,
        borderAlpha = 0.34f,
        highlightAlpha = 0.11f,
        samplesBackdrop = true,
        allowsRefraction = false
    )
    // Strong is reserved for compact, intentional controls such as transport and primary icon
    // buttons. It supplies the only lensing treatment in the shared vocabulary.
    val Strong = DerbisGlassTokens(
        blurRadius = 28.dp,
        tintAlpha = 0.19f,
        fallbackTintAlpha = 0.76f,
        borderAlpha = 0.46f,
        highlightAlpha = 0.16f,
        samplesBackdrop = true,
        allowsRefraction = true
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
        fallbackColor = fallbackColor,
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
    fallbackColor: Color,
    fallbackBorderColor: Color,
    intensity: DerbisGlassIntensity,
    artworkTint: Color?,
    borderWidth: Dp,
    style: DerbisGlassStyle = DerbisGlassStyle()
): Modifier = composed {
    val tokens = DerbisGlassDefaults.tokens(intensity)
    val accessibility = LocalDerbisGlassAccessibility.current
    val harmonizedTint = artworkTint
        ?.let { baseTint.playerMix(it, style.tintMix) }
        ?: baseTint
    val opaqueMaterial = accessibility.prefersOpaqueMaterials || style.prefersOpaqueMaterials
    // The fallback is intentionally more opaque than the frosted version. It protects type,
    // icon contrast and focus rings on API levels without RenderEffect, Low Power mode and when
    // the user chooses the low-transparency end of the Glass controls.
    val fallbackAlpha = (tokens.fallbackTintAlpha + if (opaqueMaterial) 0.18f else 0f)
        .coerceIn(0.42f, 0.96f)
    val glassTint = harmonizedTint.copy(
        alpha = if (opaqueMaterial) fallbackAlpha else (tokens.tintAlpha * style.tintAlphaScale).coerceIn(0.08f, 0.42f)
    )
    // Callers provide an opaque theme fallback as well as translucent contextual fallbacks;
    // use its hue for a stable material but let the shared role own opacity consistently.
    val materialFallback = fallbackColor
        .playerMix(harmonizedTint, 0.12f + style.tintMix * 0.28f)
        .copy(alpha = fallbackAlpha)
    val highlight = Color.White.copy(alpha = if (opaqueMaterial) tokens.highlightAlpha * 0.55f else tokens.highlightAlpha)
    val border = fallbackBorderColor.playerMix(highlight, 0.52f)
    val shouldFrost = tokens.samplesBackdrop && !opaqueMaterial && backdrop?.enabled == true && backdrop.layer != null

    if (shouldFrost) {
        this.glassSurface(
            state = checkNotNull(backdrop),
            shape = shape,
            tint = glassTint,
            fallbackColor = materialFallback,
            borderColor = border.copy(alpha = tokens.borderAlpha.coerceAtLeast(border.alpha)),
            blurRadius = tokens.blurRadius,
            borderWidth = borderWidth,
            highlightAlpha = tokens.highlightAlpha * GlassSheenShare,
            refraction = tokens.allowsRefraction && !accessibility.increasedContrast
        )
    } else {
        this
            .background(materialFallback, shape)
            .border(borderWidth, border.copy(alpha = tokens.borderAlpha.coerceAtLeast(border.alpha)), shape)
    }
}
/**
 * A full-page material used only by the Liquid Glass skin. It puts every screen on the shared
 * atmospheric backdrop before its local cards, rows, controls and sheets add their own glass.
 */
internal fun Modifier.derbisSystemGlass(): Modifier = composed {
    if (!LocalDerbisLiquidGlassEnabled.current || !DerbisIsLiquidGlass) {
        this
    } else {
        this.derbisGlass(
            shape = RectangleShape,
            baseTint = DerbisPanel,
            fallbackColor = DerbisPanel.copy(alpha = 0.76f),
            fallbackBorderColor = Color.Transparent,
            intensity = DerbisGlassIntensity.Standard,
            artworkTint = DerbisActivePalette.cyan,
            borderWidth = 0.dp
        )
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
