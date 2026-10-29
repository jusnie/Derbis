package com.yasuda.derbis.ui.lyrics

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsShareReferenceComposerContractTest {

    private val source = Files.readString(sourceFile()).replace("\r\n", "\n")

    @Test
    fun `composer uses a dedicated share sheet with selectable background swatches`() {
        assertTrue(source.contains("containerColor = Color(0xFF00313A)"))
        assertTrue(source.contains("ReferenceShareBackgrounds.forEachIndexed"))
        assertTrue(source.contains("selectedBackgroundIndex = index"))
        assertTrue(source.contains("LyricsSharePreviewKey(content, style, cardAccents ?: (0 to 0))"))
    }

    @Test
    fun `composer provides save and Android share actions side by side`() {
        assertTrue(source.contains("onClick = { saveRequest += 1 }"))
        assertTrue(source.contains("onClick = { exportRequest += 1 }"))
        assertTrue(source.contains("text = if (saved) strings.saved else strings.save"))
        assertTrue(source.contains("text = strings.share"))
    }

    private fun sourceFile(): Path = sequenceOf(
        Path.of("app/src/main/java/com/yasuda/derbis/ui/lyrics/LyricsShareSheet.kt"),
        Path.of("src/main/java/com/yasuda/derbis/ui/lyrics/LyricsShareSheet.kt")
    ).firstOrNull(Files::exists) ?: error("LyricsShareSheet.kt not found")
}
