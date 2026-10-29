package com.yasuda.derbis.ui.player

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerArtworkPauseLayoutContractTest {

    private val source = Files.readString(sourceFile()).replace("\r\n", "\n")

    @Test
    fun `pausing retains full artwork scale and position`() {
        assertTrue(source.contains("val artRestScale = 1f"))
        assertTrue(source.contains("val artRestOffset = 0.dp"))
        assertFalse(source.contains("val artworkRestsFullBleed = nocturne || state.isPlaying"))
    }

    private fun sourceFile(): Path = sequenceOf(
        Path.of("app/src/main/java/com/yasuda/derbis/ui/player/DerbisNowPlaying.kt"),
        Path.of("src/main/java/com/yasuda/derbis/ui/player/DerbisNowPlaying.kt")
    ).firstOrNull(Files::exists) ?: error("DerbisNowPlaying.kt not found")
}
