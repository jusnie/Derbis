package com.yasuda.derbis.ui.player

import com.yasuda.derbis.domain.PlayerVisualMode
import org.junit.Assert.assertEquals
import org.junit.Test

class BeatSyncDeckScopeTest {
    @Test
    fun onlyBeatSyncUsesTheAuraRenderer() {
        assertEquals(
            listOf(PlayerVisualMode.CanvasImmersive),
            PlayerVisualMode.entries.filter { it.deckLayout() == PlayerDeckLayout.Aura }
        )
    }

    @Test
    fun everyOtherDeckKeepsItsOwnRenderer() {
        val unchanged = mapOf(
            PlayerVisualMode.Artwork to PlayerDeckLayout.Poster,
            PlayerVisualMode.CanvasCard to PlayerDeckLayout.Sleeve,
            PlayerVisualMode.AppleMusic13 to PlayerDeckLayout.AppleMusic13,
            PlayerVisualMode.TidalMusic to PlayerDeckLayout.TidalMusic,
            PlayerVisualMode.Pulse to PlayerDeckLayout.Pulse,
            PlayerVisualMode.Halo to PlayerDeckLayout.Halo,
            PlayerVisualMode.Folio to PlayerDeckLayout.Folio,
            PlayerVisualMode.Flow to PlayerDeckLayout.Flow
        )
        unchanged.forEach { (mode, layout) -> assertEquals(layout, mode.deckLayout()) }
    }

    @Test
    fun theExistingSavedBeatSyncModeStillSelectsOnlyBeatSync() {
        assertEquals(PlayerVisualMode.CanvasImmersive, PlayerVisualMode.from("CanvasImmersive"))
        assertEquals(PlayerVisualMode.CanvasImmersive, PlayerVisualMode.from("canvas_immersive"))
        assertEquals(PlayerDeckLayout.Aura, PlayerVisualMode.from("CanvasImmersive").deckLayout())
    }
}
