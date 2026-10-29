package com.yasuda.derbis.player

/** UI-safe transition state emitted only while the two decks overlap. */
data class CrossfadeProgress(
    val active: Boolean = false,
    val fraction: Float = 0f,
    val fromTitle: String = "",
    val toTitle: String = "",
    /** Queue identity of the incoming deck after its logical handoff has begun. */
    val toQueueIdentity: String = "",
    /** Position reported from the audible incoming deck, never the outgoing primary deck. */
    val incomingPositionMs: Long = 0L,
    val incomingBufferedPositionMs: Long = 0L
) {
    val label: String
        get() = if (toTitle.isBlank()) "Crossfading…" else "Crossfading… $toTitle"
}

/**
 * Resolves the position that belongs to the queue's logical current track while decks overlap.
 * It intentionally returns null for the outgoing deck so an old primary position can never be
 * persisted against the incoming queue item.
 */
internal fun CrossfadeProgress.incomingPlaybackFor(queueIdentity: String): CrossfadeIncomingPlayback? {
    val identity = queueIdentity.trim()
    if (!active || identity.isEmpty() || toQueueIdentity != identity) return null
    val position = incomingPositionMs.coerceAtLeast(0L)
    return CrossfadeIncomingPlayback(
        positionMs = position,
        bufferedPositionMs = incomingBufferedPositionMs.coerceAtLeast(position)
    )
}

internal data class CrossfadeIncomingPlayback(
    val positionMs: Long,
    val bufferedPositionMs: Long
)

/**
 * A primary deck can report its old item as ended just after the incoming deck has become the
 * logical queue item. Keep that stale callback from advancing the queue a second time.
 */
internal fun shouldSuppressCrossfadeCompletion(
    transitionInProgress: Boolean,
    nowElapsedMs: Long,
    completionGuardUntilElapsedMs: Long
): Boolean = transitionInProgress || nowElapsedMs < completionGuardUntilElapsedMs
