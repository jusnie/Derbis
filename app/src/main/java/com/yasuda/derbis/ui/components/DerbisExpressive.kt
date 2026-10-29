package com.yasuda.derbis.ui.components

import com.yasuda.derbis.ui.theme.DerbisIsNocturne
import com.yasuda.derbis.ui.theme.DerbisIsPrism
import com.yasuda.derbis.ui.theme.PrismDesign
import com.yasuda.derbis.ui.theme.Nocturne78Design

import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.yasuda.derbis.ui.theme.DerbisIsAppleMusic
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DerbisBackButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = DerbisBackButtonSize,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors()
) {
    IconButton(
        onClick = onClick,
        colors = colors,
        shapes = IconButtonDefaults.shapes(),
        modifier = modifier.size(size)
    ) {
        Icon(
            if (DerbisIsAppleMusic) Icons.AutoMirrored.Rounded.ArrowBackIos else Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = contentDescription,
            modifier = if (DerbisIsAppleMusic) Modifier.padding(start = 6.dp) else Modifier
        )
    }
}

private val DerbisBackButtonSize = 48.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DerbisLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    LoadingIndicator(modifier = modifier, color = color)
}

@Composable
internal fun DerbisExpressiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(
            shape = MaterialTheme.shapes.extraLarge,
            pressedShape = MaterialTheme.shapes.medium
        ),
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun derbisExpressiveCorner(
    interactionSource: InteractionSource,
    rest: Dp,
    pressed: Dp,
    label: String
): Dp {
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) pressed else rest,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = label
    )
    return corner
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun derbisExpressiveToggleCorner(
    checked: Boolean,
    unchecked: Dp,
    checkedCorner: Dp,
    label: String
): Dp {
    val corner by animateDpAsState(
        targetValue = if (checked) checkedCorner else unchecked,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = label
    )
    return corner
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DerbisWavyProgress(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    LinearWavyProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color,
        trackColor = trackColor
    )
}

internal val DerbisGroupedOuterCorner: Dp get() = if (DerbisIsPrism) PrismDesign.surfaceCorner else if (DerbisIsNocturne) Nocturne78Design.panelCorner else 24.dp
internal val DerbisGroupedInnerCorner: Dp get() = if (DerbisIsPrism) PrismDesign.controlCorner.coerceAtMost(6.dp) else if (DerbisIsNocturne) 2.dp else 6.dp

internal fun derbisGroupedListShape(index: Int, count: Int): Shape {
    val first = index == 0
    val last = index == count - 1
    return RoundedCornerShape(
        topStart = if (first) DerbisGroupedOuterCorner else DerbisGroupedInnerCorner,
        topEnd = if (first) DerbisGroupedOuterCorner else DerbisGroupedInnerCorner,
        bottomEnd = if (last) DerbisGroupedOuterCorner else DerbisGroupedInnerCorner,
        bottomStart = if (last) DerbisGroupedOuterCorner else DerbisGroupedInnerCorner
    )
}

internal fun derbisGroupedGridShape(row: Int, column: Int, rows: Int, columns: Int): Shape {
    fun corner(isOuter: Boolean): Dp = if (isOuter) DerbisGroupedOuterCorner else DerbisGroupedInnerCorner
    val top = row == 0
    val bottom = row == rows - 1
    val start = column == 0
    val end = column == columns - 1
    return RoundedCornerShape(
        topStart = corner(top && start),
        topEnd = corner(top && end),
        bottomEnd = corner(bottom && end),
        bottomStart = corner(bottom && start)
    )
}
