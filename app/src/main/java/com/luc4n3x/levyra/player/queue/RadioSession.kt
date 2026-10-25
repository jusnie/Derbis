package com.luc4n3x.levyra.player.queue

import com.luc4n3x.levyra.domain.Track

internal const val RADIO_SESSION_MAX_ADDED_IDS = 120
internal const val RADIO_MIX_CONTEXT_TRACKS = 10

internal data class RadioSession(
    val spaceId: String,
    val seed: Track,
    val addedIds: Set<String> = emptySet()
) {
    /**
     * Endless listening belongs to its queue space, rather than to a finite starter batch. This
     * deliberately stays active after the original seed has scrolled out of the rolling history.
     */
    fun isActiveFor(snapshot: PlaybackQueueSnapshot): Boolean = spaceId == snapshot.spaceId

    /** A temporary empty recommendation response must not end endless listening. */
    fun recordAdded(ids: Collection<String>): RadioSession {
        if (ids.isEmpty()) return this
        val merged = LinkedHashSet<String>(addedIds.size + ids.size)
        merged.addAll(addedIds)
        merged.addAll(ids)
        val bounded = if (merged.size > RADIO_SESSION_MAX_ADDED_IDS) {
            merged.toList().takeLast(RADIO_SESSION_MAX_ADDED_IDS).toSet()
        } else {
            merged
        }
        return copy(addedIds = bounded)
    }
}

/**
 * Follow the track the listener is hearing now. This makes recommendations evolve naturally
 * instead of repeatedly building a finite random playlist from the first track.
 */
internal fun radioSeedFor(session: RadioSession?, snapshot: PlaybackQueueSnapshot): Track? =
    snapshot.currentTrack ?: session?.takeIf { it.isActiveFor(snapshot) }?.seed

internal fun radioMixContext(snapshot: PlaybackQueueSnapshot, insertAfterCurrent: Boolean): List<Track> {
    val tracks = snapshot.tracks
    if (tracks.isEmpty()) return emptyList()
    val end = if (insertAfterCurrent) (snapshot.currentIndex + 1).coerceIn(0, tracks.size) else tracks.size
    return tracks.subList((end - RADIO_MIX_CONTEXT_TRACKS).coerceAtLeast(0), end).toList()
}

internal fun radioRecentlyPlayed(snapshot: PlaybackQueueSnapshot): List<Track> =
    snapshot.history.asSequence()
        .distinct()
        .mapNotNull(snapshot.tracks::getOrNull)
        .toList()
