package com.luc4n3x.levyra.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentRadioWatchQueueContractTest {

    @Test
    fun `provider radio uses the next queue endpoint instead of a browse playlist shell`() {
        val repository = source("YoutubeMusicRepository.kt")
        val radioPage = functionBody(repository, "suspend fun radioPage(")
        val continuationPage = functionBody(repository, "suspend fun radioContinuationPage(")

        assertTrue(radioPage.contains("watchRepository.getRadioPage("))
        assertTrue(continuationPage.contains("watchRepository.getRadioPage("))
        assertTrue(radioPage.contains("radioId = requestedRadioId"))
        assertTrue(continuationPage.contains("radioId = cleanRadioId"))
        assertFalse(radioPage.contains("requestMusicBrowseRoot("))
        assertFalse(continuationPage.contains("requestMusicBrowseRoot("))
    }

    @Test
    fun `radio page follows the provider automix handoff and preserves its returned continuation`() {
        val watchRepository = source("YoutubeMusicWatchRepository.kt")
        val radioPage = functionBody(watchRepository, "suspend fun getRadioPage(")

        assertTrue(radioPage.contains("endpoint = \"next\""))
        assertTrue(radioPage.contains("extractAutomixQueueEndpoint(initialRoot)"))
        assertTrue(radioPage.contains("continuation = requestedContinuation"))
        assertTrue(watchRepository.contains("fun extractAutomixQueueEndpoint(root: JSONObject)"))
    }

    private fun functionBody(source: String, signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Missing $signature", start >= 0)
        val rest = source.substring(start + signature.length)
        val end = Regex("""\n    (?:private |internal |open )?(?:suspend )?fun """)
            .find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }

    private fun source(fileName: String): String = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/data/$fileName"),
        Path.of("src/main/java/com/luc4n3x/levyra/data/$fileName")
    ).firstOrNull(Files::exists)?.let(Files::readString)?.replace("\r\n", "\n")
        ?: error("$fileName not found")
}
