package com.yasuda.derbis.player.audio

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The lossless gate is pure decision logic, so it is tested without a device: what counts as a
 * lossless output, and when the sink may run a 24-bit float path.
 */
class LosslessOutputPolicyTest {

    private fun state(
        capable: Boolean = true,
        via: DerbisLosslessOutput.Kind? = DerbisLosslessOutput.Kind.USB,
        advertisesPcmFloat: Boolean = true
    ) = DerbisLosslessOutput.State(capable = capable, via = via, advertisesPcmFloat = advertisesPcmFloat)

    private fun plan(
        state: DerbisLosslessOutput.State,
        skipSilence: Boolean = false,
        manufacturer: String = "Google"
    ) = DerbisAudioOutputPolicy.plan(state, skipSilence, manufacturer)

    @Test
    fun `a wired route that advertises float runs the 24-bit path`() {
        val result = plan(state())
        assertTrue(result.floatOutput)
        assertTrue(result.bypassPcm16Stage)
    }

    @Test
    fun `skip silence keeps the PCM16 stage`() {
        val result = plan(state(), skipSilence = true)
        assertFalse(result.floatOutput)
        assertFalse(result.bypassPcm16Stage)
    }

    @Test
    fun `a route that does not advertise float keeps PCM16`() {
        val result = plan(state(advertisesPcmFloat = false))
        assertFalse(result.floatOutput)
    }

    @Test
    fun `samsung stays on PCM16 for its vendor FLAC decoder`() {
        val result = plan(state(), manufacturer = "samsung")
        assertFalse(result.floatOutput)
        assertFalse(result.bypassPcm16Stage)
    }

    @Test
    fun `bluetooth stays on PCM16 because the codec bit depth cannot be read`() {
        val result = plan(state(via = DerbisLosslessOutput.Kind.BLUETOOTH))
        assertFalse(result.floatOutput)
    }

    @Test
    fun `the phone speaker is never lossless`() {
        val state = DerbisLosslessOutput.decide(emptySet())
        assertFalse(state.capable)
        assertFalse(plan(state).floatOutput)
    }

    @Test
    fun `wired and usb outputs count as lossless`() {
        assertEquals(
            DerbisLosslessOutput.Kind.WIRED,
            DerbisLosslessOutput.decide(setOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)).via
        )
        assertEquals(
            DerbisLosslessOutput.Kind.USB,
            DerbisLosslessOutput.decide(setOf(AudioDeviceInfo.TYPE_USB_DEVICE)).via
        )
        assertEquals(
            DerbisLosslessOutput.Kind.HDMI,
            DerbisLosslessOutput.decide(setOf(AudioDeviceInfo.TYPE_HDMI_ARC)).via
        )
    }

    @Test
    fun `bluetooth without a named codec is reported unknown, not guessed`() {
        val state = DerbisLosslessOutput.decide(setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        assertFalse(state.capable)
        assertTrue(state.bluetoothCodecUnknown)
    }

    @Test
    fun `only the high bitrate bluetooth codecs count as lossless`() {
        assertTrue(DerbisLosslessOutput.isLosslessBluetoothCodec("LDAC"))
        assertTrue(DerbisLosslessOutput.isLosslessBluetoothCodec("LHDC V5"))
        assertTrue(DerbisLosslessOutput.isLosslessBluetoothCodec("aptX Lossless"))
        assertTrue(DerbisLosslessOutput.isLosslessBluetoothCodec("SSC UHQ"))
        assertFalse(DerbisLosslessOutput.isLosslessBluetoothCodec("SBC"))
        assertFalse(DerbisLosslessOutput.isLosslessBluetoothCodec("AAC"))
        assertFalse(DerbisLosslessOutput.isLosslessBluetoothCodec("aptX HD"))
        assertFalse(DerbisLosslessOutput.isLosslessBluetoothCodec("SSC", 44_100))
        assertTrue(DerbisLosslessOutput.isLosslessBluetoothCodec("SSC", 96_000))
    }

    @Test
    fun `an LDAC route is a lossless output`() {
        val state = DerbisLosslessOutput.decide(setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP), "LDAC")
        assertTrue(state.capable)
        assertEquals(DerbisLosslessOutput.Kind.BLUETOOTH, state.via)
    }
}
