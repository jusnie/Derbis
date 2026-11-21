package com.yasuda.derbis.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.R
import com.yasuda.derbis.domain.DerbisFontPreset
import com.yasuda.derbis.domain.*

/*
 * Pixel — the pink pixel skin, and the descendant of Nocturne 78.
 *
 * Nocturne 78 was a dark skin with one gold accent and serif headings. Pixel keeps the same
 * skeleton (one palette, one chrome pass, no per-screen forks) and every reference that used to
 * say Nocturne now says Pixel, but nothing about the look is shared: the ground is blush paper,
 * the ink is a deep plum, the accent is a rose that clears 4.5:1 on every surface below, and the
 * chrome is hard-edged. Corners are 0 dp, headings are monospace, and every surface is a flat
 * rectangle rather than a rounded card — there is no blur, no glass film and no glow.
 *
 * The palette stays light, so `isLight` is true and the colour scheme is built with
 * lightColorScheme. Anything that assumes a dark ground has to read `isLight` rather than
 * assume black.
 */

/** Blush paper. The ground every screen sits on. */
private val PixelPaper = Color(0xFFFFEDF1)

/** Deep plum. Text, and the one colour that must never be washed out. */
private val PixelInkDeep = Color(0xFF5B2338)

/** Rose that carries the accent role: 6.9:1 on paper, 4.6:1 on the deepest surface. */
private val PixelRose = Color(0xFF9E1B44)

internal val PixelPalette = DerbisThemes.cosmic.copy(
    id = "pixel",
    label = "Pixel",
    emoji = "♥",
    isLight = true,
    followsCover = false,
    followsMood = false,
    // A light palette keeps the same convention Ivory does: `black` is the deeper surface and
    // `ink` the lightest one, because both are used as backgrounds (DerbisBlack / DerbisInk).
    black = PixelPaper,
    ink = Color(0xFFFFF8F6),
    panel = Color(0xFFFFD9E2),
    panelSoft = Color(0xFFFFC9D6),
    cyan = PixelRose,
    blue = Color(0xFFB03060),
    violet = Color(0xFFC2245E),
    pink = Color(0xFFFF5C8A),
    orange = Color(0xFFE08A2E),
    text = PixelInkDeep,
    muted = Color(0xFF7E3B57),
    outline = Color(0xFFA84765)
)

val DerbisIsPixel: Boolean get() = DerbisActivePalette.id == PixelPalette.id

/**
 * A pixel skin has no corners. Every shape role is the same hard rectangle, so a card, a sheet,
 * a button and a dialog all share one edge.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val PixelShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
    largeIncreased = RoundedCornerShape(0.dp),
    extraLargeIncreased = RoundedCornerShape(0.dp),
    extraExtraLarge = RoundedCornerShape(0.dp)
)

internal val PixelifyFont: FontFamily = FontFamily(
    Font(resId = R.font.pixelify_sans_regular, weight = FontWeight.Normal),
    Font(resId = R.font.pixelify_sans_regular, weight = FontWeight.Medium),
    Font(resId = R.font.pixelify_sans_bold, weight = FontWeight.SemiBold),
    Font(resId = R.font.pixelify_sans_bold, weight = FontWeight.Bold),
    Font(resId = R.font.pixelify_sans_bold, weight = FontWeight.ExtraBold),
    Font(resId = R.font.pixelify_sans_bold, weight = FontWeight.Black)
)

internal val PixelTypography = derbisTypographyFor(DerbisFontPreset.System).let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        displayMedium = base.displayMedium.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        displaySmall = base.displaySmall.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        headlineLarge = base.headlineLarge.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        headlineMedium = base.headlineMedium.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        headlineSmall = base.headlineSmall.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        titleLarge = base.titleLarge.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        titleMedium = base.titleMedium.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp),
        titleSmall = base.titleSmall.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp),
        bodyLarge = base.bodyLarge.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Normal, letterSpacing = 0.1.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Normal, letterSpacing = 0.1.sp),
        bodySmall = base.bodySmall.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp),
        labelLarge = base.labelLarge.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
        labelMedium = base.labelMedium.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
        labelSmall = base.labelSmall.copy(fontFamily = PixelifyFont, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
    )
}

internal object PixelDesign {
    /** No corner radius anywhere: the pixel edge is the whole idea. */
    val artworkCorner = 0.dp
    val cardCorner = 0.dp
    val panelCorner = 0.dp
    val controlCorner = 0.dp

    /** The border every pixel surface draws instead of a shadow. */
    val border = 2.dp

    /** The hard offset shadow that replaces elevation: offset, never blurred. */
    val shadowOffset = 3.dp

    /** The deepest surface tone, for the one place a sketch needs a fourth step. */
    val variant = Color(0xFFFFB8C9)
}

internal fun pixelMiniSettings(saved: DerbisMiniPlayerStudioSettings): DerbisMiniPlayerStudioSettings {
    val userTuning = saved.tunings[MiniPlayerStyle.Floating] ?: saved.activeTuning()
    val tuning = DerbisMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating).defaults.copy(
        cornerDp = 0, heightDp = 66, artworkDp = 44, shadow = 0, glow = 0,
        artworkShape = MiniArtworkShape.Square, progress = MiniProgressStyle.Line,
        buttons = MiniButtonStyle.Outlined, colorMode = MiniColorMode.Theme,
        background = userTuning.background,
        opacity = userTuning.opacity,
        mascotHead = userTuning.mascotHead,
        pixelCharm = userTuning.pixelCharm,
        motion = MiniMotion.Reduced,
        controls = userTuning.controls, showClose = userTuning.showClose
    )
    return saved.copy(style = MiniPlayerStyle.Floating, tunings = saved.tunings + (MiniPlayerStyle.Floating to tuning))
}

internal fun pixelNavSettings(saved: DerbisNavigationStudioSettings): DerbisNavigationStudioSettings {
    val tuning = saved.activeTuning().copy(
        cornerDp = 0, heightDp = 64, indicator = NavIndicator.None,
        background = NavBackground.Solid, colorSource = NavColorSource.Theme,
        surfaceTone = NavSurfaceTone.Auto, motion = NavMotion.Minimal,
        opacity = 100, inactiveAlpha = 100, labels = NavLabelMode.Always
    )
    return saved.copy(style = NavStyle.Minimal, tunings = saved.tunings + (NavStyle.Minimal to tuning))
}
