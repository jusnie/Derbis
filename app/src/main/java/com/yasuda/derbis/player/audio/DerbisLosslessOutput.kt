/*
 * Whether the phone has an output connected right now that can carry lossless audio.
 *
 * Adapted from kushagrasinghx/BitChord's playback/audio/LosslessOutput.kt (GPL-3.0), which in
 * turn documents the same rule for its own addon gate: lossless means the file's samples reach
 * the listener's converter without being re-encoded to a lossy format first. A cable does that
 * by construction; Bluetooth only does it on a handful of high-bitrate codecs.
 *
 * Differences from the original: no Bluetooth codec tracker (reading the negotiated A2DP codec
 * needs BLUETOOTH_CONNECT and hidden APIs, so an unnamed codec is reported as unknown rather
 * than guessed) and no long-lived AudioDeviceCallback — the state is read when the audio sink is
 * built, which is the only place it decides anything.
 */
package com.yasuda.derbis.player.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import java.util.Locale

object DerbisLosslessOutput {

    enum class Kind { WIRED, USB, HDMI, DOCK, BLUETOOTH }

    data class State(
        val capable: Boolean = false,
        /** What qualified, when something did. */
        val via: Kind? = null,
        /** The Bluetooth codec in use, when Android named one. */
        val bluetoothCodec: String? = null,
        /** A Bluetooth output is connected but its codec could not be read. */
        val bluetoothCodecUnknown: Boolean = false,
        /** The route lists PCM float among its encodings, so 24-bit output is safe to try. */
        val advertisesPcmFloat: Boolean = false
    )

    /** Reads the currently connected outputs. Safe to call off the main thread. */
    fun state(context: Context): State {
        val manager = context.applicationContext.getSystemService(AudioManager::class.java)
            ?: return State()
        val outputs = runCatching { manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }
            .getOrNull()
            .orEmpty()
            .filter { it.isSink }
        val types = outputs.map { it.type }.toSet()
        val decision = decide(types)
        val via = decision.via
        val advertisesFloat = via != null && outputs.any { device ->
            device.type in kindsOf(via) && device.encodings.contains(AudioFormat.ENCODING_PCM_FLOAT)
        }
        return decision.copy(advertisesPcmFloat = advertisesFloat)
    }

    /**
     * The decision itself, from the connected output types, with the Bluetooth codec Android
     * named (null if it named none). Pure, for tests.
     */
    internal fun decide(outputTypes: Set<Int>, bluetoothCodec: String? = null, bluetoothSampleRateHz: Int? = null): State {
        val cabled = when {
            outputTypes.any { it in WIRED } -> Kind.WIRED
            outputTypes.any { it in USB } -> Kind.USB
            outputTypes.any { it in HDMI } -> Kind.HDMI
            outputTypes.any { it in DOCK } -> Kind.DOCK
            else -> null
        }
        if (cabled != null) return State(capable = true, via = cabled, bluetoothCodec = bluetoothCodec)
        if (AudioDeviceInfo.TYPE_BLUETOOTH_A2DP !in outputTypes) return State()
        if (bluetoothCodec == null) return State(bluetoothCodecUnknown = true)
        val lossless = isLosslessBluetoothCodec(bluetoothCodec, bluetoothSampleRateHz)
        return State(
            capable = lossless,
            via = Kind.BLUETOOTH.takeIf { lossless },
            bluetoothCodec = bluetoothCodec
        )
    }

    /**
     * LDAC, any LHDC generation, aptX Lossless or SSC UHQ — by the name Android reports. SBC,
     * AAC, plain aptX, aptX HD, LC3 and Opus are lossy at these bitrates, and plain SSC tops out
     * at 512 kbps; only its UHQ mode (24-bit/96 kHz) belongs here. A Samsung name without "UHQ"
     * still counts when the codec runs at 88.2 kHz or above, because that is the only Samsung
     * mode that does.
     */
    internal fun isLosslessBluetoothCodec(name: String, sampleRateHz: Int? = null): Boolean {
        val codec = name.uppercase(Locale.ROOT)
        if ("LDAC" in codec || "LHDC" in codec || ("APTX" in codec && "LOSSLESS" in codec)) return true
        if ("UHQ" in codec) return true
        val samsung = SAMSUNG_NAMES.any { it in codec }
        return samsung && sampleRateHz != null && sampleRateHz >= 88_200
    }

    /** A short, user-facing description of what the current output can do. */
    fun describe(state: State): String = when {
        state.via == Kind.USB -> "USB audio"
        state.via == Kind.WIRED -> "Wired headphones"
        state.via == Kind.HDMI -> "HDMI"
        state.via == Kind.DOCK -> "Dock"
        state.via == Kind.BLUETOOTH -> {
            val codec = state.bluetoothCodec?.takeIf { it.isNotBlank() }
            if (codec != null) "Bluetooth · $codec" else "Bluetooth · lossless codec"
        }
        state.bluetoothCodecUnknown -> "Bluetooth · codec unknown"
        else -> "Phone speaker"
    }

    private fun kindsOf(kind: Kind): Set<Int> = when (kind) {
        Kind.WIRED -> WIRED
        Kind.USB -> USB
        Kind.HDMI -> HDMI
        Kind.DOCK -> DOCK
        Kind.BLUETOOTH -> setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
    }

    private val SAMSUNG_NAMES = listOf("SSC", "SAMSUNG", "SCALABLE", "SEAMLESS")

    // AudioDeviceInfo constants, by value where a type arrived after the app's minimum API, so
    // a phone that predates it simply never reports it.
    private val WIRED = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_LINE_DIGITAL,
        AudioDeviceInfo.TYPE_AUX_LINE
    )
    private val USB = setOf(
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY
    )
    private val HDMI = setOf(
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        29 // AudioDeviceInfo.TYPE_HDMI_EARC, API 31
    )
    private val DOCK = setOf(
        AudioDeviceInfo.TYPE_DOCK,
        31 // AudioDeviceInfo.TYPE_DOCK_ANALOG, API 34
    )
}
