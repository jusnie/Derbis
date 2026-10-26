package com.luc4n3x.derbis.data.network

import com.luc4n3x.derbis.domain.DerbisDnsMode
import com.luc4n3x.derbis.domain.DerbisNetworkSettings
import com.luc4n3x.derbis.domain.DerbisNetworkSettingsError
import com.luc4n3x.derbis.domain.DerbisNetworkSettingsValidator
import com.luc4n3x.derbis.domain.DerbisProxyMode
import java.net.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisNetworkSettingsTest {

    @Test
    fun defaultsUseSystemDnsAndNoProxy() {
        val settings = DerbisNetworkSettings()

        assertTrue(DerbisNetworkSettingsValidator.validate(settings, false).isEmpty())
        assertNull(DerbisDnsCatalog.endpointFor(settings))
        assertNull(DerbisNetworkConfiguration.proxyFor(settings))
    }

    @Test
    fun normalizationTrimsTextWithoutHidingInvalidPorts() {
        val normalized = DerbisNetworkSettings(
            proxyMode = DerbisProxyMode.Http,
            proxyHost = "  proxy.example.com  ",
            proxyPort = 0,
            proxyUsername = "  user  "
        ).normalized()

        assertEquals("proxy.example.com", normalized.proxyHost)
        assertEquals("user", normalized.proxyUsername)
        assertEquals(0, normalized.proxyPort)
        assertTrue(
            DerbisNetworkSettingsValidator.validate(normalized, false)
                .contains(DerbisNetworkSettingsError.ProxyPortOutOfRange)
        )
    }

    @Test
    fun malformedProxyHostsAreRejected() {
        listOf(
            "https://proxy.example",
            "example..com",
            "-proxy.example",
            "bad_name",
            "proxy/path",
            "::::",
            "a".repeat(DerbisNetworkSettings.MAX_HOST_LENGTH + 1)
        ).forEach { host ->
            val errors = DerbisNetworkSettingsValidator.validate(
                DerbisNetworkSettings(proxyMode = DerbisProxyMode.Http, proxyHost = host),
                false
            )
            assertTrue("Expected invalid host: $host", errors.contains(DerbisNetworkSettingsError.ProxyHostInvalid))
        }
    }

    @Test
    fun proxyCredentialsAreRequiredOnlyWhenAuthenticationIsEnabled() {
        val settings = DerbisNetworkSettings(
            proxyMode = DerbisProxyMode.Socks,
            proxyHost = "proxy.example.com",
            proxyPort = 1080,
            proxyAuthenticationEnabled = true
        )

        val errors = DerbisNetworkSettingsValidator.validate(settings, false)
        assertTrue(errors.contains(DerbisNetworkSettingsError.ProxyUsernameMissing))
        assertTrue(errors.contains(DerbisNetworkSettingsError.ProxyPasswordMissing))
        assertTrue(
            DerbisNetworkSettingsValidator.validate(settings.copy(proxyAuthenticationEnabled = false), false).isEmpty()
        )
    }

    @Test
    fun customDohRequiresAValidHttpsUrlWithoutUserInfoOrFragment() {
        assertTrue(DerbisNetworkSettingsValidator.validateCustomDohUrl("https://dns.example/dns-query").isEmpty())
        assertEquals(
            listOf(DerbisNetworkSettingsError.CustomDohUrlNotHttps),
            DerbisNetworkSettingsValidator.validateCustomDohUrl("http://dns.example/dns-query")
        )
        listOf(
            "https://user:pass@dns.example/dns-query",
            "https:///dns-query",
            "https://dns.example/dns-query#fragment"
        ).forEach { value ->
            assertEquals(
                listOf(DerbisNetworkSettingsError.CustomDohUrlInvalid),
                DerbisNetworkSettingsValidator.validateCustomDohUrl(value)
            )
        }
    }

    @Test
    fun httpAndSocksProxyTypesAreCreatedWithoutResolvingTheHost() {
        val http = DerbisNetworkConfiguration.proxyFor(
            DerbisNetworkSettings(proxyMode = DerbisProxyMode.Http, proxyHost = "proxy.example.com", proxyPort = 8080)
        )
        val socks = DerbisNetworkConfiguration.proxyFor(
            DerbisNetworkSettings(proxyMode = DerbisProxyMode.Socks, proxyHost = "proxy.example.com", proxyPort = 1080)
        )

        assertNotNull(http)
        assertNotNull(socks)
        assertEquals(Proxy.Type.HTTP, http?.type())
        assertEquals(Proxy.Type.SOCKS, socks?.type())
    }

    @Test
    fun dnsCatalogContainsEveryPreset() {
        val modes = listOf(
            DerbisDnsMode.Cloudflare,
            DerbisDnsMode.Google,
            DerbisDnsMode.AdGuard,
            DerbisDnsMode.Quad9
        )

        modes.forEach { mode ->
            val endpoint = DerbisDnsCatalog.endpointFor(DerbisNetworkSettings(dnsMode = mode))
            assertNotNull(endpoint)
            assertTrue(endpoint!!.url.startsWith("https://"))
            assertTrue(endpoint.bootstrapAddresses.isNotEmpty())
        }
    }
}
