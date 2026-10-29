package com.yasuda.derbis.domain

object DerbisAudioQuality {
    const val AUTO = "Auto"
    const val HIGH = "High"
    const val MEDIUM = "Medium"
    const val LOW = "Low"
    const val DEFAULT = AUTO

    fun normalize(value: String?): String = when (value?.trim()?.lowercase()) {
        "auto" -> AUTO
        "high" -> HIGH
        "medium" -> MEDIUM
        "low" -> LOW
        else -> DEFAULT
    }
}
