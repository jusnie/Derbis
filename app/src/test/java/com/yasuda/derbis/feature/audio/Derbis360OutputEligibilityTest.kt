package com.yasuda.derbis.feature.audio

import android.media.AudioDeviceInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Derbis360OutputEligibilityTest {
    @Test
    fun `wired and usb headsets are eligible even without a product name`() {
        assertTrue(isDerbis360HeadphoneOutput(route("", AudioDeviceInfo.TYPE_WIRED_HEADPHONES, false)))
        assertTrue(isDerbis360HeadphoneOutput(route("", AudioDeviceInfo.TYPE_USB_HEADSET, false)))
    }

    @Test
    fun `builtin and ble speakers are not eligible`() {
        assertFalse(isDerbis360HeadphoneOutput(route("Phone speaker", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, false)))
        assertFalse(isDerbis360HeadphoneOutput(route("Living room", AudioDeviceInfo.TYPE_BLE_SPEAKER, true)))
        assertFalse(isDerbis360HeadphoneOutputType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertFalse(isDerbis360HeadphoneOutputType(AudioDeviceInfo.TYPE_BLE_SPEAKER))
    }

    @Test
    fun `named bluetooth headphones are eligible while likely speakers and unknown routes are rejected`() {
        assertTrue(isDerbis360HeadphoneOutput(route("Pixel Buds", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, true)))
        assertFalse(isDerbis360HeadphoneOutput(route("JBL Speaker", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, true)))
        assertFalse(isDerbis360HeadphoneOutput(route("Bluetooth", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, true)))
    }

    private fun route(name: String, type: Int, bluetooth: Boolean) = DerbisAudioOutputRoute(
        stableKey = name,
        displayName = name,
        type = type,
        bluetooth = bluetooth
    )
}
