package com.yasuda.derbis.feature.search

import com.yasuda.derbis.data.network.DerbisHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

internal data class LrcLibMatch(
    val title: String,
    val artist: String,
    val matchedLine: String
)

internal class LrcLibLyricsSearchClient(
    private val clientFactory: () -> OkHttpClient = { DerbisHttpClientFactory.externalIntegrations() }
) {
    private val client by lazy {
        clientFactory().newBuilder()
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    suspend fun search(query: String): List<LrcLibMatch> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "DERBIS/1.1 Android")
            .get()
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return@withContext emptyList()
            val array = JSONArray(body)
            val words = trimmed.lowercase().split(Regex("\\s+")).filter { it.length >= 2 }
            val out = ArrayList<LrcLibMatch>()
            for (i in 0 until array.length().coerceAtMost(16)) {
                val json = array.optJSONObject(i) ?: continue
                val trackName = json.optString("trackName").trim()
                val artistName = json.optString("artistName").trim()
                if (trackName.isBlank()) continue
                val plain = json.optString("plainLyrics").ifBlank { json.optString("syncedLyrics") }
                val lines = plain.lines().map { it.replace(Regex("\\[\\d+:\\d+(\\.\\d+)?\\]"), "").trim() }.filter { it.isNotBlank() }
                val matchingLine = lines.firstOrNull { line ->
                    val lower = line.lowercase()
                    words.any { lower.contains(it) }
                } ?: lines.firstOrNull().orEmpty()
                out.add(LrcLibMatch(trackName, artistName, matchingLine))
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }
}
