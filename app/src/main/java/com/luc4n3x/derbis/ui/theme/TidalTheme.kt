package com.luc4n3x.derbis.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color

/** A local, TIDAL-inspired deck: no TIDAL account, catalog, or artwork is used. */
internal const val TidalPaletteId = "derbis_tidal_style"
val DerbisIsTidal: Boolean get() = DerbisActivePalette.id == TidalPaletteId

internal object TidalDesign {
    val black = Color(0xFF050505)
    val surface = Color(0xFF161616)
    val elevated = Color(0xFF242424)
    val white = Color(0xFFF7F7F7)
    val muted = Color(0xFFA7A7A7)
    val accent = Color(0xFF00FFFF)
}

internal fun tidalPalette(): DerbisPalette = DerbisThemes.cosmic.copy(
    id = TidalPaletteId,
    label = "TIDAL Style",
    emoji = "◆",
    isLight = false,
    followsCover = false,
    followsMood = false,
    black = TidalDesign.black,
    ink = TidalDesign.surface,
    panel = TidalDesign.elevated,
    panelSoft = Color(0xFF303030),
    cyan = TidalDesign.accent,
    blue = TidalDesign.accent,
    violet = TidalDesign.accent,
    pink = TidalDesign.accent,
    orange = TidalDesign.accent,
    text = TidalDesign.white,
    muted = TidalDesign.muted,
    outline = Color(0xFF424242)
)

@Composable
internal fun TidalThemeEffect() {
    SideEffect { DerbisThemeController.applyPrism(tidalPalette()) }
}
