package com.luc4n3x.derbis.data

import android.content.Context
import com.luc4n3x.derbis.domain.DerbisMixStudioPlan
import com.luc4n3x.derbis.domain.DerbisMixTransitionPreset
import com.luc4n3x.derbis.domain.DerbisMixTransitionSetting
import com.luc4n3x.derbis.domain.Track
import com.luc4n3x.derbis.domain.derbisMixEntryId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/** Small independent store so Mix Studio does not require a playlist database migration. */
class DerbisMixStudioStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    init {
        // The removed two-song Library tool stored a transient plan under this reserved id.
        // Clear it so an old active queue can never revive that retired entry point.
        val activePlanId = preferences.getString(ACTIVE_KEY, null)
            ?.let { raw -> runCatching { JSONObject(raw).optString("playlistId") }.getOrNull() }
        preferences.edit()
            .remove(planKey(LEGACY_TWO_SONG_TOOL_PLAN_ID))
            .also { editor ->
                if (activePlanId == LEGACY_TWO_SONG_TOOL_PLAN_ID) editor.remove(ACTIVE_KEY)
            }
            .apply()
    }

    suspend fun load(playlistId: String): DerbisMixStudioPlan? = withContext(Dispatchers.IO) {
        if (playlistId.isBlank()) return@withContext null
        val raw = preferences.getString(planKey(playlistId), null) ?: return@withContext null
        runCatching { raw.toDerbisMixPlan(playlistId) }
            .onFailure { Timber.w(it, "Mix Studio plan read failed") }
            .getOrNull()
    }

    suspend fun save(plan: DerbisMixStudioPlan): Boolean = withContext(Dispatchers.IO) {
        if (plan.playlistId.isBlank()) return@withContext false
        runCatching {
            preferences.edit().putString(planKey(plan.playlistId), plan.toJson().toString()).commit()
        }.onFailure { Timber.w(it, "Mix Studio plan save failed") }.getOrDefault(false)
    }

    suspend fun delete(playlistId: String) = withContext(Dispatchers.IO) {
        if (playlistId.isNotBlank()) preferences.edit().remove(planKey(playlistId)).commit()
    }

    /** Arms a saved playlist plan for the exact queue started from that playlist. */
    fun activateForQueue(playlistId: String, tracks: List<Track>) {
        val signature = queueSignature(tracks)
        if (playlistId.isBlank() || signature.isBlank()) {
            clearActiveQueue()
            return
        }
        preferences.edit().putString(ACTIVE_KEY, JSONObject().apply {
            put("playlistId", playlistId)
            put("queue", signature)
        }.toString()).apply()
    }

    fun clearActiveQueue() {
        preferences.edit().remove(ACTIVE_KEY).apply()
    }

    /** Called by PlaybackService at handoff time. It cannot affect any other queue. */
    fun activeTransitionFor(
        queue: List<Track>,
        outgoing: Track,
        incoming: Track
    ): DerbisMixTransitionSetting? {
        val active = preferences.getString(ACTIVE_KEY, null)
            ?.let { raw -> runCatching { JSONObject(raw) }.getOrNull() }
            ?: return null
        val playlistId = active.optString("playlistId")
        if (playlistId.isBlank() || active.optString("queue") != queueSignature(queue)) return null
        val plan = preferences.getString(planKey(playlistId), null)
            ?.let { raw -> runCatching { raw.toDerbisMixPlan(playlistId) }.getOrNull() }
            ?: return null
        return plan.takeIf { it.enabled }?.settingFor(outgoing, incoming)
    }

    private fun queueSignature(tracks: List<Track>): String = tracks
        .joinToString(separator = "\u001f") { it.derbisMixEntryId() }
        .takeIf(String::isNotBlank)
        .orEmpty()

    private fun planKey(playlistId: String) = "$PLAN_PREFIX$playlistId"

    private fun DerbisMixStudioPlan.toJson(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("updatedAt", updatedAt)
        put("transitions", JSONArray().apply {
            transitions.mapNotNull(DerbisMixTransitionSetting::normalized).forEach { setting ->
                put(JSONObject().apply {
                    put("outgoing", setting.outgoingEntryId)
                    put("incoming", setting.incomingEntryId)
                    put("preset", setting.preset.name)
                    put("beatSyncEnabled", setting.beatSyncEnabled)
                    setting.durationMs?.let { put("durationMs", it) }
                    setting.outgoingStartMs?.let { put("outgoingStartMs", it) }
                    setting.incomingStartMs?.let { put("incomingStartMs", it) }
                })
            }
        })
    }

    private fun String.toDerbisMixPlan(playlistId: String): DerbisMixStudioPlan {
        val source = JSONObject(this)
        val transitions = source.optJSONArray("transitions")?.toTransitionSettings().orEmpty()
        return DerbisMixStudioPlan(
            playlistId = playlistId,
            enabled = source.optBoolean("enabled", false),
            transitions = transitions,
            updatedAt = source.optLong("updatedAt", 0L)
        )
    }

    private fun JSONArray.toTransitionSettings(): List<DerbisMixTransitionSetting> = buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val preset = item.optString("preset")
                .let { value -> DerbisMixTransitionPreset.entries.firstOrNull { it.name == value } }
                ?: DerbisMixTransitionPreset.Auto
            DerbisMixTransitionSetting(
                outgoingEntryId = item.optString("outgoing"),
                incomingEntryId = item.optString("incoming"),
                preset = preset,
                durationMs = item.takeIf { it.has("durationMs") }?.optLong("durationMs"),
                beatSyncEnabled = item.optBoolean("beatSyncEnabled", false),
                outgoingStartMs = item.takeIf { it.has("outgoingStartMs") }?.optLong("outgoingStartMs"),
                incomingStartMs = item.takeIf { it.has("incomingStartMs") }?.optLong("incomingStartMs")
            ).normalized()?.let(::add)
        }
    }

    private companion object {
        const val PREFERENCES = "derbis_mix_studio"
        const val PLAN_PREFIX = "plan."
        const val ACTIVE_KEY = "active_queue"
        const val LEGACY_TWO_SONG_TOOL_PLAN_ID = "derbis.mix.tool"
    }
}
