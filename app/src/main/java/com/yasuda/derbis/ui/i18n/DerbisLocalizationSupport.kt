package com.yasuda.derbis.ui.i18n

import com.yasuda.derbis.domain.DerbisLanguageCatalog

internal fun <T> localizedValueOrEnglish(
    values: Map<String, T>,
    code: String
): T = values[code] ?: values.getValue("en")

internal fun localizedBundleOrEnglish(
    bundles: Map<String, Map<String, String>>,
    code: String
): Map<String, String> {
    val en = bundles["en"] ?: emptyMap()
    val localized = bundles[code] ?: return en
    return if (code == "en") localized else en + localized
}

internal fun supportedLocalizationCodes(): Set<String> =
    DerbisLanguageCatalog.languages.map { it.code }.toSet()
