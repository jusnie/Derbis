package com.luc4n3x.derbis.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticBackupPolicyTest {
    @Test
    fun retentionKeepsNewestMatchingArchivesOnly() {
        val result = automaticBackupFilesToDelete(
            listOf(
                "derbis-auto-backup-100.derbis",
                "notes.txt",
                "derbis-auto-backup-300.derbis",
                "derbis-auto-backup-200.zip",
                "other-archive-500.derbis"
            ),
            retentionCount = 2
        )

        assertEquals(listOf("derbis-auto-backup-100.derbis"), result)
    }

    @Test
    fun retentionAcceptsThreeFiveTen() {
        val files = (1L..12L).map { "derbis-auto-backup-${1700000000000L + it}.derbis" }
        assertEquals(3, files.size - automaticBackupFilesToDelete(files, 3).size)
        assertEquals(5, files.size - automaticBackupFilesToDelete(files, 5).size)
        assertEquals(10, files.size - automaticBackupFilesToDelete(files, 10).size)
    }

    @Test
    fun automaticBackupNameMatchesDerbisNormalizedAndLegacyZip() {
        assertTrue(isAutomaticBackupName("derbis-auto-backup-1700000000000.derbis"))
        assertTrue(isAutomaticBackupName("derbis-auto-backup-1700000000000.derbis.zip"))
        assertTrue(isAutomaticBackupName("derbis-auto-backup-1700000000000.zip"))
        assertFalse(isAutomaticBackupName("derbis-auto-backup-.derbis"))
        assertFalse(isAutomaticBackupName("derbis-auto-backup-abc.derbis"))
        assertFalse(isAutomaticBackupName("derbis-auto-backup-1700000000000.txt"))
        assertFalse(isAutomaticBackupName("Derbis_2026-08-30_1255.derbis"))
        assertFalse(isAutomaticBackupName("../derbis-auto-backup-1.derbis"))
    }

    @Test
    fun retentionCoversProviderNormalizedNames() {
        val files = listOf(
            "derbis-auto-backup-1700000000010.derbis.zip",
            "derbis-auto-backup-1700000000020.derbis",
            "derbis-auto-backup-1700000000030.zip",
            "notes.txt",
            "Derbis_2026-08-30_1255.derbis"
        )
        val deleted = automaticBackupFilesToDelete(files, retentionCount = 2)
        assertEquals(listOf("derbis-auto-backup-1700000000010.derbis.zip"), deleted)
    }

    @Test
    fun vaultEntriesRejectTraversalAndUnknownNames() {
        assertTrue(vaultEntryAllowed("manifest.json"))
        assertTrue(vaultEntryAllowed("data/settings.json"))
        assertTrue(vaultEntryAllowed("data/favorites.json"))
        assertTrue(vaultEntryAllowed("data/followed_artists.json"))
        assertTrue(vaultEntryAllowed("data/playlists.json"))
        assertTrue(vaultEntryAllowed("data/history.json"))
        assertTrue(vaultEntryAllowed("data/downloads.json"))
        assertTrue(vaultEntryAllowed("data/queue.json"))
        assertTrue(vaultEntryAllowed("data/library_organization.json"))
        assertTrue(vaultEntryAllowed("payload.json"))
        assertFalse(vaultEntryAllowed("../manifest.json"))
        assertFalse(vaultEntryAllowed("/etc/passwd"))
        assertFalse(vaultEntryAllowed("data/../../secret.json"))
        assertFalse(vaultEntryAllowed("..\\manifest.json"))
        assertFalse(vaultEntryAllowed("data/evil.json"))
        assertFalse(vaultEntryAllowed(""))
    }

    @Test
    fun everyWrittenVaultSectionIsAcceptedOnRead() {
        val written = setOf(
            DerbisBackupManager.MANIFEST_ENTRY,
            DerbisBackupManager.SETTINGS_ENTRY,
            DerbisBackupManager.FAVORITES_ENTRY,
            DerbisBackupManager.FOLLOWED_ARTISTS_ENTRY,
            DerbisBackupManager.PLAYLISTS_ENTRY,
            DerbisBackupManager.ORGANIZATION_ENTRY,
            DerbisBackupManager.HISTORY_ENTRY,
            DerbisBackupManager.DOWNLOADS_ENTRY,
            DerbisBackupManager.QUEUE_ENTRY
        )
        written.forEach { entry ->
            assertTrue("$entry is written but rejected on read", vaultEntryAllowed(entry))
        }
        assertTrue(written.size <= DerbisBackupManager.MAX_ZIP_ENTRIES)
    }

    @Test
    fun optionalVaultSectionsAreNotRequiredForRestore() {
        val legacyEntries = REQUIRED_VAULT_ENTRIES + DerbisBackupManager.MANIFEST_ENTRY
        assertTrue(vaultStructureCompatible(legacyEntries))
        assertFalse(DerbisBackupManager.ORGANIZATION_ENTRY in REQUIRED_VAULT_ENTRIES)
        assertFalse(DerbisBackupManager.DOWNLOADS_ENTRY in REQUIRED_VAULT_ENTRIES)
    }

    @Test
    fun vaultFileNameUsesDerbisPrefixAndExtension() {
        val name = derbisVaultFileName(0L)
        assertTrue(name.startsWith("Derbis_"))
        assertTrue(name.endsWith(".derbis"))
        assertFalse(name.contains(" "))
    }

    @Test
    fun missingRequiredVaultSectionIsRejected() {
        val complete = setOf(
            "manifest.json",
            "data/settings.json",
            "data/favorites.json",
            "data/followed_artists.json",
            "data/playlists.json",
            "data/history.json",
            "data/queue.json"
        )
        assertEquals(emptySet<String>(), missingRequiredVaultSections(complete))
        assertEquals(setOf("data/playlists.json"), missingRequiredVaultSections(complete - "data/playlists.json"))
        assertEquals(
            setOf("data/favorites.json", "data/queue.json"),
            missingRequiredVaultSections(complete - "data/favorites.json" - "data/queue.json")
        )
    }

    @Test
    fun previewStructuralCompatibilityMatchesRequiredSections() {
        val complete = setOf(
            "data/settings.json",
            "data/favorites.json",
            "data/followed_artists.json",
            "data/playlists.json",
            "data/history.json",
            "data/queue.json"
        )
        assertTrue(vaultStructureCompatible(complete))
        assertFalse(vaultStructureCompatible(complete - "data/playlists.json"))
        assertFalse(vaultStructureCompatible(complete - "data/favorites.json" - "data/queue.json"))
    }
}
