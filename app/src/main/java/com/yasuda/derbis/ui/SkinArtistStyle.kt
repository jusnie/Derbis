package com.yasuda.derbis.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.ui.theme.*

internal fun artistHeroNameSize(name: String) = when {
    name.length <= 12 -> 48.sp
    name.length <= 18 -> 40.sp
    name.length <= 28 -> 34.sp
    else -> 28.sp
}

@Composable
internal fun artistHeroNameWeight(): FontWeight? = when {
    DerbisIsTorque -> FontWeight.Bold
    DerbisIsSpotify -> FontWeight.Black
    DerbisIsIvory -> FontWeight.Light
    DerbisIsPrism -> FontWeight.Normal
    DerbisIsNocturne -> FontWeight.Medium
    else -> DerbisType.heroTitle.fontWeight
}

internal fun artistHeroOverlineColor(): Color = when {
    DerbisIsTorque -> Color.White.copy(alpha = 0.85f)
    DerbisIsIvory -> IvoryDesign.rose
    DerbisSkinManagesLayout -> DerbisCyan
    else -> DerbisText.copy(alpha = 0.78f)
}

@Composable
internal fun artistPortraitFrame(): Modifier {
    if (DerbisIsTorque || DerbisIsSpotify || DerbisIsYouTubeMusic || DerbisIsAppleMusic || !DerbisSkinManagesLayout) return Modifier
    if (DerbisIsNocturne) {
        val shape = RoundedCornerShape(Nocturne78Design.artworkCorner)
        return Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 144.dp)
            .border(1.dp, DerbisActivePalette.outline, shape).padding(8.dp).clip(shape)
    }
    if (DerbisIsIvory) return Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 134.dp).clip(RoundedCornerShape(40.dp))
    return Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 134.dp)
        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 8.dp))
}

internal class ArtistHeroScrim(val color: Color, val middle: Float, val deep: Float, val end: Float)

internal fun artistHeroScrim(): ArtistHeroScrim =
    if (DerbisIsAppleMusic) ArtistHeroScrim(Color.Black, 0.18f, 0.42f, 0.55f)
    else if (DerbisIsYouTubeMusic) ArtistHeroScrim(Color.Black, 0.2f, 0.78f, 1f) else ArtistHeroScrim(DerbisBlack, 0.55f, 0.94f, 1f)

internal fun artistHeroShowsOverline(): Boolean = !DerbisIsSpotify && !DerbisIsAppleMusic && !DerbisIsYouTubeMusic

internal fun artistHeroNameColor(): Color = if (DerbisIsTorque || DerbisIsAppleMusic || DerbisIsYouTubeMusic) Color.White else DerbisText

internal fun artistHeroAudienceColor(): Color = (if (DerbisIsAppleMusic || DerbisIsYouTubeMusic) Color.White else DerbisText).copy(alpha = 0.72f)

@Composable
internal fun artistHeroNameStyle(nameSize: androidx.compose.ui.unit.TextUnit): androidx.compose.ui.text.TextStyle =
    if (DerbisIsAppleMusic) androidx.compose.material3.MaterialTheme.typography.headlineLarge
    else if (DerbisIsYouTubeMusic) androidx.compose.material3.MaterialTheme.typography.displayLarge
    else DerbisType.heroTitle.copy(
        fontSize = nameSize,
        lineHeight = DerbisType.heroLineHeight(nameSize),
        fontWeight = artistHeroNameWeight(),
        letterSpacing = artistHeroNameSpacing()
    )
