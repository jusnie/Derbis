package com.yasuda.derbis.ui.spotify

import android.webkit.WebViewClient
import com.yasuda.derbis.data.spotify.SpotifyCookie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyBrowserLoginTest {

    @Test
    fun webPlayerHostDetectionCoversOpenAndPlayDomainsOnly() {
        assertTrue(isSpotifyWebPlayerHost("open.spotify.com"))
        assertTrue(isSpotifyWebPlayerHost("PLAY.SPOTIFY.COM"))
        assertFalse(isSpotifyWebPlayerHost("accounts.spotify.com"))
        assertFalse(isSpotifyWebPlayerHost(null))
    }

    @Test
    fun googleAuthHostDetectionFlagsEmbeddedOAuthHostname() {
        assertTrue(isGoogleAuthHost("accounts.google.com"))
        assertTrue(isGoogleAuthHost("my.accounts.google.com"))
        assertFalse(isGoogleAuthHost("accounts.spotify.com"))
        assertFalse(isGoogleAuthHost("google.com"))
    }

    @Test
    fun postLoginStatusUrlRecognizesSpotifyAccountStatusTarget() {
        assertTrue(isSpotifyPostLoginStatusUrl("https://accounts.spotify.com/en/status"))
        assertTrue(isSpotifyPostLoginStatusUrl("https://accounts.spotify.com/status?flow_ctx=123"))
        assertTrue(isSpotifyPostLoginStatusUrl("https://accounts.spotify.com/en/account/overview/"))
        assertFalse(isSpotifyPostLoginStatusUrl("https://accounts.spotify.com/en/login"))
        assertFalse(isSpotifyPostLoginStatusUrl("https://open.spotify.com/"))
    }

    @Test
    fun webViewErrorFilterIgnoresAbortsAndSubresources() {
        // Subresource errors must never fail the login sheet.
        assertFalse(
            shouldTreatWebViewErrorAsFatal(
                isForMainFrame = false,
                errorCode = WebViewClient.ERROR_HOST_LOOKUP,
                description = "net::ERR_NAME_NOT_RESOLVED",
                suppressedAbort = false
            )
        )
        // Aborted navigations caused by stopLoading() on open.spotify.com redirect are benign.
        assertFalse(
            shouldTreatWebViewErrorAsFatal(
                isForMainFrame = true,
                errorCode = WebViewClient.ERROR_UNKNOWN,
                description = "net::ERR_ABORTED",
                suppressedAbort = false
            )
        )
        assertFalse(
            shouldTreatWebViewErrorAsFatal(
                isForMainFrame = true,
                errorCode = WebViewClient.ERROR_CONNECT,
                description = "net::ERR_CONNECTION_ABORTED",
                suppressedAbort = true
            )
        )
        // Genuine main-frame DNS/connectivity failures are reported.
        assertTrue(
            shouldTreatWebViewErrorAsFatal(
                isForMainFrame = true,
                errorCode = WebViewClient.ERROR_HOST_LOOKUP,
                description = "net::ERR_INTERNET_DISCONNECTED",
                suppressedAbort = false
            )
        )
    }

    @Test
    fun spotifyCookieParserExtractsSpDcFromFullBrowserCookieJarHeader() {
        val sampleSpDc = "AQB" + "x9K2mPq7LvRt5Wz8Yn4Jc1Hd6Fs3Ga0Ue".repeat(2)
        val header = "sp_landing=https%3A%2F%2Fopen.spotify.com; sp_t=99f001; sp_dc=$sampleSpDc; sp_key=00a11b"
        assertEquals(sampleSpDc, SpotifyCookie.parse(header))
        assertNull(SpotifyCookie.parse("sp_t=99f001; sp_landing=1"))
    }
}
