package com.yasuda.derbis.ui.player

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleMusic13PlayerPresentationContractTest {

    private val playerSource = readSource("AppleMusicPlayer.kt")
    private val auraSource = readSource("PlayerDeckAura.kt")
    private val nowPlayingSource = readSource("DerbisNowPlaying.kt")

    @Test
    fun `portrait deck uses a static stretched cover and independent action row`() {
        assertTrue(playerSource.contains("private fun AppleMusic13StretchedArtwork"))
        assertTrue(playerSource.contains(".fillMaxHeight(AppleMusic13StretchedArtworkHeight)"))
        assertTrue(playerSource.contains("private fun AppleMusic13ActionDock"))
        assertFalse(playerSource.contains("AppleMusic13SourceActionDock"))
    }

    @Test
    fun `Velvet receives the contained sleeve with an opt in Canvas loop`() {
        assertTrue(auraSource.contains("private fun AuraVelvetSleeve"))
        assertTrue(auraSource.contains("VelvetSleeveWidth"))
        assertTrue(auraSource.contains("MotionArtworkLayer("))
        assertTrue(auraSource.contains("enabled = motionEnabled"))
        assertTrue(auraSource.contains("staticArtworkMotionEnabled = false"))
    }

    @Test
    fun `Apple stretched presentation uses Canvas only when explicitly enabled`() {
        assertTrue(playerSource.contains("private fun AppleMusic13StretchedArtwork"))
        assertTrue(playerSource.contains("enabled = canvas?.enabled == true"))
        assertTrue(playerSource.contains("staticArtworkMotionEnabled = false"))
    }

    @Test
    fun `play pause never changes Apple Music 13 artwork geometry`() {
        assertTrue(playerSource.contains("val artElevation by animateDpAsState("))
        assertTrue(playerSource.contains("targetValue = 18.dp"))
        assertFalse(playerSource.contains("PausedArtworkScale"))
        assertFalse(playerSource.contains("if (playing) 1f else"))
    }

    @Test
    fun `deck receives Apple Music seek and transport controls`() {
        assertTrue(nowPlayingSource.contains("} else if (appleMusicPlayer) {\n                AppleMusicSeekbar("))
        assertTrue(nowPlayingSource.contains("} else if (appleMusicPlayer) {\n                AppleMusicTransport("))
    }

    private fun readSource(name: String): String = sequenceOf(
        Path.of("app/src/main/java/com/yasuda/derbis/ui/player", name),
        Path.of("src/main/java/com/yasuda/derbis/ui/player", name)
    ).firstOrNull(Files::exists)?.let(Files::readString)
        ?: error("$name not found")
}
