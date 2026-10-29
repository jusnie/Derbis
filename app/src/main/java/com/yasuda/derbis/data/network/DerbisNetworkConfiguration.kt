package com.yasuda.derbis.data.network

import com.yasuda.derbis.data.network.byedpi.ByeDpiSecureResolver
import com.yasuda.derbis.data.network.byedpi.ByeDpiSupervisor
import com.yasuda.derbis.domain.DerbisDnsMode
import com.yasuda.derbis.domain.DerbisNetworkSettings
import com.yasuda.derbis.domain.DerbisProxyMode
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import okhttp3.dnsoverhttps.DnsOverHttps

internal data class DerbisDnsEndpoint(
    val url: String,
    val bootstrapAddresses: List<String>
)

internal object DerbisDnsCatalog {
    val CLOUDFLARE = DerbisDnsEndpoint(
        url = "https://cloudflare-dns.com/dns-query",
        bootstrapAddresses = listOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001")
    )
    val GOOGLE = DerbisDnsEndpoint(
        url = "https://dns.google/dns-query",
        bootstrapAddresses = listOf("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844")
    )
    val ADGUARD = DerbisDnsEndpoint(
        url = "https://dns.adguard-dns.com/dns-query",
        bootstrapAddresses = listOf("94.140.14.14", "94.140.15.15", "2a10:50c0::ad1:ff", "2a10:50c0::ad2:ff")
    )
    val QUAD9 = DerbisDnsEndpoint(
        url = "https://dns.quad9.net/dns-query",
        bootstrapAddresses = listOf("9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9")
    )

    fun endpointFor(settings: DerbisNetworkSettings): DerbisDnsEndpoint? = when (settings.dnsMode) {
        DerbisDnsMode.System -> null
        DerbisDnsMode.Cloudflare -> CLOUDFLARE
        DerbisDnsMode.Google -> GOOGLE
        DerbisDnsMode.AdGuard -> ADGUARD
        DerbisDnsMode.Quad9 -> QUAD9
        DerbisDnsMode.Custom -> settings.customDohUrl.trim()
            .takeIf { it.startsWith("https://", ignoreCase = true) }
            ?.let { DerbisDnsEndpoint(it, emptyList()) }
    }
}

private data class ResolvedDnsHolder(
    val dns: Dns,
    val generation: Long
)

internal object DerbisNetworkConfiguration {
    private val generationCounter = AtomicLong(0L)

    @Volatile
    private var settings: DerbisNetworkSettings = DerbisNetworkSettings()

    @Volatile
    private var proxyPassword: String = ""

    @Volatile
    private var resolvedDnsHolder: ResolvedDnsHolder = ResolvedDnsHolder(
        dns = DerbisNetworkIntelligence.dns,
        generation = -1L
    )

    @Volatile
    private var byeDpiResolverHolder: ResolvedDnsHolder = ResolvedDnsHolder(
        dns = DerbisNetworkIntelligence.dns,
        generation = -1L
    )

    val generation: Long get() = generationCounter.get()

    fun current(): DerbisNetworkSettings = settings

    fun hasProxyPassword(): Boolean = proxyPassword.isNotEmpty()

    @Synchronized
    fun apply(newSettings: DerbisNetworkSettings, newProxyPassword: String) {
        val normalized = newSettings.normalized()
        val previous = settings
        if (normalized == settings && newProxyPassword == proxyPassword) {
            if (normalized.byeDpiEnabled && !ByeDpiSupervisor.isEngaged()) ByeDpiSupervisor.start()
            return
        }
        settings = normalized
        proxyPassword = newProxyPassword
        generationCounter.incrementAndGet()
        resolvedDnsHolder = ResolvedDnsHolder(DerbisNetworkIntelligence.dns, -1L)
        byeDpiResolverHolder = ResolvedDnsHolder(DerbisNetworkIntelligence.dns, -1L)
        runCatching { dohConnectionPool.evictAll() }
        runCatching { dohDispatcher.cancelAll() }

        val byeDpiToggledOn = normalized.byeDpiEnabled && (!previous.byeDpiEnabled || !ByeDpiSupervisor.isRunning())
        val byeDpiToggledOff = !normalized.byeDpiEnabled && (previous.byeDpiEnabled || ByeDpiSupervisor.isRunning())
        if (byeDpiToggledOn) {
            ByeDpiSupervisor.start()
        } else if (byeDpiToggledOff) {
            ByeDpiSupervisor.stop()
        }

        DerbisHttpClientFactory.onConfigurationChanged()
    }

    fun dns(): Dns {
        val currentGeneration = generationCounter.get()
        val holder = resolvedDnsHolder
        if (holder.generation == currentGeneration) return holder.dns
        return synchronized(this) {
            val targetGeneration = generationCounter.get()
            val currentHolder = resolvedDnsHolder
            if (currentHolder.generation == targetGeneration) {
                currentHolder.dns
            } else {
                val built = buildDns(settings, proxyPassword)
                resolvedDnsHolder = ResolvedDnsHolder(built, targetGeneration)
                built
            }
        }
    }

    fun byeDpiResolver(): Dns {
        val currentGeneration = generationCounter.get()
        val holder = byeDpiResolverHolder
        if (holder.generation == currentGeneration) return holder.dns
        return synchronized(this) {
            val targetGeneration = generationCounter.get()
            val currentHolder = byeDpiResolverHolder
            if (currentHolder.generation == targetGeneration) {
                currentHolder.dns
            } else {
                val built = buildByeDpiResolver(settings, proxyPassword)
                byeDpiResolverHolder = ResolvedDnsHolder(built, targetGeneration)
                built
            }
        }
    }

    fun buildByeDpiResolver(target: DerbisNetworkSettings, targetProxyPassword: String = proxyPassword): Dns {
        val modes = if (target.dnsMode == DerbisDnsMode.System) BYEDPI_DEFAULT_DOH_MODES else listOf(target.dnsMode)
        val resolvers = modes.mapNotNull { mode ->
            val endpoint = target.copy(dnsMode = mode)
            val dns = buildDns(endpoint, targetProxyPassword)
            if (dns === DerbisNetworkIntelligence.dns) null else mode.id to dns
        }
        return ByeDpiSecureResolver(resolvers, DerbisNetworkIntelligence.dns)
    }

    fun proxy(): Proxy? = proxyFor(settings)

    fun proxyFor(target: DerbisNetworkSettings): Proxy? {
        if (!target.usesProxy || target.proxyHost.isBlank()) return null
        val type = when (target.proxyMode) {
            DerbisProxyMode.Http -> Proxy.Type.HTTP
            DerbisProxyMode.Socks -> Proxy.Type.SOCKS
            DerbisProxyMode.Disabled -> return null
        }
        return runCatching {
            Proxy(type, InetSocketAddress.createUnresolved(target.proxyHost, target.proxyPort))
        }.getOrNull()
    }

    fun proxyAuthenticator(): Authenticator? = proxyAuthenticatorFor(settings, proxyPassword)

    fun proxyAuthenticatorFor(target: DerbisNetworkSettings, password: String): Authenticator? {
        if (!target.usesProxy || !target.proxyAuthenticationEnabled) return null
        if (target.proxyUsername.isBlank() || password.isEmpty()) return null
        val credential = Credentials.basic(target.proxyUsername, password)
        return Authenticator { _: Route?, response: Response ->
            if (response.request.header(PROXY_AUTHORIZATION) != null) return@Authenticator null
            if (responseChainLength(response) > MAX_PROXY_AUTH_ATTEMPTS) return@Authenticator null
            response.request.newBuilder().header(PROXY_AUTHORIZATION, credential).build()
        }
    }

    fun buildDns(target: DerbisNetworkSettings, targetProxyPassword: String = proxyPassword): Dns {
        val endpoint = DerbisDnsCatalog.endpointFor(target) ?: return DerbisNetworkIntelligence.dns
        val url = endpoint.url.toHttpUrlOrNull() ?: return DerbisNetworkIntelligence.dns
        if (!url.isHttps) return DerbisNetworkIntelligence.dns
        val bootstrap = endpoint.bootstrapAddresses.mapNotNull { address ->
            runCatching { InetAddress.getByName(address) }.getOrNull()
        }
        val bootstrapClient = OkHttpClient.Builder()
            .connectTimeout(DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(DOH_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectionPool(dohConnectionPool)
            .dispatcher(dohDispatcher)
            .apply {
                proxyFor(target)?.let(::proxy)
                proxyAuthenticatorFor(target, targetProxyPassword)?.let(::proxyAuthenticator)
            }
            .build()
        return runCatching {
            DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(url)
                .includeIPv6(true)
                .apply { if (bootstrap.isNotEmpty()) bootstrapDnsHosts(bootstrap) }
                .build()
        }.getOrElse { DerbisNetworkIntelligence.dns }
    }

    private fun responseChainLength(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    internal val dohConnectionPool = okhttp3.ConnectionPool(4, 5, TimeUnit.MINUTES)
    private val dohDispatcher = okhttp3.Dispatcher().apply {
        maxRequests = 8
        maxRequestsPerHost = 8
    }

    private val BYEDPI_DEFAULT_DOH_MODES = listOf(DerbisDnsMode.Google, DerbisDnsMode.Cloudflare, DerbisDnsMode.AdGuard)
    private const val PROXY_AUTHORIZATION = "Proxy-Authorization"
    private const val MAX_PROXY_AUTH_ATTEMPTS = 2
    private const val DOH_TIMEOUT_SECONDS = 6L
    private const val DOH_CALL_TIMEOUT_SECONDS = 10L
}
