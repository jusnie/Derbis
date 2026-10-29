package com.yasuda.derbis.data

import android.content.Context
import com.yasuda.derbis.domain.DerbisBeatGrid
import java.security.MessageDigest
import org.json.JSONObject
import timber.log.Timber

/** Persistent, device-local analysis cache. It stores no audio and never uploads a listening sample. */
class DerbisBeatGridStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun gridFor(trackId: String): DerbisBeatGrid? {
        if (trackId.isBlank()) return null
        val raw = preferences.getString(key(trackId), null) ?: return null
        return runCatching {
            val value = JSONObject(raw)
            DerbisBeatGrid(
                bpm = value.optDouble("bpm", Double.NaN).toFloat().takeIf { it.isFinite() },
                downbeatMs = value.optLong("downbeatMs", -1L).takeIf { it >= 0L },
                confidence = value.optDouble("confidence", 0.0).toFloat().coerceIn(0f, 1f),
                camelotKey = value.optString("camelotKey").take(3)
            )
        }.onFailure { Timber.w(it, "Beat grid cache read failed") }.getOrNull()
    }

    fun save(trackId: String, grid: DerbisBeatGrid) {
        if (trackId.isBlank() || !grid.isUsable) return
        runCatching {
            preferences.edit().putString(key(trackId), JSONObject().apply {
                put("bpm", grid.validBpm)
                put("downbeatMs", grid.downbeatMs ?: 0L)
                put("confidence", grid.confidence)
                put("camelotKey", grid.camelotKey.take(3))
            }.toString()).apply()
        }.onFailure { Timber.w(it, "Beat grid cache save failed") }
    }

    private fun key(trackId: String): String = "grid.${trackId.sha256()}"

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val PREFERENCES = "derbis_beat_grids"
    }
}
