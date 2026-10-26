package com.luc4n3x.derbis.feature.motion

import com.luc4n3x.derbis.domain.DerbisCanvasQuality

enum class MotionCanvasSurface {
    Card,
    Immersive
}

data class MotionCanvasConditions(
    val unmetered: Boolean,
    val dataSaverActive: Boolean,
    val batterySaverActive: Boolean,
    val lowRamDevice: Boolean
)

data class MotionCanvasProfile(
    val maxDimensionPx: Int,
    val maxBitrateBps: Int,
    val forceHighestSupportedBitrate: Boolean
)

object MotionCanvasQualityPolicy {

    fun profile(
        quality: DerbisCanvasQuality,
        surface: MotionCanvasSurface,
        conditions: MotionCanvasConditions
    ): MotionCanvasProfile {
        val effective = effectiveQuality(quality, conditions)
        return when (effective) {
            DerbisCanvasQuality.DataSaver -> when (surface) {
                MotionCanvasSurface.Card -> MotionCanvasProfile(720, 1_200_000, false)
                MotionCanvasSurface.Immersive -> MotionCanvasProfile(720, 1_800_000, false)
            }
            DerbisCanvasQuality.High -> when (surface) {
                MotionCanvasSurface.Card -> MotionCanvasProfile(1_440, 8_000_000, true)
                MotionCanvasSurface.Immersive -> MotionCanvasProfile(2_160, 20_000_000, true)
            }
            DerbisCanvasQuality.Auto -> when (surface) {
                // Keep the default path identical to main so optional motion artwork
                // cannot become more aggressive than the proven playback baseline.
                MotionCanvasSurface.Card -> MotionCanvasProfile(1_280, 4_000_000, false)
                MotionCanvasSurface.Immersive -> MotionCanvasProfile(1_920, 8_000_000, false)
            }
        }
    }

    internal fun effectiveQuality(
        quality: DerbisCanvasQuality,
        conditions: MotionCanvasConditions
    ): DerbisCanvasQuality {
        if (conditions.batterySaverActive || conditions.lowRamDevice) return DerbisCanvasQuality.DataSaver
        if (conditions.dataSaverActive) return DerbisCanvasQuality.DataSaver
        return when (quality) {
            DerbisCanvasQuality.DataSaver -> DerbisCanvasQuality.DataSaver
            DerbisCanvasQuality.High ->
                if (conditions.unmetered) DerbisCanvasQuality.High else DerbisCanvasQuality.Auto
            DerbisCanvasQuality.Auto ->
                if (conditions.unmetered) DerbisCanvasQuality.Auto else DerbisCanvasQuality.DataSaver
        }
    }
}
