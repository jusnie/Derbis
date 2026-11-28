package com.yasuda.derbis.data

import coil3.Extras
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.size.Dimension
import coil3.size.Size
import com.yasuda.derbis.domain.DerbisArtworkQuality
import com.yasuda.derbis.domain.DerbisPersonalOrbit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val FULL_RESOLUTION_ARTWORK_SIZE = 1200
private const val SPOTIFY_MEDIUM_ARTWORK_SIZE = 300
private val AppleArtworkSizePattern = Regex("/(\\d+)x(\\d+)bb(?=[.-])")
private val GoogleWidthHeightPattern = Regex("=w(\\d+)-h(\\d+)")
private val GoogleSquarePattern = Regex("=s(\\d+)")

internal enum class ArtworkTier(val pixels: Int) {
    Thumbnail(256),
    Card(512),
    Detail(768),
    Full(FULL_RESOLUTION_ARTWORK_SIZE)
}

/**
 * The artwork ceiling, read by every URL the app rewrites.
 *
 * Volatile because it is written from the settings thread and read from Coil's decode threads;
 * a torn read here would not corrupt anything, but a stale one would keep asking for full-size
 * artwork for a screen the user had already told to stop.
 *
 * It holds pixels rather than the enum so that a boot-time restore can set it before the
 * settings object exists, which is the order the application actually starts in.
 */
internal object ArtworkQualityCeiling {
    @Volatile
    private var pixels: Int = DerbisArtworkQuality.Medium.pixels

    fun update(quality: DerbisArtworkQuality) {
        pixels = quality.pixels
    }

    /** Direct setter for boot-time restore, before the settings object exists. */
    fun updatePixels(value: Int) {
        pixels = value.coerceIn(DerbisArtworkQuality.Low.pixels, ArtworkTier.Full.pixels)
    }

    fun current(): Int = pixels
}

internal object ArtworkQualityPolicy {
    fun tierFor(targetPixels: Int?): ArtworkTier {
        if (targetPixels == null || targetPixels <= 0) return ArtworkTier.Full
        return ArtworkTier.entries.firstOrNull { targetPixels <= it.pixels } ?: ArtworkTier.Full
    }

    fun tierFor(size: Size): ArtworkTier {
        val width = (size.width as? Dimension.Pixels)?.px
        val height = (size.height as? Dimension.Pixels)?.px
        return tierFor(listOfNotNull(width, height).maxOrNull())
    }
}

internal object ArtworkUrlResolver {
    fun resolve(url: String, tier: ArtworkTier): String =
        resolve(url, tier, ArtworkQualityCeiling.current())

    /**
     * [ceilingPixels] is a cap, never a target: the effective size is the smaller of what the
     * view asked for and what the setting allows, so a thumbnail is untouched by selecting High.
     */
    fun resolve(url: String, tier: ArtworkTier, ceilingPixels: Int): String {
        val pixels = minOf(tier.pixels, ceilingPixels)
        val clean = url.trim()
        if (!clean.startsWith("https://", ignoreCase = true)) return clean
        if (clean.indexOf('?') >= 0) return clean
        val parsed = clean.toHttpUrlOrNull() ?: return clean
        val host = parsed.host.lowercase()
        return when {
            host == "i.scdn.co" && parsed.encodedPath.contains("/image/ab67616d00001e02", ignoreCase = true) ->
                if (pixels > SPOTIFY_MEDIUM_ARTWORK_SIZE) {
                    clean.replace("ab67616d00001e02", "ab67616d0000b273", ignoreCase = true)
                } else {
                    clean
                }
            isHostOrSubdomain(host, "mzstatic.com") -> appleArtworkUrl(clean, tier, pixels)
            isHostOrSubdomain(host, "googleusercontent.com") || isHostOrSubdomain(host, "ggpht.com") ->
                googleArtworkUrl(clean, tier, pixels)
            host == "e-cdns-images.dzcdn.net" -> deezerArtworkUrl(clean, tier, pixels)
            else -> clean
        }
    }

    private fun appleArtworkUrl(url: String, tier: ArtworkTier, pixels: Int): String {
        val size = pixels.toString()
        val templated = url
            .replace("{w}", size, ignoreCase = true)
            .replace("{h}", size, ignoreCase = true)
        if (templated != url) return templated

        val match = AppleArtworkSizePattern.find(url) ?: return url
        val width = match.groupValues[1].toIntOrNull() ?: return url
        val height = match.groupValues[2].toIntOrNull() ?: return url
        // Left alone only when the full-size tier is asking and the source is already that big:
        // a cap that is doing something must still be able to shrink a larger asset.
        if (tier == ArtworkTier.Full && pixels == tier.pixels && (width >= pixels || height >= pixels)) return url
        if (width == pixels && height == pixels) return url
        return url.replaceRange(match.range, "/${pixels}x${pixels}bb")
    }

    private fun googleArtworkUrl(url: String, tier: ArtworkTier, pixels: Int): String {
        if (tier == ArtworkTier.Full && pixels == tier.pixels && googleArtworkAlreadyLargeEnough(url, pixels)) return url
        return DerbisPersonalOrbit.upscaledArtworkUrl(url, pixels)
    }

    private fun googleArtworkAlreadyLargeEnough(url: String, pixels: Int): Boolean {
        GoogleWidthHeightPattern.find(url)?.let { match ->
            val width = match.groupValues[1].toIntOrNull() ?: return@let
            val height = match.groupValues[2].toIntOrNull() ?: return@let
            return width >= pixels || height >= pixels
        }
        GoogleSquarePattern.find(url)?.let { match ->
            val size = match.groupValues[1].toIntOrNull() ?: return@let
            return size >= pixels
        }
        return false
    }

    /**
     * Deezer publishes fixed assets only — 250, 500 and 1000 px — so the cap cannot land on
     * 480 or 720 exactly. It picks the step that suits the ceiling and never climbs above what
     * the view asked for, which is the same rule the other two hosts follow.
     */
    private fun deezerArtworkUrl(url: String, tier: ArtworkTier, pixels: Int): String =
        when (ArtworkQualityPolicy.tierFor(pixels).coerceAtMost(tier)) {
            ArtworkTier.Thumbnail -> url
            ArtworkTier.Card -> url.replace("/cover_medium/", "/cover_big/", ignoreCase = true)
            ArtworkTier.Detail, ArtworkTier.Full -> url
                .replace("/cover_medium/", "/cover_xl/", ignoreCase = true)
                .replace("/cover_big/", "/cover_xl/", ignoreCase = true)
        }

    private fun isHostOrSubdomain(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")
}

internal fun fullResolutionArtworkUrl(url: String): String = ArtworkUrlResolver.resolve(url, ArtworkTier.Full)

internal val ArtworkTierOverride = Extras.Key<ArtworkTier?>(default = null)

internal class ArtworkResolutionInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val source = chain.request.data as? String ?: return chain.proceed()
        val tier = chain.request.getExtra(ArtworkTierOverride) ?: ArtworkQualityPolicy.tierFor(chain.size)
        val resolved = ArtworkUrlResolver.resolve(source, tier)
        if (resolved == source) return chain.proceed()
        val request = chain.request.newBuilder()
            .data(resolved)
            .build()
        return chain.withRequest(request).proceed()
    }
}
