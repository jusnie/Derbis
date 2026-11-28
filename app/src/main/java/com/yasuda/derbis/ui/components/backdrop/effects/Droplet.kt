/*
 * The press-and-hold lyrics droplet: an optical magnifier of the page standing
 * off the artwork, ported from the Apple Music 13 reference player's WebGL
 * water lens (the blurred-lyric orb that wakes under the finger, magnifies the
 * scene and wobbles its own outline).
 *
 * This is intentionally NOT the rounded-rect [lens] used by the glass panels:
 * the droplet samples the recorded page magnificationally about its own centre,
 * discards everything outside its wobbling boundary and lights its rim. Written
 * for Derbis against the vendored Kyant0/backdrop engine (v2.0.0, Apache-2.0);
 * the AGSL itself is original to this project.
 */
package com.yasuda.derbis.ui.components.backdrop.effects

import androidx.annotation.FloatRange
import com.yasuda.derbis.ui.components.backdrop.BackdropEffectScope
import com.yasuda.derbis.ui.components.backdrop.internal.RuntimeShaderEffect
import com.yasuda.derbis.ui.components.backdrop.isRuntimeShaderSupported
import org.intellij.lang.annotations.Language

/**
 * Magnifying droplet refraction with a wobbling boundary.
 *
 * [time] drives the outline wobble and rim shimmer — pass a frame clock in
 * seconds. [magnification] is the centre magnification of the bead (1.14 is the
 * reference value). [wobble] scales the outline wobble amplitude; 1f is the
 * reference's idle wobble, raise it while the finger moves. [chromaticAberration]
 * splits the rim into a chromatic fringe — the reference dispersed the edge in
 * the full chrome tier only.
 *
 * The effect outputs fully transparent pixels outside the droplet boundary, so
 * whatever clips the surface (the lens is an ellipse) never becomes visible.
 */
fun BackdropEffectScope.droplet(
    time: Float,
    magnification: Float = 1.14f,
    @FloatRange(from = 0.0) wobble: Float = 1f,
    chromaticAberration: Boolean = false
) {
    if (!isRuntimeShaderSupported()) return
    if (magnification <= 0f || wobble < 0f) return
    // A surface that has not been laid out yet is 0 x 0 on its first frame; skip
    // until there are real pixels to refract (same rule the rounded-rect lens uses).
    val surfaceSize = size
    if (surfaceSize.width < 2f || surfaceSize.height < 2f) return

    val shader = obtainRuntimeShader(
        "DropletRefraction",
        DropletRefractionShaderString
    )
    shader.apply {
        setFloatUniform("size", surfaceSize.width, surfaceSize.height)
        setFloatUniform("offset", -padding, -padding)
        setFloatUniform("time", time)
        setFloatUniform("magnification", magnification)
        setFloatUniform("wobble", wobble)
        setFloatUniform("chromaticAberration", if (chromaticAberration) 1f else 0f)
    }
    effect(RuntimeShaderEffect(shader, "content"))
}

@Language("AGSL")
private const val DropletRefractionShaderString = """
uniform shader content;

uniform float2 size;
uniform float2 offset;
uniform float time;
uniform float magnification;
uniform float wobble;
uniform float chromaticAberration;

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    if (halfSize.x < 1.0 || halfSize.y < 1.0) {
        return half4(0.0);
    }
    float2 center = halfSize;
    float2 p = coord + offset;
    // Unit-ellipse space: the droplet is an axis-aligned ellipse, wobbling in
    // angle around its perimeter like the reference's sinusoidal outline.
    float2 q = (p - center) / halfSize;
    float ang = atan(q.y, q.x);
    float r = 1.0 + wobble * (0.045 * sin(3.0 * ang + time * 1.1)
            + 0.035 * sin(2.0 * ang - time * 0.8 + 1.3)
            + 0.02 * sin(5.0 * ang + time * 1.7));
    float len = length(q);
    float sd = len - r;
    float px = 1.0 / min(halfSize.x, halfSize.y);
    float inside = 1.0 - smoothstep(-1.5 * px, 1.5 * px, sd);
    if (inside <= 0.001) {
        return half4(0.0);
    }

    float2 n = q / max(len, 1e-4);
    float edge = smoothstep(0.55, 1.0, clamp(len / r, 0.0, 1.0));

    // Slow ripple on the refracting sample, like the reference's sin/cos field.
    float2 rip = float2(sin(p.y * 0.05 + time * 2.1) + sin(p.x * 0.031 - time * 1.3),
                        cos(p.x * 0.045 - time * 1.8) + cos(p.y * 0.027 + time * 1.1)) * 1.6;

    // Centre magnification with a pull-in toward the bead edge and the rim ripple.
    float2 base = center + (p - center) / magnification - n * edge * edge * 26.0 + rip;
    float2 ca = n * edge * edge * 3.0 * chromaticAberration;

    half3 col;
    col.r = content.eval(base + ca).r;
    col.g = content.eval(base).g;
    col.b = content.eval(base - ca).b;
    col = col * 1.04 + half3(0.015);

    // Rim light: a soft edge band lit from the top-left, warmed from the bottom-right.
    float rim = 1.0 - smoothstep(0.0, 6.0 * px, -sd);
    float lt = pow(max(dot(n, normalize(float2(-0.55, -0.85))), 0.0), 2.0);
    float lb = pow(max(dot(n, normalize(float2(0.6, 0.8))), 0.0), 3.0);
    col += rim * (0.12 + 0.55 * lt + 0.25 * lb);
    float inner = 1.0 - smoothstep(0.0, 22.0 * px, -sd);
    col += inner * (0.06 * lt - 0.03 * lb);

    // Specular bead near the upper-left interior, the water-droplet catchlight.
    float2 sq = q + float2(0.32, 0.66);
    col += exp(-(sq.x * sq.x / 0.05 + sq.y * sq.y / 0.008)) * 0.38;

    col = clamp(col, half3(0.0), half3(1.0));
    return half4(col * inside, inside);
}
"""
