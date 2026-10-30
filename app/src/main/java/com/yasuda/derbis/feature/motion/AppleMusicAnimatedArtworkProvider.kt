package com.yasuda.derbis.feature.motion

import android.content.Context
import com.yasuda.derbis.data.network.DerbisHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Resolves official Apple Music animated artwork using verified open-source endpoints:
 * Primary: https://artwork.m8tec.top (m8tec/apple-music-animated-artworks)
 * Secondary / Mirror: https://amaa.tromme.app (sweatyeggs69/AppleArt)
 *
 * Both endpoints return official Apple CDN master HLS (.m3u8) streams from
 * mvod.itunes.apple.com / video-ssl.itunes.apple.com with square (url) and tall (url_tall) covers.
 */
class AppleMusicAnimatedArtworkProvider(context: Context) : MotionArtworkProvider {
    override val id: String = PROVIDER_ID

    private val client: OkHttpClient = DerbisHttpClientFactory.media(context.applicationContext).newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult = withContext(Dispatchers.IO) {
        val artist = identity.artists.firstOrNull()?.trim().orEmpty()
        val album = identity.album.trim().ifBlank { identity.title.trim() }
        if (artist.isBlank() || album.isBlank()) return@withContext MotionArtworkProviderResult.NoMatch

        val endpoints = listOf(PRIMARY_ENDPOINT, SECONDARY_ENDPOINT)
        for (baseEndpoint in endpoints) {
            try {
                val encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8.name())
                val encodedAlbum = URLEncoder.encode(album, StandardCharsets.UTF_8.name())
                val url = "$baseEndpoint/api/v1/artwork/search?artist=$encodedArtist&album=$encodedAlbum"
                    .toHttpUrlOrNull() ?: continue

                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", "YasudaMusic/1.0 (Android; AppleMusicAnimatedArtwork)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 404) return@use
                    if (!response.isSuccessful) return@use

                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) return@use

                    val json = runCatching { JSONObject(body) }.getOrNull() ?: return@use
                    val squareUrl = json.optString("url").trim()
                    val tallUrl = json.optString("url_tall").trim()
                    val selectedUrl = tallUrl.ifBlank { squareUrl }
                    if (selectedUrl.isBlank()) return@use

                    val expiresAt = System.currentTimeMillis() + MOTION_ARTWORK_POSITIVE_TTL_MS
                    val candidate = MotionArtworkCandidate(
                        provider = id,
                        scope = MotionArtworkScope.ALBUM,
                        identity = identity,
                        url = selectedUrl,
                        mimeType = if (selectedUrl.contains(".m3u8")) "application/x-mpegURL" else "video/mp4",
                        width = 1080,
                        height = if (tallUrl.isNotBlank() && selectedUrl == tallUrl) 1920 else 1080,
                        expiresAtMs = expiresAt
                    )
                    return@withContext MotionArtworkProviderResult.Found(listOf(candidate))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.d(error, "Apple Music animated artwork probe failed for %s - %s on %s", artist, album, baseEndpoint)
            }
        }

        MotionArtworkProviderResult.NoMatch
    }

    companion object {
        const val PROVIDER_ID = "apple-music-animated-artwork"
        private const val PRIMARY_ENDPOINT = "https://artwork.m8tec.top"
        private const val SECONDARY_ENDPOINT = "https://amaa.tromme.app"
    }
}
