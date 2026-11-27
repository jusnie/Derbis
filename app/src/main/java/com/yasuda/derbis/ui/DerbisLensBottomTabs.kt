package com.yasuda.derbis.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.DerbisTab
import com.yasuda.derbis.ui.components.DerbisGlassIntensity
import com.yasuda.derbis.ui.components.derbisGlass
import com.yasuda.derbis.ui.theme.DerbisActivePalette
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics
import com.yasuda.derbis.ui.theme.prismGlassColor
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The Liquid Glass bottom bar, ported from iOS `DerbisLensTabBar`.
 *
 * Drag-to-scrub: pressing the bar and dragging across it snaps the selection to the slot under
 * the finger, with a light selection tick on every slot change. Hold (~130 ms without moving) or
 * move more than 5 dp and the lens wakes up: the droplet forms under the finger, the selected
 * icon magnifies, and the resting capsule hides. Releasing snaps back to the resting capsule.
 *
 * The bar itself is a floating capsule of real page glass — it is not a full-width dock.
 */
@Composable
internal fun DerbisLiquidGlassTabs(
    entries: List<DerbisTabEntry>,
    selected: DerbisTab,
    applySystemInset: Boolean,
    modifier: Modifier,
    onSelect: (DerbisTabEntry, Boolean) -> Unit
) {
    if (entries.isEmpty()) {
        Spacer(modifier)
        return
    }
    val haptics = LocalDerbisHaptics.current
    val density = LocalDensity.current
    val capsule = RoundedCornerShape(percent = 50)
    val dockHeight = 62.dp
    val dockGutter = 16.dp
    var lensPositionPx by remember { mutableFloatStateOf(0f) }
    var isHoldingLens by remember { mutableStateOf(false) }
    val selectedHolder = rememberUpdatedState(entries.indexOfFirst { it.tab == selected }.coerceAtLeast(0))
    val onSelectHolder = rememberUpdatedState(onSelect)

    val dockModifier = modifier.fillMaxWidth()

    Column(dockModifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (applySystemInset) Modifier.navigationBarsPadding() else Modifier)
                .padding(horizontal = dockGutter, vertical = 6.dp)
                .height(dockHeight)
                .shadow(10.dp, capsule, ambientColor = Color.Black.copy(alpha = 0.32f), spotColor = Color.Black.copy(alpha = 0.44f))
                .clip(capsule)
                .then(
                    Modifier.derbisGlass(
                        shape = capsule,
                        baseTint = Color.White,
                        fallbackColor = prismGlassColor(),
                        fallbackBorderColor = Color.White.copy(alpha = if (DerbisActivePalette.isLight) 0.32f else 0.22f),
                        intensity = DerbisGlassIntensity.Strong,
                        artworkTint = DerbisActivePalette.cyan
                    )
                )
                .pointerInput(entries) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val slotWidthPx = size.width.toFloat() / entries.size.toFloat()
                        val startX = down.position.x
                        val holdThresholdPx = 5.dp.toPx()
                        lensPositionPx = startX
                        var lastIndex = selectedHolder.value
                        val scrubTo: (Float) -> Unit = { x ->
                            lensPositionPx = x
                            val idx = ((x / slotWidthPx).toInt()).coerceIn(0, entries.size - 1)
                            if (idx != lastIndex) {
                                lastIndex = idx
                                haptics.perform(DerbisHapticAction.SeekSnap)
                                onSelectHolder.value(entries[idx], true)
                            }
                        }
                        // Select the pressed slot immediately, matching the iOS scrub.
                        scrubTo(startX)

                        val activation = withTimeoutOrNull(130L) {
                            var outcome = Activation.Released
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pointer = event.changes.firstOrNull { it.id == down.id }
                                if (pointer == null) {
                                    outcome = Activation.Cancelled
                                    break
                                }
                                scrubTo(pointer.position.x)
                                if (kotlin.math.abs(pointer.position.x - startX) > holdThresholdPx) {
                                    outcome = Activation.Activate
                                    break
                                }
                                if (pointer.changedToUp() || !pointer.pressed) break
                            }
                            outcome
                        }
                        // null = 130 ms elapsed with no qualifying move: the hold is the gesture.
                        var lensActivated = false
                        if (activation == null || activation == Activation.Activate) {
                            haptics.perform(DerbisHapticAction.Confirm)
                            isHoldingLens = true
                            lensActivated = true
                        }
                        if (lensActivated) {
                            // Lens stays up while the finger is down; scrub until release.
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                scrubTo(pointer.position.x)
                                if (pointer.changedToUp() || !pointer.pressed) break
                            }
                        }
                        isHoldingLens = false
                    }
                }
        ) {
            val slotWidth = maxWidth / entries.size.toFloat()
            val totalWidth = maxWidth
            val selectedIndex = entries.indexOfFirst { it.tab == selected }.coerceAtLeast(0)
            val indicatorWidth = maxOf(54.dp, slotWidth - 10.dp)
            val restingX by animateDpAsState(
                targetValue = slotWidth * selectedIndex.toFloat() + (slotWidth - indicatorWidth) * 0.5f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "derbis-lens-resting"
            )
            val restingAlpha by animateFloatAsState(
                targetValue = if (isHoldingLens) 0f else 1f,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
                label = "derbis-lens-resting-alpha"
            )

            // Resting selection capsule: the quiet accent fill behind the active tab.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = restingX)
                    .width(indicatorWidth)
                    .height(46.dp)
                    .graphicsLayer { alpha = restingAlpha }
                    .clip(capsule)
                    .background(DerbisCyan.copy(alpha = 0.18f))
                    .border(0.85.dp, DerbisCyan.copy(alpha = 0.42f), capsule)
            )

            // The items: icon over label, selected magnifies while the lens is up.
            Row(Modifier.fillMaxSize()) {
                entries.forEach { entry ->
                    val active = entry.tab == selected
                    val magnified = isHoldingLens && active
                    val itemScale by animateFloatAsState(
                        targetValue = if (magnified) 1.16f else 1f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                        label = "derbis-lens-item-scale"
                    )
                    val itemOffsetY by animateDpAsState(
                        targetValue = if (magnified) (-2).dp else 0.dp,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                        label = "derbis-lens-item-offset"
                    )
                    val tint = if (active) DerbisCyan else Color.White.copy(alpha = 0.68f)
                    val itemOffsetPx = with(density) { itemOffsetY.toPx() }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(active, role = Role.Tab, onClick = { onSelect(entry, active) })
                            .padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (active) entry.selectedIcon else entry.unselectedIcon,
                            contentDescription = entry.label,
                            tint = tint,
                            modifier = Modifier
                                .size(if (entry.label.isEmpty()) 20.dp else 18.dp)
                                .graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                    translationY = itemOffsetPx
                                }
                        )
                        if (entry.label.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = entry.label,
                                color = tint,
                                fontSize = 10.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                    translationY = itemOffsetPx
                                }
                            )
                        }
                    }
                }
            }

            // The droplet: the one surface that reads as a bead of liquid. It bends the bar
            // visually by magnifying its glyph and lifts off the surface with a glow.
            androidx.compose.animation.AnimatedVisibility(
                visible = isHoldingLens,
                enter = fadeIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)) +
                    scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)),
                exit = fadeOut(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)) +
                    scaleOut(targetScale = 0.88f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                val dropletWidth = minOf(slotWidth + 14.dp, maxOf(76.dp, slotWidth * 1.08f))
                val totalPx = with(density) { totalWidth.toPx() }
                val slotPx = totalPx / entries.size
                val clampedX = lensPositionPx
                    .coerceIn(slotPx * 0.48f, totalPx - slotPx * 0.48f)
                val dropletOffsetPx = clampedX - with(density) { dropletWidth.toPx() } / 2f
                val activeEntry = entries[selectedIndex]
                Box(
                    modifier = Modifier
                        .offset { IntOffset(dropletOffsetPx.toInt(), -with(density) { 4.dp.toPx() }.toInt()) }
                        .width(dropletWidth)
                        .height(58.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = capsule,
                            ambientColor = Color.White.copy(alpha = 0.20f),
                            spotColor = Color.Black.copy(alpha = 0.5f)
                        )
                        .clip(capsule)
                        .then(
                            Modifier.derbisGlass(
                                shape = capsule,
                                baseTint = DerbisCyan,
                                fallbackColor = DerbisCyan.copy(alpha = 0.88f),
                                fallbackBorderColor = Color.White.copy(alpha = 0.36f),
                                intensity = DerbisGlassIntensity.Strong,
                                artworkTint = DerbisActivePalette.cyan
                            )
                        )
                        .drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.maxDimension * 0.66f
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = activeEntry.selectedIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (activeEntry.label.isEmpty()) 21.dp else 19.dp)
                        )
                        if (activeEntry.label.isNotEmpty()) {
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = activeEntry.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class Activation {
    Activate,
    Released,
    Cancelled
}
