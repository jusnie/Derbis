package com.yasuda.derbis.domain

import java.net.URI

enum class DerbisDnsMode(val id: String) {
    System("system"),
    Cloudflare("cloudflare"),
    Google("google"),
    AdGuard("adguard"),
    Quad9("quad9"),
    Custom("custom");

    companion object {
        fun fromId(value: String?): DerbisDnsMode =
            entries.firstOrNull { it.id.equals(value?.trim(), ignoreCase = true) } ?: System
    }
}

enum class DerbisProxyMode(val id: String) {
    Disabled("disabled"),
    Http("http"),
    Socks("socks");

    companion object {
        fun fromId(value: String?): DerbisProxyMode =
            entries.firstOrNull { it.id.equals(value?.trim(), ignoreCase = true) } ?: Disabled
    }
}

data class DerbisNetworkSettings(
    val dnsMode: DerbisDnsMode = DerbisDnsMode.System,
    val customDohUrl: String = "",
    val proxyMode: DerbisProxyMode = DerbisProxyMode.Disabled,
    val proxyHost: String = "",
    val proxyPort: Int = DEFAULT_PROXY_PORT,
    val proxyUsername: String = "",
    val proxyAuthenticationEnabled: Boolean = false,
    val bypassProxyForStreams: Boolean = true,
    val byeDpiEnabled: Boolean = false,
    val youtubeRegionProfileEnabled: Boolean = false
) {
    val usesProxy: Boolean get() = proxyMode != DerbisProxyMode.Disabled

    fun normalized(): DerbisNetworkSettings = copy(
        customDohUrl = customDohUrl.trim().take(MAX_URL_LENGTH),
        proxyHost = proxyHost.trim().take(MAX_HOST_LENGTH),
        proxyUsername = proxyUsername.trim().take(MAX_CREDENTIAL_LENGTH)
    )

    companion object {
        const val MIN_PORT = 1
        const val MAX_PORT = 65_535
        const val DEFAULT_PROXY_PORT = 8080
        const val MAX_HOST_LENGTH = 253
        const val MAX_URL_LENGTH = 512
        const val MAX_CREDENTIAL_LENGTH = 256
    }
}

enum class DerbisNetworkSettingsError {
    ProxyHostMissing,
    ProxyHostInvalid,
    ProxyPortOutOfRange,
    ProxyUsernameMissing,
    ProxyPasswordMissing,
    CustomDohUrlMissing,
    CustomDohUrlNotHttps,
    CustomDohUrlInvalid
}

object DerbisNetworkSettingsValidator {
    private val HOST_LABEL_PATTERN = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")
    private val IPV4_PATTERN = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")

    fun validate(settings: DerbisNetworkSettings, hasProxyPassword: Boolean): List<DerbisNetworkSettingsError> {
        val errors = mutableListOf<DerbisNetworkSettingsError>()
        if (settings.dnsMode == DerbisDnsMode.Custom) {
            errors += validateCustomDohUrl(settings.customDohUrl)
        }
        if (settings.usesProxy) {
            val host = settings.proxyHost.trim()
            when {
                host.isBlank() -> errors += DerbisNetworkSettingsError.ProxyHostMissing
                host.length > DerbisNetworkSettings.MAX_HOST_LENGTH || !isValidProxyHost(host) ->
                    errors += DerbisNetworkSettingsError.ProxyHostInvalid
            }
            if (settings.proxyPort !in DerbisNetworkSettings.MIN_PORT..DerbisNetworkSettings.MAX_PORT) {
                errors += DerbisNetworkSettingsError.ProxyPortOutOfRange
            }
            if (settings.proxyAuthenticationEnabled) {
                val username = settings.proxyUsername.trim()
                if (username.isBlank() || username.length > DerbisNetworkSettings.MAX_CREDENTIAL_LENGTH) {
                    errors += DerbisNetworkSettingsError.ProxyUsernameMissing
                }
                if (!hasProxyPassword) errors += DerbisNetworkSettingsError.ProxyPasswordMissing
            }
        }
        return errors
    }

    fun validateCustomDohUrl(value: String): List<DerbisNetworkSettingsError> {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return listOf(DerbisNetworkSettingsError.CustomDohUrlMissing)
        if (trimmed.length > DerbisNetworkSettings.MAX_URL_LENGTH) {
            return listOf(DerbisNetworkSettingsError.CustomDohUrlInvalid)
        }
        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            return listOf(DerbisNetworkSettingsError.CustomDohUrlNotHttps)
        }
        val uri = runCatching { URI(trimmed) }.getOrNull()
            ?: return listOf(DerbisNetworkSettingsError.CustomDohUrlInvalid)
        val host = uri.host?.trim().orEmpty()
        if (host.isBlank() || uri.userInfo != null || uri.fragment != null) {
            return listOf(DerbisNetworkSettingsError.CustomDohUrlInvalid)
        }
        if (uri.port != -1 && uri.port !in DerbisNetworkSettings.MIN_PORT..DerbisNetworkSettings.MAX_PORT) {
            return listOf(DerbisNetworkSettingsError.CustomDohUrlInvalid)
        }
        return emptyList()
    }

    private fun isValidProxyHost(value: String): Boolean {
        if (IPV4_PATTERN.matches(value)) {
            return value.split('.').all { it.toIntOrNull() in 0..255 }
        }
        if (value.contains(':')) {
            val literal = value.removePrefix("[").removeSuffix("]")
            if (literal == value && (value.startsWith('[') || value.endsWith(']'))) return false
            if (!literal.contains(':') || literal.any {
                    !it.isDigit() && it !in 'a'..'f' && it !in 'A'..'F' && it != ':'
                }
            ) return false
            return runCatching { URI("http://[$literal]/").host != null }.getOrDefault(false)
        }
        if (value.length > DerbisNetworkSettings.MAX_HOST_LENGTH) return false
        return value.split('.').all { HOST_LABEL_PATTERN.matches(it) }
    }
}

enum class DerbisNetworkTestOutcome {
    Success,
    InvalidConfiguration,
    DnsResolutionFailed,
    ProxyAuthenticationFailed,
    Timeout,
    ConnectionRefused,
    TlsFailure,
    UnknownError
}
