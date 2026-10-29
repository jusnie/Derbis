package com.yasuda.derbis.viewmodel

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessListeningMobileNetworkContractTest {

    private val source = Files.readString(sourceFile()).replace("\r\n", "\n")

    @Test
    fun `user enabled Endless Listening is not blocked by the lyrics data saver profile`() {
        val request = functionBody("private fun radioTailRequest(")

        assertTrue(request.contains("!hasInternetCapableNetwork()"))
        assertFalse(request.contains("!lyricsNetworkProfile().connected"))
    }

    @Test
    fun `prefetch restrictions still allow an Endless queue request`() {
        val prefetch = functionBody("private fun prefetchNextTracks(")
        val restricted = prefetch.indexOf("if (!lyricsNetworkProfile().connected)")
        val radioTail = prefetch.indexOf("ensureRadioTail(force = false)", restricted)

        assertTrue(restricted >= 0)
        assertTrue(radioTail > restricted)
    }

    private fun functionBody(signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Missing $signature", start >= 0)
        val rest = source.substring(start + signature.length)
        val end = Regex("""\n    (?:private |internal |override )?(?:suspend )?fun """)
            .find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }

    private fun sourceFile(): Path = sequenceOf(
        Path.of("app/src/main/java/com/yasuda/derbis/viewmodel/DerbisViewModel.kt"),
        Path.of("src/main/java/com/yasuda/derbis/viewmodel/DerbisViewModel.kt")
    ).firstOrNull(Files::exists) ?: error("DerbisViewModel.kt not found")
}
