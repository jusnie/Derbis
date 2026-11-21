package com.yasuda.derbis.feature.motion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkPriorityHoldTest {

    @Test
    fun secondaryVerifiedWhilePrimaryPendingIsHeldInAuto() {
        assertTrue(
            shouldHoldMotionPublication(
                candidateProviderRank = 1,
                pendingProviderRanks = listOf(0, 2),
                forcedSource = false,
                alreadyPublished = false
            )
        )
    }

    @Test
    fun primaryVerifiedPublishesImmediatelyEvenWithSlowerProvidersPending() {
        assertFalse(
            shouldHoldMotionPublication(
                candidateProviderRank = 0,
                pendingProviderRanks = listOf(1, 2),
                forcedSource = false,
                alreadyPublished = false
            )
        )
    }

    @Test
    fun lowerPriorityCandidatePublishesOnceHigherPriorityProvidersSettled() {
        assertFalse(
            shouldHoldMotionPublication(
                candidateProviderRank = 1,
                pendingProviderRanks = listOf(2),
                forcedSource = false,
                alreadyPublished = false
            )
        )
        assertFalse(
            shouldHoldMotionPublication(
                candidateProviderRank = 2,
                pendingProviderRanks = emptyList(),
                forcedSource = false,
                alreadyPublished = false
            )
        )
    }

    @Test
    fun explicitSourceNeverWaitsForOtherProviders() {
        assertFalse(
            shouldHoldMotionPublication(
                candidateProviderRank = 1,
                pendingProviderRanks = listOf(0),
                forcedSource = true,
                alreadyPublished = false
            )
        )
    }

    @Test
    fun publishedArtworkIsNeverHeldBackFromAnUpgrade() {
        assertFalse(
            shouldHoldMotionPublication(
                candidateProviderRank = 1,
                pendingProviderRanks = listOf(0),
                forcedSource = false,
                alreadyPublished = true
            )
        )
    }

    @Test
    fun runtimeProviderOrderHonorsActiveProvidersAndSpotifyPreference() {
        val configured = MotionArtworkConfig().normalized().providerOrder
        val withoutSpotifySession = motionArtworkProviderOrder(
            configured,
            com.yasuda.derbis.domain.DerbisCanvasSource.Auto,
            preferSpotifyCanvas = false
        )
        val withSpotifySession = motionArtworkProviderOrder(
            configured,
            com.yasuda.derbis.domain.DerbisCanvasSource.Auto,
            preferSpotifyCanvas = true
        )

        assertTrue(
            withoutSpotifySession == listOf(
                AppleMusicAnimatedArtworkProvider.PROVIDER_ID,
                SpotifyCanvasProvider.PROVIDER_ID,
                DerbisCanvasCatalogProvider.PROVIDER_ID
            )
        )
        assertTrue(
            withSpotifySession == listOf(
                SpotifyCanvasProvider.PROVIDER_ID,
                AppleMusicAnimatedArtworkProvider.PROVIDER_ID,
                DerbisCanvasCatalogProvider.PROVIDER_ID
            )
        )
    }

    @Test
    fun settleWindowStaysShortEnoughToAvoidVisibleMotionDelay() {
        assertTrue(MOTION_PRIORITY_SETTLE_MS in 500L..2_000L)
    }
}
