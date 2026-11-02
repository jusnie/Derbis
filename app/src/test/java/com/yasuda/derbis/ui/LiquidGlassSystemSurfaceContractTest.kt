package com.yasuda.derbis.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassSystemSurfaceContractTest {

    @Test
    fun `Liquid Glass skin activates the shared backdrop with no separate toggle`() {
        val app = source("ui/DerbisApp.kt")

        assertTrue(app.contains("enabled = DerbisIsLiquidGlass &&"))
        assertTrue(app.contains("Modifier.layerBackdrop(appBackdrop)"))
        // Every glass surface samples this one layer, and the page recorded into it must never
        // be handed the same layer back: drawing a GraphicsLayer while it is being recorded
        // throws, which is what crashed the skin on selection.
        assertTrue(app.contains("CompositionLocalProvider(LocalDerbisAppBackdrop provides null)"))
        assertFalse(app.contains("LocalDerbisAppBackdrop provides appBackdrop"))
        // The old RenderEffect frost is gone; chrome draws the backdrop glass instead.
        assertFalse(app.contains("glassBackdropSource("))
        assertFalse(app.contains("glassFrost("))
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
