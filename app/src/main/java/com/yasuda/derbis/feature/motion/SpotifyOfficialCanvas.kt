package com.yasuda.derbis.feature.motion

import com.yasuda.derbis.data.spotify.DerbisSpotifySession
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

internal data class SpotifyMatchedTrack(
    val spotifyId: String,
    val title: String,
    val artists: List<String>,
    val album: String,
    val durationMs: Long,
    val isrc: String,
    val year: String
)

/**
 * Resolves official Spotify Canvas video loops and track IDs using the listener's web-player
 * session ([SpotifyTokenProvider]).
 *
 * Queries three independent tiers so endpoint/hash changes never break Canvas playback:
 * 1. Spotify Web Player Pathfinder GraphQL (`api-partner.spotify.com/pathfinder/v1|v2/query`,
 *    `operationName = "canvas"`) using live hashes captured from `open.spotify.com` plus known
 *    persisted-query hashes.
 * 2. Regional `spclient` (`apresolve.spotify.com/?type=spclient`) `/canvaz-cache/v0/canvases`
 *    protobuf with `Bearer` + `Client-Token`.
 * 3. Public CanvasStore fallback (`regional.canvasstore.slnode.spicylyrics.org`) once the session
 *    token is verified active.
 */
internal object SpotifyOfficialCanvas {
    private const val APRESOLVE_URL = "https://apresolve.spotify.com/?type=spclient"
    private const val PATHFINDER_V1_URL = "https://api-partner.spotify.com/pathfinder/v1/query"
    private const val PATHFINDER_V2_URL = "https://api-partner.spotify.com/pathfinder/v2/query"
    private const val WEB_SEARCH_URL = "https://api.spotify.com/v1/search"
    private const val CANVAS_STORE_URL = "https://regional.canvasstore.slnode.spicylyrics.org/provider/canvaslist"

    private const val ANDROID_APP_UA = "Spotify/9.0.34.593 Android/35 (Pixel 9)"
    private const val IOS_APP_UA = "Spotify/9.0.34.593 iOS/18.4 (iPhone15,3)"
    private const val MAX_ATTEMPTS = 2
    private const val SPCLIENT_CACHE_TTL_MS = 30L * 60L * 1_000L

    private val DEFAULT_SPCLIENT_HOSTS = listOf(
        "gae2-spclient.spotify.com",
        "gew4-spclient.spotify.com",
        "guc3-spclient.spotify.com",
        "spclient.wg.spotify.com"
    )

    private val KNOWN_CANVAS_HASHES = listOf(
        "575138ab27cd5c1b3e54da54d0a7cc8d85485402de26340c2145f0f6bb5e7a9f",
        "1b1e1915481c99f4349af88268c6b49a2b601cf0db7b8749b5dd750880ab6fc5"
    )

    private val KNOWN_SEARCH_HASHES = listOf(
        "fcad5a3e0d5af727fb76966f06971c19cfa2275e6ff7671196753e008611873c",
        "75bbf6bfcfdf85fa8fc828417bf0c4b75d0679a1fb1ab02cc73d13b61817b60d"
    )

    private val CANVAS_MP4_REGEX = Regex(
        """https://[A-Za-z0-9.-]*(?:scdn\.co|spotifycdn\.com)/[^"'\s\x00-\x1F\\]+\.mp4(?:\?[^"'\s\x00-\x1F\\]*)?"""
    )
    private val TRACK_ID = Regex("^[A-Za-z0-9]{22}$")

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(14, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val urlCache = ConcurrentHashMap<String, String>()

    @Volatile private var cachedSpclientHosts: List<String> = emptyList()
    @Volatile private var spclientExpiresAtMs: Long = 0L

    sealed interface Result {
        data class Found(val url: String) : Result
        data object NoCanvas : Result
        data class Failed(val reason: String) : Result
    }

    private sealed interface TierOutcome {
        data class Found(val url: String) : TierOutcome
        data object Empty : TierOutcome
        data class Unauthorized(val code: Int) : TierOutcome
        data class TransientError(val detail: String) : TierOutcome
    }

    suspend fun canvasUrl(trackId: String, forceRefresh: Boolean = false): String? {
        val id = normalizeTrackId(trackId) ?: return null
        if (!forceRefresh) urlCache[id]?.let { return it }
        return when (val result = probe(id)) {
            is Result.Found -> {
                urlCache[id] = result.url
                result.url
            }
            else -> null
        }
    }

    suspend fun probe(trackId: String): Result {
        val id = normalizeTrackId(trackId)
            ?: return Result.Failed("That is not a Spotify track id.")
        urlCache[id]?.let { return Result.Found(it) }

        var lastReason = "Could not reach Spotify."
        repeat(MAX_ATTEMPTS) { attempt ->
            val creds = SpotifyTokenProvider.credentials(forceRefresh = attempt > 0)
                ?: return Result.Failed(
                    if (DerbisSpotifySession.hasSession()) {
                        if (SpotifyTokenProvider.lastHarvestSawExpiredCookie()) {
                            "Spotify session expired — sign in again to refresh Canvas."
                        } else {
                            "Spotify did not mint a token for that session. It has probably expired — sign in again."
                        }
                    } else {
                        "No Spotify session yet. Sign in or paste an sp_dc first."
                    }
                )

            val trackUri = "spotify:track:$id"

            // Tier 1: Spotify Web Player Pathfinder GraphQL `canvas` query.
            when (val pf = queryPathfinderCanvas(creds, trackUri)) {
                is TierOutcome.Found -> {
                    urlCache[id] = pf.url
                    return Result.Found(pf.url)
                }
                is TierOutcome.Unauthorized -> {
                    SpotifyTokenProvider.invalidate()
                    lastReason = "Spotify rejected the session (${pf.code})"
                    return@repeat
                }
                is TierOutcome.Empty,
                is TierOutcome.TransientError -> Unit
            }

            // Tier 2: Regional spclient `/canvaz-cache/v0/canvases` protobuf.
            when (val sp = querySpclientCanvaz(creds, trackUri)) {
                is TierOutcome.Found -> {
                    urlCache[id] = sp.url
                    return Result.Found(sp.url)
                }
                is TierOutcome.Unauthorized -> {
                    SpotifyTokenProvider.invalidate()
                    lastReason = "Spotify rejected the session (${sp.code})"
                    return@repeat
                }
                is TierOutcome.Empty -> {
                    // Token is valid; check public CanvasStore mirror in case regional edge lacked metadata.
                    queryCanvasStore(id)?.let { mirrorUrl ->
                        urlCache[id] = mirrorUrl
                        return Result.Found(mirrorUrl)
                    }
                    return Result.NoCanvas
                }
                is TierOutcome.TransientError -> {
                    lastReason = sp.detail
                    queryCanvasStore(id)?.let { mirrorUrl ->
                        urlCache[id] = mirrorUrl
                        return Result.Found(mirrorUrl)
                    }
                }
            }
        }
        return Result.Failed(lastReason)
    }

    /**
     * Searches Spotify's catalog for matching tracks using the active Web Player session so
     * non-Spotify (11-char YouTube) tracks resolve to exact 22-char Spotify track IDs.
     */
    suspend fun searchTracks(
        title: String,
        artist: String,
        isrc: String,
        limit: Int = 6
    ): List<SpotifyMatchedTrack> {
        if (!DerbisSpotifySession.hasSession()) return emptyList()
        val creds = SpotifyTokenProvider.credentials() ?: return emptyList()

        val queries = buildList {
            if (isrc.isNotBlank()) add("isrc:${isrc.trim()}")
            val combined = "${title.trim()} ${artist.trim()}".trim()
            if (combined.isNotBlank()) add(combined)
        }
        if (queries.isEmpty()) return emptyList()

        val results = LinkedHashMap<String, SpotifyMatchedTrack>()
        for (query in queries) {
            val webHits = searchViaWebApi(creds, query, limit)
            for (hit in webHits) {
                results.putIfAbsent(hit.spotifyId, hit)
            }
            if (results.isNotEmpty()) break

            val pathfinderHits = searchViaPathfinder(creds, query, limit)
            for (hit in pathfinderHits) {
                results.putIfAbsent(hit.spotifyId, hit)
            }
            if (results.isNotEmpty()) break
        }
        return results.values.toList()
    }

    fun invalidate() {
        urlCache.clear()
        SpotifyCanvasProvider.clearCaches()
    }

    private suspend fun queryPathfinderCanvas(
        creds: SpotifyTokenProvider.SessionCredentials,
        trackUri: String
    ): TierOutcome = withContext(Dispatchers.IO) {
        val hashes = buildList {
            SpotifyTokenProvider.pathfinderHash("canvas")?.let(::add)
            KNOWN_CANVAS_HASHES.forEach { if (it !in this) add(it) }
        }
        var sawUnauthorized: Int? = null
        var sawCleanNullCanvas = false

        for (hash in hashes) {
            val bodies = listOf(
                JSONObject()
                    .put("variables", JSONObject().put("trackUri", trackUri))
                    .put("operationName", "canvas")
                    .put(
                        "extensions",
                        JSONObject().put(
                            "persistedQuery",
                            JSONObject().put("version", 1).put("sha256Hash", hash)
                        )
                    )
                    .toString(),
                JSONObject()
                    .put("variables", JSONObject().put("uri", trackUri))
                    .put("operationName", "canvas")
                    .put(
                        "extensions",
                        JSONObject().put(
                            "persistedQuery",
                            JSONObject().put("version", 1).put("sha256Hash", hash)
                        )
                    )
                    .toString()
            )
            for (payload in bodies) {
                for (endpoint in listOf(PATHFINDER_V1_URL, PATHFINDER_V2_URL)) {
                    val outcome = try {
                        val request = Request.Builder()
                            .url(endpoint)
                            .post(payload.toByteArray(Charsets.UTF_8).toRequestBody("application/json".toMediaType()))
                            .header("Authorization", "Bearer ${creds.accessToken}")
                            .apply { creds.clientToken?.let { header("client-token", it) } }
                            .header("spotify-app-version", creds.clientVersion)
                            .header("app-platform", "WebPlayer")
                            .header("Origin", "https://open.spotify.com")
                            .header("Referer", "https://open.spotify.com/")
                            .header("Accept", "application/json")
                            .header("User-Agent", SpotifyTokenProvider.BROWSER_UA)
                            .build()
                        http.newCall(request).execute().use { response ->
                            val code = response.code
                            val raw = response.body?.string().orEmpty()
                            when {
                                code == 401 -> TierOutcome.Unauthorized(code)
                                !response.isSuccessful -> TierOutcome.TransientError("Pathfinder HTTP $code")
                                raw.isBlank() -> TierOutcome.Empty
                                else -> parsePathfinderCanvasResponse(raw)
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        Timber.d(error, "Pathfinder canvas query failed on %s", endpoint)
                        TierOutcome.TransientError(error.message ?: "Pathfinder failed")
                    }

                    when (outcome) {
                        is TierOutcome.Found -> return@withContext outcome
                        is TierOutcome.Unauthorized -> {
                            sawUnauthorized = outcome.code
                            return@withContext outcome
                        }
                        is TierOutcome.Empty -> {
                            sawCleanNullCanvas = true
                            break
                        }
                        is TierOutcome.TransientError -> Unit
                    }
                }
                if (sawCleanNullCanvas) break
            }
            if (sawCleanNullCanvas) break
        }
        sawUnauthorized?.let { return@withContext TierOutcome.Unauthorized(it) }
        if (sawCleanNullCanvas) TierOutcome.Empty else TierOutcome.TransientError("Pathfinder unavailable")
    }

    private fun parsePathfinderCanvasResponse(raw: String): TierOutcome {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return TierOutcome.TransientError("Malformed JSON")
        val errors = root.optJSONArray("errors")
        if (errors != null && errors.length() > 0) {
            val firstMsg = errors.optJSONObject(0)?.optString("message").orEmpty()
            return TierOutcome.TransientError(firstMsg.ifBlank { "Pathfinder GraphQL error" })
        }
        val data = root.optJSONObject("data") ?: return TierOutcome.TransientError("Missing data")

        val directUrl = data.optJSONObject("trackUnion")
            ?.optJSONObject("canvas")
            ?.let { canvasObj ->
                canvasObj.optString("url").trim().ifBlank { canvasObj.optString("canvasUrl").trim() }
            }
            ?.takeIf { it.startsWith("https://") }
            ?: data.optJSONObject("canvas")
                ?.optJSONArray("canvasesList")
                ?.optJSONObject(0)
                ?.let { item ->
                    item.optString("canvasUrl").trim().ifBlank { item.optString("url").trim() }
                }
                ?.takeIf { it.startsWith("https://") }
            ?: CANVAS_MP4_REGEX.find(raw.replace("\\/", "/"))?.value

        return if (directUrl != null) TierOutcome.Found(directUrl) else TierOutcome.Empty
    }

    private suspend fun querySpclientCanvaz(
        creds: SpotifyTokenProvider.SessionCredentials,
        trackUri: String
    ): TierOutcome = withContext(Dispatchers.IO) {
        val hosts = resolveSpclientHosts()
        val bodyBytes = encodeRequest(trackUri)
        val userAgents = listOf(
            ANDROID_APP_UA to false,
            SpotifyTokenProvider.BROWSER_UA to true,
            IOS_APP_UA to false
        )

        var sawEmptySuccess = false
        var lastStatus = 0

        for (host in hosts.take(3)) {
            val url = "https://$host/canvaz-cache/v0/canvases"
            for ((ua, webHeaders) in userAgents) {
                val result = try {
                    val request = Request.Builder()
                        .url(url)
                        .post(bodyBytes.toRequestBody("application/protobuf".toMediaType()))
                        .header("Authorization", "Bearer ${creds.accessToken}")
                        .apply {
                            creds.clientToken?.let { header("client-token", it) }
                            if (webHeaders) {
                                header("app-platform", "WebPlayer")
                                header("spotify-app-version", creds.clientVersion)
                                header("Origin", "https://open.spotify.com")
                                header("Referer", "https://open.spotify.com/")
                            }
                        }
                        .header("Accept", "application/protobuf")
                        .header("Accept-Language", "en")
                        .header("User-Agent", ua)
                        .build()
                    http.newCall(request).execute().use { response ->
                        response.code to if (response.isSuccessful) response.body?.bytes() else null
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    Timber.d(error, "spclient canvaz-cache failed on %s", host)
                    null
                } ?: continue

                val (code, bytes) = result
                lastStatus = code
                when {
                    code == 401 -> return@withContext TierOutcome.Unauthorized(code)
                    code in 200..299 -> {
                        if (bytes != null && bytes.isNotEmpty()) {
                            decodeResponse(bytes, trackUri)?.let { foundUrl ->
                                return@withContext TierOutcome.Found(foundUrl)
                            }
                        }
                        sawEmptySuccess = true
                    }
                    code == 204 || code == 404 -> {
                        sawEmptySuccess = true
                    }
                }
            }
            if (sawEmptySuccess) break
        }

        when {
            sawEmptySuccess -> TierOutcome.Empty
            lastStatus == 403 -> TierOutcome.Unauthorized(403)
            lastStatus != 0 -> TierOutcome.TransientError("Spotify answered $lastStatus")
            else -> TierOutcome.TransientError("Could not reach Spotify.")
        }
    }

    private suspend fun resolveSpclientHosts(): List<String> {
        val now = System.currentTimeMillis()
        val current = cachedSpclientHosts
        if (current.isNotEmpty() && now < spclientExpiresAtMs) return current

        val resolved = runCatching {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(APRESOLVE_URL)
                    .header("Accept", "application/json")
                    .header("User-Agent", SpotifyTokenProvider.BROWSER_UA)
                    .build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use emptyList()
                    val json = JSONObject(response.body?.string().orEmpty())
                    val arr = json.optJSONArray("spclient") ?: return@use emptyList()
                    buildList {
                        for (i in 0 until arr.length()) {
                            val host = arr.optString(i).substringBefore(':').trim()
                            if (host.endsWith(".spotify.com")) add(host)
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())

        val combined = (resolved + DEFAULT_SPCLIENT_HOSTS).distinct()
        cachedSpclientHosts = combined
        spclientExpiresAtMs = now + SPCLIENT_CACHE_TTL_MS
        return combined
    }

    private suspend fun queryCanvasStore(spotifyId: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$CANVAS_STORE_URL/$spotifyId")
                .header("Accept", "application/json")
                .header("User-Agent", "YasudaMusic/1.0 (Android; SpotifyCanvasProvider)")
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@use null
                val url = JSONObject(body)
                    .optJSONObject("data")
                    ?.optJSONObject(spotifyId)
                    ?.optString("url")
                    ?.trim()
                url?.takeIf { it.startsWith("https://") }
            }
        }.getOrNull()
    }

    private suspend fun searchViaWebApi(
        creds: SpotifyTokenProvider.SessionCredentials,
        query: String,
        limit: Int
    ): List<SpotifyMatchedTrack> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
            val url = "$WEB_SEARCH_URL?type=track&limit=${limit.coerceIn(1, 10)}&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${creds.accessToken}")
                .apply { creds.clientToken?.let { header("client-token", it) } }
                .header("app-platform", "WebPlayer")
                .header("spotify-app-version", creds.clientVersion)
                .header("Origin", "https://open.spotify.com")
                .header("Referer", "https://open.spotify.com/")
                .header("Accept", "application/json")
                .header("User-Agent", SpotifyTokenProvider.BROWSER_UA)
                .build()
            http.newCall(request).execute().use { response ->
                if (response.code == 401) {
                    SpotifyTokenProvider.invalidate()
                    return@use emptyList()
                }
                if (!response.isSuccessful) return@use emptyList()
                val items = JSONObject(response.body?.string().orEmpty())
                    .optJSONObject("tracks")
                    ?.optJSONArray("items")
                    ?: return@use emptyList()
                parseWebApiTrackItems(items)
            }
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Timber.d(error, "Spotify Web API search failed for %s", query)
        }.getOrDefault(emptyList())
    }

    private fun parseWebApiTrackItems(items: JSONArray): List<SpotifyMatchedTrack> = buildList {
        for (i in 0 until items.length()) {
            val obj = items.optJSONObject(i) ?: continue
            val id = normalizeTrackId(obj.optString("id")) ?: continue
            val title = obj.optString("name").trim()
            if (title.isBlank()) continue
            val artistsArr = obj.optJSONArray("artists")
            val artists = buildList {
                if (artistsArr != null) {
                    for (j in 0 until artistsArr.length()) {
                        artistsArr.optJSONObject(j)?.optString("name")?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?.let(::add)
                    }
                }
            }
            val albumObj = obj.optJSONObject("album")
            val albumName = albumObj?.optString("name")?.trim().orEmpty()
            val releaseYear = albumObj?.optString("release_date")?.trim()?.take(4)
                ?.takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty()
            val durationMs = obj.optLong("duration_ms", 0L)
            val isrc = obj.optJSONObject("external_ids")?.optString("isrc")?.trim().orEmpty()
            add(
                SpotifyMatchedTrack(
                    spotifyId = id,
                    title = title,
                    artists = artists,
                    album = albumName,
                    durationMs = durationMs,
                    isrc = isrc,
                    year = releaseYear
                )
            )
        }
    }

    private suspend fun searchViaPathfinder(
        creds: SpotifyTokenProvider.SessionCredentials,
        query: String,
        limit: Int
    ): List<SpotifyMatchedTrack> = withContext(Dispatchers.IO) {
        val hashes = buildList {
            SpotifyTokenProvider.pathfinderHash("searchDesktop")?.let(::add)
            KNOWN_SEARCH_HASHES.forEach { if (it !in this) add(it) }
        }
        for (hash in hashes) {
            val payload = JSONObject()
                .put(
                    "variables",
                    JSONObject()
                        .put("searchTerm", query)
                        .put("offset", 0)
                        .put("limit", limit.coerceIn(1, 10))
                        .put("numberOfTopResults", 3)
                        .put("includeAudiobooks", false)
                        .put("includeArtistHasConcertsField", false)
                        .put("includePreReleases", false)
                )
                .put("operationName", "searchDesktop")
                .put(
                    "extensions",
                    JSONObject().put(
                        "persistedQuery",
                        JSONObject().put("version", 1).put("sha256Hash", hash)
                    )
                )
                .toString()

            for (endpoint in listOf(PATHFINDER_V2_URL, PATHFINDER_V1_URL)) {
                val parsed = runCatching {
                    val request = Request.Builder()
                        .url(endpoint)
                        .post(payload.toByteArray(Charsets.UTF_8).toRequestBody("application/json".toMediaType()))
                        .header("Authorization", "Bearer ${creds.accessToken}")
                        .apply { creds.clientToken?.let { header("client-token", it) } }
                        .header("app-platform", "WebPlayer")
                        .header("spotify-app-version", creds.clientVersion)
                        .header("Origin", "https://open.spotify.com")
                        .header("Referer", "https://open.spotify.com/")
                        .header("Accept", "application/json")
                        .header("User-Agent", SpotifyTokenProvider.BROWSER_UA)
                        .build()
                    http.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use emptyList()
                        parsePathfinderSearchTracks(response.body?.string().orEmpty())
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                }.getOrDefault(emptyList())

                if (parsed.isNotEmpty()) return@withContext parsed
            }
        }
        emptyList()
    }

    private fun parsePathfinderSearchTracks(raw: String): List<SpotifyMatchedTrack> {
        if (raw.isBlank()) return emptyList()
        val items = runCatching {
            JSONObject(raw)
                .optJSONObject("data")
                ?.optJSONObject("searchV2")
                ?.optJSONObject("tracksV2")
                ?.optJSONArray("items")
        }.getOrNull() ?: return emptyList()

        return buildList {
            for (i in 0 until items.length()) {
                val data = items.optJSONObject(i)
                    ?.optJSONObject("item")
                    ?.optJSONObject("data")
                    ?: continue
                val rawId = data.optString("id").ifBlank { data.optString("uri") }
                val id = normalizeTrackId(rawId) ?: continue
                val title = data.optString("name").trim()
                if (title.isBlank()) continue
                val artistItems = data.optJSONObject("artists")?.optJSONArray("items")
                val artists = buildList {
                    if (artistItems != null) {
                        for (j in 0 until artistItems.length()) {
                            val name = artistItems.optJSONObject(j)
                                ?.optJSONObject("profile")
                                ?.optString("name")
                                ?.trim()
                                .orEmpty()
                            if (name.isNotBlank()) add(name)
                        }
                    }
                }
                val album = data.optJSONObject("albumOfTrack")?.optString("name")?.trim().orEmpty()
                val durationMs = data.optJSONObject("duration")?.optLong("totalMilliseconds", 0L) ?: 0L
                add(
                    SpotifyMatchedTrack(
                        spotifyId = id,
                        title = title,
                        artists = artists,
                        album = album,
                        durationMs = durationMs,
                        isrc = "",
                        year = ""
                    )
                )
            }
        }
    }

    internal fun normalizeTrackId(raw: String): String? {
        val cleaned = raw.trim()
            .removePrefix("spotify:track:")
            .removePrefix("spotify:")
            .substringAfterLast("/track/", missingDelimiterValue = raw.trim().removePrefix("spotify:track:").removePrefix("spotify:"))
            .substringBefore('?')
            .trim()
        return cleaned.takeIf { TRACK_ID.matches(it) }
    }

    /** `CanvasRequest { repeated Track tracks = 1; }`, `Track { string track_uri = 1; }` */
    private fun encodeRequest(trackUri: String): ByteArray {
        val track = ByteArrayOutputStream().also { writeString(it, 1, trackUri) }
        val request = ByteArrayOutputStream().also { writeBytes(it, 1, track.toByteArray()) }
        return request.toByteArray()
    }

    private fun decodeResponse(bytes: ByteArray, wantedUri: String): String? {
        val entries = mutableListOf<Pair<String, String?>>()
        var index = 0
        while (index < bytes.size) {
            val header = readVarint(bytes, index) ?: break
            index = header.second
            val field = (header.first ushr 3).toInt()
            val wire = (header.first and 7L).toInt()
            if (field == 1 && wire == 2) {
                val length = readVarint(bytes, index) ?: break
                val start = length.second
                val end = start + length.first.toInt()
                if (end > bytes.size) break
                decodeCanvas(bytes.copyOfRange(start, end))?.let(entries::add)
                index = end
            } else {
                index = skipField(bytes, index, wire) ?: break
            }
        }
        entries.firstOrNull { it.second == wantedUri }?.first
            ?.takeIf { it.startsWith("https://") }
            ?.let { return it }
        entries.firstOrNull()?.first
            ?.takeIf { it.startsWith("https://") }
            ?.let { return it }
        return CANVAS_MP4_REGEX.find(String(bytes, Charsets.ISO_8859_1))?.value
    }

    private fun decodeCanvas(bytes: ByteArray): Pair<String, String?>? {
        var url: String? = null
        var trackUri: String? = null
        var index = 0
        while (index < bytes.size) {
            val header = readVarint(bytes, index) ?: break
            index = header.second
            val field = (header.first ushr 3).toInt()
            val wire = (header.first and 7L).toInt()
            if (wire == 2 && (field == 1 || field == 2 || field == 5)) {
                val length = readVarint(bytes, index) ?: break
                val start = length.second
                val end = start + length.first.toInt()
                if (end > bytes.size) break
                val text = String(bytes, start, end - start, Charsets.UTF_8)
                when (field) {
                    2 -> url = text
                    5 -> trackUri = text
                }
                index = end
            } else {
                index = skipField(bytes, index, wire) ?: break
            }
        }
        return url?.let { it to trackUri }
    }

    private fun readVarint(bytes: ByteArray, start: Int): Pair<Long, Int>? {
        var value = 0L
        var shift = 0
        var index = start
        while (index < bytes.size && shift <= 63) {
            val byte = bytes[index].toInt() and 0xFF
            value = value or ((byte and 0x7F).toLong() shl shift)
            index++
            if ((byte and 0x80) == 0) return value to index
            shift += 7
        }
        return null
    }

    private fun skipField(bytes: ByteArray, index: Int, wire: Int): Int? = when (wire) {
        0 -> readVarint(bytes, index)?.second
        1 -> (index + 8).takeIf { it <= bytes.size }
        2 -> readVarint(bytes, index)?.let { (len, start) ->
            (start + len.toInt()).takeIf { it <= bytes.size }
        }
        5 -> (index + 4).takeIf { it <= bytes.size }
        else -> null
    }

    private fun writeVarint(out: ByteArrayOutputStream, raw: Long) {
        var value = raw
        while ((value and -0x80L) != 0L) {
            out.write(((value.toInt() and 0x7F) or 0x80))
            value = value ushr 7
        }
        out.write(value.toInt() and 0x7F)
    }

    private fun writeString(out: ByteArrayOutputStream, field: Int, value: String) =
        writeBytes(out, field, value.toByteArray(Charsets.UTF_8))

    private fun writeBytes(out: ByteArrayOutputStream, field: Int, value: ByteArray) {
        writeVarint(out, ((field shl 3) or 2).toLong())
        writeVarint(out, value.size.toLong())
        out.write(value)
    }
}
