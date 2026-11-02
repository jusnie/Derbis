package com.yasuda.derbis.feature.motion

import android.content.Context
import com.yasuda.derbis.data.network.DerbisHttpClientFactory
import com.yasuda.derbis.data.spotify.DerbisSpotifySession
import com.yasuda.derbis.data.spotify.SpotifySearchClient
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
 * Resolves official Spotify Canvas looping video URLs (canvaz.scdn.co/...) two ways:
 *
 * 1. With a Spotify web session (`sp_dc`) stored by the listener, [SpotifyOfficialCanvas] asks
 *    Spotify itself, which is where the web player gets the same clip.
 * 2. Without a session, the public CanvasStore / Spicy Lyrics mirror answers:
 *    https://regional.canvasstore.slnode.spicylyrics.org/provider/canvaslist/{trackId}
 *
 * The mirror needs no account; the official path is preferred whenever one exists.
 */
class SpotifyCanvasProvider(context: Context) : MotionArtworkProvider {
    override val id: String = PROVIDER_ID

    private val client: OkHttpClient = DerbisHttpClientFactory.media(context.applicationContext).newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val spotifySearch = SpotifySearchClient()

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult = withContext(Dispatchers.IO) {
        val rawTrackId = identity.trackId.removePrefix("spotify:").trim()
        val resolvedSpotifyId = if (rawTrackId.length == 22 && rawTrackId.all { it.isLetterOrDigit() }) {
            rawTrackId
        } else {
            // Attempt to resolve Spotify track ID via search
            val query = if (identity.isrc.isNotBlank()) "isrc:${identity.isrc}" else "${identity.title} ${identity.artists.firstOrNull().orEmpty()}"
            val searchResults = runCatching { spotifySearch.cachedSearch(query, limit = 1) }.getOrNull()
            val candidateTrack = searchResults?.songs?.firstOrNull() ?: searchResults?.topTrack
            val candidateId = candidateTrack?.id?.removePrefix("spotify:")?.trim().orEmpty()
            if (candidateId.length == 22 && candidateId.all { it.isLetterOrDigit() }) {
                candidateId
            } else {
                return@withContext MotionArtworkProviderResult.NoMatch
            }
        }

        // Spotify's own Canvas first whenever the listener has handed over a web session (see
        // SpotifyOfficialCanvas): it is the source the web player itself reads, so it answers for
        // tracks the community mirror has never indexed, and it cannot be rate-limited by a third
        // party. Everything below stays as the no-session path.
        if (DerbisSpotifySession.hasSession()) {
            SpotifyOfficialCanvas.canvasUrl(resolvedSpotifyId)?.let { officialUrl ->
                return@withContext MotionArtworkProviderResult.Found(
                    listOf(candidate(identity, officialUrl))
                )
            }
        }

        try {
            val url = "$CANVAS_ENDPOINT/$resolvedSpotifyId".toHttpUrlOrNull()
                ?: return@withContext MotionArtworkProviderResult.NoMatch

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "YasudaMusic/1.0 (Android; SpotifyCanvasProvider)")
                .build()

            client.newCall(request).execute().use { response ->
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

                val dataObj = json.optJSONObject("data") ?: return@withContext MotionArtworkProviderResult.NoMatch
                val trackData = dataObj.optJSONObject(resolvedSpotifyId) ?: return@withContext MotionArtworkProviderResult.NoMatch
                val canvasUrl = trackData.optString("url").trim()
                if (canvasUrl.isBlank() || !canvasUrl.startsWith("http")) {
                    return@withContext MotionArtworkProviderResult.NoMatch
                }

                MotionArtworkProviderResult.Found(listOf(candidate(identity, canvasUrl)))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Spotify Canvas probe failed for %s (%s)", identity.title, resolvedSpotifyId)
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private fun candidate(identity: MotionTrackIdentity, canvasUrl: String) = MotionArtworkCandidate(
        provider = id,
        scope = MotionArtworkScope.TRACK,
        identity = identity,
        url = canvasUrl,
        mimeType = "video/mp4",
        width = 720,
        height = 1280,
        expiresAtMs = System.currentTimeMillis() + MOTION_ARTWORK_POSITIVE_TTL_MS
    )

    companion object {
        const val PROVIDER_ID = "spotify-canvas"
        private const val CANVAS_ENDPOINT = "https://regional.canvasstore.slnode.spicylyrics.org/provider/canvaslist"
    }
}
