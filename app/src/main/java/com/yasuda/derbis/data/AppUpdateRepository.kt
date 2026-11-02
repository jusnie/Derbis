package com.yasuda.derbis.data

import android.content.Context
import android.os.Build
import com.yasuda.derbis.BuildConfig
import com.yasuda.derbis.data.network.DerbisHttpClientFactory
import com.yasuda.derbis.domain.AppUpdateInfo
import com.yasuda.derbis.nexus.update.DerbisUpdateArtifact
import com.yasuda.derbis.nexus.update.DerbisUpdateSelector
import com.yasuda.derbis.update.AppUpdateContract
import com.yasuda.derbis.update.DerbisUpdateTrack
import com.yasuda.derbis.update.UpdateRelease
import com.yasuda.derbis.update.UpdateReleaseAsset
import com.yasuda.derbis.update.buildIdOf
import com.yasuda.derbis.update.isNewerThan
import com.yasuda.derbis.update.isPublicUpdateAddress
import com.yasuda.derbis.update.resolveInstalledTrack
import com.yasuda.derbis.update.selectUpdateRelease
import com.yasuda.derbis.update.updateTrackOf
import com.yasuda.derbis.update.versionOf
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.ResponseBody
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

internal data class InstallableAppUpdate(
    val info: AppUpdateInfo,
    val assetDownloadUrl: String,
    val assetSizeBytes: Long
)

/**
 * Reads published Derbis releases and answers one question: which release should this installed
 * build be comparing itself against?
 *
 * The answer depends on the installed build's own track (see [DerbisUpdateTrack]): a stable
 * install only ever sees stable releases, a test install prefers test builds and falls back to
 * stable. The parsing and ordering rules live in `update/UpdateDetection.kt` and are unit-tested
 * there; this class only fetches, maps JSON onto them, and picks the file to download.
 */
class AppUpdateRepository(
    context: Context,
    private val track: DerbisUpdateTrack = installedUpdateTrack()
) {
    private val baseClient = DerbisHttpClientFactory.general(context.applicationContext)
    private val client = baseClient.newBuilder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                if (!hostname.equals(UPDATE_METADATA_HOST, ignoreCase = true)) {
                    throw UnknownHostException("Update metadata host not allowed")
                }
                val addresses = baseClient.dns.lookup(hostname)
                if (addresses.isEmpty() || addresses.any { !isPublicUpdateAddress(it) }) {
                    throw UnknownHostException("Update metadata destination is not public")
                }
                return addresses
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .connectTimeout(UPDATE_METADATA_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(UPDATE_METADATA_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(UPDATE_METADATA_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(UPDATE_METADATA_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun latest(): AppUpdateInfo = withContext(Dispatchers.IO) {
        describe(newestRelease()).info
    }

    internal suspend fun latestInstallable(): InstallableAppUpdate = withContext(Dispatchers.IO) {
        val described = describe(newestRelease())
        if (!described.info.isNewer) throw IllegalStateException("No newer Derbis release is available")
        if (described.assetDownloadUrl.isBlank()) {
            throw IllegalStateException("The latest Derbis release has no installable APK")
        }
        described
    }

    internal fun validateLatestReleaseMetadata(): Boolean {
        val releases = fetchReleases()
        return releases.any { it.tag.isNotBlank() || it.title.isNotBlank() }
    }

    private fun newestRelease(): UpdateRelease {
        val releases = fetchReleases()
        val effectiveTrack = resolveInstalledTrack(track, releases, BuildConfig.VERSION_NAME)
        return selectUpdateRelease(releases, effectiveTrack)
            ?: throw IllegalStateException("Nessuna release Android pubblicata per DERBIS")
    }

    private fun fetchReleases(): List<UpdateRelease> {
        val endpoint = validateDerbisReleaseListUrl(BuildConfig.UPDATE_RELEASES_URL)
            ?: throw IOException("Invalid update metadata endpoint")
        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DERBIS/${BuildConfig.VERSION_NAME}")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isRedirect) throw IOException("Update metadata redirect rejected")
            if (!response.isSuccessful) {
                val message = when (response.code) {
                    404 -> "Nessuna release pubblicata per DERBIS"
                    403 -> "Controllo aggiornamenti temporaneamente limitato"
                    else -> "Controllo aggiornamenti non riuscito (${response.code})"
                }
                throw IllegalStateException(message)
            }
            if (!updateMetadataContentTypeAccepted(response.header("Content-Type"))) {
                throw IOException("Unexpected update metadata content type")
            }
            val array = JSONArray(readUpdateMetadataBody(response.body))
            val releases = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    parseRelease(item)?.let(::add)
                }
            }
            if (releases.isEmpty()) {
                throw IllegalStateException("Nessuna release Android pubblicata per DERBIS")
            }
            return releases
        }
    }

    /**
     * Maps one GitHub release onto the detector's model, or null when it is not a Derbis Android
     * release at all (drafts, the Windows tags, anything without an APK).
     */
    private fun parseRelease(item: JSONObject): UpdateRelease? {
        if (item.optBoolean("draft")) return null
        val tag = item.optString("tag_name").trim()
        if (tag.isBlank()) return null
        val prerelease = item.optBoolean("prerelease")
        val releaseTrack = updateTrackOf(tag, prerelease) ?: return null
        val version = versionOf(tag) ?: return null
        return UpdateRelease(
            tag = tag,
            versionName = version,
            track = releaseTrack,
            buildId = buildIdOf(tag),
            title = item.optString("name").ifBlank { "DERBIS $version" },
            notes = item.optString("body").trim(),
            htmlUrl = item.optString("html_url"),
            publishedAtMs = parseUpdatePublishedAt(item.optString("published_at")),
            assets = releaseAssets(item)
        )
    }

    private fun describe(release: UpdateRelease): InstallableAppUpdate {
        val selected = DerbisUpdateSelector.selectArtifact(
            artifacts = release.assets.map { asset ->
                DerbisUpdateArtifact(
                    name = asset.name,
                    downloadUrl = asset.downloadUrl,
                    sizeBytes = asset.sizeBytes,
                    contentType = asset.contentType,
                    abi = asset.abi
                )
            },
            sdk = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.orEmpty().toList()
        )
        val assetDownloadUrl = selected?.downloadUrl.orEmpty()
        val directApk = selected?.isApk == true && assetDownloadUrl.isNotBlank()
        // The banner routes through the in-app installer only when this build is allowed to
        // fetch the file itself; otherwise it opens the release page, which still works.
        val downloadUrl = if (directApk && BuildConfig.UPSTREAM_UPDATES_ENABLED) {
            AppUpdateContract.INSTALL_URI
        } else {
            assetDownloadUrl.ifBlank { release.htmlUrl }
        }
        val info = AppUpdateInfo(
            currentVersionName = BuildConfig.VERSION_NAME,
            latestVersionName = release.versionName,
            latestTag = release.tag,
            releaseTitle = release.title,
            releaseNotes = release.notes,
            publishedAtEpochMs = release.publishedAtMs,
            downloadUrl = downloadUrl,
            releaseUrl = release.htmlUrl.ifBlank { assetDownloadUrl },
            assetName = selected?.name.orEmpty(),
            directApk = directApk,
            isNewer = isNewerThan(release, BuildConfig.VERSION_NAME),
            assetSizeBytes = selected?.sizeBytes?.coerceAtLeast(0L) ?: 0L,
            track = release.track.key,
            buildId = release.buildId
        )
        return InstallableAppUpdate(
            info = info,
            assetDownloadUrl = if (directApk) assetDownloadUrl else "",
            assetSizeBytes = selected?.sizeBytes?.coerceAtLeast(0L) ?: 0L
        )
    }

    private fun releaseAssets(root: JSONObject): List<UpdateReleaseAsset> {
        val assets = root.optJSONArray("assets") ?: return emptyList()
        return buildList {
            for (index in 0 until assets.length()) {
                val item = assets.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                val url = item.optString("browser_download_url").trim()
                if (url.isBlank()) continue
                add(
                    UpdateReleaseAsset(
                        name = name,
                        downloadUrl = url,
                        contentType = item.optString("content_type").trim(),
                        sizeBytes = item.optLong("size", 0L).coerceAtLeast(0L),
                        abi = DerbisUpdateSelector.inferAbi(name)
                    )
                )
            }
        }
    }
}

/** The release line this installed build belongs to. */
internal fun installedUpdateTrack(): DerbisUpdateTrack =
    DerbisUpdateTrack.fromKey(BuildConfig.UPDATE_TRACK)

internal const val MAX_UPDATE_METADATA_BYTES = 1_048_576L
internal const val UPDATE_METADATA_CONNECT_TIMEOUT_SECONDS = 5L
internal const val UPDATE_METADATA_READ_TIMEOUT_SECONDS = 10L
internal const val UPDATE_METADATA_CALL_TIMEOUT_SECONDS = 12L
private const val UPDATE_METADATA_HOST = "api.github.com"
private const val UPDATE_RELEASES_PATH = "/repos/jusnie/Derbis/releases"
private const val UPDATE_RELEASES_QUERY = "per_page=30"

internal fun validateDerbisReleaseListUrl(value: String): HttpUrl? {
    val url = value.trim().toHttpUrlOrNull() ?: return null
    if (!url.isHttps || url.port != 443) return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null) return null
    if (!url.host.equals(UPDATE_METADATA_HOST, ignoreCase = true)) return null
    if (url.encodedPath != UPDATE_RELEASES_PATH || url.query != UPDATE_RELEASES_QUERY) return null
    return url
}

internal fun updateMetadataContentTypeAccepted(value: String?): Boolean {
    val mime = value?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return mime == "application/json" || mime == "application/vnd.github+json"
}

internal fun readUpdateMetadataBody(body: ResponseBody): String {
    val declaredLength = body.contentLength()
    if (declaredLength > MAX_UPDATE_METADATA_BYTES) throw IOException("Update metadata is too large")
    val source = body.source()
    source.request(MAX_UPDATE_METADATA_BYTES + 1L)
    if (source.buffer.size > MAX_UPDATE_METADATA_BYTES) throw IOException("Update metadata exceeded size limit")
    return source.buffer.readUtf8()
}

internal fun parseUpdatePublishedAt(raw: String): Long {
    val value = raw.trim()
    if (value.isBlank()) return 0L
    return runCatching { Instant.parse(value).toEpochMilli() }
        .getOrDefault(0L)
        .coerceAtLeast(0L)
}
