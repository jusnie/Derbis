package com.luc4n3x.levyra.player

import org.junit.Assert.assertFalse
import org.junit.Test

class EightDAudioProcessorTest {
    @Test fun `processor is inactive until configured and toggle defaults off`() {
        val processor = EightDAudioProcessor()
        assertFalse(processor.enabled)
        assertFalse(processor.isActive)
    }
}
