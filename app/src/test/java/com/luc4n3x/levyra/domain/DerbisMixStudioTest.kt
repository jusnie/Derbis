package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisMixStudioTest {
    @Test
    fun autoUsesBeatAlignedBlendOnlyForUsableCompatibleTracks() {
        val outgoing = track("out", energy = 58, bpm = 120f, key = "8A")
        val incoming = track("in", energy = 60, bpm = 122f, key = "9A")

        val plan = DerbisMixStudio.autoTransition(outgoing, incoming)

        assertEquals(DerbisMixTransitionPreset.Blend, plan.preset)
        assertTrue(plan.beatSyncEnabled)
        assertTrue(plan.keyCompatible)
        assertEquals(120f / 122f, plan.incomingTempo, .001f)
        assertTrue(plan.durationMs >= DerbisMixStudio.MIN_TRANSITION_MS)
    }

    @Test
    fun uncertainOrDistantTempoDoesNotClaimBeatSync() {
        val outgoing = track("out", bpm = 90f, confidence = .4f)
        val incoming = track("in", bpm = 130f)

        val plan = DerbisMixStudio.autoTransition(outgoing, incoming)

        assertFalse(plan.beatSyncEnabled)
        assertEquals(1f, plan.incomingTempo, .001f)
        assertEquals(DerbisMixTransitionPreset.Fade, plan.preset)
    }

    @Test
    fun cutUsesShortestSupportedHandoff() {
        val plan = DerbisMixStudio.autoTransition(
            track("out", durationMs = 240_000L),
            track("in", durationMs = 240_000L),
            DerbisMixTransitionPreset.Cut
        )

        assertEquals(DerbisMixStudio.MIN_TRANSITION_MS, plan.durationMs)
    }

    @Test
    fun smartReorderPrefersCompatibleNeighbor() {
        val first = track("first", energy = 52, bpm = 120f, key = "8A")
        val distant = track("distant", energy = 95, bpm = 174f, key = "2B")
        val compatible = track("compatible", energy = 55, bpm = 121f, key = "9A")

        val reordered = DerbisMixStudio.smartReorder(listOf(first, distant, compatible))

        assertEquals(listOf("first", "compatible", "distant"), reordered.map { it.id })
    }

    @Test
    fun camelotRulesSupportAdjacentAndRelativeKeysOnly() {
        assertTrue(DerbisMixStudio.camelotCompatible("8A", "9A"))
        assertTrue(DerbisMixStudio.camelotCompatible("8A", "8B"))
        assertFalse(DerbisMixStudio.camelotCompatible("8A", "10B"))
        assertFalse(DerbisMixStudio.camelotCompatible("unknown", "8A"))
    }

    private fun track(
        id: String,
        durationMs: Long = 240_000L,
        energy: Int = 50,
        bpm: Float? = null,
        confidence: Float = .9f,
        key: String = ""
    ) = DerbisMixStudioTrack(
        id = id,
        durationMs = durationMs,
        energy = energy,
        beatGrid = DerbisBeatGrid(
            bpm = bpm,
            downbeatMs = 0L,
            confidence = confidence,
            camelotKey = key
        )
    )
}
