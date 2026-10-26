package com.luc4n3x.derbis.viewmodel

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessListeningStartupContractTest {

    private val source = Files.readString(sourceFile()).replace("\r\n", "\n")

    @Test
    fun `a single track starts fetching its Endless Listening tail immediately`() {
        val play = functionBody("fun play(track: Track)")

        assertTrue(play.contains("queueEngine.replace(listOf(track), 0, keepPlaybackModes = true, radioEnabled = true)"))
        assertTrue(play.indexOf("ensureRadioTail(force = true)") > play.indexOf("beginRadioSession()"))
    }

    @Test
    fun `all one track entry points start fetching their Endless Listening tail immediately`() {
        val playFrom = functionBody("fun playFrom(")

        assertTrue(playFrom.contains("if (list.size <= 1) {"))
        assertTrue(playFrom.indexOf("ensureRadioTail(force = true)") > playFrom.indexOf("beginRadioSession()"))
    }

    private fun functionBody(signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Missing $signature", start >= 0)
        val rest = source.substring(start + signature.length)
        val end = NextMember.find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }

    private fun sourceFile(): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/derbis/viewmodel/DerbisViewModel.kt"),
        Path.of("src/main/java/com/luc4n3x/derbis/viewmodel/DerbisViewModel.kt")
    ).firstOrNull(Files::exists) ?: error("DerbisViewModel.kt not found")

    private companion object {
        val NextMember = Regex("""\n {4}(?:private |internal |override )?(?:suspend )?fun """)
    }
}
