package com.yasuda.derbis.data

import com.yasuda.derbis.domain.DerbisAudioSettings
import com.yasuda.derbis.domain.DerbisBackupSettings
import com.yasuda.derbis.domain.DerbisDownloadSettings
import com.yasuda.derbis.domain.DerbisInterfaceSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class DerbisPreferencesJamDisplayNameTest {

    @Test
    fun normalizeJamDisplayNameTrimsAndEnforcesMaxLength() {
        assertEquals("Luca", normalizeJamDisplayName("  Luca  "))
        assertEquals(32, normalizeJamDisplayName("a".repeat(50)).length)
        assertEquals("a".repeat(32), normalizeJamDisplayName("a".repeat(50)))
        assertEquals("", normalizeJamDisplayName("   "))
    }

    @Test
    fun preferencesSnapshotIncludesJamDisplayName() {
        val snapshot = DerbisPreferencesSnapshot(
            onboarded = true,
            tastes = emptySet(),
            userName = "TestUser",
            languageCode = "en",
            animationsEnabled = true,
            motionArtworkEnabled = true,
            dynamicColor = true,
            sponsorBlock = true,
            skipSilence = false,
            audioQuality = "Auto",
            dismissedUpdateVersion = "",
            lastTrack = null,
            lastPositionMs = 0L,
            recentSearches = emptyList(),
            personalOrbitTracks = emptyList(),
            audioNormalization = false,
            lyricsTranslationEnabled = false,
            themePreset = "apple_music",
            audioSettings = DerbisAudioSettings(),
            interfaceSettings = DerbisInterfaceSettings(),
            downloadSettings = DerbisDownloadSettings(),
            backupSettings = DerbisBackupSettings(),
            jamDisplayName = "DJ Luca"
        )
        assertEquals("DJ Luca", snapshot.jamDisplayName)
    }
}
