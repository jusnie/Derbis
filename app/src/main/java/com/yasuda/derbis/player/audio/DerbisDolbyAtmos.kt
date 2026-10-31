package com.yasuda.derbis.player.audio

import android.media.MediaCodecList
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import java.util.Locale
import timber.log.Timber

/**
 * Whether an E-AC-3 (Dolby Atmos) stream would find a decoder on this phone.
 *
 * Ported from BitChord, where the question was forced by addon sources handing every device an
 * Atmos rendition and letting the ones without a licensed decoder skip the track in silence.
 * Derbis has the same failure mode waiting in any source that ships JOC audio, and the same
 * reason to ask early: a format no renderer supports is not reliably an error — the track
 * selector declines it, the period ends with no renderer enabled, and the song skips itself
 * with nothing on screen to explain why.
 *
 * E-AC-3 is licensed, so its decoder ships with the vendor image rather than with Android:
 * present on most flagships, absent on plenty of phones that look identical from up here.
 *
 * The verdict has to be the renderer's verdict, because the renderer is who acts on it.
 * [MediaCodecUtil] is asked first — it is precisely what `MediaCodecSelector` hands
 * `MediaCodecAudioRenderer`, including Media3's per-device workarounds and blocklists. Both
 * mime types are asked for: a decoder declaring plain `audio/eac3` can carry a JOC stream, with
 * the height objects dropped and the 5.1 core downmixed, and Media3 falls back to one for
 * exactly that reason. [MediaCodecList] is the backstop for the case where the Media3 query
 * throws, and `REGULAR_CODECS` rather than `ALL_CODECS`, because the extra entries only exist
 * for specific configurations and counting them says yes to a decoder the renderer never gets.
 *
 * Passthrough is deliberately not covered: a phone with no decoder can still send E-AC-3 to a
 * receiver over HDMI, but that answer changes with the audio route and cannot be cached, so a
 * receiver user sees the switch greyed out rather than a capability that flickers.
 *
 * Answered once per process and cached: the codec list cannot change while the app runs, and
 * the walk costs tens of milliseconds on a path that is otherwise holding up audio.
 */
object DerbisDolbyAtmos {

    private const val TAG = "Derbis"

    /** E-AC-3 JOC itself plus the plain E-AC-3 core it degrades to. */
    private val DOLBY_MIMES = listOf("audio/eac3-joc", "audio/eac3")

    @Volatile
    private var probed: Boolean? = null

    /** Test seam: stand in for a device without a Dolby decoder. */
    @Volatile
    internal var forced: Boolean? = null

    val decoderAvailable: Boolean
        get() = forced ?: probed ?: probe().also { probed = it }

    /** Clears the cached probe so the next read asks the platform again. */
    internal fun reset() {
        probed = null
        forced = null
    }

    /**
     * Yes on anything that cannot be established.
     *
     * The two mistakes are not symmetric: guessing "no" on a capable flagship silently drops
     * it from a rendition it can play and greys out the switch that would restore it, while
     * guessing "yes" only restores the behaviour that shipped before any of this existed.
     */
    private fun probe(): Boolean {
        val viaMedia3 = media3Decoders()
        if (viaMedia3 != null) {
            Timber.tag(TAG).d("Dolby decoders (Media3): %s", viaMedia3.ifEmpty { "none" })
            return viaMedia3.isNotEmpty()
        }
        return runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->
                !info.isEncoder && info.supportedTypes.any { it.lowercase(Locale.ROOT) in DOLBY_MIMES }
            }.map { it.name }
        }.onSuccess {
            Timber.tag(TAG).d("Dolby decoders (platform): %s", it.ifEmpty { "none" })
        }.map {
            it.isNotEmpty()
        }.getOrElse {
            Timber.tag(TAG).w(it, "could not read the codec list; assuming E-AC-3 plays")
            true
        }
    }

    /** Isolated so the unstable Media3 surface stays one function wide. */
    @OptIn(UnstableApi::class)
    private fun media3Decoders(): List<String>? = runCatching {
        DOLBY_MIMES.flatMap { MediaCodecUtil.getDecoderInfos(it, false, false) }
            .map { it.name }
            .distinct()
    }.getOrElse {
        Timber.tag(TAG).w(it, "Media3 could not enumerate Dolby decoders")
        null
    }
}

/** Why an Atmos stream is or is not being taken. */
enum class DerbisAtmosStatus {
    /** Decoder present and the listener left the switch on. */
    WANTED,

    /** The listener turned it off, which is a real preference: Atmos is lossy. */
    DISABLED,

    /** No E-AC-3 decoder on this device, so an Atmos rendition would be silent. */
    NO_DECODER
}

/**
 * The whole decision, as a pure function of the two things that decide it.
 *
 * Kept separate from [DerbisDolbyAtmos] so the rules can be tested without a codec list, and
 * so the preference and the capability stay un-folded: the listener's answer is persisted and
 * must survive being read on a phone that cannot honour it, without quietly rewriting itself.
 */
internal fun derbisAtmosStatus(decoderAvailable: Boolean, enabled: Boolean): DerbisAtmosStatus = when {
    !decoderAvailable -> DerbisAtmosStatus.NO_DECODER
    !enabled -> DerbisAtmosStatus.DISABLED
    else -> DerbisAtmosStatus.WANTED
}

/** A short, honest reason, for logs and the technical audio sheet. */
internal fun derbisAtmosReason(status: DerbisAtmosStatus): String = when (status) {
    DerbisAtmosStatus.WANTED -> "decoder available"
    DerbisAtmosStatus.DISABLED -> "switched off in Settings · Audio"
    DerbisAtmosStatus.NO_DECODER -> "no Dolby E-AC-3 decoder on this phone"
}

/**
 * Is this stream the immersive mix?
 *
 * Two spellings, and both appear in the wild: `audio/eac3-joc` as a mime type, and `ec+3` as
 * the codec string Media3 reports for E-AC-3 with Joint Object Coding. Plain `eac3` and `ac-3`
 * are the same codec *without* the height objects, which is a 5.1 render and not an Atmos one.
 */
internal fun isDolbyAtmosStream(mimeType: String?, codecs: String? = null): Boolean {
    val mime = mimeType.orEmpty().trim().lowercase(Locale.ROOT)
    val codec = codecs.orEmpty().trim().lowercase(Locale.ROOT)
    if (mime == "audio/eac3-joc") return true
    return codec == "ec+3" || codec.startsWith("ec+3.") || codec.contains("joc")
}
