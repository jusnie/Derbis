package com.yasuda.derbis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.ui.theme.DerbisActivePalette
import com.yasuda.derbis.ui.theme.DerbisText

/**
 * How far the adaptive dock has to be compacted before the Liquid Glass skin fuses the bottom
 * chrome into one row of orbs and a narrow player pill. The dock compacts past half way during
 * the first short scroll, which is exactly when the mock-up's layout is the useful one.
 */
internal const val LIQUID_GLASS_FUSED_DOCK_THRESHOLD = 0.5f

/** Matches the other glass chrome springs, so the dock melts at the same speed as the island. */
private const val FusedDockDamping = 0.74f
private const val FusedDockStiffness = 380f

/** The pill keeps its full height while it is fused between two orbs. */
private val FlatDockCompaction: () -> Float = { 0f }

/**
 * The Liquid Glass bottom chrome as one continuous glass group.
 *
 * Laid out as `[orb] [mini player] [orb]` at all times. When [fused] is false the orbs animate
 * down to zero width, so the row is exactly the plain mini player that fills the bottom of the
 * screen today; when it is true the dock below has already shrunk away and the two orbs grow out
 * of the pill's edges, narrowing it into the mock-up's compact bar. Because the orbs own real
 * layout width (not a scale), the weighted pill re-measures every frame of the spring — the width
 * change is a layout morph, not a cross-fade between two different bars.
 */
@Composable
fun LiquidGlassFusedDock(
    fused: Boolean,
    orbSize: Dp,
    startIcon: ImageVector,
    startLabel: String,
    onStart: () -> Unit,
    endIcon: ImageVector,
    endLabel: String,
    onEnd: () -> Unit,
    animated: Boolean,
    modifier: Modifier = Modifier,
    mini: @Composable () -> Unit
) {
    val fraction by animateFloatAsState(
        targetValue = if (fused) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = FusedDockDamping, stiffness = FusedDockStiffness)
        } else {
            snap()
        },
        label = "liquid-glass-fused-dock"
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FusedDockOrb(
            fraction = fraction,
            orbSize = orbSize,
            icon = startIcon,
            label = startLabel,
            onClick = onStart,
            alignStart = true
        )
        Box(modifier = Modifier.weight(1f)) { mini() }
        FusedDockOrb(
            fraction = fraction,
            orbSize = orbSize,
            icon = endIcon,
            label = endLabel,
            onClick = onEnd,
            alignStart = false
        )
    }
}

/**
 * One glass orb of the fused row. Width, height and the circle itself all follow [fraction], so at
 * zero it occupies no space at all and the mini player keeps the full width it has today.
 *
 * The orb reserves [OrbEdgeGapDp] beside the pill only while it is visible; that gap is what keeps
 * the three pieces reading as separate glass lenses rather than one smeared bar.
 */
@Composable
private fun FusedDockOrb(
    fraction: Float,
    orbSize: Dp,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    alignStart: Boolean
) {
    if (fraction <= 0.01f) return
    val circle = orbSize * fraction
    Box(
        modifier = Modifier
            .width((orbSize + OrbEdgeGapDp) * fraction)
            .height(circle)
            .alpha(fraction.coerceIn(0f, 1f)),
        contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .size(circle)
                .derbisChromeGlass(
                    shape = CircleShape,
                    tint = defaultGlassSurfaceTint(DerbisActivePalette.isLight),
                    fallback = Modifier.derbisLightweightGlass(CircleShape, fusedOrbFallback())
                )
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DerbisText,
                modifier = Modifier.size(circle * 0.42f)
            )
        }
    }
}

private val OrbEdgeGapDp = 8.dp

/** Only used when the glass engine cannot sample the page (API < 31, opaque materials). */
private fun fusedOrbFallback(): Color =
    if (DerbisActivePalette.isLight) {
        Color(0xFFFAFAFA).copy(alpha = 0.82f)
    } else {
        Color(0xFF121212).copy(alpha = 0.82f)
    }

/**
 * The portrait bottom chrome: the fused glass row, plus the ordinary dock for as long as the row
 * has not taken over.
 *
 * [compaction] is a lambda on purpose. The old chrome sampled the dock's compaction through a
 * lambda so a scrolling list could animate it without invalidating the whole app; the fused switch
 * needs to read it during composition, so that read lives here, in a composable small enough to
 * recompose on every frame of the spring without touching the screen behind it.
 */
@Composable
fun LiquidGlassBottomChrome(
    enabled: Boolean,
    compaction: () -> Float,
    orbSize: Dp,
    startIcon: ImageVector,
    startLabel: String,
    onStart: () -> Unit,
    endIcon: ImageVector,
    endLabel: String,
    onEnd: () -> Unit,
    animated: Boolean,
    modifier: Modifier = Modifier,
    mini: @Composable (() -> Float) -> Unit,
    dock: @Composable () -> Unit
) {
    val fused = enabled && compaction() > LIQUID_GLASS_FUSED_DOCK_THRESHOLD
    // Between the orbs the pill is the tallest thing in the row, so it stops accepting the dock's
    // compaction shrink: the orbs are its new frame, not the dock.
    val pillCompaction = if (fused) FlatDockCompaction else compaction
    val millis: (Int) -> Int = { if (animated) it else 0 }
    LiquidGlassFusedDock(
        fused = fused,
        orbSize = orbSize,
        startIcon = startIcon,
        startLabel = startLabel,
        onStart = onStart,
        endIcon = endIcon,
        endLabel = endLabel,
        onEnd = onEnd,
        animated = animated,
        modifier = modifier,
        mini = { mini(pillCompaction) }
    )
    AnimatedVisibility(
        visible = !fused,
        enter = expandVertically(animationSpec = tween(millis(220))) + fadeIn(animationSpec = tween(millis(180))),
        exit = shrinkVertically(animationSpec = tween(millis(200))) + fadeOut(animationSpec = tween(millis(140)))
    ) {
        dock()
    }
}
