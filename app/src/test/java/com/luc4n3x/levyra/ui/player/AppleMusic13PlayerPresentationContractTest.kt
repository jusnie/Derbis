package com.luc4n3x.levyra.ui.player

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleMusic13PlayerPresentationContractTest {

    private val playerSource = readSource("AppleMusicPlayer.kt")
    private val nowPlayingSource = readSource("LevyraNowPlaying.kt")

    @Test
    fun `portrait deck uses a distinct elevated sleeve and independent action row`() {
        assertTrue(playerSource.contains("private val AppleMusic13ArtworkCorner = 24.dp"))
        assertTrue(playerSource.contains(".padding(top = 60.dp)"))
        assertTrue(playerSource.contains("private fun AppleMusic13ActionDock"))
        assertFalse(playerSource.contains("AppleMusic13SourceActionDock"))
        assertFalse(playerSource.contains("SimpMusic"))
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
        Path.of("app/src/main/java/com/luc4n3x/levyra/ui/player", name),
        Path.of("src/main/java/com/luc4n3x/levyra/ui/player", name)
    ).firstOrNull(Files::exists)?.let(Files::readString)
        ?: error("$name not found")
}
