package com.luc4n3x.derbis.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.luc4n3x.derbis.R
import com.luc4n3x.derbis.domain.DerbisFontPreset

private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun googleFontFamily(name: String): FontFamily = FontFamily(
    Font(googleFont = GoogleFont(name), fontProvider = googleFontsProvider)
)

val OutfitFont: FontFamily = googleFontFamily("Outfit")
private val InterFont: FontFamily = googleFontFamily("Inter")
private val ManropeFont: FontFamily = googleFontFamily("Manrope")
private val PoppinsFont: FontFamily = googleFontFamily("Poppins")
private val MontserratFont: FontFamily = googleFontFamily("Montserrat")
private val NunitoFont: FontFamily = googleFontFamily("Nunito")
private val RobotoFont: FontFamily = googleFontFamily("Roboto")

internal val SpotifyFont: FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.Bold, FontWeight.Black).map { weight ->
        Font(googleFont = GoogleFont("Figtree"), fontProvider = googleFontsProvider, weight = weight)
    }
)

internal val AppleMusicFont: FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(googleFont = GoogleFont("Inter"), fontProvider = googleFontsProvider, weight = weight)
    }
)

/**
 * A legal SF Pro-style fallback reserved for the Apple Music 13 deck. SF Pro itself cannot be
 * bundled with Derbis, so this expands the existing open Inter family through its heavier 800/900
 * weights while preserving the compact, neutral system-sans character of that treatment.
 */
internal val AppleMusic13SfProFallbackFont: FontFamily = FontFamily(
    listOf(
        FontWeight.Normal,
        FontWeight.Medium,
        FontWeight.SemiBold,
        FontWeight.Bold,
        FontWeight.ExtraBold,
        FontWeight.Black
    ).map { weight ->
        Font(googleFont = GoogleFont("Inter"), fontProvider = googleFontsProvider, weight = weight)
    }
)

private val defaultTypography = Typography()

private val DerbisLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private val DerbisPlatformTextStyle = PlatformTextStyle(includeFontPadding = false)

private fun TextStyle.derbisRhythm(
    fontFamily: FontFamily,
    fontWeight: FontWeight? = null,
    letterSpacing: androidx.compose.ui.unit.TextUnit = this.letterSpacing,
    retuneLineHeight: Boolean = true
): TextStyle = copy(
    fontFamily = fontFamily,
    fontWeight = fontWeight ?: this.fontWeight,
    letterSpacing = letterSpacing,
    lineHeight = if (retuneLineHeight && fontSize.isSp) {
        DerbisTypeRhythm.lineHeight(fontSize)
    } else {
        lineHeight
    },
    lineHeightStyle = DerbisLineHeightStyle,
    platformStyle = DerbisPlatformTextStyle
)

internal fun typographyForFamily(fontFamily: FontFamily): Typography = Typography(
    displayLarge = defaultTypography.displayLarge.derbisRhythm(fontFamily, FontWeight.Bold, (-0.25).sp, retuneLineHeight = false),
    displayMedium = defaultTypography.displayMedium.derbisRhythm(fontFamily, FontWeight.Bold, (-0.2).sp, retuneLineHeight = false),
    displaySmall = defaultTypography.displaySmall.derbisRhythm(fontFamily, FontWeight.Bold, (-0.15).sp, retuneLineHeight = false),
    headlineLarge = defaultTypography.headlineLarge.derbisRhythm(fontFamily, FontWeight.Bold, (-0.15).sp, retuneLineHeight = false),
    headlineMedium = defaultTypography.headlineMedium.derbisRhythm(fontFamily, FontWeight.Bold, (-0.1).sp, retuneLineHeight = false),
    headlineSmall = defaultTypography.headlineSmall.derbisRhythm(fontFamily, FontWeight.SemiBold, 0.sp, retuneLineHeight = false),
    titleLarge = defaultTypography.titleLarge.derbisRhythm(fontFamily, FontWeight.SemiBold, 0.sp),
    titleMedium = defaultTypography.titleMedium.derbisRhythm(fontFamily, FontWeight.SemiBold),
    titleSmall = defaultTypography.titleSmall.derbisRhythm(fontFamily, FontWeight.SemiBold),
    bodyLarge = defaultTypography.bodyLarge.derbisRhythm(fontFamily),
    bodyMedium = defaultTypography.bodyMedium.derbisRhythm(fontFamily),
    bodySmall = defaultTypography.bodySmall.derbisRhythm(fontFamily),
    labelLarge = defaultTypography.labelLarge.derbisRhythm(fontFamily),
    labelMedium = defaultTypography.labelMedium.derbisRhythm(fontFamily),
    labelSmall = defaultTypography.labelSmall.derbisRhythm(fontFamily)
)

private val typographyByPreset: Map<DerbisFontPreset, Typography> = mapOf(
    DerbisFontPreset.Outfit to typographyForFamily(OutfitFont),
    DerbisFontPreset.Inter to typographyForFamily(InterFont),
    DerbisFontPreset.Manrope to typographyForFamily(ManropeFont),
    DerbisFontPreset.Poppins to typographyForFamily(PoppinsFont),
    DerbisFontPreset.Montserrat to typographyForFamily(MontserratFont),
    DerbisFontPreset.Nunito to typographyForFamily(NunitoFont),
    DerbisFontPreset.Roboto to typographyForFamily(RobotoFont),
    DerbisFontPreset.System to typographyForFamily(FontFamily.Default)
)

val DerbisTypography: Typography = typographyByPreset.getValue(DerbisFontPreset.Outfit)

internal fun derbisTypographyFor(preset: DerbisFontPreset): Typography =
    typographyByPreset[preset] ?: DerbisTypography

internal fun derbisFontFamilyFor(preset: DerbisFontPreset): FontFamily = when (preset) {
    DerbisFontPreset.Outfit -> OutfitFont
    DerbisFontPreset.Inter -> InterFont
    DerbisFontPreset.Manrope -> ManropeFont
    DerbisFontPreset.Poppins -> PoppinsFont
    DerbisFontPreset.Montserrat -> MontserratFont
    DerbisFontPreset.Nunito -> NunitoFont
    DerbisFontPreset.Roboto -> RobotoFont
    DerbisFontPreset.System -> FontFamily.Default
}
