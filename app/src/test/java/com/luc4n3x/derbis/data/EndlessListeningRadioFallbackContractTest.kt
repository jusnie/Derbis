package com.luc4n3x.derbis.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessListeningRadioFallbackContractTest {

    private val source = Files.readString(sourceFile()).replace("\r\n", "\n")

    @Test
    fun `radio search remains available before a Quick Pick resolves a YouTube identity`() {
        val radio = functionBody("suspend fun radio(")

        assertTrue(radio.contains("if (videoId.isNotBlank()) {"))
        assertTrue(radio.contains("appendSearchRadioCandidates(seed, languageCode, boundedLimit, diversify, candidates)"))
        assertFalse(radio.contains("if (videoId.isBlank()) return@withContext emptyList()"))
    }

    private fun functionBody(signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Missing $signature", start >= 0)
        val rest = source.substring(start + signature.length)
        val end = Regex("""\n    (?:open )?(?:suspend )?fun """).find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }

    private fun sourceFile(): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/derbis/data/YoutubeMusicRepository.kt"),
        Path.of("src/main/java/com/luc4n3x/derbis/data/YoutubeMusicRepository.kt")
    ).firstOrNull(Files::exists) ?: error("YoutubeMusicRepository.kt not found")
}
