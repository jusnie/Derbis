package com.luc4n3x.derbis.domain

import kotlin.math.abs
import kotlin.math.roundToInt

enum class DerbisMixTransitionPreset {
    Auto,
    Fade,
    Rise,
    Blend,
    Cut
}

data class DerbisBeatGrid(
    val bpm: Float? = null,
    val downbeatMs: Long? = null,
    val confidence: Float = 0f,
    val camelotKey: String = ""
) {
    val validBpm: Float?
        get() = bpm?.takeIf { it.isFinite() && it in MIN_BPM..MAX_BPM }

    val beatDurationMs: Long?
        get() = validBpm?.let { (60_000f / it).roundToInt().toLong() }

    val isUsable: Boolean
        get() = validBpm != null && confidence >= MIN_CONFIDENCE

    companion object {
        const val MIN_BPM = 55f
        const val MAX_BPM = 220f
        const val MIN_CONFIDENCE = .5f
    }
}

data class DerbisMixStudioTrack(
    val id: String,
    val durationMs: Long,
    val energy: Int = 50,
    val beatGrid: DerbisBeatGrid = DerbisBeatGrid()
)

data class DerbisMixStudioTransition(
    val outgoingTrackId: String,
    val incomingTrackId: String,
    val preset: DerbisMixTransitionPreset,
    val durationMs: Long,
    val outgoingStartMs: Long,
    val incomingStartMs: Long,
    val incomingTempo: Float,
    val beatSyncEnabled: Boolean,
    val keyCompatible: Boolean
)

data class DerbisBeatSyncPlan(
    val incomingTempo: Float,
    val incomingSeekMs: Long,
    val aligned: Boolean
)

/**
 * Deterministic mix planning used by the Mix Studio editor and playback engine. It uses provided
 * beat-grid metadata only when it has enough confidence; otherwise every transition falls back to
 * a safe musical crossfade instead of pretending a track is beat matched.
 */
object DerbisMixStudio {
    const val MIN_TRANSITION_MS = 800L
    const val MAX_TRANSITION_MS = 30_000L
    const val DEFAULT_TRANSITION_MS = 6_000L
    private const val MIN_TEMPO_RATIO = .92f
    private const val MAX_TEMPO_RATIO = 1.08f

    fun autoTransition(
        outgoing: DerbisMixStudioTrack,
        incoming: DerbisMixStudioTrack,
        requestedPreset: DerbisMixTransitionPreset = DerbisMixTransitionPreset.Auto,
        bars: Int = 8
    ): DerbisMixStudioTransition {
        val preset = resolvedPreset(outgoing, incoming, requestedPreset)
        val beatSync = beatSync(outgoing.beatGrid, incoming.beatGrid)
        val duration = transitionDuration(outgoing, incoming, preset, bars, beatSync.aligned)
        val outgoingStart = (outgoing.durationMs - duration).coerceAtLeast(0L)
        val incomingStart = beatSync.incomingSeekMs
        return DerbisMixStudioTransition(
            outgoingTrackId = outgoing.id,
            incomingTrackId = incoming.id,
            preset = preset,
            durationMs = duration,
            outgoingStartMs = outgoingStart,
            incomingStartMs = incomingStart,
            incomingTempo = beatSync.incomingTempo,
            beatSyncEnabled = beatSync.aligned,
            keyCompatible = camelotCompatible(outgoing.beatGrid.camelotKey, incoming.beatGrid.camelotKey)
        )
    }

    fun beatSync(reference: DerbisBeatGrid, incoming: DerbisBeatGrid): DerbisBeatSyncPlan {
        val referenceBpm = reference.validBpm
        val incomingBpm = incoming.validBpm
        if (!reference.isUsable || !incoming.isUsable || referenceBpm == null || incomingBpm == null) {
            return DerbisBeatSyncPlan(incomingTempo = 1f, incomingSeekMs = 0L, aligned = false)
        }
        val tempo = referenceBpm / incomingBpm
        if (tempo !in MIN_TEMPO_RATIO..MAX_TEMPO_RATIO) {
            return DerbisBeatSyncPlan(incomingTempo = 1f, incomingSeekMs = 0L, aligned = false)
        }
        val incomingBeatMs = incoming.beatDurationMs ?: return DerbisBeatSyncPlan(1f, 0L, false)
        val downbeat = incoming.downbeatMs?.coerceAtLeast(0L) ?: 0L
        val seek = nearestBeatAtOrAfter(downbeat, incomingBeatMs, downbeat)
        return DerbisBeatSyncPlan(
            incomingTempo = tempo,
            incomingSeekMs = seek,
            aligned = true
        )
    }

    /** Returns a phase-aligned seek target for the incoming deck at a chosen outgoing position. */
    fun alignIncomingToReference(
        referencePositionMs: Long,
        reference: DerbisBeatGrid,
        incoming: DerbisBeatGrid
    ): DerbisBeatSyncPlan {
        val base = beatSync(reference, incoming)
        if (!base.aligned) return base
        val referenceBeatMs = reference.beatDurationMs ?: return base.copy(aligned = false)
        val incomingBeatMs = incoming.beatDurationMs ?: return base.copy(aligned = false)
        val referenceDownbeat = reference.downbeatMs?.coerceAtLeast(0L) ?: 0L
        val incomingDownbeat = incoming.downbeatMs?.coerceAtLeast(0L) ?: 0L
        val beatsFromReferenceDownbeat = ((referencePositionMs - referenceDownbeat).coerceAtLeast(0L) / referenceBeatMs)
        return base.copy(
            incomingSeekMs = incomingDownbeat + beatsFromReferenceDownbeat * incomingBeatMs
        )
    }

    /**
     * Selects the latest safe outgoing beat before a desired crossfade point. Returning null means
     * the track has no usable beat grid, so playback must retain its ordinary time-based handoff.
     */
    fun beatAlignedTransitionStartMs(
        durationMs: Long,
        desiredTransitionMs: Long,
        grid: DerbisBeatGrid
    ): Long? {
        val beatMs = grid.beatDurationMs ?: return null
        if (!grid.isUsable || durationMs <= desiredTransitionMs || beatMs <= 0L) return null
        val target = (durationMs - desiredTransitionMs).coerceAtLeast(0L)
        val origin = grid.downbeatMs?.coerceAtLeast(0L) ?: 0L
        if (target < origin) return null
        val beatIndex = (target - origin) / beatMs
        return origin + beatIndex * beatMs
    }

    /** Reorders a set so adjacent tracks have the least tempo/key/energy discontinuity. */
    fun smartReorder(tracks: List<DerbisMixStudioTrack>): List<DerbisMixStudioTrack> {
        if (tracks.size < 3) return tracks
        val pending = tracks.drop(1).toMutableList()
        val ordered = mutableListOf(tracks.first())
        while (pending.isNotEmpty()) {
            val previous = ordered.last()
            val next = pending.minByOrNull { candidate -> transitionCost(previous, candidate) } ?: break
            ordered += next
            pending.remove(next)
        }
        return ordered
    }

    fun camelotCompatible(first: String, second: String): Boolean {
        val left = camelot(first) ?: return false
        val right = camelot(second) ?: return false
        if (left.second == right.second && abs(left.first - right.first) in setOf(0, 1, 11)) return true
        return left.first == right.first && left.second != right.second
    }

    private fun resolvedPreset(
        outgoing: DerbisMixStudioTrack,
        incoming: DerbisMixStudioTrack,
        requested: DerbisMixTransitionPreset
    ): DerbisMixTransitionPreset {
        if (requested != DerbisMixTransitionPreset.Auto) return requested
        val bothBeatMatched = beatSync(outgoing.beatGrid, incoming.beatGrid).aligned
        return when {
            bothBeatMatched && abs(outgoing.energy - incoming.energy) <= 16 -> DerbisMixTransitionPreset.Blend
            incoming.energy - outgoing.energy >= 18 -> DerbisMixTransitionPreset.Rise
            else -> DerbisMixTransitionPreset.Fade
        }
    }

    private fun transitionDuration(
        outgoing: DerbisMixStudioTrack,
        incoming: DerbisMixStudioTrack,
        preset: DerbisMixTransitionPreset,
        bars: Int,
        beatMatched: Boolean
    ): Long {
        if (preset == DerbisMixTransitionPreset.Cut) return MIN_TRANSITION_MS
        val beats = bars.coerceIn(2, 32) * 4
        val referenceBeatMs = outgoing.beatGrid.beatDurationMs
        val gridDuration = if (beatMatched && referenceBeatMs != null) beats * referenceBeatMs else DEFAULT_TRANSITION_MS
        val shaped = when (preset) {
            DerbisMixTransitionPreset.Rise -> gridDuration + 1_000L
            DerbisMixTransitionPreset.Blend -> gridDuration
            DerbisMixTransitionPreset.Fade, DerbisMixTransitionPreset.Auto -> gridDuration
            DerbisMixTransitionPreset.Cut -> MIN_TRANSITION_MS
        }
        val safeMaximum = minOf(
            MAX_TRANSITION_MS,
            outgoing.durationMs.takeIf { it > 0L }?.div(2L) ?: MAX_TRANSITION_MS,
            incoming.durationMs.takeIf { it > 0L }?.div(2L) ?: MAX_TRANSITION_MS
        ).coerceAtLeast(MIN_TRANSITION_MS)
        return shaped.coerceIn(MIN_TRANSITION_MS, safeMaximum)
    }

    private fun transitionCost(first: DerbisMixStudioTrack, second: DerbisMixStudioTrack): Float {
        val bpmCost = tempoDistance(first.beatGrid.validBpm, second.beatGrid.validBpm) * 45f
        val keyCost = when {
            first.beatGrid.camelotKey.isBlank() || second.beatGrid.camelotKey.isBlank() -> 12f
            camelotCompatible(first.beatGrid.camelotKey, second.beatGrid.camelotKey) -> 0f
            else -> 35f
        }
        val energyCost = abs(first.energy.coerceIn(0, 100) - second.energy.coerceIn(0, 100)).toFloat() * .35f
        return bpmCost + keyCost + energyCost
    }

    private fun tempoDistance(first: Float?, second: Float?): Float {
        if (first == null || second == null) return .45f
        val normalized = abs(first - second) / maxOf(first, second)
        return normalized.coerceIn(0f, 1f)
    }

    private fun camelot(value: String): Pair<Int, Char>? {
        val normalized = value.trim().uppercase()
        if (normalized.length !in 2..3) return null
        val number = normalized.dropLast(1).toIntOrNull()?.takeIf { it in 1..12 } ?: return null
        val letter = normalized.last().takeIf { it == 'A' || it == 'B' } ?: return null
        return number to letter
    }

    private fun nearestBeatAtOrAfter(positionMs: Long, beatMs: Long, downbeatMs: Long): Long {
        if (beatMs <= 0L) return positionMs.coerceAtLeast(0L)
        val relative = (positionMs - downbeatMs).coerceAtLeast(0L)
        return downbeatMs + ((relative + beatMs - 1L) / beatMs) * beatMs
    }
}
