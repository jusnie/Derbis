package com.luc4n3x.derbis.domain

import java.util.Locale

data class DerbisLanguageOption(
    val code: String,
    val flag: String,
    val englishName: String,
    val nativeName: String
)

object DerbisLanguageCatalog {
    val languages = listOf(
        DerbisLanguageOption("en", "🇬🇧", "English", "English"),
        DerbisLanguageOption("it", "🇮🇹", "Italian", "Italiano"),
        DerbisLanguageOption("es", "🇪🇸", "Spanish", "Español"),
        DerbisLanguageOption("fr", "🇫🇷", "French", "Français"),
        DerbisLanguageOption("de", "🇩🇪", "German", "Deutsch"),
        DerbisLanguageOption("pt", "🇵🇹", "Portuguese", "Português"),
        DerbisLanguageOption("nl", "🇳🇱", "Dutch", "Nederlands"),
        DerbisLanguageOption("pl", "🇵🇱", "Polish", "Polski"),
        DerbisLanguageOption("ro", "🇷🇴", "Romanian", "Română"),
        DerbisLanguageOption("el", "🇬🇷", "Greek", "Ελληνικά"),
        DerbisLanguageOption("sv", "🇸🇪", "Swedish", "Svenska"),
        DerbisLanguageOption("da", "🇩🇰", "Danish", "Dansk"),
        DerbisLanguageOption("cs", "🇨🇿", "Czech", "Čeština"),
        DerbisLanguageOption("sk", "🇸🇰", "Slovak", "Slovenčina"),
        DerbisLanguageOption("hr", "🇭🇷", "Croatian", "Hrvatski"),
        DerbisLanguageOption("sr", "🇷🇸", "Serbian", "Српски"),
        DerbisLanguageOption("sq", "🇦🇱", "Albanian", "Shqip"),
        DerbisLanguageOption("bg", "🇧🇬", "Bulgarian", "Български"),
        DerbisLanguageOption("hu", "🇭🇺", "Hungarian", "Magyar"),
        DerbisLanguageOption("fi", "🇫🇮", "Finnish", "Suomi"),
        DerbisLanguageOption("et", "🇪🇪", "Estonian", "Eesti"),
        DerbisLanguageOption("nb", "🇳🇴", "Norwegian Bokmål", "Norsk bokmål"),
        DerbisLanguageOption("ca", "🇪🇸", "Catalan", "Català"),
        DerbisLanguageOption("uk", "🇺🇦", "Ukrainian", "Українська"),
        DerbisLanguageOption("ru", "🇷🇺", "Russian", "Русский"),
        DerbisLanguageOption("kk", "🇰🇿", "Kazakh", "Қазақша"),
        DerbisLanguageOption("tr", "🇹🇷", "Turkish", "Türkçe"),
        DerbisLanguageOption("ar", "🇸🇦", "Arabic", "العربية"),
        DerbisLanguageOption("fa", "🇮🇷", "Persian", "فارسی"),
        DerbisLanguageOption("zh", "🇨🇳", "Chinese (Simplified)", "简体中文"),
        DerbisLanguageOption("zh-Hant", "🇹🇼", "Chinese (Traditional)", "繁體中文"),
        DerbisLanguageOption("ja", "🇯🇵", "Japanese", "日本語"),
        DerbisLanguageOption("ko", "🇰🇷", "Korean", "한국어"),
        DerbisLanguageOption("hi", "🇮🇳", "Hindi", "हिन्दी"),
        DerbisLanguageOption("bn", "🇧🇩", "Bengali", "বাংলা"),
        DerbisLanguageOption("id", "🇮🇩", "Indonesian", "Bahasa Indonesia"),
        DerbisLanguageOption("ms", "🇲🇾", "Malay", "Bahasa Melayu"),
        DerbisLanguageOption("vi", "🇻🇳", "Vietnamese", "Tiếng Việt"),
        DerbisLanguageOption("th", "🇹🇭", "Thai", "ไทย"),
        DerbisLanguageOption("fil", "🇵🇭", "Filipino", "Filipino"),
        DerbisLanguageOption("he", "🇮🇱", "Hebrew", "עברית")
    )

    private val supportedCodes = languages.map { it.code }.toSet()
    private val rtlCodes = setOf("ar", "fa", "he")
    private val traditionalChineseRegions = setOf("TW", "HK", "MO")

    fun normalize(code: String): String {
        val rawTag = code.trim().replace('_', '-')
        if (rawTag.isBlank()) return "en"
        val locale = Locale.forLanguageTag(rawTag)
        val language = when (locale.language.lowercase(Locale.ROOT)) {
            "in" -> "id"
            "tl" -> "fil"
            "iw" -> "he"
            else -> locale.language.lowercase(Locale.ROOT)
        }
        val canonical = when {
            language == "zh" && (
                locale.script.equals("Hant", ignoreCase = true) ||
                    locale.country.uppercase(Locale.ROOT) in traditionalChineseRegions
            ) -> "zh-Hant"
            language == "zh" -> "zh"
            else -> language
        }
        return if (canonical in supportedCodes) canonical else "en"
    }

    fun deviceDefault(): String = normalize(Locale.getDefault().toLanguageTag())

    fun displayName(code: String): String {
        val language = languages.firstOrNull { it.code == normalize(code) }
        return language?.let { "${it.flag} ${it.nativeName}" } ?: "🇬🇧 English"
    }

    fun isRtl(code: String): Boolean = normalize(code) in rtlCodes
}
