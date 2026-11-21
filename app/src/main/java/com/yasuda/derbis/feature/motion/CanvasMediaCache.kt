@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.yasuda.derbis.feature.motion

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.yasuda.derbis.data.network.DerbisHttpClientFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Dedicated disk + HTTP cache for Spotify Canvas `.mp4` and animated artwork streams so loops
 * never re-fetch over the network on 5–8s wraparound and upcoming queue tracks start on frame 1
 * without cold network latency.
 */
internal object CanvasMediaCache {
    private const val LOW_RAM_MAX_BYTES = 48L * 1024L * 1024L
    private const val DEFAULT_MAX_BYTES = 128L * 1024L * 1024L
    private const val MIN_MAX_BYTES = 24L * 1024L * 1024L
    private const val PREFETCH_MAX_BYTES = 6L * 1024L * 1024L
    private const val FRAGMENT_SIZE_BYTES = 2L * 1024L * 1024L
    private const val BUFFER_BYTES = 64 * 1024
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36"

    @Volatile
    private var cache: SimpleCache? = null

    @Volatile
    private var sharedDataSourceFactory: DataSource.Factory? = null

    private val prefetchLocks = ConcurrentHashMap<String, Mutex>()

    fun get(context: Context): SimpleCache {
        val appContext = context.applicationContext
        return cache ?: synchronized(this) {
            cache ?: create(appContext).also { cache = it }
        }
    }

    fun dataSourceFactory(context: Context): DataSource.Factory {
        val appContext = context.applicationContext
        return sharedDataSourceFactory ?: synchronized(this) {
            sharedDataSourceFactory ?: buildDataSourceFactory(appContext).also {
                sharedDataSourceFactory = it
            }
        }
    }

    suspend fun prefetch(context: Context, artwork: MotionArtwork) = withContext(Dispatchers.IO) {
        val url = artwork.url.trim()
        if (!url.startsWith("https://")) return@withContext
        // Progressive MP4 loops (Spotify Canvas) are 1–4 MB total; HLS master playlists are tiny
        // manifests whose segments depend on viewport selection, so only pre-warm progressive MP4s.
        val isProgressiveMp4 = artwork.mimeType.equals("video/mp4", ignoreCase = true) ||
            (url.contains(".mp4", ignoreCase = true) && !url.contains(".m3u8", ignoreCase = true))
        if (!isProgressiveMp4) return@withContext

        val appContext = context.applicationContext
        val lock = prefetchLocks.computeIfAbsent(url) { Mutex() }
        try {
            lock.withLock {
                val simpleCache = get(appContext)
                if (simpleCache.getCachedBytes(url, 0L, PREFETCH_MAX_BYTES) >= 512L * 1024L) {
                    return@withLock
                }
                val source = (dataSourceFactory(appContext) as CacheDataSource.Factory).createDataSource()
                val spec = DataSpec.Builder()
                    .setUri(Uri.parse(url))
                    .setKey(url)
                    .setPosition(0L)
                    .setLength(C.LENGTH_UNSET.toLong())
                    .build()
                var writer: CacheWriter? = null
                writer = CacheWriter(source, spec, ByteArray(BUFFER_BYTES)) { requestLength, bytesCached, _ ->
                    if (requestLength > PREFETCH_MAX_BYTES || bytesCached > PREFETCH_MAX_BYTES) {
                        writer?.cancel()
                    }
                }
                cacheUntilCancelled(writer)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Timber.d(error, "Canvas media prefetch skipped for %s", artwork.provider)
        } finally {
            prefetchLocks.remove(url, lock)
        }
    }

    private suspend fun cacheUntilCancelled(writer: CacheWriter) = coroutineScope {
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                writer.cancel()
            }
        }
        try {
            writer.cache()
        } finally {
            watcher.cancel()
        }
    }

    private fun buildDataSourceFactory(appContext: Context): CacheDataSource.Factory {
        val okHttp = DerbisHttpClientFactory.media(appContext).newBuilder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        val upstream = OkHttpDataSource.Factory(okHttp)
            .setUserAgent(USER_AGENT)
        val simpleCache = get(appContext)
        val sink = CacheDataSink.Factory()
            .setCache(simpleCache)
            .setFragmentSize(FRAGMENT_SIZE_BYTES)
        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstream)
            .setCacheWriteDataSinkFactory(sink)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun create(appContext: Context): SimpleCache {
        val directory = File(appContext.cacheDir, "derbis_canvas_cache")
        val maxBytes = maxCacheBytes(appContext, directory)
        return runCatching {
            SimpleCache(
                directory,
                LeastRecentlyUsedCacheEvictor(maxBytes),
                StandaloneDatabaseProvider(appContext)
            )
        }.getOrElse {
            directory.deleteRecursively()
            directory.mkdirs()
            SimpleCache(
                directory,
                LeastRecentlyUsedCacheEvictor(maxBytes),
                StandaloneDatabaseProvider(appContext)
            )
        }
    }

    private fun maxCacheBytes(context: Context, directory: File): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val base = if (activityManager.isLowRamDevice) LOW_RAM_MAX_BYTES else DEFAULT_MAX_BYTES
        val availableBudget = (directory.parentFile?.usableSpace ?: context.cacheDir.usableSpace) / 12L
        return availableBudget.coerceIn(MIN_MAX_BYTES, base)
    }
}
