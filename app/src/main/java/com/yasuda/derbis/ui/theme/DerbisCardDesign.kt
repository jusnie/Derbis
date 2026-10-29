package com.yasuda.derbis.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DerbisCardDesign {
    val ThumbCorner: Dp get() = if (DerbisIsSpotify) 4.dp else if (DerbisIsNocturne) 6.dp else 10.dp
    val ArtworkCorner: Dp get() = if (DerbisIsTorque) 2.dp else if (DerbisIsSpotify) 4.dp else if (DerbisIsPrism) 16.dp else if (DerbisIsNocturne) 6.dp else 12.dp
    val EditorialCorner: Dp get() = if (DerbisIsTorque) 2.dp else if (DerbisIsSpotify) 8.dp else if (DerbisIsPrism) 24.dp else if (DerbisIsNocturne) 10.dp else 20.dp
    val SurfaceCorner: Dp get() = if (DerbisIsSpotify) 8.dp else if (DerbisIsNocturne) 14.dp else 24.dp

    val ThumbShape: Shape get() = RoundedCornerShape(ThumbCorner)
    val ArtworkShape: Shape get() = RoundedCornerShape(ArtworkCorner)
    val EditorialShape: Shape get() = RoundedCornerShape(EditorialCorner)
    val SurfaceShape: Shape get() = RoundedCornerShape(SurfaceCorner)

    val RowThumb: Dp = 56.dp
    val RowHeight: Dp = 72.dp
    val RowTextGap: Dp = 14.dp
    val RowLineGap: Dp = 3.dp
    val RowHorizontalPadding: Dp = 4.dp

    val CaptionTopGap: Dp = 10.dp
    val CaptionLineGap: Dp = 3.dp

    val CardTitleSize = 14.5.sp
    val CardSubtitleSize = 12.5.sp
    val RowTitleSize = 15.5.sp
    val RowSubtitleSize = 13.sp
    val StatusGlyph: Dp = 13.dp
}
