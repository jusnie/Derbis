package com.yasuda.derbis.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.LyricLine
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import kotlin.math.hypot
import kotlin.math.pow
import android.os.Build
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.graphicsLayer
import com.yasuda.derbis.ui.GlassLensTier
import com.yasuda.derbis.ui.LocalDerbisGlassAccessibility
import com.yasuda.derbis.ui.LocalDerbisGlassTier
import com.yasuda.derbis.ui.components.LocalDerbisPlayerBackdrop
import com.yasuda.derbis.ui.components.backdrop.backdrops.LayerBackdrop
import com.yasuda.derbis.ui.components.backdrop.drawBackdrop
import com.yasuda.derbis.ui.components.backdrop.effects.droplet

private val LensWidth = 236.dp
private val LensHeight = 272.dp
private val LensShape = RoundedCornerShape(percent = 50)
private const val HoldRevealDelayMs = 150L

/**
 * The press-and-hold lyrics lens for the full-bleed cover: hold a finger (roughly) still on
 * the artwork and a tall bead of chrome glass wakes underneath it, with the current lyric
 * window rendered large, frosted and out of focus — a hint of the song, not a reading surface.
 * Drag while holding to slide the same blurred window around the stage; release to put the
 * droplet back to sleep.
 *
 * [lines] is the full lyrics document for the current track; [positionProvider] reads the
 * playback clock (word-timed when available). An instrument breaks or an empty document shows
 * a single soft "Instrumental" bead instead.
 */
@Composable
internal fun ArtworkLyricLensOverlay(
    center: Offset?,
    lines: List<LyricLine>,
    positionProvider: (() -> Long)?,
    fallbackPositionMs: Long,
    accent: Color,
    modifier: Modifier = Modifier
) {
    // The Liquid Glass skin records the deck's own artwork backdrop into an optical
    // layer; when that recording is live, the droplet refracts the very scene it
    // covers — magnification, wobbling water boundary, chromatic rim, spring squash.
    // Everywhere else the chrome bead keeps doing its job over whatever is behind it.
    val backdrop = LocalDerbisPlayerBackdrop.current
    val tier = LocalDerbisGlassTier.current
    val refractionReady = backdrop != null &&
        tier != GlassLensTier.Efficient &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !LocalDerbisGlassAccessibility.current.increasedContrast
    if (refractionReady) {
        RefractionDropletLens(
            backdrop = backdrop!!,
            center = center,
            lines = lines,
            positionProvider = positionProvider,
            fallbackPositionMs = fallbackPositionMs,
            accent = accent,
            modifier = modifier
        )
    } else {
        LegacyArtworkLyricLensOverlay(
            center = center,
            lines = lines,
            positionProvider = positionProvider,
            fallbackPositionMs = fallbackPositionMs,
            accent = accent,
            modifier = modifier
        )
    }
}

/** The original chrome-bead lens, kept for surfaces without a recorded backdrop and
 *  for the accessibility tiers that ask for no live refraction. */
@Composable
private fun LegacyArtworkLyricLensOverlay(
    center: Offset?,
    lines: List<LyricLine>,
    positionProvider: (() -> Long)?,
    fallbackPositionMs: Long,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (center != null) 1f else 0f,
        animationSpec = tween(durationMillis = 170),
        label = "lyric-lens-alpha"
    )
    // Keep the last finger position so the droplet fades out where it was released,
    // not at the stage midpoint.
    var lastCenter by remember { mutableStateOf(Offset(120f, 140f)) }
    LaunchedEffect(center) {
        if (center != null) lastCenter = center
    }
    if (alpha <= 0.012f) {
        // Nothing to draw yet, but keep the overlay inert and cheap.
        return
    }

    // Follow playback at reading speed: the text is blurred anyway, so 5 Hz is invisible.
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(180)
            tick += 1
        }
    }
    @Suppress("UNUSED_EXPRESSION") tick
    val positionMs = positionProvider?.invoke() ?: fallbackPositionMs

    val activeLines = remember(lines) {
        lines.filter { !it.isMetadata && !it.isInstrumental && it.text.isNotBlank() }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val lensW = with(density) { LensWidth.toPx() }
        val lensH = with(density) { LensHeight.toPx() }
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val desired = center ?: lastCenter
        val cx = desired.x.coerceIn(lensW / 2f + 4f, (maxW - lensW / 2f - 4f).coerceAtLeast(lensW / 2f + 4f))
        val cy = desired.y.coerceIn(lensH / 2f + 4f, (maxH - lensH / 2f - 4f).coerceAtLeast(lensH / 2f + 4f))

        val windowLines = rememberLyricWindow(activeLines, positionMs)

        Box(
            modifier = Modifier
                .offset { IntOffset((cx - lensW / 2f).toInt(), (cy - lensH / 2f).toInt()) }
                .size(LensWidth, LensHeight)
                .alpha(alpha)
                .shadow(elevation = 26.dp, shape = LensShape, clip = false)
                .clip(LensShape)
        ) {
            // Chrome film: a dense top light falling off to a cool edge, with the accent
            // warming the bottom of the bead.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.38f),
                            0.30f to Color.White.copy(alpha = 0.16f),
                            0.62f to Color.White.copy(alpha = 0.08f),
                            1f to accent.copy(alpha = 0.26f)
                        )
                    )
            )

            // The frosted words under the glass — large, soft and barely legible.
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .blur(7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                windowLines.forEach { line ->
                    val isCurrent = line.startMs <= positionMs && positionMs < line.endMs
                    Text(
                        text = line.text,
                        color = Color.White.copy(alpha = if (isCurrent) 0.95f else 0.68f),
                        fontSize = 20.sp,
                        lineHeight = 23.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(9.dp))
                }
            }

            // Bright rim with a cool inner reflection so the bead reads as standing off the cover.
            Box(
                Modifier
                    .matchParentSize()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.62f),
                            0.38f to Color.White.copy(alpha = 0.08f),
                            0.76f to accent.copy(alpha = 0.38f),
                            1f to Color.White.copy(alpha = 0.30f)
                        ),
                        shape = LensShape
                    )
            )
            // Top-left sheen, the specular kick of a water droplet.
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val sheen = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width * 0.34f, size.height * 0.22f),
                            radius = size.width * 0.62f
                        )
                        drawRect(brush = sheen)
                    }
            )
        }
    }
}

/**
 * Spring state for the droplet, driven by finger motion: [speedState] low-passes the
 * drag velocity, the squash spring kicks on every fast movement and rings back to
 * neutral, and the axis-aligned scales stretch the bead along its travel direction —
 * the same squash-and-stretch the reference's physics loop applies to its lens.
 */
private class DropletPhysics {
    val speedState = androidx.compose.runtime.mutableFloatStateOf(0f)
    val scaleXState = androidx.compose.runtime.mutableFloatStateOf(1f)
    val scaleYState = androidx.compose.runtime.mutableFloatStateOf(1f)
    val wobbleState = androidx.compose.runtime.mutableFloatStateOf(0.8f)

    private var dirX = 0f
    private var dirY = 1f
    private var squash = 0f
    private var squashVel = 0f
    private var lastX = Float.NaN
    private var lastY = 0f
    private var lastNanos = 0L

    fun onPosition(x: Float, y: Float, nanos: Long) {
        if (lastX.isNaN()) {
            lastX = x; lastY = y; lastNanos = nanos; return
        }
        val dt = (nanos - lastNanos) / 1_000_000_000f
        if (dt <= 0f) return
        val dx = x - lastX
        val dy = y - lastY
        val instant = hypot(dx, dy) / dt
        speedState.floatValue = speedState.floatValue * 0.62f + instant * 0.38f
        if (instant > 30f) {
            val inv = 1f / instant
            var nx = dirX * 0.7f + dx * inv * 0.3f
            var ny = dirY * 0.7f + dy * inv * 0.3f
            val n = hypot(nx, ny)
            if (n > 0.001f) { nx /= n; ny /= n }
            dirX = nx; dirY = ny
        }
        squashVel += (instant / 750f).coerceAtMost(1.15f) * 2.1f
        lastX = x; lastY = y; lastNanos = nanos
    }

    fun tick(dtSeconds: Float) {
        val decay = 0.86f.pow(dtSeconds * 60f)
        val speed = speedState.floatValue * decay
        speedState.floatValue = speed
        squashVel += (-squash * 160f - squashVel * 11.5f) * dtSeconds
        squash = (squash + squashVel * dtSeconds).coerceIn(-1.4f, 1.4f)
        val amt = (speed * 0.00016f).coerceAtMost(0.22f)
        scaleXState.floatValue = 1f + amt * dirX - amt * dirY * 0.5f + squash * 0.5f
        scaleYState.floatValue = 1f + amt * dirY - amt * dirX * 0.5f - squash * 0.5f
        wobbleState.floatValue = 0.75f + (speed * 0.0006f).coerceAtMost(0.6f)
    }
}

/**
 * The reference's water lens, ported to the recorded glass backdrop: the droplet
 * magnifies the page underneath (artwork included), its boundary wobbles in
 * silhouette, the rim splits into a chromatic fringe on the full chrome tier, and
 * the whole bead squashes along the finger's travel direction.
 *
 * The backdrop renders itself un-clipped to its interior — the shader outputs
 * transparency outside its wobbling contour, and the surface's ellipse box only
 * crops pixels that are already transparent. The lyrics window sits on top,
 * blurred, with a legibility scrim underneath it drawn by the surface itself.
 */
@Composable
private fun RefractionDropletLens(
    backdrop: LayerBackdrop,
    center: Offset?,
    lines: List<LyricLine>,
    positionProvider: (() -> Long)?,
    fallbackPositionMs: Long,
    accent: Color,
    modifier: Modifier
) {
    val chromatic = LocalDerbisGlassTier.current == GlassLensTier.Full
    val alpha by animateFloatAsState(
        targetValue = if (center != null) 1f else 0f,
        animationSpec = tween(durationMillis = 170),
        label = "lens-alpha"
    )
    var lastCenter by remember { mutableStateOf(Offset(120f, 140f)) }
    LaunchedEffect(center) { if (center != null) lastCenter = center }

    // One frame clock for the wobble and the spring; only runs while composed.
    val physics = remember { DropletPhysics() }
    val timeState = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { frame ->
                val dt = if (last == 0L) 0f else (frame - last) / 1_000_000_000f
                last = frame
                physics.tick(dt)
                timeState.floatValue = frame / 1_000_000_000f
            }
        }
    }
    LaunchedEffect(center) {
        val c = center
        if (c != null) physics.onPosition(c.x, c.y, System.nanoTime())
    }

    if (alpha <= 0.012f) {
        // Nothing to draw yet, but keep the overlay inert and cheap.
        return
    }

    // Follow playback at reading speed: the text is blurred anyway, so ~5 Hz is invisible.
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(180)
            tick += 1
        }
    }
    @Suppress("UNUSED_EXPRESSION") tick
    val positionMs = positionProvider?.invoke() ?: fallbackPositionMs

    val activeLines = remember(lines) {
        lines.filter { !it.isMetadata && !it.isInstrumental && it.text.isNotBlank() }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val lensW = with(density) { LensWidth.toPx() }
        val lensH = with(density) { LensHeight.toPx() }
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val desired = center ?: lastCenter
        val cx = desired.x.coerceIn(lensW / 2f + 4f, (maxW - lensW / 2f - 4f).coerceAtLeast(lensW / 2f + 4f))
        val cy = desired.y.coerceIn(lensH / 2f + 4f, (maxH - lensH / 2f - 4f).coerceAtLeast(lensH / 2f + 4f))

        val windowLines = rememberLyricWindow(activeLines, positionMs)

        Box(
            modifier = Modifier
                .offset { IntOffset((cx - lensW / 2f).toInt(), (cy - lensH / 2f).toInt()) }
                .size(LensWidth, LensHeight)
                .alpha(alpha)
                .graphicsLayer {
                    scaleX = physics.scaleXState.floatValue
                    scaleY = physics.scaleYState.floatValue
                }
                .shadow(elevation = 26.dp, shape = LensShape, clip = false)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { LensShape },
                    effects = {
                        droplet(
                            time = timeState.floatValue,
                            magnification = 1.14f,
                            wobble = physics.wobbleState.floatValue,
                            chromaticAberration = chromatic
                        )
                    },
                    highlight = null,
                    shadow = null,
                    onDrawSurface = {
                        // Legibility scrim over the refracted artwork, under the words.
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.30f),
                                0.34f to Color.Transparent,
                                0.72f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.34f)
                            ),
                            size = size
                        )
                    },
                    backdropScale = 0.5f
                )
        ) {
            // The frosted words under the water bead — large, soft, barely legible.
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .blur(7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                windowLines.forEach { line ->
                    val isCurrent = line.startMs <= positionMs && positionMs < line.endMs
                    Text(
                        text = line.text,
                        color = Color.White.copy(alpha = if (isCurrent) 0.95f else 0.70f),
                        fontSize = 20.sp,
                        lineHeight = 23.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(9.dp))
                }
            }

            // Top-left sheen above the words: the specular kick of a water droplet.
            // The radial circle fades before the ellipse corners, so no hard edges.
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                                center = Offset(size.width * 0.34f, size.height * 0.22f),
                                radius = size.width * 0.60f
                            )
                        )
                    }
            )
        }
    }
}

/** Pick the ±2 line window around the current playback position. */
@Composable
private fun rememberLyricWindow(lines: List<LyricLine>, positionMs: Long): List<LyricLine> {
    return remember(lines, positionMs / 900L) {
        if (lines.isEmpty()) {
            listOf(LyricLine(startMs = 0L, endMs = Long.MAX_VALUE, text = "Instrumental"))
        } else {
            val activeIndex = lines.indexOfLast { it.startMs <= positionMs }.let { if (it >= 0) it else 0 }
            val half = 2
            val lower = (activeIndex - half).coerceIn(0, (lines.size - (half * 2 + 1)).coerceAtLeast(0))
            lines.subList(lower, minOf(lines.size, lower + half * 2 + 1)).toList()
        }
    }
}

/**
 * Detect the press-and-hold on the stage without stealing the swipe gestures layered on top of
 * it. Nothing is consumed until the lens wakes, so a quick swipe still skips tracks; once the
 * lens is showing, the finger is "through the glass" and the stage stops reacting.
 *
 * [onCenter] receives the lens center in this modifier's pixel coordinates, or null when the
 * lens should hide; [onReveal] fires the moment the hold is recognized.
 */
internal fun Modifier.artworkHoldLens(
    onCenter: (Offset?) -> Unit,
    onReveal: () -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        onCenter(null)
        val down = awaitFirstDown(requireUnconsumed = false)
        val start = down.position
        onCenter(start)
        val slop = viewConfiguration.touchSlop

        // Phase 1 — a still hold for HoldRevealDelayMs wakes the droplet. The deadline is
        // absolute so a trembling finger cannot starve the reveal with fresh move events.
        var moved = false
        var releasedEarly = false
        val revealDeadline = System.currentTimeMillis() + HoldRevealDelayMs
        while (true) {
            val remaining = revealDeadline - System.currentTimeMillis()
            if (remaining <= 0L) break // stillness won — hold achieved
            val event = withTimeoutOrNull(remaining) { awaitPointerEvent() }
            if (event == null) break // stillness won — hold achieved
            var allUp = true
            for (change in event.changes) {
                if (change.pressed) {
                    allUp = false
                    if (hypot((change.position - start).x, (change.position - start).y) > slop) {
                        moved = true
                    }
                }
            }
            if (allUp) releasedEarly = true
            if (moved || releasedEarly) break
        }
        if (moved || releasedEarly) {
            onCenter(null)
            return@awaitEachGesture
        }
        onReveal()

        // Phase 2 — keep the droplet under the finger; swallow the stage's swipe tracking.
        while (true) {
            val event = awaitPointerEvent()
            var anyPressed = false
            for (change in event.changes) {
                if (change.pressed) {
                    anyPressed = true
                    change.consume()
                    onCenter(change.position)
                }
            }
            if (!anyPressed) break
        }
        onCenter(null)
    }
}
