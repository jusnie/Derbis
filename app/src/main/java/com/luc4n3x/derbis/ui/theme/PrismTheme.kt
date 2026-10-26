package com.luc4n3x.derbis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.derbis.data.ArtworkPalette
import com.luc4n3x.derbis.data.ArtworkPaletteCache
import com.luc4n3x.derbis.domain.*
import com.luc4n3x.derbis.ui.artwork.rememberArtworkPalette
import com.luc4n3x.derbis.ui.preferredPlayerArtworkUrl
import com.materialkolor.hct.Hct

// Keep the historic palette id so existing installations retain their selected skin.
internal const val PrismPaletteId = "derbis_prism"
internal const val TorquePaletteId = "derbis_torque"
internal val DerbisIsLiquidGlass: Boolean get() = DerbisActivePalette.id == PrismPaletteId
val DerbisIsTorque: Boolean get() = DerbisActivePalette.id == TorquePaletteId
val DerbisIsPrism: Boolean get() = DerbisIsLiquidGlass || DerbisIsTorque || DerbisIsIvory || DerbisIsSpotify || DerbisIsAppleMusic || DerbisIsTidal || DerbisIsYouTubeMusic
internal val DerbisSkinManagesLayout: Boolean get() = DerbisIsNocturne || DerbisIsPrism
internal val DerbisSkinLabel: String get() = DerbisActivePalette.label
internal var activePrismSettings by mutableStateOf(PrismSettings())
    private set
internal var activeTorqueSettings by mutableStateOf(TorqueSettings())
    private set

internal object PrismDesign {
    val artworkCorner: Dp get() = if (DerbisIsTorque) 0.dp else if (DerbisIsYouTubeMusic) YouTubeMusicDesign.playerArtworkCorner else if (DerbisIsAppleMusic) AppleMusicDesign.cornerL else if (DerbisIsSpotify) SpotifyDesign.cornerSmall else if (DerbisIsIvory) IvoryDesign.coverCorner else 18.dp
    val tileCorner: Dp get() = if (DerbisIsTorque) 2.dp else if (DerbisIsYouTubeMusic) YouTubeMusicDesign.cardCorner else if (DerbisIsAppleMusic) AppleMusicDesign.cornerL else if (DerbisIsSpotify) SpotifyDesign.cornerSmall else if (DerbisIsIvory) IvoryDesign.tileCorner else 12.dp
    val surfaceCorner: Dp get() = if (DerbisIsTorque) 2.dp else if (DerbisIsYouTubeMusic) YouTubeMusicDesign.tileCorner else if (DerbisIsAppleMusic) AppleMusicDesign.cornerXl else if (DerbisIsSpotify) SpotifyDesign.cornerLarge else if (DerbisIsIvory) IvoryDesign.surfaceCorner else 20.dp
    val controlCorner: Dp get() = if (DerbisIsTorque) 2.dp else if (DerbisIsYouTubeMusic) YouTubeMusicDesign.chipCorner else if (DerbisIsAppleMusic) AppleMusicDesign.cornerL else if (DerbisIsSpotify) SpotifyDesign.corner else if (DerbisIsIvory) IvoryDesign.controlCorner else 14.dp
    val gutter = 24.dp
    val rowHeight = 64.dp
    val iconTile = 44.dp
    val hairline = 1.dp
    val seekbarHeight: Dp get() = if (DerbisIsTorque) 3.dp else if (DerbisIsIvory || DerbisIsSpotify || DerbisIsAppleMusic) 4.dp else if (DerbisIsYouTubeMusic) 2.dp else 6.dp

    fun cut(corner: Dp): Shape =
        if (DerbisIsTorque) CutCornerShape(bottomEnd = 12.dp)
        else if (DerbisIsIvory || DerbisIsSpotify || DerbisIsAppleMusic || DerbisIsYouTubeMusic) RoundedCornerShape(corner)
        else RoundedCornerShape(topStart = corner, topEnd = corner, bottomEnd = corner / 3, bottomStart = corner)
}

internal object PrismAccent {
    // Liquid Glass uses cool spectral refraction rather than the old raspberry Prism gradient.
    private val nightStart = Color(0xFF72D5FF)
    private val nightEnd = Color(0xFFC4B5FD)
    private val lightStart = Color(0xFF007B9B)
    private val lightEnd = Color(0xFF5E5CC6)

    fun gradient(light: Boolean = DerbisActivePalette.isLight): Brush = when {
        DerbisIsTorque || DerbisIsSpotify || DerbisIsAppleMusic || DerbisIsYouTubeMusic -> SolidColor(DerbisActivePalette.blue)
        DerbisIsIvory -> SolidColor(IvoryDesign.ink)
        light -> Brush.horizontalGradient(listOf(lightStart, lightEnd))
        else -> Brush.horizontalGradient(listOf(nightStart, nightEnd))
    }

    fun wash(light: Boolean): Color = when {
        DerbisIsTorque || DerbisIsIvory || DerbisIsSpotify || DerbisIsAppleMusic || DerbisIsYouTubeMusic -> Color.Transparent
        light -> lightStart.copy(alpha = 0.07f)
        else -> nightStart.copy(alpha = 0.16f)
    }

    val Tint: Color get() = DerbisCyan.copy(alpha = if (DerbisActivePalette.isLight) 0.10f else 0.16f)
    val Hairline: Color get() = DerbisText.copy(alpha = if (DerbisActivePalette.isLight) 0.10f else 0.08f)
}

private val PrismRoundShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val TorqueShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp), small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp), large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

internal val PrismShapes: Shapes get() = if (DerbisIsTorque) TorqueShapes else if (DerbisIsSpotify) SpotifyShapes else if (DerbisIsAppleMusic) AppleMusicShapes else if (DerbisIsYouTubeMusic) YouTubeMusicShapes else if (DerbisIsIvory) IvoryShapes else PrismRoundShapes

internal val PrismTypography = derbisTypographyFor(DerbisFontPreset.Outfit).let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Normal, letterSpacing = (-1.2).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Normal, letterSpacing = (-1.0).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.6).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.6.sp)
    )
}

internal fun prismPalette(settings: PrismSettings = PrismSettings(), systemDark: Boolean = true): DerbisPalette {
    val light = settings.appearance == PrismAppearance.Light ||
        settings.appearance == PrismAppearance.Automatic && !systemDark
    // Neutral system materials must stay translucent too. A large amount of the app uses
    // palette surfaces directly rather than a dedicated card composable; keeping these opaque
    // is what made a selected Liquid Glass skin look like the standard dark UI.
    val translucent = settings.translucency != PrismTranslucency.Reduced
    fun systemMaterial(day: Long, night: Long, alpha: Float): Color =
        prismColor(light, day, night).copy(alpha = if (translucent) alpha else 1f)
    return DerbisThemes.cosmic.copy(
        id = PrismPaletteId, label = "Liquid Glass", emoji = "◈", isLight = light,
        followsCover = false, followsMood = false,
        black = prismColor(light, 0xFFF5F8FC, 0xFF080A0F),
        ink = systemMaterial(0xFFFFFFFF, 0xFF10141C, if (light) 0.70f else 0.66f),
        panel = systemMaterial(0xFFF8FAFD, 0xFF161C27, if (light) 0.76f else 0.70f),
        panelSoft = systemMaterial(0xFFEAF1F8, 0xFF202A38, if (light) 0.84f else 0.78f),
        cyan = prismColor(light, 0xFF006E99, 0xFF7DD3FC),
        blue = prismColor(light, 0xFF2563EB, 0xFF93C5FD),
        violet = prismColor(light, 0xFF635BCE, 0xFFC4B5FD),
        pink = prismColor(light, 0xFF3D7EA6, 0xFFA5D8FF),
        orange = prismColor(light, 0xFF007C91, 0xFF67E8F9),
        text = prismColor(light, 0xFF121821, 0xFFF4F8FF),
        muted = prismColor(light, 0xFF556271, 0xFFAAB6C7),
        outline = prismColor(light, 0xFFD5E0EB, 0xFF334155)
    )
}

private fun prismColor(light: Boolean, day: Long, night: Long): Color = Color(if (light) day else night)

internal const val TorqueRed = 0xFFE8322D.toInt()

internal fun torquePalette(settings: TorqueSettings = TorqueSettings(), coverSeed: Int? = null): DerbisPalette {
    val seed = if (settings.accent == TorqueAccent.Cover) coverSeed ?: TorqueRed else TorqueRed
    val source = Hct.fromInt(seed)
    val chroma = if (seed == TorqueRed) source.chroma else source.chroma.coerceAtLeast(36.0)
    fun tone(value: Double, maxChroma: Double) = Color(Hct.from(source.hue, chroma.coerceAtMost(maxChroma), value).toInt())
    val band = if (seed == TorqueRed) Color(TorqueRed) else tone(46.0, 64.0)
    return DerbisThemes.cosmic.copy(
        id = TorquePaletteId, label = "Torque", emoji = "◢", isLight = false,
        followsCover = false, followsMood = false,
        black = Color(0xFF111111), ink = Color(0xFF161616),
        panel = Color(0xFF1D1D1D), panelSoft = Color(0xFF272727),
        cyan = tone(66.0, 72.0), blue = band, violet = tone(34.0, 56.0),
        pink = tone(74.0, 60.0), orange = band,
        text = Color(0xFFF4F4F4), muted = Color(0xFFA9A9A9), outline = Color(0xFF333333)
    )
}

@Composable
internal fun PrismThemeEffect(settings: DerbisInterfaceSettings) {
    val prism = settings.prism
    val systemDark = isSystemInDarkTheme()
    val palette = remember(prism, systemDark) { prismPalette(prism, systemDark) }
    SideEffect {
        activePrismSettings = prism
        DerbisThemeController.applyPrism(palette)
    }
}

@Composable
internal fun TorqueThemeEffect(settings: DerbisInterfaceSettings, track: Track?) {
    val torque = settings.torque
    val key = remember(track?.id, track?.thumbnailUrl, track?.largeThumbnailUrl) {
        track?.let { ArtworkPaletteCache.key(it.id, it.thumbnailUrl, it.largeThumbnailUrl) }.orEmpty()
    }
    val fallback = remember { ArtworkPalette(TorqueRed, TorqueRed) }
    val cover by rememberArtworkPalette(
        paletteKey = if (torque.accent == TorqueAccent.Cover) key else "",
        artworkUrl = track?.let(::preferredPlayerArtworkUrl).orEmpty(),
        fallback = fallback,
        retainPreviousUntilLoaded = true
    )
    val palette = remember(torque, cover) { torquePalette(torque, cover.start.takeIf { it != TorqueRed }) }
    SideEffect {
        activeTorqueSettings = torque
        DerbisThemeController.applyPrism(palette)
    }
}

internal fun prismMiniSettings(saved: DerbisMiniPlayerStudioSettings): DerbisMiniPlayerStudioSettings {
    if (DerbisIsSpotify) return spotifyMiniSettings(saved)
    if (DerbisIsAppleMusic) return appleMusicMiniSettings(saved)
    if (DerbisIsYouTubeMusic) return youTubeMusicMiniSettings(saved)
    val torque = DerbisIsTorque
    val liquidGlass = DerbisActivePalette.id == PrismPaletteId
    val style = if (liquidGlass) MiniPlayerStyle.LiquidGlass else MiniPlayerStyle.Floating
    val tuning = DerbisMiniPlayerStudioSettings.spec(style).defaults.copy(
        cornerDp = if (torque) 2 else if (DerbisIsIvory) 26 else if (liquidGlass) 30 else 20,
        heightDp = 64,
        artworkDp = 46,
        shadow = 0,
        glow = 0,
        artworkShape = if (torque) MiniArtworkShape.Square else MiniArtworkShape.Rounded,
        progress = MiniProgressStyle.Line,
        buttons = if (torque) MiniButtonStyle.Outlined else MiniButtonStyle.Filled,
        colorMode = MiniColorMode.Theme,
        background = MiniBackground.Solid,
        motion = if (liquidGlass) MiniMotion.Reduced else MiniMotion.Normal,
        controls = saved.activeTuning().controls,
        showClose = saved.activeTuning().showClose
    )
    return saved.copy(style = style, tunings = saved.tunings + (style to tuning))
}

private fun spotifyMiniSettings(saved: DerbisMiniPlayerStudioSettings): DerbisMiniPlayerStudioSettings {
    val tuning = DerbisMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating).defaults.copy(
        cornerDp = 6, heightDp = 56, insetDp = 8, artworkDp = 40, shadow = 0, glow = 0,
        artworkShape = MiniArtworkShape.Rounded, progress = MiniProgressStyle.Line,
        buttons = MiniButtonStyle.Plain, colorMode = MiniColorMode.Artwork,
        background = MiniBackground.Solid, motion = MiniMotion.Normal,
        controls = saved.activeTuning().controls, showClose = saved.activeTuning().showClose
    )
    return saved.copy(style = MiniPlayerStyle.Floating, tunings = saved.tunings + (MiniPlayerStyle.Floating to tuning))
}

private fun appleMusicMiniSettings(saved: DerbisMiniPlayerStudioSettings): DerbisMiniPlayerStudioSettings {
    val tuning = DerbisMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating).defaults.copy(
        cornerDp = 24, heightDp = 48, insetDp = 20, artworkDp = 30, shadow = 0, glow = 0,
        artworkShape = MiniArtworkShape.Rounded, progress = MiniProgressStyle.Hidden,
        buttons = MiniButtonStyle.Plain, colorMode = MiniColorMode.Theme,
        background = MiniBackground.Solid, motion = MiniMotion.Normal,
        controls = saved.activeTuning().controls, showClose = saved.activeTuning().showClose
    )
    return saved.copy(style = MiniPlayerStyle.Floating, tunings = saved.tunings + (MiniPlayerStyle.Floating to tuning))
}

private fun youTubeMusicMiniSettings(saved: DerbisMiniPlayerStudioSettings): DerbisMiniPlayerStudioSettings {
    val tuning = DerbisMiniPlayerStudioSettings.spec(MiniPlayerStyle.YtMusic).defaults.copy(
        cornerDp = 0, heightDp = YouTubeMusicDesign.miniHeight.value.toInt(), insetDp = 0,
        artworkDp = YouTubeMusicDesign.miniThumb.value.toInt(), shadow = 0, glow = 0,
        artworkShape = MiniArtworkShape.Square, progress = MiniProgressStyle.Line,
        buttons = MiniButtonStyle.Plain, colorMode = MiniColorMode.Theme,
        background = MiniBackground.Solid, motion = MiniMotion.Normal,
        controls = MiniControlSet.Essential, showClose = false
    )
    return saved.copy(style = MiniPlayerStyle.YtMusic, tunings = saved.tunings + (MiniPlayerStyle.YtMusic to tuning))
}

internal fun skinMiniPlayerCorner(tuning: MiniPlayerTuning): Dp =
    when {
        DerbisIsYouTubeMusic -> 0.dp
        DerbisIsTorque -> 2.dp
        DerbisIsIvory -> 26.dp
        DerbisIsSpotify -> SpotifyDesign.corner
        DerbisIsAppleMusic -> AppleMusicDesign.miniHeight / 2
        DerbisIsNocturne -> 10.dp
        else -> tuning.cornerDp.dp
    }

internal fun prismNavSettings(saved: DerbisNavigationStudioSettings): DerbisNavigationStudioSettings {
    val liquidGlass = DerbisActivePalette.id == PrismPaletteId
    val style = if (liquidGlass) NavStyle.LiquidGlass else NavStyle.FloatingDock
    val tuning = saved.activeTuning().copy(
        cornerDp = if (DerbisIsTorque) 0 else if (liquidGlass) 32 else 24,
        heightDp = 64,
        indicator = if (liquidGlass) NavIndicator.Floating else NavIndicator.Pill,
        background = if (liquidGlass) NavBackground.Translucent else NavBackground.Solid,
        colorSource = NavColorSource.Theme,
        surfaceTone = NavSurfaceTone.Auto,
        motion = if (liquidGlass) NavMotion.Spring else NavMotion.Minimal,
        opacity = if (liquidGlass) 72 else 96,
        inactiveAlpha = 100,
        labels = NavLabelMode.Always
    )
    return saved.copy(style = style, tunings = saved.tunings + (style to tuning))
}

internal fun skinNavigationSettings(saved: DerbisNavigationStudioSettings): DerbisNavigationStudioSettings = when {
    DerbisIsNocturne -> nocturneNavSettings(saved)
    DerbisIsPrism -> prismNavSettings(saved)
    else -> saved
}

internal fun skinMiniSettings(saved: DerbisMiniPlayerStudioSettings): DerbisMiniPlayerStudioSettings {
    return when {
        DerbisIsNocturne -> nocturneMiniSettings(saved)
        DerbisIsPrism -> prismMiniSettings(saved)
        else -> saved
    }
}
