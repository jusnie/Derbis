package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassSystemSurfaceContractTest {

    @Test
    fun `Liquid Glass skin activates a shared backdrop even without a separate toggle`() {
        val app = source("ui/LevyraApp.kt")

        assertTrue(app.contains("enabled = (state.interfaceSettings.liquidGlassEnabled ||"))
        assertTrue(app.contains("state.interfaceSettings.skin == LevyraSkin.LiquidGlass)"))
        assertTrue(app.contains("CompositionLocalProvider(LocalGlassBackdrop provides dockGlass)"))
        assertTrue(app.contains("modifier = Modifier.glassBackdropSource(dockGlass)"))
        assertFalse(app.contains(".nestedScroll(dockState)\n                    .glassBackdropSource(dockGlass)"))
    }

    @Test
    fun `Liquid Glass palette keeps common system materials translucent`() {
        val prism = source("ui/theme/PrismTheme.kt")

        assertTrue(prism.contains("val translucent = settings.translucency != PrismTranslucency.Reduced"))
        assertTrue(prism.contains("fun systemMaterial(day: Long, night: Long, alpha: Float): Color"))
        assertTrue(prism.contains("panel = systemMaterial"))
        assertTrue(prism.contains("panelSoft = systemMaterial"))
    }

    @Test
    fun `shared browse and settings surfaces use the glass primitive`() {
        val app = source("ui/LevyraApp.kt")
        val artists = source("ui/SearchArtistSuggestions.kt")
        val fresh = source("ui/ExploreFreshCurrents.kt")
        val navigation = source("ui/PrismNavigation.kt")

        assertTrue(app.contains("private fun SettingsTile("))
        assertTrue(app.contains("val liquidGlass = LevyraIsLiquidGlass"))
        assertTrue(app.contains("private fun SearchHeader("))
        assertTrue(app.contains("val liquidGlassField = LevyraIsLiquidGlass"))
        assertTrue(app.contains("private fun LevyraExploreShortcutTile("))
        assertTrue(app.contains("val tileSurface = if (LevyraIsLiquidGlass)"))
        assertTrue(app.contains("private fun LibraryEmptyState("))
        assertTrue(app.contains("Modifier.levyraGlass("))
        assertTrue(navigation.contains("val liquidGlass = LevyraIsLiquidGlass"))
        assertTrue(navigation.contains("val dockModifier = if (liquidGlass)"))
        assertTrue(fresh.contains("val rowSurface = if (LevyraIsLiquidGlass)"))
        assertTrue(artists.contains("val rowSurface = if (LevyraIsLiquidGlass)"))
        assertTrue(artists.contains("Modifier.levyraGlass("))
    }

    private fun source(relativePath: String): String = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
    ).firstOrNull(Files::exists)?.let(Files::readString)?.replace("\r\n", "\n")
        ?: error("$relativePath not found")
}
