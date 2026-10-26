package com.luc4n3x.derbis.data.network

import com.luc4n3x.derbis.domain.DerbisNetworkSettings
import com.luc4n3x.derbis.domain.DerbisNetworkSettingsValidator
import com.luc4n3x.derbis.domain.DerbisNetworkTestOutcome
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object DerbisNetworkTester {
    private const val PROBE_URL = "https://www.youtube.com/generate_204"
    private const val PROBE_TIMEOUT_SECONDS = 10L
    private const val PROXY_AUTH_REQUIRED = 407

    suspend fun test(
        settings: DerbisNetworkSettings,
        proxyPassword: String
    ): DerbisNetworkTestOutcome = withContext(Dispatchers.IO) {
        if (DerbisNetworkSettingsValidator.validate(settings, proxyPassword.isNotEmpty()).isNotEmpty()) {
            return@withContext DerbisNetworkTestOutcome.InvalidConfiguration
        }
        val normalized = settings.normalized()
        val client = OkHttpClient.Builder()
            .connectTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .dns(DerbisNetworkConfiguration.buildDns(normalized, proxyPassword))
            .apply {
                val configuredProxy = DerbisNetworkConfiguration.proxyFor(normalized)
                if (configuredProxy != null) {
                    proxy(configuredProxy)
                    DerbisNetworkConfiguration.proxyAuthenticatorFor(normalized, proxyPassword)
                        ?.let(::proxyAuthenticator)
                } else {
                    proxy(Proxy.NO_PROXY)
                }
            }
            .build()
        try {
            val request = Request.Builder().url(PROBE_URL).head().build()
            client.newCall(request).execute().use { response ->
                when {
                    response.code == PROXY_AUTH_REQUIRED -> DerbisNetworkTestOutcome.ProxyAuthenticationFailed
                    response.isSuccessful || response.isRedirect -> DerbisNetworkTestOutcome.Success
                    else -> DerbisNetworkTestOutcome.UnknownError
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            classify(error)
        } finally {
            runCatching { client.connectionPool.evictAll() }
            runCatching { client.dispatcher.executorService.shutdown() }
        }
    }

    internal fun classify(error: Throwable): DerbisNetworkTestOutcome {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            when (current) {
                is UnknownHostException -> return DerbisNetworkTestOutcome.DnsResolutionFailed
                is SSLException -> return DerbisNetworkTestOutcome.TlsFailure
                is SocketTimeoutException -> return DerbisNetworkTestOutcome.Timeout
                is ConnectException,
                is NoRouteToHostException -> return DerbisNetworkTestOutcome.ConnectionRefused
                is InterruptedIOException -> return DerbisNetworkTestOutcome.Timeout
            }
            if (isProxyAuthenticationFailure(current)) return DerbisNetworkTestOutcome.ProxyAuthenticationFailed
            val next = current.cause
            current = if (next === current) null else next
            depth++
        }
        return DerbisNetworkTestOutcome.UnknownError
    }

    private fun isProxyAuthenticationFailure(error: Throwable): Boolean {
        val message = error.message ?: return false
        return message.contains("Failed to authenticate with proxy", ignoreCase = true)
    }

    private const val MAX_CAUSE_DEPTH = 8
}
