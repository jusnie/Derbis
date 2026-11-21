package com.yasuda.derbis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AppleMusicAudioBadgeTest {

    @Test
    fun atmosOnlyForJointObjectCodingStreams() {
        assertEquals(
            "Dolby Atmos",
            appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "E-AC-3", mimeType = "audio/eac3-joc", channels = 6))
        )
        assertEquals(
            "E-AC-3 · High",
            appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "E-AC-3", bitrateKbps = 640, mimeType = "audio/eac3"))
        )
    }

    @Test
    fun losslessTiersFollowTheSampleRate() {
        assertEquals(
            "FLAC · Lossless",
            appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "FLAC", sampleRateHz = 44_100, mimeType = "audio/flac"))
        )
        assertEquals(
            "FLAC · Hi-Fi",
            appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "FLAC", sampleRateHz = 96_000, mimeType = "audio/flac"))
        )
        assertEquals(
            "FLAC · Hi-Fi",
            appleMusicAudioBadgeLabel(
                PlayerAudioSpec(codec = "FLAC", sampleRateHz = 44_100, mimeType = "audio/flac", quality = "HI-FI")
            )
        )
    }

    @Test
    fun lossyStreamsMapToLowMediumAndHighBands() {
        assertEquals(
            "AAC · Low",
            appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "AAC", bitrateKbps = 64, mimeType = "audio/mp4a-latm"))
        )
        assertEquals(
            "AAC · Medium",
            appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "AAC", bitrateKbps = 128, mimeType = "audio/mp4a-latm"))
        )
        assertEquals(
            "AAC · Medium",
            appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "AAC", bitrateKbps = 160, mimeType = "audio/mp4a-latm"))
        )
        assertEquals(
            "AAC · High",
            appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "AAC", bitrateKbps = 320, mimeType = "audio/mp4a-latm", quality = "HI-FI"))
        )
        assertEquals("", appleMusicAudioBadgeLabel(PlayerAudioSpec()))
    }
}
