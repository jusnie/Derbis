package com.yasuda.derbis.feature.player.domain

import com.yasuda.derbis.architecture.mobius.DerbisInit
import com.yasuda.derbis.architecture.mobius.DerbisNext

object PlayerInit : DerbisInit<PlayerModel, PlayerEffect> {
    override fun init(model: PlayerModel): DerbisNext<PlayerModel, PlayerEffect> {
        val normalized = model.normalized()
        val track = normalized.currentTrack
        return if (track == null) {
            DerbisNext.next(normalized)
        } else {
            DerbisNext.dispatch(
                normalized,
                PlayerEffect.PersistSnapshot(track, normalized.safePositionMs, normalized.isPlaying),
                PlayerEffect.LoadLyrics(track)
            )
        }
    }
}
