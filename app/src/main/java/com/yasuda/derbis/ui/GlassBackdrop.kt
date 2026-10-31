package com.yasuda.derbis.ui

import android.app.ActivityManager
import android.view.accessibility.AccessibilityManager
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader as AndroidShader
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.yasuda.derbis.domain.DerbisGlassEffects
import com.yasuda.derbis.ui.theme.LocalDerbisVisualCapabilities
import com.yasuda.derbis.ui.theme.rememberPowerSaveMode
import kotlin.math.ceil

/**
 * Lens budget for the current device. Provided once by the app root; composition-local rather
 * than per-panel so a screen with thirty glass rows does not query ActivityManager thirty times.
 */
val LocalDerbisGlassTier = staticCompositionLocalOf { GlassLensTier.Full }

@Composable
fun rememberDerbisGlassTier(
    heavyBlur: Boolean = LocalDerbisVisualCapabilities.current.heavyBlur
): GlassLensTier {
    val context = LocalContext.current.applicationContext
    val lowRam = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val powerSave = rememberPowerSaveMode()
    val effects = LocalDerbisGlassStyle.current.effects
    return remember(heavyBlur, lowRam, powerSave, effects) {
        resolveGlassLensTier(heavyBlur, lowRam, powerSave, effects)
    }
}

/**
 * Accessibility state for shared glass material. Android exposes High text contrast rather than a
 * direct "Reduce transparency" switch, so high contrast selects an opaque, bordered material.
 * The user-facing transparency setting can request the same safe treatment through
 * [DerbisGlassStyle.prefersOpaqueMaterials].
 */
@Immutable
data class DerbisGlassAccessibility(
    val increasedContrast: Boolean = false
) {
    val prefersOpaqueMaterials: Boolean get() = increasedContrast
}

val LocalDerbisGlassAccessibility = staticCompositionLocalOf { DerbisGlassAccessibility() }

@Composable
fun rememberDerbisGlassAccessibility(): DerbisGlassAccessibility {
    val context = LocalContext.current
    val manager = remember(context) {
        context.applicationContext.getSystemService(AccessibilityManager::class.java)
    }
    // The public high-contrast text API arrived in Android 16 / API 36. Earlier Android
    // releases retain the normal material while Derbis's own accessibility settings can still
    // select opaque surfaces.
    var highTextContrast by remember(manager) {
        mutableStateOf(
            Build.VERSION.SDK_INT >= 36 && manager?.isHighContrastTextEnabled == true
        )
    }
    DisposableEffect(manager, context) {
        if (manager == null || Build.VERSION.SDK_INT < 36) return@DisposableEffect onDispose { }
        val listener = AccessibilityManager.HighContrastTextStateChangeListener { enabled ->
            highTextContrast = enabled
        }
        manager.addHighContrastTextStateChangeListener(context.mainExecutor, listener)
        onDispose { manager.removeHighContrastTextStateChangeListener(listener) }
    }
    return remember(highTextContrast) { DerbisGlassAccessibility(increasedContrast = highTextContrast) }
}

private val blurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

enum class GlassBackdropQuality(val resolutionScale: Float) {
    Native(1f),
    Balanced(0.75f),
    Efficient(0.5f)
}

internal fun resolveGlassBackdropQuality(blurRadiusDp: Float): GlassBackdropQuality = when {
    blurRadiusDp <= 20f -> GlassBackdropQuality.Native
    blurRadiusDp <= 26f -> GlassBackdropQuality.Balanced
    else -> GlassBackdropQuality.Efficient
}

internal fun glassBackdropLayerDimension(nativePixels: Float, resolutionScale: Float): Int =
    ceil(nativePixels.coerceAtLeast(0f) * resolutionScale.coerceIn(0.01f, 1f))
        .toInt()
        .coerceAtLeast(1)

/**
 * How much of the Liquid Glass lens the device can afford. The lens and its chromatic
 * dispersion re-sample the backdrop three extra times per pixel inside the rim band, so the
 * tier gates *cost*, never appearance: every tier still renders frosted glass.
 */
enum class GlassLensTier {
    /** Lens refraction, dispersion and specular rim. Flagship-class devices. */
    Full,

    /** Lens refraction only, at a coarser frosting scale. */
    Balanced,

    /** No lens at all; frost is capped at the cheapest scale and most panels stay translucent. */
    Efficient;

    /** Cheapest quality bucket this tier is allowed to sample at. */
    val ceilingQuality: GlassBackdropQuality
        get() = when (this) {
            Full -> GlassBackdropQuality.Native
            Balanced -> GlassBackdropQuality.Balanced
            Efficient -> GlassBackdropQuality.Efficient
        }
}

internal fun resolveGlassLensTier(
    heavyBlur: Boolean,
    lowRamDevice: Boolean,
    powerSaveMode: Boolean,
    effects: DerbisGlassEffects = DerbisGlassEffects.Automatic
): GlassLensTier = when (effects) {
    // The user asked for the fast, plain version: no lens, cheapest frost.
    DerbisGlassEffects.Off -> GlassLensTier.Efficient
    // "Always on" is an explicit opt-in, so it outranks the automatic downgrades; it still
    // respects the app's own visual-performance switch, which can disable glass entirely.
    DerbisGlassEffects.AlwaysOn -> if (heavyBlur) GlassLensTier.Full else GlassLensTier.Efficient
    DerbisGlassEffects.Automatic -> when {
        !heavyBlur || lowRamDevice || powerSaveMode -> GlassLensTier.Efficient
        else -> GlassLensTier.Full
    }
}

/** Picks whichever of the two buckets renders fewer pixels. */
internal fun coarsestGlassBackdropQuality(
    a: GlassBackdropQuality,
    b: GlassBackdropQuality
): GlassBackdropQuality = if (a.resolutionScale <= b.resolutionScale) a else b

@Composable
fun rememberGlassBlurAllowed(): Boolean {
    if (!blurSupported || !LocalDerbisVisualCapabilities.current.heavyBlur) return false
    val context = LocalContext.current.applicationContext
    val lowRam = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val powerSave = rememberPowerSaveMode()
    return !lowRam && !powerSave
}

@Immutable
data class DerbisGlassStyle(
    val blur: Int = DEFAULT_LEVEL,
    val transparency: Int = DEFAULT_LEVEL,
    val tint: Int = DEFAULT_LEVEL,
    val refraction: Boolean = true,
    /** User-facing Auto / Always on / Off budget for the optical lens. */
    val effects: DerbisGlassEffects = DerbisGlassEffects.Automatic
) {
    val blurScale: Float get() = 0.5f + blur.coerceIn(0, 100) / 100f
    val tintAlphaScale: Float get() = 1.45f - transparency.coerceIn(0, 100) / 100f * 0.9f
    val tintMix: Float get() = tint.coerceIn(0, 100) / 100f * 0.2f
    val saturation: Float get() = 1f + tint.coerceIn(0, 100) / 100f * 0.36f
    /** The lowest-transparency setting is a legibility choice, not merely a darker blur. */
    val prefersOpaqueMaterials: Boolean get() = transparency.coerceIn(0, 100) <= OPAQUE_TRANSPARENCY_THRESHOLD

    companion object {
        const val DEFAULT_LEVEL = 50
        private const val OPAQUE_TRANSPARENCY_THRESHOLD = 16
    }
}

val LocalDerbisGlassStyle = staticCompositionLocalOf { DerbisGlassStyle() }
