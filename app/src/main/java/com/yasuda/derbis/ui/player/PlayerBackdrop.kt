package com.yasuda.derbis.ui.player

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yasuda.derbis.data.DerbisArtworkCache
import com.yasuda.derbis.ui.PlayerAmbience
import com.yasuda.derbis.ui.createPlayerAmbientColorMatrix
import com.yasuda.derbis.ui.createPlayerTintedMistMatrix
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import com.yasuda.derbis.ui.theme.LocalDerbisVisualCapabilities
import kotlin.math.cos
import kotlin.math.sin

private const val MistDecodePx = 96
private const val LegacyMistDecodePx = 512
private const val HardwareBlurMinSdk = 31

internal fun mistDecodePxForApi(sdkInt: Int): Int =
    if (sdkInt >= HardwareBlurMinSdk) MistDecodePx else LegacyMistDecodePx
private const val MistScale = 1.32f
private const val MistAlphaPlaying = 0.64f
private const val MistAlphaPaused = 0.48f
private const val BackdropColorMillis = 900
private const val ArtworkDriftMillis = 46_000
private const val TwoPi = 6.2831855f
private const val MistBreath = 0.05f
private const val MistSwayX = 0.035f
private const val MistSwayY = 0.03f
private const val MistFadeInMillis = 480
private const val MistFadeOutMillis = 320

@Composable
internal fun PlayerBackdrop(
    artworkUrl: String,
    ambience: PlayerAmbience,
    isPlaying: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
    fixedColors: Boolean = false,
    focusY: Float = 0.34f
) {
    if (LocalAppleMusicPlayerPalette.current != null) {
        Box(modifier.background(Color.Black))
        return
    }
    if (com.yasuda.derbis.ui.theme.DerbisIsPrism) {
        com.yasuda.derbis.ui.theme.PrismPlayerAtmosphere(modifier)
        return
    }
    if (com.yasuda.derbis.ui.theme.DerbisIsNocturne) {
        Box(modifier.background(Color.Black))
        return
    }
    val colorSpec = DerbisPlayerDesign.motion(
        animationsEnabled,
        tween<Color>(BackdropColorMillis, easing = DerbisPlayerDesign.Decelerate)
    )
    val primary = animateColorAsState(ambience.primary, colorSpec, label = "player-backdrop-primary")
    val secondary = animateColorAsState(ambience.secondary, colorSpec, label = "player-backdrop-secondary")
    val glowStrength = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.7f,
        animationSpec = DerbisPlayerDesign.motion(animationsEnabled, DerbisPlayerDesign.emphasizedTween(600)),
        label = "player-backdrop-glow"
    )
    val base = animateColorAsState(ambience.base, colorSpec, label = "player-backdrop-base")
    val ambientColorFilter = if (fixedColors) {
        val tint = primary.value
        remember(tint) { ColorFilter.colorMatrix(createPlayerTintedMistMatrix(tint)) }
    } else {
        remember { ColorFilter.colorMatrix(createPlayerAmbientColorMatrix()) }
    }
    // The deck has one background now: the cover, blurred past recognition, with two of its own
    // colours drifting over it. Nothing here is selectable, so it cannot be set to something
    // duller than the record. The drift only runs while playing — a paused deck should be a still.
    val drifting = isPlaying && animationsEnabled && LocalDerbisVisualCapabilities.current.decorativeMotion
    val drift = if (drifting) {
        rememberInfiniteTransition(label = "player-backdrop-drift").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(ArtworkDriftMillis, easing = LinearEasing),
                RepeatMode.Restart
            ),
            label = "player-backdrop-drift-phase"
        )
    } else {
        null
    }

    Box(modifier = modifier.drawBehind { drawRect(base.value) }) {
        PlayerArtworkMist(
            artworkUrl = artworkUrl,
            colorFilter = ambientColorFilter,
            isPlaying = isPlaying,
            animationsEnabled = animationsEnabled,
            phase = drift
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawArtworkWash(primary, secondary, base, glowStrength.value, focusY, phase) }
        )
    }
}

@Composable
private fun PlayerArtworkMist(
    artworkUrl: String,
    colorFilter: ColorFilter?,
    isPlaying: Boolean,
    animationsEnabled: Boolean,
    phase: State<Float>?
) {
    if (artworkUrl.isBlank()) return
    val context = LocalContext.current
    val mistDecodePx = mistDecodePxForApi(Build.VERSION.SDK_INT)
    val mistAlpha by animateFloatAsState(
        targetValue = if (isPlaying) MistAlphaPlaying else MistAlphaPaused,
        animationSpec = DerbisPlayerDesign.motion(animationsEnabled, DerbisPlayerDesign.emphasizedTween(600)),
        label = "player-backdrop-mist-alpha"
    )
    AnimatedContent(
        targetState = artworkUrl,
        transitionSpec = {
            if (animationsEnabled) {
                fadeIn(tween(MistFadeInMillis, easing = DerbisPlayerDesign.Decelerate)) togetherWith
                    fadeOut(tween(MistFadeOutMillis, easing = DerbisPlayerDesign.Standard))
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "player-backdrop-mist"
    ) { url ->
        val request = remember(context, url, mistDecodePx) {
            ImageRequest.Builder(context)
                .data(DerbisArtworkCache.large(url))
                .size(mistDecodePx, mistDecodePx)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            filterQuality = FilterQuality.High,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // A slow sway and breath, so the blur underneath the colours is alive too.
                    // Anchored at sin(0) = 0: with motion off the mist sits exactly where it did.
                    // The state is read here, in the layer, so the drift never recomposes.
                    val drift = phase?.value ?: 0f
                    val swing = sin(drift * TwoPi)
                    scaleX = MistScale + MistBreath * swing
                    scaleY = MistScale + MistBreath * swing
                    translationX = size.width * MistSwayX * swing
                    translationY = size.height * MistSwayY * sin(drift * TwoPi * 2f)
                    alpha = mistAlpha
                }
                .blur(48.dp)
        )
    }
}

/**
 * Lays the cover's two colours over the blurred cover as soft, slowly travelling orbs.
 *
 * The anchors orbit rather than pulse, because a pulse in place reads as a stutter at 40 seconds
 * and a drift reads as the record breathing. [phase] is 0..1 per cycle; every offset is written so
 * that phase 0 is the resting composition.
 */
/**
 * The cover's colours, lifted just enough to read as light on a blurred cover instead of grey.
 * Hue is never touched — only saturation and value — so the wash still belongs to the artwork.
 */
private fun driftColor(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = (hsv[1] * 1.25f).coerceIn(0.35f, 0.85f)
    hsv[2] = hsv[2].coerceIn(0.42f, 0.76f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun DrawScope.drawArtworkWash(
    primary: State<Color>,
    secondary: State<Color>,
    base: State<Color>,
    strength: Float,
    focusY: Float,
    phase: State<Float>?
) {
    val lead = driftColor(primary.value)
    val trail = driftColor(secondary.value)
    val angle = (phase?.value ?: 0f) * TwoPi
    val breath = 0.5f + 0.5f * sin(angle * 2f)

    val leadCenter = Offset(
        x = size.width * (0.5f + 0.26f * cos(angle)),
        y = size.height * (focusY + 0.05f * sin(angle))
    )
    val leadReach = size.maxDimension * (0.60f + 0.07f * breath)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(lead.copy(alpha = (0.22f + 0.08f * breath) * strength), Color.Transparent),
            center = leadCenter,
            radius = leadReach
        ),
        radius = leadReach,
        center = leadCenter
    )

    val trailCenter = Offset(
        x = size.width * (0.5f - 0.30f * cos(angle + 1.9f)),
        y = size.height * (0.74f + 0.09f * sin(angle + 1.9f))
    )
    val trailReach = size.maxDimension * (0.52f + 0.06f * (1f - breath))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(trail.copy(alpha = (0.18f + 0.06f * (1f - breath)) * strength), Color.Transparent),
            center = trailCenter,
            radius = trailReach
        ),
        radius = trailReach,
        center = trailCenter
    )

    // Legibility: the top holds a tint of the cover so titles stay readable, the bottom sinks into
    // black for the controls.
    drawRect(
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to base.value.copy(alpha = 0.70f),
                0.14f to Color.Transparent,
                0.50f to Color.Transparent,
                0.72f to Color.Black.copy(alpha = 0.40f),
                1.00f to Color.Black.copy(alpha = 0.86f)
            )
        )
    )
}
