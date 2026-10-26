package com.luc4n3x.derbis.viewmodel

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplicitTrackStartContractTest {

    private val source = Files.readString(sourceFile()).replace("\r\n", "\n")

    @Test
    fun `a direct track selection starts at zero rather than the saved resume cursor`() {
        assertEquals(0L, explicitTrackStartPositionMs())

        val reset = functionBody("private fun restartExplicitTrackAtBeginning(")
        assertTrue(reset.contains("pendingSeekMs = startPositionMs"))
        assertTrue(reset.contains("queueEngine.updatePosition(startPositionMs)"))
    }

    @Test
    fun `all normal user selection routes reset before resolving`() {
        listOf(
            "fun playAll(",
            "fun playQueueTrack(",
            "fun play(track:",
            "fun playFrom(",
            "fun playDownloaded("
        ).forEach { signature ->
            assertTrue("$signature must reset a direct selection", functionBody(signature).contains("restartExplicitTrackAtBeginning()"))
        }
    }

    private fun functionBody(signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Missing $signature", start >= 0)
        val rest = source.substring(start + signature.length)
        val end = Regex("""\n    (?:private |internal )?(?:suspend )?fun """)
            .find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }

    private fun sourceFile(): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/derbis/viewmodel/DerbisViewModel.kt"),
        Path.of("src/main/java/com/luc4n3x/derbis/viewmodel/DerbisViewModel.kt")
    ).firstOrNull(Files::exists) ?: error("DerbisViewModel.kt not found")
}
