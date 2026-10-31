package com.yasuda.derbis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AppleMusicAudioBadgeTest {

    @Test
    fun atmosOnlyForJointObjectCodingStreams() {
        assertEquals("Dolby Atmos", appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "E-AC-3", mimeType = "audio/eac3-joc", channels = 6)))
        assertEquals("E-AC-3 · 640 kbps", appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "E-AC-3", bitrateKbps = 640, mimeType = "audio/eac3")))
    }

    @Test
    fun losslessTiersFollowTheSampleRate() {
        assertEquals("Lossless", appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "FLAC", sampleRateHz = 44_100, mimeType = "audio/flac")))
        assertEquals("Hi-Fi", appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "FLAC", sampleRateHz = 96_000, mimeType = "audio/flac")))
        assertEquals(
            "Hi-Fi",
            appleMusicAudioBadgeLabel(
                PlayerAudioSpec(codec = "FLAC", sampleRateHz = 44_100, mimeType = "audio/flac", quality = "HI-FI")
            )
        )
    }

    @Test
    fun lossyStreamsShowTheRealCodecAndBitrate() {
        assertEquals("AAC · 160 kbps", appleMusicAudioBadgeLabel(PlayerAudioSpec(codec = "AAC", bitrateKbps = 160, mimeType = "audio/mp4a-latm")))
        assertEquals("", appleMusicAudioBadgeLabel(PlayerAudioSpec()))
    }

    @Test
    fun hqLossyUpgradeReadsHiFiInsteadOfItsCodec() {
        val upgrade = PlayerAudioSpec(codec = "AAC", bitrateKbps = 320, mimeType = "audio/mp4a-latm", quality = "HI-FI")
        assertEquals("Hi-Fi · 320 kbps", appleMusicAudioBadgeLabel(upgrade))
        val plain = PlayerAudioSpec(codec = "AAC", bitrateKbps = 128, mimeType = "audio/mp4a-latm")
        assertEquals("AAC · 128 kbps", appleMusicAudioBadgeLabel(plain))
    }
}
