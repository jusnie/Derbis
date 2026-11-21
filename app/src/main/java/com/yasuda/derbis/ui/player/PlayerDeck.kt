package com.yasuda.derbis.ui.player

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.DerbisInterfaceSettings
import com.yasuda.derbis.domain.PlayerVisualMode
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.player.audio.isDolbyAtmosStream
import com.yasuda.derbis.ui.DerbisPlayerPane
import java.util.Locale

internal enum class PlayerDeckLayout {
    Derbis,
    Expressive,
    Poster,
    Pulse,
    Flow,
    Sleeve,
    AppleMusic13,
    TidalMusic,
    Frost,
    One,
    Vinyl
}

/**
 * Modes presented in the Player Deck chooser. Gallery and Tidal Music remain readable legacy
 * layouts, but are intentionally not selectable here: Gallery is a fallback presentation and
 * Tidal is chosen through its dedicated app skin instead of a second player-deck entry.
 */
internal val PlayerDeckOrder: List<PlayerVisualMode> = listOf(
    PlayerVisualMode.Flow,
    PlayerVisualMode.CanvasCard,
    PlayerVisualMode.AppleMusic13,
    PlayerVisualMode.Pulse,
    PlayerVisualMode.Frost,
    PlayerVisualMode.DerbisOne,
    PlayerVisualMode.Vinyl
)

internal fun PlayerVisualMode.deckLayout(): PlayerDeckLayout = when (this) {
    PlayerVisualMode.CanvasCard -> PlayerDeckLayout.Sleeve
    PlayerVisualMode.Artwork -> PlayerDeckLayout.Poster
    PlayerVisualMode.AppleMusic13 -> PlayerDeckLayout.AppleMusic13
    PlayerVisualMode.TidalMusic -> PlayerDeckLayout.TidalMusic
    PlayerVisualMode.Pulse -> PlayerDeckLayout.Pulse
    PlayerVisualMode.Flow -> PlayerDeckLayout.Flow
    PlayerVisualMode.Frost -> PlayerDeckLayout.Frost
    PlayerVisualMode.DerbisOne -> PlayerDeckLayout.One
    PlayerVisualMode.Vinyl -> PlayerDeckLayout.Vinyl
}

/** Only the Apple Music 13 deck supplies the Apple Music palette and scene. */
internal fun PlayerVisualMode.usesAppleMusicPlayerStyle(): Boolean =
    this == PlayerVisualMode.AppleMusic13

internal fun PlayerDeckLayout.showsLyricsFace(): Boolean =
    this != PlayerDeckLayout.Pulse

internal fun resolvePlayerDeckLayout(
    mode: PlayerVisualMode,
    isVideoMode: Boolean,
    isLiveRadio: Boolean,
    pane: DerbisPlayerPane,
    hasTrack: Boolean
): PlayerDeckLayout {
    val layout = mode.deckLayout()
    if (layout == PlayerDeckLayout.Derbis) return layout
    // The Apple Music 13 deck owns its portrait, landscape and video layout.
    if (layout == PlayerDeckLayout.AppleMusic13) {
        return if (hasTrack && !isLiveRadio) layout else PlayerDeckLayout.Derbis
    }
    val supported = hasTrack && !isVideoMode && !isLiveRadio && pane == DerbisPlayerPane.Stacked
    return if (supported) layout else PlayerDeckLayout.Derbis
}

internal fun resolvePlayerDeckVisualMode(
    mode: PlayerVisualMode,
    layout: PlayerDeckLayout
): PlayerVisualMode = if (layout == mode.deckLayout() || mode == PlayerVisualMode.Artwork) {
    mode
} else {
    PlayerVisualMode.CanvasCard
}

/**
 * The live transport state a deck needs when it draws its own seek control instead of calling
 * [PlayerDeckSlots.progress]. Kept together so a custom deck does not take seven loose parameters.
 */
@Immutable
internal class PlayerDeckPlayback(
    val positionMs: Long,
    val bufferedPositionMs: Long,
    val durationMs: Long,
    val activeLyric: com.yasuda.derbis.domain.LyricLine?,
    val lyricsLabel: String,
    val onSeek: (Float) -> Unit,
    val onOpenLyrics: () -> Unit
) {
    val duration: Long get() = durationMs.coerceAtLeast(0L)

    /** 0..1, clamped. The one number a custom seek control needs. */
    fun fraction(): Float {
        if (duration <= 0L) return 0f
        return (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    val hasDuration: Boolean get() = duration > 0L
}

@Immutable
/**
 * The tracks either side of the one playing, for a stage that shows the queue rather than a single
 * cover.
 *
 * [previous] and [next] are null at the ends of a queue on purpose: a carousel that wraps is a
 * different promise from one that does not, and a deck drawing a neighbour that will never play
 * would be lying about what pressing next does.
 */
internal data class PlayerDeckQueue(
    val previous: Track?,
    val current: Track,
    val next: Track?,
    val index: Int,
    val total: Int,
    val farPrevious: Track? = null,
    val farNext: Track? = null
) {
    val hasNeighbours: Boolean get() = previous != null || next != null
}

internal fun playerDeckQueue(
    queue: List<Track>,
    trackId: String,
    playlistEntryId: String = ""
): PlayerDeckQueue? {
    if (trackId.isBlank() || queue.size < 2) return null
    val index = if (playlistEntryId.isNotBlank()) {
        queue.indexOfFirst { it.playlistEntryId == playlistEntryId }.takeIf { it >= 0 }
            ?: queue.indexOfFirst { it.id == trackId }
    } else {
        queue.indexOfFirst { it.id == trackId }
    }
    if (index < 0) return null
    return PlayerDeckQueue(
        previous = queue.getOrNull(index - 1),
        current = queue[index],
        next = queue.getOrNull(index + 1),
        index = index,
        total = queue.size,
        farPrevious = queue.getOrNull(index - 2),
        farNext = queue.getOrNull(index + 2)
    )
}

internal data class PlayerDeckQueuePosition(
    val index: Int,
    val total: Int
) {
    val indexLabel: String get() = String.format(Locale.ROOT, "%02d", index + 1)
    val totalLabel: String get() = String.format(Locale.ROOT, "%02d", total)
}

internal fun playerDeckQueuePosition(queue: List<Track>, trackId: String): PlayerDeckQueuePosition? {
    if (queue.size < 2 || trackId.isBlank()) return null
    val index = queue.indexOfFirst { it.id == trackId }
    return if (index < 0) null else PlayerDeckQueuePosition(index, queue.size)
}

internal fun playerDeckAlbumEyebrow(track: Track): String {
    val album = track.album.trim()
    return if (album.isEmpty() || album.equals(track.title.trim(), ignoreCase = true)) "" else album.uppercase()
}

internal fun expressiveTitleSize(title: String, compact: Boolean): TextUnit {
    val length = title.trim().length
    val base = when {
        length <= 12 -> 46f
        length <= 22 -> 40f
        length <= 36 -> 34f
        else -> 29f
    }
    return (if (compact) base - 6f else base).sp
}

@Immutable
internal data class PlayerAudioSpec(
    val codec: String = "",
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val channels: Int? = null,
    val mimeType: String = "",
    val codecString: String = "",
    /** "LOSSLESS" or "HI-FI" when the stream earns one; null keeps the raw [codec] on the badge. */
    val quality: String? = null
) {
    val isEmpty: Boolean
        get() = codec.isEmpty() && bitrateKbps == null && sampleRateHz == null && channels == null &&
            mimeType.isEmpty() && codecString.isEmpty()
}

/** Lossy bitrate from which a stream counts as the Hi-Fi / High tier: the 320 kbps HQ upgrade. */
internal const val PLAYER_HIFI_MIN_KBPS = 256

/** Lossy bitrate from which a stream counts as the Medium tier (96..255 kbps balanced). */
internal const val PLAYER_MEDIUM_MIN_KBPS = 96

/**
 * The listener-facing quality band for [spec], matching the Low / Medium / High / Lossless / Hi-Fi
 * vocabulary in audio settings (preview/player-audio-chip.html).
 */
internal fun playerAudioTierLabel(spec: PlayerAudioSpec): String {
    val mime = spec.mimeType.lowercase(Locale.ROOT)
    val codecs = spec.codecString.lowercase(Locale.ROOT)
    if (isDolbyAtmosStream(mime, codecs)) return ""
    if (isLosslessAudioStream(mime, codecs)) {
        val hiRes = spec.quality == "HI-FI" || (spec.sampleRateHz ?: 0) >= 88_200
        return if (hiRes) "Hi-Fi" else "Lossless"
    }
    val kbps = spec.bitrateKbps ?: 0
    return when {
        kbps >= PLAYER_HIFI_MIN_KBPS -> "High"
        kbps >= PLAYER_MEDIUM_MIN_KBPS -> "Medium"
        kbps > 0 -> "Low"
        spec.quality == "HI-FI" -> "High"
        spec.quality == "LOSSLESS" -> "Lossless"
        else -> ""
    }
}

/**
 * Compact two-token badge string shared by every player deck (`AAC · Medium`, `FLAC · Lossless`,
 * `FLAC · Hi-Fi`, `Dolby Atmos`), returning `""` before a stream resolves rather than inventing
 * a placeholder bitrate.
 */
internal fun playerAudioBadgeLabel(spec: PlayerAudioSpec): String {
    val mime = spec.mimeType.lowercase(Locale.ROOT)
    val codecs = spec.codecString.lowercase(Locale.ROOT)
    if (isDolbyAtmosStream(mime, codecs)) return "Dolby Atmos"
    val codec = spec.codec.ifEmpty { playerAudioCodecLabel(spec.mimeType, spec.codecString) }
    val tier = playerAudioTierLabel(spec)
    return when {
        codec.isNotEmpty() && tier.isNotEmpty() -> "$codec · $tier"
        tier.isNotEmpty() -> tier
        codec.isNotEmpty() -> codec
        else -> ""
    }
}

/**
 * What a listener is actually getting, when the codec name would undersell it.
 *
 * A lossless source is reported as LOSSLESS, the same source at 24-bit or 88.2 kHz and up as
 * HI-FI, and a lossy stream only earns HI-FI once it is a genuine 320 kbps upgrade
 * ([PLAYER_HIFI_MIN_KBPS] and above). Everything below that returns null so the badge keeps
 * naming the codec — AAC and OPUS stay honest instead of being dressed up.
 */
internal fun playerAudioQualityLabel(
    mimeType: String?,
    codecs: String?,
    sampleRateHz: Int? = null,
    bitDepth: Int? = null,
    bitrateKbps: Int? = null
): String? {
    val mime = mimeType.orEmpty().lowercase(Locale.ROOT)
    val codec = codecs.orEmpty().lowercase(Locale.ROOT)
    // Atmos is a different mix rather than a rung on the bitrate ladder, so it keeps its own name
    // instead of being flattened into HI-FI by a 768 kbps E-AC-3 bitrate.
    if (isDolbyAtmosStream(mime, codec)) return null
    if (isLosslessAudioStream(mime, codec)) {
        val hiRes = (bitDepth ?: 0) >= 24 || (sampleRateHz ?: 0) >= 88_200
        return if (hiRes) "HI-FI" else "LOSSLESS"
    }
    return "HI-FI".takeIf { (bitrateKbps ?: 0) >= PLAYER_HIFI_MIN_KBPS }
}

/**
 * The codec that is really playing, for the technical sheet and the lossless checks.
 *
 * Deliberately unaware of [playerAudioQualityLabel]: "Lossless" is a badge, not a codec, and
 * the technical rows have to keep telling the user it is FLAC or ALAC.
 */
internal fun playerAudioCodecLabel(
    mimeType: String?,
    codecs: String?
): String {
    val mime = mimeType.orEmpty().lowercase(Locale.ROOT)
    val codec = codecs.orEmpty().lowercase(Locale.ROOT)
    return when {
        mime == "audio/opus" || codec.startsWith("opus") -> "OPUS"
        mime == "audio/mp4a-latm" || codec.startsWith("mp4a") -> "AAC"
        mime == "audio/mpeg" -> "MP3"
        mime == "audio/mpeg-l2" -> "MP2"
        mime == "audio/flac" || codec == "flac" -> "FLAC"
        mime == "audio/vorbis" || codec == "vorbis" -> "VORBIS"
        mime == "audio/alac" || codec == "alac" -> "ALAC"
        isDolbyAtmosStream(mime, codec) -> "DOLBY ATMOS"
        mime == "audio/eac3" || codec == "eac3" -> "E-AC-3"
        mime == "audio/ac3" || codec == "ac-3" -> "AC-3"
        mime == "audio/raw" || codec.startsWith("pcm") -> "PCM"
        mime.startsWith("audio/") -> mime.removePrefix("audio/").uppercase(Locale.ROOT).take(8)
        else -> ""
    }
}

/** FLAC, ALAC, WAV and raw PCM: formats that carry the samples themselves. */
internal fun isLosslessAudioStream(mime: String, codec: String): Boolean =
    mime == "audio/flac" || mime == "audio/alac" || mime == "audio/wav" ||
        mime == "audio/x-wav" || mime == "audio/raw" ||
        codec == "flac" || codec == "alac" || codec.startsWith("pcm")

internal fun playerAudioSpecLabels(spec: PlayerAudioSpec): List<String> = buildList {
    (spec.quality ?: spec.codec).takeIf { it.isNotEmpty() }?.let(::add)
    // A FLAC or ALAC bitrate is a compression ratio, not a quality figure, so it stays off the
    // badge once the stream already says Lossless or Hi-Fi.
    val lossless = isLosslessAudioStream(
        spec.mimeType.lowercase(Locale.ROOT),
        spec.codecString.lowercase(Locale.ROOT)
    )
    if (!lossless) spec.bitrateKbps?.takeIf { it > 0 }?.let { add("$it kbps") }
    spec.sampleRateHz?.takeIf { it > 0 }?.let { add(formatSampleRate(it)) }
    spec.channels?.takeIf { it > 0 }?.let { add(formatChannelLayout(it)) }
}

private fun formatSampleRate(sampleRateHz: Int): String {
    val khz = sampleRateHz / 1000f
    val text = if (sampleRateHz % 1000 == 0) {
        (sampleRateHz / 1000).toString()
    } else {
        String.format(Locale.ROOT, "%.1f", khz)
    }
    return "$text kHz"
}

private fun formatChannelLayout(channels: Int): String = when (channels) {
    1 -> "1.0"
    2 -> "2.0"
    6 -> "5.1"
    8 -> "7.1"
    else -> "${channels}ch"
}

@Immutable
internal class PlayerDeckSlots(
    val header: @Composable () -> Unit,
    val stage: @Composable (Track, Modifier, Dp?) -> Unit,
    val gestures: @Composable (Track, Modifier) -> Unit,
    val controls: @Composable ColumnScope.(Track) -> Unit,
    val controlsWithoutMetadata: @Composable ColumnScope.(Track) -> Unit,
    val engagement: @Composable (Track) -> Unit,
    val progress: @Composable (style: PlayerProgressStyle) -> Unit,
    /**
     * Artwork for any track, at the size asked for.
     *
     * A deck whose stage shows its neighbours needs their covers too, and it must not open a
     * second loading path to get them: this is the same renderer the centre card uses.
     */
    val cover: @Composable (Track, Dp, Dp?) -> Unit,
    /** The queue around the current track; null when there is nothing either side of it. */
    val queue: PlayerDeckQueue?,
    /**
     * The player's error line, on its own.
     *
     * It used to be rendered inside [progress], which was fine while every deck with a failure to
     * report also drew the shared seek bar. Vinyl draws its own, so it never called that slot and
     * a failed stream was completely silent there: no message, no seek, just a play button that
     * did nothing. A deck that owns its seek still needs somewhere to put the reason.
     */
    val error: @Composable () -> Unit,
    val dock: @Composable (Track) -> Unit,
    val dockActions: @Composable (Track) -> List<PlayerDockAction>,
    val creditsEyebrow: PlayerCreditsEyebrow? = null
)

internal class PlayerCreditsEyebrow(
    val trackId: String,
    val heading: String,
    val names: List<String>,
    val contentDescription: String,
    val onClick: () -> Unit
)

internal val DefaultPlayerVisualMode: PlayerVisualMode = DerbisInterfaceSettings().playerVisualMode
