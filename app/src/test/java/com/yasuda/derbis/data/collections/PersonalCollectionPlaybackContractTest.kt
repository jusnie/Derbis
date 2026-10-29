package com.yasuda.derbis.data.collections

import com.yasuda.derbis.data.local.PersonalCollectionTrackEntity
import com.yasuda.derbis.player.DerbisMediaItemFactory
import com.yasuda.derbis.player.DerbisPlaybackCacheKey
import com.yasuda.derbis.player.offline.isPersonalCollectionStreamingOnly
import com.yasuda.derbis.player.offline.personalCollectionDownloadExtension
import com.yasuda.derbis.ui.i18n.DerbisPersonalCollectionCatalog
import com.yasuda.derbis.ui.i18n.DerbisPersonalCollectionStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalCollectionPlaybackContractTest {

    private val row = PersonalCollectionTrackEntity(
        collectionId = "c1",
        entryKey = "uabc",
        trackId = "pc:c1:uabc",
        position = 0,
        title = "Song",
        artist = "Artist",
        album = "Album",
        albumArtist = "Artist",
        durationMs = 180_000L,
        audioUrl = "https://alice@cdn.example.com/a.flac",
        videoUrl = "",
        artworkUrl = "https://cdn.example.com/a.jpg",
        artworkFile = "",
        trackNumber = 3,
        discNumber = 1,
        year = 2001,
        addedAt = 1L
    )

    @Test
    fun catalogTrackNeverCarriesTheRemoteUrl() {
        val track = row.toTrack()
        assertEquals("pc:c1:uabc", track.id)
        assertEquals(PERSONAL_COLLECTION_SOURCE, track.source)
        assertEquals("", track.streamUrl)
        assertEquals("", track.videoUrl)
        assertEquals("", track.thumbnailUrl)
        assertTrue(track.isPersonalCollectionTrack())
        assertEquals("2001", track.year)
        assertEquals(3, track.trackNumber)
    }

    @Test
    fun onlyResolvedRemoteStreamsAreRoutedToTheCollectionDataSource() {
        val track = row.toTrack()
        assertFalse(DerbisMediaItemFactory.isPersonalCollectionStream(track))
        assertTrue(DerbisMediaItemFactory.isPersonalCollectionStream(track.copy(streamUrl = "https://cdn.example.com/a.flac")))
        assertTrue(DerbisMediaItemFactory.isPersonalCollectionStream(track.copy(streamUrl = "http://nas.local/a.flac")))
        assertFalse(DerbisMediaItemFactory.isPersonalCollectionStream(track.copy(streamUrl = "content://media/external/audio/media/9", source = "Offline")))
        assertFalse(DerbisMediaItemFactory.isPersonalCollectionStream(track.copy(id = "dQw4w9WgXcQ", streamUrl = "https://rr1.googlevideo.com/x")))
    }

    @Test
    fun mimeTypesFollowTheRealContainerAndUnknownsAreSniffed() {
        assertEquals("audio/flac", DerbisMediaItemFactory.personalCollectionMimeTypeFor("https://h/a.FLAC?sig=1"))
        assertEquals("audio/ogg", DerbisMediaItemFactory.personalCollectionMimeTypeFor("https://h/a.opus"))
        assertEquals("audio/mpeg", DerbisMediaItemFactory.personalCollectionMimeTypeFor("https://h/a.mp3#t=1"))
        assertEquals("audio/mp4", DerbisMediaItemFactory.personalCollectionMimeTypeFor("https://h/a.m4a"))
        assertNull(DerbisMediaItemFactory.personalCollectionMimeTypeFor("https://h/a.aac"))
        assertEquals("application/x-mpegURL", DerbisMediaItemFactory.personalCollectionMimeTypeFor("https://h/live/index.m3u8"))
        assertNull(DerbisMediaItemFactory.personalCollectionMimeTypeFor("https://h/stream?id=4"))
    }

    @Test
    fun cacheKeysAreIsolatedPerTrackAndPerUrl() {
        val track = row.toTrack().copy(streamUrl = "https://cdn.example.com/a.flac")
        val key = DerbisPlaybackCacheKey.personalCollection(track)
        assertTrue(key.startsWith("derbis:pc:"))
        assertFalse(key.contains("cdn.example.com"))
        assertNotEquals(key, DerbisPlaybackCacheKey.personalCollection(track.copy(streamUrl = "https://cdn.example.com/b.flac")))
        assertNotEquals(key, DerbisPlaybackCacheKey.personalCollection(track.copy(id = "pc:c2:uabc")))
    }

    @Test
    fun downloadsAcceptOnlyRealAudioResponses() {
        assertEquals("mp3", personalCollectionDownloadExtension("audio/mpeg", "/a"))
        assertEquals("flac", personalCollectionDownloadExtension("application/octet-stream", "/music/a.flac"))
        assertEquals("opus", personalCollectionDownloadExtension("audio/ogg; codecs=opus", "/a.opus"))
        assertEquals("m4a", personalCollectionDownloadExtension("audio/x-m4a", "/a"))
        assertNull(personalCollectionDownloadExtension("text/html; charset=utf-8", "/a.mp3"))
        assertNull(personalCollectionDownloadExtension("application/json", "/a.mp3"))
        assertNull(personalCollectionDownloadExtension("application/octet-stream", "/a.exe"))
        assertTrue(isPersonalCollectionStreamingOnly("https://h/live/index.m3u8?token=1"))
        assertTrue(isPersonalCollectionStreamingOnly("https://h/manifest.mpd"))
        assertFalse(isPersonalCollectionStreamingOnly("https://h/a.mp3"))
    }

    @Test
    fun everyLanguageHasEveryStringWithMatchingPlaceholders() {
        val placeholder = Regex("\\{[a-z]+\\}")
        val english = DerbisPersonalCollectionCatalog.rawValues("en")
        assertEquals(DerbisPersonalCollectionStrings.KEY_COUNT, english.size)
        DerbisPersonalCollectionCatalog.codes().forEach { code ->
            val values = DerbisPersonalCollectionCatalog.rawValues(code)
            assertEquals(code, DerbisPersonalCollectionStrings.KEY_COUNT, values.size)
            values.forEachIndexed { index, value ->
                assertTrue("$code[$index] blank", value.isNotBlank())
                assertEquals(
                    "$code[$index] placeholders",
                    placeholder.findAll(english[index]).map { it.value }.toSet(),
                    placeholder.findAll(value).map { it.value }.toSet()
                )
            }
        }
        assertEquals(41, DerbisPersonalCollectionCatalog.codes().size)
        assertEquals(DerbisPersonalCollectionStrings.ERROR_COUNT, CollectionSyncError.entries.size)
    }

    @Test
    fun formattedStringsFillPlaceholdersAndIsolateRtlNumbers() {
        val english = DerbisPersonalCollectionCatalog.forCode("en")
        assertEquals("Skipped entries: 3", english.skipped(3))
        assertEquals("2 added · 1 removed", english.syncSummary(2, 1))
        assertEquals("Remove “Jazz”?", english.removeTitle("Jazz"))
        CollectionSyncError.entries.forEach { error -> assertTrue(english.error(error).isNotBlank()) }
        val arabic = DerbisPersonalCollectionCatalog.forCode("ar")
        assertTrue(arabic.skipped(4).contains("⁨"))
        assertEquals("en", DerbisPersonalCollectionCatalog.forCode("xx").code)
        assertEquals("it", DerbisPersonalCollectionCatalog.forCode("it-IT").code)
    }
}
