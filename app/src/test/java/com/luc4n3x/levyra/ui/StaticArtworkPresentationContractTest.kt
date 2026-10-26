package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticArtworkPresentationContractTest {

    @Test
    fun `Canvas is opt in and unmatched tracks retain the supplied static cover`() {
        val layer = source("ui/MotionArtworkLayer.kt")

        assertFalse(layer.contains("staticArtworkOnly"))
        assertTrue(layer.contains("Canvas video is strictly opt-in"))
        assertTrue(layer.contains("enabled &&"))
        assertTrue(layer.contains("staticArtworkMotionEnabled: Boolean = false"))
        assertTrue(layer.contains("MotionArtworkStaticFallback("))
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
    fun `animated artwork preference is restored as an off by default opt in`() {
        val viewModel = source("viewmodel/LevyraViewModel.kt")
        val app = source("ui/LevyraApp.kt")

        assertTrue(viewModel.contains("motionArtworkEnabled = settings.motionArtworkEnabled"))
        assertTrue(viewModel.contains("motionArtworkEnabled = snapshot.motionArtworkEnabled"))
        assertTrue(viewModel.contains("preferences.setMotionArtworkEnabled(value)"))
        assertTrue(app.contains("onMotionArtwork = viewModel::setMotionArtworkEnabled"))
        assertTrue(app.contains("title = strings.motionArtwork"))
    }

    private fun source(relativePath: String): String = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
    ).firstOrNull(Files::exists)?.let(Files::readString)?.replace("\r\n", "\n")
        ?: error("$relativePath not found")
}
