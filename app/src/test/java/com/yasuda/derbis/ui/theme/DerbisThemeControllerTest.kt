package com.yasuda.derbis.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DerbisThemeControllerTest {

    @Test
    fun amoledPaletteDoesNotImplicitlyEnablePureBlackMode() {
        try {
            DerbisThemeController.apply(DerbisThemes.AMOLED, pureBlack = false)
            assertFalse(DerbisIsPureBlack)

            DerbisThemeController.apply(DerbisThemes.AMOLED, pureBlack = true)
            assertTrue(DerbisIsPureBlack)

            DerbisThemeController.apply(DerbisThemes.MINIMAL_WHITE, pureBlack = true)
            assertFalse(DerbisIsPureBlack)
        } finally {
            DerbisThemeController.apply(DerbisThemes.COSMIC, pureBlack = false)
        }
    }
}
