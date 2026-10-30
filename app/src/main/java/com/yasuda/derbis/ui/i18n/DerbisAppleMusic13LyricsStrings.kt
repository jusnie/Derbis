package com.yasuda.derbis.ui.i18n

/** Copy for the exclusive Apple Music 13-inspired renderer. Brand naming remains stable by design. */
internal val appleMusic13LyricsKeys: Set<String> = setOf(
    "appleMusic13Lyrics",
    "appleMusic13LyricsSubtitle",
    "lyricsRendererDefault",
    "lyricsRendererDefaultDesc",
    "lyricsRendererSpicy",
    "lyricsRendererSpicyDesc",
    "lyricsRendererPreviewTitle",
    "lyricsRendererPreviewSubtitle"
)

private val appleMusic13LyricsBundles: Map<String, Map<String, String>> = mapOf(
    "en" to mapOf(
        "appleMusic13Lyrics" to "Apple Music 13 Lyrics",
        "appleMusic13LyricsSubtitle" to "Liquid Glass aesthetic with real-time karaoke highlight and kinetic swipe-up transitions.",
        "lyricsRendererDefault" to "Default",
        "lyricsRendererDefaultDesc" to "Apple Music 13 style · Liquid Glass aesthetic, real-time karaoke highlight and kinetic swipe-up transitions.",
        "lyricsRendererSpicy" to "Spicy",
        "lyricsRendererSpicyDesc" to "Spicy Lyrics style · Bold typography, active verse magnification, progressive depth blur and dynamic fiery glow.",
        "lyricsRendererPreviewTitle" to "Live Preview",
        "lyricsRendererPreviewSubtitle" to "Synchronized real-time preview of your selected lyrics style."
    )
)

internal fun appleMusic13LyricsLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(appleMusic13LyricsBundles, code)
