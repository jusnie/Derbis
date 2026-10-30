package com.yasuda.derbis.feature.motion

import android.content.Context
import com.yasuda.derbis.data.EditorialChartsRepository
import com.yasuda.derbis.data.network.DerbisHttpClientFactory
import com.yasuda.derbis.data.spotify.SpotifySearchClient
import com.yasuda.derbis.domain.SearchResults
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Resolves official Spotify Canvas looping video URLs using the public CanvasStore / Spicy Lyrics network:
 * Endpoint: https://regional.canvasstore.slnode.spicylyrics.org/provider/canvaslist/{trackId}
 *
 * Returns official Spotify CDN looping Canvas MP4s (canvaz.scdn.co/...) without requiring
 * private user cookies or authentication tokens.
 */
class SpotifyCanvasProvider internal constructor(
    private val client: OkHttpClient,
    private val searchSpotify: suspend (String, Int) -> SearchResults
) : MotionArtworkProvider {
    constructor(context: Context) : this(
        client = DerbisHttpClientFactory.media(context.applicationContext).newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build(),
        searchSpotify = catalogSearch(context)
    )

    override val id: String = PROVIDER_ID

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult = withContext(Dispatchers.IO) {
        try {
            val resolvedIdentity = resolveIdentity(identity)
                ?: return@withContext MotionArtworkProviderResult.NoMatch
            val resolvedSpotifyId = spotifyTrackId(resolvedIdentity.trackId)
                ?: return@withContext MotionArtworkProviderResult.NoMatch
            val url = "$CANVAS_ENDPOINT/$resolvedSpotifyId".toHttpUrlOrNull()
                ?: return@withContext MotionArtworkProviderResult.NoMatch
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "YasudaMusic/1.0 (Android; SpotifyCanvasProvider)")
                .build()

            awaitMotionArtworkResponse(client.newCall(request)).use { response ->
                if (response.code == 404) return@withContext MotionArtworkProviderResult.NoMatch
                if (!response.isSuccessful) {
                    return@withContext MotionArtworkProviderResult.Failed(
                        IOException("Spotify Canvas HTTP ${response.code}")
                    )
                }

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext MotionArtworkProviderResult.NoMatch
                val json = runCatching { JSONObject(body) }.getOrNull()
                    ?: return@withContext MotionArtworkProviderResult.NoMatch
                val data = json.optJSONObject("data")
                    ?.optJSONObject(resolvedSpotifyId)
                    ?: return@withContext MotionArtworkProviderResult.NoMatch
                val canvasUrl = data.optString("url").trim().toHttpUrlOrNull()
                    ?.takeIf { it.isHttps }
                    ?: return@withContext MotionArtworkProviderResult.NoMatch
                val candidate = MotionArtworkCandidate(
                    provider = id,
                    scope = MotionArtworkScope.TRACK,
                    identity = resolvedIdentity,
                    url = canvasUrl.toString(),
                    mimeType = "video/mp4",
                    width = 720,
                    height = 1280,
                    expiresAtMs = System.currentTimeMillis() + MOTION_ARTWORK_POSITIVE_TTL_MS
                )
                MotionArtworkProviderResult.Found(listOf(candidate))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Spotify Canvas probe failed for %s", identity.title)
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private suspend fun resolveIdentity(identity: MotionTrackIdentity): MotionTrackIdentity? {
        if (spotifyTrackId(identity.trackId) != null) return identity
        val queries = listOfNotNull(
            identity.isrc.takeIf(String::isNotBlank)?.let { "isrc:$it" },
            "${identity.title} ${identity.artists.firstOrNull().orEmpty()}".trim().takeIf(String::isNotBlank)
        ).distinct()
        for (query in queries) {
            val results = searchSpotify(query, SEARCH_LIMIT)
            val best = (results.songs + listOfNotNull(results.topTrack))
                .distinctBy { it.id }
                .map(MotionTrackIdentity::from)
                .filter { spotifyTrackId(it.trackId) != null }
                .map { candidate ->
                    candidate to CanonicalTrackMatcher.matchMetadata(identity, candidate, MotionArtworkScope.TRACK)
                }
                .filter { (_, match) -> match.accepted }
                .maxByOrNull { (_, match) -> match.score }
                ?.first
            if (best != null) return best
        }
        return null
    }

    companion object {
        const val PROVIDER_ID = "spotify-canvas"
        private const val CANVAS_ENDPOINT = "https://regional.canvasstore.slnode.spicylyrics.org/provider/canvaslist"
        private const val SEARCH_LIMIT = 6
        private val SPOTIFY_TRACK_ID = Regex("^[A-Za-z0-9]{22}$")

        private fun spotifyTrackId(value: String): String? =
            value.removePrefix("spotify:").trim().takeIf(SPOTIFY_TRACK_ID::matches)

        private fun catalogSearch(context: Context): suspend (String, Int) -> SearchResults {
            val catalog = EditorialChartsRepository.get(context.applicationContext)
            val search = SpotifySearchClient(catalogSearch = catalog::searchSpotifyMetadata)
            return { query, limit -> search.search(query, limit) }
        }
    }
}
