package com.luc4n3x.levyra.domain

/** A saved, user-owned transition map for one local playlist. */
data class DerbisMixStudioPlan(
    val playlistId: String,
    val enabled: Boolean = false,
    val transitions: List<DerbisMixTransitionSetting> = emptyList(),
    val updatedAt: Long = 0L
) {
    fun settingFor(outgoing: Track, incoming: Track): DerbisMixTransitionSetting? =
        transitions.firstOrNull {
            it.outgoingEntryId == outgoing.derbisMixEntryId() &&
                it.incomingEntryId == incoming.derbisMixEntryId()
        }
}

/**
 * Values are deliberately limited to controls the current two-player audio pipeline can honour:
 * duration and handoff character. Beat data is kept separately until actual source analysis is
 * available, so a user never receives a false Beat Sync claim.
 */
data class DerbisMixTransitionSetting(
    val outgoingEntryId: String,
    val incomingEntryId: String,
    val preset: DerbisMixTransitionPreset = DerbisMixTransitionPreset.Auto,
    val durationMs: Long? = null,
    val beatSyncEnabled: Boolean = false
) {
    val key: String get() = "$outgoingEntryId\u0000$incomingEntryId"

    fun normalized(): DerbisMixTransitionSetting? {
        if (outgoingEntryId.isBlank() || incomingEntryId.isBlank()) return null
        return copy(durationMs = durationMs?.coerceIn(DerbisMixStudio.MIN_TRANSITION_MS, DerbisMixStudio.MAX_TRANSITION_MS))
    }
}

fun Track.derbisMixEntryId(): String = playlistEntryId.ifBlank { id }

fun DerbisMixStudioPlan.withAutoTransitions(tracks: List<Track>): DerbisMixStudioPlan = copy(
    transitions = tracks.zipWithNext().mapNotNull { (outgoing, incoming) ->
        DerbisMixTransitionSetting(
            outgoingEntryId = outgoing.derbisMixEntryId(),
            incomingEntryId = incoming.derbisMixEntryId()
        ).normalized()
    },
    updatedAt = System.currentTimeMillis()
)

fun DerbisMixStudioPlan.upsert(setting: DerbisMixTransitionSetting): DerbisMixStudioPlan {
    val normalized = setting.normalized() ?: return this
    return copy(
        transitions = transitions.filterNot { it.key == normalized.key } + normalized,
        updatedAt = System.currentTimeMillis()
    )
}
