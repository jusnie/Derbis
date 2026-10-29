package com.yasuda.derbis.feature.motion

import com.yasuda.derbis.domain.DerbisCanvasSource
import com.yasuda.derbis.domain.DerbisInterfaceSettings
import com.yasuda.derbis.viewmodel.shouldRefreshMotionArtworkOwnership
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkFallbackPolicyTest {
    @Test
    fun appleAndTidalMissesOrFailuresContinueTheAutoChain() {
        assertTrue(shouldContinueMotionArtworkFallback(MotionArtworkProviderResult.NoMatch))
        assertTrue(shouldContinueMotionArtworkFallback(MotionArtworkProviderResult.Failed()))
        assertTrue(shouldContinueMotionArtworkFallback(MotionArtworkProviderResult.Found(emptyList())))
        assertFalse(shouldContinueMotionArtworkFallback(MotionArtworkProviderResult.Found(listOf(candidate()))))
    }

    @Test
    fun wifiOnlyTransitionResetsMotionArtworkOwnershipAndPolicyBlocksPublish() {
        val initial = DerbisInterfaceSettings()

        assertTrue(shouldRefreshMotionArtworkOwnership(initial, initial.copy(motionArtworkWifiOnly = true)))
        assertFalse(shouldRefreshMotionArtworkOwnership(initial, initial.copy(canvasSource = DerbisCanvasSource.Auto)))
        assertFalse(shouldRefreshMotionArtworkOwnership(initial, initial))
        assertFalse(shouldPublishMotionArtwork(false))
        assertTrue(shouldPublishMotionArtwork(true))
    }

    private fun candidate() = MotionArtworkCandidate(
        provider = DerbisCanvasCatalogProvider.PROVIDER_ID,
        scope = MotionArtworkScope.TRACK,
        identity = MotionTrackIdentity(
            title = "Track",
            artists = listOf("Artist"),
            album = "Album",
            durationMs = 180_000L,
            isrc = "",
            upc = "",
            year = "",
            trackId = "track",
            albumId = "album"
        ),
        url = "https://example.invalid/video.mp4",
        mimeType = "video/mp4",
        expiresAtMs = Long.MAX_VALUE
    )
}
