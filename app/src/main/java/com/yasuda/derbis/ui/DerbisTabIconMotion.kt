package com.yasuda.derbis.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.yasuda.derbis.domain.DerbisDockIcons
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Per-icon keyframe motion ported from the "Tab bar preview" HTML mock.
 *
 * The mock ships one hand-tuned animation per glyph (`a-heart`, `a-star`, ...), fired when a tab
 * becomes active, replayed when the active tab is tapped again, and played when an icon is picked
 * in the editor. Every channel here is a pure function of a 0..1 progress value, interpolated
 * piecewise between the mock's keyframes; the per-icon easing and duration are applied to the
 * progress itself. Transforms are translations in dp plus uniform scale/rotation, matching the
 * mock's `transform` keyframes.
 */
internal data class TabIconTransform(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationDegrees: Float = 0f,
    val translationXDp: Float = 0f,
    val translationYDp: Float = 0f
) {
    companion object {
        val Identity = TabIconTransform()
    }
}

private data class IconStop(val p: Float, val t: TabIconTransform)
private data class IconMotion(val durationMillis: Int, val easing: Easing, val stops: List<IconStop>)

private fun stop(
    p: Float,
    scale: Float? = null,
    scaleX: Float? = null,
    scaleY: Float? = null,
    rotate: Float = 0f,
    tx: Float = 0f,
    ty: Float = 0f
): IconStop {
    val sx = scaleX ?: scale ?: 1f
    val sy = scaleY ?: scale ?: 1f
    return IconStop(p, TabIconTransform(sx, sy, rotate, tx, ty))
}

private val EASE = FastOutSlowInEasing
private val EASE_POP = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)   /* a-pop */
private val EASE_STAR = CubicBezierEasing(0.3f, 1.2f, 0.5f, 1f)  /* a-star */
private val EASE_PLUS = CubicBezierEasing(0.3f, 1.3f, 0.5f, 1f)  /* a-plus */
private val EASE_CLOCK = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)   /* a-clock */

private val ICON_MOTIONS: Map<String, IconMotion> = mapOf(
    "pop" to IconMotion(500, EASE_POP, listOf(
        stop(0f), stop(0.30f, scale = 0.7f), stop(0.65f, scale = 1.22f), stop(1f)
    )),
    "heart" to IconMotion(650, EASE, listOf(
        stop(0f), stop(0.15f, scale = 1.35f), stop(0.30f, scale = 0.95f),
        stop(0.45f, scale = 1.25f), stop(0.70f, scale = 1f), stop(1f)
    )),
    "star" to IconMotion(600, EASE_STAR, listOf(
        stop(0f), stop(0.50f, scale = 1.3f, rotate = 180f), stop(1f, rotate = 360f)
    )),
    "clock" to IconMotion(700, EASE_CLOCK, listOf(
        stop(0f), stop(1f, rotate = 360f)
    )),
    "bolt" to IconMotion(550, EASE, listOf(
        stop(0f), stop(0.20f, scale = 1.3f, rotate = -12f), stop(0.40f, scale = 0.9f, rotate = 10f),
        stop(0.60f, scale = 1.2f, rotate = -6f), stop(1f)
    )),
    "download" to IconMotion(550, EASE, listOf(
        stop(0f), stop(0.30f, ty = 6f), stop(0.55f, ty = -3f), stop(1f)
    )),
    "search" to IconMotion(600, EASE, listOf(
        stop(0f), stop(0.25f, scale = 1.1f, rotate = -22f), stop(0.55f, scale = 1.1f, rotate = 14f),
        stop(0.80f, rotate = -6f), stop(1f)
    )),
    "home" to IconMotion(550, EASE, listOf(
        stop(0f), stop(0.30f, scale = 1.08f, ty = -6f),
        stop(0.55f, scaleX = 0.92f, scaleY = 1.06f), stop(1f)
    )),
    "note" to IconMotion(600, EASE, listOf(
        stop(0f), stop(0.25f, scale = 1.1f, rotate = -18f),
        stop(0.60f, scale = 1.1f, rotate = 14f), stop(1f)
    )),
    "plus" to IconMotion(550, EASE_PLUS, listOf(
        stop(0f), stop(0.50f, scale = 1.3f, rotate = 50f), stop(1f, scale = 1f, rotate = 90f)
    )),
    "grid" to IconMotion(550, EASE, listOf(
        stop(0f), stop(0.35f, scale = 0.78f, rotate = -8f),
        stop(0.70f, scale = 1.15f, rotate = 6f), stop(1f)
    )),
    "list" to IconMotion(500, EASE, listOf(
        stop(0f), stop(0.30f, tx = 5f), stop(0.60f, tx = -3f), stop(1f)
    )),
    "playlist" to IconMotion(500, EASE, listOf(
        stop(0f), stop(0.30f, tx = 5f), stop(0.60f, tx = -3f), stop(1f)
    ))
)

/** The mock's per-glyph `i-*` animation id for an icon-picker id. Unknown glyphs pop. */
internal fun tabIconMotionId(iconId: String?): String = when (iconId) {
    DerbisDockIcons.Home -> "home"
    DerbisDockIcons.List -> "list"
    DerbisDockIcons.Search -> "search"
    DerbisDockIcons.Playlist -> "playlist"
    DerbisDockIcons.Plus -> "plus"
    DerbisDockIcons.Heart -> "heart"
    DerbisDockIcons.Note -> "note"
    DerbisDockIcons.Star -> "star"
    DerbisDockIcons.Download -> "download"
    DerbisDockIcons.Clock -> "clock"
    DerbisDockIcons.Bolt -> "bolt"
    DerbisDockIcons.Grid -> "grid"
    else -> "pop"
}

private fun motionFor(iconId: String?): IconMotion = ICON_MOTIONS.getValue(tabIconMotionId(iconId))

/** Piecewise evaluation of the mock's keyframes at [progress] in [0, 1]. */
internal fun tabIconTransformAt(iconId: String?, progress: Float): TabIconTransform {
    val stops = motionFor(iconId).stops
    val p = progress.coerceIn(0f, 1f)
    if (p <= stops.first().p) return stops.first().t
    if (p >= stops.last().p) return stops.last().t
    for (index in 1 until stops.size) {
        val upper = stops[index]
        if (p <= upper.p) {
            val lower = stops[index - 1]
            val span = upper.p - lower.p
            val local = if (span <= 0f) 1f else (p - lower.p) / span
            return TabIconTransform(
                scaleX = lower.t.scaleX + (upper.t.scaleX - lower.t.scaleX) * local,
                scaleY = lower.t.scaleY + (upper.t.scaleY - lower.t.scaleY) * local,
                rotationDegrees = lower.t.rotationDegrees + (upper.t.rotationDegrees - lower.t.rotationDegrees) * local,
                translationXDp = lower.t.translationXDp + (upper.t.translationXDp - lower.t.translationXDp) * local,
                translationYDp = lower.t.translationYDp + (upper.t.translationYDp - lower.t.translationYDp) * local
            )
        }
    }
    return TabIconTransform.Identity
}

/**
 * One-shot keyframe player for a dock glyph. Returns a provider for the live [TabIconTransform]
 * plus `fire()`, which (re)plays the animation — the caller decides when to fire (selection
 * change, same-tap replay, icon-picker selection, toggle-on), so every path stays in sync.
 * Reduced-motion collapses to identity.
 */
@Composable
internal fun rememberTabIconMotion(
    iconId: String?
): Pair<() -> TabIconTransform, () -> Unit> {
    val animationsEnabled = LocalAnimationsEnabled.current
    val spec = remember(iconId) { motionFor(iconId) }
    val progress = remember { Animatable(1f) }
    var pending by remember { mutableIntStateOf(0) }
    LaunchedEffect(pending) {
        if (pending > 0 && animationsEnabled) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(spec.durationMillis, easing = spec.easing))
        }
    }
    return { tabIconTransformAt(iconId, progress.value) } to { if (animationsEnabled) pending += 1 }
}

/**
 * Fires [fire] when [active] flips false→true, skipping the first composition. The mock does not
 * play the glyph animation on first paint — only when selection actually changes.
 */
@Composable
internal fun TabIconFireOnBecomeActive(active: Boolean, fire: () -> Unit) {
    val seeded = remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(active) {
        if (!seeded.value) {
            seeded.value = true
        } else if (active) {
            fire()
        }
    }
}

/**
 * Pop-in for a glyph that just entered the bar (the mock's `.pop` on newly inserted buttons:
 * 0% { transform: scale(.6); opacity: 0 }). Returns a provider for (scale, alpha).
 */
@Composable
internal fun rememberTabIconAppear(): () -> Pair<Float, Float> {
    val animationsEnabled = LocalAnimationsEnabled.current
    val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animationsEnabled) {
            progress.animateTo(1f, tween(350, easing = CubicBezierEasing(0.3f, 1f, 0.5f, 1f)))
        }
    }
    return { (0.6f + 0.4f * progress.value) to progress.value }
}
