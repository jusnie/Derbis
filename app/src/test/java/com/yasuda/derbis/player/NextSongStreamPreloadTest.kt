package com.yasuda.derbis.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Next-Song Stream Preloading (ArchiveTune pattern).
 * Validates the sliding-window trigger thresholds, freshness, token validity,
 * and 1.5 MB stream buffer allocation for gapless transitions and seamless crossfading.
 */
class NextSongStreamPreloadTest {

    @Test
    fun triggerWindowActivatesThirtySecondsBeforeEnd() {
        val songDurationMs = 180_000L // 3 minutes

        // Position at 2:00 (60s remaining) -> no trigger
        val atTwoMinutes = shouldTriggerStreamPreload(
            durationMs = songDurationMs,
            positionMs = 120_000L,
            preloadEnabled = true,
            alreadyPreloaded = false,
            triggerRemainingMs = 30_000L
        )
        assertFalse(atTwoMinutes)

        // Position at 2:31 (29s remaining <= 30s) -> triggers preload
        val atThirtySeconds = shouldTriggerStreamPreload(
            durationMs = songDurationMs,
            positionMs = 151_000L,
            preloadEnabled = true,
            alreadyPreloaded = false,
            triggerRemainingMs = 30_000L
        )
        assertTrue(atThirtySeconds)

        // Once already preloaded, should not re-trigger
        val alreadyCached = shouldTriggerStreamPreload(
            durationMs = songDurationMs,
            positionMs = 155_000L,
            preloadEnabled = true,
            alreadyPreloaded = true,
            triggerRemainingMs = 30_000L
        )
        assertFalse(alreadyCached)
    }

    @Test
    fun shortTracksTriggerAtSeventyPercentProgress() {
        val shortTrackDurationMs = 25_000L // 25 seconds (less than 30s trigger window)

        // At 10 seconds (40%) -> no trigger
        assertFalse(
            shouldTriggerStreamPreload(
                durationMs = shortTrackDurationMs,
                positionMs = 10_000L,
                preloadEnabled = true,
                alreadyPreloaded = false
            )
        )

        // At 18 seconds (72% >= 70%) -> triggers preload
        assertTrue(
            shouldTriggerStreamPreload(
                durationMs = shortTrackDurationMs,
                positionMs = 18_000L,
                preloadEnabled = true,
                alreadyPreloaded = false
            )
        )
    }

    @Test
    fun preloadDisabledSettingStrictlySuppressesPreload() {
        // Even within 5 seconds of the end, preload is suppressed if disabled by user
        assertFalse(
            shouldTriggerStreamPreload(
                durationMs = 200_000L,
                positionMs = 195_000L,
                preloadEnabled = false,
                alreadyPreloaded = false
            )
        )
    }

    @Test
    fun expiredPrecacheTriggersFreshResolution() {
        val maxAgeMs = 15L * 60L * 1_000L // 15 minutes
        val preparedAt = 100_000L

        // Fresh at 5 minutes
        assertTrue(isQueuePrecacheFresh(preparedAt, preparedAt + 5L * 60L * 1_000L, maxAgeMs))

        // Expired at 16 minutes (e.g. paused for a long time)
        assertFalse(isQueuePrecacheFresh(preparedAt, preparedAt + 16L * 60L * 1_000L, maxAgeMs))
    }

    @Test
    fun streamPreloadByteConstantsAlignWithArchiveTuneArchitecture() {
        // ArchiveTune buffer: 1.5 MB audio chunk (~15-20s of Opus 160k / AAC 256k)
        val expectedPreloadBytes = 1536L * 1024L
        assertEquals(expectedPreloadBytes, PlaybackWarmup.STREAM_PRELOAD_BYTES)
    }
}
