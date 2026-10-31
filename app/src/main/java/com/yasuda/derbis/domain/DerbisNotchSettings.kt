package com.yasuda.derbis.domain

/**
 * Everything the Derbis Notch can be told to show.
 *
 * The island is a music surface, so every switch here is about the track that is playing — cover,
 * lyrics, transport, progress, motion. Nothing in this file can reach calls, notifications, timers
 * or another app, which is what keeps it a player feature rather than a system-wide island.
 */
data class DerbisNotchSettings(
    /** How the active lyric line is dressed inside the expanded card. */
    val style: DerbisNotchStyle = DerbisNotchStyle.Default,
    /** Cover art, on the collapsed pill and in the card. */
    val showArtwork: Boolean = true,
    /** The synced lyric sheet beside the artwork. Off gives the card one tight column. */
    val showLyrics: Boolean = true,
    /** Previous, play and next in the card. */
    val showTransport: Boolean = true,
    /** The playback hairline pinned to the card's bottom edge. */
    val showProgress: Boolean = true,
    /** The three breathing bars on the collapsed pill. */
    val showWaveform: Boolean = true,
    /** A tick when the island opens or closes. */
    val haptics: Boolean = true
) {
    /** Without a lyric sheet there is nothing to sit beside the artwork, so the card goes single-column. */
    val compactCard: Boolean get() = !showLyrics
}
