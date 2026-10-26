package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleMusic13LyricsFocusContractTest {

    private val source = Files.readString(sourceFile()).replace("\r\n", "\n")
    private val lyricsScreen = functionBody("private fun AppleMusic13FullscreenLyrics(")

    @Test
    fun `full screen lyrics hide the track header and playback control panel`() {
        assertFalse(lyricsScreen.contains("Same compact record header as SimpMusic's lyrics body"))
        assertTrue(lyricsScreen.contains("val showPlaybackChrome = false"))
        assertTrue(lyricsScreen.contains("visible = showPlaybackChrome && showCluster && !imageShareSelectionMode"))
    }

    @Test
    fun `image lyric selection is static rather than another live lyrics renderer`() {
        assertTrue(lyricsScreen.contains("val liveLyricActive = !imageShareSelectionMode && index == activeIndex"))
        assertTrue(lyricsScreen.contains("if (!imageShareSelectionMode && state.lyricsSynced && line.words.isNotEmpty())"))
    }

    private fun functionBody(signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Missing $signature", start >= 0)
        return source.substring(start, source.indexOf("\nprivate fun AppleMusic13LyricsFloatingButton(", start))
    }

    private fun sourceFile(): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"),
        Path.of("src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
    ).firstOrNull(Files::exists) ?: error("LevyraApp.kt not found")
}
