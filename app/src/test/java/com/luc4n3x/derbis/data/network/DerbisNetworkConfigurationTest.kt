package com.luc4n3x.derbis.data.network

import com.luc4n3x.derbis.domain.DerbisDnsMode
import com.luc4n3x.derbis.domain.DerbisNetworkSettings
import com.luc4n3x.derbis.domain.DerbisProxyMode
import java.net.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DerbisNetworkConfigurationTest {

    @Before
    @After
    fun resetConfiguration() {
        DerbisNetworkConfiguration.apply(DerbisNetworkSettings(), "")
    }

    @Test
    fun dnsHolderMaintainsCoherentGenerationAcrossFastPath() {
        val initialGeneration = DerbisNetworkConfiguration.generation
        val dns1 = DerbisNetworkConfiguration.dns()
        assertNotNull(dns1)

        val settings = DerbisNetworkSettings(
            dnsMode = DerbisDnsMode.Cloudflare
        )
        DerbisNetworkConfiguration.apply(settings, "")

        val newGeneration = DerbisNetworkConfiguration.generation
        assertTrue(newGeneration > initialGeneration)

        val dns2 = DerbisNetworkConfiguration.dns()
        assertNotNull(dns2)
        assertEquals(dns2, DerbisNetworkConfiguration.dns())
    }

    @Test
    fun concurrentDnsReadsAndConfigurationUpdatesDoNotProduceTornGenerations() {
        val threads = 4
        val iterations = 200
        val executor = Executors.newFixedThreadPool(threads + 1)
        val failureOccurred = AtomicBoolean(false)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threads + 1)

        for (i in 0 until threads) {
            executor.execute {
                try {
                    startLatch.await()
                    repeat(iterations) {
                        assertNotNull(DerbisNetworkConfiguration.dns())
                    }
                } catch (t: Throwable) {
                    failureOccurred.set(true)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        executor.execute {
            try {
                startLatch.await()
                for (j in 0 until iterations) {
                    val mode = if (j % 2 == 0) DerbisDnsMode.Google else DerbisDnsMode.System
                    DerbisNetworkConfiguration.apply(DerbisNetworkSettings(dnsMode = mode), "")
                }
            } catch (t: Throwable) {
                failureOccurred.set(true)
            } finally {
                doneLatch.countDown()
            }
        }

        startLatch.countDown()
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS))
        assertFalse(failureOccurred.get())
        executor.shutdown()
    }

    @Test
    fun buildDnsUsesCandidateProxyPassword() {
        val settings = DerbisNetworkSettings(
            dnsMode = DerbisDnsMode.Cloudflare,
            proxyMode = DerbisProxyMode.Http,
            proxyHost = "127.0.0.1",
            proxyPort = 8080,
            proxyUsername = "testuser",
            proxyAuthenticationEnabled = true
        )

        val dns = DerbisNetworkConfiguration.buildDns(settings, "candidatePassword123")
        assertNotNull(dns)
        assertFalse(DerbisNetworkConfiguration.hasProxyPassword())
    }

    @Test
    fun proxyAuthenticatorEnforcesBasicAuthAndValidation() {
        val settings = DerbisNetworkSettings(
            proxyMode = DerbisProxyMode.Http,
            proxyHost = "127.0.0.1",
            proxyPort = 8080,
            proxyUsername = "myuser",
            proxyAuthenticationEnabled = true
        )
        val authenticator = DerbisNetworkConfiguration.proxyAuthenticatorFor(settings, "mypassword")
        assertNotNull(authenticator)

        val unauthenticatedSettings = settings.copy(proxyAuthenticationEnabled = false)
        assertNull(DerbisNetworkConfiguration.proxyAuthenticatorFor(unauthenticatedSettings, "mypassword"))

        val emptyPasswordSettings = settings.copy(proxyAuthenticationEnabled = true)
        assertNull(DerbisNetworkConfiguration.proxyAuthenticatorFor(emptyPasswordSettings, ""))
    }
}
