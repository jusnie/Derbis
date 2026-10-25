package com.luc4n3x.levyra.ui.i18n

/** Copy for the exclusive Apple Music 13-inspired renderer. Brand naming remains stable by design. */
internal val appleMusic13LyricsKeys: Set<String> = setOf(
    "appleMusic13Lyrics",
    "appleMusic13LyricsSubtitle"
)

private val appleMusic13LyricsBundles: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf(
        "appleMusic13Lyrics" to "Apple Music 13 Lyrics",
        "appleMusic13LyricsSubtitle" to "Replaces Derbis lyrics until you turn it off."
    )
)

internal fun appleMusic13LyricsLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(appleMusic13LyricsBundles, code)
