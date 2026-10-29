package com.yasuda.derbis.player.queue

import com.yasuda.derbis.domain.Track

internal fun shouldPromptForQueueDestination(queueSpaces: List<QueueSpaceSummary>): Boolean =
    queueSpaces.size > 1

internal fun mergePendingQueueDestinationTracks(
    current: List<Track>,
    additions: List<Track>
): List<Track> = (current + additions).distinctBy { track ->
    track.id.ifBlank { "${track.title}|${track.artist}" }
}
