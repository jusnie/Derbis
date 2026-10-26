package com.luc4n3x.derbis.data.network

import com.luc4n3x.derbis.data.hqaudio.ProviderDestinationPolicy
import com.luc4n3x.derbis.data.network.byedpi.ByeDpiSocketFactory
import com.luc4n3x.derbis.data.network.byedpi.ByeDpiSupervisor
import com.luc4n3x.derbis.domain.DerbisNetworkSettings
import com.luc4n3x.derbis.domain.DerbisProxyMode
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JioSaavnNetworkIsolationTest {

    @Before
    @After
    fun resetEnvironment() {
        ByeDpiSupervisor.resetForTesting()
        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(), "")
    }

    @Test
    fun jioSaavnDomainsAreNeverClassifiedAsYoutube() {
        val jioHosts = listOf(
            "www.jiosaavn.com",
            "jiosaavn.com",
            "api.jiosaavn.com",
            "aac.saavncdn.com",
            "c.saavncdn.com"
        )

        for (host in jioHosts) {
            assertFalse("JioSaavn host must not be classified as YouTube: $host", YoutubeNetworkPolicy.isYoutubeHost(host))
            assertFalse("JioSaavn host must not be classified as YouTube media: $host", YoutubeNetworkPolicy.isYoutubeMediaHost(host))
            assertTrue("Allowed by ProviderDestinationPolicy: $host", ProviderDestinationPolicy.allows("https://$host/test".toHttpUrl()))
        }
    }

    @Test
    fun jioSaavnTrafficIsNeverRoutedThroughByeDpiEvenWhenByeDpiIsRunning() {
        val byeDpiPort = 1088
        ByeDpiSupervisor.setRunningForTesting(byeDpiPort)
        val settings = DerbisNetworkSettings(
            byeDpiEnabled = true,
            youtubeRegionProfileEnabled = true
        )
        DerbisNetworkConfiguration.apply(settings, "")

        val jioApiUri = URI("https://www.jiosaavn.com/api.php?__call=autocomplete.get&query=test")
        val jioCdnUri = URI("https://aac.saavncdn.com/123/sample_320.mp4")

        val apiProxies = YoutubeNetworkPolicy.selectProxies(jioApiUri, settings)
        assertEquals(listOf(Proxy.NO_PROXY), apiProxies)

        val cdnProxies = YoutubeNetworkPolicy.selectProxies(jioCdnUri, settings)
        assertEquals(listOf(Proxy.NO_PROXY), cdnProxies)

        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(jioApiUri.host, settings, streamBypass = false))
        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(jioCdnUri.host, settings, streamBypass = true))
        assertTrue(YoutubeNetworkPolicy.routesThroughByeDpi("rr1---sn-4g5ednls.googlevideo.com", settings, streamBypass = false))
    }

    @Test
    fun externalIntegrationsClientDoesNotRouteThroughByeDpi() {
        val byeDpiPort = 1088
        ByeDpiSupervisor.setRunningForTesting(byeDpiPort)
        DerbisNetworkConfiguration.apply(
            DerbisNetworkSettings(byeDpiEnabled = true, youtubeRegionProfileEnabled = true),
            ""
        )

        val externalClient = DerbisHttpClientFactory.externalIntegrations()
        val configuredProxy = externalClient.proxy

        if (configuredProxy != null) {
            assertNotEquals(Proxy.Type.SOCKS, configuredProxy.type())
        }
        assertFalse(externalClient.socketFactory is ByeDpiSocketFactory)
    }

    @Test
    fun jioSaavnUnderUserProxyWithStreamBypassRemainsDirectAndBypassesByeDpi() {
        val externalHost = "10.0.0.50"
        val externalPort = 3128
        val settings = DerbisNetworkSettings(
            proxyMode = DerbisProxyMode.Http,
            proxyHost = externalHost,
            proxyPort = externalPort,
            bypassProxyForStreams = true,
            byeDpiEnabled = true,
            youtubeRegionProfileEnabled = true
        )
        DerbisNetworkConfiguration.apply(settings, "")
        ByeDpiSupervisor.setRunningForTesting(1088)

        val jioCdnUri = URI("https://aac.saavncdn.com/999/audio.mp4")

        assertFalse(YoutubeNetworkPolicy.routesThroughByeDpi(jioCdnUri.host, settings, streamBypass = true))
        val apiProxies = YoutubeNetworkPolicy.selectProxies(jioCdnUri, settings)
        assertEquals(Proxy.Type.HTTP, apiProxies.single().type())
        assertEquals(externalHost, (apiProxies.single().address() as InetSocketAddress).hostString)
    }
}
