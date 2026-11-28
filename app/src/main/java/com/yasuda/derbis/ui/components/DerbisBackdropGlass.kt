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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.ui.components.backdrop.DerbisGlassSafety
import com.yasuda.derbis.ui.components.backdrop.drawBackdrop
import com.yasuda.derbis.ui.components.backdrop.backdrops.LayerBackdrop
import com.yasuda.derbis.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.yasuda.derbis.ui.components.backdrop.effects.blur
import com.yasuda.derbis.ui.components.backdrop.effects.colorControls
import com.yasuda.derbis.ui.components.backdrop.effects.lens
import com.yasuda.derbis.ui.components.backdrop.highlight.Highlight
import com.yasuda.derbis.ui.components.backdrop.highlight.HighlightElement
import com.yasuda.derbis.ui.GlassLensTier
import com.yasuda.derbis.ui.LocalDerbisGlassAccessibility
import com.yasuda.derbis.ui.LocalDerbisGlassTier
import com.yasuda.derbis.ui.components.backdrop.internal.ShapeProvider
import com.yasuda.derbis.ui.components.backdrop.shadow.Shadow

/**
 * The page layer every Liquid Glass surface samples. Null when the glass engine is
 * off — no skin selected, glass disabled, or the device cannot render it — and each
 * surface then falls back to its opaque material.
 */
val LocalDerbisAppBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * The player deck's own artwork backdrop, recorded separately from the page:
 * the Liquid Glass reference player paints its full-bleed blurred-cover scene
 * here, and the press-and-hold droplet magnifies that recording — not the page
 * behind the player, which the droplet has no business revealing. Null outside
 * the glass skin.
 */
val LocalDerbisPlayerBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * The backdrop pipeline needs [android.graphics.RenderEffect] on a
 * [android.graphics.RenderNode], which is Android 12 (API 31) and up.
 */
fun isBackdropGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt >= Build.VERSION_CODES.S

/** Apple-matched defaults, fixed rather than exposed as sliders. */
private const val VIBRANCY = 1f
private const val BLUR_RADIUS_DP = 16f
private const val LENS_HEIGHT = 0.5f
private const val LENS_AMOUNT = 0.5f
private const val LENS_MAX_DP = 48f

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
    if (!enabled || !isBackdropGlassSupported() || DerbisGlassSafety.disabled) return null
    return rememberLayerBackdrop(
        onDraw = {
            // Reaching this lambda means the GPU path is live for this process; the
            // crash guard uses that to decide whether a process death is worth
            // blaming on glass.
            DerbisGlassSafety.noteDrawn()
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
 * [shape] should be a [CornerBasedShape]; anything else keeps a plain frosted panel.
 */
@Composable
fun Modifier.derbisBackdropGlass(
    shape: CornerBasedShape,
    tint: Color = Color.Transparent,
    lensEnabled: Boolean = true,
    /** Optional accent used for the artwork bloom under the content (defaults off for callers without artwork). */
    bloom: Color? = null
): Modifier {
    val backdrop = LocalDerbisAppBackdrop.current
        ?.takeUnless { DerbisGlassSafety.disabled }
        ?: return this
    val density = LocalDensity.current
    // The cheap tier is what a low-RAM device and the battery saver get now that they no
    // longer lose the material outright: no refraction, and a coarser recording.
    val tier = LocalDerbisGlassTier.current
    val accessibility = LocalDerbisGlassAccessibility.current
    val scale = if (tier == GlassLensTier.Efficient) 0.25f else DERBIS_GLASS_RESOLUTION_SCALE
    val blurPx = with(density) { BLUR_RADIUS_DP.dp.toPx() } * scale
    val lensHeightPx = with(density) { (LENS_HEIGHT * LENS_MAX_DP).dp.toPx() } * scale
    val lensAmountPx = with(density) { (LENS_AMOUNT * LENS_MAX_DP).dp.toPx() } * scale
    // Same rule the other glass pipeline uses: a lens is an optical allowance, and the
    // "reduce motion and effects" and high-contrast requests outrank it.
    val showLens = lensEnabled &&
        tier != GlassLensTier.Efficient &&
        !accessibility.increasedContrast &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val surfaceTint = tint.takeIf { it.alpha > 0f }

    val prominentRim = lensEnabled
    val sheen = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (prominentRim) 0.62f else 0.44f),
        0.42f to Color.White.copy(alpha = 0.06f),
        1f to Color.White.copy(alpha = if (prominentRim) 0.22f else 0.14f)
    )
    val glow = bloom?.takeIf { it.alpha > 0f }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(saturation = 1f + 0.75f * VIBRANCY)
            blur(blurPx)
            if (showLens) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = true,
                    // iOS draws the dispersion fringes only on the full tier; the balanced
                    // tier keeps the lens but not the colour split.
                    chromaticAberration = tier == GlassLensTier.Full
                )
            }
        },
        highlight = { Highlight.Default },
        shadow = {
            // iOS light drops black@0.10 under every tier; night drops 0.46 for chrome and
            // 0.30 for content. DerbisGlassTier cannot express radius, so shadow strength
            // follows the same prominentRim / lensEnabled signals the surface already uses.
            val isLight = com.yasuda.derbis.ui.theme.DerbisActivePalette.isLight
            val radius = if (prominentRim) 16.dp else 8.dp
            val offset = DpOffset(0.dp, if (prominentRim) 8.dp else 4.dp)
            val strength = when {
                isLight -> 0.10f
                prominentRim -> 0.46f
                else -> 0.30f
            }
            Shadow(radius = radius, offset = offset, color = Color.Black.copy(alpha = strength))
        },
        onDrawSurface = if (surfaceTint == null && glow == null) {
            null
        } else {
            {
                if (surfaceTint != null) drawRect(color = surfaceTint, size = size)
                // Bloom: a soft artwork-accent wash over the blurred sample, under the
                // content. This is the iOS bloom: the light that makes the panel read as
                // lit from behind rather than merely frosted.
                if (glow != null) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glow.copy(alpha = 0.34f),
                                glow.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.18f),
                            radius = size.maxDimension * 0.82f
                        ),
                        size = size
                    )
                }
                // Sheen: the short light a ceiling throws across the upper third of a pane.
                // The rim catches the edge; this is the body of the pane picking it up.
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (prominentRim) 0.16f else 0.10f),
                        0.26f to Color.White.copy(alpha = 0.03f),
                        0.52f to Color.Transparent
                    ),
                    size = size
                )
            }
        },
        backdropScale = scale
    ).then(
        // Specular rim: a one-pixel edge light along the top of the material, drawn above
        // everything else on the surface, the same way the iOS rim catches the top edge.
        Modifier.border(1.dp, sheen, shape)
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
    fallback: Modifier,
    /** Artwork accent for the bloom wash, when the caller has it. Bars and docks pass the
     *  dominant artwork colour; panels pass null and draw their own bloom. */
    bloom: Color? = null
): Modifier = composed {
    val backdropShape = shape as? CornerBasedShape
    if (backdropShape != null && LocalDerbisAppBackdrop.current != null) {
        this.derbisBackdropGlass(shape = backdropShape, tint = tint, bloom = bloom)
    } else {
        this.then(fallback)
    }
}

/** BitChord's edge hairline, drawn where the glass rim itself is not rendered. */
internal val DerbisGlassEdgeWidth = 0.5.dp

internal fun derbisGlassEdgeColor(isLight: Boolean): Color =
    if (isLight) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.10f)

/**
 * The Taurus optical substrate, drawn locally by every lightweight glass surface.
 *
 * The iOS material never samples the page: each surface paints its own small "field" (the skin
 * atmosphere plus the accent ramps), films it and rims it. This is the Android twin of that
 * substrate, used wherever a surface cannot or must not sample the recorded page layer — inside
 * a recorded page, on Android 11 and older, in the cheap tiers. The blooms are soft by
 * construction (large radial gradients), so no blur pass is required and the cost stays a single
 * draw pass.
 *
 * Layer order mirrors the iOS material: base, field blooms, caller film, content, sheen.
 */
private object DerbisOpticalSubstrate {
    // The cool spectral refraction ramp of the Liquid Glass skin (night pair / light pair).
    val accentDark = Color(0xFF38BDF8)    // nightStart (iOS liquidGlass accent)
    val accentLight = Color(0xFF007B9B)   // lightStart
    val secondaryDark = Color(0xFF818CF8)  // nightEnd (iOS liquidGlass secondaryAccent)
    val secondaryLight = Color(0xFF5E5CC6) // lightEnd

    val baseDark = Color(0xFF080A10)
    // iOS light field paints pure white underneath the blooms; kept here for parity.
    val baseLight = Color(0xFFFFFFFF)

    fun base(isLight: Boolean): Color = if (isLight) baseLight else baseDark
    fun accent(isLight: Boolean): Color = if (isLight) accentLight else accentDark
    fun secondary(isLight: Boolean): Color = if (isLight) secondaryLight else secondaryDark
}

private fun pathOf(outline: Outline): Path = when (outline) {
    is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
    is Outline.Generic -> outline.path
}

/**
 * BitChord's lightweight glass, rebuilt to the Taurus optical recipe: a locally-drawn field
 * (base + two accent blooms), the caller's film, the ceiling sheen above the content and, when
 * [rim] is set, the dispersed lens-band fringe and the directional [Highlight] — with no sample
 * of the page behind. This is what a content row, a card or a control draws when it must not
 * sample the layer it is being recorded into, and what every surface draws when the backdrop
 * engine cannot run at all (Android 11 and older, high-contrast materials).
 */
fun Modifier.derbisLightweightGlass(
    shape: Shape,
    fill: Color,
    rim: Boolean = false,
    hairline: Boolean = true
): Modifier {
    val corner = shape as? CornerBasedShape
    val isLight = com.yasuda.derbis.ui.theme.DerbisActivePalette.isLight
    val base = DerbisOpticalSubstrate.base(isLight)
    val accent = DerbisOpticalSubstrate.accent(isLight)
    val secondary = DerbisOpticalSubstrate.secondary(isLight)
    return this
        .clip(shape)
        .drawWithContent {
            // Base: the near-black pane the field is lit on top of.
            drawRect(base)
            // Field: two radial blooms, top-leading and bottom-trailing — the same sides the
            // iOS field blooms from.
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.20f),
                        accent.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(0f, 0f),
                    radius = size.maxDimension * 0.92f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = 0.17f),
                        secondary.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 1.08f, size.height * 1.12f),
                    radius = size.maxDimension * 0.86f
                )
            )
            if (rim) {
                // Chrome-tier bloom: the third light from the lower-leading corner.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondary.copy(alpha = 0.11f),
                            Color.Transparent
                        ),
                        center = Offset(0f, size.height * 1.1f),
                        radius = size.maxDimension * 0.7f
                    )
                )
            }
            // Film: the caller's tint sits over the field and under the content.
            drawRect(fill)
            drawContent()
            // Sheen: the short light a ceiling throws across the upper third of a pane.
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (rim) 0.15f else 0.10f),
                    0.26f to Color.White.copy(alpha = 0.03f),
                    0.52f to Color.Transparent
                )
            )
        }
        .then(
            if (rim) {
                Modifier.drawWithContent {
                    drawContent()
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val fringe = pathOf(outline)
                    val stroke = 1.4.dp.toPx()
                    val shift = 0.7.dp.toPx()
                    // Two sub-pixel-offset gradient strokes split the accent into its colour
                    // fringes — the refraction cue a flat fill can never imitate.
                    withTransform({ translate(shift, 0f) }) {
                        drawPath(
                            fringe,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.30f),
                                    Color.White.copy(alpha = 0.14f),
                                    secondary.copy(alpha = 0.24f)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            ),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                            blendMode = BlendMode.Plus
                        )
                    }
                    withTransform({ translate(-shift, 0f) }) {
                        drawPath(
                            fringe,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    secondary.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.10f),
                                    accent.copy(alpha = 0.26f)
                                ),
                                start = Offset(size.width, size.height),
                                end = Offset.Zero
                            ),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                            blendMode = BlendMode.Plus
                        )
                    }
                }
            } else {
                Modifier
            }
        )
        .then(
            if (rim && corner != null) {
                HighlightElement(
                    shapeProvider = ShapeProvider { corner },
                    highlight = { Highlight.Default }
                )
            } else {
                Modifier
            }
        )
        .then(
            if (hairline) Modifier.border(DerbisGlassEdgeWidth, derbisGlassEdgeColor(isLight), shape) else Modifier
        )
}


/** The tint a glass surface is filled with when the caller does not supply one. */
internal fun defaultGlassSurfaceTint(isLight: Boolean): Color =
    if (isLight) {
        // Taurus parity: the light film is a dense white over the field (the iOS default at
        // 50 transparency is min(0.96, 0.56 + 0.34) = 0.90), not a faint wash.
        Color.White.copy(alpha = 0.90f)
    } else {
        // Taurus parity: night chrome is a white film over the blurred page, not a dark slab —
        // matches the iOS surface film (white@0.065) with the reduced-transparency lift
        // (white@0.16) so the rim always reads.
        Color.White.copy(alpha = 0.16f)
    }
