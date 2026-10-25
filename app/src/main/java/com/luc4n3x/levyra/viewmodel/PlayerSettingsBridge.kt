package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.ReactiveAuraPreset
import com.luc4n3x.levyra.domain.LevyraSkin

fun LevyraViewModel.setPlayerVisualMode(mode: PlayerVisualMode) {
    val current = state.value.interfaceSettings
    val targetSkin = if (mode == PlayerVisualMode.TidalMusic) LevyraSkin.TidalStyle else current.skin
    if (current.playerVisualMode == mode && current.skin == targetSkin) return
    // Tidal Music is an original Derbis deck that deliberately selects the matching local skin.
    setInterfaceSettings(current.copy(playerVisualMode = mode, skin = targetSkin))
}

fun LevyraViewModel.setPlayerBackground(mode: PlayerBackgroundMode) {
    val current = state.value.interfaceSettings
    if (current.playerBackground == mode) return
    setInterfaceSettings(current.copy(playerBackground = mode))
}

fun LevyraViewModel.setReactiveAuraPreset(preset: ReactiveAuraPreset) {
    val current = state.value.interfaceSettings
    if (current.reactiveAuraPreset == preset) return
    setInterfaceSettings(current.copy(reactiveAuraPreset = preset))
}

fun LevyraViewModel.setReactiveAuraIntensity(intensity: Int) {
    val current = state.value.interfaceSettings
    val coerced = intensity.coerceIn(0, 100)
    if (current.reactiveAuraIntensity == coerced) return
    setInterfaceSettings(current.copy(reactiveAuraIntensity = coerced))
}
