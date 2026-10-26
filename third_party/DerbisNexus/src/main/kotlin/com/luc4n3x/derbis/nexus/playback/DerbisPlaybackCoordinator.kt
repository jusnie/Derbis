package com.luc4n3x.derbis.nexus.playback

enum class DerbisTransitionReason {
    USER_NEXT,
    USER_PREVIOUS,
    USER_SELECT,
    AUTO_ADVANCE,
    REPEAT,
    ERROR_RECOVERY,
    QUEUE_REPLACED
}

data class DerbisPlaybackSnapshot(
    val trackId: String,
    val positionMs: Long,
    val durationMs: Long,
    val ended: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val completionRatio: Double
        get() = if (durationMs <= 0L) 0.0 else positionMs.coerceIn(0L, durationMs).toDouble() / durationMs.toDouble()
}

sealed interface DerbisPlaybackSignal {
    val trackId: String

    data class Completed(
        override val trackId: String,
        val completionRatio: Double,
        val reason: DerbisTransitionReason
    ) : DerbisPlaybackSignal

    data class Skipped(
        override val trackId: String,
        val completionRatio: Double,
        val reason: DerbisTransitionReason
    ) : DerbisPlaybackSignal

    data class Replayed(
        override val trackId: String
    ) : DerbisPlaybackSignal

    data class Failed(
        override val trackId: String,
        val completionRatio: Double
    ) : DerbisPlaybackSignal

    data class Transitioned(
        override val trackId: String,
        val completionRatio: Double,
        val reason: DerbisTransitionReason
    ) : DerbisPlaybackSignal
}

class DerbisPlaybackCoordinator(
    private val completionThreshold: Double = 0.82
) {
    init {
        require(completionThreshold in 0.5..1.0)
    }

    fun classify(
        snapshot: DerbisPlaybackSnapshot,
        reason: DerbisTransitionReason
    ): DerbisPlaybackSignal {
        val ratio = snapshot.completionRatio
        return when (reason) {
            DerbisTransitionReason.AUTO_ADVANCE -> if (snapshot.ended || ratio >= completionThreshold) {
                DerbisPlaybackSignal.Completed(
                    trackId = snapshot.trackId,
                    completionRatio = if (snapshot.ended) 1.0 else ratio,
                    reason = reason
                )
            } else {
                DerbisPlaybackSignal.Transitioned(snapshot.trackId, ratio, reason)
            }
            DerbisTransitionReason.REPEAT -> DerbisPlaybackSignal.Replayed(snapshot.trackId)
            DerbisTransitionReason.ERROR_RECOVERY -> DerbisPlaybackSignal.Failed(snapshot.trackId, ratio)
            DerbisTransitionReason.QUEUE_REPLACED -> DerbisPlaybackSignal.Transitioned(snapshot.trackId, ratio, reason)
            DerbisTransitionReason.USER_NEXT,
            DerbisTransitionReason.USER_PREVIOUS,
            DerbisTransitionReason.USER_SELECT -> if (snapshot.ended || ratio >= completionThreshold) {
                DerbisPlaybackSignal.Completed(snapshot.trackId, if (snapshot.ended) 1.0 else ratio, reason)
            } else {
                DerbisPlaybackSignal.Skipped(snapshot.trackId, ratio, reason)
            }
        }
    }
}
