package com.yasuda.derbis.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassSystemSurfaceContractTest {

    @Test
    fun `Liquid Glass skin activates a shared backdrop even without a separate toggle`() {
        val app = source("ui/DerbisApp.kt")

        assertTrue(app.contains("enabled = (state.interfaceSettings.liquidGlassEnabled ||"))
        assertTrue(app.contains("state.interfaceSettings.skin == DerbisSkin.LiquidGlass)"))
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
        val app = source("ui/DerbisApp.kt")
        val artists = source("ui/SearchArtistSuggestions.kt")
        val fresh = source("ui/ExploreFreshCurrents.kt")
        val navigation = source("ui/PrismNavigation.kt")

        assertTrue(app.contains("private fun SettingsTile("))
        assertTrue(app.contains("val liquidGlass = DerbisIsLiquidGlass"))
        assertTrue(app.contains("private fun SearchHeader("))
        assertTrue(app.contains("val liquidGlassField = DerbisIsLiquidGlass"))
        assertTrue(app.contains("private fun DerbisExploreShortcutTile("))
        assertTrue(app.contains("val tileSurface = if (DerbisIsLiquidGlass)"))
        assertTrue(app.contains("private fun LibraryEmptyState("))
        assertTrue(app.contains("Modifier.derbisGlass("))
        assertTrue(navigation.contains("val liquidGlass = DerbisIsLiquidGlass"))
        assertTrue(navigation.contains("val dockModifier = if (liquidGlass)"))
        assertTrue(fresh.contains("val rowSurface = if (DerbisIsLiquidGlass)"))
        assertTrue(artists.contains("val rowSurface = if (DerbisIsLiquidGlass)"))
        assertTrue(artists.contains("Modifier.derbisGlass("))
    }

    private fun source(relativePath: String): String = sequenceOf(
        Path.of("app/src/main/java/com/yasuda/derbis/$relativePath"),
        Path.of("src/main/java/com/yasuda/derbis/$relativePath")
    ).firstOrNull(Files::exists)?.let(Files::readString)?.replace("\r\n", "\n")
        ?: error("$relativePath not found")
}
