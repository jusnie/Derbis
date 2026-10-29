package com.yasuda.derbis.feature.player.bridge

import com.yasuda.derbis.feature.player.domain.PlayerModel
import com.yasuda.derbis.viewmodel.DerbisUiState

object DerbisPlayerStateBridge {
    fun fromUiState(state: DerbisUiState): PlayerModel = PlayerModel(
        currentTrack = state.currentTrack,
        queue = state.queue,
        isPlaying = state.isPlaying,
        isResolving = state.isResolving,
        isVideoMode = state.isVideoMode,
        repeatMode = state.repeatMode,
        shuffleEnabled = state.shuffleEnabled,
        positionMs = state.positionMs,
        durationMs = state.durationMs,
        errorMessage = state.playerError
    ).normalized()
}
