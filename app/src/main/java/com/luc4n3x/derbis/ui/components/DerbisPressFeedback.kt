package com.luc4n3x.derbis.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.luc4n3x.derbis.ui.LocalAnimationsEnabled
import com.luc4n3x.derbis.ui.theme.DerbisHapticAction
import com.luc4n3x.derbis.ui.theme.DerbisMotion
import com.luc4n3x.derbis.ui.theme.LocalDerbisHaptics

object DerbisPressScale {
    const val Row: Float = DerbisMotion.Scale.Row
    const val Tile: Float = DerbisMotion.Scale.Tile
    const val Surface: Float = DerbisMotion.Scale.Surface
    const val Control: Float = DerbisMotion.Scale.Control
}

@Composable
fun Modifier.derbisPressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = DerbisPressScale.Surface,
    interactionSource: MutableInteractionSource? = null,
    role: Role? = null,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    haptic: DerbisHapticAction? = null,
    longPressHaptic: DerbisHapticAction? = DerbisHapticAction.Confirm
): Modifier {
    val animationsEnabled = LocalAnimationsEnabled.current
    val haptics = LocalDerbisHaptics.current
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && animationsEnabled) pressedScale else 1f,
        animationSpec = DerbisMotion.physics(animationsEnabled, DerbisMotion.press),
        label = "derbis-press"
    )
    val indication = if (animationsEnabled) null else LocalIndication.current
    val click: () -> Unit = {
        haptic?.let(haptics::perform)
        onClick()
    }
    val longClick: (() -> Unit)? = onLongClick?.let { action ->
        {
            longPressHaptic?.let(haptics::perform)
            action()
        }
    }
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (longClick == null) {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = indication,
                    enabled = enabled,
                    onClickLabel = onClickLabel,
                    role = role,
                    onClick = click
                )
            } else {
                Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = indication,
                    enabled = enabled,
                    onClickLabel = onClickLabel,
                    role = role,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = longClick,
                    onClick = click
                )
            }
        )
}
