package com.luc4n3x.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DerbisFontPresetTest {

    @Test
    fun parsesPresetNamesIgnoringCaseAndWhitespace() {
        assertEquals(DerbisFontPreset.Manrope, DerbisFontPreset.from("  manROPE "))
        assertEquals(DerbisFontPreset.Montserrat, DerbisFontPreset.from("Montserrat"))
    }

    @Test
    fun unknownPresetFallsBackToOutfit() {
        assertEquals(DerbisFontPreset.Outfit, DerbisFontPreset.from("not-a-font"))
    }

    @Test
    fun systemPresetIsLanguageNeutralAndNeedsNoGoogleFont() {
        assertEquals("System", DerbisFontPreset.System.displayName)
        assertEquals(null, DerbisFontPreset.System.googleFontName)
    }
}
