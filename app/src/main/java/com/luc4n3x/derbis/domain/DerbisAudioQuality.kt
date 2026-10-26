package com.luc4n3x.derbis.domain

object DerbisAudioQuality {
    const val AUTO = "Auto"
    const val HIGH = "High"
    const val LOW = "Low"
    const val DEFAULT = HIGH

    fun normalize(value: String?): String = when (value?.trim()?.lowercase()) {
        "auto" -> AUTO
        "high" -> HIGH
        "low" -> LOW
        else -> DEFAULT
    }
}
