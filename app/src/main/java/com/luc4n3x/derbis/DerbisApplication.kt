package com.luc4n3x.derbis

import android.app.Application
import android.content.ComponentCallbacks2
import com.luc4n3x.derbis.data.AutomaticBackupScheduler
import com.luc4n3x.derbis.data.FollowedArtistsStore
import com.luc4n3x.derbis.data.DerbisArtworkCache
import com.luc4n3x.derbis.data.DerbisArtworkStartupMetrics
import com.luc4n3x.derbis.data.DerbisPreferences
import com.luc4n3x.derbis.data.NewPipeRuntime
import com.luc4n3x.derbis.data.PlaybackResolver
import com.luc4n3x.derbis.data.spotify.SpotifyYouTubeMatchCache
import com.luc4n3x.derbis.data.ReleaseRadarWorker
import com.luc4n3x.derbis.data.SmartOfflineScheduler
import com.luc4n3x.derbis.data.YoutubeLocalDecoder
import com.luc4n3x.derbis.data.preloadDerbisPreferences
import com.luc4n3x.derbis.data.network.DerbisNetworkController
import com.luc4n3x.derbis.feature.cast.CastRuntimeInitializer
import com.luc4n3x.derbis.player.PlaybackNetworkStack
import com.luc4n3x.derbis.player.waveseek.WaveSeekRuntime
import com.luc4n3x.derbis.runtime.RuntimeHooks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class DerbisApplication : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        CastRuntimeInitializer.initialize(this)
        RuntimeHooks.start(this)
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        preloadDerbisPreferences(this)
        DerbisArtworkStartupMetrics.beginSession()
        runCatching { DerbisNetworkController.applyStoredConfiguration(this) }
            .onFailure { Timber.w(it, "Network configuration bootstrap failed") }
        DerbisArtworkCache.configure(this)
        startupScope.launch(Dispatchers.IO) {
            SpotifyYouTubeMatchCache.init(this@DerbisApplication)
        }
        YoutubeLocalDecoder.install(this)
        PlaybackNetworkStack.initialize(this)
        WaveSeekRuntime.start(this)
        runCatching { NewPipeRuntime.ensure(this) }
            .onFailure { Timber.w(it, "Extractor initialization failed") }
        warmPlaybackPipeline()
        startupScope.launch {
            delay(1800L)
            try {
                val releaseNotificationsEnabled = DerbisPreferences(this@DerbisApplication)
                    .interfaceSettings()
                    .releaseNotificationsEnabled
                if (!releaseNotificationsEnabled) {
                    ReleaseRadarWorker.cancel(this@DerbisApplication)
                } else {
                    when (val followedArtists = FollowedArtistsStore(this@DerbisApplication).loadOrNull()) {
                        null -> Timber.w("Release radar state unavailable; preserving existing schedule")
                        emptyList<com.luc4n3x.derbis.domain.FollowedArtist>() ->
                            ReleaseRadarWorker.cancel(this@DerbisApplication)
                        else -> ReleaseRadarWorker.schedule(this@DerbisApplication)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Release radar scheduling failed")
            }
            runCatching {
                AutomaticBackupScheduler.schedule(
                    this@DerbisApplication,
                    DerbisPreferences(this@DerbisApplication).backupSettings()
                )
            }.onFailure { Timber.w(it, "Automatic backup scheduling failed") }
            runCatching {
                SmartOfflineScheduler.schedule(
                    this@DerbisApplication,
                    DerbisPreferences(this@DerbisApplication).smartOfflineSettings()
                )
            }.onFailure { Timber.w(it, "Smart Offline scheduling failed") }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (shouldReleaseMemory(level)) YoutubeLocalDecoder.trimMemory()
    }

    private fun shouldReleaseMemory(level: Int): Boolean =
        level == RUNNING_LOW_LEVEL ||
            level == RUNNING_CRITICAL_LEVEL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND

    private companion object {
        const val RUNNING_LOW_LEVEL = 10
        const val RUNNING_CRITICAL_LEVEL = 15
    }

    private fun warmPlaybackPipeline() {
        startupScope.launch {
            runCatching { PlaybackResolver.getInstance(this@DerbisApplication).warmNetwork() }
                .onFailure { Timber.w(it, "Network warmup failed") }
        }
        startupScope.launch {
            delay(2500L)
            runCatching { YoutubeLocalDecoder.prewarm() }
                .onFailure { Timber.w(it, "Local decoder prewarm failed") }
        }
    }
}
