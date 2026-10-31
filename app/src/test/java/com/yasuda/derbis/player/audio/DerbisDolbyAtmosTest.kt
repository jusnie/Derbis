package com.yasuda.derbis.player.audio

import com.yasuda.derbis.ui.player.technicalDolbyAtmosLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisDolbyAtmosTest {

    @Test
    fun jocIsTheOnlyDolbyStreamThatIsAtmos() {
        assertTrue(isDolbyAtmosStream("audio/eac3-joc", null))
        assertTrue(isDolbyAtmosStream("audio/eac3-joc", "ec+3"))
        assertTrue(isDolbyAtmosStream("audio/eac3", "ec+3.2"))
        assertTrue(isDolbyAtmosStream(null, "joc"))
        // Same codec family without the height objects: a 5.1 render, not an Atmos one.
        assertFalse(isDolbyAtmosStream("audio/eac3", "ec-3"))
        assertFalse(isDolbyAtmosStream("audio/eac3", null))
        assertFalse(isDolbyAtmosStream("audio/ac3", "ac-3"))
        assertFalse(isDolbyAtmosStream("audio/opus", "opus"))
        assertFalse(isDolbyAtmosStream(null, null))
    }

    @Test
    fun statusFoldsThePreferenceAndTheDeviceTogether() {
        assertEquals(DerbisAtmosStatus.WANTED, derbisAtmosStatus(decoderAvailable = true, enabled = true))
        assertEquals(DerbisAtmosStatus.DISABLED, derbisAtmosStatus(decoderAvailable = true, enabled = false))
        // No decoder loses even when the switch is on: that combination is the silent one.
        assertEquals(DerbisAtmosStatus.NO_DECODER, derbisAtmosStatus(decoderAvailable = false, enabled = true))
        assertEquals(DerbisAtmosStatus.NO_DECODER, derbisAtmosStatus(decoderAvailable = false, enabled = false))
    }

    @Test
    fun everyStatusExplainsItself() {
        assertEquals("decoder available", derbisAtmosReason(DerbisAtmosStatus.WANTED))
        assertTrue(derbisAtmosReason(DerbisAtmosStatus.DISABLED).contains("Settings"))
        assertTrue(derbisAtmosReason(DerbisAtmosStatus.NO_DECODER).contains("decoder"))
    }

    @Test
    fun technicalRowSaysWhyNot() {
        assertEquals("Yes · E-AC-3 decoder available", technicalDolbyAtmosLabel(enabled = true, decoderAvailable = true))
        assertEquals("Off · switched off in Settings", technicalDolbyAtmosLabel(enabled = false, decoderAvailable = true))
        assertEquals(
            "No · no E-AC-3 decoder on this phone",
            technicalDolbyAtmosLabel(enabled = true, decoderAvailable = false)
        )
    }

    @Test
    fun forcedProbeAnswersWithoutTouchingTheCodecList() {
        try {
            DerbisDolbyAtmos.forced = false
            assertFalse(DerbisDolbyAtmos.decoderAvailable)
            DerbisDolbyAtmos.forced = true
            assertTrue(DerbisDolbyAtmos.decoderAvailable)
        } finally {
            DerbisDolbyAtmos.reset()
        }
    }
}
