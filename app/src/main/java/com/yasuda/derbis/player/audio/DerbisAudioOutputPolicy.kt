/*
 * Decides whether the audio sink may run a 24-bit float path.
 *
 * Adapted from kushagrasinghx/BitChord's playback/AudioOutputPolicy.kt (GPL-3.0). BitChord's
 * point is that float output is not a quality switch that is safe on every Android route: some
 * OEM speaker paths accept the AudioTrack and convert it back to PCM16 in AudioFlinger, and on
 * affected Samsung FLAC decoders that combination produces timestamp discontinuities and badly
 * distorted audio. Derbis had the same conclusion, hard-coded: its sink called
 * setEnableFloatOutput(false) and ended the DSP chain with Pcm16OutputAudioProcessor, so a
 * 24-bit source was truncated before the DAC ever saw it — even over USB.
 *
 * The rule here is the conservative half of BitChord's: float only on an external route that
 * actually advertises PCM float, never on the phone speaker, never when skip-silence is on
 * (SilenceSkippingAudioProcessor only accepts 16-bit), and never on Samsung, whose vendor FLAC
 * decoder is the one BitChord singles out as unsafe with float.
 */
package com.yasuda.derbis.player.audio

import android.os.Build

object DerbisAudioOutputPolicy {

    /** What the sink should do for the route that is connected right now. */
    data class Plan(
        /** Feed the sink float samples instead of truncating to 16-bit. */
        val floatOutput: Boolean,
        /** Drop the PCM16 conversion stage, which would otherwise undo the float path. */
        val bypassPcm16Stage: Boolean
    ) {
        companion object {
            val Pcm16 = Plan(floatOutput = false, bypassPcm16Stage = false)
        }
    }

    /**
     * @param state the connected route, from [DerbisLosslessOutput.state]
     * @param skipSilenceEnabled the player's skip-silence switch: it forces PCM16 downstream
     * @param manufacturer the device manufacturer, injectable for tests
     */
    fun plan(
        state: DerbisLosslessOutput.State,
        skipSilenceEnabled: Boolean,
        manufacturer: String = Build.MANUFACTURER
    ): Plan {
        if (!state.capable) return Plan.Pcm16
        if (state.via == DerbisLosslessOutput.Kind.BLUETOOTH) {
            // The negotiated codec bit depth is not readable without BLUETOOTH_CONNECT, and an
            // A2DP route almost never lists PCM float, so a Bluetooth path stays at 16-bit
            // rather than gambling on the codec.
            return Plan.Pcm16
        }
        if (!state.advertisesPcmFloat) return Plan.Pcm16
        if (skipSilenceEnabled) return Plan.Pcm16
        if (isSamsung(manufacturer)) return Plan.Pcm16
        return Plan(floatOutput = true, bypassPcm16Stage = true)
    }

    /** Samsung's vendor FLAC decoder emits invalid timestamps with PCM float. */
    fun isUnsafeFloatFlacDecoder(name: String): Boolean {
        val normalized = name.lowercase()
        return normalized == "c2.sec.flac.decoder" ||
            (normalized.startsWith("omx.sec.") && normalized.contains("flac"))
    }

    private fun isSamsung(manufacturer: String): Boolean =
        manufacturer.trim().equals("samsung", ignoreCase = true)
}
