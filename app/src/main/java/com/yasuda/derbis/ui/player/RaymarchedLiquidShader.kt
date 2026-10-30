package com.yasuda.derbis.ui.player

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.yasuda.derbis.player.VisualizerAudioProcessor
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.LocalDerbisVisualCapabilities
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Audio-Reactive Raymarched Liquid Shader (notK50BML OuterTune pattern).
 *
 * Uses AGSL [RuntimeShader] (Android 13+, API 33) to raymarch a 3D Signed Distance Field (SDF)
 * of organic liquid metaballs / ferrofluid. The fluid's volume, displacement, surface tension,
 * and specular highlights react dynamically to audio frequencies extracted directly from
 * [VisualizerAudioProcessor.waveformState].
 *
 * For devices below API 33, an optimized procedural 2D harmonic liquid metaball canvas
 * fallback is used, ensuring 100% device compatibility.
 */
internal const val RAYMARCHED_LIQUID_AGSL = """
uniform float2 uResolution;
uniform float uTime;
uniform float uBass;
uniform float uMid;
uniform float uTreble;
uniform float uEnergy;
layout(color) uniform half4 uBase;
layout(color) uniform half4 uPrimary;
layout(color) uniform half4 uSecondary;
layout(color) uniform half4 uAccent;

// Smooth minimum for organic liquid metaball blending
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

// 3D Scene Distance Field (Liquid metaballs)
float mapScene(float3 p, float t, float bass, float mid, float treble) {
    // Primary core droplet pulsing with bass
    float3 c1 = p - float3(0.0, 0.12 + sin(t * 1.1) * 0.10, 2.0);
    float r1 = 0.54 + bass * 0.28;
    float d1 = length(c1) - r1;

    // Orbiting droplet 2 (driven by mid frequencies)
    float3 c2 = p - float3(
        cos(t * 0.85) * 0.50,
        sin(t * 1.05) * 0.38 - 0.08,
        2.1 + sin(t * 0.7) * 0.22
    );
    float r2 = 0.40 + mid * 0.20;
    float d2 = length(c2) - r2;

    // Orbiting droplet 3 (satellite droplet)
    float3 c3 = p - float3(
        -cos(t * 1.25) * 0.42,
        -sin(t * 0.75) * 0.32 + 0.18,
        1.9 + cos(t * 0.9) * 0.18
    );
    float r3 = 0.34 + treble * 0.16;
    float d3 = length(c3) - r3;

    // Small droplet 4 (high-frequency floater)
    float3 c4 = p - float3(
        sin(t * 1.5) * 0.30,
        cos(t * 1.3) * 0.42,
        1.75
    );
    float r4 = 0.26 + bass * 0.14;
    float d4 = length(c4) - r4;

    // Viscous fluid blend
    float blendK = 0.38 + bass * 0.18;
    float d = smin(d1, d2, blendK);
    d = smin(d, d3, 0.32);
    d = smin(d, d4, 0.26);

    // Surface acoustic micro-ripples from treble
    float ripple = sin(p.x * 10.0 + t * 4.0) * sin(p.y * 10.0 - t * 3.5) * 0.018 * treble;
    return d + ripple;
}

// Calculate normal via tetrahedron central differences
float3 calcNormal(float3 p, float t, float bass, float mid, float treble) {
    float2 e = float2(0.006, -0.006);
    return normalize(
        e.xyy * mapScene(p + e.xyy, t, bass, mid, treble) +
        e.yyx * mapScene(p + e.yyx, t, bass, mid, treble) +
        e.yxy * mapScene(p + e.yxy, t, bass, mid, treble) +
        e.xxx * mapScene(p + e.xxx, t, bass, mid, treble)
    );
}

half4 main(float2 fragCoord) {
    float minDim = max(min(uResolution.x, uResolution.y), 1.0);
    float2 uv = (fragCoord * 2.0 - uResolution) / minDim;
    float2 screenUv = fragCoord / uResolution;

    float t = uTime * 0.85;
    float bass = clamp(uBass, 0.0, 1.5);
    float mid = clamp(uMid, 0.0, 1.5);
    float treble = clamp(uTreble, 0.0, 1.5);

    // Camera setup
    float3 ro = float3(0.0, 0.0, 0.0);
    float3 rd = normalize(float3(uv.x, -uv.y, 1.35));

    // Raymarch loop (28 steps - fast and silky smooth on mobile GPU)
    float dist = 0.0;
    float hitDist = -1.0;
    for (int i = 0; i < 28; i++) {
        float3 p = ro + rd * dist;
        float d = mapScene(p, t, bass, mid, treble);
        if (d < 0.004) {
            hitDist = dist;
            break;
        }
        if (dist > 4.2) break;
        dist += d * 0.72;
    }

    // Elegant deep backdrop gradient
    half3 bg = mix(
        uBase.rgb * 0.45,
        mix(uPrimary.rgb * 0.12, uSecondary.rgb * 0.08, half(screenUv.y)),
        half(1.0 - screenUv.y * 0.6)
    );

    if (hitDist > 0.0) {
        float3 p = ro + rd * hitDist;
        float3 n = calcNormal(p, t, bass, mid, treble);
        float3 viewDir = -rd;

        // Key light
        float3 lightDir = normalize(float3(0.55, 0.75, -0.9));
        float diff = max(dot(n, lightDir), 0.0);

        // Blinn-Phong specular glint (pure glossy liquid reflections)
        float3 halfVector = normalize(lightDir + viewDir);
        float spec = pow(max(dot(n, halfVector), 0.0), 36.0);

        // Fresnel edge reflection (liquid mercury sheen)
        float fresnel = pow(clamp(1.0 - dot(n, viewDir), 0.0, 1.0), 2.8);

        // Color shading based on surface normal and depth
        half3 fluidColor = mix(
            uPrimary.rgb * 0.85,
            uSecondary.rgb * 0.95,
            half(clamp(n.y * 0.5 + 0.5, 0.0, 1.0))
        );

        // Add specular and vibrant chromatic rim glow
        half3 finalColor = fluidColor * half(0.35 + diff * 0.65)
                         + half3(1.0, 1.0, 1.0) * half(spec * (0.8 + bass * 0.4))
                         + uAccent.rgb * half(fresnel * (1.1 + treble * 0.6));

        // Soft fog fade at the silhouette
        float fog = clamp((hitDist - 1.2) / 2.6, 0.0, 1.0);
        half3 blended = mix(finalColor, bg, half(fog * 0.35));

        // Vignette edges so foreground content remains legible
        float vig = 1.0 - smoothstep(0.4, 1.2, length(uv));
        blended *= half(0.75 + 0.25 * vig);

        return half4(blended, 1.0);
    }

    // Vignette for background
    float vig = 1.0 - smoothstep(0.3, 1.1, length(uv));
    bg *= half(0.70 + 0.30 * vig);
    return half4(bg, 1.0);
}
"""

@Composable
internal fun RaymarchedLiquidBackdrop(
    primary: State<Color>,
    secondary: State<Color>,
    accent: State<Color>,
    base: State<Color>,
    isPlaying: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val decorative = LocalDerbisVisualCapabilities.current.decorativeMotion
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val animate = animationsEnabled && decorative && lifecycleState.isAtLeast(Lifecycle.State.STARTED)

    var time by remember { mutableFloatStateOf(0f) }
    var smoothedBass by remember { mutableFloatStateOf(0f) }
    var smoothedMid by remember { mutableFloatStateOf(0f) }
    var smoothedTreble by remember { mutableFloatStateOf(0f) }
    var smoothedEnergy by remember { mutableFloatStateOf(0f) }

    val presence by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.72f,
        animationSpec = DerbisPlayerDesign.motion(animationsEnabled && decorative, DerbisPlayerDesign.emphasizedTween(700)),
        label = "liquid-presence"
    )

    LaunchedEffect(animate, isPlaying) {
        if (!animate) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
                    time += dt * (if (isPlaying) 1f else 0.25f)

                    val waveform = if (isPlaying) VisualizerAudioProcessor.waveformState.value else FloatArray(0)
                    val signal = extractLiquidAudioSignal(waveform)

                    val attack = 0.35f
                    val decay = 0.12f
                    smoothedBass += (signal.bass - smoothedBass) * (if (signal.bass > smoothedBass) attack else decay)
                    smoothedMid += (signal.mid - smoothedMid) * (if (signal.mid > smoothedMid) 0.30f else 0.15f)
                    smoothedTreble += (signal.treble - smoothedTreble) * (if (signal.treble > smoothedTreble) 0.40f else 0.18f)
                    smoothedEnergy += (signal.energy - smoothedEnergy) * (if (signal.energy > smoothedEnergy) attack else decay)
                }
                lastNanos = now
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { createRaymarchedLiquidShader() }
        if (shader != null) {
            val brush = remember(shader) { ShaderBrush(shader) }
            Canvas(modifier = modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@Canvas

                shader.setFloatUniform("uResolution", w, h)
                shader.setFloatUniform("uTime", time)
                shader.setFloatUniform("uBass", smoothedBass * presence)
                shader.setFloatUniform("uMid", smoothedMid * presence)
                shader.setFloatUniform("uTreble", smoothedTreble * presence)
                shader.setFloatUniform("uEnergy", smoothedEnergy * presence)
                shader.setColorUniform("uBase", base.value.toArgb())
                shader.setColorUniform("uPrimary", primary.value.toArgb())
                shader.setColorUniform("uSecondary", secondary.value.toArgb())
                shader.setColorUniform("uAccent", accent.value.toArgb())

                drawRect(brush)
            }
            return
        }
    }

    // Procedural 2D harmonic liquid metaball fallback for API < 33
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawProceduralLiquidFallback(
                    primary = primary.value,
                    secondary = secondary.value,
                    accent = accent.value,
                    base = base.value,
                    time = time,
                    bass = smoothedBass * presence,
                    mid = smoothedMid * presence
                )
            }
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createRaymarchedLiquidShader(): RuntimeShader? = runCatching {
    RuntimeShader(RAYMARCHED_LIQUID_AGSL)
}.getOrNull()

internal data class LiquidAudioSignal(
    val bass: Float,
    val mid: Float,
    val treble: Float,
    val energy: Float
)

internal fun extractLiquidAudioSignal(waveform: FloatArray): LiquidAudioSignal {
    if (waveform.isEmpty()) return LiquidAudioSignal(0f, 0f, 0f, 0f)

    var bassSum = 0f
    var midSum = 0f
    var trebleSum = 0f

    val bassCount = minOf(12, waveform.size)
    for (i in 0 until bassCount) {
        bassSum += waveform[i]
    }
    val midEnd = minOf(36, waveform.size)
    for (i in bassCount until midEnd) {
        midSum += waveform[i]
    }
    for (i in midEnd until waveform.size) {
        trebleSum += waveform[i]
    }

    val bassAvg = if (bassCount > 0) (bassSum / bassCount) * 2.2f else 0f
    val midAvg = if (midEnd > bassCount) (midSum / (midEnd - bassCount)) * 1.8f else 0f
    val trebleAvg = if (waveform.size > midEnd) (trebleSum / (waveform.size - midEnd)) * 2.4f else 0f

    val energy = (bassAvg * 0.5f + midAvg * 0.3f + trebleAvg * 0.2f).coerceIn(0f, 2f)
    return LiquidAudioSignal(
        bass = bassAvg.coerceIn(0f, 2f),
        mid = midAvg.coerceIn(0f, 2f),
        treble = trebleAvg.coerceIn(0f, 2f),
        energy = energy
    )
}

/**
 * Procedural harmonic liquid metaball renderer for API < 33.
 */
private fun DrawScope.drawProceduralLiquidFallback(
    primary: Color,
    secondary: Color,
    accent: Color,
    base: Color,
    time: Float,
    bass: Float,
    mid: Float
) {
    drawRect(lerp(base, Color.Black, 0.45f))

    val angle = time * 0.6f
    val extent = maxOf(size.width, size.height)

    // Center primary core droplet
    val centerRadius = extent * (0.34f + bass * 0.12f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = 0.88f),
                primary.copy(alpha = 0.35f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * (0.42f + sin(angle) * 0.05f)),
            radius = centerRadius
        ),
        radius = centerRadius,
        center = Offset(size.width * 0.5f, size.height * (0.42f + sin(angle) * 0.05f))
    )

    // Orbiting secondary liquid blob
    val orbX = size.width * (0.5f + cos(angle * 0.9f) * 0.25f)
    val orbY = size.height * (0.45f + sin(angle * 1.1f) * 0.20f)
    val orbRadius = extent * (0.28f + mid * 0.10f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                secondary.copy(alpha = 0.82f),
                secondary.copy(alpha = 0.28f),
                Color.Transparent
            ),
            center = Offset(orbX, orbY),
            radius = orbRadius
        ),
        radius = orbRadius,
        center = Offset(orbX, orbY)
    )

    // Vibrant accent rim highlight
    val accentX = size.width * (0.5f - sin(angle * 1.3f) * 0.22f)
    val accentY = size.height * (0.40f - cos(angle * 0.8f) * 0.18f)
    val accentRadius = extent * 0.22f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = 0.70f),
                accent.copy(alpha = 0.18f),
                Color.Transparent
            ),
            center = Offset(accentX, accentY),
            radius = accentRadius
        ),
        radius = accentRadius,
        center = Offset(accentX, accentY)
    )

    // Legibility vignette
    drawRect(
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.15f),
            0.5f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.85f)
        )
    )
}
