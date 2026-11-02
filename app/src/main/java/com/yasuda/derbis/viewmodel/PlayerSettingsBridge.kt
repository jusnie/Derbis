package com.yasuda.derbis.viewmodel

import com.yasuda.derbis.domain.PlayerVisualMode
import com.yasuda.derbis.domain.DerbisSkin

fun DerbisViewModel.setPlayerVisualMode(mode: PlayerVisualMode) {
    val current = state.value.interfaceSettings
    val targetSkin = if (mode == PlayerVisualMode.TidalMusic) DerbisSkin.TidalStyle else current.skin
    if (current.playerVisualMode == mode && current.skin == targetSkin) return
    // Tidal Music is an original Derbis deck that deliberately selects the matching local skin.
    setInterfaceSettings(current.copy(playerVisualMode = mode, skin = targetSkin))
}
