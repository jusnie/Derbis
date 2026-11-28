package com.yasuda.derbis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.ui.StableRemoteArtwork
import com.yasuda.derbis.ui.theme.DerbisActivePalette
import com.yasuda.derbis.ui.theme.DerbisText
import java.util.Locale
import kotlin.math.absoluteValue
import com.yasuda.derbis.ui.theme.DerbisTypeRhythm

internal val DerbisArtistItemWidth = 122.dp
internal val DerbisArtistAvatarSize = 114.dp
internal val DerbisArtistShelfSpacing = 18.dp
internal val DerbisArtistNameSpacing = 11.dp

private val DerbisArtistHighResThreshold = 128.dp
private val DerbisArtistAvatarSurface = Color(0xFF0B0A14)

private val DerbisArtistAccentPalette = listOf(
    Color(0xFF00E5FF) to Color(0xFF7B42FF),
    Color(0xFF1B5CFF) to Color(0xFFFF4FD8),
    Color(0xFFFF7A18) to Color(0xFF8E57FF),
    Color(0xFF00D4A6) to Color(0xFFFF3B5C),
    Color(0xFFFFB000) to Color(0xFF00E5FF)
)

internal fun derbisArtistAccent(
    key: String,
    accentStart: Int = 0,
    accentEnd: Int = 0
): Pair<Color, Color> {
    if (accentStart != 0 && accentEnd != 0) return Color(accentStart) to Color(accentEnd)
    val normalized = key.trim().lowercase(Locale.ROOT)
    if (normalized.isEmpty()) return DerbisArtistAccentPalette.first()
    val index = (normalized.hashCode().toLong().absoluteValue % DerbisArtistAccentPalette.size).toInt()
    return DerbisArtistAccentPalette[index]
}

@Composable
internal fun DerbisArtistAvatar(
    name: String,
    thumbnailUrl: String,
    accentStart: Color,
    accentEnd: Color,
    modifier: Modifier = Modifier,
    size: Dp = DerbisArtistAvatarSize,
    ringOverride: Color? = null
) {
    val ringBrush = if (ringOverride != null) {
        SolidColor(ringOverride)
    } else {
        Brush.sweepGradient(
            listOf(
                accentStart.copy(alpha = 0.94f),
                Color.White.copy(alpha = 0.42f),
                accentEnd.copy(alpha = 0.92f),
                accentStart.copy(alpha = 0.94f)
            )
        )
    }
    val innerSurface = if (DerbisActivePalette.isLight) {
        Color.White.copy(alpha = 0.86f)
    } else {
        DerbisArtistAvatarSurface.copy(alpha = 0.74f)
    }

    val ringWidth = (size * 0.013f).coerceAtLeast(1.dp)
    val ringGap = (size * 0.026f).coerceAtLeast(1.5.dp)
    val glowElevation = (size * 0.14f).coerceAtMost(22.dp)

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = glowElevation,
                shape = CircleShape,
                clip = false,
                ambientColor = accentStart.copy(alpha = 0.18f),
                spotColor = accentEnd.copy(alpha = 0.22f)
            )
            .background(brush = ringBrush, shape = CircleShape)
            .padding(ringWidth)
            .background(innerSurface, CircleShape)
            .padding(ringGap),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUrl.isNotBlank()) {
            StableRemoteArtwork(
                url = thumbnailUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(DerbisArtistAvatarSurface),
                highRes = size >= DerbisArtistHighResThreshold
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accentStart.copy(alpha = 0.34f),
                                DerbisArtistAvatarSurface,
                                accentEnd.copy(alpha = 0.26f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = name,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(size * 0.35f)
                )
            }
        }
    }
}

@Composable
internal fun DerbisArtistShelfItem(
    name: String,
    thumbnailUrl: String,
    accentStart: Color,
    accentEnd: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .width(DerbisArtistItemWidth)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DerbisArtistNameSpacing)
    ) {
        DerbisArtistAvatar(
            name = name,
            thumbnailUrl = thumbnailUrl,
            accentStart = accentStart,
            accentEnd = accentEnd,
            size = DerbisArtistAvatarSize
        )
        Text(
            text = name,
            color = DerbisText,
            fontSize = 14.sp,
            lineHeight = DerbisTypeRhythm.lineHeight(14.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.2).sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
