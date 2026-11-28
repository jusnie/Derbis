/*
 * The Liquid Glass page background from the glass-skin mock.
 *
 * Four modes share one stack: a black canvas, a mode layer, and a dimming veil. Glow paints a
 * static radial field in the accent pair; Animated drifts four blobs through the frame (skipped
 * when motion is off); Blur paints the current artwork, scaled and heavily blurred, plus an
 * accent tint; Custom decodes the user-picked image from internal storage and does the same.
 * Every mode honours intensity (how much of the colour field shows), blur (Blur/Custom), brightness
 * (the veil) and the accent override (hue/lightness or "follow the artwork").
 */
package com.yasuda.derbis.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.platform.LocalContext
import com.yasuda.derbis.domain.DerbisBackgroundMode
import com.yasuda.derbis.domain.DerbisInterfaceSettings
import java.io.File
import kotlin.math.max

private data class GlassAccentPair(val primary: Color, val secondary: Color)

private fun hsv(hue: Float, saturation: Float, value: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(((hue % 360f) + 360f) % 360f, saturation, value)))

/** Accent pair for the background field: the override hue/lightness, or the artwork/theme accent. */
private fun resolveGlassAccents(settings: DerbisInterfaceSettings, fallbackAccent: Color): GlassAccentPair {
    val hue = settings.glassAccentHue
    if (hue >= 0) {
        val primary = hsv(hue.toFloat(), 0.72f, (settings.glassAccentLightness.coerceIn(0, 100) / 100f))
        val secondary = hsv(
            hue.toFloat() + 28f,
            0.65f,
            max(0.12f, settings.glassAccentLightness * 0.45f / 100f)
        )
        return GlassAccentPair(primary, secondary)
    }
    val hsvFallback = FloatArray(3)
    android.graphics.Color.colorToHSV(fallbackAccent.toArgb(), hsvFallback)
    val secondary = hsv(hsvFallback[0] + 32f, (hsvFallback[1] * 0.85f).coerceIn(0.25f, 1f), hsvFallback[2] * 0.5f)
    return GlassAccentPair(fallbackAccent, secondary)
}

private val GlassBgDeepTeal = Color(0xFF101820)

private const val MAX_BACKGROUND_BITMAP_PX = 1440

/**
 * The mock's four background modes. Always opaque black underneath; callers composite
 * translucent content above.
 */
@Composable
internal fun GlassBackgroundEngine(
    settings: DerbisInterfaceSettings,
    accent: Color,
    artworkUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val accents = resolveGlassAccents(settings, accent)
    val intensity = (settings.glassBackgroundIntensity.coerceIn(20, 100) / 100f)
    val veil = (100 - settings.glassBackgroundBrightness.coerceIn(15, 100)) / 100f * 0.85f

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        when (settings.glassBackgroundMode) {
            DerbisBackgroundMode.Glow -> GlowLayer(accents, intensity)
            DerbisBackgroundMode.Animated -> AnimatedLayer(
                accents = accents,
                intensity = intensity,
                motion = settings.glassBackgroundMotion
            )
            DerbisBackgroundMode.ArtworkBlur -> ArtworkBlurLayer(
                settings = settings,
                accents = accents,
                intensity = intensity,
                artworkUrl = artworkUrl
            )
            DerbisBackgroundMode.CustomImage -> CustomImageLayer(
                settings = settings,
                accents = accents,
                intensity = intensity
            )
        }
        if (veil > 0.01f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = veil)))
        }
    }
}

@Composable
private fun GlowLayer(accents: GlassAccentPair, intensity: Float) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        drawGlassRadial(accents.primary, center = Offset(size.width * 0.12f, size.height * 0.14f), radius = max(size.width, size.height) * 0.78f, alpha = 0.95f * intensity)
        drawGlassRadial(accents.secondary, center = Offset(size.width * 0.96f, size.height * 0.68f), radius = max(size.width, size.height) * 0.66f, alpha = 0.9f * intensity)
        drawGlassRadial(accents.primary, center = Offset(size.width * 0.42f, size.height * 1.02f), radius = max(size.width, size.height) * 0.55f, alpha = 0.75f * intensity)
    }
}

private fun DrawScope.drawGlassRadial(color: Color, center: Offset, radius: Float, alpha: Float) {
    if (radius <= 0f) return
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.28f), Color.Transparent),
            center = center,
            radius = radius
        )
    )
}

@Composable
private fun AnimatedLayer(accents: GlassAccentPair, intensity: Float, motion: Boolean) {
    if (!motion) {
        // Frozen frame: no transition, no per-frame redraw cost.
        BlobLayer(accents, intensity, t1 = 0f, t2 = 0f, t3 = 0f)
        return
    }
    val drift = rememberInfiniteTransition(label = "glass-background-drift")
    val t1 by drift.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift-1"
    )
    val t2 by drift.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(14000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift-2"
    )
    val t3 by drift.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift-3"
    )
    BlobLayer(accents, intensity, t1, t2, t3)
}

@Composable
private fun BlobLayer(accents: GlassAccentPair, intensity: Float, t1: Float, t2: Float, t3: Float) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val span = max(w, h)
        // Four blobs drifting through the frame, matching the mock's keyframes.
        drawGlassRadial(accents.primary, center = Offset(w * (0.08f + 0.42f * t1), h * (0.10f + 0.42f * t1 * t1)), radius = span * 0.72f, alpha = 0.92f * intensity)
        drawGlassRadial(accents.secondary, center = Offset(w * (0.92f - 0.40f * t2), h * (0.88f - 0.36f * t2)), radius = span * 0.68f, alpha = 0.9f * intensity)
        drawGlassRadial(accents.primary, center = Offset(w * (0.18f + 0.36f * t3), h * (0.46f + 0.18f * t3)), radius = span * 0.52f, alpha = 0.72f * intensity)
        drawGlassRadial(accents.secondary, center = Offset(w * (0.78f - 0.28f * t1), h * (0.14f + 0.30f * t2)), radius = span * 0.46f, alpha = 0.6f * intensity)
    }
}

@Composable
private fun ArtworkBlurLayer(
    settings: DerbisInterfaceSettings,
    accents: GlassAccentPair,
    intensity: Float,
    artworkUrl: String?
) {
    val blurDp = settings.glassBackgroundBlur.dpClamp()
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().clipToBounds()) {
        if (!artworkUrl.isNullOrBlank()) {
            val request = androidx.compose.runtime.remember(artworkUrl) {
                ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurDp)
                    .alpha(0.35f + 0.55f * intensity)
            )
        } else {
            // No artwork playing: paint the procedural gradient field the mock draws on canvas.
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(accents.primary, GlassBgDeepTeal, accents.secondary),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    alpha = 0.9f * intensity
                )
            }
        }
        // Accent tint to keep the palette coherent with the rest of the design.
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawGlassRadial(accents.primary, center = Offset(size.width * 0.2f, size.height * 0.12f), radius = max(size.width, size.height) * 0.7f, alpha = 0.28f * intensity)
        }
    }
}

@Composable
private fun CustomImageLayer(
    settings: DerbisInterfaceSettings,
    accents: GlassAccentPair,
    intensity: Float
) {
    val blurDp = settings.glassBackgroundBlur.dpClamp()
    val path = settings.glassBackgroundImage
    val image by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            if (path.isBlank() || !File(path).isFile) return@withContext null
            runCatching {
                // Decode at a capped resolution: a 12MP gallery shot is ~48MB of heap otherwise.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (bounds.outWidth / sample > MAX_BACKGROUND_BITMAP_PX ||
                    bounds.outHeight / sample > MAX_BACKGROUND_BITMAP_PX
                ) {
                    sample *= 2
                }
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(Modifier.fillMaxSize().clipToBounds()) {
        val bitmap = image
        if (bitmap != null) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurDp)
                    .alpha(0.45f + 0.45f * intensity)
            ) {
                // Cover-crop: scale so the bitmap fills the frame, then center.
                val srcW = bitmap.width.toFloat().coerceAtLeast(1f)
                val srcH = bitmap.height.toFloat().coerceAtLeast(1f)
                val scale = max(size.width / srcW, size.height / srcH)
                val dstW = (srcW * scale).toInt().coerceAtLeast(1)
                val dstH = (srcH * scale).toInt().coerceAtLeast(1)
                drawImage(
                    image = bitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(
                        (size.width - dstW).toInt() / 2,
                        (size.height - dstH).toInt() / 2
                    ),
                    dstSize = androidx.compose.ui.unit.IntSize(dstW, dstH)
                )
            }
        } else {
            GlowLayer(accents, intensity)
        }
    }
}

private fun Int.dpClamp() =
    androidx.compose.ui.unit.Dp(this.toFloat().coerceIn(0f, 80f))
