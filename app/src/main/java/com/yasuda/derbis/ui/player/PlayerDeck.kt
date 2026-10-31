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
    Aura,
    Expressive,
    Poster,
    Pulse,
    Halo,
    Folio,
    Flow,
    Sleeve,
    AppleMusic13,
    TidalMusic
}

/**
 * Modes presented in the Player Deck chooser. Gallery and Tidal Music remain readable legacy
 * layouts, but are intentionally not selectable here: Gallery is a fallback presentation and
 * Tidal is chosen through its dedicated app skin instead of a second player-deck entry.
 */
internal val PlayerDeckOrder: List<PlayerVisualMode> = listOf(
    PlayerVisualMode.Flow,
    PlayerVisualMode.CanvasCard,
    PlayerVisualMode.CanvasImmersive,
    PlayerVisualMode.Halo,
    PlayerVisualMode.Folio,
    PlayerVisualMode.AppleMusic13,
    PlayerVisualMode.Pulse
)

internal fun PlayerVisualMode.deckLayout(): PlayerDeckLayout = when (this) {
    PlayerVisualMode.CanvasCard -> PlayerDeckLayout.Sleeve
    PlayerVisualMode.CanvasImmersive -> PlayerDeckLayout.Aura
    PlayerVisualMode.Artwork -> PlayerDeckLayout.Poster
    PlayerVisualMode.AppleMusic13 -> PlayerDeckLayout.AppleMusic13
    PlayerVisualMode.TidalMusic -> PlayerDeckLayout.TidalMusic
    PlayerVisualMode.Pulse -> PlayerDeckLayout.Pulse
    PlayerVisualMode.Halo -> PlayerDeckLayout.Halo
    PlayerVisualMode.Folio -> PlayerDeckLayout.Folio
    PlayerVisualMode.Flow -> PlayerDeckLayout.Flow
}

internal fun PlayerVisualMode.usesAuraPlayPause(): Boolean =
    deckLayout() == PlayerDeckLayout.Aura

/** Only the Apple Music 13 deck supplies the Apple Music palette and scene. */
internal fun PlayerVisualMode.usesAppleMusicPlayerStyle(): Boolean =
    this == PlayerVisualMode.AppleMusic13

internal fun PlayerDeckLayout.showsLyricsFace(): Boolean =
    this != PlayerDeckLayout.Aura && this != PlayerDeckLayout.Pulse

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

@Immutable
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

/** Lossy bitrate from which a stream counts as the Hi-Fi tier: the 320 kbps HQ upgrade. */
internal const val PLAYER_HIFI_MIN_KBPS = 256

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
