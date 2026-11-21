package com.yasuda.derbis.domain

/**
 * How hard the resolver pushes for bitrate.
 *
 * The four steps are the ones the audio panel offers: Low and Medium trade quality for data,
 * High takes the biggest lossy stream, and Lossless demands a lossless one wherever the source
 * can deliver it. Auto is gone from the picker — it took exactly the same branch as High — so a
 * stored Auto reads as High instead of silently dropping to data saver.
 */
object DerbisAudioQuality {
    const val LOSSLESS = "Lossless"
    const val HIGH = "High"
    const val MEDIUM = "Medium"
    const val LOW = "Low"

    /** Legacy value: builds before the four-step picker stored it, and it resolved as High. */
    const val AUTO = HIGH
    const val DEFAULT = HIGH

    fun normalize(value: String?): String = when (value?.trim()?.lowercase()) {
        "lossless" -> LOSSLESS
        "low" -> LOW
        "medium" -> MEDIUM
        "high", "auto" -> HIGH
        else -> DEFAULT
    }

    fun prefersLossless(value: String?): Boolean = normalize(value) == LOSSLESS
}
