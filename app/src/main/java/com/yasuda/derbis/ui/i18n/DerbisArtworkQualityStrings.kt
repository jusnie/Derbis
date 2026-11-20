package com.yasuda.derbis.ui.i18n

/**
 * Copy for the artwork-quality setting.
 *
 * Separate from the other settings files because it is the only one whose subtitle describes a
 * measurement rather than a behaviour: "480 px · lightest covers, about a fifth of the data" is
 * a claim about bytes, and a translator who rewrites it as "lighter covers" turns a stated
 * trade-off back into an implied one.
 *
 * Translation falls back to English for every locale without an entry here, and
 * `localizedBundleOrEnglish` merges the English map underneath, so a missing key can never
 * surface as a raw id.
 */
internal val artworkQualityKeys = setOf(
    "artworkQualityTitle",
    "artworkQualityLow",
    "artworkQualityMedium",
    "artworkQualityHigh",
    "artworkQualityLowSubtitle",
    "artworkQualityMediumSubtitle",
    "artworkQualityHighSubtitle"
)

private fun artworkQuality(
    title: String,
    low: String,
    medium: String,
    high: String,
    lowSubtitle: String,
    mediumSubtitle: String,
    highSubtitle: String
): Map<String, String> = mapOf(
    "artworkQualityTitle" to title,
    "artworkQualityLow" to low,
    "artworkQualityMedium" to medium,
    "artworkQualityHigh" to high,
    "artworkQualityLowSubtitle" to lowSubtitle,
    "artworkQualityMediumSubtitle" to mediumSubtitle,
    "artworkQualityHighSubtitle" to highSubtitle
)

private val artworkQualityBundles: Map<String, Map<String, String>> = mapOf(
    "en" to artworkQuality(
        "Artwork quality",
        "Low",
        "Medium",
        "High",
        "480 px · lightest covers, about a fifth of the data of full size",
        "720 px · sharp covers, about a third of the data of full size",
        "1080 px · largest covers, close to full size"
    ),
    "it" to artworkQuality(
        "Qualità delle copertine",
        "Bassa",
        "Media",
        "Alta",
        "480 px · copertine più leggere, circa un quinto dei dati della dimensione piena",
        "720 px · copertine nitide, circa un terzo dei dati della dimensione piena",
        "1080 px · copertine più grandi, quasi la dimensione piena"
    ),
    "es" to artworkQuality(
        "Calidad de las portadas",
        "Baja",
        "Media",
        "Alta",
        "480 px · portadas más ligeras, cerca de un quinto de los datos del tamaño completo",
        "720 px · portadas nítidas, cerca de un tercio de los datos del tamaño completo",
        "1080 px · portadas más grandes, casi el tamaño completo"
    ),
    "fr" to artworkQuality(
        "Qualité des pochettes",
        "Basse",
        "Moyenne",
        "Haute",
        "480 px · pochettes les plus légères, environ un cinquième des données de la taille réelle",
        "720 px · pochettes nettes, environ un tiers des données de la taille réelle",
        "1080 px · pochettes les plus grandes, presque la taille réelle"
    ),
    "de" to artworkQuality(
        "Cover-Qualität",
        "Niedrig",
        "Mittel",
        "Hoch",
        "480 px · leichteste Cover, etwa ein Fünftel der Daten der vollen Größe",
        "720 px · scharfe Cover, etwa ein Drittel der Daten der vollen Größe",
        "1080 px · größte Cover, nahe der vollen Größe"
    )
)

internal fun artworkQualityLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(artworkQualityBundles, code)

internal fun artworkQualityLocalizationCodes(): Set<String> = artworkQualityBundles.keys
