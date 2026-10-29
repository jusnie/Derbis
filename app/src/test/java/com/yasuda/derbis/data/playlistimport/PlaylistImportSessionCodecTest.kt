package com.yasuda.derbis.data.playlistimport

import com.yasuda.derbis.domain.DerbisLanguageCatalog
import com.yasuda.derbis.nexus.playlistimport.CandidateKind
import com.yasuda.derbis.nexus.playlistimport.ImportEntry
import com.yasuda.derbis.nexus.playlistimport.ImportedPlaylistDescriptor
import com.yasuda.derbis.nexus.playlistimport.ImportedTrackIdentity
import com.yasuda.derbis.nexus.playlistimport.IncompleteReason
import com.yasuda.derbis.nexus.playlistimport.MatchCandidate
import com.yasuda.derbis.nexus.playlistimport.PlaylistImportCompleteness
import com.yasuda.derbis.nexus.playlistimport.PlaylistImportHealer
import com.yasuda.derbis.nexus.playlistimport.PlaylistImportReview
import com.yasuda.derbis.nexus.playlistimport.PlaylistImportSource
import com.yasuda.derbis.nexus.playlistimport.PlaylistMatchEngine
import com.yasuda.derbis.nexus.playlistimport.ResolutionPreference
import com.yasuda.derbis.ui.i18n.playlistImportHubKeys
import com.yasuda.derbis.ui.i18n.playlistImportHubLocalizationCodes
import com.yasuda.derbis.ui.i18n.playlistImportHubLocalizationEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImportSessionCodecTest {
    private fun session(): PlaylistImportSession {
        val identity = ImportedTrackIdentity(0, "Blinding Lights", listOf("The Weeknd"), durationMs = 200_000L, isrc = "USUG11904206", explicit = null)
        val candidate = MatchCandidate("abcdefghijk", "Blinding Lights", listOf("The Weeknd"), durationMs = 200_500L, kind = CandidateKind.SONG)
        val resolved = PlaylistImportReview.applyOutcome(ImportEntry(identity), PlaylistMatchEngine.select(identity, listOf(candidate)))
        val pending = ImportEntry(identity.copy(position = 1, title = "Ünïcødé ☕", artists = listOf("Ñandú")))
        return PlaylistImportSession(
            id = "0f8fad5b-d9cb-469f-a165-70867728950e",
            createdAt = 1L,
            updatedAt = 2L,
            inputHash = playlistImportInputHash("https://open.spotify.com/playlist/x"),
            inputLabel = "open.spotify.com/playlist/x",
            descriptor = ImportedPlaylistDescriptor(PlaylistImportSource.SPOTIFY, "x", "Hits", "Spotify", "", "https://i/a", 171),
            completeness = PlaylistImportCompleteness.Incomplete(100, 171, IncompleteReason.PROVIDER_PAGE_LIMIT),
            phase = PlaylistImportPhase.MATCHING,
            preference = ResolutionPreference.PREFER_LOCAL,
            playlistName = "Hits",
            entries = PlaylistImportHealer.heal(listOf(resolved, pending)).entries,
            tracks = emptyMap(),
            committedPlaylistId = "committed-id"
        )
    }

    @Test
    fun sessionRoundTripsWithoutLoss() {
        val original = session()
        val decoded = PlaylistImportSessionCodec.decode(org.json.JSONObject(PlaylistImportSessionCodec.encode(original).toString()))
        assertEquals(original, decoded)
    }

    @Test
    fun inputHashIsStableAndTrimmed() {
        assertEquals(playlistImportInputHash("abc"), playlistImportInputHash("  abc \n"))
        assertNotEquals(playlistImportInputHash("abc"), playlistImportInputHash("abd"))
    }

    @Test
    fun importHubCoversEveryCatalogLanguage() {
        val codes = DerbisLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(codes, playlistImportHubLocalizationCodes())
        val english = playlistImportHubLocalizationEntries("en")
        codes.forEach { code ->
            val entries = playlistImportHubLocalizationEntries(code)
            assertEquals(playlistImportHubKeys.toSet(), entries.keys)
            entries.forEach { (key, value) -> assertTrue("$code/$key is blank", value.isNotBlank()) }
            if (code != "en") assertNotEquals("$code falls back to English", english, entries)
            listOf("detected" to "{source}", "importAction" to "{count}", "readyAttention" to "{attention}").forEach { (key, placeholder) ->
                assertTrue("$code/$key lacks $placeholder", entries.getValue(key).contains(placeholder))
            }
        }
    }
}
