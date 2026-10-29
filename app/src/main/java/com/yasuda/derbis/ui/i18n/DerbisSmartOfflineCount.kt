package com.yasuda.derbis.ui.i18n

import android.icu.text.PluralRules
import com.yasuda.derbis.domain.DerbisLanguageCatalog
import java.text.NumberFormat
import java.util.Locale

fun DerbisStrings.formatSmartOfflineSongCount(count: Int): String {
    val safeCount = count.coerceAtLeast(0)
    val locale = Locale.forLanguageTag(code)
    val category = PluralRules.forLocale(locale).select(safeCount.toDouble())
    val nounKey = "smartOfflineSong" + category.replaceFirstChar { it.uppercaseChar() }
    val formattedCount = NumberFormat.getIntegerInstance(locale).format(safeCount)
    val safeNumber = if (DerbisLanguageCatalog.isRtl(code)) "\u2068$formattedCount\u2069" else formattedCount
    return value("smartOfflineSongCountPattern")
        .replace("{count}", safeNumber)
        .replace("{songs}", value(nounKey))
}
