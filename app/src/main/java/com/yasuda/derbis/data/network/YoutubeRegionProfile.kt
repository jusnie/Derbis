package com.yasuda.derbis.data.network

import com.yasuda.derbis.domain.DerbisContentLocale
import com.yasuda.derbis.domain.DerbisContentLocales

object YoutubeRegionProfile {
    const val US_GL = "US"
    const val US_HL = "en"
    const val US_ACCEPT_LANGUAGE = "en-US,en;q=0.9"

    fun isEnabled(): Boolean {
        return DerbisNetworkConfiguration.current().youtubeRegionProfileEnabled
    }

    fun effectiveLocale(preferredLanguageCode: String): DerbisContentLocale {
        if (isEnabled()) {
            return DerbisContentLocales.forLanguage("en")
        }
        return DerbisContentLocales.forLanguage(preferredLanguageCode)
    }

    fun cacheScope(languageCode: String): String {
        val locale = effectiveLocale(languageCode)
        return "${locale.hl}-${locale.gl}".lowercase()
    }

    fun effectiveAcceptLanguage(defaultAcceptLanguage: String): String {
        if (isEnabled()) {
            return US_ACCEPT_LANGUAGE
        }
        return defaultAcceptLanguage
    }
}
