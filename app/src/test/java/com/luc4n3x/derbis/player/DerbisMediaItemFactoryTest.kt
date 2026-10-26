package com.luc4n3x.derbis.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DerbisMediaItemFactoryTest {
    @Test
    fun detectsHlsAcrossSupportedUrlForms() {
        assertEquals("application/x-mpegURL", DerbisMediaItemFactory.mimeTypeFor("https://example.com/live.m3u8", true))
        assertEquals("application/x-mpegURL", DerbisMediaItemFactory.mimeTypeFor("https://example.com/videoplayback?mime=application%2Fx-mpegurl", true))
        assertEquals("application/x-mpegURL", DerbisMediaItemFactory.mimeTypeFor("https://example.com/videoplayback?mime=application/vnd.apple.mpegurl", true))
    }

    @Test
    fun detectsDashAndDirectVideoContainers() {
        assertEquals("application/dash+xml", DerbisMediaItemFactory.mimeTypeFor("https://example.com/manifest.mpd", true))
        assertEquals("video/webm", DerbisMediaItemFactory.mimeTypeFor("https://example.com/videoplayback?mime=video%2Fwebm", true))
        assertEquals("video/mp4", DerbisMediaItemFactory.mimeTypeFor("https://example.com/videoplayback?mime=video%2Fmp4", true))
    }

    @Test
    fun localContentUriUsesExtractorSniffing() {
        assertNull(DerbisMediaItemFactory.mimeTypeFor("content://media/external/audio/media/42", false))
        assertNull(DerbisMediaItemFactory.mimeTypeFor("file:///storage/emulated/0/Music/download", false))
    }

    @Test
    fun liveRadioOnlyForcesManifestMimeTypes() {
        assertNull(DerbisMediaItemFactory.liveRadioMimeTypeFor("https://radio.example/live"))
        assertNull(DerbisMediaItemFactory.liveRadioMimeTypeFor("https://radio.example/live.mp3"))
        assertEquals(
            "application/x-mpegURL",
            DerbisMediaItemFactory.liveRadioMimeTypeFor("https://radio.example/live.m3u8")
        )
    }

    @Test
    fun jioSaavnAudioOnlyMp4NeverBecomesVideoInNormalPlayback() {
        assertEquals(
            "audio/mp4",
            DerbisMediaItemFactory.mimeTypeFor("https://aac.saavncdn.com/820/abcdef_320.mp4", false)
        )
        assertEquals(
            "audio/mp4",
            DerbisMediaItemFactory.mimeTypeFor(
                "https://web.saavncdn.com/820/abcdef_320.mp4?Expires=4102444800&Signature=abc",
                false
            )
        )
    }

    @Test
    fun realVideoRemainsVideoRegardlessOfMp4Extension() {
        assertEquals("video/mp4", DerbisMediaItemFactory.mimeTypeFor("https://example.com/clip_320.mp4", true))
        assertEquals(
            "video/mp4",
            DerbisMediaItemFactory.mimeTypeFor("https://example.com/videoplayback?mime=video%2Fmp4", false)
        )
    }

    @Test
    fun aacAndM4aStayAudioAlongsideJioSaavnMp4() {
        assertEquals("audio/mp4", DerbisMediaItemFactory.mimeTypeFor("https://example.com/track.m4a", false))
        assertEquals("audio/mp4", DerbisMediaItemFactory.mimeTypeFor("https://example.com/track.aac", false))
        assertEquals(
            "audio/mp4",
            DerbisMediaItemFactory.mimeTypeFor("https://example.com/videoplayback?mime=audio%2Fmp4", false)
        )
    }
}
