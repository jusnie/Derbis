package com.yasuda.derbis.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordAuthorizationCopyTest {
    @Test
    fun supportedAndFallbackAuthorizationCopyIsAvailableWithoutTokenInstructions() {
        for (code in listOf("en", "it", "fil", "tl-PH", "fr", "unknown")) {
            val copy = discordAuthorizationCopy(code)
            val labels = listOf(copy.connect, copy.authorize, copy.unavailable, copy.connecting,
                copy.continueAuthorization, copy.reconnect, copy.failed, copy.presence,
                copy.presenceHint, copy.disconnect, copy.disconnectHint)
            assertTrue(code, labels.all { it.isNotBlank() })
            assertFalse(labels.joinToString(" ").contains("user token", ignoreCase = true))
        }
    }

    @Test
    fun localLanguageAndFallbackSelectionStayDeterministic() {
        assertEquals(discordAuthorizationCopy("fil"), discordAuthorizationCopy("tl-PH"))
        assertEquals(discordAuthorizationCopy("it"), discordAuthorizationCopy("IT"))
        assertEquals(discordAuthorizationCopy("en"), discordAuthorizationCopy("unknown"))
    }
}
