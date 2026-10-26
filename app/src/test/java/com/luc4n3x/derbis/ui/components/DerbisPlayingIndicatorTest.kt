package com.luc4n3x.derbis.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisPlayingIndicatorTest {

    @Test
    fun `indicator animates only during active playback with motion enabled`() {
        assertTrue(playingIndicatorAnimates(playing = true, animationsEnabled = true))
        assertFalse(playingIndicatorAnimates(playing = false, animationsEnabled = true))
        assertFalse(playingIndicatorAnimates(playing = true, animationsEnabled = false))
    }
}
