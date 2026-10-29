package com.yasuda.derbis.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.animation.animateColorAsState
import com.yasuda.derbis.ui.components.derbisExpressiveCorner
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.ui.components.DerbisPressScale
import com.yasuda.derbis.ui.components.derbisExpressiveToggleCorner
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign

@Immutable
internal data class PlayerDockAction(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val active: Boolean = false,
    val toggle: Boolean = false,
    val busy: Boolean = false,
    val enabled: Boolean = true,
    val stateDescription: String? = null
)

@Composable
internal fun PlayerActionDock(
    actions: List<PlayerDockAction>,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return
    Row(
        modifier = modifier
            .widthIn(max = DerbisPlayerDesign.DockMaxWidth)
            .fillMaxWidth()
            .height(dockHeight(compact)),
        horizontalArrangement = Arrangement.spacedBy(DerbisPlayerDesign.DockGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEachIndexed { index, action ->
            key(action.key) {
                PlayerDockSegment(
                    action = action,
                    first = index == 0,
                    last = index == actions.lastIndex,
                    surfaces = surfaces,
                    animated = animated
                )
            }
        }
    }
}

@Composable
private fun RowScope.PlayerDockSegment(
    action: PlayerDockAction,
    first: Boolean,
    last: Boolean,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val prism = PlayerPrismStyle
    val outer = dockHeight(false) / 2
    val inner = derbisExpressiveCorner(
        interactionSource = interaction,
        rest = if (action.active) outer else DerbisPlayerDesign.DockInnerCorner,
        pressed = outer,
        label = "player-dock-inner"
    )
    val fill by animateColorAsState(
        targetValue = playerDockFill(action.active, surfaces, prism),
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.standardTween(220)),
        label = "player-dock-fill"
    )
    val tint by animateColorAsState(
        targetValue = playerDockTint(action.active, surfaces, prism),
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.standardTween(220)),
        label = "player-dock-tint"
    )
    val shape = if (prism) RoundedCornerShape(com.yasuda.derbis.ui.theme.PrismDesign.controlCorner) else dockSegmentShape(first = first, last = last, outer = outer, inner = inner)
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer { alpha = if (action.enabled) 1f else DisabledDockAlpha }
            .clip(shape)
            .background(fill)
            .derbisPressable(
                onClick = { if (!action.busy) action.onClick() },
                enabled = action.enabled,
                interactionSource = interaction,
                pressedScale = DerbisPressScale.Control,
                role = Role.Button
            )
            .dockActionSemantics(action),
        contentAlignment = Alignment.Center
    ) {
        PlayerSegmentGlyph(action.icon, tint, action.busy, DerbisPlayerDesign.DockGlyph)
    }
}

private fun playerDockFill(active: Boolean, surfaces: PlayerSurfaceTokens, prism: Boolean): Color =
    if (prism) {
        if (active) com.yasuda.derbis.ui.theme.PrismAccent.Tint else Color.Transparent
    } else if (active) surfaces.hero else surfaces.tonal

private fun playerDockTint(active: Boolean, surfaces: PlayerSurfaceTokens, prism: Boolean): Color =
    if (prism) {
        if (active) surfaces.activeContent else surfaces.contentMuted
    } else if (active) surfaces.heroContent else surfaces.content

@Composable
internal fun PlayerToggleControl(
    icon: ImageVector,
    label: String,
    active: Boolean,
    toggle: Boolean,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    glyph: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    busy: Boolean = false,
    stateDescription: String? = null
) {
    val tint by animateColorAsState(
        targetValue = surfaces.tintFor(active),
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.standardTween(200)),
        label = "player-toggle-tint"
    )
    val fill by animateColorAsState(
        targetValue = if (active) surfaces.active else surfaces.controlQuiet,
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.standardTween(200)),
        label = "player-toggle-fill"
    )
    val corner = derbisExpressiveToggleCorner(
        checked = active,
        unchecked = DerbisPlayerDesign.MinimumTouchTarget / 2,
        checkedCorner = ToggleCheckedCorner,
        label = "player-toggle-corner"
    )
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = Modifier
            .size(DerbisPlayerDesign.MinimumTouchTarget)
            .graphicsLayer { alpha = if (enabled) 1f else DisabledDockAlpha }
            .clip(shape)
            .background(fill)
            .derbisPressable(
                onClick = { if (!busy) onClick() },
                enabled = enabled,
                pressedScale = DerbisPressScale.Control,
                role = Role.Button
            )
            .semantics {
                contentDescription = label
                segmentToggleState(toggle, active)?.let { toggleableState = it }
                stateDescription?.let { this.stateDescription = it }
            },
        contentAlignment = Alignment.Center
    ) {
        PlayerSegmentGlyph(icon, tint, busy, glyph)
    }
}

private val ToggleCheckedCorner: Dp = 14.dp
private const val DisabledDockAlpha = 0.42f

private fun dockSegmentShape(first: Boolean, last: Boolean, outer: Dp, inner: Dp): Shape {
    val start = if (first) outer else inner
    val end = if (last) outer else inner
    return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
}

private fun Modifier.dockActionSemantics(action: PlayerDockAction): Modifier = semantics {
    contentDescription = action.label
    segmentToggleState(action.toggle, action.active)?.let { toggleableState = it }
    action.stateDescription?.let { stateDescription = it }
}

private fun dockHeight(compact: Boolean) =
    if (compact) DerbisPlayerDesign.DockHeightCompact else DerbisPlayerDesign.DockHeight
