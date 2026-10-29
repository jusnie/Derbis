package com.yasuda.derbis.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import com.yasuda.derbis.ui.components.PlayerIcon
import com.yasuda.derbis.ui.theme.DerbisHapticAction
import com.yasuda.derbis.ui.theme.DerbisHaptics
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.DerbisPlayerShapes
import com.yasuda.derbis.ui.theme.DerbisSegment
import com.yasuda.derbis.ui.theme.LocalDerbisHaptics

private const val DisabledSegmentAlpha = 0.42f
private const val BusyGlyphRatio = 0.82f

@Composable
internal fun RowScope.PlayerSegmentButton(
    position: DerbisSegment,
    weight: Float,
    container: Color,
    innerCorner: Dp,
    contentDescription: String,
    animated: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    toggleState: ToggleableState? = null,
    stateDescription: String? = null,
    outline: Color = Color.Transparent,
    haptic: DerbisHapticAction? = null,
    shapeOverride: Shape? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val haptics = LocalDerbisHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squished = pressed && enabled && animated
    val animatedWeight by animateFloatAsState(
        targetValue = segmentWeight(weight, squished),
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.pressSpring()),
        label = "player-segment-weight"
    )
    val corner by animateDpAsState(
        targetValue = segmentCorner(innerCorner, squished),
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.pressSpring()),
        label = "player-segment-corner"
    )
    val fill by animateColorAsState(
        targetValue = container,
        animationSpec = DerbisPlayerDesign.motion(animated, DerbisPlayerDesign.standardTween(200)),
        label = "player-segment-fill"
    )
    val shape = shapeOverride ?: DerbisPlayerShapes.segment(position, corner)

    Box(
        modifier = modifier
            .weight(animatedWeight)
            .fillMaxHeight()
            .graphicsLayer { alpha = segmentAlpha(enabled) }
            .clip(shape)
            .background(fill)
            .segmentOutline(outline, shape)
            .clickable(
                interactionSource = interaction,
                indication = segmentIndication(animated),
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.performOptional(haptic)
                    onClick()
                }
            )
            .segmentSemantics(contentDescription, toggleState, stateDescription),
        contentAlignment = Alignment.Center,
        content = content
    )
}

private fun segmentWeight(weight: Float, squished: Boolean): Float =
    if (squished) weight * (1f + DerbisPlayerDesign.SegmentPressGrowth) else weight

private fun segmentCorner(innerCorner: Dp, squished: Boolean): Dp =
    if (squished) max(innerCorner, DerbisPlayerDesign.SegmentPressedInnerCorner) else innerCorner

private fun segmentAlpha(enabled: Boolean): Float = if (enabled) 1f else DisabledSegmentAlpha

@Composable
private fun segmentIndication(animated: Boolean): Indication? =
    if (animated) null else LocalIndication.current

private fun DerbisHaptics.performOptional(action: DerbisHapticAction?) {
    if (action != null) perform(action)
}

private fun Modifier.segmentOutline(outline: Color, shape: Shape): Modifier =
    if (outline.alpha > 0f) border(DerbisPlayerDesign.Hairline, outline, shape) else this

private fun Modifier.segmentSemantics(
    description: String,
    toggleState: ToggleableState?,
    stateLabel: String?
): Modifier = semantics {
    contentDescription = description
    if (toggleState != null) toggleableState = toggleState
    if (stateLabel != null) stateDescription = stateLabel
}

internal fun segmentPosition(index: Int, count: Int): DerbisSegment = when {
    count == 1 -> DerbisSegment.Single
    index == 0 -> DerbisSegment.Leading
    index == count - 1 -> DerbisSegment.Trailing
    else -> DerbisSegment.Middle
}

internal fun segmentToggleState(toggle: Boolean, active: Boolean): ToggleableState? =
    if (toggle) ToggleableState(active) else null

@Composable
internal fun PlayerSegmentGlyph(icon: ImageVector, tint: Color, busy: Boolean, size: Dp) {
    if (busy) {
        CircularProgressIndicator(
            modifier = Modifier.size(size * BusyGlyphRatio),
            strokeWidth = DerbisPlayerDesign.SpaceXxs,
            color = tint
        )
    } else {
        PlayerIcon(icon, tint, Modifier.size(size))
    }
}
