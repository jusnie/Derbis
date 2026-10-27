package com.luc4n3x.derbis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.derbis.ui.theme.DerbisActivePalette
import com.luc4n3x.derbis.ui.theme.DerbisGlassBorder
import com.luc4n3x.derbis.ui.theme.DerbisIsLiquidGlass
import com.luc4n3x.derbis.ui.theme.DerbisPlayerDesign

enum class DerbisConnectedPosition {
    Single,
    Top,
    Middle,
    Bottom;

    companion object {
        fun of(index: Int, count: Int): DerbisConnectedPosition = when {
            count <= 1 -> Single
            index <= 0 -> Top
            index >= count - 1 -> Bottom
            else -> Middle
        }
    }
}

@Immutable
class DerbisConnectedShapes(outerCorner: Dp, innerCorner: Dp) {
    val single: Shape = RoundedCornerShape(outerCorner)
    val top: Shape = RoundedCornerShape(
        topStart = outerCorner,
        topEnd = outerCorner,
        bottomStart = innerCorner,
        bottomEnd = innerCorner
    )
    val middle: Shape = RoundedCornerShape(innerCorner)
    val bottom: Shape = RoundedCornerShape(
        topStart = innerCorner,
        topEnd = innerCorner,
        bottomStart = outerCorner,
        bottomEnd = outerCorner
    )

    val leading: Shape = RoundedCornerShape(
        topStart = outerCorner,
        bottomStart = outerCorner,
        topEnd = innerCorner,
        bottomEnd = innerCorner
    )
    val trailing: Shape = RoundedCornerShape(
        topStart = innerCorner,
        bottomStart = innerCorner,
        topEnd = outerCorner,
        bottomEnd = outerCorner
    )

    fun forRowPosition(position: DerbisConnectedPosition): Shape = when (position) {
        DerbisConnectedPosition.Single -> single
        DerbisConnectedPosition.Top -> leading
        DerbisConnectedPosition.Middle -> middle
        DerbisConnectedPosition.Bottom -> trailing
    }

    fun forPosition(position: DerbisConnectedPosition): Shape = when (position) {
        DerbisConnectedPosition.Single -> single
        DerbisConnectedPosition.Top -> top
        DerbisConnectedPosition.Middle -> middle
        DerbisConnectedPosition.Bottom -> bottom
    }
}

@Immutable
class DerbisConnectedStyle(
    val gap: Dp,
    val shapes: DerbisConnectedShapes,
    val fill: Color,
    val selectedFill: Color,
    val disabledFill: Color,
    val borderColor: Color,
    val selectedBorderColor: Color,
    val borderWidth: Dp
) {
    fun fillFor(selected: Boolean, enabled: Boolean): Color = when {
        !enabled -> disabledFill
        selected -> selectedFill
        else -> fill
    }

    fun borderFor(selected: Boolean): Color = if (selected) selectedBorderColor else borderColor
}

object DerbisConnectedDefaults {

    val Gap: Dp = 2.dp
    val OuterCorner: Dp = DerbisPlayerDesign.CornerMd
    val InnerCorner: Dp = DerbisPlayerDesign.CornerXxs

    @Composable
    fun style(
        accent: Color? = null,
        gap: Dp = Gap,
        outerCorner: Dp = OuterCorner,
        innerCorner: Dp = InnerCorner
    ): DerbisConnectedStyle {
        val border = DerbisGlassBorder
        return remember(accent, gap, outerCorner, innerCorner, border) {
            DerbisConnectedStyle(
                gap = gap,
                shapes = DerbisConnectedShapes(outerCorner, innerCorner),
                fill = Color.White.copy(alpha = 0.058f),
                selectedFill = accent?.copy(alpha = 0.13f) ?: Color.White.copy(alpha = 0.10f),
                disabledFill = Color.White.copy(alpha = 0.02f),
                borderColor = border,
                selectedBorderColor = accent?.copy(alpha = 0.30f) ?: border,
                borderWidth = DerbisPlayerDesign.Hairline
            )
        }
    }
}

fun Modifier.derbisConnectedRowSurface(
    position: DerbisConnectedPosition,
    style: DerbisConnectedStyle,
    selected: Boolean = false,
    enabled: Boolean = true,
    bordered: Boolean = selected || position == DerbisConnectedPosition.Single
): Modifier = composed {
    val shape = style.shapes.forRowPosition(position)
    val fill = style.fillFor(selected, enabled)
    if (LocalDerbisLiquidGlassEnabled.current && DerbisIsLiquidGlass) {
        this.derbisGlass(
            shape = shape,
            baseTint = if (selected) DerbisActivePalette.cyan else fill,
            fallbackColor = fill,
            fallbackBorderColor = style.borderFor(selected),
            intensity = if (selected) DerbisGlassIntensity.Standard else DerbisGlassIntensity.Subtle,
            artworkTint = DerbisActivePalette.cyan,
            borderWidth = if (bordered) style.borderWidth else 0.dp
        )
    } else {
        val base = this.clip(shape).background(fill, shape)
        if (bordered) base.border(style.borderWidth, style.borderFor(selected), shape) else base
    }
}

fun Modifier.derbisConnectedSurface(
    position: DerbisConnectedPosition,
    style: DerbisConnectedStyle,
    selected: Boolean = false,
    enabled: Boolean = true,
    bordered: Boolean = selected || position == DerbisConnectedPosition.Single
): Modifier = composed {
    val shape = style.shapes.forPosition(position)
    val fill = style.fillFor(selected, enabled)
    if (LocalDerbisLiquidGlassEnabled.current && DerbisIsLiquidGlass) {
        this.derbisGlass(
            shape = shape,
            baseTint = if (selected) DerbisActivePalette.cyan else fill,
            fallbackColor = fill,
            fallbackBorderColor = style.borderFor(selected),
            intensity = if (selected) DerbisGlassIntensity.Standard else DerbisGlassIntensity.Subtle,
            artworkTint = DerbisActivePalette.cyan,
            borderWidth = if (bordered) style.borderWidth else 0.dp
        )
    } else {
        val base = this.clip(shape).background(fill, shape)
        if (bordered) base.border(style.borderWidth, style.borderFor(selected), shape) else base
    }
}

@Composable
fun DerbisConnectedGroup(
    modifier: Modifier = Modifier,
    style: DerbisConnectedStyle = DerbisConnectedDefaults.style(),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(style.gap),
        content = content
    )
}
