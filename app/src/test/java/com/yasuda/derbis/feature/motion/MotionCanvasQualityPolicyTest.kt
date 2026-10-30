package com.yasuda.derbis.feature.motion

import com.yasuda.derbis.domain.DerbisCanvasQuality
import com.yasuda.derbis.domain.DerbisCanvasSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionCanvasQualityPolicyTest {

    private val healthy = MotionCanvasConditions(
        unmetered = true,
        dataSaverActive = false,
        batterySaverActive = false,
        lowRamDevice = false
    )

    @Test
    fun `immersive auto preserves the main playback baseline`() {
        val profile = MotionCanvasQualityPolicy.profile(
            DerbisCanvasQuality.Auto,
            MotionCanvasSurface.Immersive,
            healthy
        )
        assertEquals(1_920, profile.maxDimensionPx)
        assertEquals(8_000_000, profile.maxBitrateBps)
        assertFalse(profile.forceHighestSupportedBitrate)
    }

    @Test
    fun `card auto preserves the main playback baseline`() {
        val profile = MotionCanvasQualityPolicy.profile(
            DerbisCanvasQuality.Auto,
            MotionCanvasSurface.Card,
            healthy
        )
        assertEquals(1_280, profile.maxDimensionPx)
        assertEquals(4_000_000, profile.maxBitrateBps)
        assertFalse(profile.forceHighestSupportedBitrate)
    }

    @Test
    fun `card stays more conservative than immersive at the same quality`() {
        val card = MotionCanvasQualityPolicy.profile(
            DerbisCanvasQuality.Auto,
            MotionCanvasSurface.Card,
            healthy
        )
        val immersive = MotionCanvasQualityPolicy.profile(
            DerbisCanvasQuality.Auto,
            MotionCanvasSurface.Immersive,
            healthy
        )
        assertTrue(card.maxDimensionPx < immersive.maxDimensionPx)
        assertTrue(card.maxBitrateBps < immersive.maxBitrateBps)
    }

    @Test
    fun `high quality raises the immersive ceiling above auto`() {
        val auto = MotionCanvasQualityPolicy.profile(
            DerbisCanvasQuality.Auto,
            MotionCanvasSurface.Immersive,
            healthy
        )
        val high = MotionCanvasQualityPolicy.profile(
            DerbisCanvasQuality.High,
            MotionCanvasSurface.Immersive,
            healthy
        )
        assertTrue(high.maxDimensionPx > auto.maxDimensionPx)
        assertTrue(high.maxBitrateBps > auto.maxBitrateBps)
        assertTrue(high.forceHighestSupportedBitrate)
    }

    @Test
    fun `auto falls back to data saver on a metered network`() {
        val profile = MotionCanvasQualityPolicy.profile(
            DerbisCanvasQuality.Auto,
            MotionCanvasSurface.Immersive,
            healthy.copy(unmetered = false)
        )
        assertEquals(720, profile.maxDimensionPx)
        assertFalse(profile.forceHighestSupportedBitrate)
    }

    @Test
    fun `high is capped to the auto tier on a metered network`() {
        val metered = MotionCanvasQualityPolicy.profile(
            DerbisCanvasQuality.High,
            MotionCanvasSurface.Immersive,
            healthy.copy(unmetered = false)
        )
        val auto = MotionCanvasQualityPolicy.profile(
            DerbisCanvasQuality.Auto,
            MotionCanvasSurface.Immersive,
            healthy
        )
        assertEquals(auto, metered)
    }

    @Test
    fun `battery saver forces the data saver tier even when high is requested`() {
        assertEquals(
            DerbisCanvasQuality.DataSaver,
            MotionCanvasQualityPolicy.effectiveQuality(
                DerbisCanvasQuality.High,
                healthy.copy(batterySaverActive = true)
            )
        )
    }

    @Test
    fun `low ram devices force the data saver tier`() {
        assertEquals(
            DerbisCanvasQuality.DataSaver,
            MotionCanvasQualityPolicy.effectiveQuality(
                DerbisCanvasQuality.High,
                healthy.copy(lowRamDevice = true)
            )
        )
    }

    @Test
    fun `android data saver forces the data saver tier`() {
        assertEquals(
            DerbisCanvasQuality.DataSaver,
            MotionCanvasQualityPolicy.effectiveQuality(
                DerbisCanvasQuality.High,
                healthy.copy(dataSaverActive = true)
            )
        )
    }

    @Test
    fun `unknown stored quality falls back to auto`() {
        assertEquals(DerbisCanvasQuality.Auto, DerbisCanvasQuality.from("ultra"))
        assertEquals(DerbisCanvasQuality.High, DerbisCanvasQuality.from("high"))
    }

    @Test
    fun `unknown providers are excluded from provider order`() {
        val order = listOf("untrusted-provider", DerbisCanvasCatalogProvider.PROVIDER_ID)
        assertEquals(
            listOf(DerbisCanvasCatalogProvider.PROVIDER_ID),
            motionArtworkProviderOrder(order, DerbisCanvasSource.Auto)
        )
    }

    @Test
    fun `auto order keeps public providers and removes unknowns and duplicates`() {
        val expected = listOf(
            AppleMusicAnimatedArtworkProvider.PROVIDER_ID,
            SpotifyCanvasProvider.PROVIDER_ID,
            DerbisCanvasCatalogProvider.PROVIDER_ID
        )
        val configured = listOf("untrusted-provider") + expected + SpotifyCanvasProvider.PROVIDER_ID

        assertEquals(expected, motionArtworkProviderOrder(configured, DerbisCanvasSource.Auto))
    }

    @Test
    fun `cache keys stay stable because there is no source switch`() {
        assertEquals("abc", motionArtworkCacheKey("abc", DerbisCanvasSource.Auto))
    }

    @Test
    fun `stored legacy source values migrate to auto`() {
        assertEquals(DerbisCanvasSource.Auto, DerbisCanvasSource.from(""))
        assertEquals(DerbisCanvasSource.Auto, DerbisCanvasSource.from("community"))
        assertEquals(DerbisCanvasSource.Auto, DerbisCanvasSource.from("apple"))
        assertEquals(DerbisCanvasSource.Auto, DerbisCanvasSource.from("tidal"))
    }
}
