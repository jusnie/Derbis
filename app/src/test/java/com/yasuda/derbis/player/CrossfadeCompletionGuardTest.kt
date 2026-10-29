package com.yasuda.derbis.player

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfadeCompletionGuardTest {

    @Test
    fun `outgoing deck completion stays suppressed through the post handoff guard`() {
        assertTrue(shouldSuppressCrossfadeCompletion(true, nowElapsedMs = 40L, completionGuardUntilElapsedMs = 0L))
        assertTrue(shouldSuppressCrossfadeCompletion(false, nowElapsedMs = 999L, completionGuardUntilElapsedMs = 1_000L))
        assertFalse(shouldSuppressCrossfadeCompletion(false, nowElapsedMs = 1_000L, completionGuardUntilElapsedMs = 1_000L))
    }

    @Test
    fun `service controller and view model all reject a stale outgoing completion`() {
        val service = source("app/src/main/java/com/yasuda/derbis/player/PlaybackService.kt")
        val player = source("app/src/main/java/com/yasuda/derbis/player/DerbisPlayer.kt")
        val viewModel = source("app/src/main/java/com/yasuda/derbis/viewmodel/DerbisViewModel.kt")

        assertTrue(service.contains("if (handoffCommitted) armCrossfadeCompletionGuard()"))
        assertTrue(service.contains("!isQueueTransitionCompletionSuppressed"))
        assertTrue(player.contains("if (PlaybackService.isQueueTransitionCompletionSuppressed) return"))
        assertTrue(viewModel.contains("if (PlaybackService.isQueueTransitionCompletionSuppressed) return"))
    }

    private fun source(path: String): String = sequenceOf(Path.of(path), Path.of(path.removePrefix("app/")))
        .firstOrNull(Files::exists)?.let(Files::readString) ?: error("$path not found")
}
