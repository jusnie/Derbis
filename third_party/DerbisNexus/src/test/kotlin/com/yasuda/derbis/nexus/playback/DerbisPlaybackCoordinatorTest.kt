package com.yasuda.derbis.nexus.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisPlaybackCoordinatorTest {
    private val coordinator = DerbisPlaybackCoordinator()

    @Test
    fun naturalEndIsAlwaysCompletionEvenWithStalePosition() {
        val signal = coordinator.classify(
            DerbisPlaybackSnapshot("track", positionMs = 70_000L, durationMs = 100_000L, ended = true),
            DerbisTransitionReason.AUTO_ADVANCE
        )
        assertTrue(signal is DerbisPlaybackSignal.Completed)
        assertEquals(1.0, (signal as DerbisPlaybackSignal.Completed).completionRatio, 0.0)
    }

    @Test
    fun manualEarlyTransitionIsSkip() {
        val signal = coordinator.classify(
            DerbisPlaybackSnapshot("track", positionMs = 10_000L, durationMs = 100_000L),
            DerbisTransitionReason.USER_NEXT
        )
        assertTrue(signal is DerbisPlaybackSignal.Skipped)
    }

    @Test
    fun incompleteAutomaticTransitionIsNeutral() {
        val signal = coordinator.classify(
            DerbisPlaybackSnapshot("track", positionMs = 20_000L, durationMs = 100_000L),
            DerbisTransitionReason.AUTO_ADVANCE
        )
        assertTrue(signal is DerbisPlaybackSignal.Transitioned)
    }

    @Test
    fun repeatIsNotLearnedAsSkip() {
        val signal = coordinator.classify(
            DerbisPlaybackSnapshot("track", positionMs = 100_000L, durationMs = 100_000L),
            DerbisTransitionReason.REPEAT
        )
        assertTrue(signal is DerbisPlaybackSignal.Replayed)
    }
}
