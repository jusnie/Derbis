package com.luc4n3x.derbis.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.derbis.ui.LocalGlassBackdrop
import com.luc4n3x.derbis.ui.LocalDerbisGlassStyle
import com.luc4n3x.derbis.ui.playerMix
import com.luc4n3x.derbis.ui.theme.DerbisPlayerDesign
import com.luc4n3x.derbis.ui.theme.DerbisIsNocturne
import com.luc4n3x.derbis.ui.theme.Nocturne78Icons

data class PlayerControlLabels(
    val shuffle: String,
    val previous: String,
    val play: String,
    val pause: String,
    val next: String,
    val repeat: String
)

private val DensePlayerHorizontalTouchTarget = 40.dp

private fun ioniconForPlayer(icon: ImageVector): ImageVector? {
    return when (icon.name.substringAfterLast('.')) {
        "PlayArrow" -> DerbisIonicons.Play
        "Pause" -> DerbisIonicons.Pause
        "SkipPrevious" -> DerbisIonicons.SkipPrevious
        "SkipNext" -> DerbisIonicons.SkipNext
        "Shuffle" -> DerbisIonicons.Shuffle
        "Repeat" -> DerbisIonicons.Repeat
        "RepeatOne" -> DerbisIonicons.RepeatOne
        "KeyboardArrowDown" -> DerbisIonicons.ChevronDown
        "MoreVert" -> DerbisIonicons.MoreVertical
        "PlaylistAdd" -> DerbisIonicons.AddCircle
        "Favorite" -> DerbisIonicons.Heart
        "FavoriteBorder" -> DerbisIonicons.HeartOutline
        "Download", "FileDownload" -> DerbisIonicons.Download
        "QueueMusic", "PlaylistPlay" -> DerbisIonicons.Queue
        "Subject", "TextFields" -> DerbisIonicons.Lyrics
        "Bedtime", "Schedule", "Timer" -> DerbisIonicons.Timer
        "Equalizer", "GraphicEq", "Tune" -> DerbisIonicons.Equalizer
        "PhoneAndroid", "Devices" -> DerbisIonicons.Device
        "Settings" -> DerbisIonicons.Settings
        "Share" -> DerbisIonicons.Share
        else -> null
    }
}

@Composable
internal fun PlayerIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val nocturne = DerbisIsNocturne
    val resolved = remember(icon, nocturne) { if (nocturne) Nocturne78Icons.resolve(icon) else ioniconForPlayer(icon) ?: icon }
    Icon(
        imageVector = resolved,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

fun Modifier.playerGlass(
    shape: Shape,
    fill: Color = DerbisPlayerDesign.GlassFill,
    borderTop: Color = DerbisPlayerDesign.GlassBorderTop,
    borderBottom: Color = DerbisPlayerDesign.GlassBorderBottom,
    gradientBorder: Boolean = false
): Modifier = composed {
    val fallbackBorder = borderTop.playerMix(borderBottom, 0.5f)
    if (!LocalDerbisLiquidGlassEnabled.current) {
        return@composed this
            .background(fill, shape)
            .then(
                if (gradientBorder) {
                    Modifier.border(
                        BorderStroke(
                            DerbisPlayerDesign.Hairline,
                            Brush.verticalGradient(listOf(borderTop, borderBottom))
                        ),
                        shape
                    )
                } else {
                    Modifier.border(DerbisPlayerDesign.Hairline, fallbackBorder, shape)
                }
            )
    }

    this.resolvedDerbisGlass(
        backdrop = LocalGlassBackdrop.current,
        shape = shape,
        baseTint = fill.copy(alpha = 1f),
        fallbackColor = fill,
        fallbackBorderColor = fallbackBorder,
        intensity = DerbisGlassIntensity.Standard,
        artworkTint = null,
        borderWidth = DerbisPlayerDesign.Hairline,
        style = LocalDerbisGlassStyle.current
    )
}

@Composable
fun PlayerGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = DerbisPlayerDesign.HeaderButton,
    iconSize: Dp = 21.dp,
    tint: Color = DerbisPlayerDesign.TextPrimary,
    fill: Color = DerbisPlayerDesign.GlassFill,
    borderTop: Color = DerbisPlayerDesign.GlassBorderTop,
    borderBottom: Color = DerbisPlayerDesign.GlassBorderBottom,
    gradientBorder: Boolean = false,
    shape: Shape = CircleShape,
    enabled: Boolean = true
) {
    SpringIconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            // Dense player rows keep the full 48dp vertical target without reserving an
            // invisible 48dp column around every 36–40dp circular control.
            minWidth = maxOf(size, DensePlayerHorizontalTouchTarget),
            minHeight = DerbisPlayerDesign.MinimumTouchTarget
        ),
        enabled = enabled,
        pressedScale = 0.94f,
        contentDescription = contentDescription
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .playerGlass(
                    shape = shape,
                    fill = fill,
                    borderTop = borderTop,
                    borderBottom = borderBottom,
                    gradientBorder = gradientBorder
                ),
            contentAlignment = Alignment.Center
        ) {
            PlayerIcon(
                icon = icon,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
