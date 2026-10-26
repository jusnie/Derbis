package com.luc4n3x.derbis.viewmodel

import com.luc4n3x.derbis.domain.PlayerBackgroundMode
import com.luc4n3x.derbis.domain.PlayerVisualMode
import com.luc4n3x.derbis.domain.ReactiveAuraPreset
import com.luc4n3x.derbis.domain.DerbisSkin

fun DerbisViewModel.setPlayerVisualMode(mode: PlayerVisualMode) {
    val current = state.value.interfaceSettings
    val targetSkin = if (mode == PlayerVisualMode.TidalMusic) DerbisSkin.TidalStyle else current.skin
    if (current.playerVisualMode == mode && current.skin == targetSkin) return
    // Tidal Music is an original Derbis deck that deliberately selects the matching local skin.
    setInterfaceSettings(current.copy(playerVisualMode = mode, skin = targetSkin))
}

fun DerbisViewModel.setPlayerBackground(mode: PlayerBackgroundMode) {
    val current = state.value.interfaceSettings
    if (current.playerBackground == mode) return
    setInterfaceSettings(current.copy(playerBackground = mode))
}

fun DerbisViewModel.setReactiveAuraPreset(preset: ReactiveAuraPreset) {
    val current = state.value.interfaceSettings
    if (current.reactiveAuraPreset == preset) return
    setInterfaceSettings(current.copy(reactiveAuraPreset = preset))
}

fun DerbisViewModel.setReactiveAuraIntensity(intensity: Int) {
    val current = state.value.interfaceSettings
    val coerced = intensity.coerceIn(0, 100)
    if (current.reactiveAuraIntensity == coerced) return
    setInterfaceSettings(current.copy(reactiveAuraIntensity = coerced))
}
