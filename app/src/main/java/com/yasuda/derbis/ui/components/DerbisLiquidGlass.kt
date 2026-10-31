package com.yasuda.derbis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
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
import com.yasuda.derbis.ui.GlassBackdropQuality
import com.yasuda.derbis.ui.GlassLensTier
import com.yasuda.derbis.ui.LocalDerbisGlassTier
import com.yasuda.derbis.ui.DerbisGlassStyle
import com.yasuda.derbis.ui.LocalDerbisGlassAccessibility
import com.yasuda.derbis.ui.LocalDerbisGlassStyle
import com.yasuda.derbis.ui.playerMix
import com.yasuda.derbis.ui.theme.DerbisActivePalette
import com.yasuda.derbis.ui.theme.DerbisIsLiquidGlass
import com.yasuda.derbis.ui.theme.DerbisPanel
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import androidx.compose.foundation.shape.CornerBasedShape

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
    val allowsRefraction: Boolean,
    /**
     * Frost sampling bucket for this role. Rows and cards ask for a coarser bucket than chrome:
     * the backdrop is already blurred, so half-resolution frosting is indistinguishable while
     * costing a quarter of the pixels per panel — which is what keeps long glass lists smooth.
     */
    val samplingQuality: GlassBackdropQuality
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
        allowsRefraction = false,
        samplingQuality = GlassBackdropQuality.Efficient
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
        allowsRefraction = false,
        samplingQuality = GlassBackdropQuality.Balanced
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
        allowsRefraction = true,
        // Small controls keep the sharp bucket: at their size the frost layer is cheap, and a
        // soft edge would blunt the lens.
        samplingQuality = GlassBackdropQuality.Native
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
    // Efficient-tier devices only pay for real frosting on the small chrome controls. Content
    // rows and page surfaces keep the translucent material, which looks identical at rest and
    // removes dozens of per-frame layer samples from a scrolling settings or library screen.
    val tier = LocalDerbisGlassTier.current
    // The Liquid Glass skin now draws real glass: the page is recorded into a backdrop
    // layer and every chrome-strength surface samples it (blur, vibrancy, lens). Subtle
    // content rows keep the cheap translucent material, because sampling the backdrop
    // per list row is what would cost frames while scrolling.
    val backdropShape = shape as? CornerBasedShape
    val backdropReady = backdropShape != null &&
        !opaqueMaterial &&
        intensity != DerbisGlassIntensity.Subtle &&
        LocalDerbisAppBackdrop.current != null
    if (backdropReady && backdropShape != null) {
        val lensAllowed = tokens.allowsRefraction &&
            tier != GlassLensTier.Efficient &&
            !accessibility.increasedContrast
        return@composed this.derbisBackdropGlass(
            shape = backdropShape,
            // BitChord's surface film: a neutral near-black (or near-white) at 40%, not a
            // tint of the artwork. Anything stronger hides the page it is supposed to show.
            tint = defaultGlassSurfaceTint(DerbisActivePalette.isLight),
            lensEnabled = lensAllowed
        )
    }
    // No frost layer any more: a surface either samples the page backdrop with the real
    // glass, or it is BitChord's lightweight material — the same tint with the edge light —
    // because a card drawn inside the recorded page must not sample that page. The old
    // blurred RenderEffect sample was a second, weaker glass stacked under the real one.
    this
        .derbisLightweightGlass(
            shape = shape,
            fill = materialFallback,
            rim = !opaqueMaterial && tier != GlassLensTier.Efficient,
            hairline = false
        )
        .border(borderWidth, border.copy(alpha = tokens.borderAlpha.coerceAtLeast(border.alpha)), shape)
}

/**
 * A full-page material used only by the Liquid Glass skin. It seats every screen on the shared
 * atmospheric backdrop before its local cards, rows, controls and sheets add their own glass.
 *
 * This deliberately does **not** sample the backdrop: a full-screen frost layer is re-recorded
 * and re-blurred every frame, which is by far the most expensive glass surface in the app and
 * buys almost nothing, because what sits behind a page is already a soft gradient. A cheap
 * translucent film plus a top sheen reads as the same material for free, so every screen can
 * carry glass without costing a frame.
 */
internal fun Modifier.derbisSystemGlass(): Modifier = composed {
    if (!LocalDerbisLiquidGlassEnabled.current || !DerbisIsLiquidGlass) {
        this
    } else {
        // Deliberately light: the page film only needs to seat the atmosphere, not hide it.
        val film = DerbisPanel.copy(
            alpha = (DerbisPanel.alpha * PageGlassFilmShare).coerceIn(0.10f, 0.34f)
        )
        this.drawWithContent {
            drawRect(film)
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = PageGlassSheenAlpha),
                    0.28f to Color.Transparent
                )
            )
            drawContent()
        }
    }
}

private const val PageGlassFilmShare = 0.45f
private const val PageGlassSheenAlpha = 0.05f

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
