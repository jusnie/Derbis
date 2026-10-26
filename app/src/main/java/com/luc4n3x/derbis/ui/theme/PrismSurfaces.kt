package com.luc4n3x.derbis.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.luc4n3x.derbis.domain.PrismTranslucency
import com.luc4n3x.derbis.ui.player.PlayerSurfaceTokens

@Composable
internal fun prismGlassColor(): Color {
    val capabilities = LocalDerbisVisualCapabilities.current
    val opaque = activePrismSettings.translucency == PrismTranslucency.Reduced || !capabilities.heavyBlur
    if (DerbisIsIvory) return DerbisActivePalette.ink
    if (DerbisIsSpotify) return SpotifyDesign.elevated
    if (DerbisIsAppleMusic) return AppleMusicDesign.chrome
    if (DerbisIsYouTubeMusic) return YouTubeMusicDesign.surface
    // A single, quiet material keeps the skin coherent; low-RAM/power-save uses an opaque
    // fallback rather than attempting costly backdrop sampling.
    return MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (opaque) 0.88f else 0.42f)
}

@Composable
internal fun PrismAtmosphere(modifier: Modifier = Modifier) {
    val palette = DerbisActivePalette
    val wash = PrismAccent.wash(palette.isLight)
    Box(modifier.fillMaxSize().background(palette.black).drawWithCache {
        val top = Brush.verticalGradient(listOf(wash.copy(alpha = wash.alpha * 0.6f), Color.Transparent), endY = size.height * 0.32f)
        onDrawBehind { drawRect(top) }
    })
}

@Composable
internal fun PrismPlayerAtmosphere(modifier: Modifier = Modifier) {
    val palette = DerbisActivePalette
    if (DerbisIsTorque || DerbisIsIvory || DerbisIsSpotify || DerbisIsAppleMusic || DerbisIsYouTubeMusic) {
        Box(modifier.fillMaxSize().background(if (DerbisIsAppleMusic || DerbisIsYouTubeMusic) Color.Black else palette.black))
        return
    }
    val wash = PrismAccent.wash(palette.isLight)
    Box(modifier.fillMaxSize().background(palette.black).drawWithCache {
        val fall = Brush.verticalGradient(listOf(wash, wash.copy(alpha = wash.alpha * 0.35f), Color.Transparent), endY = size.height * 0.72f)
        val halo = Brush.radialGradient(
            listOf(palette.violet.copy(alpha = if (palette.isLight) 0.05f else 0.10f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.3f), radius = size.width * 0.8f
        )
        onDrawBehind { drawRect(fall); drawRect(halo) }
    })
}

internal fun prismPlayerSurfaces(palette: DerbisPalette): PlayerSurfaceTokens = if (palette.id == AppleMusicPaletteId) PlayerSurfaceTokens(
    amoled = false, control = AppleMusicVibrant.quaternary, controlQuiet = Color.Transparent, outline = Color.Transparent,
    active = AppleMusicVibrant.tertiary, activeContent = AppleMusicVibrant.primary, tonal = AppleMusicVibrant.quaternary,
    hero = AppleMusicVibrant.primary, heroContent = Color.Black,
    content = AppleMusicVibrant.primary, contentMuted = AppleMusicVibrant.secondary, contentFaint = AppleMusicVibrant.tertiary, glow = Color.Transparent
) else if (palette.id == SpotifyPaletteId) PlayerSurfaceTokens(
    amoled = false, control = SpotifyDesign.tinted, controlQuiet = Color.Transparent, outline = Color.Transparent,
    active = SpotifyDesign.tintedHighlight, activeContent = SpotifyDesign.green, tonal = SpotifyDesign.tinted,
    hero = Color.White, heroContent = Color.Black,
    content = SpotifyDesign.text, contentMuted = SpotifyDesign.subdued, contentFaint = SpotifyDesign.faint, glow = Color.Transparent
) else PlayerSurfaceTokens(
    amoled = false,
    control = palette.panelSoft.copy(alpha = 0.56f),
    controlQuiet = palette.panel.copy(alpha = 0.38f),
    outline = palette.outline.copy(alpha = 0.46f),
    active = palette.panelSoft.copy(alpha = 0.68f),
    activeContent = palette.cyan,
    tonal = palette.panel.copy(alpha = 0.44f),
    hero = palette.cyan,
    heroContent = prismHeroContent(palette),
    content = palette.text,
    contentMuted = palette.muted,
    contentFaint = palette.muted,
    glow = Color.Transparent
)

internal fun prismHeroContent(palette: DerbisPalette): Color = when {
    palette.id == IvoryPaletteId -> palette.black
    palette.cyan.luminance() > PrismHeroLightAccentLuminance -> Color.Black
    else -> Color.White
}

private const val PrismHeroLightAccentLuminance = 0.5f
