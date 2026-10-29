package com.yasuda.derbis.feature.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkVerificationPlanTest {
    @Test
    fun fullCatalogCandidateBudgetKeepsSecondaryCandidates() {
        val ranked = listOf(
            rankedCandidate("catalog-1", providerRank = 0),
            rankedCandidate("catalog-2", providerRank = 0),
            rankedCandidate("catalog-3", providerRank = 0),
            rankedCandidate("secondary", providerRank = 1)
        )

        val plan = buildMotionArtworkVerificationPlan(ranked)

        assertEquals(listOf("catalog-1", "catalog-2", "catalog-3", "secondary"), plan.candidates.map { it.candidate.url })
        assertTrue(plan.exhaustive)
    }

    @Test
    fun oversizedProviderSetRemainsBoundedWithoutDroppingFallbackProviders() {
        val ranked = listOf(
            rankedCandidate("catalog-1", providerRank = 0),
            rankedCandidate("catalog-2", providerRank = 0),
            rankedCandidate("catalog-3", providerRank = 0),
            rankedCandidate("catalog-4", providerRank = 0),
            rankedCandidate("secondary", providerRank = 1),
            rankedCandidate("tertiary", providerRank = 2)
        )

        val plan = buildMotionArtworkVerificationPlan(ranked)

        assertEquals(
            listOf("catalog-1", "catalog-2", "catalog-3", "secondary", "tertiary"),
            plan.candidates.map { it.candidate.url }
        )
        assertFalse(plan.exhaustive)
    }

    @Test
    fun transientProviderOrVerifierFailuresNeverBecomeGlobalNegativeCacheEntries() {
        assertTrue(
            shouldNegativeCacheMotionArtwork(
                providerFailed = false,
                verifierFailed = false,
                verificationExhaustive = true
            )
        )
        assertFalse(
            shouldNegativeCacheMotionArtwork(
                providerFailed = true,
                verifierFailed = false,
                verificationExhaustive = true
            )
        )
        assertFalse(
            shouldNegativeCacheMotionArtwork(
                providerFailed = false,
                verifierFailed = true,
                verificationExhaustive = true
            )
        )
        assertFalse(
            shouldNegativeCacheMotionArtwork(
                providerFailed = false,
                verifierFailed = false,
                verificationExhaustive = false
            )
        )
    }

    private fun rankedCandidate(url: String, providerRank: Int): MotionArtworkRankedCandidate =
        MotionArtworkRankedCandidate(
            candidate = MotionArtworkCandidate(
                provider = when (providerRank) {
                    0 -> "catalog-primary"
                    1 -> "catalog-secondary"
                    else -> "catalog-tertiary"
                },
                scope = MotionArtworkScope.TRACK,
                identity = MotionTrackIdentity(
                    title = "Track",
                    artists = listOf("Artist"),
                    album = "Album",
                    durationMs = 180_000L,
                    isrc = "",
                    upc = "",
                    year = "",
                    trackId = url,
                    albumId = "album"
                ),
                url = url,
                mimeType = "video/mp4",
                expiresAtMs = Long.MAX_VALUE
            ),
            confidence = 100,
            providerRank = providerRank
        )
}
