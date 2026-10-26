package com.luc4n3x.derbis.feature.player.domain

import com.luc4n3x.derbis.architecture.mobius.DerbisInit
import com.luc4n3x.derbis.architecture.mobius.DerbisNext

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
