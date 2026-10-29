package com.yasuda.derbis.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun nonNegativeCornerRadius(radius: Dp): Dp = radius.coerceAtLeast(0.dp)

enum class DerbisSegment {
    Leading,
    Middle,
    Trailing,
    Single
}

object DerbisPlayerShapes {

    private val Round = CornerSize(percent = 50)

    fun artworkCorner(artworkSize: Dp): Dp =
        (artworkSize * DerbisPlayerDesign.ArtworkCornerRatio)
            .coerceIn(DerbisPlayerDesign.ArtworkCornerMin, DerbisPlayerDesign.ArtworkCornerMax)

    fun segment(position: DerbisSegment, innerCorner: Dp): Shape {
        if (DerbisIsPrism) return RoundedCornerShape(PrismDesign.controlCorner)
        if (DerbisIsNocturne) return RoundedCornerShape(Nocturne78Design.controlCorner)
        val inner = CornerSize(nonNegativeCornerRadius(innerCorner))
        return when (position) {
            DerbisSegment.Leading -> RoundedCornerShape(
                topStart = Round,
                topEnd = inner,
                bottomEnd = inner,
                bottomStart = Round
            )
            DerbisSegment.Middle -> RoundedCornerShape(inner)
            DerbisSegment.Trailing -> RoundedCornerShape(
                topStart = inner,
                topEnd = Round,
                bottomEnd = Round,
                bottomStart = inner
            )
            DerbisSegment.Single -> RoundedCornerShape(Round)
        }
    }
}
