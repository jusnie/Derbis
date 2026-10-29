package com.yasuda.derbis.feature.player.presentation

import com.yasuda.derbis.architecture.mobius.DerbisViewMapper
import com.yasuda.derbis.feature.player.domain.PlayerModel

data class PlayerViewData(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val isPlaying: Boolean,
    val isResolving: Boolean,
    val isVideoMode: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val progress: Float,
    val canPlay: Boolean,
    val canPause: Boolean,
    val canSeek: Boolean,
    val canSkip: Boolean,
    val errorMessage: String?
)

object PlayerViewDataMapper : DerbisViewMapper<PlayerModel, PlayerViewData> {
    override fun map(model: PlayerModel): PlayerViewData {
        val track = model.currentTrack
        return PlayerViewData(
            title = track?.title.orEmpty(),
            artist = track?.artist.orEmpty(),
            album = track?.album.orEmpty(),
            artworkUrl = track?.let { it.largeThumbnailUrl.ifBlank { it.thumbnailUrl } }.orEmpty(),
            isPlaying = model.isPlaying,
            isResolving = model.isResolving,
            isVideoMode = model.isVideoMode,
            positionMs = model.safePositionMs,
            durationMs = model.safeDurationMs,
            progress = model.progress,
            canPlay = track != null && !model.isPlaying,
            canPause = track != null && model.isPlaying,
            canSeek = track != null && model.safeDurationMs > 0L,
            canSkip = model.queue.isNotEmpty(),
            errorMessage = model.errorMessage
        )
    }
}
