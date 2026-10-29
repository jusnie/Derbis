package com.yasuda.derbis.data.playlistimport

import com.yasuda.derbis.data.YoutubeMusicRepository
import com.yasuda.derbis.domain.SearchPage
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.nexus.playlistimport.ImportedTrackIdentity
import com.yasuda.derbis.nexus.playlistimport.ResolutionPreference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImportDurationCheckTest {
    private val upload = Track(
        id = "upload", title = "Kadebostany - Mind If I Stay (Astero Remix)", artist = "Soul Music", album = "",
        durationMs = 0L, streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "test",
        moodTags = emptySet(), energy = 50, vocal = 50, replayScore = 50, cacheScore = 50, accentStart = 0, accentEnd = 0,
        videoType = "MUSIC_VIDEO_TYPE_UGC"
    )

    private val repository = object : YoutubeMusicRepository(null) {
        override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> =
            SearchPage(emptyList(), "")

        override suspend fun searchForImport(query: String, limit: Int, languageCode: String): List<Track> = emptyList()
    }

    private val identity = ImportedTrackIdentity(0, "Mind If I Stay (Astero Remix)", listOf("Kadebostany"),
        album = "Monumental: Chapter II", durationMs = 192_000)

    private fun catalog(duration: Long, lookups: MutableList<String>) = PlaylistImportCatalog(
        repository, { emptyList() }, { "en" },
        youtubeSearch = { listOf(upload) },
        durationLookup = { videoId, _ -> lookups += videoId; duration }
    )

    @Test
    fun matchingLengthConfirmsAnUploadWithoutReportedLength() = runBlocking {
        val lookups = mutableListOf<String>()
        val outcome = catalog(192_500L, lookups).resolve(identity, false, ResolutionPreference.SMART).outcome
        assertTrue(outcome.confidence.autoAccepted)
        assertEquals("upload", outcome.selected?.candidate?.id)
        assertEquals(listOf("upload"), lookups)
    }

    @Test
    fun differentLengthKeepsTheUploadInReview() = runBlocking {
        val outcome = catalog(230_000L, mutableListOf()).resolve(identity, false, ResolutionPreference.SMART).outcome
        assertFalse(outcome.confidence.autoAccepted)
    }
}
