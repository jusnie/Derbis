package com.yasuda.derbis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.yasuda.derbis.ui.icons.Pause
import com.yasuda.derbis.ui.icons.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yasuda.derbis.ui.components.DerbisGlassIntensity
import com.yasuda.derbis.ui.components.DerbisLoadingIndicator
import com.yasuda.derbis.ui.components.derbisGlass
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.theme.*

private fun StudioHero.secondLine(): String =
    lead.takeIf { it.isNotBlank() && !it.trim().endsWith(track.artist.trim(), ignoreCase = true) } ?: track.title

@Composable
private fun SoundtrackArt(hero: StudioHero, modifier: Modifier) {
    if (hero.artistArtworkUrl.isNotBlank()) {
        AsyncImage(hero.artistArtworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        CoverImage(hero.track, modifier, highRes = true)
    }
}

@Composable
private fun SoundtrackCompanions(urls: List<String>, ring: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        urls.take(3).forEach { url ->
            StableRemoteArtwork(
                url, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(30.dp).clip(CircleShape).border(2.dp, ring, CircleShape)
            )
        }
    }
}

/**
 * The Liquid Glass hero deliberately uses one uninterrupted artwork surface. The former split
 * Prism treatment is not used for this skin: content floats over the cover in refractive glass.
 */
@Composable
internal fun LiquidGlassSoundtrackHero(hero: StudioHero, playLabel: String) {
    val shape = RoundedCornerShape(32.dp)
    val artworkTint = DerbisActivePalette.cyan
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(shape)
            .background(DerbisPanelSoft)
            .border(1.dp, Color.White.copy(alpha = 0.34f), shape)
            .derbisPressable(onClick = hero.onOpen, onClickLabel = playLabel, role = Role.Button)
    ) {
        SoundtrackArt(hero, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.04f),
                        0.48f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f)
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .derbisGlass(
                    shape = CircleShape,
                    baseTint = Color.White,
                    fallbackColor = Color.Black.copy(alpha = 0.40f),
                    fallbackBorderColor = Color.White.copy(alpha = 0.48f),
                    intensity = DerbisGlassIntensity.Strong,
                    artworkTint = artworkTint
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(DerbisCyan))
            Text(hero.label, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (hero.companionArtworkUrls.isNotEmpty()) {
            Box(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                SoundtrackCompanions(hero.companionArtworkUrls, Color.White.copy(alpha = 0.8f))
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 108.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                hero.track.artist,
                color = Color.White,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-1.2).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                hero.secondLine(),
                color = Color.White.copy(alpha = 0.84f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        PrismPlayDisc(
            playing = hero.isCurrent && hero.isPlaying,
            resolving = hero.isCurrent && hero.isResolving,
            size = 64.dp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 20.dp)
        )
    }
}

@Composable
internal fun TorqueSoundtrackHero(hero: StudioHero, playLabel: String) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(0.82f).background(DerbisPanel)
            .derbisPressable(onClick = hero.onOpen, onClickLabel = playLabel, role = Role.Button)
    ) {
        SoundtrackArt(hero, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 0.75f to Color.Black.copy(alpha = 0.35f))))
        Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(DerbisActivePalette.blue))
        Text(
            hero.label.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.TopStart).background(Color.Black).padding(horizontal = 12.dp, vertical = 8.dp)
        )
        if (hero.companionArtworkUrls.isNotEmpty()) Box(Modifier.align(Alignment.TopEnd).padding(14.dp)) { SoundtrackCompanions(hero.companionArtworkUrls, Color.Black) }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 20.dp, end = 104.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(hero.track.artist.uppercase(), color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hero.secondLine(), color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp).size(72.dp).clip(CircleShape)
                .background(Color.Black).border(5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val playing = hero.isCurrent && hero.isPlaying
            if (hero.isCurrent && hero.isResolving) DerbisLoadingIndicator(color = Color.White, modifier = Modifier.size(28.dp))
            else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(38.dp))
        }
    }
}
