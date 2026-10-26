package com.luc4n3x.derbis.viewmodel

import androidx.compose.runtime.Immutable
import com.luc4n3x.derbis.domain.DerbisMixStudioPlan
import com.luc4n3x.derbis.domain.DerbisMixTransitionPreset
import com.luc4n3x.derbis.domain.DerbisMixTransitionSetting
import com.luc4n3x.derbis.domain.Playlist
import com.luc4n3x.derbis.domain.Track
import com.luc4n3x.derbis.domain.upsert
import com.luc4n3x.derbis.domain.withAutoTransitions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class DerbisMixStudioSession(
    val playlist: Playlist,
    val draft: DerbisMixStudioPlan,
    val baseline: DerbisMixStudioPlan,
    val saving: Boolean = false,
    val failed: Boolean = false
) {
    val dirty: Boolean get() = draft != baseline
    val transitionCount: Int get() = playlist.tracks.zipWithNext().count()
}

class DerbisMixStudioController(
    private val scope: CoroutineScope,
    private val savePlan: suspend (DerbisMixStudioPlan) -> Boolean
) {
    private val _session = MutableStateFlow<DerbisMixStudioSession?>(null)
    val session: StateFlow<DerbisMixStudioSession?> = _session.asStateFlow()

    fun open(playlist: Playlist, saved: DerbisMixStudioPlan?) {
        val base = (saved ?: DerbisMixStudioPlan(playlistId = playlist.id)).copy(playlistId = playlist.id)
        _session.value = DerbisMixStudioSession(playlist = playlist, draft = base, baseline = base)
    }

    fun close() {
        _session.value = null
    }

    fun setEnabled(enabled: Boolean) = edit { draft ->
        val updated = draft.copy(enabled = enabled, updatedAt = System.currentTimeMillis())
        if (enabled && updated.transitions.isEmpty()) updated.withAutoTransitions(session.value?.playlist?.tracks.orEmpty()) else updated
    }

    fun applyAutoMix() = edit { it.withAutoTransitions(session.value?.playlist?.tracks.orEmpty()) }

    fun setPreset(outgoing: Track, incoming: Track, preset: DerbisMixTransitionPreset) {
        edit { current ->
            current.upsert(
                DerbisMixTransitionSetting(
                    outgoingEntryId = outgoing.playlistEntryId.ifBlank { outgoing.id },
                    incomingEntryId = incoming.playlistEntryId.ifBlank { incoming.id },
                    preset = preset,
                    durationMs = current.settingFor(outgoing, incoming)?.durationMs,
                    beatSyncEnabled = current.settingFor(outgoing, incoming)?.beatSyncEnabled == true
                )
            )
        }
    }

    fun setBeatSync(outgoing: Track, incoming: Track, enabled: Boolean) {
        edit { current ->
            val existing = current.settingFor(outgoing, incoming)
                ?: DerbisMixTransitionSetting(
                    outgoingEntryId = outgoing.playlistEntryId.ifBlank { outgoing.id },
                    incomingEntryId = incoming.playlistEntryId.ifBlank { incoming.id }
                )
            current.upsert(existing.copy(beatSyncEnabled = enabled))
        }
    }

    fun adjustDuration(outgoing: Track, incoming: Track, deltaMs: Long) {
        edit { current ->
            val existing = current.settingFor(outgoing, incoming)
                ?: DerbisMixTransitionSetting(
                    outgoingEntryId = outgoing.playlistEntryId.ifBlank { outgoing.id },
                    incomingEntryId = incoming.playlistEntryId.ifBlank { incoming.id }
                )
            current.upsert(existing.copy(durationMs = ((existing.durationMs ?: 6_000L) + deltaMs).coerceIn(800L, 30_000L)))
        }
    }

    fun save() {
        val current = _session.value ?: return
        if (current.saving || !current.dirty) return
        _session.update { it?.copy(saving = true, failed = false) }
        scope.launch {
            val requested = _session.value?.draft ?: return@launch
            val saved = runCatching { savePlan(requested) }
                .onFailure { Timber.w(it, "Mix Studio plan save failed") }
                .getOrDefault(false)
            _session.update { latest ->
                if (latest == null || latest.draft != requested) latest
                else if (saved) latest.copy(baseline = requested, saving = false, failed = false)
                else latest.copy(saving = false, failed = true)
            }
        }
    }

    private fun edit(transform: (DerbisMixStudioPlan) -> DerbisMixStudioPlan) {
        _session.update { current ->
            current?.let { session ->
                if (session.saving) session else session.copy(draft = transform(session.draft), failed = false)
            }
        }
    }
}
