package com.yasuda.derbis.feature.player.domain

import com.yasuda.derbis.architecture.mobius.DerbisNext
import com.yasuda.derbis.architecture.mobius.DerbisUpdate
import com.yasuda.derbis.domain.RepeatMode
import com.yasuda.derbis.domain.Track

object PlayerUpdate : DerbisUpdate<PlayerModel, PlayerEvent, PlayerEffect> {
    override fun update(model: PlayerModel, event: PlayerEvent): DerbisNext<PlayerModel, PlayerEffect> {
        val safeModel = model.normalized()
        return when (event) {
            is PlayerEvent.TrackRequested -> onTrackRequested(safeModel, event)
            PlayerEvent.PlayClicked -> onPlayClicked(safeModel)
            PlayerEvent.PauseClicked -> onPauseClicked(safeModel)
            PlayerEvent.TogglePlayClicked -> if (safeModel.isPlaying) onPauseClicked(safeModel) else onPlayClicked(safeModel)
            PlayerEvent.StopClicked -> DerbisNext.dispatch(safeModel.copy(isPlaying = false, isResolving = false, positionMs = 0L, pendingSeekMs = null), PlayerEffect.StopPlayback)
            PlayerEvent.NextClicked -> DerbisNext.dispatch(safeModel.copy(errorMessage = null), PlayerEffect.SelectNextTrack)
            PlayerEvent.PreviousClicked -> DerbisNext.dispatch(safeModel.copy(errorMessage = null), PlayerEffect.SelectPreviousTrack)
            is PlayerEvent.SeekRequested -> onSeekRequested(safeModel, event)
            is PlayerEvent.ProgressChanged -> DerbisNext.next(safeModel.copy(positionMs = event.positionMs.coerceAtLeast(0L), durationMs = event.durationMs.coerceAtLeast(0L)))
            PlayerEvent.ResolveStarted -> DerbisNext.next(safeModel.copy(isResolving = true, errorMessage = null))
            is PlayerEvent.ResolveSucceeded -> onResolveSucceeded(safeModel, event.track)
            is PlayerEvent.ResolveFailed -> onResolveFailed(safeModel, event.message)
            PlayerEvent.PlaybackStarted -> DerbisNext.next(safeModel.copy(isPlaying = true, isResolving = false, errorMessage = null))
            PlayerEvent.PlaybackPaused -> DerbisNext.dispatch(safeModel.copy(isPlaying = false), PlayerEffect.PersistSnapshot(safeModel.currentTrack, safeModel.safePositionMs, false))
            PlayerEvent.PlaybackCompleted -> onPlaybackCompleted(safeModel)
            is PlayerEvent.QueueChanged -> DerbisNext.next(safeModel.copy(queue = event.queue).normalized())
            is PlayerEvent.VideoModeChanged -> onVideoModeChanged(safeModel, event.enabled)
            is PlayerEvent.RepeatModeChanged -> DerbisNext.next(safeModel.copy(repeatMode = event.repeatMode))
            is PlayerEvent.ShuffleChanged -> DerbisNext.next(safeModel.copy(shuffleEnabled = event.enabled))
            PlayerEvent.ErrorConsumed -> DerbisNext.next(safeModel.copy(errorMessage = null))
        }
    }

    private fun onTrackRequested(model: PlayerModel, event: PlayerEvent.TrackRequested): DerbisNext<PlayerModel, PlayerEffect> {
        val queue = event.queue.ifEmpty { model.queue }
        val nextModel = model.copy(
            currentTrack = event.track,
            queue = queue,
            isPlaying = event.startPlaying,
            isResolving = event.startPlaying,
            positionMs = 0L,
            durationMs = event.track.durationMs.coerceAtLeast(0L),
            errorMessage = null,
            pendingSeekMs = null
        ).normalized()
        return if (event.startPlaying) {
            DerbisNext.dispatch(nextModel, PlayerEffect.ResolveTrack(event.track, nextModel.isVideoMode), PlayerEffect.LoadLyrics(event.track))
        } else {
            DerbisNext.dispatch(nextModel, PlayerEffect.LoadLyrics(event.track), PlayerEffect.PersistSnapshot(event.track, 0L, false))
        }
    }

    private fun onPlayClicked(model: PlayerModel): DerbisNext<PlayerModel, PlayerEffect> {
        val track = model.currentTrack ?: return DerbisNext.next(model)
        return when {
            model.isResolving -> DerbisNext.next(model.copy(isPlaying = true, errorMessage = null))
            track.playableUrlFor(model.isVideoMode).isBlank() -> {
                val unresolvedTrack = track.clearResolvedStreams()
                DerbisNext.dispatch(
                    model.copy(
                        currentTrack = unresolvedTrack,
                        isPlaying = true,
                        isResolving = true,
                        errorMessage = null
                    ),
                    PlayerEffect.ResolveTrack(unresolvedTrack, model.isVideoMode)
                )
            }
            else -> DerbisNext.dispatch(model.copy(isPlaying = true, isResolving = false, errorMessage = null), PlayerEffect.StartPlayback(track))
        }
    }

    private fun onPauseClicked(model: PlayerModel): DerbisNext<PlayerModel, PlayerEffect> =
        DerbisNext.dispatch(model.copy(isPlaying = false), PlayerEffect.PausePlayback, PlayerEffect.PersistSnapshot(model.currentTrack, model.safePositionMs, false))

    private fun onSeekRequested(model: PlayerModel, event: PlayerEvent.SeekRequested): DerbisNext<PlayerModel, PlayerEffect> {
        val position = event.positionMs.coerceAtLeast(0L)
        return if (model.currentTrack == null) {
            DerbisNext.next(model.copy(positionMs = position, pendingSeekMs = position))
        } else {
            DerbisNext.dispatch(model.copy(positionMs = position, pendingSeekMs = position), PlayerEffect.SeekTo(position), PlayerEffect.PersistSnapshot(model.currentTrack, position, model.isPlaying))
        }
    }

    private fun onResolveSucceeded(model: PlayerModel, track: Track): DerbisNext<PlayerModel, PlayerEffect> {
        val updated = model.copy(currentTrack = track, isResolving = false, durationMs = track.durationMs.coerceAtLeast(0L), errorMessage = null)
        val effects = buildSet {
            if (model.isPlaying) add(PlayerEffect.StartPlayback(track))
            model.pendingSeekMs?.let { add(PlayerEffect.SeekTo(it)) }
            add(PlayerEffect.PersistSnapshot(track, updated.safePositionMs, updated.isPlaying))
        }
        return DerbisNext(updated.copy(pendingSeekMs = null), effects)
    }

    private fun onResolveFailed(model: PlayerModel, message: String): DerbisNext<PlayerModel, PlayerEffect> {
        val clean = message.trim().ifBlank { "Riproduzione non riuscita" }
        return DerbisNext.dispatch(model.copy(isPlaying = false, isResolving = false, errorMessage = clean), PlayerEffect.ReportPlaybackError(clean))
    }

    private fun onPlaybackCompleted(model: PlayerModel): DerbisNext<PlayerModel, PlayerEffect> =
        when {
            model.repeatMode == RepeatMode.One && model.currentTrack != null -> DerbisNext.dispatch(model.copy(positionMs = 0L, isPlaying = true), PlayerEffect.StartPlayback(model.currentTrack))
            model.queue.isNotEmpty() -> DerbisNext.dispatch(model.copy(positionMs = 0L), PlayerEffect.SelectNextTrack)
            else -> DerbisNext.dispatch(model.copy(isPlaying = false, positionMs = model.safeDurationMs), PlayerEffect.PersistSnapshot(model.currentTrack, model.safeDurationMs, false))
        }

    private fun onVideoModeChanged(model: PlayerModel, enabled: Boolean): DerbisNext<PlayerModel, PlayerEffect> {
        if (model.isVideoMode == enabled) {
            return DerbisNext.next(model.copy(errorMessage = null))
        }

        val unresolvedTrack = model.currentTrack?.clearResolvedStreams()
        val updated = model.copy(
            currentTrack = unresolvedTrack,
            isVideoMode = enabled,
            isResolving = unresolvedTrack != null && model.isPlaying,
            errorMessage = null
        )

        return if (unresolvedTrack != null && model.isPlaying) {
            DerbisNext.dispatch(updated, PlayerEffect.ResolveTrack(unresolvedTrack, enabled))
        } else {
            DerbisNext.next(updated)
        }
    }

    private fun Track.clearResolvedStreams(): Track =
        copy(streamUrl = "", videoStreamUrl = "")

    private fun Track.playableUrlFor(isVideoMode: Boolean): String =
        if (isVideoMode) videoStreamUrl else streamUrl
}
