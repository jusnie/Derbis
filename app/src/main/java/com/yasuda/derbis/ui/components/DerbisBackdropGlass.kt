/*
 * Liquid Glass rendering for the Derbis Liquid Glass skin.
 *
 * The engine is Kyant0/backdrop v2.0.0 (Apache-2.0), vendored under this package
 * (see [com.yasuda.derbis.ui.components.backdrop]). The integration values and the
 * draw pipeline are adapted from EchoMusicApp/Echo-Music's GlassEffectConfig and
 * Modifier.liquidGlass (GPL-3.0), by way of kushagrasinghx/BitChord's LiquidGlass.kt
 * (GPL-3.0). Derbis is GPL-3.0-only, so the whole chain stays under the same terms.
 *
 * What this replaces: the previous hand-rolled RenderEffect pipeline sampled a
 * backdrop that only ever contained the atmospheric gradient, so every panel looked
 * like flat tinted glass. Here the page itself is recorded into a layer and every
 * glass surface samples it — real blur, vibrancy and lens refraction of the artwork
 * and text underneath, the way Apple's material behaves.
 */
package com.yasuda.derbis.ui.components

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.ui.components.backdrop.drawBackdrop
import com.yasuda.derbis.ui.components.backdrop.backdrops.LayerBackdrop
import com.yasuda.derbis.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.yasuda.derbis.ui.components.backdrop.effects.blur
import com.yasuda.derbis.ui.components.backdrop.effects.colorControls
import com.yasuda.derbis.ui.components.backdrop.effects.lens
import com.yasuda.derbis.ui.components.backdrop.highlight.Highlight
import com.yasuda.derbis.ui.components.backdrop.shadow.Shadow

/**
 * The page layer every Liquid Glass surface samples. Null when the glass engine is
 * off — no skin selected, glass disabled, or the device cannot render it — and each
 * surface then falls back to its opaque material.
 */
val LocalDerbisAppBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * The backdrop pipeline needs [android.graphics.RenderEffect] on a
 * [android.graphics.RenderNode], which is Android 12 (API 31) and up.
 */
fun isBackdropGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt >= Build.VERSION_CODES.S

/** Apple-matched defaults, fixed rather than exposed as sliders. */
private const val VIBRANCY = 1f
private const val BLUR_RADIUS_DP = 8f
private const val LENS_HEIGHT = 0.5f
private const val LENS_AMOUNT = 0.5f
private const val LENS_MAX_DP = 48f
private const val SURFACE_OPACITY = 0.4f

/**
 * Fraction of the surface resolution the backdrop is recorded and processed at.
 *
 * A third is nine times fewer pixels through the colour matrix, the blur and the lens
 * shader on as many as six surfaces at once, and the blur hides the upscale.
 */
const val DERBIS_GLASS_RESOLUTION_SCALE = 0.33f

/**
 * Records the page into [LocalDerbisAppBackdrop]. Only drawn when the glass skin is
 * active: recording a whole page into a layer that nothing samples is pure cost.
 */
@Composable
fun rememberDerbisAppBackdrop(enabled: Boolean, background: Color): LayerBackdrop? {
    if (!enabled || !isBackdropGlassSupported()) return null
    return rememberLayerBackdrop(
        onDraw = {
            // A layer records only what is drawn into it. Pages here paint no
            // background of their own, so without this floor the recording is
            // transparent and the glass has nothing to blur.
            drawRect(background)
            drawContent()
        }
    )
}

/**
 * Renders this composable as a Liquid Glass surface sampling
 * [LocalDerbisAppBackdrop]: vibrancy, blur, lens refraction, a rim highlight, a soft
 * shadow and a theme-adaptive tint on top.
 *
 * Returns the receiver unchanged when no backdrop is available, so callers keep
 * their own opaque fallback for unsupported devices and for the low-transparency
 * accessibility settings.
 *
 * [shape] must be a [CornerBasedShape]: the lens effect throws for anything else.
 */
@Composable
fun Modifier.derbisBackdropGlass(
    shape: CornerBasedShape,
    tint: Color = Color.Transparent,
    lensEnabled: Boolean = true
): Modifier {
    val backdrop = LocalDerbisAppBackdrop.current ?: return this
    val density = LocalDensity.current
    val scale = DERBIS_GLASS_RESOLUTION_SCALE
    val blurPx = with(density) { BLUR_RADIUS_DP.dp.toPx() } * scale
    val lensHeightPx = with(density) { (LENS_HEIGHT * LENS_MAX_DP).dp.toPx() } * scale
    val lensAmountPx = with(density) { (LENS_AMOUNT * LENS_MAX_DP).dp.toPx() } * scale
    val showLens = lensEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val surfaceTint = tint.takeIf { it.alpha > 0f }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(saturation = 1f + 0.5f * VIBRANCY)
            blur(blurPx)
            if (showLens) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = true,
                    chromaticAberration = true
                )
            }
        },
        highlight = { Highlight.Default },
        shadow = { Shadow.Default },
        onDrawSurface = if (surfaceTint == null) {
            null
        } else {
            {
                drawRect(color = surfaceTint, size = size)
            }
        },
        backdropScale = scale
    )
}

/**
 * Chrome-strength glass for bars, docks and panels: samples the page backdrop with the
 * full BitChord recipe when the Liquid Glass skin is active, and otherwise falls through
 * to the caller's own frosted or solid material.
 *
 * Wrapped in [composed] because the call sites are plain `Modifier` extensions.
 */
fun Modifier.derbisChromeGlass(
    shape: Shape,
    tint: Color,
    fallback: Modifier
): Modifier = composed {
    val backdropShape = shape as? CornerBasedShape
    if (backdropShape != null && LocalDerbisAppBackdrop.current != null) {
        this.derbisBackdropGlass(shape = backdropShape, tint = tint)
    } else {
        this.then(fallback)
    }
}

/** The tint a glass surface is filled with when the caller does not supply one. */
internal fun defaultGlassSurfaceTint(isLight: Boolean): Color =
    (if (isLight) Color(0xFFFAFAFA) else Color(0xFF121212)).copy(alpha = SURFACE_OPACITY)
