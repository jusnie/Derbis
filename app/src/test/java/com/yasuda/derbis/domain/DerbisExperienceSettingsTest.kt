package com.yasuda.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisExperienceSettingsTest {
    @Test
    fun interfaceSettingsClampGestureValues() {
        val normalized = DerbisInterfaceSettings(
            doubleTapSeekSeconds = 120,
            longPressSpeed = 8f
        ).normalized()

        assertEquals(30, normalized.doubleTapSeekSeconds)
        assertEquals(3f, normalized.longPressSpeed)
    }

    @Test
    fun gestureActionsRestoreCaseInsensitivelyAndRejectUnknownValuesSafely() {
        assertEquals(PlayerDoubleTapAction.PlayPause, PlayerDoubleTapAction.from("playpause"))
        assertEquals(PlayerLongPressAction.Queue, PlayerLongPressAction.from("QUEUE"))
        assertEquals(PlayerVerticalSwipeAction.Volume, PlayerVerticalSwipeAction.from("volume"))
        assertEquals(PlayerDoubleTapAction.Seek, PlayerDoubleTapAction.from("unknown"))
        assertEquals(PlayerLongPressAction.Speed, PlayerLongPressAction.from("unknown"))
        assertEquals(PlayerVerticalSwipeAction.BrightnessAndVolume, PlayerVerticalSwipeAction.from("unknown"))
    }

    @Test
    fun downloadSettingsClampConcurrency() {
        assertEquals(1, DerbisDownloadSettings(maxConcurrentDownloads = 0).normalized().maxConcurrentDownloads)
        assertEquals(4, DerbisDownloadSettings(maxConcurrentDownloads = 12).normalized().maxConcurrentDownloads)
    }

    @Test
    fun downloadSettingsNormalizeDestinationTreeUri() {
        val normalized = DerbisDownloadSettings(
            destinationTreeUri = "  content://com.android.externalstorage.documents/tree/1234-5678%3AMusic  "
        ).normalized()

        assertEquals(
            "content://com.android.externalstorage.documents/tree/1234-5678%3AMusic",
            normalized.destinationTreeUri
        )
        assertEquals("", DerbisDownloadSettings().normalized().destinationTreeUri)
    }

    @Test
    fun downloadPresetsSelectIndependentOfflineQuality() {
        assertEquals("High", DerbisDownloadSettings(preset = DerbisDownloadPreset.HighQuality).resolverAudioQuality)
        assertEquals("Low", DerbisDownloadSettings(preset = DerbisDownloadPreset.DataSaver).resolverAudioQuality)
        assertEquals(null, DerbisDownloadSettings(preset = DerbisDownloadPreset.Automatic).resolverAudioQuality)
    }

    @Test
    fun downloadProfilesPersistPresetAndRequestedQuality() {
        val automatic = DerbisDownloadSettings(preset = DerbisDownloadPreset.Automatic)
        val high = DerbisDownloadSettings(preset = DerbisDownloadPreset.HighQuality)
        val saver = DerbisDownloadSettings(preset = DerbisDownloadPreset.DataSaver)

        assertEquals("Automatic", automatic.storedPresetKey)
        assertEquals("Medium", automatic.storedQualityKey("Medium"))
        assertEquals("Auto", automatic.storedQualityKey())
        assertEquals("HighQuality", high.storedPresetKey)
        assertEquals("High", high.storedQualityKey("Medium"))
        assertEquals("DataSaver", saver.storedPresetKey)
        assertEquals("Low", saver.storedQualityKey("Medium"))
    }

    @Test
    fun duplicatePolicyHonorsSkipExistingSetting() {
        val downloaded = setOf("track-1")

        assertTrue(DerbisDownloadSettings(skipExisting = true).shouldSkipExistingDownload("track-1", downloaded))
        assertFalse(DerbisDownloadSettings(skipExisting = false).shouldSkipExistingDownload("track-1", downloaded))
        assertFalse(DerbisDownloadSettings(skipExisting = true).shouldSkipExistingDownload("", downloaded))
        assertFalse(DerbisDownloadSettings(skipExisting = true).shouldSkipExistingDownload("track-2", downloaded))
    }

    @Test
    fun downloadSettingsNormalizeRateAndParallelism() {
        val invalid = DerbisDownloadSettings(maxRateKbps = 777).normalized()
        val dataSaver = DerbisDownloadSettings(preset = DerbisDownloadPreset.DataSaver).normalized()
        val highQuality = DerbisDownloadSettings(preset = DerbisDownloadPreset.HighQuality, maxRateKbps = 4096).normalized()

        assertEquals(0, invalid.maxRateKbps)
        assertEquals(0, dataSaver.effectiveRateKbps)
        assertEquals(16, dataSaver.maxParallelFragments)
        assertEquals(4096, highQuality.effectiveRateKbps)
        assertEquals(24, highQuality.maxParallelFragments)
    }
}
