package com.luc4n3x.derbis.viewmodel

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessListeningResolvedSeedContractTest {

    private val source = Files.readString(sourceFile()).replace("\r\n", "\n")

    @Test
    fun `resolving a single-track seed restarts Endless Listening from the stable queue entry`() {
        val playback = functionBody("private fun startPlayback(")
        val queueUpdate = playback.indexOf("queueEngine.updateTrackAt(selectedIndex, resolved)")
        val restart = playback.indexOf("if (queueEngine.state.value.radioEnabled) {")

        assertTrue(queueUpdate >= 0)
        assertTrue(restart > queueUpdate)
        assertTrue(playback.contains("radioJob?.cancel()"))
        assertTrue(playback.contains("beginRadioSession()"))
        assertTrue(playback.contains("ensureRadioTail(force = true)"))
    }

    private fun functionBody(signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Missing $signature", start >= 0)
        val rest = source.substring(start + signature.length)
        val end = Regex("""\n    private fun """).find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }

    private fun sourceFile(): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/derbis/viewmodel/DerbisViewModel.kt"),
        Path.of("src/main/java/com/luc4n3x/derbis/viewmodel/DerbisViewModel.kt")
    ).firstOrNull(Files::exists) ?: error("DerbisViewModel.kt not found")
}
