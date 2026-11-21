package com.yasuda.derbis.feature.motion

import android.content.Context
import com.yasuda.derbis.BuildConfig
import com.yasuda.derbis.data.network.DerbisHttpClientFactory
import com.yasuda.derbis.data.spotify.DerbisSpotifySession
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

/**
 * Resolves official 9:16 Spotify Canvas looping video URLs (`canvaz.scdn.co/...` /
 * `*.spotifycdn.com/...`):
 *
 * 1. Resolves non-Spotify (11-char YouTube / catalog) track identities to verified 22-char Spotify
 *    track IDs via [SpotifyOfficialCanvas.searchTracks] (verified through [CanonicalTrackMatcher])
 *    or optional configured `BuildConfig.CANVAS_RESOLVER_URL`.
 * 2. Queries [SpotifyOfficialCanvas] when a Spotify web session (`sp_dc`) is active, with fallback
 *    to the public CanvasStore mirror.
 */
class SpotifyCanvasProvider(context: Context) : MotionArtworkProvider {
    override val id: String = PROVIDER_ID

    private val appContext = context.applicationContext

    private val client: OkHttpClient = DerbisHttpClientFactory.media(appContext).newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    init {
        runCatching {
            DerbisSpotifySession.initialize(appContext)
            SpotifyTokenProvider.init(appContext)
        }
    }

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult = withContext(Dispatchers.IO) {
        val resolvedTargets = resolveSpotifyTargets(identity)
        if (resolvedTargets.isEmpty()) {
            return@withContext MotionArtworkProviderResult.NoMatch
        }

        var lastFailure: Throwable? = null
        for (target in resolvedTargets) {
            if (DerbisSpotifySession.hasSession()) {
                when (val official = SpotifyOfficialCanvas.probe(target.spotifyId)) {
                    is SpotifyOfficialCanvas.Result.Found -> {
                        return@withContext MotionArtworkProviderResult.Found(
                            listOf(candidate(target.matchedIdentity, official.url))
                        )
                    }
                    is SpotifyOfficialCanvas.Result.NoCanvas -> continue
                    is SpotifyOfficialCanvas.Result.Failed -> {
                        lastFailure = IOException(official.reason)
                    }
                }
            }

            when (val mirror = fetchPublicCanvasMirror(target.spotifyId, target.matchedIdentity)) {
                is MotionArtworkProviderResult.Found -> return@withContext mirror
                MotionArtworkProviderResult.NoMatch -> continue
                is MotionArtworkProviderResult.Failed -> lastFailure = mirror.cause
            }
        }

        if (lastFailure != null) {
            MotionArtworkProviderResult.Failed(lastFailure)
        } else {
            MotionArtworkProviderResult.NoMatch
        }
    }

    private data class ResolvedSpotifyTarget(
        val spotifyId: String,
        val matchedIdentity: MotionTrackIdentity,
        val matchScore: Int
    )

    private suspend fun resolveSpotifyTargets(identity: MotionTrackIdentity): List<ResolvedSpotifyTarget> {
        SpotifyOfficialCanvas.normalizeTrackId(identity.trackId)?.let { directId ->
            return listOf(
                ResolvedSpotifyTarget(
                    spotifyId = directId,
                    matchedIdentity = identity,
                    matchScore = 100
                )
            )
        }

        val cacheKey = trackCacheKey(identity)
        matchedTargetsCache[cacheKey]?.let { cached ->
            if (cached.isNotEmpty()) return cached
        }

        val primaryArtist = identity.artists.firstOrNull().orEmpty()
        val searchHits = SpotifyOfficialCanvas.searchTracks(
            title = identity.title,
            artist = primaryArtist,
            isrc = identity.isrc,
            limit = 8
        )

        val verified = searchHits
            .mapNotNull { hit ->
                val hitIdentity = MotionTrackIdentity(
                    title = hit.title,
                    artists = hit.artists.ifEmpty { identity.artists },
                    album = hit.album.ifBlank { identity.album },
                    durationMs = hit.durationMs.takeIf { it > 0L } ?: identity.durationMs,
                    isrc = hit.isrc.uppercase(Locale.ROOT),
                    upc = "",
                    year = hit.year.ifBlank { identity.year },
                    trackId = "spotify:track:${hit.spotifyId}",
                    albumId = ""
                )
                val probeCandidate = candidate(hitIdentity, PROBE_PLACEHOLDER_URL)
                val match = CanonicalTrackMatcher.match(identity, probeCandidate)
                if (match.accepted && match.score >= DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE) {
                    ResolvedSpotifyTarget(
                        spotifyId = hit.spotifyId,
                        matchedIdentity = hitIdentity,
                        matchScore = match.score
                    )
                } else {
                    null
                }
            }
            .sortedByDescending { it.matchScore }
            .distinctBy { it.spotifyId }
            .take(MAX_RELEASE_VARIANTS_TO_TRY)

        if (verified.isNotEmpty()) {
            if (matchedTargetsCache.size > MAX_TRACK_CACHE_ENTRIES) {
                matchedTargetsCache.clear()
            }
            matchedTargetsCache[cacheKey] = verified
            return verified
        }

        resolveViaConfiguredResolver(identity)?.let { resolvedId ->
            val single = listOf(ResolvedSpotifyTarget(resolvedId, identity, 96))
            matchedTargetsCache[cacheKey] = single
            return single
        }

        return emptyList()
    }

    private fun resolveViaConfiguredResolver(identity: MotionTrackIdentity): String? {
        val baseUrl = BuildConfig.CANVAS_RESOLVER_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) return null
        return runCatching {
            val title = URLEncoder.encode(identity.title, StandardCharsets.UTF_8.name())
            val artist = URLEncoder.encode(identity.artists.firstOrNull().orEmpty(), StandardCharsets.UTF_8.name())
            val isrc = URLEncoder.encode(identity.isrc, StandardCharsets.UTF_8.name())
            val url = "$baseUrl/resolve?title=$title&artist=$artist&isrc=$isrc".toHttpUrlOrNull()
                ?: return@runCatching null
            val clientKey = BuildConfig.CANVAS_CLIENT_KEY.trim()
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .apply {
                    if (clientKey.isNotBlank()) {
                        header("X-Derbis-Key", clientKey)
                    }
                }
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val json = JSONObject(response.body?.string().orEmpty())
                SpotifyOfficialCanvas.normalizeTrackId(
                    json.optString("spotifyId").ifBlank { json.optString("trackId") }
                )
            }
        }.getOrNull()
    }

    private fun fetchPublicCanvasMirror(
        spotifyId: String,
        matchedIdentity: MotionTrackIdentity
    ): MotionArtworkProviderResult {
        return try {
            val url = "$CANVAS_ENDPOINT/$spotifyId".toHttpUrlOrNull()
                ?: return MotionArtworkProviderResult.NoMatch

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "YasudaMusic/1.0 (Android; SpotifyCanvasProvider)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404 || response.code == 204) {
                    return MotionArtworkProviderResult.NoMatch
                }
                if (!response.isSuccessful) {
                    return MotionArtworkProviderResult.Failed(
                        IOException("Spotify Canvas mirror HTTP ${response.code}")
                    )
                }

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return MotionArtworkProviderResult.NoMatch

                val json = runCatching { JSONObject(body) }.getOrNull()
                    ?: return MotionArtworkProviderResult.NoMatch

                val canvasUrl = json.optJSONObject("data")
                    ?.optJSONObject(spotifyId)
                    ?.optString("url")
                    ?.trim()
                    .orEmpty()
                if (canvasUrl.isBlank() || !canvasUrl.startsWith("https://")) {
                    return MotionArtworkProviderResult.NoMatch
                }

                MotionArtworkProviderResult.Found(listOf(candidate(matchedIdentity, canvasUrl)))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Spotify Canvas mirror probe failed for %s (%s)", matchedIdentity.title, spotifyId)
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private fun trackCacheKey(identity: MotionTrackIdentity): String =
        listOf(
            identity.isrc.trim().uppercase(Locale.ROOT),
            normalizeMotionText(identity.title),
            combinedArtistSignature(identity.artists),
            (identity.durationMs / 1_000L).toString()
        ).joinToString("|")

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
        private const val PROBE_PLACEHOLDER_URL = "https://canvaz.scdn.co/probe.mp4"
        private const val MAX_RELEASE_VARIANTS_TO_TRY = 3
        private const val MAX_TRACK_CACHE_ENTRIES = 256

        private val matchedTargetsCache = ConcurrentHashMap<String, List<ResolvedSpotifyTarget>>()

        internal fun clearCaches() {
            matchedTargetsCache.clear()
        }
    }
}
