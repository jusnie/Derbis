package com.luc4n3x.levyra.feature.motion

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
    fun runtimeProviderOrderContainsOnlyTheOwnedCatalog() {
        val order = motionArtworkProviderOrder(MotionArtworkConfig().normalized().providerOrder, com.luc4n3x.levyra.domain.LevyraCanvasSource.Auto)

        assertTrue(order == listOf(DerbisCanvasCatalogProvider.PROVIDER_ID))
    }

    @Test
    fun settleWindowStaysShortEnoughToAvoidVisibleMotionDelay() {
        assertTrue(MOTION_PRIORITY_SETTLE_MS in 500L..2_000L)
    }
}
