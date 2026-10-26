package com.luc4n3x.derbis.player.queue

import com.luc4n3x.derbis.domain.Track

internal fun shouldPromptForQueueDestination(queueSpaces: List<QueueSpaceSummary>): Boolean =
    queueSpaces.size > 1

internal fun mergePendingQueueDestinationTracks(
    current: List<Track>,
    additions: List<Track>
): List<Track> = (current + additions).distinctBy { track ->
    track.id.ifBlank { "${track.title}|${track.artist}" }
}
