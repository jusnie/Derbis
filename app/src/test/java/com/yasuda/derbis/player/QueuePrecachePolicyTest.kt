package com.yasuda.derbis.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueuePrecachePolicyTest {
    @Test
    fun transitionReuseRequiresSameQueuePairAndFreshEntry() {
        assertTrue(
            queuePrecacheMatchesTransition(
                preparedSourceIdentity = "id:a",
                preparedTargetIdentity = "id:b",
                currentSourceIdentity = "id:a",
                currentTargetIdentity = "id:b",
                preparedAtElapsedMs = 1_000L,
                nowElapsedMs = 10_000L,
                maxAgeMs = 60_000L
            )
        )
        assertFalse(
            queuePrecacheMatchesTransition(
                preparedSourceIdentity = "id:a",
                preparedTargetIdentity = "id:b",
                currentSourceIdentity = "id:c",
                currentTargetIdentity = "id:b",
                preparedAtElapsedMs = 1_000L,
                nowElapsedMs = 10_000L,
                maxAgeMs = 60_000L
            )
        )
        assertFalse(
            queuePrecacheMatchesTransition(
                preparedSourceIdentity = "id:a",
                preparedTargetIdentity = "id:b",
                currentSourceIdentity = "id:a",
                currentTargetIdentity = "id:b",
                preparedAtElapsedMs = 1_000L,
                nowElapsedMs = 70_001L,
                maxAgeMs = 60_000L
            )
        )
    }

    @Test
    fun targetReuseSurvivesQueueAdvanceButNotDifferentTrack() {
        assertTrue(
            queuePrecacheMatchesTarget(
                preparedTargetIdentity = "id:b",
                requestedTargetIdentity = "id:b",
                preparedAtElapsedMs = 5_000L,
                nowElapsedMs = 20_000L,
                maxAgeMs = 60_000L
            )
        )
        assertFalse(
            queuePrecacheMatchesTarget(
                preparedTargetIdentity = "id:b",
                requestedTargetIdentity = "id:c",
                preparedAtElapsedMs = 5_000L,
                nowElapsedMs = 20_000L,
                maxAgeMs = 60_000L
            )
        )
    }

    @Test
    fun preparedTrackCanBeConsumedByOriginalOrResolvedId() {
        assertTrue(queuePrecacheMatchesTrackId("queue-id", "resolved-id", "queue-id"))
        assertTrue(queuePrecacheMatchesTrackId("queue-id", "resolved-id", "resolved-id"))
        assertFalse(queuePrecacheMatchesTrackId("queue-id", "resolved-id", "other"))
        assertFalse(queuePrecacheMatchesTrackId("queue-id", "resolved-id", ""))
    }

    @Test
    fun streamPreloadTriggerRespectsLeadWindowAndProgressThreshold() {
        // Standard 4-minute track: triggers when remaining <= 30s
        assertTrue(
            shouldTriggerStreamPreload(
                durationMs = 240_000L,
                positionMs = 215_000L, // 25s remaining <= 30s
                preloadEnabled = true,
                alreadyPreloaded = false
            )
        )
        assertFalse(
            shouldTriggerStreamPreload(
                durationMs = 240_000L,
                positionMs = 120_000L, // 120s remaining > 30s, 50% < 70%
                preloadEnabled = true,
                alreadyPreloaded = false
            )
        )

        // Short 20-second track: triggers when progress >= 70%
        assertTrue(
            shouldTriggerStreamPreload(
                durationMs = 20_000L,
                positionMs = 15_000L, // 75% >= 70%
                preloadEnabled = true,
                alreadyPreloaded = false
            )
        )

        // Disabled or already preloaded: does not trigger
        assertFalse(
            shouldTriggerStreamPreload(
                durationMs = 240_000L,
                positionMs = 220_000L,
                preloadEnabled = false,
                alreadyPreloaded = false
            )
        )
        assertFalse(
            shouldTriggerStreamPreload(
                durationMs = 240_000L,
                positionMs = 220_000L,
                preloadEnabled = true,
                alreadyPreloaded = true
            )
        )
    }

    @Test
    fun streamPreloadValidityRequiresFreshnessAndPreloadedFlag() {
        assertTrue(
            isStreamPreloadValid(
                preparedTargetIdentity = "track-2",
                requestedTargetIdentity = "track-2",
                streamPreloaded = true,
                preparedAtElapsedMs = 1_000L,
                nowElapsedMs = 10_000L,
                maxAgeMs = 60_000L
            )
        )
        assertFalse(
            isStreamPreloadValid(
                preparedTargetIdentity = "track-2",
                requestedTargetIdentity = "track-2",
                streamPreloaded = false,
                preparedAtElapsedMs = 1_000L,
                nowElapsedMs = 10_000L,
                maxAgeMs = 60_000L
            )
        )
        assertFalse(
            isStreamPreloadValid(
                preparedTargetIdentity = "track-2",
                requestedTargetIdentity = "track-2",
                streamPreloaded = true,
                preparedAtElapsedMs = 1_000L,
                nowElapsedMs = 70_000L,
                maxAgeMs = 60_000L
            )
        )
    }
}
