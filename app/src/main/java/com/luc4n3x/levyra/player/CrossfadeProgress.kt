package com.luc4n3x.levyra.player

/** UI-safe transition state emitted only while the two decks overlap. */
data class CrossfadeProgress(
    val active: Boolean = false,
    val fraction: Float = 0f,
    val fromTitle: String = "",
    val toTitle: String = ""
) {
    val label: String
        get() = if (toTitle.isBlank()) "Crossfading…" else "Crossfading… $toTitle"
}
