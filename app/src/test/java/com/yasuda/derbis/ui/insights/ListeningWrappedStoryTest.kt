package com.yasuda.derbis.ui.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningWrappedStoryTest {

    @Test
    fun `reference height keeps full size type`() {
        assertEquals(1f, wrappedTypeScale(720f), 0.001f)
        // Anything taller is capped: the story never scales display type up.
        assertEquals(1f, wrappedTypeScale(1_080f), 0.001f)
    }

    @Test
    fun `short viewports shrink display type proportionally`() {
        assertEquals(0.9f, wrappedTypeScale(648f), 0.001f)
        assertEquals(0.8f, wrappedTypeScale(576f), 0.001f)
    }

    @Test
    fun `scale never collapses below the legibility floor`() {
        // Landscape phones and split screen land here; the floor protects readability.
        assertTrue(wrappedTypeScale(360f) >= 0.74f)
        assertEquals(0.74f, wrappedTypeScale(200f), 0.001f)
        // An unmeasured page agrees with the floor rather than rendering at zero size.
        assertEquals(0.74f, wrappedTypeScale(0f), 0.001f)
    }
}
