package com.yasuda.derbis.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.ui.LocalDerbisGlassAccessibility
import com.yasuda.derbis.ui.theme.DerbisActivePalette
import com.yasuda.derbis.ui.theme.DerbisBlack
import com.yasuda.derbis.ui.theme.DerbisInk
import com.yasuda.derbis.ui.theme.DerbisIsPureBlack
import com.yasuda.derbis.ui.theme.DerbisMotion
import com.yasuda.derbis.ui.theme.DerbisPanel
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import kotlin.math.roundToInt

private val DockCollapseDistance = 56.dp
private val DockExpandDistance = 24.dp
private val DockShadowDark = 16.dp
private val DockShadowLight = 10.dp
private const val DockFadeSpan = 0.6f

@Stable
class DerbisDockState internal constructor(
    private val collapseDistancePx: Float,
    private val expandDistancePx: Float
) : NestedScrollConnection {
    var compact by mutableStateOf(false)
        private set
    internal var compactionAllowed = true
    private var travelPx = 0f

    fun expand() {
        travelPx = 0f
        if (compact) compact = false
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y > 0f) {
            expand()
            return Offset.Zero
        }
        val delta = consumed.y
        if (delta == 0f || !delta.isFinite()) return Offset.Zero
        if (travelPx != 0f && (travelPx > 0f != delta > 0f)) travelPx = 0f
        travelPx += delta
        if (!compact && travelPx <= -collapseDistancePx) {
            travelPx = 0f
            if (compactionAllowed) compact = true
        } else if (compact && travelPx >= expandDistancePx) {
            expand()
        }
        return Offset.Zero
    }
}

@Composable
fun rememberDerbisDockState(): DerbisDockState {
    val density = LocalDensity.current
    val state = remember(density) {
        with(density) { DerbisDockState(DockCollapseDistance.toPx(), DockExpandDistance.toPx()) }
    }
    val context = LocalContext.current
    var touchExploration by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
            touchExploration = enabled
        }
        touchExploration = manager?.isTouchExplorationEnabled == true
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    LaunchedEffect(state, touchExploration) {
        state.compactionAllowed = !touchExploration
        if (touchExploration) state.expand()
    }
    return state
}

@Composable
fun DerbisDockState.animatedCompaction(animated: Boolean): State<Float> =
    animateFloatAsState(
        targetValue = if (compact) 1f else 0f,
        animationSpec = DerbisMotion.physics(animated, if (compact) DerbisMotion.settle else DerbisMotion.spatial),
        label = "dock-compaction"
    )

fun dockFade(compaction: Float): Float = (1f - compaction / DockFadeSpan).coerceIn(0f, 1f)

fun Modifier.dockLerpHeight(compaction: () -> Float, expanded: Dp, compact: Dp): Modifier =
    layout { measurable, constraints ->
        val height = lerpPx(expanded.toPx(), compact.toPx(), compaction()).roundToInt()
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(placeable.width, height) { placeable.place(0, 0) }
    }

fun Modifier.dockLerpSize(compaction: () -> Float, expanded: Dp, compact: Dp): Modifier =
    layout { measurable, _ ->
        val size = lerpPx(expanded.toPx(), compact.toPx(), compaction()).roundToInt()
        val placeable = measurable.measure(Constraints.fixed(size, size))
        layout(size, size) { placeable.place(0, 0) }
    }

fun Modifier.dockFoldHeight(compaction: () -> Float): Modifier =
    clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minHeight = 0))
            val height = (placeable.height * (1f - compaction().coerceIn(0f, 1f))).roundToInt()
            layout(placeable.width, height) { placeable.place(0, 0) }
        }
        .graphicsLayer { alpha = dockFade(compaction()) }

fun Modifier.dockFoldWidth(compaction: () -> Float): Modifier =
    clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            val width = (placeable.width * (1f - compaction().coerceIn(0f, 1f))).roundToInt()
            layout(width, placeable.height) { placeable.placeRelative(0, 0) }
        }
        .graphicsLayer { alpha = dockFade(compaction()) }

fun Modifier.dockClipHeight(compaction: () -> Float, collapseBy: Dp): Modifier =
    clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val height = (placeable.height - collapseBy.toPx() * compaction().coerceIn(0f, 1f))
                .roundToInt()
                .coerceAtLeast(0)
            layout(placeable.width, height) { placeable.place(0, 0) }
        }

@Immutable
private data class DockMaterial(
    val ground: Color,
    val frostTop: Color,
    val frostBottom: Color,
    val solidTop: Color,
    val solidBottom: Color,
    val rim: Color,
    val shadow: Dp
)

@Composable
private fun rememberDockMaterial(): DockMaterial {
    val isLight = DerbisActivePalette.isLight
    val pureBlack = DerbisIsPureBlack
    val ink = DerbisInk
    val black = DerbisBlack
    val panel = DerbisPanel
    return remember(isLight, pureBlack, ink, black, panel) {
        when {
            isLight -> DockMaterial(
                ground = black,
                frostTop = Color.White.copy(alpha = 0.54f),
                frostBottom = panel.copy(alpha = 0.68f),
                solidTop = Color.White.copy(alpha = 0.96f),
                solidBottom = panel.copy(alpha = 0.98f),
                rim = Color(0x2B11131F),
                shadow = DockShadowLight
            )
            pureBlack -> DockMaterial(
                ground = Color.Black,
                frostTop = Color.Black.copy(alpha = 0.56f),
                frostBottom = Color.Black.copy(alpha = 0.74f),
                solidTop = Color.Black.copy(alpha = 0.98f),
                solidBottom = Color.Black,
                rim = Color.White.copy(alpha = 0.16f),
                shadow = DockShadowDark
            )
            else -> DockMaterial(
                ground = black,
                frostTop = ink.copy(alpha = 0.54f),
                frostBottom = black.copy(alpha = 0.70f),
                solidTop = ink.copy(alpha = 0.96f),
                solidBottom = black.copy(alpha = 0.98f),
                rim = Color.White.copy(alpha = 0.17f),
                shadow = DockShadowDark
            )
        }
    }
}

@Composable
fun DerbisAdaptiveDockSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val material = rememberDockMaterial()
    val shape = remember {
        RoundedCornerShape(topStart = DerbisPlayerDesign.DockTrayCorner, topEnd = DerbisPlayerDesign.DockTrayCorner)
    }
    val accessibility = LocalDerbisGlassAccessibility.current
    val solidBrush = remember(material) { Brush.verticalGradient(listOf(material.solidTop, material.solidBottom)) }
    val rimBrush = remember(material) { Brush.verticalGradient(0f to material.rim, 0.35f to Color.Transparent) }
    val materialModifier = if (accessibility.prefersOpaqueMaterials) {
        Modifier.background(solidBrush)
    } else {
        Modifier.derbisChromeGlass(
            shape = shape,
            tint = defaultGlassSurfaceTint(DerbisActivePalette.isLight),
            fallback = Modifier.background(solidBrush)
        )
    }
    Column(
        modifier = modifier
            .shadow(material.shadow, shape, clip = false)
            // Frost must be clipped before its border: the old order could leave square samples
            // visible in the dock's rounded top corners.
            .clip(shape)
            .then(materialModifier)
            .border(DerbisPlayerDesign.Hairline, rimBrush, shape),
        content = content
    )
}

private fun lerpPx(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)
