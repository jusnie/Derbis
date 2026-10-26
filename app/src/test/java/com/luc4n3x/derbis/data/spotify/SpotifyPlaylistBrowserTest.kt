package com.luc4n3x.derbis.data.spotify

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyPlaylistBrowserTest {

    @Test
    fun signedInLibraryUsesPlaylistMetadataAndArtwork() {
        val page = parseSpotifyPlaylistLibraryPage(
            JSONObject(
                """
                {
                  "offset": 0,
                  "limit": 50,
                  "total": 1,
                  "next": null,
                  "items": [
                    {
                      "id": "37i9dQZF1DXcBWIGoYBM5M",
                      "name": "Today's Top Hits",
                      "description": "Big songs right now",
                      "owner": { "display_name": "Spotify" },
                      "images": [{ "url": "https://image.example/top.jpg" }],
                      "items": { "total": 50 },
                      "external_urls": { "spotify": "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M" }
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        assertEquals(1, page.playlists.size)
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", page.playlists.single().id)
        assertEquals("Today's Top Hits", page.playlists.single().title)
        assertEquals("Spotify", page.playlists.single().owner)
        assertEquals(50, page.playlists.single().trackCount)
        assertEquals("https://image.example/top.jpg", page.playlists.single().artworkUrl)
        assertNull(page.nextOffset)
    }

    @Test
    fun signedInLibraryUsesSpotifyPaginationForFollowedOrOwnedPlaylists() {
        val page = parseSpotifyPlaylistLibraryPage(
            json = JSONObject(
                """
                {
                  "offset": 50,
                  "limit": 50,
                  "total": 137,
                  "next": "https://api.spotify.com/v1/me/playlists?offset=100&limit=50",
                  "items": [
                    {
                      "id": "37i9dQZF1DXcBWIGoYBM5M",
                      "name": "A library playlist",
                      "owner": { "display_name": "Someone else" },
                      "images": []
                    }
                  ]
                }
                """.trimIndent()
            ),
            requestedOffset = 50,
            requestedLimit = 50
        )

        assertEquals(1, page.playlists.size)
        assertEquals("Someone else", page.playlists.single().owner)
        assertEquals(137, page.total)
        assertEquals(100, page.nextOffset)
    }

    @Test
    fun signedInLibraryStopsWhenSpotifyDoesNotProvideAnotherPage() {
        val page = parseSpotifyPlaylistLibraryPage(
            JSONObject("""{ "offset": 0, "limit": 50, "total": 1, "next": null, "items": [] }""")
        )

        assertNull(page.nextOffset)
    }

    @Test
    fun playlistHeaderReadsCurrentItemsTotalAndConstructsFallbackLink() {
        val header = parseSpotifyPlaylistHeader(
            JSONObject(
                """
                {
                  "id": "5dN0sP0L6B9R7gM3wQ2kAa",
                  "name": "My imported songs",
                  "owner": { "display_name": "Derbis user" },
                  "images": [],
                  "items": { "total": 2 }
                }
                """.trimIndent()
            )
        )

        assertEquals(2, header.trackCount)
        assertEquals("https://open.spotify.com/playlist/5dN0sP0L6B9R7gM3wQ2kAa", header.spotifyUrl)
    }

    @Test
    fun currentPlaylistItemRowsReadItemAndSkipEpisodes() {
        val page = parseSpotifyPlaylistItems(
            JSONObject(
                """
                {
                  "total": 3,
                  "next": "https://api.spotify.com/v1/playlists/example/items?offset=50",
                  "items": [
                    {
                      "item": {
                        "type": "track",
                        "id": "6rqhFgbbKwnb9MLmUQDhG6",
                        "name": "Song A",
                        "artists": [{ "name": "Artist A" }, { "name": "Artist B" }],
                        "album": { "name": "Album A", "images": [{ "url": "https://image.example/a.jpg" }] },
                        "duration_ms": 184000,
                        "explicit": true,
                        "external_urls": { "spotify": "https://open.spotify.com/track/6rqhFgbbKwnb9MLmUQDhG6" }
                      }
                    },
                    { "item": { "type": "episode", "id": "episode" } },
                    { "item": null }
                  ]
                }
                """.trimIndent()
            )
        )

        assertEquals(3, page.total)
        assertEquals(3, page.itemCount)
        assertTrue(page.hasNext)
        assertEquals(1, page.tracks.size)
        assertEquals("Song A", page.tracks.single().title)
        assertEquals("Artist A, Artist B", page.tracks.single().artist)
        assertEquals("Album A", page.tracks.single().album)
        assertTrue(page.tracks.single().explicit)
    }

    @Test
    fun legacyTrackRowsAndEmptyNextAreHandledDefensively() {
        val page = parseSpotifyPlaylistItems(
            JSONObject(
                """
                {
                  "total": 1,
                  "next": null,
                  "items": [
                    {
                      "track": {
                        "id": "4uLU6hMCjMI75M1A2tKUQC",
                        "name": "Song B",
                        "artists": [{ "name": "Artist B" }],
                        "album": { "name": "Album B", "images": [] },
                        "duration_ms": 212000
                      }
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        assertFalse(page.hasNext)
        assertEquals(1, page.tracks.size)
        assertEquals("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC", page.tracks.single().spotifyUrl)
    }
}
