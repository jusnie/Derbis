package com.yasuda.derbis.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitPointerEvent
import androidx.compose.foundation.gestures.withTimeoutOrNull
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

        // Phase 1 — a still hold for HoldRevealDelayMs wakes the droplet.
        var moved = false
        var releasedEarly = false
        while (true) {
            val event = withTimeoutOrNull(HoldRevealDelayMs) { awaitPointerEvent() }
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
