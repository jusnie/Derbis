package com.yasuda.derbis.ui.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object DerbisTypeRhythm {

    fun lineHeight(fontSizeSp: Float): TextUnit = derbisLineHeightSp(fontSizeSp).sp

    fun lineHeight(fontSize: TextUnit): TextUnit = lineHeight(fontSize.value)
}

fun derbisLineHeightRatio(fontSizeSp: Float): Float = when {
    fontSizeSp <= 0f -> 1f
    fontSizeSp <= 11f -> 1.45f
    fontSizeSp <= 16f -> 1.40f
    fontSizeSp <= 24f -> 1.30f
    fontSizeSp <= 34f -> 1.22f
    else -> 1.16f
}

fun derbisLineHeightSp(fontSizeSp: Float): Float {
    if (fontSizeSp <= 0f) return 0f
    val raw = fontSizeSp * derbisLineHeightRatio(fontSizeSp)
    return Math.round(raw * 2f) / 2f
}
