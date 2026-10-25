package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

/**
 * Resolves only rights-cleared loops served by the owner's Derbis Canvas Catalog.
 *
 * Unlike an unofficial streaming-service scraper, the Android client never submits account
 * cookies/tokens to the catalog and only accepts HTTPS media from the configured catalog host.
 * The endpoint and key are release-build configuration, not user credentials.
 */
class DerbisCanvasCatalogProvider(context: Context) : MotionArtworkProvider {
    override val id: String = PROVIDER_ID

    private val client: OkHttpClient = LevyraHttpClientFactory.media(context.applicationContext).newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val catalogEndpoint = configuredDerbisCanvasCatalogEndpoint()
    private val catalogKey = BuildConfig.DERBIS_CANVAS_CATALOG_KEY.trim()

    override suspend fun find(identity: MotionTrackIdentity): MotionArtworkProviderResult = withContext(Dispatchers.IO) {
        val resolverEndpoint = catalogEndpoint
        if (resolverEndpoint == null || catalogKey.isBlank()) return@withContext MotionArtworkProviderResult.NoMatch
        try {
            client.newCall(buildRequest(identity, resolverEndpoint)).awaitDerbisCatalogResponse().use { response ->
                when {
                    response.code == 404 -> MotionArtworkProviderResult.NoMatch
                    !response.isSuccessful -> MotionArtworkProviderResult.Failed(
                        IOException("Derbis Canvas Catalog HTTP ${response.code}")
                    )
                    else -> parseResponse(response, identity, resolverEndpoint)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.d(error, "Derbis Canvas Catalog lookup failed for %s", identity.title)
            MotionArtworkProviderResult.Failed(error)
        }
    }

    private fun buildRequest(identity: MotionTrackIdentity, resolverEndpoint: HttpUrl): Request {
        val payload = JSONObject().apply {
            if (identity.isrc.isNotBlank()) put("isrc", identity.isrc)
            put("title", identity.title)
            put("artist", identity.artists.firstOrNull().orEmpty())
            put("album", identity.album)
            if (identity.durationMs > 0L) put("durationMs", identity.durationMs)
        }
        return Request.Builder()
            .url(resolverEndpoint)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .header("X-Derbis-Canvas-Key", catalogKey)
            .build()
    }

    private fun parseResponse(
        response: Response,
        requested: MotionTrackIdentity,
        resolverEndpoint: HttpUrl
    ): MotionArtworkProviderResult {
        val body = response.body.readBoundedDerbisCatalogText(MAX_RESPONSE_BYTES)
            ?: return MotionArtworkProviderResult.Failed(IOException("Derbis Canvas Catalog response is too large"))
        val payload = runCatching { JSONObject(body) }.getOrElse {
            return MotionArtworkProviderResult.Failed(IOException("Derbis Canvas Catalog returned invalid JSON", it))
        }
        if (payload.optString("status").trim().lowercase() != "resolved") {
            return if (payload.optString("status").trim().lowercase() == "miss") {
                MotionArtworkProviderResult.NoMatch
            } else {
                MotionArtworkProviderResult.Failed(IOException("Derbis Canvas Catalog returned an unexpected status"))
            }
        }
        val media = derbisCanvasCatalogMediaUrl(payload.optString("url"), resolverEndpoint)
            ?: return MotionArtworkProviderResult.NoMatch
        val mimeType = payload.optString("mimeType")
            .trim()
            .takeIf { it.equals("video/mp4", true) || it.equals("application/x-mpegURL", true) }
            ?: if (media.encodedPath.endsWith(".m3u8", true)) "application/x-mpegURL" else "video/mp4"
        val width = payload.optInt("width", 0).takeIf { it in 64..8_192 }
        val height = payload.optInt("height", 0).takeIf { it in 64..8_192 }
        val expiresAt = payload.optLong("expiresAtMs", 0L)
            .takeIf { it > System.currentTimeMillis() }
            ?: System.currentTimeMillis() + MOTION_ARTWORK_POSITIVE_TTL_MS
        return MotionArtworkProviderResult.Found(
            listOf(
                MotionArtworkCandidate(
                    provider = id,
                    scope = MotionArtworkScope.TRACK,
                    identity = requested,
                    url = media.toString(),
                    mimeType = mimeType,
                    width = width,
                    height = height,
                    expiresAtMs = expiresAt
                )
            )
        )
    }

    companion object {
        const val PROVIDER_ID = "derbis-canvas-catalog"
        private const val MAX_RESPONSE_BYTES = 64 * 1024
        private const val USER_AGENT = "Derbis/1.0 (Android; OwnedCanvasCatalog)"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

internal fun configuredDerbisCanvasCatalogEndpoint(
    rawUrl: String = BuildConfig.DERBIS_CANVAS_CATALOG_URL
): HttpUrl? = rawUrl.trim().toHttpUrlOrNull()?.takeIf(::isValidDerbisCatalogEndpoint)

internal fun isDerbisCanvasCatalogConfigured(): Boolean =
    BuildConfig.DERBIS_CANVAS_CATALOG_KEY.isNotBlank() && configuredDerbisCanvasCatalogEndpoint() != null

internal fun isValidDerbisCatalogEndpoint(url: HttpUrl): Boolean =
    url.scheme == "https" &&
        url.port == 443 &&
        url.username.isEmpty() &&
        url.password.isEmpty() &&
        url.query == null &&
        url.fragment == null &&
        url.encodedPath == "/v1/motion-artwork/resolve"

/** The catalog serves its own verified media, preventing arbitrary third-party video URLs. */
internal fun derbisCanvasCatalogMediaUrl(rawUrl: String, resolverEndpoint: HttpUrl): HttpUrl? {
    val media = rawUrl.trim().toHttpUrlOrNull() ?: return null
    return media.takeIf {
        it.scheme == "https" &&
            it.port == 443 &&
            it.host.equals(resolverEndpoint.host, ignoreCase = true) &&
            it.encodedPath.startsWith("/media/") &&
            (it.encodedPath.endsWith(".mp4", true) || it.encodedPath.endsWith(".m3u8", true))
    }
}

private suspend fun Call.awaitDerbisCatalogResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : okhttp3.Callback {
        override fun onFailure(call: Call, error: IOException) {
            if (continuation.isActive) continuation.resumeWithException(error)
        }

        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) continuation.resumeWith(Result.success(response)) else response.close()
        }
    })
}

private fun ResponseBody.readBoundedDerbisCatalogText(maxBytes: Int): String? {
    if (contentLength() > maxBytes) return null
    return byteStream().use { input ->
        val output = ByteArrayOutputStream(minOf(8 * 1024, maxBytes))
        val buffer = ByteArray(4 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) return null
            output.write(buffer, 0, count)
        }
        output.toString(Charsets.UTF_8.name())
    }
}
