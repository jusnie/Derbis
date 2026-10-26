package com.luc4n3x.derbis.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueuePrefetchPolicyTest {

    @Test
    fun `next track preload stays enabled by default`() {
        assertTrue(DerbisAudioSettings().preloadNextTrack)
        assertTrue(queuePrefetchAllowed(DerbisAudioSettings()))
    }

    @Test
    fun `disabling preload blocks the optional queue prefetch`() {
        assertFalse(queuePrefetchAllowed(DerbisAudioSettings(preloadNextTrack = false)))
    }

    @Test
    fun `preload stays independent from gapless`() {
        assertTrue(queuePrefetchAllowed(DerbisAudioSettings(gaplessEnabled = false)))
        assertFalse(queuePrefetchAllowed(DerbisAudioSettings(gaplessEnabled = true, preloadNextTrack = false)))
    }
}
