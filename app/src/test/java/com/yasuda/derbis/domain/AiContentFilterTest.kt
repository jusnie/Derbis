package com.yasuda.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiContentFilterTest {
    @Test
    fun blocksOnlyExplicitAiDisclosureOrKnownGeneratorMetadata() {
        assertTrue(AiContentFilter.blocks(track(title = "AI generated lullaby")))
        assertTrue(AiContentFilter.blocks(track(artist = "Suno AI")))
        assertTrue(AiContentFilter.blocks(track(metadataProvider = "Udio")))
        assertFalse(AiContentFilter.blocks(track(title = "Artificial Flowers", artist = "Human Artist")))
    }

    @Test
    fun removesBlockedTracksOnlyWhenEnabled() {
        val human = track(title = "Human song")
        val generated = track(title = "Made with AI")

        assertEquals(listOf(human), AiContentFilter.filter(listOf(human, generated), enabled = true))
        assertEquals(listOf(human, generated), AiContentFilter.filter(listOf(human, generated), enabled = false))
    }

    private fun track(
        title: String = "Track",
        artist: String = "Artist",
        metadataProvider: String = ""
    ) = Track(
        id = title,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 1_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        metadataProvider = metadataProvider
    )
}
