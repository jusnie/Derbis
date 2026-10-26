package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticArtworkPresentationContractTest {

    @Test
    fun `motion artwork layer always renders its supplied static cover`() {
        val layer = source("ui/MotionArtworkLayer.kt")

        assertTrue(layer.contains("private val staticArtworkOnly = true"))
        assertTrue(layer.contains("if (staticArtworkOnly)"))
        assertTrue(layer.contains("onDynamicBackdropPalette(null)"))
        assertTrue(layer.contains("Box(modifier = modifier) { staticArtwork() }"))
        assertFalse(layer.contains("LivingArtworkLayer("))
    }

    @Test
    fun `full player artwork has no palette ink glow or coloured shadow`() {
        val hero = source("ui/player/PlayerArtworkHero.kt")

        assertFalse(hero.contains("drawBehind"))
        assertFalse(hero.contains("glowColor"))
        assertTrue(hero.contains("ambientColor = Color.Black.copy(alpha = 0.46f)"))
    }

    @Test
    fun `saved motion preference cannot bring back video artwork or its controls`() {
        val viewModel = source("viewmodel/LevyraViewModel.kt")
        val app = source("ui/LevyraApp.kt")

        assertTrue(viewModel.contains("motionArtworkEnabled = false,"))
        assertTrue(viewModel.contains("preferences.setMotionArtworkEnabled(false)"))
        assertFalse(app.contains("onMotionArtwork"))
        assertFalse(app.contains("strings.motionArtwork"))
    }

    private fun source(relativePath: String): String = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
    ).firstOrNull(Files::exists)?.let(Files::readString)?.replace("\r\n", "\n")
        ?: error("$relativePath not found")
}
