package com.yasuda.derbis.feature.jam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTogetherLaunchTest {

    @Test
    fun listenTogetherWebAndDeepLinkUrlCodeExtraction() {
        val originalCode = JamSessionCode("192.168.1.100", 50_000, "0123456789abcdef0123456789abcdef")
        val encodedCode = originalCode.encoded()

        // 1. Deep link format: derbis://jam/<code
        val deepLinkUri = "derbis://jam/$encodedCode"
        val parsedFromDeepLink = JamSessionCode.parse(deepLinkUri)
        assertNotNull(parsedFromDeepLink)
        assertEquals(originalCode, parsedFromDeepLink)

        // 2. Web URL format: https://derbis-web.vercel.app/jam/<code
        val webPath = "/jam/$encodedCode"
        val codeFromWeb = webPath.removePrefix("/jam/").trim()
        val parsedFromWeb = JamSessionCode.parse("derbis://jam/$codeFromWeb")
        assertNotNull(parsedFromWeb)
        assertEquals(originalCode, parsedFromWeb)
    }

    @Test
    fun listenTogetherInvitePayloadFormatting() {
        val code = JamSessionCode("10.0.0.5", 48_000, "abcdef0123456789abcdef0123456789")
        val title = "Starboy"
        val artist = "The Weeknd"

        val inviteText = "🎧 Listen Together on Derbis Jam!\n" +
            "🎶 $title — $artist\n" +
            "Session Code: ${code.formatted()}\n" +
            "Join: https://derbis-web.vercel.app/jam/${code.formatted()}"

        assertTrue(inviteText.contains("Listen Together on Derbis Jam"))
        assertTrue(inviteText.contains(title))
        assertTrue(inviteText.contains(artist))
        assertTrue(inviteText.contains("https://derbis-web.vercel.app/jam/"))
    }
}
