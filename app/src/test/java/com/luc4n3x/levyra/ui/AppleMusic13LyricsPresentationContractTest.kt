package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleMusic13LyricsPresentationContractTest {

    private val source = Files.readString(sourceFile()).replace("\r\n", "\n")
    private val lyricsScreen = functionBody("private fun AppleMusic13FullscreenLyrics(")

    @Test
    fun `full screen lyrics keep the title singer right tools and transport visible`() {
        assertTrue(lyricsScreen.contains("text = track?.title ?: strings.lyrics"))
        assertTrue(lyricsScreen.contains("text = track?.artist.orEmpty()"))
        assertTrue(lyricsScreen.contains("visible = showCluster && !imageShareSelectionMode"))
        assertTrue(lyricsScreen.contains("AppleMusic13LyricsFloatingButton("))
        assertFalse(lyricsScreen.contains("showPlaybackChrome"))
    }

    @Test
    fun `only the current synced lyric is sharp and white`() {
        assertTrue(lyricsScreen.contains("val lyricLineBlur = if (!imageShareSelectionMode && !liveLyricActive) 2.dp else 0.dp"))
        assertTrue(lyricsScreen.contains(".blur(lyricLineBlur)"))
        assertTrue(lyricsScreen.contains("imageSelectionActive -> Color(0xFF102026)"))
        assertTrue(lyricsScreen.contains("liveLyricActive -> Color.White"))
        assertFalse(lyricsScreen.contains("AppleMusic13WordByWordLyrics"))
    }

    @Test
    fun `focus mode retains a compact seek strip and timing action`() {
        assertTrue(lyricsScreen.contains("visible = !showCluster && !imageShareSelectionMode"))
        assertTrue(lyricsScreen.contains("formatSeekbarMillis(state.durationMs)"))
        assertTrue(lyricsScreen.contains("offset(y = (-56).dp)"))
    }

    @Test
    fun `image lyric selection is a dedicated static line picker`() {
        assertTrue(lyricsScreen.contains("ImageLyricsSelectionHeader("))
        assertTrue(lyricsScreen.contains("Color(0xFF0087A6)"))
        assertTrue(lyricsScreen.contains("text = strings.shareLyricsContinue"))
        assertTrue(lyricsScreen.contains("val liveLyricActive = !imageShareSelectionMode && index == activeIndex"))
        assertTrue(lyricsScreen.contains("Full lines, not individual words, follow the sync clock"))
        assertFalse(lyricsScreen.contains("AppleMusic13WordByWordLyrics"))
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
