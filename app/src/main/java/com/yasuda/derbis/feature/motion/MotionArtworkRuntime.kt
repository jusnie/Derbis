package com.yasuda.derbis.feature.motion

import com.yasuda.derbis.core.config.RuntimeConfigSnapshot
import com.yasuda.derbis.core.config.VersionedRuntimeConfig

data class MotionArtworkConfig(
    val providerOrder: List<String> = listOf(
        AppleMusicAnimatedArtworkProvider.PROVIDER_ID,
        SpotifyCanvasProvider.PROVIDER_ID,
        DerbisCanvasCatalogProvider.PROVIDER_ID
    ),
    val minimumConfidence: Int = DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE,
    val requestTimeoutMs: Long = 6_500L,
    val positiveTtlMs: Long = MOTION_ARTWORK_POSITIVE_TTL_MS,
    val negativeTtlMs: Long = MOTION_ARTWORK_NEGATIVE_TTL_MS
) {
    fun normalized(): MotionArtworkConfig = copy(
        providerOrder = providerOrder
            .filter {
                it in setOf(
                    AppleMusicAnimatedArtworkProvider.PROVIDER_ID,
                    SpotifyCanvasProvider.PROVIDER_ID,
                    DerbisCanvasCatalogProvider.PROVIDER_ID
                )
            }
            .distinct()
            .ifEmpty {
                listOf(
                    AppleMusicAnimatedArtworkProvider.PROVIDER_ID,
                    SpotifyCanvasProvider.PROVIDER_ID
                )
            },
        minimumConfidence = minimumConfidence.coerceIn(70, 100),
        requestTimeoutMs = requestTimeoutMs.coerceIn(2_500L, 12_000L),
        positiveTtlMs = positiveTtlMs.coerceIn(60L * 60L * 1000L, 7L * 24L * 60L * 60L * 1000L),
        negativeTtlMs = negativeTtlMs.coerceIn(10L * 60L * 1000L, 6L * 60L * 60L * 1000L)
    )
}

object MotionArtworkRuntime {
    private val config = VersionedRuntimeConfig(MotionArtworkConfig().normalized())

    fun snapshot(): RuntimeConfigSnapshot<MotionArtworkConfig> = config.snapshot().withCacheSchema()

    fun update(value: MotionArtworkConfig): RuntimeConfigSnapshot<MotionArtworkConfig> =
        config.update(value.normalized()).withCacheSchema()

    private fun RuntimeConfigSnapshot<MotionArtworkConfig>.withCacheSchema() = copy(
        epoch = epoch + MOTION_ARTWORK_CACHE_SCHEMA_EPOCH
    )
}

internal const val MOTION_ARTWORK_CACHE_SCHEMA_EPOCH = 10L
internal const val DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE = 84
