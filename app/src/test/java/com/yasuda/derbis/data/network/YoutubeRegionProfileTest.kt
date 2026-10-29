package com.yasuda.derbis.data.network

import com.yasuda.derbis.domain.DerbisContentLocales
import com.yasuda.derbis.domain.DerbisNetworkSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YoutubeRegionProfileTest {

    @Before
    @After
    fun resetSettings() {
        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(), "")
    }

    @Test
    fun reflectsEnabledStateFromConfiguration() {
        assertFalse(YoutubeRegionProfile.isEnabled())

        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(youtubeRegionProfileEnabled = true), "")
        assertTrue(YoutubeRegionProfile.isEnabled())

        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(youtubeRegionProfileEnabled = false), "")
        assertFalse(YoutubeRegionProfile.isEnabled())
    }

    @Test
    fun effectiveLocaleResolvesCorrectly() {
        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(youtubeRegionProfileEnabled = false), "")
        val italian = YoutubeRegionProfile.effectiveLocale("it")
        val russian = YoutubeRegionProfile.effectiveLocale("ru")
        assertEquals(DerbisContentLocales.forLanguage("it"), italian)
        assertEquals(DerbisContentLocales.forLanguage("ru"), russian)

        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(youtubeRegionProfileEnabled = true), "")
        val forcedItalian = YoutubeRegionProfile.effectiveLocale("it")
        val forcedRussian = YoutubeRegionProfile.effectiveLocale("ru")
        val forcedJapanese = YoutubeRegionProfile.effectiveLocale("ja")

        assertEquals("en", forcedItalian.hl)
        assertEquals("US", forcedItalian.gl)
        assertEquals("en", forcedRussian.hl)
        assertEquals("US", forcedRussian.gl)
        assertEquals("en", forcedJapanese.hl)
        assertEquals("US", forcedJapanese.gl)
    }

    @Test
    fun effectiveAcceptLanguageResolvesCorrectly() {
        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(youtubeRegionProfileEnabled = false), "")
        assertEquals("*", YoutubeRegionProfile.effectiveAcceptLanguage("*"))
        assertEquals("it-IT,it;q=0.9", YoutubeRegionProfile.effectiveAcceptLanguage("it-IT,it;q=0.9"))

        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(youtubeRegionProfileEnabled = true), "")
        assertEquals("en-US,en;q=0.9", YoutubeRegionProfile.effectiveAcceptLanguage("*"))
        assertEquals("en-US,en;q=0.9", YoutubeRegionProfile.effectiveAcceptLanguage("ru-RU,ru;q=0.9"))
    }

    @Test
    fun interceptorAppliesUsAcceptLanguageOnlyWhenEnabledAndHostIsYoutube() {
        val ytRequest = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player")
            .header("X-Youtube-Client-Name", "1")
            .header("X-Youtube-Client-Version", "2.20260630.01.00")
            .build()

        val nonYtRequest = Request.Builder()
            .url("https://api.jiosaavn.com/test")
            .build()

        var interceptedRequest: Request? = null
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(YoutubeClientIdentityInterceptor)
            .addInterceptor { chain ->
                interceptedRequest = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(youtubeRegionProfileEnabled = false), "")
        client.newCall(ytRequest).execute().close()
        assertEquals(null, interceptedRequest?.header("Accept-Language"))

        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(youtubeRegionProfileEnabled = true), "")
        client.newCall(ytRequest).execute().close()
        assertEquals("en-US,en;q=0.9", interceptedRequest?.header("Accept-Language"))

        client.newCall(nonYtRequest).execute().close()
        assertEquals(null, interceptedRequest?.header("Accept-Language"))
    }
}
