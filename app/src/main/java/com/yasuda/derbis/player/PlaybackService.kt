package com.yasuda.derbis.player

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.MediaRouter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.app.ActivityManager
import android.os.Debug
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.yasuda.derbis.BuildConfig
import com.yasuda.derbis.MainActivity
import com.yasuda.derbis.data.DerbisBeatGridStore
import com.yasuda.derbis.data.DerbisMixStudioStore
import com.yasuda.derbis.domain.DerbisMixStudio
import com.yasuda.derbis.data.FavoritesStore
import com.yasuda.derbis.data.DerbisPreferences
import com.yasuda.derbis.data.PlaybackResolver
import com.yasuda.derbis.data.collections.PersonalCollectionHttp
import com.yasuda.derbis.data.collections.PersonalCollectionNetworkPolicy
import com.yasuda.derbis.data.collections.personalCollectionIdOf
import com.yasuda.derbis.data.collections.isPersonalCollectionTrack
import com.yasuda.derbis.data.network.DerbisHttpClientFactory
import com.yasuda.derbis.data.classifyPlaybackFailureReason
import com.yasuda.derbis.data.isTerminalPlaybackFailure
import com.yasuda.derbis.data.playbackRecoveryPlanFor
import com.yasuda.derbis.data.YoutubeMusicRepository
import com.yasuda.derbis.data.LyricsLatencyProfiles
import com.yasuda.derbis.domain.DerbisAudioSettings
import com.yasuda.derbis.domain.DerbisAutomationSettings
import com.yasuda.derbis.domain.PlaybackBufferMode
import com.yasuda.derbis.domain.ReplayGainMetadata
import com.yasuda.derbis.domain.ReplayGainMode
import com.yasuda.derbis.domain.selectReplayGain
import com.yasuda.derbis.domain.LyricLine
import com.yasuda.derbis.domain.LyricsEngine
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.feature.discord.DerbisDiscordRichPresence
import com.yasuda.derbis.feature.discord.derbisDiscordPresenceSnapshot
import com.yasuda.derbis.feature.radio.LIVE_RADIO_SOURCE
import com.yasuda.derbis.feature.radio.LiveRadioPlaylistDataSource
import com.yasuda.derbis.feature.radio.LiveRadioStreamMetadata
import com.yasuda.derbis.feature.systemintegration.OPLUS_LYRIC_INFO_KEY
import com.yasuda.derbis.feature.systemintegration.OPlusLyricsPayloadContext
import com.yasuda.derbis.feature.systemintegration.buildOPlusLyricsPayload
import com.yasuda.derbis.feature.systemintegration.detectDerbisRomMediaCapabilities
import com.yasuda.derbis.feature.systemintegration.DerbisDeviceIntegrationCenter
import com.yasuda.derbis.feature.systemintegration.DerbisDeviceProfiles
import com.yasuda.derbis.feature.systemintegration.derbisIslandAdapters
import com.yasuda.derbis.feature.systemintegration.derbisIslandPlayerState
import com.yasuda.derbis.feature.radio.RadioUrlPolicy
import com.yasuda.derbis.feature.radio.isLiveRadio
import com.yasuda.derbis.feature.radio.liveRadioStreamMetadata
import com.yasuda.derbis.feature.radio.nextLiveRadioStreamMetadata
import com.yasuda.derbis.feature.cast.RemotePlaybackBackend
import com.yasuda.derbis.feature.cast.RemotePlaybackBackendProvider
import com.yasuda.derbis.feature.cast.RemotePlaybackState
import com.yasuda.derbis.feature.cast.CastHandoffConverter
import com.yasuda.derbis.feature.cast.LocalPlaybackSnapshot
import com.yasuda.derbis.feature.audio.DerbisAudioOutputRepository
import com.yasuda.derbis.feature.audio.DerbisAudioRouteSelectionState
import com.yasuda.derbis.feature.audio.findDerbisAudioOutputDevice
import com.yasuda.derbis.feature.audio.isDerbis360HeadphoneOutput
import com.yasuda.derbis.feature.audio.isDerbis360HeadphoneOutputType
import com.yasuda.derbis.feature.audio.queryDerbisAudioOutputState
import com.yasuda.derbis.player.queue.PersistentQueueEngine
import com.yasuda.derbis.player.queue.PlaybackQueueSnapshot
import com.yasuda.derbis.player.queue.playbackQueueIdentity
import com.yasuda.derbis.player.sabr.SabrDataSource
import com.yasuda.derbis.player.sabr.SabrStreamSpec
import com.yasuda.derbis.runtime.RuntimeHooks
import com.yasuda.derbis.runtime.RuntimeSignal
import com.yasuda.derbis.player.enhanced.EnhancedAudioMetrics
import com.yasuda.derbis.player.enhanced.EnhancedAudioProcessor
import com.yasuda.derbis.player.enhanced.EnhancedAudioSourceFormatListener
import com.yasuda.derbis.widget.DerbisWidgetBridge
import com.yasuda.derbis.widget.DerbisWidgetCenter
import com.yasuda.derbis.widget.studio.WidgetStudioPlaybackSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.io.IOException
import java.util.ArrayList

@UnstableApi
class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private lateinit var autoLibrary: AndroidAutoLibrary
    private lateinit var queueEngine: PersistentQueueEngine
    private lateinit var favoritesStore: FavoritesStore
    private lateinit var mixStudioStore: DerbisMixStudioStore
    private lateinit var beatGridStore: DerbisBeatGridStore
    private lateinit var resolver: PlaybackResolver
    private lateinit var musicRepository: YoutubeMusicRepository
    private lateinit var sharedMediaSourceFactory: MediaSource.Factory
    private lateinit var stabilitySignals: PlaybackStabilitySignals
    private val adaptivePlaybackPolicy by lazy { AdaptivePlaybackPolicy(this) }
    private val playbackWarmup by lazy { PlaybackWarmup(this) }
    private var queueSkipJob: Job? = null
    private var appExitRequested = false
    private var servicePrefetchJob: Job? = null
    private var servicePrefetchTargetIdentity: String? = null
    private var servicePrefetchRequestToken = 0L
    @Volatile private var preparedQueueNext: PreparedQueueNext? = null
    private var serviceRecoveryJob: Job? = null
    private var stickyRestoreJob: Job? = null
    private var playbackWatchdogJob: Job? = null
    private var queueTransitionJob: Job? = null
    private var queueTransitionMonitorJob: Job? = null
    private var memoryGuardJob: Job? = null
    private var castHandoffJob: Job? = null
    private var remotePlaybackBackend: RemotePlaybackBackend? = null
    private var remoteSessionPlayer: Player? = null
    private var remoteSessionPlayerListener: Player.Listener? = null
    private var memoryGuardHighSamples = 0
    private var lastMemoryRecycleElapsedMs = 0L
    private var lastDiscordPresenceSyncElapsedMs = 0L
    private val discordLyricsEngine = LyricsEngine()
    @Volatile private var discordLyricsTrackId: String = ""
    @Volatile private var discordLyricsLines: List<LyricLine> = emptyList()
    @Volatile private var discordLyricsSynced: Boolean = false
    @Volatile private var discordLyricsLatencyProfiles = LyricsLatencyProfiles()
    private var lastDiscordLyricLine: String = ""
    private var transitionPlayer: ExoPlayer? = null
    private var transitionNormalization: NormalizationAudioProcessor? = null
    private var transitionEightDAudioProcessor: EightDAudioProcessor? = null
    private var currentAudioSettings = DerbisAudioSettings()
    private var activeCustomBufferProfile: PlaybackBufferProfile? = null

    @Volatile
    private var aaudioOutputRequested = false
    private var primaryAudioSink: AudioSink? = null
    private var currentAudioNormalization = false
    private val enhancedAudioProcessor = EnhancedAudioProcessor()
    private val normalizationProcessor = NormalizationAudioProcessor()
    private val equalizerProcessor = DerbisEqualizerAudioProcessor()
    private val parametricEqualizerProcessor = DerbisParametricEqualizerAudioProcessor()
    private val spatialAudioProcessor = StereoSpatialAudioProcessor()
    private val eightDAudioProcessor = EightDAudioProcessor()
    private val limiterProcessor = TruePeakLimiterAudioProcessor()
    private val visualizerProcessor = VisualizerAudioProcessor()
    private val beatGridProcessor = BeatGridAnalysisAudioProcessor { trackId, grid ->
        if (::beatGridStore.isInitialized) beatGridStore.save(trackId, grid)
    }
    private val pcm16OutputProcessor = Pcm16OutputAudioProcessor()
    private val audioOffloadController = AudioOffloadController()
    private lateinit var playbackWakeLock: PowerManager.WakeLock
    private lateinit var playbackStateStore: SharedPreferences
    private lateinit var audioManager: AudioManager
    private lateinit var mediaRouter: MediaRouter
    private var serviceRecoveryAttempts = 0
    private var serviceRecoveryExhausted = false
    private var watchdogPositionMs = C.TIME_UNSET
    private var watchdogAdvancedAtMs = 0L
    private val watchdogRecoveryAllowance = WatchdogRecoveryAllowance()
    private var lastPlaybackExpected: Boolean? = null
    private var lastPlaybackHeartbeatAtMs = 0L
    private var appliedPlayerWakeMode = C.WAKE_MODE_NETWORK

    private data class PreparedQueueNext(
        val sourceIdentity: String,
        val targetIdentity: String,
        val targetTrackId: String,
        val resolved: Track,
        val preparedAtElapsedMs: Long,
        val streamPreloaded: Boolean = false,
        val preloadedBytes: Long = 0L
    )

    private val platformMediaAudioAttributes by lazy {
        android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refreshAudioRouteCenterState()
            if (addedDevices.any { it.isSink && isBluetoothOutputType(it.type) }) {
                resumeAfterRouteReconnect()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (routedOutputIsBluetooth && removedDevices.any { it.isSink && isBluetoothOutputType(it.type) }) {
                lostRouteWasBluetooth = true
            }
            refreshAudioRouteCenterState()
        }
    }

    private val mediaRouteCallback = object : MediaRouter.SimpleCallback() {
        override fun onRouteSelected(router: MediaRouter, type: Int, info: MediaRouter.RouteInfo) {
            refreshAudioRouteCenterState()
        }

        override fun onRouteUnselected(router: MediaRouter, type: Int, info: MediaRouter.RouteInfo) {
            refreshAudioRouteCenterState()
        }

        override fun onRouteChanged(router: MediaRouter, info: MediaRouter.RouteInfo) {
            refreshAudioRouteCenterState()
        }
    }

    private val audioRouteVolumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val streamType = intent?.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_MUSIC)
                ?: AudioManager.STREAM_MUSIC
            if (streamType == AudioManager.STREAM_MUSIC) refreshAudioRouteCenterState()
        }
    }

    private val deviceVolumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != VOLUME_CHANGED_ACTION) return
            if (intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) != AudioManager.STREAM_MUSIC) return
            pausePlaybackIfMuted()
        }
    }

    companion object {
        private const val RUNNING_LOW_LEVEL = 10
        private const val RUNNING_CRITICAL_LEVEL = 15
        const val EXTRA_VIDEO_URL = "derbis.videoUrl"
        const val EXTRA_VIDEO_CACHE_KEY = "derbis.videoCacheKey"
        const val EXTRA_VIDEO_MIME_TYPE = "derbis.videoMimeType"
        const val EXTRA_VIDEO_MODE = "derbis.videoMode"
        const val EXTRA_LIVE_RADIO = "derbis.liveRadio"
        const val EXTRA_PERSONAL_COLLECTION = "derbis.personalCollection"
        const val EXTRA_YOUTUBE_LOUDNESS_DB = "derbis.youtubeLoudnessDb"
        const val EXTRA_YOUTUBE_PERCEPTUAL_LOUDNESS_DB = "derbis.youtubePerceptualLoudnessDb"
        const val EXTRA_REPLAY_GAIN_TRACK_DB = "derbis.replayGain.trackDb"
        const val EXTRA_REPLAY_GAIN_ALBUM_DB = "derbis.replayGain.albumDb"
        const val EXTRA_REPLAY_GAIN_TRACK_PEAK = "derbis.replayGain.trackPeak"
        const val EXTRA_REPLAY_GAIN_ALBUM_PEAK = "derbis.replayGain.albumPeak"
        const val ACTION_GET_PLATFORM_TOKEN = "derbis.media.GET_PLATFORM_TOKEN"
        const val ACTION_SET_VIDEO_SUBTITLE = "derbis.media.SET_VIDEO_SUBTITLE"
        const val KEY_PLATFORM_TOKEN = "derbis.media.PLATFORM_TOKEN"
        const val KEY_VIDEO_SUBTITLE_ID = "derbis.media.VIDEO_SUBTITLE_ID"
        private const val PLAYBACK_STATE_PREFS = "derbis.playback.service.state"
        private const val KEY_PLAYBACK_EXPECTED = "playbackExpected"
        private const val KEY_PLAYBACK_HEARTBEAT_AT = "playbackHeartbeatAt"
        private const val PLAYBACK_HEARTBEAT_INTERVAL_MS = 30_000L
        private const val STICKY_RESTORE_MAX_AGE_MS = 12L * 60L * 60L * 1_000L
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private const val WATCHDOG_INTERVAL_MS = 5_000L
        private const val LIVE_RADIO_STALE_PAUSE_MS = 20_000L
        private const val WATCHDOG_STALL_TIMEOUT_MS = 15_000L
        private const val MAX_TRANSITION_LOOKAHEAD_MS = 45_000L
        private const val TRANSITION_PREPARE_TIMEOUT_MS = 8_000L
        private const val PREPARED_QUEUE_WAIT_MS = 2_000L
        private const val PREPARED_QUEUE_MAX_AGE_MS = 15L * 60L * 1_000L
        private const val PREPARED_QUEUE_INITIAL_PRIME_BYTES = 384L * 1024L
        private const val PREPARED_QUEUE_PRIME_BYTES = 1536L * 1024L
        private const val STREAM_PRELOAD_TRIGGER_REMAINING_MS = 30_000L
        private const val PRIMARY_HANDOFF_WARN_MS = 5_000L
        private const val PRIMARY_HANDOFF_SYNC_TIMEOUT_MS = 2_500L
        private const val RECOVERY_CONNECTIVITY_WAIT_MS = 15_000L
        private const val RECOVERY_CONNECTIVITY_POLL_MS = 5_000L
        private const val PRIMARY_HANDOFF_FADE_MS = 240L
        // Allows the Media3 STATE_ENDED callback for the outgoing deck to drain after a handoff.
        private const val CROSSFADE_COMPLETION_GUARD_MS = 2_000L
        private const val PRIMARY_HANDOFF_SYNC_TOLERANCE_MS = 250L
        private const val PRIMARY_HANDOFF_SYNC_LEAD_MS = 120L
        private const val TRANSITION_STEP_MS = 50L
        private const val SEEK_TOLERANCE_MS = 1_500L
        private const val AUDIO_ROUTE_SELECTION_VERIFY_MS = 1_200L
        private const val AUDIO_ROUTE_FAILURE_FEEDBACK_MS = 4_000L
        private val ONLINE_RECOVERY_DELAYS_MS = longArrayOf(500L, 2_000L, 5_000L, 10_000L)
        private val LOCAL_RECOVERY_DELAYS_MS = longArrayOf(250L, 750L, 1_500L, 3_000L, 5_000L, 10_000L)

        private val _activePlayerFlow = MutableStateFlow<ExoPlayer?>(null)
        val activePlayerFlow: StateFlow<ExoPlayer?> = _activePlayerFlow.asStateFlow()

        private val _remotePlaybackStateFlow = MutableStateFlow(RemotePlaybackState())
        val remotePlaybackStateFlow: StateFlow<RemotePlaybackState> = _remotePlaybackStateFlow.asStateFlow()

        private val _sleepTimerStateFlow = MutableStateFlow<PlaybackSleepTimerState>(PlaybackSleepTimerState.Disabled)
        val sleepTimerStateFlow: StateFlow<PlaybackSleepTimerState> = _sleepTimerStateFlow.asStateFlow()

        private val _crossfadeProgressFlow = MutableStateFlow(CrossfadeProgress())
        val crossfadeProgressFlow: StateFlow<CrossfadeProgress> = _crossfadeProgressFlow.asStateFlow()

        private val _liveRadioMetadataFlow = MutableStateFlow(LiveRadioStreamMetadata())
        internal val liveRadioMetadataFlow: StateFlow<LiveRadioStreamMetadata> = _liveRadioMetadataFlow.asStateFlow()

        private val _enhancedAudioMetricsFlow = MutableStateFlow(EnhancedAudioMetrics())
        val enhancedAudioMetricsFlow: StateFlow<EnhancedAudioMetrics> = _enhancedAudioMetricsFlow.asStateFlow()

        internal val audioOffloadState: AudioOffloadState
            get() = activeService?.audioOffloadController?.state ?: AudioOffloadState()

        @Volatile
        var activePlayer: ExoPlayer? = null
            private set(value) {
                field = value
                _activePlayerFlow.value = value
            }

        fun startSleepTimer(minutes: Int): Boolean {
            val service = activeService ?: return false
            if (minutes <= 0) {
                service.sleepTimer.cancel()
                return true
            }
            val totalMs = minutes * 60_000L
            service.sleepTimer.startCountdown(totalMs, service.automationSettings.fadeMsFor(totalMs))
            return true
        }

        fun startSleepTimerEndOfTrack(): Boolean {
            val service = activeService ?: return false
            service.sleepTimer.startEndOfTrack()
            return true
        }

        fun cancelSleepTimer(): Boolean {
            val service = activeService ?: return false
            service.sleepTimer.cancel()
            return true
        }

        fun publishLiveRadioArtwork(trackId: String, artworkUri: String): Boolean {
            val service = activeService ?: return false
            service.serviceScope.launch { service.publishLiveRadioArtworkInternal(trackId, artworkUri) }
            return true
        }

        fun publishTrackMetadata(track: Track): Boolean {
            val service = activeService ?: return false
            service.serviceScope.launch { service.publishTrackMetadataInternal(track) }
            return true
        }

        fun publishSystemLyrics(
            track: Track,
            lines: List<LyricLine>,
            synced: Boolean,
            provider: String
        ): Boolean {
            val service = activeService ?: return false
            service.serviceScope.launch {
                service.publishSystemLyricsInternal(track, lines, synced, provider)
            }
            return true
        }

        @Volatile
        private var activeService: PlaybackService? = null

        private val premiumAudioSettingsLock = Any()
        private var pendingAudioSettings: DerbisAudioSettings? = null
        private var pendingAudioNormalization = false

        @Volatile
        private var uiRecoveryAvailable = false

        fun setUiRecoveryAvailable(available: Boolean) {
            uiRecoveryAvailable = available
        }

        /** True after a pause transport command until a new Play command reaches the session. */
        internal val isExplicitPauseActive: Boolean
            get() = activeService?.playbackPausedByUser == true

        /** Records a fresh in-app play selection before its stream resolves asynchronously. */
        internal fun beginExplicitPlaybackRequest() {
            activeService?.playbackPausedByUser = false
        }

        suspend fun stopForAppExit() {
            val service = activeService ?: return
            service.appExitRequested = true
            service.queueSkipJob?.cancel()
            service.serviceRecoveryJob?.cancel()
            service.stickyRestoreJob?.cancel()
            service.cancelQueueTransition()
            service.cancelServicePrefetch()
            service.clearPreparedQueueNextInternal()
            service.markPlaybackExpected(false, force = true)
            withContext(Dispatchers.IO) {
                service.playbackStateStore.edit().putBoolean(KEY_PLAYBACK_EXPECTED, false).commit()
            }
            service.mediaSession?.player?.clearMediaItems()
            service.stopDerbisPlaybackForAppExit()
        }

        fun requestQueueNext(): Boolean {
            val service = activeService ?: return false
            service.skipQueue(forward = true, respectRepeatOne = false)
            return true
        }

        fun requestQueuePrevious(): Boolean {
            val service = activeService ?: return false
            service.skipQueue(forward = false, respectRepeatOne = false)
            return true
        }

        fun requestAudioOutput(routeKey: String?): Boolean {
            val service = activeService ?: return false
            service.audioRouteSelectionJob?.cancel()
            service.audioRouteSelectionJob = service.serviceScope.launch {
                service.selectAudioOutput(routeKey)
            }
            return true
        }

        fun prepareQueueNext(track: com.yasuda.derbis.domain.Track): Boolean =
            activeService?.prepareQueueNextInternal(track) == true

        fun clearPreparedQueueNext() {
            activeService?.clearPreparedQueueNextInternal()
        }

        fun clearPreparedQueueNextIfStale() {
            activeService?.clearPreparedQueueNextIfStaleInternal()
        }

        fun consumePreparedQueueNext(trackId: String) {
            activeService?.consumePreparedQueueNextInternal(trackId)
        }

        @Volatile
        var isQueueTransitionInProgress: Boolean = false
            private set

        @Volatile
        private var crossfadeCompletionGuardUntilElapsedMs: Long = 0L

        /**
         * Covers the short asynchronous tail after an incoming deck has been handed off. Without
         * it, the outgoing primary deck's delayed STATE_ENDED callback can start another queue
         * advance and interrupt the song that is already audible.
         */
        val isQueueTransitionCompletionSuppressed: Boolean
            get() = shouldSuppressCrossfadeCompletion(
                transitionInProgress = isQueueTransitionInProgress,
                nowElapsedMs = SystemClock.elapsedRealtime(),
                completionGuardUntilElapsedMs = crossfadeCompletionGuardUntilElapsedMs
            )

        private fun armCrossfadeCompletionGuard() {
            crossfadeCompletionGuardUntilElapsedMs =
                SystemClock.elapsedRealtime() + CROSSFADE_COMPLETION_GUARD_MS
        }

        private fun clearCrossfadeCompletionGuard() {
            crossfadeCompletionGuardUntilElapsedMs = 0L
        }

        fun applyPremiumAudioSettings(
            settings: DerbisAudioSettings,
            audioNormalization: Boolean
        ) {
            val normalized = settings.normalized()
            synchronized(premiumAudioSettingsLock) {
                pendingAudioSettings = normalized
                pendingAudioNormalization = audioNormalization
                activeService?.applyPremiumAudioSettingsInternal(normalized, audioNormalization)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val autoSearchRequests by lazy {
        AndroidAutoSearchRequests<MediaSession.ControllerInfo, MediaItem>(serviceScope) { autoLibrary.search(it) }
    }
    private val sleepTimer by lazy {
        PlaybackSleepTimer(
            scope = serviceScope,
            onFadeVolume = ::applySleepFadeVolume,
            onExpired = ::pausePlaybackForSleepTimer
        )
    }
    private var sleepTimerStateJob: Job? = null
    private var automationSettingsJob: Job? = null
    private var discordLyricsProfilesJob: Job? = null

    @Volatile
    private var automationSettings = DerbisAutomationSettings()
    private val playbackFailureGuard = ConsecutivePlaybackFailureGuard()
    private var sleepFadeBaselineVolume: Float? = null
    private var pausedByRouteLossAtMs: Long? = null
    private var liveRadioPausedAtMs = C.TIME_UNSET
    private var liveRadioReconnectJob: Job? = null
    // Set by an explicit pause or stop action. It blocks delayed worker callbacks from turning an
    // explicitly paused session back into audible playback.
    @Volatile
    private var playbackPausedByUser = false
    private var lastTransitionMediaId: String? = null
    private val romMediaCapabilities by lazy { detectDerbisRomMediaCapabilities() }
    private var romMediaId = ""
    private var romMediaGeneration = 0L
    private var deviceIntegration: DerbisDeviceIntegrationCenter? = null
    private var pendingDeviceLyrics: (() -> Unit)? = null
    private var deviceIntegrationResolved = false

    @Volatile
    private var routedOutputIsBluetooth = false
    private var lostRouteWasBluetooth = false
    private var deviceVolumeReceiverRegistered = false
    private var audioRouteVolumeReceiverRegistered = false
    private var audioRouteSelectionJob: Job? = null
    private var audioRouteFeedbackResetJob: Job? = null
    private var preferredAudioRouteKey: String? = null
    private var audioRouteSelectionState = DerbisAudioRouteSelectionState.Idle
    private var systemMediaActions: SystemMediaActionController? = null
    private val studioWidgetSync by lazy { WidgetStudioPlaybackSync(this) }
    private val platformTokenCommand by lazy { SessionCommand(ACTION_GET_PLATFORM_TOKEN, Bundle.EMPTY) }
    private val videoSubtitleCommand by lazy { SessionCommand(ACTION_SET_VIDEO_SUBTITLE, Bundle.EMPTY) }

    private fun applyPremiumAudioSettingsInternal(
        settings: DerbisAudioSettings,
        audioNormalization: Boolean
    ) {
        val normalized = settings.normalized()
        val parametricEnabled = normalized.parametricEqualizerEnabled && normalized.activeParametricProfile != null
        enhancedAudioProcessor.userEnabled = normalized.enhancedAudioEnabled
        normalizationProcessor.enabled = audioNormalization || normalized.replayGainActive
        equalizerProcessor.enabled = normalized.equalizerEnabled && !parametricEnabled
        equalizerProcessor.setBandLevels(normalized.bandLevels)
        equalizerProcessor.bassBoost = normalized.bassBoost
        equalizerProcessor.preampDb = normalized.preampDb
        parametricEqualizerProcessor.setConfiguration(parametricEnabled, normalized.activeParametricProfile)
        spatialAudioProcessor.strength = normalized.virtualizer
        eightDAudioProcessor.enabled = normalized.eightDAudioEnabled
        eightDAudioProcessor.experience = normalized.derbis360Experience
        transitionEightDAudioProcessor?.apply {
            enabled = normalized.eightDAudioEnabled
            experience = normalized.derbis360Experience
        }
        limiterProcessor.enabled = truePeakLimiterRequired(normalized, parametricEnabled, audioNormalization)
        updateQueueTransitionSettings(normalized, audioNormalization)
        activePlayer?.currentMediaItem?.mediaMetadata?.extras?.let { extras ->
            val queueSnapshot = queueEngine.state.value
            configureNormalizationProcessor(
                processor = normalizationProcessor,
                settings = currentAudioSettings,
                youtubeLoudnessDb = extras.floatOrNull(EXTRA_YOUTUBE_LOUDNESS_DB),
                youtubePerceptualLoudnessDb = extras.floatOrNull(EXTRA_YOUTUBE_PERCEPTUAL_LOUDNESS_DB),
                replayGain = ReplayGainMetadata(
                    trackGainDb = extras.floatOrNull(EXTRA_REPLAY_GAIN_TRACK_DB),
                    albumGainDb = extras.floatOrNull(EXTRA_REPLAY_GAIN_ALBUM_DB),
                    trackPeak = extras.floatOrNull(EXTRA_REPLAY_GAIN_TRACK_PEAK),
                    albumPeak = extras.floatOrNull(EXTRA_REPLAY_GAIN_ALBUM_PEAK)
                ),
                albumContext = replayGainAlbumContext(
                    snapshot = queueSnapshot,
                    queueIndex = queueSnapshot.currentIndex,
                    track = queueSnapshot.currentTrack
                )
            )
        }
        updateAaudioOutputRequest(normalized.aaudioOutputEnabled)
        refreshAudioOffloadPolicy()
    }

    private fun refreshAudioOffloadPolicy() {
        val player = activePlayer ?: return
        val parameters = player.playbackParameters
        audioOffloadController.update(
            player,
            AudioOffloadInputs.from(
                settings = currentAudioSettings,
                enhancedAudioRequiresPcmProcessing = currentAudioSettings.enhancedAudioEnabled &&
                    !enhancedAudioProcessor.isLosslessSource,
                audioNormalization = currentAudioNormalization,
                speed = parameters.speed,
                pitch = parameters.pitch,
                skipSilenceEnabled = player.skipSilenceEnabled,
                aaudioOutputSupported = NativeAudioIntegration.isAaudioOutputSupported()
            )
        )
    }

    private fun configureNormalizationProcessor(
        processor: NormalizationAudioProcessor,
        settings: DerbisAudioSettings,
        youtubeLoudnessDb: Float?,
        youtubePerceptualLoudnessDb: Float?,
        replayGain: ReplayGainMetadata,
        albumContext: Boolean
    ) {
        val mode = settings.effectiveReplayGainMode
        val selection = selectReplayGain(mode, replayGain, albumContext)
        if (mode != ReplayGainMode.OFF && selection != null) {
            processor.setReplayGain(
                gainDb = selection.gainDb,
                peak = selection.peak,
                preampDb = settings.replayGainPreampDb,
                preventClipping = settings.replayGainPreventClipping
            )
        } else {
            processor.setYoutubeLoudness(youtubeLoudnessDb, youtubePerceptualLoudnessDb)
        }
    }

    private fun replayGainAlbumContext(
        snapshot: PlaybackQueueSnapshot,
        queueIndex: Int,
        track: Track?
    ): Boolean {
        val current = track ?: return false
        if (queueIndex !in snapshot.tracks.indices) return false
        val identity = replayGainAlbumIdentity(current)
        if (identity.isBlank()) return false
        val neighborIndices = if (snapshot.shuffleEnabled) {
            val playbackOrder = snapshot.shuffleOrder
                .filter { it in snapshot.tracks.indices }
                .distinct()
                .takeIf { it.size == snapshot.tracks.size }
                ?: return false
            val cursor = playbackOrder.indexOf(queueIndex)
            if (cursor < 0) return false
            sequenceOf(
                playbackOrder.getOrNull(cursor - 1),
                playbackOrder.getOrNull(cursor + 1)
            ).filterNotNull()
        } else {
            sequenceOf(queueIndex - 1, queueIndex + 1).filter { it in snapshot.tracks.indices }
        }
        return neighborIndices.any { replayGainAlbumIdentity(snapshot.tracks[it]) == identity }
    }

    private fun queueTransitionTargetIndex(snapshot: PlaybackQueueSnapshot): Int? {
        val currentIndex = snapshot.currentIndex
        if (currentIndex !in snapshot.tracks.indices) return null
        return if (snapshot.shuffleEnabled) {
            val order = snapshot.shuffleOrder
                .filter { it in snapshot.tracks.indices }
                .distinct()
                .takeIf { it.size == snapshot.tracks.size }
                ?: return null
            val cursor = order.indexOf(currentIndex)
            when {
                cursor < 0 -> null
                cursor + 1 < order.size -> order[cursor + 1]
                snapshot.repeatMode == com.yasuda.derbis.domain.RepeatMode.All -> order.firstOrNull()
                else -> null
            }
        } else {
            when {
                currentIndex < snapshot.tracks.lastIndex -> currentIndex + 1
                snapshot.repeatMode == com.yasuda.derbis.domain.RepeatMode.All -> 0
                else -> null
            }
        }
    }

    private fun replayGainAlbumIdentity(track: Track): String {
        val browseId = track.albumBrowseId.trim()
        if (browseId.isNotEmpty()) return "id:$browseId"
        val appleAlbumId = track.appleAlbumId.trim()
        if (appleAlbumId.isNotEmpty()) return "apple:$appleAlbumId"
        val canonicalAlbumUrl = track.canonicalAlbumUrl.trim()
        if (canonicalAlbumUrl.isNotEmpty()) return "url:$canonicalAlbumUrl"
        val album = track.album.trim().lowercase()
        if (album.isEmpty()) return ""
        val albumArtist = track.albumArtist.ifBlank { track.artist }.trim().lowercase()
        return if (albumArtist.isNotEmpty()) "$albumArtist\u0000$album" else "album:$album"
    }

    private fun Bundle?.floatOrNull(key: String): Float? =
        this?.takeIf { it.containsKey(key) }?.getFloat(key)?.takeIf { it.isFinite() }

    private fun DefaultAudioSink.Builder.withDerbisAudioOutput(context: Context): DefaultAudioSink.Builder = apply {
        NativeAudioIntegration.audioOutputProvider(context) { aaudioOutputRequested }?.let(::setAudioOutputProvider)
    }

    private fun updateAaudioOutputRequest(requested: Boolean) {
        if (aaudioOutputRequested == requested) return
        aaudioOutputRequested = requested
        if (!NativeAudioIntegration.isAaudioOutputSupported()) return
        val player = activePlayer ?: return
        val sink = primaryAudioSink ?: return
        val provider = NativeAudioIntegration.audioOutputProvider(this) { aaudioOutputRequested } ?: return
        player.createMessage { _, _ -> sink.setAudioOutputProvider(provider) }.send()
    }

    private fun activateServiceAndApplyPendingAudioSettings() {
        synchronized(premiumAudioSettingsLock) {
            activeService = this
            pendingAudioSettings?.let { settings ->
                applyPremiumAudioSettingsInternal(settings, pendingAudioNormalization)
            }
        }
    }

    private fun updateRomMediaGeneration(mediaItem: MediaItem?) {
        val mediaId = mediaItem?.mediaId.orEmpty()
        if (mediaId == romMediaId) return
        romMediaId = mediaId
        romMediaGeneration = (romMediaGeneration + 1L).coerceAtLeast(1L)
    }

    private fun publishLiveRadioArtworkInternal(trackId: String, artworkUri: String) {
        val player = activePlayer ?: return
        val current = player.currentMediaItem ?: return
        if (current.mediaId != trackId || !isLiveRadioMediaItem(current)) return
        val artwork = Uri.parse(artworkUri)
        if (current.mediaMetadata.artworkUri == artwork) return
        val updatedItem = current
            .buildUpon()
            .setMediaMetadata(current.mediaMetadata.buildUpon().setArtworkUri(artwork).build())
            .build()
        val index = player.currentMediaItemIndex
        if (index in 0 until player.mediaItemCount) player.replaceMediaItem(index, updatedItem)
    }

    private fun publishTrackMetadataInternal(track: Track) {
        val player = activePlayer ?: return
        if (track.streamUrl.isBlank()) return
        val fresh = DerbisMediaItemFactory.metadataOnly(track).mediaMetadata
        for (index in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(index)
            if (item.localConfiguration?.uri?.toString() != track.streamUrl) continue
            val extras = Bundle(item.mediaMetadata.extras ?: Bundle.EMPTY).apply {
                putString("derbis.title", track.title)
                putString("derbis.artist", track.artist)
                putString("derbis.album", track.album)
            }
            val metadata = item.mediaMetadata
                .buildUpon()
                .setTitle(fresh.title)
                .setDisplayTitle(fresh.displayTitle)
                .setArtist(fresh.artist)
                .setSubtitle(fresh.subtitle)
                .setAlbumTitle(fresh.albumTitle)
                .setArtworkUri(fresh.artworkUri)
                .setExtras(extras)
                .build()
            if (metadata != item.mediaMetadata) {
                player.replaceMediaItem(index, item.buildUpon().setMediaMetadata(metadata).build())
            }
        }
    }

    private fun publishSystemLyricsInternal(
        track: Track,
        lines: List<LyricLine>,
        synced: Boolean,
        provider: String
    ) {
        // Cache for Discord Rich Presence before any ROM-gated early return below, so lyrics
        // reach the profile status on devices without the OxygenOS lyric metadata channel.
        discordLyricsTrackId = track.id
        discordLyricsLines = lines
        discordLyricsSynced = synced
        // Lyrics usually arrive after playback starts; republish now so the line shows up
        // immediately instead of waiting for the next playback tick or the 30 s resync.
        runCatching { (activePlayer ?: mediaSession?.player)?.let(::publishDiscordLyricLineIfChanged) }
        val mediaId = DerbisMediaItemFactory.mediaId(track)
        val forwardLyrics = { deviceIntegration?.onLyrics(mediaId, track.id, lines, synced) }
        if (deviceIntegrationResolved) forwardLyrics() else pendingDeviceLyrics = { forwardLyrics() }
        if (!romMediaCapabilities.timedLyricsMetadata) return
        val player = activePlayer ?: return
        val current = player.currentMediaItem ?: return
        if (mediaId.isBlank() || current.mediaId != mediaId || romMediaId != mediaId) return

        val payload = buildOPlusLyricsPayload(
            track = track,
            lines = lines,
            synced = synced,
            context = OPlusLyricsPayloadContext(
                provider = provider,
                packageName = packageName,
                generation = romMediaGeneration
            )
        )
        val existingExtras = current.mediaMetadata.extras
        val existingPayload = existingExtras?.getString(OPLUS_LYRIC_INFO_KEY)
        if (existingPayload == payload) return
        if (payload == null && existingPayload == null) return

        val extras = Bundle(existingExtras ?: Bundle.EMPTY).apply {
            if (payload == null) remove(OPLUS_LYRIC_INFO_KEY) else putString(OPLUS_LYRIC_INFO_KEY, payload)
        }
        val updatedMetadata = current.mediaMetadata
            .buildUpon()
            .setExtras(extras)
            .build()
        val updatedItem = current
            .buildUpon()
            .setMediaMetadata(updatedMetadata)
            .build()
        val index = player.currentMediaItemIndex
        if (index in 0 until player.mediaItemCount && player.currentMediaItem?.mediaId == mediaId) {
            player.replaceMediaItem(index, updatedItem)
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaRouter = getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter
        playbackStateStore = getSharedPreferences(PLAYBACK_STATE_PREFS, Context.MODE_PRIVATE)
        playbackWakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:PlaybackService")
            .apply { setReferenceCounted(false) }
        queueEngine = PersistentQueueEngine.get(this)
        favoritesStore = FavoritesStore(this)
        mixStudioStore = DerbisMixStudioStore(this)
        beatGridStore = DerbisBeatGridStore(this)
        resolver = PlaybackResolver.getInstance(this)
        musicRepository = YoutubeMusicRepository(this)
        autoLibrary = AndroidAutoLibrary(this)
        val prefs = DerbisPreferences(this)
        val snapshot = prefs.snapshot()
        val automaticBufferProfile = AdaptivePlaybackPolicy(this).serviceBuffers()
        val bufferProfile = playbackBufferProfile(automaticBufferProfile, snapshot.audioSettings.playbackBuffer)
        activeCustomBufferProfile = bufferProfile.takeIf {
            snapshot.audioSettings.playbackBuffer.mode == PlaybackBufferMode.CUSTOM
        }
        val stableBufferProfile = stabilityBufferProfile(bufferProfile, customProfile = activeCustomBufferProfile != null)
        val sharedAllocator = androidx.media3.exoplayer.upstream.DefaultAllocator(true, androidx.media3.common.C.DEFAULT_BUFFER_SEGMENT_SIZE)
        fun buildLoadControl(profile: PlaybackBufferProfile) = DefaultLoadControl.Builder()
            .setAllocator(sharedAllocator)
            .setBufferDurationsMs(
                profile.minBufferMs,
                profile.maxBufferMs,
                profile.playbackBufferMs,
                profile.rebufferMs
            )
            .setBackBuffer(profile.backBufferMs, false)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        stabilitySignals = PlaybackStabilitySignals()
        val loadControl = AdaptiveStabilityLoadControl(
            normal = buildLoadControl(bufferProfile),
            stable = buildLoadControl(stableBufferProfile),
            signals = stabilitySignals
        )
        val baseHttpFactory = PlaybackNetworkStack.playbackFactory(this)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Accept-Encoding" to "identity"
                )
            )
        val upstreamFactory = DerbisYoutubeDataSource.Factory(baseHttpFactory)
        val liveRadioHttpClient = DerbisHttpClientFactory.streaming(this).newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(com.yasuda.derbis.feature.radio.RadioRedirectInterceptor())
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                if (!RadioUrlPolicy.isAllowed(request.url.toString())) {
                    throw IOException("Blocked unsafe live radio URL")
                }
                chain.proceed(request.newBuilder().header("Icy-MetaData", "1").build())
            }
            .dns(RadioUrlPolicy.publicDns)
            .readTimeout(LIVE_RADIO_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
        val liveRadioDataSourceFactory = OkHttpDataSource.Factory(liveRadioHttpClient)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Accept-Encoding" to "identity",
                    "Icy-MetaData" to "1",
                    "User-Agent" to "Derbis/${BuildConfig.VERSION_NAME} (Android; Live Radio)"
                )
            )
        val cache = DerbisMediaCache.get(this)
        val cacheSinkFactory = CacheDataSink.Factory()
            .setCache(cache)
            .setFragmentSize(256L * 1024L)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(cacheSinkFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val defaultFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        val localDataSourceFactory = DefaultDataSource.Factory(this)
        val personalCollectionDataSourceFactory: (String) -> DataSource.Factory = { collectionId ->
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(PersonalCollectionHttp.mediaClient)
                        .setDefaultRequestProperties(
                            mapOf(
                                "Accept" to "*/*",
                                "Accept-Encoding" to "identity",
                                PersonalCollectionNetworkPolicy.COLLECTION_HEADER to collectionId
                            )
                        )
                )
                .setCacheWriteDataSinkFactory(cacheSinkFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }

        val mergingFactory = DerbisMediaSourceFactory(
            defaultFactory,
            cacheDataSourceFactory,
            localDataSourceFactory,
            SabrDataSource.Factory(baseHttpFactory),
            liveRadioDataSourceFactory,
            personalCollectionDataSourceFactory
        ).apply {
            setLoadErrorHandlingPolicy(DerbisPlaybackLoadErrorHandlingPolicy)
        }
        sharedMediaSourceFactory = mergingFactory

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(false)
                    .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(
                        arrayOf(
                            enhancedAudioProcessor,
                            normalizationProcessor,
                            equalizerProcessor,
                            parametricEqualizerProcessor,
                            spatialAudioProcessor,
                            eightDAudioProcessor,
                            limiterProcessor,
                            visualizerProcessor,
                            beatGridProcessor,
                            pcm16OutputProcessor
                        )
                    )
                    .withDerbisAudioOutput(context)
                    .build()
                    .also { primaryAudioSink = it }
            }
        }
        renderersFactory.setEnableDecoderFallback(true)
        renderersFactory.setMediaCodecSelector(VideoDecoderFallback.Shared.selector(NativeAudioIntegration.mediaCodecSelector))
        renderersFactory.setExtensionRendererMode(NativeAudioIntegration.EXTENSION_RENDERER_MODE)

        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mergingFactory)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(stabilitySignals)
        player.addListener(loadControl)
        player.addAnalyticsListener(stabilitySignals)
        player.addAnalyticsListener(
            EnhancedAudioSourceFormatListener(enhancedAudioProcessor, "primary") {
                refreshAudioOffloadPolicy()
            }
        )
        player.addAnalyticsListener(audioOffloadController)
        RuntimeHooks.attachPlayer(player)
        RuntimeHooks.player(RuntimeSignal.PLAYER_CREATED)
        RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_CREATE)
        currentAudioSettings = snapshot.audioSettings.normalized()
        currentAudioNormalization = snapshot.audioNormalization
        player.skipSilenceEnabled = snapshot.skipSilence
        applyPremiumAudioSettingsInternal(snapshot.audioSettings, snapshot.audioNormalization)
        RuntimeHooks.dsp(RuntimeSignal.DSP_CREATED)
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        mediaRouter.addCallback(
            MediaRouter.ROUTE_TYPE_LIVE_AUDIO,
            mediaRouteCallback,
            MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS
        )
        registerAudioRouteVolumeReceiver()
        refreshAudioRouteCenterState()
        serviceScope.launch {
            enhancedAudioProcessor.metricsState.collect {
                _enhancedAudioMetricsFlow.value = it
            }
        }

        activePlayer = player
        refreshAudioOffloadPolicy()
        player.addListener(object : Player.Listener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                refreshAudioOffloadPolicy()
            }

            override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
                refreshAudioOffloadPolicy()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (activePlayer !== player) return
                queueEngine.state.value.currentTrack?.id?.takeIf(String::isNotBlank)
                    ?.let(beatGridProcessor::beginTrack)
                if (mediaItem?.mediaId != lastTransitionMediaId) {
                    lastTransitionMediaId = mediaItem?.mediaId
                    VideoDecoderFallback.Shared.reset()
                    _liveRadioMetadataFlow.value = LiveRadioStreamMetadata()
                    liveRadioPausedAtMs = C.TIME_UNSET
                    liveRadioReconnectJob?.cancel()
                }
                updateRomMediaGeneration(mediaItem)
                RuntimeHooks.player(
                    action = RuntimeSignal.PLAYER_TRANSITION,
                    mode = if (mediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true) {
                        RuntimeSignal.MODE_VIDEO
                    } else {
                        RuntimeSignal.MODE_AUDIO
                    }
                )
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT && sleepTimer.consumeEndOfTrackBoundary()) {
                    pausePlaybackForSleepTimer()
                    return
                }
                updatePlayerWakeMode(player, mediaItem)
                applyPlaybackTrackSelection(player, mediaItem)
                if (serviceRecoveryJob?.isActive != true && stickyRestoreJob?.isActive != true) {
                    serviceRecoveryExhausted = false
                    serviceRecoveryAttempts = 0
                }
                val extras = mediaItem?.mediaMetadata?.extras
                enhancedAudioProcessor.isRemotePlayback = _remotePlaybackStateFlow.value.connected
                val queueSnapshot = queueEngine.state.value
                configureNormalizationProcessor(
                    processor = normalizationProcessor,
                    settings = currentAudioSettings,
                    youtubeLoudnessDb = extras.floatOrNull(EXTRA_YOUTUBE_LOUDNESS_DB),
                    youtubePerceptualLoudnessDb = extras.floatOrNull(EXTRA_YOUTUBE_PERCEPTUAL_LOUDNESS_DB),
                    replayGain = ReplayGainMetadata(
                        trackGainDb = extras.floatOrNull(EXTRA_REPLAY_GAIN_TRACK_DB),
                        albumGainDb = extras.floatOrNull(EXTRA_REPLAY_GAIN_ALBUM_DB),
                        trackPeak = extras.floatOrNull(EXTRA_REPLAY_GAIN_TRACK_PEAK),
                        albumPeak = extras.floatOrNull(EXTRA_REPLAY_GAIN_ALBUM_PEAK)
                    ),
                    albumContext = replayGainAlbumContext(
                        snapshot = queueSnapshot,
                        queueIndex = queueSnapshot.currentIndex,
                        track = queueSnapshot.currentTrack
                    )
                )
                watchdogPositionMs = C.TIME_UNSET
                watchdogAdvancedAtMs = SystemClock.elapsedRealtime()
                if (!isLocalMediaItem(mediaItem) && !isLiveRadioMediaItem(mediaItem)) prefetchServiceQueueNext()
            }

            override fun onMetadata(metadata: Metadata) {
                if (activePlayer !== player || !isLiveRadioMediaItem(player.currentMediaItem)) return
                val icy = (0 until metadata.length()).firstNotNullOfOrNull { index -> metadata[index] as? IcyInfo } ?: return
                val received = liveRadioStreamMetadata(icy.title, String(icy.rawMetadata, Charsets.UTF_8))
                _liveRadioMetadataFlow.value = nextLiveRadioStreamMetadata(_liveRadioMetadataFlow.value, received)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (activePlayer !== player) return
                RuntimeHooks.player(
                    action = RuntimeSignal.PLAYER_STATE,
                    value = playbackState,
                    mode = if (player.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true) {
                        RuntimeSignal.MODE_VIDEO
                    } else {
                        RuntimeSignal.MODE_AUDIO
                    }
                )
                if (playbackState == Player.STATE_READY && player.playWhenReady) {
                    playbackFailureGuard.onHealthyPlayback()
                }
                if (playbackState != Player.STATE_ENDED) return
                // An outgoing deck can report STATE_ENDED after an explicit pause. That late
                // callback must never start queue auto-advance again.
                if (playbackPausedByUser) return
                if (isLiveRadioMediaItem(player.currentMediaItem)) {
                    markPlaybackExpected(false, force = true)
                    releasePlaybackWakeLock()
                    return
                }
                if (sleepTimer.consumeEndOfTrackBoundary()) {
                    pausePlaybackForSleepTimer()
                    return
                }
                if (DerbisWidgetBridge.onNext == null && !isQueueTransitionCompletionSuppressed) {
                    skipQueue(forward = true, respectRepeatOne = true, autoAdvance = true)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (activePlayer !== player) return
                if (VideoDecoderFallback.Shared.redirect(error)) {
                    Timber.w("Video decoder failed; retrying with the next available decoder")
                    player.prepare()
                    return
                }
                NativeAudioIntegration.redirectFailedPlatformDecoder(error)?.let { mimeType ->
                    Timber.w("Platform audio decoder failed for %s; retrying with FFmpeg", mimeType)
                    player.prepare()
                    return
                }
                updatePlaybackProtection(player)
                discardIncompatiblePlaybackCache(error)
                val failureKind = classifyPlaybackFailureReason(playbackFailureReasonOf(error))
                RuntimeHooks.player(
                    action = RuntimeSignal.PLAYER_ERROR,
                    mode = if (player.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true) {
                        RuntimeSignal.MODE_VIDEO
                    } else {
                        RuntimeSignal.MODE_AUDIO
                    },
                    failure = failureKind.ordinal
                )
                if (isLiveRadioMediaItem(player.currentMediaItem)) {
                    markPlaybackExpected(false, force = true)
                    releasePlaybackWakeLock()
                } else if (isTerminalPlaybackFailure(failureKind)) {
                    serviceRecoveryExhausted = true
                    markPlaybackExpected(false, force = true)
                    releasePlaybackWakeLock()
                    skipUnrecoverableTrack()
                } else {
                    scheduleServiceRecovery(error)
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (activePlayer !== player) return
                // A worker that was already completing when the listener pressed pause must not
                // be allowed to resurrect the primary deck. Normal user/system Play commands go
                // through the forwarding session player below and clear this flag first.
                if (playWhenReady && playbackPausedByUser) {
                    runCatching { player.pause() }
                        .onFailure { Timber.w(it, "Paused playback restart could not be stopped") }
                    return
                }
                if (isLiveRadioMediaItem(player.currentMediaItem)) {
                    if (playWhenReady) reconnectStaleLiveRadio(player) else liveRadioPausedAtMs = SystemClock.elapsedRealtime()
                }
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) {
                    lostRouteWasBluetooth = lostRouteWasBluetooth || routedOutputIsBluetooth
                    pausedByRouteLossAtMs = SystemClock.elapsedRealtime()
                } else {
                    lostRouteWasBluetooth = false
                    pausedByRouteLossAtMs = null
                }
                if (!playWhenReady) {
                    // This is intentionally unconditional. A transition coroutine may already
                    // have completed while its secondary ExoPlayer is still the audible deck.
                    // Releasing that deck is the only reliable pause-all-audio boundary.
                    cancelPendingPlaybackAfterPause()
                }
                if (playWhenReady && serviceRecoveryExhausted) {
                    serviceRecoveryExhausted = false
                    serviceRecoveryAttempts = 0
                    markPlaybackExpected(true, force = true)
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (activePlayer !== player) return
                updatePlaybackProtection(player)
                deviceIntegration?.onPlayerEvents(events)
                if (
                    events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
                    events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                    events.contains(Player.EVENT_POSITION_DISCONTINUITY)
                ) {
                    publishDiscordRichPresence(player)
                    lastDiscordPresenceSyncElapsedMs = SystemClock.elapsedRealtime()
                }
            }
        })
        startPlaybackWatchdog(player)
        serviceScope.launch {
            val profile = withContext(Dispatchers.IO) { DerbisDeviceProfiles.current(applicationContext) }
            val adapters = derbisIslandAdapters(applicationContext, profile)
            deviceIntegrationResolved = true
            if (adapters.isEmpty()) {
                pendingDeviceLyrics = null
                return@launch
            }
            deviceIntegration = DerbisDeviceIntegrationCenter(
                scope = serviceScope,
                adapters = adapters,
                latencyProfiles = prefs.lyricsLatencyProfilesFlow,
                audioOutput = DerbisAudioOutputRepository.state,
                playerState = { derbisIslandPlayerState(player) }
            )
            pendingDeviceLyrics?.invoke()
            pendingDeviceLyrics = null
            deviceIntegration?.refresh()
        }
        sleepTimerStateJob?.cancel()
        sleepTimerStateJob = serviceScope.launch {
            sleepTimer.state.collect { state -> _sleepTimerStateFlow.value = state }
        }
        automationSettingsJob?.cancel()
        // Keeps the presence lyric line aligned with the user's latency calibration.
        discordLyricsProfilesJob?.cancel()
        discordLyricsProfilesJob = serviceScope.launch {
            prefs.lyricsLatencyProfilesFlow.collect { profiles ->
                discordLyricsLatencyProfiles = profiles
            }
        }
        audioRouteSelectionJob?.cancel()
        audioRouteFeedbackResetJob?.cancel()
        automationSettingsJob = serviceScope.launch {
            prefs.automationSettingsFlow.collect { settings ->
                automationSettings = settings
                updateDeviceVolumeReceiver(settings.pauseOnMute)
            }
        }
        serviceScope.launch {
            while (isActive) {
                val queueTrack = queueEngine.state.value.currentTrack
                val playbackDeck = activePlayer ?: player
                if (
                    queueTrack != null &&
                    playbackDeck.currentMediaItem?.mediaId == DerbisMediaItemFactory.mediaId(queueTrack)
                ) {
                    queueEngine.updatePosition(playbackDeck.currentPosition)
                }
                val now = SystemClock.elapsedRealtime()
                if (now - lastDiscordPresenceSyncElapsedMs >= 30_000L) {
                    publishDiscordRichPresence(playbackDeck)
                    lastDiscordPresenceSyncElapsedMs = now
                }
                if (playbackDeck.isPlaying) {
                    publishDiscordLyricLineIfChanged(playbackDeck)
                }
                delay(2_000L)
            }
        }

        val systemActions = SystemMediaActionController(
            context = this,
            scope = serviceScope,
            queueState = queueEngine.state,
            favoriteMembership = favoritesStore.observeMembership(),
            onStateChanged = studioWidgetSync::onActionsChanged
        )
        systemMediaActions = systemActions

        val callback = object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val commandBuilder = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                    .buildUpon()
                SystemMediaActionController.sessionCommands.forEach(commandBuilder::add)
                if (controller.packageName == packageName) {
                    commandBuilder.add(platformTokenCommand)
                    commandBuilder.add(videoSubtitleCommand)
                }
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                    .setAvailableSessionCommands(commandBuilder.build())
                    .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                    .build()
            }

            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                isForPlayback: Boolean
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                serviceScope.launch(Dispatchers.IO) {
                    runCatching {
                        if (queueEngine.state.value.tracks.isEmpty()) {
                            queueEngine.restore(
                                fallbackTracks = emptyList(),
                                fallbackIndex = -1,
                                fallbackPositionMs = 0L,
                                fallbackRadioEnabled = true
                            )
                        }
                        val snapshot = queueEngine.state.value
                        val track = snapshot.currentTrack ?: error("Nessun brano da ripristinare")
                        if (track.isLiveRadio()) error("Live radio cannot be resumed as recorded playback")
                        val item = if (isForPlayback) {
                            val resolved = resolveQueueTrack(track)
                            queueEngine.updateTrackAt(snapshot.currentIndex, resolved)
                            if (!isLocalPlaybackTrack(resolved)) prefetchServiceQueueNext()
                            DerbisMediaItemFactory.build(resolved)
                        } else {
                            DerbisMediaItemFactory.metadataOnly(track)
                        }
                        MediaSession.MediaItemsWithStartPosition(
                            listOf(item),
                            0,
                            snapshot.positionMs.coerceAtLeast(0L)
                        )
                    }.onSuccess(future::set).onFailure(future::setException)
                }
                return future
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                return when (customCommand.customAction) {
                    ACTION_GET_PLATFORM_TOKEN -> {
                        if (controller.packageName != packageName) {
                            Futures.immediateFuture(
                                SessionResult(androidx.media3.session.SessionError.ERROR_PERMISSION_DENIED)
                            )
                        } else {
                            val extras = Bundle().apply {
                                putParcelable(KEY_PLATFORM_TOKEN, session.platformToken)
                            }
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, extras))
                        }
                    }
                    ACTION_SET_VIDEO_SUBTITLE -> {
                        if (controller.packageName != packageName) {
                            Futures.immediateFuture(
                                SessionResult(androidx.media3.session.SessionError.ERROR_PERMISSION_DENIED)
                            )
                        } else {
                            applyVideoSubtitleSelection(
                                player,
                                args.getString(KEY_VIDEO_SUBTITLE_ID)?.trim().orEmpty()
                            )
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                    }
                    else -> SystemMediaCommand.fromCustomAction(customCommand.customAction)
                        ?.let { command -> handleSystemMediaCommand(command, session.player) }
                        ?: super.onCustomCommand(session, controller, customCommand, args)
                }
            }
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootParams = LibraryParams.Builder().setExtras(Bundle(params?.extras ?: Bundle.EMPTY).apply {
                    putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
                }).build()
                return Futures.immediateFuture(LibraryResult.ofItem(autoLibrary.root(), rootParams))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return libraryListFuture(params) {
                    autoLibrary.children(parentId, page, pageSize)
                }
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                return libraryItemFuture(null) { autoLibrary.item(mediaId) }
            }

            override fun onSearch(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<Void>> {
                val request = autoSearchRequests.start(browser, query)
                return autoLibraryFuture {
                    val items = request.await()
                    if (!autoSearchRequests.isCurrent(browser, query, request)) {
                        throw CancellationException("Search superseded")
                    }
                    session.notifySearchResultChanged(browser, query, items.size, params)
                    Timber.d("Android Auto search ready: items=%d", items.size)
                    LibraryResult.ofVoid(params)
                }
            }

            override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
                autoSearchRequests.disconnect(controller)
                super.onDisconnected(session, controller)
            }

            override fun onGetSearchResult(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return autoLibraryFuture {
                    val items = autoSearchRequests.result(browser, query)
                        .androidAutoWindow(AndroidAutoPageWindow.of(page, pageSize, 100))
                    LibraryResult.ofItemList(items, params)
                }
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<MediaItem>
            ): ListenableFuture<List<MediaItem>> {
                return mediaItemsFuture { autoLibrary.playableItems(mediaItems) }
            }

            override fun onSetMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                if (mediaItems.none { it.mediaId.startsWith(AndroidAutoLibrary.ID_TRACK_PREFIX) }) {
                    return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
                }
                val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                val job = serviceScope.launch {
                    try {
                        val selected = autoLibrary.playableSelection(mediaItems, startIndex)
                        future.set(MediaSession.MediaItemsWithStartPosition(listOf(selected), 0, startPositionMs))
                    } catch (cancelled: CancellationException) {
                        future.cancel(false)
                        throw cancelled
                    } catch (error: Exception) {
                        future.setException(error)
                    }
                }
                future.addListener({ if (future.isCancelled) job.cancel() }, com.google.common.util.concurrent.MoreExecutors.directExecutor())
                job.invokeOnCompletion { if (!future.isDone) future.cancel(false) }
                return future
            }
        }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        remotePlaybackBackend = RemotePlaybackBackendProvider.create(this)
        serviceScope.launch {
            remotePlaybackBackend?.state?.collect { state ->
                _remotePlaybackStateFlow.value = state
            }
        }
        val sessionPlayer = bindRemoteSessionPlayer(player)
        val forwardingPlayer = createSessionPlayerFacade(sessionPlayer)

        mediaSession = MediaLibrarySession.Builder(this, forwardingPlayer, callback)
            .setSessionActivity(sessionActivity)
            .setMediaButtonPreferences(systemActions.initialButtons())
            .build()
            .also(systemActions::attach)
            .also(studioWidgetSync::attach)

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
        NotificationManagerCompat.from(this).deleteNotificationChannel(RETIRED_PLAYBACK_LIVE_UPDATE_CHANNEL_ID)
        activateServiceAndApplyPendingAudioSettings()
        refreshAudioRouteCenterState()
        startQueueTransitionMonitor(player)
        startMemoryGuard(player)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null && !scheduleStickyPlaybackRestore(startId)) return START_NOT_STICKY
        return START_STICKY
    }

    private fun createSessionPlayerFacade(delegate: Player): Player = object : androidx.media3.common.ForwardingPlayer(delegate) {
    private val commandListeners = java.util.IdentityHashMap<Player.Listener, Player.Listener>()

    override fun play() {
        // Every supported transport (full/mini player, notification, Bluetooth and
        // external media controllers) reaches this session player, so it is the single
        // place that authorizes a new audible playback start after an explicit pause.
        playbackPausedByUser = false
        super.play()
    }

    override fun pause() {
        // Do not merely forward pause to the MediaSession primary player: a crossfade
        // may have a separate incoming ExoPlayer that is currently audible.
        pauseAllAudiblePlayback(delegate, explicitUserRequest = true)
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            playbackPausedByUser = false
            super.setPlayWhenReady(true)
        } else {
            // Some controller integrations issue this lower-level command rather than
            // Player.pause(); it has exactly the same pause-all-audio contract.
            pauseAllAudiblePlayback(delegate, explicitUserRequest = true)
        }
    }

    override fun addListener(listener: Player.Listener) {
        val wrapped = commandListeners.getOrPut(listener) {
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = listener.onEvents(player, events)
                override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) = listener.onTimelineChanged(timeline, reason)
                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) = listener.onTracksChanged(tracks)
                override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) = listener.onMediaMetadataChanged(mediaMetadata)
                override fun onPlaylistMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) = listener.onPlaylistMetadataChanged(mediaMetadata)
                override fun onIsLoadingChanged(isLoading: Boolean) = listener.onIsLoadingChanged(isLoading)
                override fun onPlaybackStateChanged(playbackState: Int) = listener.onPlaybackStateChanged(playbackState)
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = listener.onPlayWhenReadyChanged(playWhenReady, reason)
                override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) = listener.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
                override fun onIsPlayingChanged(isPlaying: Boolean) = listener.onIsPlayingChanged(isPlaying)
                override fun onRepeatModeChanged(repeatMode: Int) = listener.onRepeatModeChanged(repeatMode)
                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = listener.onShuffleModeEnabledChanged(shuffleModeEnabled)
                override fun onPlayerError(error: PlaybackException) = listener.onPlayerError(error)
                override fun onPlayerErrorChanged(error: PlaybackException?) = listener.onPlayerErrorChanged(error)
                override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) = listener.onPositionDiscontinuity(oldPosition, newPosition, reason)
                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = listener.onPlaybackParametersChanged(playbackParameters)
                override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) = listener.onSeekBackIncrementChanged(seekBackIncrementMs)
                override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) = listener.onSeekForwardIncrementChanged(seekForwardIncrementMs)
                override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) = listener.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)
                override fun onTrackSelectionParametersChanged(parameters: androidx.media3.common.TrackSelectionParameters) = listener.onTrackSelectionParametersChanged(parameters)
                override fun onAudioSessionIdChanged(audioSessionId: Int) = listener.onAudioSessionIdChanged(audioSessionId)
                override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) = listener.onAudioAttributesChanged(audioAttributes)
                override fun onVolumeChanged(volume: Float) = listener.onVolumeChanged(volume)
                override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) = listener.onSkipSilenceEnabledChanged(skipSilenceEnabled)
                override fun onSurfaceSizeChanged(width: Int, height: Int) = listener.onSurfaceSizeChanged(width, height)
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) = listener.onVideoSizeChanged(videoSize)
                override fun onRenderedFirstFrame() = listener.onRenderedFirstFrame()
                override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) = listener.onDeviceInfoChanged(deviceInfo)
                override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) = listener.onDeviceVolumeChanged(volume, muted)
                override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) = listener.onCues(cueGroup)

                override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
                    listener.onAvailableCommandsChanged(getAvailableCommands())
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    listener.onMediaItemTransition(mediaItem, reason)
                    listener.onAvailableCommandsChanged(getAvailableCommands())
                }
            }
        }
        super.addListener(wrapped)
    }

    override fun removeListener(listener: Player.Listener) {
        commandListeners.remove(listener)?.let { super.removeListener(it) }
    }

    override fun getDuration(): Long {
        if (isLiveRadioMediaItem(currentMediaItem)) return androidx.media3.common.C.TIME_UNSET
        val realDuration = super.getDuration()
        if (realDuration > 0L) return realDuration
        val metadataDuration = currentMediaItem?.mediaMetadata?.extras
            ?.getLong("derbis.durationMs", androidx.media3.common.C.TIME_UNSET)
            ?: androidx.media3.common.C.TIME_UNSET
        return metadataDuration.takeIf { it > 0L } ?: androidx.media3.common.C.TIME_UNSET
    }

    override fun isCurrentMediaItemSeekable(): Boolean {
        return !isLiveRadioMediaItem(currentMediaItem) && getDuration() > 0L && !isCurrentMediaItemLive
    }

    override fun isCurrentMediaItemLive(): Boolean {
        return isLiveRadioMediaItem(currentMediaItem) || super.isCurrentMediaItemLive()
    }

    override fun getAvailableCommands(): androidx.media3.common.Player.Commands {
        val commands = super.getAvailableCommands().buildUpon()
        if (isCurrentMediaItemSeekable) {
            commands.add(androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        } else {
            commands.remove(androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        }
        commands.remove(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS)
        commands.remove(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        commands.remove(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT)
        commands.remove(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        if (canSkipToPreviousTrack()) {
            commands.addAll(
                androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS,
                androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
            )
        }
        if (canSkipToNextTrack()) {
            commands.addAll(
                androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT,
                androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
            )
        }
        return commands.build()
    }

    override fun isCommandAvailable(command: Int): Boolean = availableCommands.contains(command)

    override fun hasNextMediaItem(): Boolean = canSkipToNextTrack()

    override fun hasPreviousMediaItem(): Boolean = canSkipToPreviousTrack()

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (isLiveRadioMediaItem(currentMediaItem)) return
        super.seekTo(mediaItemIndex, positionMs)
    }

    override fun seekToNext() = seekRemoteOrLocal(forward = true)

    override fun seekToNextMediaItem() = seekRemoteOrLocal(forward = true)

    override fun seekToPrevious() = seekRemoteOrLocal(forward = false)

    override fun seekToPreviousMediaItem() = seekRemoteOrLocal(forward = false, allowRewind = false)

    private fun seekRemoteOrLocal(forward: Boolean, allowRewind: Boolean = true) {
        if (isLiveRadioMediaItem(currentMediaItem)) return
        if (delegate.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE) {
            skipQueue(forward, respectRepeatOne = false, allowRewind = allowRewind)
            return
        }
        if (!forward && allowRewind && delegate.currentPosition > 5_000L) {
            delegate.seekTo(0L)
            queueEngine.updatePosition(0L)
            return
        }
        val canMoveInsideWindow = if (forward) {
            delegate.currentMediaItemIndex < delegate.mediaItemCount - 1
        } else {
            delegate.currentMediaItemIndex > 0
        }
        if (canMoveInsideWindow) {
            if (forward) super.seekToNextMediaItem() else super.seekToPreviousMediaItem()
        } else {
            skipCastQueue(forward, delegate)
        }
    }
}

    /**
     * Rebuilds the optional Cast wrapper around the currently audible local deck. The wrapper is
     * recreated only after the outgoing deck has faded out, so its teardown can never cut audio.
     */
    private fun bindRemoteSessionPlayer(localPlayer: Player): Player {
        remoteSessionPlayerListener?.let { listener ->
            runCatching { remoteSessionPlayer?.removeListener(listener) }
        }
        val transport = remotePlaybackBackend?.attachLocalPlayer(localPlayer) ?: localPlayer
        val listener = object : Player.Listener {
            override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
                if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
                    audioRouteSelectionJob?.cancel()
                    audioRouteSelectionJob = null
                    audioRouteFeedbackResetJob?.cancel()
                    clearPreferredAudioOutput()
                    audioRouteSelectionState = DerbisAudioRouteSelectionState.Idle
                    cancelQueueTransition()
                    cancelServicePrefetch()
                    clearPreparedQueueNextInternal()
                    handoffQueueToCast(transport)
                } else {
                    castHandoffJob?.cancel()
                }
                refreshAudioRouteCenterState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (transport.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE) return
                val mediaId = mediaItem?.mediaId ?: return
                val snapshot = queueEngine.state.value
                val index = snapshot.tracks.indexOfFirst { DerbisMediaItemFactory.metadataOnly(it).mediaId == mediaId }
                if (index >= 0 && index != snapshot.currentIndex) {
                    queueEngine.select(index, transport.currentPosition, rememberCurrent = true)
                }
            }
        }
        transport.addListener(listener)
        remoteSessionPlayer = transport
        remoteSessionPlayerListener = listener
        return transport
    }

    /**
     * Keeps MediaSession, notification and Bluetooth controls attached to the deck the listener
     * is actually hearing. Unlike rebuilding an old primary deck, this is metadata/control-only
     * and does not stop or seek the incoming audio.
     */
    private fun setMediaSessionDeck(player: Player) {
        val session = mediaSession ?: return
        systemMediaActions?.detach()
        studioWidgetSync.detach()
        session.setPlayer(createSessionPlayerFacade(player))
        systemMediaActions?.attach(session)
        studioWidgetSync.attach(session)
    }

    private fun handleSystemMediaCommand(
        command: SystemMediaCommand,
        sessionPlayer: Player
    ): ListenableFuture<SessionResult> {
        val queue = queueEngine.state.value
        val sessionItem = systemMediaSessionItem(sessionPlayer.currentMediaItem)
        val invalidState = Futures.immediateFuture(SessionResult(androidx.media3.session.SessionError.ERROR_INVALID_STATE))
        when (command) {
            SystemMediaCommand.AddFavorite, SystemMediaCommand.RemoveFavorite -> {
                val target = systemFavoriteTarget(queue, sessionItem) ?: return invalidState
                val favorite = command == SystemMediaCommand.AddFavorite
                val result = SettableFuture.create<SessionResult>()
                serviceScope.launch {
                    try {
                        favoritesStore.setFavorite(target, favorite)
                        result.set(SessionResult(SessionResult.RESULT_SUCCESS))
                    } catch (cancelled: CancellationException) {
                        result.cancel(false)
                        throw cancelled
                    } catch (error: Exception) {
                        Timber.w(error, "System favorite action failed")
                        result.set(SessionResult(androidx.media3.session.SessionError.ERROR_UNKNOWN))
                    }
                }
                return result
            }
            SystemMediaCommand.EnableShuffle, SystemMediaCommand.DisableShuffle -> {
                if (!systemQueueControlsAvailable(queue, sessionItem)) return invalidState
                queueEngine.setShuffle(command == SystemMediaCommand.EnableShuffle)
                clearPreparedQueueNextIfStaleInternal()
            }
            SystemMediaCommand.SetRepeatOff, SystemMediaCommand.SetRepeatAll, SystemMediaCommand.SetRepeatOne -> {
                val mode = command.repeatModeTarget
                if (mode == null || !systemQueueControlsAvailable(queue, sessionItem)) return invalidState
                queueEngine.setRepeatMode(mode)
                val remote = sessionPlayer.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
                sessionPlayer.repeatMode = when {
                    mode == com.yasuda.derbis.domain.RepeatMode.One -> Player.REPEAT_MODE_ONE
                    mode == com.yasuda.derbis.domain.RepeatMode.All && remote -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }
                clearPreparedQueueNextIfStaleInternal()
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    private fun canSkipToPreviousTrack(): Boolean {
        if (isLiveRadioMediaItem(mediaSession?.player?.currentMediaItem ?: activePlayer?.currentMediaItem)) return false
        return queueEngine.state.value.currentTrack != null
    }

    private fun canSkipToNextTrack(): Boolean {
        if (isLiveRadioMediaItem(mediaSession?.player?.currentMediaItem ?: activePlayer?.currentMediaItem)) return false
        val snapshot = queueEngine.state.value
        if (snapshot.currentTrack == null) return false
        return snapshot.currentIndex < snapshot.tracks.lastIndex ||
            snapshot.shuffleEnabled ||
            snapshot.repeatMode != com.yasuda.derbis.domain.RepeatMode.Off ||
            snapshot.radioEnabled
    }

    private fun skipQueue(
        forward: Boolean,
        respectRepeatOne: Boolean,
        autoAdvance: Boolean = false,
        allowRewind: Boolean = true,
        onAdvanced: (() -> Unit)? = null
    ) {
        // A deliberate Next/Previous command is a new listener action. In contrast, an old
        // end-of-track callback must not override an explicit Pause.
        if (autoAdvance && playbackPausedByUser) return
        if (!autoAdvance) playbackPausedByUser = false
        val activeMediaItem = activePlayer?.currentMediaItem ?: mediaSession?.player?.currentMediaItem
        if (isLiveRadioMediaItem(activeMediaItem)) return
        cancelQueueTransition()
        queueSkipJob?.cancel()
        cancelServicePrefetch()
        queueSkipJob = serviceScope.launch {
            if (autoAdvance && playbackPausedByUser) return@launch
            val player = activePlayer ?: return@launch
            if (allowRewind && rewindInsteadOfSkip(player, forward)) return@launch
            val target = withContext(Dispatchers.IO) {
                selectSkipTarget(forward, respectRepeatOne)
            } ?: return@launch
            if (autoAdvance && playbackPausedByUser) return@launch
            val resolved = withContext(Dispatchers.IO) {
                resolveSkipTarget(target, autoAdvance)
            } ?: run {
                abandonAutoAdvance(target, autoAdvance)
                return@launch
            }
            if (autoAdvance && playbackPausedByUser) return@launch
            playSkipTarget(player, resolved)
            onAdvanced?.invoke()
        }
    }

    private fun rewindInsteadOfSkip(player: ExoPlayer, forward: Boolean): Boolean {
        if (forward || player.currentPosition <= 5_000L) return false
        player.seekTo(0L)
        queueEngine.updatePosition(0L)
        return true
    }

    private suspend fun selectSkipTarget(forward: Boolean, respectRepeatOne: Boolean): com.yasuda.derbis.domain.Track? {
        if (queueEngine.state.value.tracks.isEmpty()) {
            queueEngine.restore(
                fallbackTracks = emptyList(),
                fallbackIndex = -1,
                fallbackPositionMs = 0L,
                fallbackRadioEnabled = true
            )
        }
        val selected = if (forward) queueEngine.next(respectRepeatOne) else queueEngine.previous()
        if (selected != null || !forward) return selected
        return expandRadioForSkip()
    }

    private suspend fun expandRadioForSkip(): com.yasuda.derbis.domain.Track? {
        val initial = queueEngine.state.value
        if (!initial.radioEnabled) return null
        val seed = initial.currentTrack ?: return null
        if (isLocalPlaybackTrack(seed) || !hasInternetCapableNetwork()) return null
        val additions = try {
            musicRepository.radio(seed, DerbisPreferences(this).languageCode(), 5)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Background radio expansion failed")
            emptyList()
        }
        if (additions.isEmpty()) return null
        val current = queueEngine.state.value
        if (!current.radioEnabled || current.generation != initial.generation ||
            current.currentTrack?.let(::playbackQueueIdentity) != playbackQueueIdentity(seed)
        ) return null
        queueEngine.appendRadioTracks(additions)
        return queueEngine.next(respectRepeatOne = false)
    }

    private suspend fun resolveSkipTarget(
        target: com.yasuda.derbis.domain.Track,
        autoAdvance: Boolean
    ): com.yasuda.derbis.domain.Track? {
        val targetIdentity = playbackQueueIdentity(target)
        preparedQueueTrackForTarget(targetIdentity)?.let { resolved ->
            consumePreparedQueueNextInternal(target.id.ifBlank { resolved.id })
            return resolved
        }
        return if (autoAdvance) {
            resolveQueueTrackPersistently(target)
        } else {
            runCatching { resolveQueueTrack(target) }
                .onFailure { Timber.w(it, "Background queue resolution failed") }
                .getOrNull()
        }
    }

    private fun abandonAutoAdvance(target: com.yasuda.derbis.domain.Track, autoAdvance: Boolean) {
        if (!autoAdvance) return
        Timber.e("Background queue auto-advance gave up for %s", target.title)
        markPlaybackExpected(false, force = true)
        releasePlaybackWakeLock()
    }

    private fun playSkipTarget(player: ExoPlayer, resolved: com.yasuda.derbis.domain.Track) {
        queueEngine.updateTrackAt(queueEngine.state.value.currentIndex, resolved)
        player.setMediaItem(DerbisMediaItemFactory.build(resolved))
        RuntimeHooks.player(RuntimeSignal.PLAYER_PREPARE)
        RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_PREPARE)
        player.prepare()
        player.play()
        queueEngine.updatePosition(0L)
        DerbisWidgetCenter.update(
            this,
            resolved.title,
            resolved.artist,
            resolved.largeThumbnailUrl.ifBlank { resolved.thumbnailUrl },
            true
        )
        if (!isLocalPlaybackTrack(resolved)) prefetchServiceQueueNext()
    }

    private fun prefetchServiceQueueNext() {
        if (isLiveRadioMediaItem(mediaSession?.player?.currentMediaItem)) return
        val target = queueEngine.upcoming(1).firstOrNull() ?: return
        prepareQueueNextInternal(target)
    }

    private fun prepareQueueNextInternal(target: Track, deepStreamPreload: Boolean = false): Boolean {
        if (!currentAudioSettings.preloadNextTrack) return false
        val snapshot = queueEngine.state.value
        val current = snapshot.currentTrack ?: return false
        val expectedNext = queueEngine.upcoming(1).firstOrNull() ?: return false
        val sourceIdentity = playbackQueueIdentity(current)
        val targetIdentity = playbackQueueIdentity(target)
        if (targetIdentity != playbackQueueIdentity(expectedNext)) return false

        val now = SystemClock.elapsedRealtime()
        preparedQueueNext?.let { prepared ->
            val matches = queuePrecacheMatchesTransition(
                preparedSourceIdentity = prepared.sourceIdentity,
                preparedTargetIdentity = prepared.targetIdentity,
                currentSourceIdentity = sourceIdentity,
                currentTargetIdentity = targetIdentity,
                preparedAtElapsedMs = prepared.preparedAtElapsedMs,
                nowElapsedMs = now,
                maxAgeMs = PREPARED_QUEUE_MAX_AGE_MS
            ) || queuePrecacheMatchesTarget(
                preparedTargetIdentity = prepared.targetIdentity,
                requestedTargetIdentity = targetIdentity,
                preparedAtElapsedMs = prepared.preparedAtElapsedMs,
                nowElapsedMs = now,
                maxAgeMs = PREPARED_QUEUE_MAX_AGE_MS
            )
            if (matches && prepared.resolved.streamUrl.isNotBlank()) {
                if (!deepStreamPreload || prepared.streamPreloaded) {
                    if (prepared.sourceIdentity != sourceIdentity) {
                        preparedQueueNext = prepared.copy(sourceIdentity = sourceIdentity)
                    }
                    return true
                }
                // Track already resolved, perform deep stream preloading (1.5 MB ArchiveTune chunk)
                if (servicePrefetchJob?.isActive == true && servicePrefetchTargetIdentity == targetIdentity) return true
                if (!isLocalPlaybackTrack(prepared.resolved) && !hasInternetCapableNetwork()) return false

                cancelServicePrefetch()
                val requestToken = ++servicePrefetchRequestToken
                servicePrefetchTargetIdentity = targetIdentity
                servicePrefetchJob = serviceScope.launch {
                    try {
                        val warmed = if (!isLocalPlaybackTrack(prepared.resolved)) {
                            withContext(Dispatchers.IO) {
                                runCatching { playbackWarmup.prime(prepared.resolved, PREPARED_QUEUE_PRIME_BYTES) }
                                    .onFailure { Timber.d(it, "Stream preloading warmup skipped") }
                                    .getOrDefault(false)
                            }
                        } else true
                        if (!queuePairStillCurrent(sourceIdentity, targetIdentity)) return@launch
                        preparedQueueNext = prepared.copy(
                            sourceIdentity = sourceIdentity,
                            streamPreloaded = warmed,
                            preloadedBytes = if (warmed) PREPARED_QUEUE_PRIME_BYTES else 0L
                        )
                    } finally {
                        if (servicePrefetchRequestToken == requestToken) {
                            servicePrefetchTargetIdentity = null
                            servicePrefetchJob = null
                        }
                    }
                }
                return true
            }
        }

        if (servicePrefetchJob?.isActive == true && servicePrefetchTargetIdentity == targetIdentity) return true
        if (!isLocalPlaybackTrack(target) && !hasInternetCapableNetwork()) return false

        cancelServicePrefetch()
        val requestToken = ++servicePrefetchRequestToken
        servicePrefetchTargetIdentity = targetIdentity
        servicePrefetchJob = serviceScope.launch {
            try {
                val resolved = withContext(Dispatchers.IO) {
                    runCatching { resolveQueueTrack(target) }
                        .onFailure { Timber.d(it, "Service queue prefetch skipped") }
                        .getOrNull()
                } ?: return@launch
                if (!queuePairStillCurrent(sourceIdentity, targetIdentity)) return@launch

                val primeBytes = if (deepStreamPreload) PREPARED_QUEUE_PRIME_BYTES else PREPARED_QUEUE_INITIAL_PRIME_BYTES
                val warmed = if (!isLocalPlaybackTrack(resolved)) {
                    withContext(Dispatchers.IO) {
                        runCatching { playbackWarmup.prime(resolved, primeBytes) }
                            .onFailure { Timber.d(it, "Prepared queue warmup skipped") }
                            .getOrDefault(false)
                    }
                } else true

                preparedQueueNext = PreparedQueueNext(
                    sourceIdentity = sourceIdentity,
                    targetIdentity = targetIdentity,
                    targetTrackId = target.id,
                    resolved = resolved,
                    preparedAtElapsedMs = SystemClock.elapsedRealtime(),
                    streamPreloaded = (deepStreamPreload && warmed) || isLocalPlaybackTrack(resolved),
                    preloadedBytes = if (warmed) primeBytes else 0L
                )
            } finally {
                if (servicePrefetchRequestToken == requestToken) {
                    servicePrefetchTargetIdentity = null
                    servicePrefetchJob = null
                }
            }
        }
        return true
    }

    private fun queuePairStillCurrent(sourceIdentity: String, targetIdentity: String): Boolean {
        val current = queueEngine.state.value.currentTrack?.let(::playbackQueueIdentity) ?: return false
        val next = queueEngine.upcoming(1).firstOrNull()?.let(::playbackQueueIdentity) ?: return false
        return current == sourceIdentity && next == targetIdentity
    }

    private fun preparedQueueTrackForTarget(targetIdentity: String): Track? {
        val prepared = preparedQueueNext ?: return null
        val now = SystemClock.elapsedRealtime()
        if (!queuePrecacheMatchesTarget(
                preparedTargetIdentity = prepared.targetIdentity,
                requestedTargetIdentity = targetIdentity,
                preparedAtElapsedMs = prepared.preparedAtElapsedMs,
                nowElapsedMs = now,
                maxAgeMs = PREPARED_QUEUE_MAX_AGE_MS
            ) || prepared.resolved.streamUrl.isBlank()
        ) {
            if (!isQueuePrecacheFresh(prepared.preparedAtElapsedMs, now, PREPARED_QUEUE_MAX_AGE_MS)) {
                preparedQueueNext = null
            }
            return null
        }
        return prepared.resolved
    }

    private fun preparedQueueTrackForTransition(sourceIdentity: String, targetIdentity: String): Track? {
        val prepared = preparedQueueNext ?: return null
        val now = SystemClock.elapsedRealtime()
        if (!queuePrecacheMatchesTransition(
                preparedSourceIdentity = prepared.sourceIdentity,
                preparedTargetIdentity = prepared.targetIdentity,
                currentSourceIdentity = sourceIdentity,
                currentTargetIdentity = targetIdentity,
                preparedAtElapsedMs = prepared.preparedAtElapsedMs,
                nowElapsedMs = now,
                maxAgeMs = PREPARED_QUEUE_MAX_AGE_MS
            ) || prepared.resolved.streamUrl.isBlank()
        ) {
            if (!isQueuePrecacheFresh(prepared.preparedAtElapsedMs, now, PREPARED_QUEUE_MAX_AGE_MS)) {
                preparedQueueNext = null
            }
            return null
        }
        return prepared.resolved
    }

    private suspend fun awaitPreparedQueueTrackForTransition(
        sourceIdentity: String,
        targetIdentity: String
    ): Track? {
        preparedQueueTrackForTransition(sourceIdentity, targetIdentity)?.let { return it }
        val inFlight = servicePrefetchJob
        if (servicePrefetchTargetIdentity != targetIdentity || inFlight?.isActive != true) return null
        return withTimeoutOrNull(PREPARED_QUEUE_WAIT_MS) {
            while (inFlight.isActive) {
                preparedQueueTrackForTransition(sourceIdentity, targetIdentity)?.let { return@withTimeoutOrNull it }
                delay(25L)
            }
            preparedQueueTrackForTransition(sourceIdentity, targetIdentity)
        }
    }

    private fun clearPreparedQueueNextInternal() {
        preparedQueueNext = null
        cancelServicePrefetch()
    }

    private fun clearPreparedQueueNextIfStaleInternal() {
        val currentIdentity = queueEngine.state.value.currentTrack?.let(::playbackQueueIdentity)
        val nextIdentity = queueEngine.upcoming(1).firstOrNull()?.let(::playbackQueueIdentity)
        if (currentIdentity == null || nextIdentity == null) {
            clearPreparedQueueNextInternal()
            return
        }
        val now = SystemClock.elapsedRealtime()
        preparedQueueNext = preparedQueueNext
            ?.takeIf { it.resolved.streamUrl.isNotBlank() }
            ?.takeIf { prepared ->
                queuePrecacheMatchesTarget(
                    preparedTargetIdentity = prepared.targetIdentity,
                    requestedTargetIdentity = nextIdentity,
                    preparedAtElapsedMs = prepared.preparedAtElapsedMs,
                    nowElapsedMs = now,
                    maxAgeMs = PREPARED_QUEUE_MAX_AGE_MS
                )
            }
            ?.copy(sourceIdentity = currentIdentity)
        if (servicePrefetchTargetIdentity != nextIdentity) cancelServicePrefetch()
    }

    private fun consumePreparedQueueNextInternal(trackId: String) {
        val prepared = preparedQueueNext ?: return
        if (!queuePrecacheMatchesTrackId(prepared.targetTrackId, prepared.resolved.id, trackId)) return
        preparedQueueNext = null
        if (servicePrefetchTargetIdentity == prepared.targetIdentity) cancelServicePrefetch()
    }

    private fun cancelServicePrefetch() {
        servicePrefetchRequestToken++
        servicePrefetchJob?.cancel()
        servicePrefetchJob = null
        servicePrefetchTargetIdentity = null
    }

    private suspend fun resolveQueueTrackPersistently(
        track: com.yasuda.derbis.domain.Track
    ): com.yasuda.derbis.domain.Track? {
        var attempts = 0
        while (isPlaybackRecoveryExpected() && !isStickyRestoreExpired()) {
            if (!isLocalPlaybackTrack(track) && !hasInternetCapableNetwork()) {
                releasePlaybackWakeLock()
                Timber.d("Background queue auto-advance waiting for network")
                delay(5_000L)
                attempts = 0
                continue
            }
            acquirePlaybackWakeLock()
            val resolved = runCatching { resolveQueueTrack(track) }
                .onFailure { Timber.w(it, "Background queue resolution failed") }
                .getOrNull()
            if (resolved != null) return resolved
            if (attempts >= ONLINE_RECOVERY_DELAYS_MS.lastIndex) return null
            delay(ONLINE_RECOVERY_DELAYS_MS[attempts])
            attempts++
        }
        return null
    }

    private suspend fun resolveQueueTrack(track: com.yasuda.derbis.domain.Track): com.yasuda.derbis.domain.Track {
        if (track.isPersonalCollectionTrack()) return resolver.resolve(track)
        if (isLocalPlaybackTrack(track)) {
            if (!isLocalPlaybackUri(track.streamUrl)) {
                throw IOException("File offline non disponibile per ${track.title}")
            }
            return track.copy(videoStreamUrl = "")
        }
        val hasYoutubeIdentity = track.videoUrl.contains("youtube.com", true) ||
            track.videoUrl.contains("youtu.be", true) ||
            Regex("^[A-Za-z0-9_-]{11}$").matches(track.id)
        val candidate = if (hasYoutubeIdentity) {
            track
        } else {
            if (!hasInternetCapableNetwork()) throw IOException("Connessione Internet non disponibile")
            val match = musicRepository.searchSongMatch(track.title, track.artist, DerbisPreferences(this).languageCode())
            match?.copy(
                title = track.title.ifBlank { match.title },
                artist = track.artist.ifBlank { match.artist },
                album = track.album.ifBlank { match.album },
                thumbnailUrl = track.thumbnailUrl.ifBlank { match.thumbnailUrl },
                largeThumbnailUrl = track.largeThumbnailUrl.ifBlank { match.largeThumbnailUrl },
                accentStart = track.accentStart,
                accentEnd = track.accentEnd
            ) ?: track
        }
        return resolver.resolve(candidate)
    }

    private fun updateQueueTransitionSettings(settings: DerbisAudioSettings, audioNormalization: Boolean) {
        currentAudioSettings = settings.normalized()
        currentAudioNormalization = audioNormalization
        transitionPlayer?.setPlaybackParameters(
            PlaybackParameters(currentAudioSettings.playbackSpeed, currentAudioSettings.pitch)
        )
        if (currentAudioSettings.crossfadeSeconds <= 0 || !currentAudioSettings.gaplessEnabled) {
            cancelQueueTransition()
        }
    }

    private fun startQueueTransitionMonitor(player: ExoPlayer) {
        queueTransitionMonitorJob?.cancel()
        queueTransitionMonitorJob = serviceScope.launch {
            while (isActive) {
                maybePrepareQueueTransition(player)
                delay(250L)
            }
        }
    }

    private fun maybePrepareQueueTransition(player: ExoPlayer) {
        if (isLiveRadioMediaItem(player.currentMediaItem)) return
        if (sleepTimer.isEndOfTrackActive()) return
        if (!player.isPlaying || player.playbackState != Player.STATE_READY) return
        val duration = player.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: return
        val position = player.currentPosition

        // Next-Song Stream Preloading (ArchiveTune sliding-window trigger):
        // Proactively caches 1.5 MB of the upcoming song into Media3 cache before track ends
        if (currentAudioSettings.preloadNextTrack) {
            val nextUpcoming = queueEngine.upcoming(1).firstOrNull()
            if (nextUpcoming != null && !isLocalPlaybackTrack(nextUpcoming)) {
                val nextIdentity = playbackQueueIdentity(nextUpcoming)
                val now = SystemClock.elapsedRealtime()
                val isAlreadyPreloaded = preparedQueueNext?.let { prepared ->
                    prepared.targetIdentity == nextIdentity &&
                        prepared.streamPreloaded &&
                        isQueuePrecacheFresh(prepared.preparedAtElapsedMs, now, PREPARED_QUEUE_MAX_AGE_MS)
                } == true
                if (shouldTriggerStreamPreload(duration, position, true, isAlreadyPreloaded, STREAM_PRELOAD_TRIGGER_REMAINING_MS)) {
                    prepareQueueNextInternal(nextUpcoming, deepStreamPreload = true)
                }
            }
        }

        if (queueTransitionJob?.isActive == true) return
        val snapshot = queueEngine.state.value
        val current = snapshot.currentTrack ?: return
        val nextIndex = queueTransitionTargetIndex(snapshot) ?: return
        val next = snapshot.tracks.getOrNull(nextIndex) ?: return
        val videoMode = player.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true
        val mixStudioSetting = mixStudioStore.activeTransitionFor(snapshot.tracks, current, next)
        // A beta custom timeline can begin before Music 1's natural tail. Begin preparation only
        // shortly before that selected point; normal crossfades still prepare near the track end.
        val handoffAtMs = mixStudioSetting?.outgoingStartMs
            ?.coerceIn(0L, (duration - DerbisMixStudio.MIN_TRANSITION_MS).coerceAtLeast(0L))
            ?: duration
        val remaining = handoffAtMs - position
        if (remaining !in 1L..MAX_TRANSITION_LOOKAHEAD_MS) return
        val plan = planAutoMix(
            current = current.copy(durationMs = duration),
            next = next,
            settings = currentAudioSettings,
            repeatMode = snapshot.repeatMode,
            videoMode = videoMode,
            lowRam = adaptivePlaybackPolicy.current(videoMode = false).lowRam,
            shuffleEnabled = snapshot.shuffleEnabled,
            mixStudioSetting = mixStudioSetting,
            currentBeatGrid = mixStudioSetting?.takeIf { currentAudioSettings.beatSyncEnabled && it.beatSyncEnabled }?.let { beatGridStore.gridFor(current.id) },
            nextBeatGrid = mixStudioSetting?.takeIf { currentAudioSettings.beatSyncEnabled && it.beatSyncEnabled }?.let { beatGridStore.gridFor(next.id) }
        ) ?: return
        if (remaining > plan.preloadLeadMs) return
        queueTransitionJob = serviceScope.launch {
            runQueueTransition(player, snapshot, current, next, nextIndex, plan)
        }
    }

    private suspend fun runQueueTransition(
        primary: ExoPlayer,
        snapshot: PlaybackQueueSnapshot,
        current: Track,
        target: Track,
        targetQueueIndex: Int,
        plan: AutoMixPlan
    ) {
        val currentIdentity = playbackQueueIdentity(current)
        val targetIdentity = playbackQueueIdentity(target)
        var secondary: ExoPlayer? = null
        var handoffCommitted = false
        var incomingPromoted = false
        try {
            val resolved = awaitPreparedQueueTrackForTransition(currentIdentity, targetIdentity)
                ?: withContext(Dispatchers.IO) { resolveQueueTrack(target) }
            if (!transitionStillValid(snapshot.generation, currentIdentity, targetIdentity, primary)) return
            secondary = prepareTransitionPlayerWithDecoderFallback(
                track = resolved,
                queueSnapshot = snapshot,
                queueIndex = targetQueueIndex
            ) {
                transitionStillValid(snapshot.generation, currentIdentity, targetIdentity, primary)
            } ?: return

            val transitionStartMs = plan.transitionStartMs
                ?: (primary.duration - plan.transitionMs).coerceAtLeast(0L)
            // Poll tightly: a beat is ~430-600 ms at club tempo, so a coarse poll would start the
            // incoming deck visibly off the beat the grid just aligned to.
            while (primary.duration > 0L && primary.currentPosition < transitionStartMs) {
                if (!transitionStillValid(snapshot.generation, currentIdentity, targetIdentity, primary)) return
                delay(20L)
            }
            if (!transitionStillValid(snapshot.generation, currentIdentity, targetIdentity, primary)) return

            isQueueTransitionInProgress = true
            if (plan.beatAligned) {
                secondary.seekTo(plan.incomingStartMs)
                secondary.setPlaybackParameters(
                    PlaybackParameters(
                        (currentAudioSettings.playbackSpeed * plan.incomingTempo).coerceIn(.25f, 4f),
                        currentAudioSettings.pitch
                    )
                )
            }

            // The incoming deck is now playing. Advance the persistent queue before the fade so
            // every player surface, the queue highlight, and the saved position identify the
            // song the listener has just started hearing—not the outgoing deck's final seconds.
            secondary.play()
            val handedOff = queueEngine.handoffNext(
                expectedGeneration = snapshot.generation,
                expectedCurrentIdentity = currentIdentity,
                expectedNextIdentity = targetIdentity,
                resolved = resolved
            )
            if (handedOff == null) {
                Timber.w("Crossfade queue changed before incoming-audio handoff")
                return
            }
            handoffCommitted = true
            consumePreparedQueueNextInternal(target.id.ifBlank { resolved.id })
            _crossfadeProgressFlow.value = CrossfadeProgress(
                active = true,
                fromTitle = current.title,
                toTitle = handedOff.title,
                toQueueIdentity = targetIdentity,
                incomingPositionMs = secondary.currentPosition.coerceAtLeast(0L),
                incomingBufferedPositionMs = secondary.bufferedPosition.coerceAtLeast(0L)
            )
            fadePlayers(primary, secondary, plan.transitionMs, plan.curve, onProgress = { fraction ->
                _crossfadeProgressFlow.value = CrossfadeProgress(
                    active = true,
                    fraction = fraction,
                    fromTitle = current.title,
                    toTitle = handedOff.title,
                    toQueueIdentity = targetIdentity,
                    incomingPositionMs = secondary.currentPosition.coerceAtLeast(0L),
                    incomingBufferedPositionMs = secondary.bufferedPosition.coerceAtLeast(0L)
                )
            }) {
                crossfadeHandoffStillValid(primary, secondary, targetIdentity) &&
                    primary.currentPosition + SEEK_TOLERANCE_MS >= transitionStartMs
            }

            // The old implementation rebuilt this already-audible deck back into the original
            // ExoPlayer, then tried to synchronize and fade a second time. That was the audible
            // stop/restart chase. Promote the pre-warmed incoming deck instead: it has the exact
            // decoder, buffer and media clock the listener is already hearing.
            incomingPromoted = promoteIncomingCrossfadeDeck(outgoing = primary, incoming = secondary)
            if (!incomingPromoted) return

            // A beat-matched deck runs at a corrected speed. Glide back to the listener's own
            // speed once the outgoing deck is gone, or the new song would play fast forever.
            if (plan.beatAligned) {
                serviceScope.launch { restorePlaybackSpeedAfterBeatSync(plan.incomingTempo) }
            }

            queueEngine.updatePosition(secondary.currentPosition.coerceAtLeast(0L))
            if (!isLocalPlaybackTrack(resolved)) prefetchServiceQueueNext()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Queue crossfade failed")
        } finally {
            // STATE_ENDED is delivered asynchronously. Keep both the service and UI controller
            // from interpreting the outgoing deck's late callback as a second queue advance.
            if (handoffCommitted) armCrossfadeCompletionGuard()
            isQueueTransitionInProgress = false
            _crossfadeProgressFlow.value = CrossfadeProgress()
            if (!incomingPromoted) {
                primary.volume = 1f
                releaseTransitionPlayer(secondary)
            }
        }
    }

    /**
     * Eases the promoted deck back to the normal playback speed after a beat-matched handoff.
     * The correction is tapered instead of snapped so the tail of the blend does not jump.
     */
    private suspend fun restorePlaybackSpeedAfterBeatSync(appliedRatio: Float) {
        val base = currentAudioSettings
        val target = base.playbackSpeed
        val applied = (target * appliedRatio).coerceIn(.25f, 4f)
        if (kotlin.math.abs(applied - target) < .0005f) return
        val steps = 12
        repeat(steps) { index ->
            if (currentAudioSettings.playbackSpeed != target) return
            val progress = (index + 1) / steps.toFloat()
            val speed = applied + (target - applied) * progress
            activePlayer?.setPlaybackParameters(PlaybackParameters(speed, base.pitch))
            delay(90L)
        }
        if (currentAudioSettings.playbackSpeed == target) {
            activePlayer?.setPlaybackParameters(PlaybackParameters(target, base.pitch))
        }
    }

    /**
     * Makes the incoming pre-warmed deck the long-lived deck. There is intentionally no
     * setMediaItem/prepare/seek on it here: touching its stream after it has become audible is
     * exactly what caused the old stop-then-chase handoff.
     */
    private fun promoteIncomingCrossfadeDeck(outgoing: ExoPlayer, incoming: ExoPlayer): Boolean {
        if (transitionPlayer !== incoming) {
            Timber.w("Crossfade promotion ignored because the incoming deck was replaced")
            return false
        }

        // It is no longer a disposable transition resource. From this point onward this is the
        // one deck used for normal playback, recovery, queue monitoring and UI position.
        transitionPlayer = null
        transitionNormalization = null
        transitionEightDAudioProcessor = null
        activePlayer = incoming
        incoming.volume = 1f
        updatePlayerWakeMode(incoming, incoming.currentMediaItem)
        applyPlaybackTrackSelection(incoming, incoming.currentMediaItem)
        attachPromotedDeckRuntime(incoming)

        // Move MediaSession to the deck that is already producing the incoming audio before the
        // old deck is torn down. The second call wraps that same deck for Cast; neither call
        // changes the incoming ExoPlayer's stream, position or playWhenReady.
        setMediaSessionDeck(incoming)
        val transport = bindRemoteSessionPlayer(incoming)
        setMediaSessionDeck(transport)

        runCatching { outgoing.pause() }
            .onFailure { Timber.w(it, "Crossfade outgoing deck pause failed") }
        runCatching { outgoing.clearMediaItems() }
            .onFailure { Timber.w(it, "Crossfade outgoing deck clear failed") }
        runCatching { outgoing.release() }
            .onFailure { Timber.w(it, "Crossfade outgoing deck release failed") }
        return true
    }

    /** Attaches the service responsibilities that previously belonged to the first ExoPlayer. */
    private fun attachPromotedDeckRuntime(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (activePlayer !== player) return
                if (mediaItem?.mediaId != lastTransitionMediaId) {
                    lastTransitionMediaId = mediaItem?.mediaId
                    VideoDecoderFallback.Shared.reset()
                    _liveRadioMetadataFlow.value = LiveRadioStreamMetadata()
                    liveRadioPausedAtMs = C.TIME_UNSET
                    liveRadioReconnectJob?.cancel()
                }
                updateRomMediaGeneration(mediaItem)
                updatePlayerWakeMode(player, mediaItem)
                applyPlaybackTrackSelection(player, mediaItem)
                watchdogPositionMs = C.TIME_UNSET
                watchdogAdvancedAtMs = SystemClock.elapsedRealtime()
                if (!isLocalMediaItem(mediaItem) && !isLiveRadioMediaItem(mediaItem)) {
                    prefetchServiceQueueNext()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (activePlayer !== player) return
                if (playbackState == Player.STATE_READY && player.playWhenReady) {
                    playbackFailureGuard.onHealthyPlayback()
                }
                if (playbackState != Player.STATE_ENDED || playbackPausedByUser) return
                if (isLiveRadioMediaItem(player.currentMediaItem)) {
                    markPlaybackExpected(false, force = true)
                    releasePlaybackWakeLock()
                    return
                }
                if (sleepTimer.consumeEndOfTrackBoundary()) {
                    pausePlaybackForSleepTimer()
                    return
                }
                if (DerbisWidgetBridge.onNext == null && !isQueueTransitionCompletionSuppressed) {
                    skipQueue(forward = true, respectRepeatOne = true, autoAdvance = true)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (activePlayer !== player) return
                updatePlaybackProtection(player)
                discardIncompatiblePlaybackCache(error)
                val failureKind = classifyPlaybackFailureReason(playbackFailureReasonOf(error))
                if (isLiveRadioMediaItem(player.currentMediaItem)) {
                    markPlaybackExpected(false, force = true)
                    releasePlaybackWakeLock()
                } else if (isTerminalPlaybackFailure(failureKind)) {
                    serviceRecoveryExhausted = true
                    markPlaybackExpected(false, force = true)
                    releasePlaybackWakeLock()
                    skipUnrecoverableTrack()
                } else {
                    scheduleServiceRecovery(error)
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (activePlayer !== player) return
                if (playWhenReady && playbackPausedByUser) {
                    runCatching { player.pause() }
                        .onFailure { Timber.w(it, "Promoted deck restart could not be stopped") }
                    return
                }
                if (isLiveRadioMediaItem(player.currentMediaItem)) {
                    if (playWhenReady) reconnectStaleLiveRadio(player) else liveRadioPausedAtMs = SystemClock.elapsedRealtime()
                }
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) {
                    lostRouteWasBluetooth = lostRouteWasBluetooth || routedOutputIsBluetooth
                    pausedByRouteLossAtMs = SystemClock.elapsedRealtime()
                } else {
                    lostRouteWasBluetooth = false
                    pausedByRouteLossAtMs = null
                }
                if (!playWhenReady) cancelPendingPlaybackAfterPause()
                if (playWhenReady && serviceRecoveryExhausted) {
                    serviceRecoveryExhausted = false
                    serviceRecoveryAttempts = 0
                    markPlaybackExpected(true, force = true)
                }
            }

            override fun onEvents(changedPlayer: Player, events: Player.Events) {
                if (activePlayer !== player) return
                updatePlaybackProtection(changedPlayer)
                deviceIntegration?.onPlayerEvents(events)
            }
        })
        startPlaybackWatchdog(player)
        startQueueTransitionMonitor(player)
        startMemoryGuard(player)
    }

    private suspend fun prepareTransitionPlayerWithDecoderFallback(
        track: Track,
        queueSnapshot: PlaybackQueueSnapshot,
        queueIndex: Int,
        transitionIsValid: () -> Boolean
    ): ExoPlayer? {
        for (attempt in 0 until 2) {
            val candidate = buildTransitionPlayer(track, queueSnapshot, queueIndex).also { transitionPlayer = it }
            candidate.setPlaybackParameters(
                PlaybackParameters(currentAudioSettings.playbackSpeed, currentAudioSettings.pitch)
            )
            candidate.volume = 0f
            candidate.setMediaItem(DerbisMediaItemFactory.build(track))
            RuntimeHooks.player(RuntimeSignal.PLAYER_PREPARE)
            RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_PREPARE)
            candidate.prepare()

            val prepared = withTimeoutOrNull(TRANSITION_PREPARE_TIMEOUT_MS) {
                while (candidate.playbackState != Player.STATE_READY) {
                    if (!transitionIsValid()) return@withTimeoutOrNull false
                    if (candidate.playerError != null) return@withTimeoutOrNull false
                    delay(50L)
                }
                true
            } == true
            if (prepared) return candidate

            val decoderError = candidate.playerError
            releaseTransitionPlayer(candidate)
            if (
                attempt == 0 &&
                decoderError != null &&
                NativeAudioIntegration.redirectFailedBackgroundDecoder(decoderError) != null
            ) {
                continue
            }
            return null
        }
        return null
    }

    private suspend fun awaitPrimaryHandoffReady(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        targetIdentity: String,
        title: String
    ): Boolean {
        val startedAt = SystemClock.elapsedRealtime()
        var warned = false
        while (true) {
            if (!handoffStillValid(primary, secondary, targetIdentity)) return false
            if (primary.playbackState == Player.STATE_READY && primary.playerError == null) return true
            if (secondary.playbackState == Player.STATE_ENDED || secondary.playerError != null) return false
            if (!warned && SystemClock.elapsedRealtime() - startedAt >= PRIMARY_HANDOFF_WARN_MS) {
                warned = true
                Timber.w("Crossfade handoff still preparing primary player for %s", title)
            }
            delay(40L)
        }
    }

    private suspend fun synchronizePrimaryHandoff(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        targetIdentity: String
    ): Boolean {
        // The secondary deck is already audible. Keep it alive while the muted primary retries
        // synchronization instead of giving up after a few attempts and cutting playback short.
        while (true) {
            if (!handoffStillValid(primary, secondary, targetIdentity)) return false
            if (primary.playerError != null || secondary.playerError != null || secondary.playbackState == Player.STATE_ENDED) {
                return false
            }
            if (!awaitHandoffSecondaryReady(primary, secondary, targetIdentity)) {
                delay(20L)
                continue
            }
            if (!crossfadeHandoffNeedsResync(
                    primary.currentPosition,
                    secondary.currentPosition,
                    PRIMARY_HANDOFF_SYNC_TOLERANCE_MS
                )
            ) return true

            if (resyncPrimaryHandoff(primary, secondary, targetIdentity)) return true
            delay(20L)
        }
    }

    private suspend fun awaitHandoffSecondaryReady(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        targetIdentity: String
    ): Boolean {
        if (crossfadeHandoffCanResync(secondary.playbackState)) return true
        return withTimeoutOrNull(PRIMARY_HANDOFF_SYNC_TIMEOUT_MS) {
            while (!crossfadeHandoffCanResync(secondary.playbackState)) {
                if (!handoffStillValid(primary, secondary, targetIdentity)) return@withTimeoutOrNull false
                delay(30L)
            }
            true
        } == true
    }

    private suspend fun resyncPrimaryHandoff(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        targetIdentity: String
    ): Boolean {
        primary.seekTo(
            crossfadeHandoffSeekPosition(
                secondaryPositionMs = secondary.currentPosition,
                durationMs = primary.duration,
                leadMs = PRIMARY_HANDOFF_SYNC_LEAD_MS
            )
        )
        return withTimeoutOrNull(PRIMARY_HANDOFF_SYNC_TIMEOUT_MS) {
            while (true) {
                if (!handoffStillValid(primary, secondary, targetIdentity)) return@withTimeoutOrNull false
                if (primary.playerError != null) return@withTimeoutOrNull false
                if (primary.playbackState == Player.STATE_READY &&
                    !crossfadeHandoffNeedsResync(
                        primary.currentPosition,
                        secondary.currentPosition,
                        PRIMARY_HANDOFF_SYNC_TOLERANCE_MS
                    )
                ) return@withTimeoutOrNull true
                delay(30L)
            }
        } == true
    }

    private fun crossfadeHandoffStillValid(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        targetIdentity: String
    ): Boolean {
        val current = queueEngine.state.value
        return !playbackPausedByUser &&
            playbackQueueIdentity(current.currentTrack ?: return false) == targetIdentity &&
            !sleepTimer.isEndOfTrackActive() &&
            primary.playWhenReady &&
            secondary.playWhenReady &&
            primary.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) != true
    }

    private fun handoffStillValid(
        primary: ExoPlayer,
        secondary: ExoPlayer,
        targetIdentity: String
    ): Boolean {
        val current = queueEngine.state.value
        return !playbackPausedByUser &&
            playbackQueueIdentity(current.currentTrack ?: return false) == targetIdentity &&
            primary.playWhenReady &&
            secondary.playWhenReady &&
            primary.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) != true
    }

    private fun buildTransitionPlayer(
        track: Track,
        queueSnapshot: PlaybackQueueSnapshot,
        queueIndex: Int
    ): ExoPlayer {
        val normalization = NormalizationAudioProcessor().apply {
            enabled = currentAudioNormalization || currentAudioSettings.replayGainActive
            configureNormalizationProcessor(
                processor = this,
                settings = currentAudioSettings,
                youtubeLoudnessDb = track.youtubeLoudnessDb,
                youtubePerceptualLoudnessDb = track.youtubePerceptualLoudnessDb,
                replayGain = ReplayGainMetadata(
                    trackGainDb = track.replayGainTrackDb,
                    albumGainDb = track.replayGainAlbumDb,
                    trackPeak = track.replayGainTrackPeak,
                    albumPeak = track.replayGainAlbumPeak
                ),
                albumContext = replayGainAlbumContext(
                    snapshot = queueSnapshot,
                    queueIndex = queueIndex,
                    track = track
                )
            )
        }
        transitionNormalization = normalization
        val parametricActive = currentAudioSettings.parametricEqualizerEnabled &&
            currentAudioSettings.activeParametricProfile != null
        val equalizer = DerbisEqualizerAudioProcessor().apply {
            enabled = currentAudioSettings.equalizerEnabled && !parametricActive
            setBandLevels(currentAudioSettings.bandLevels)
            bassBoost = currentAudioSettings.bassBoost
            preampDb = currentAudioSettings.preampDb
            outputProfile = equalizerProcessor.outputProfile
        }
        val parametricEqualizer = DerbisParametricEqualizerAudioProcessor().apply {
            setConfiguration(parametricActive, currentAudioSettings.activeParametricProfile)
        }
        val spatial = StereoSpatialAudioProcessor().apply {
            strength = currentAudioSettings.virtualizer
        }
        val eightD = EightDAudioProcessor().apply {
            enabled = currentAudioSettings.eightDAudioEnabled
            experience = currentAudioSettings.derbis360Experience
            headphoneOutputAvailable = eightDAudioProcessor.headphoneOutputAvailable
        }
        transitionEightDAudioProcessor = eightD
        val limiter = TruePeakLimiterAudioProcessor().apply {
            enabled = truePeakLimiterRequired(currentAudioSettings, parametricActive, currentAudioNormalization)
        }
        val enhancedAudio = EnhancedAudioProcessor().apply {
            userEnabled = currentAudioSettings.enhancedAudioEnabled
            isRemotePlayback = remotePlaybackStateFlow.value.connected
        }
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<Renderer>
            ) = Unit

            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false)
                .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                .setAudioProcessors(
                    arrayOf(
                        enhancedAudio,
                        normalization,
                        equalizer,
                        parametricEqualizer,
                        spatial,
                        eightD,
                        limiter,
                        Pcm16OutputAudioProcessor()
                    )
                )
                .withDerbisAudioOutput(context)
                .build()
        }.apply {
            setEnableDecoderFallback(true)
            setMediaCodecSelector(NativeAudioIntegration.mediaCodecSelector)
            setExtensionRendererMode(NativeAudioIntegration.EXTENSION_RENDERER_MODE)
        }
        val builder = ExoPlayer.Builder(this)
            .setRenderersFactory(renderers)
            .setMediaSourceFactory(sharedMediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false
            )
            .setHandleAudioBecomingNoisy(false)
        activeCustomBufferProfile?.let { profile ->
            builder.setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        profile.minBufferMs,
                        profile.maxBufferMs,
                        transitionPlaybackBufferMs(profile.playbackBufferMs),
                        profile.rebufferMs
                    )
                    .setBackBuffer(profile.backBufferMs, false)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
        }
        return builder
            .build()
            .also { transitionPlayer ->
                preferredAudioRouteKey
                    ?.let { findDerbisAudioOutputDevice(audioManager, it) }
                    ?.let(transitionPlayer::setPreferredAudioDevice)
                transitionPlayer.addAnalyticsListener(EnhancedAudioSourceFormatListener(enhancedAudio, "transition"))
                RuntimeHooks.attachPlayer(transitionPlayer)
            }
    }

    private suspend fun fadePlayers(
        outgoing: ExoPlayer,
        incoming: ExoPlayer,
        durationMs: Long,
        curve: CrossfadeCurve = CrossfadeCurve.EqualPower,
        onProgress: (Float) -> Unit = {},
        isValid: () -> Boolean = { true }
    ) {
        val steps = (durationMs / TRANSITION_STEP_MS).toInt().coerceIn(8, 120)
        val mediaStepMs = (durationMs / steps).coerceAtLeast(10L)
        repeat(steps + 1) { step ->
            if ((!outgoing.playWhenReady || !incoming.playWhenReady || !isValid()) && step < steps) {
                throw CancellationException("Playback paused during crossfade")
            }
            val fraction = step.toFloat() / steps.toFloat()
            onProgress(fraction)
            val gains = equalPowerCrossfade(fraction, curve)
            outgoing.volume = gains.outgoing
            incoming.volume = gains.incoming
            if (step < steps) {
                val stepDelay = crossfadeStepWallClockMs(mediaStepMs, outgoing.playbackParameters.speed)
                delay(stepDelay)
            }
        }
    }

    private fun transitionStillValid(
        generation: Long,
        currentIdentity: String,
        targetIdentity: String,
        player: ExoPlayer
    ): Boolean {
        val current = queueEngine.state.value
        return !playbackPausedByUser &&
            current.generation == generation &&
            playbackQueueIdentity(current.currentTrack ?: return false) == currentIdentity &&
            current.repeatMode != com.yasuda.derbis.domain.RepeatMode.One &&
            !sleepTimer.isEndOfTrackActive() &&
            queueEngine.upcoming(1).firstOrNull()?.let(::playbackQueueIdentity) == targetIdentity &&
            player.playWhenReady &&
            player.currentMediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) != true
    }

    private fun applyPlaybackTrackSelection(player: ExoPlayer, mediaItem: MediaItem?) {
        val videoMode = mediaItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true
        val disableVideo = PlaybackTrackSelectionPolicy.disableVideoTracks(videoMode)
        runCatching {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, disableVideo)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .setPreferredTextLanguage(null)
                .build()
        }.onFailure { Timber.w(it, "Track selection update failed") }
    }

    private fun applyVideoSubtitleSelection(player: ExoPlayer, subtitleId: String) {
        val videoMode = player.currentMediaItem?.mediaMetadata?.extras
            ?.getBoolean(EXTRA_VIDEO_MODE, false) == true
        val selection = subtitleId.takeIf { videoMode && it.isNotBlank() }?.let { requestedId ->
            player.currentTracks.groups.firstNotNullOfOrNull { group ->
                if (group.type != C.TRACK_TYPE_TEXT) return@firstNotNullOfOrNull null
                val index = (0 until group.length).firstOrNull { group.getTrackFormat(it).id == requestedId }
                    ?: return@firstNotNullOfOrNull null
                TrackSelectionOverride(group.mediaTrackGroup, listOf(index))
            }
        }
        runCatching {
            val builder = player.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, selection == null)
                .setPreferredTextLanguage(null)
            if (selection != null) builder.setOverrideForType(selection)
            player.trackSelectionParameters = builder.build()
        }.onFailure { Timber.w(it, "Subtitle track selection update failed") }
    }

    private fun startMemoryGuard(player: ExoPlayer) {
        memoryGuardJob?.cancel()
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val threshold = PlaybackMemoryGuardPolicy.thresholdBytes(
            totalDeviceMemoryBytes = memoryInfo.totalMem,
            lowRamDevice = activityManager.isLowRamDevice
        )
        memoryGuardJob = serviceScope.launch {
            while (isActive) {
                delay(PlaybackMemoryGuardPolicy.SAMPLE_INTERVAL_MS)
                if (!player.isPlaying) {
                    memoryGuardHighSamples = 0
                    continue
                }
                val nativeAllocatedBytes = withContext(Dispatchers.Default) { Debug.getNativeHeapAllocatedSize() }
                if (activePlayer !== player || !player.isPlaying) {
                    memoryGuardHighSamples = 0
                    continue
                }
                memoryGuardHighSamples = PlaybackMemoryGuardPolicy.nextHighSampleCount(
                    current = memoryGuardHighSamples,
                    nativeAllocatedBytes = nativeAllocatedBytes,
                    thresholdBytes = threshold
                )
                val now = SystemClock.elapsedRealtime()
                if (
                    PlaybackMemoryGuardPolicy.shouldRecycle(
                        highSamples = memoryGuardHighSamples,
                        nowElapsedMs = now,
                        lastRecycleElapsedMs = lastMemoryRecycleElapsedMs
                    )
                ) {
                    memoryGuardHighSamples = 0
                    lastMemoryRecycleElapsedMs = now
                    recyclePlaybackPipeline(player, threshold, nativeAllocatedBytes)
                }
            }
        }
    }

    private fun recyclePlaybackPipeline(
        player: ExoPlayer,
        thresholdBytes: Long,
        nativeAllocatedBytes: Long
    ) {
        val item = player.currentMediaItem ?: return
        val position = player.currentPosition.coerceAtLeast(0L)
        val resumePlayback = player.playWhenReady
        Timber.w(
            "Native playback memory %d bytes above %d, recycling playback pipeline",
            nativeAllocatedBytes,
            thresholdBytes
        )
        cancelQueueTransition()
        clearPreparedQueueNextInternal()
        runCatching { player.stop() }.onFailure { Timber.w(it, "Memory guard stop failed") }
        runCatching { player.clearMediaItems() }.onFailure { Timber.w(it, "Memory guard clear failed") }
        runCatching {
            player.setMediaItem(item, position)
            RuntimeHooks.player(
                action = RuntimeSignal.PLAYER_PREPARE,
                mode = if (item.mediaMetadata.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true) {
                    RuntimeSignal.MODE_VIDEO
                } else {
                    RuntimeSignal.MODE_AUDIO
                }
            )
            RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_PREPARE)
            player.prepare()
            player.playWhenReady = resumePlayback
        }.onFailure { Timber.w(it, "Memory guard playback restore failed") }
    }

    private fun cancelQueueTransition() {
        queueTransitionJob?.cancel()
        queueTransitionJob = null
        clearCrossfadeCompletionGuard()
        isQueueTransitionInProgress = false
        _crossfadeProgressFlow.value = CrossfadeProgress()
        activePlayer?.volume = 1f
        releaseTransitionPlayer()
    }

    /**
     * Cancels work that was allowed to begin while playback was active. This is deliberately
     * separate from the player calls so the primary-player listener can also use it for a raw
     * pause (for example an audio-focus pause) without recursively controlling the session.
     */
    private fun cancelPendingPlaybackAfterPause() {
        queueSkipJob?.cancel()
        queueSkipJob = null
        serviceRecoveryJob?.cancel()
        serviceRecoveryJob = null
        stickyRestoreJob?.cancel()
        stickyRestoreJob = null
        castHandoffJob?.cancel()
        castHandoffJob = null
        liveRadioReconnectJob?.cancel()
        liveRadioReconnectJob = null
        cancelQueueTransition()
        clearPreparedQueueNextInternal()
        markPlaybackExpected(false, force = true)
        releasePlaybackWakeLock()
    }

    /** Stops every deck that can output audio for one MediaSession pause command. */
    private fun pauseAllAudiblePlayback(
        target: Player? = null,
        explicitUserRequest: Boolean = false
    ) {
        if (explicitUserRequest) {
            playbackPausedByUser = true
            // A listener pause takes precedence over reconnect automation.
            lostRouteWasBluetooth = false
            pausedByRouteLossAtMs = null
        }
        cancelPendingPlaybackAfterPause()
        runCatching { target?.pause() }
            .onFailure { Timber.w(it, "Session playback pause failed") }
        activePlayer
            ?.takeIf { it !== target }
            ?.let { primary ->
                runCatching { primary.pause() }
                    .onFailure { Timber.w(it, "Primary playback pause failed") }
            }
    }

    private fun stopDerbisPlaybackForAppExit() {
        pauseAllAudiblePlayback(mediaSession?.player, explicitUserRequest = true)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun applySleepFadeVolume(volume: Float) {
        val player = mediaSession?.player ?: return
        if (volume >= 1f) {
            val baseline = sleepFadeBaselineVolume ?: return
            sleepFadeBaselineVolume = null
            runCatching { player.volume = baseline }
                .onFailure { Timber.w(it, "Sleep timer volume restore failed") }
            return
        }
        val baseline = sleepFadeBaselineVolume ?: player.volume.also { sleepFadeBaselineVolume = it }
        runCatching { player.volume = (baseline * volume).coerceIn(0f, 1f) }
            .onFailure { Timber.w(it, "Sleep timer fade failed") }
    }

    private fun resumeAfterRouteReconnect() {
        val player = mediaSession?.player ?: return
        refreshAudioOutputProfile()
        if (playbackPausedByUser || !lostRouteWasBluetooth || !routedOutputIsBluetooth) return
        val eligible = PlaybackAutomationPolicy.shouldResumeOnRouteReconnect(
            enabled = automationSettings.resumeOnBluetoothReconnect,
            pausedByRouteLossAtMs = pausedByRouteLossAtMs,
            nowMs = SystemClock.elapsedRealtime(),
            playerReady = player.playbackState == Player.STATE_READY,
            hasQueueItem = player.mediaItemCount > 0 && player.currentMediaItem != null,
            alreadyPlaying = player.playWhenReady
        )
        if (!eligible) return
        pausedByRouteLossAtMs = null
        lostRouteWasBluetooth = false
        runCatching { player.play() }.onFailure { Timber.w(it, "Bluetooth resume failed") }
    }

    private fun pausePlaybackIfMuted() {
        val player = mediaSession?.player ?: return
        val manager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val streamVolume = runCatching { manager.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(1)
        if (!PlaybackAutomationPolicy.shouldPauseForMute(automationSettings.pauseOnMute, streamVolume, player.isPlaying)) {
            return
        }
        pausedByRouteLossAtMs = null
        lostRouteWasBluetooth = false
        runCatching { player.pause() }.onFailure { Timber.w(it, "Mute pause failed") }
    }

    private fun skipUnrecoverableTrack() {
        val player = mediaSession?.player ?: return
        if (isLiveRadioMediaItem(player.currentMediaItem)) return
        val hasQueueItem = player.mediaItemCount > 0
        if (!playbackFailureGuard.shouldSkipAfterUnrecoverableError(
                enabled = automationSettings.skipUnrecoverableErrors,
                hasQueueItem = hasQueueItem
            )
        ) {
            return
        }
        Timber.w("Skipping track after unrecoverable playback error")
        skipQueue(forward = true, respectRepeatOne = false, autoAdvance = true) {
            serviceRecoveryExhausted = false
            serviceRecoveryAttempts = 0
            markPlaybackExpected(true, force = true)
        }
    }

    private fun updateDeviceVolumeReceiver(enabled: Boolean) {
        if (enabled == deviceVolumeReceiverRegistered) return
        if (enabled) {
            val filter = IntentFilter(VOLUME_CHANGED_ACTION)
            val registered = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(deviceVolumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(deviceVolumeReceiver, filter)
                }
            }.isSuccess
            deviceVolumeReceiverRegistered = registered
        } else {
            runCatching { unregisterReceiver(deviceVolumeReceiver) }
            deviceVolumeReceiverRegistered = false
        }
    }

    private fun pausePlaybackForSleepTimer() {
        pauseAllAudiblePlayback(mediaSession?.player, explicitUserRequest = true)
    }

    private fun releaseTransitionPlayer(expected: ExoPlayer? = null) {
        val player = transitionPlayer ?: return
        if (expected != null && player !== expected) return
        transitionPlayer = null
        transitionNormalization = null
        transitionEightDAudioProcessor = null
        runCatching { player.pause() }
            .onFailure { Timber.w(it, "Queue crossfade secondary pause failed") }
        runCatching { player.clearMediaItems() }
            .onFailure { Timber.w(it, "Queue crossfade secondary clear failed") }
        runCatching { player.release() }
            .onFailure { Timber.w(it, "Queue crossfade secondary release failed") }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (shouldCancelPrefetchForMemoryPressure(level)) {
            clearPreparedQueueNextInternal()
            cancelQueueTransition()
        }
    }

    private fun shouldCancelPrefetchForMemoryPressure(level: Int): Boolean =
        level == RUNNING_LOW_LEVEL ||
            level == RUNNING_CRITICAL_LEVEL ||
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND

    /** Keeps Discord activity aligned with the active media service; task removal clears it. */
    private fun publishDiscordRichPresence(player: Player? = mediaSession?.player ?: activePlayer) {
        val playbackDeck = player ?: return
        val currentTrack = queueEngine.state.value.currentTrack
        val duration = playbackDeck.duration.takeIf { it > 0L } ?: currentTrack?.durationMs ?: 0L
        val positionMs = playbackDeck.currentPosition
        DerbisDiscordRichPresence.publish(
            applicationContext,
            derbisDiscordPresenceSnapshot(
                track = currentTrack,
                positionMs = positionMs,
                durationMs = duration,
                playing = playbackDeck.isPlaying,
                lyricLine = discordLyricLineFor(currentTrack, positionMs)
            )
        )
    }

    /**
     * The lyric line to publish right now, or blank when the option is off, the track has no
     * synced lyrics, or the line is an instrumental/metadata marker rather than sung text.
     */
    private fun discordLyricLineFor(track: Track?, positionMs: Long): String {
        if (track == null) return ""
        if (!DerbisDiscordRichPresence.isLyricsEnabled(this)) return ""
        if (!discordLyricsSynced || discordLyricsTrackId != track.id) return ""
        val line = discordLyricsEngine.currentLine(
            positionMs = discordLyricPositionMs(positionMs, discordLyricsLatencyProfileOffset(track.id)),
            lines = discordLyricsLines
        ) ?: return ""
        if (line.isInstrumental || line.isMetadata) return ""
        return line.text
    }

    /**
     * The user's calibrated lyric delay for the active output route plus this track. Bluetooth
     * in particular lags the audio clock, so without this the presence line leads the song.
     */
    private fun discordLyricsLatencyProfileOffset(trackId: String): Long {
        val route = runCatching { DerbisAudioOutputRepository.state.value.active }.getOrNull()
        return runCatching {
            discordLyricsLatencyProfiles.effectiveOffsetMs(
                trackId = trackId,
                routeKey = route?.stableKey,
                bluetooth = route?.bluetooth == true
            )
        }.getOrDefault(0L)
    }

    /**
     * Republish as soon as the sung line changes. The presence layer coalesces these into
     * Discord's 5-per-20-seconds budget, so polling every playback tick stays safe.
     */
    private fun publishDiscordLyricLineIfChanged(player: Player) {
        val track = queueEngine.state.value.currentTrack
        val line = discordLyricLineFor(track, player.currentPosition)
        if (line == lastDiscordLyricLine) return
        lastDiscordLyricLine = line
        publishDiscordRichPresence(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Derbis follows the requested close policy: removing its task from Android's recents is
        // an explicit stop. Clear the activity, foreground notification, and every audible deck
        // instead of leaving a media service running without a visible Derbis task.
        DerbisDiscordRichPresence.clear()
        stopDerbisPlaybackForAppExit()
    }

    override fun onDestroy() {
        // Keep the logical incoming position if the service is torn down while both decks overlap.
        // cancelQueueTransition() clears the progress flow, so capture it before cancelling.
        val crossfadePositionAtDestruction = _crossfadeProgressFlow.value
            .takeIf { it.active }
            ?.incomingPositionMs
            ?.coerceAtLeast(0L)
        RuntimeHooks.dsp(RuntimeSignal.DSP_RELEASED)
        queueSkipJob?.cancel()
        clearPreparedQueueNextInternal()
        serviceRecoveryJob?.cancel()
        stickyRestoreJob?.cancel()
        playbackWatchdogJob?.cancel()
        cancelQueueTransition()
        memoryGuardJob?.cancel()
        castHandoffJob?.cancel()
        queueTransitionMonitorJob?.cancel()
        sleepTimerStateJob?.cancel()
        automationSettingsJob?.cancel()
        discordLyricsProfilesJob?.cancel()
        systemMediaActions?.detach()
        systemMediaActions = null
        studioWidgetSync.detach()
        deviceIntegration?.release()
        deviceIntegration = null
        pendingDeviceLyrics = null
        updateDeviceVolumeReceiver(false)
        sleepTimer.cancel()
        _sleepTimerStateFlow.value = PlaybackSleepTimerState.Disabled
        _liveRadioMetadataFlow.value = LiveRadioStreamMetadata()
        _remotePlaybackStateFlow.value = RemotePlaybackState()
        if (!appExitRequested) {
            val position = crossfadePositionAtDestruction
                ?: mediaSession?.player?.currentPosition?.coerceAtLeast(0L)
            if (position != null) queueEngine.updatePosition(position)
        }
        releasePlaybackWakeLock()
        synchronized(premiumAudioSettingsLock) {
            if (activeService === this) activeService = null
        }
        if (::autoLibrary.isInitialized) autoLibrary.close()
        serviceScope.cancel()
        mediaSession?.run {
            RuntimeHooks.player(RuntimeSignal.PLAYER_RELEASED)
            player.release()
            release()
        }
        activePlayer = null
        mediaSession = null
        runCatching {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        }
        runCatching { mediaRouter.removeCallback(mediaRouteCallback) }
        unregisterAudioRouteVolumeReceiver()
        DerbisAudioOutputRepository.reset()
        super.onDestroy()
    }

    private fun handoffQueueToCast(castPlayer: Player) {
        val snapshot = queueEngine.state.value
        val current = snapshot.currentTrack ?: return
        val handoff = CastHandoffConverter.toHandoff(
            LocalPlaybackSnapshot(
                queueIds = snapshot.tracks.map(::playbackQueueIdentity),
                currentIndex = snapshot.currentIndex,
                positionMs = castPlayer.currentPosition,
                playing = castPlayer.playWhenReady,
                shuffle = snapshot.shuffleEnabled,
                repeatMode = snapshot.repeatMode
            )
        )
        val generation = snapshot.generation
        val window = snapshot.tracks.subList(
            handoff.windowStartIndex,
            handoff.windowStartIndex + handoff.queueWindowIds.size
        )
        castHandoffJob?.cancel()
        castHandoffJob = serviceScope.launch(Dispatchers.IO) {
            val resolved = window.map { track -> resolver.resolve(track, isVideoMode = false) }
            val currentState = queueEngine.state.value
            if (currentState.generation != generation ||
                currentState.currentTrack?.let(::playbackQueueIdentity) != playbackQueueIdentity(current)
            ) return@launch
            withContext(Dispatchers.Main) {
                if (castPlayer.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE) return@withContext
                castPlayer.setMediaItems(resolved.map { DerbisMediaItemFactory.build(it) }, handoff.currentIndex, handoff.positionMs)
                castPlayer.shuffleModeEnabled = handoff.shuffle
                castPlayer.repeatMode = when (handoff.repeatMode) {
                    com.yasuda.derbis.domain.RepeatMode.One -> Player.REPEAT_MODE_ONE
                    com.yasuda.derbis.domain.RepeatMode.All -> Player.REPEAT_MODE_ALL
                    com.yasuda.derbis.domain.RepeatMode.Off -> Player.REPEAT_MODE_OFF
                }
                if (playbackPausedByUser) return@withContext
                castPlayer.prepare()
                if (handoff.playing) castPlayer.play()
            }
        }
    }

    private fun skipCastQueue(forward: Boolean, castPlayer: Player) {
        queueSkipJob?.cancel()
        queueSkipJob = serviceScope.launch(Dispatchers.IO) {
            val selected = if (forward) queueEngine.next(respectRepeatOne = false) else queueEngine.previous()
            if (selected != null) withContext(Dispatchers.Main) { handoffQueueToCast(castPlayer) }
        }
    }

    private fun refreshAudioOutputProfile() {
        if (!::audioManager.isInitialized) return
        val types = routedOutputTypes(audioManager)
        routedOutputIsBluetooth = types.any(::isBluetoothOutputType)
        val derbis360HeadphoneOutputAvailable = DerbisAudioOutputRepository.state.value.active
            ?.let(::isDerbis360HeadphoneOutput)
            ?: types.any(::isDerbis360HeadphoneOutputType)
        eightDAudioProcessor.headphoneOutputAvailable = derbis360HeadphoneOutputAvailable
        transitionEightDAudioProcessor?.headphoneOutputAvailable = derbis360HeadphoneOutputAvailable
        equalizerProcessor.outputProfile = when {
            types.any { it == AudioDeviceInfo.TYPE_USB_DEVICE || it == AudioDeviceInfo.TYPE_USB_HEADSET || it == AudioDeviceInfo.TYPE_USB_ACCESSORY } -> DerbisEqualizerAudioProcessor.OutputProfile.USB
            types.any { it == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it == AudioDeviceInfo.TYPE_WIRED_HEADSET || it == AudioDeviceInfo.TYPE_LINE_ANALOG } -> DerbisEqualizerAudioProcessor.OutputProfile.WIRED
            types.any(::isBluetoothOutputType) -> DerbisEqualizerAudioProcessor.OutputProfile.BLUETOOTH
            else -> DerbisEqualizerAudioProcessor.OutputProfile.SPEAKER
        }
    }

    private fun refreshAudioRouteCenterState() {
        if (!::audioManager.isInitialized || !::mediaRouter.isInitialized) return
        var snapshot = queryDerbisAudioOutputState(
            audioManager = audioManager,
            mediaRouter = mediaRouter,
            directSelectionAvailable = directAudioRouteSelectionAvailable(),
            requestedRouteKey = preferredAudioRouteKey,
            selectionState = audioRouteSelectionState
        )
        val requested = preferredAudioRouteKey
        if (requested != null && snapshot.connected.none { it.routeKey == requested }) {
            clearPreferredAudioOutput()
            audioRouteSelectionState = DerbisAudioRouteSelectionState.Failed
            scheduleAudioRouteFailureFeedbackReset()
            snapshot = queryDerbisAudioOutputState(
                audioManager,
                mediaRouter,
                directAudioRouteSelectionAvailable(),
                preferredAudioRouteKey,
                audioRouteSelectionState
            )
        } else if (requested != null && snapshot.active?.routeKey == requested) {
            audioRouteSelectionState = DerbisAudioRouteSelectionState.Idle
            snapshot = snapshot.copy(selectionState = audioRouteSelectionState)
        } else if (requested != null && audioRouteSelectionState == DerbisAudioRouteSelectionState.Idle) {
            clearPreferredAudioOutput()
            snapshot = queryDerbisAudioOutputState(
                audioManager,
                mediaRouter,
                directAudioRouteSelectionAvailable(),
                preferredAudioRouteKey,
                audioRouteSelectionState
            )
        }
        DerbisAudioOutputRepository.publish(snapshot)
        refreshAudioOutputProfile()
    }

    private suspend fun selectAudioOutput(routeKey: String?) {
        audioRouteFeedbackResetJob?.cancel()
        if (!directAudioRouteSelectionAvailable()) {
            audioRouteSelectionState = DerbisAudioRouteSelectionState.Failed
            scheduleAudioRouteFailureFeedbackReset()
            refreshAudioRouteCenterState()
            return
        }
        if (routeKey == null) {
            clearPreferredAudioOutput()
            audioRouteSelectionState = DerbisAudioRouteSelectionState.Idle
            refreshAudioRouteCenterState()
            return
        }
        val device = findDerbisAudioOutputDevice(audioManager, routeKey)
        if (device == null) {
            preferredAudioRouteKey = null
            audioRouteSelectionState = DerbisAudioRouteSelectionState.Failed
            scheduleAudioRouteFailureFeedbackReset()
            refreshAudioRouteCenterState()
            return
        }
        preferredAudioRouteKey = routeKey
        audioRouteSelectionState = DerbisAudioRouteSelectionState.Applying
        refreshAudioRouteCenterState()
        val applied = runCatching {
            activePlayer?.setPreferredAudioDevice(device)
            transitionPlayer?.setPreferredAudioDevice(device)
        }.onFailure { Timber.w(it, "Audio output preference failed") }.isSuccess
        if (!applied) {
            clearPreferredAudioOutput()
            audioRouteSelectionState = DerbisAudioRouteSelectionState.Failed
            scheduleAudioRouteFailureFeedbackReset()
            refreshAudioRouteCenterState()
            return
        }
        delay(AUDIO_ROUTE_SELECTION_VERIFY_MS)
        val activeRouteKey = queryDerbisAudioOutputState(
            audioManager,
            mediaRouter,
            directSelectionAvailable = true,
            requestedRouteKey = preferredAudioRouteKey,
            selectionState = audioRouteSelectionState
        ).active?.routeKey
        if (activeRouteKey == routeKey) {
            audioRouteSelectionState = DerbisAudioRouteSelectionState.Idle
        } else {
            clearPreferredAudioOutput()
            audioRouteSelectionState = DerbisAudioRouteSelectionState.Failed
            scheduleAudioRouteFailureFeedbackReset()
        }
        refreshAudioRouteCenterState()
    }

    private fun scheduleAudioRouteFailureFeedbackReset() {
        audioRouteFeedbackResetJob?.cancel()
        audioRouteFeedbackResetJob = serviceScope.launch {
            delay(AUDIO_ROUTE_FAILURE_FEEDBACK_MS)
            if (audioRouteSelectionState == DerbisAudioRouteSelectionState.Failed) {
                audioRouteSelectionState = DerbisAudioRouteSelectionState.Idle
                refreshAudioRouteCenterState()
            }
        }
    }

    private fun directAudioRouteSelectionAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activePlayer != null &&
            mediaSession?.player?.deviceInfo?.playbackType != DeviceInfo.PLAYBACK_TYPE_REMOTE

    private fun clearPreferredAudioOutput() {
        preferredAudioRouteKey = null
        runCatching { activePlayer?.setPreferredAudioDevice(null) }
            .onFailure { Timber.w(it, "Unable to clear preferred audio output") }
        runCatching { transitionPlayer?.setPreferredAudioDevice(null) }
            .onFailure { Timber.w(it, "Unable to clear transition audio output") }
    }

    private fun registerAudioRouteVolumeReceiver() {
        if (audioRouteVolumeReceiverRegistered) return
        val filter = IntentFilter(VOLUME_CHANGED_ACTION)
        audioRouteVolumeReceiverRegistered = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(audioRouteVolumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(audioRouteVolumeReceiver, filter)
            }
        }.isSuccess
    }

    private fun unregisterAudioRouteVolumeReceiver() {
        if (!audioRouteVolumeReceiverRegistered) return
        runCatching { unregisterReceiver(audioRouteVolumeReceiver) }
        audioRouteVolumeReceiverRegistered = false
    }

    private fun isBluetoothOutputType(type: Int): Boolean =
        type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)

    @Suppress("DEPRECATION")
    private fun routedOutputTypes(manager: AudioManager): Set<Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return runCatching {
                manager.getAudioDevicesForAttributes(platformMediaAudioAttributes).map { it.type }.toSet()
            }.getOrDefault(emptySet())
        }
        return when {
            manager.isBluetoothA2dpOn -> setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            manager.isBluetoothScoOn -> setOf(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
            manager.isWiredHeadsetOn -> setOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            else -> emptySet()
        }
    }

    private fun updatePlaybackProtection(player: Player) {
        (player as? ExoPlayer)?.let { updatePlayerWakeMode(it, it.currentMediaItem) }
        val isLiveRadio = isLiveRadioMediaItem(player.currentMediaItem)
        val playbackExpected = !serviceRecoveryExhausted &&
            player.mediaItemCount > 0 &&
            player.playWhenReady &&
            player.playbackState != Player.STATE_ENDED
        if (playbackExpected) {
            acquirePlaybackWakeLock()
            if (isLiveRadio) {
                markPlaybackExpected(false, force = true)
            } else {
                markPlaybackExpected(true)
            }
        } else if (!shouldPreservePlaybackExpectation(player)) {
            releasePlaybackWakeLock()
            markPlaybackExpected(false)
        }
    }

    private fun reconnectStaleLiveRadio(player: ExoPlayer) {
        val pausedAtMs = liveRadioPausedAtMs
        liveRadioPausedAtMs = C.TIME_UNSET
        if (pausedAtMs == C.TIME_UNSET || SystemClock.elapsedRealtime() - pausedAtMs < LIVE_RADIO_STALE_PAUSE_MS) return
        val mediaId = player.currentMediaItem?.mediaId ?: return
        liveRadioReconnectJob?.cancel()
        liveRadioReconnectJob = serviceScope.launch {
            val stillCurrent = player.currentMediaItem?.mediaId == mediaId && player.playWhenReady
            val loaded = player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING
            if (!stillCurrent || !loaded) return@launch
            _liveRadioMetadataFlow.value = LiveRadioStreamMetadata()
            player.stop()
            player.prepare()
        }
    }

    private fun isPlaybackRecoveryInFlight(): Boolean =
        serviceRecoveryJob?.isActive == true ||
            stickyRestoreJob?.isActive == true ||
            queueSkipJob?.isActive == true

    private fun shouldPreservePlaybackExpectation(player: Player): Boolean =
        isPlaybackRecoveryInFlight() &&
            (player.mediaItemCount == 0 ||
                player.playbackState == Player.STATE_ENDED ||
                player.playbackState == Player.STATE_IDLE)

    @SuppressLint("WakelockTimeout")
    private fun acquirePlaybackWakeLock() {
        if (!playbackWakeLock.isHeld) playbackWakeLock.acquire()
    }

    private fun releasePlaybackWakeLock() {
        if (::playbackWakeLock.isInitialized && playbackWakeLock.isHeld) playbackWakeLock.release()
    }

    private fun markPlaybackExpected(expected: Boolean, force: Boolean = false) {
        val now = System.currentTimeMillis()
        val heartbeatDue = expected && now - lastPlaybackHeartbeatAtMs >= PLAYBACK_HEARTBEAT_INTERVAL_MS
        if (!force && lastPlaybackExpected == expected && !heartbeatDue) return
        lastPlaybackExpected = expected
        lastPlaybackHeartbeatAtMs = now
        playbackStateStore.edit()
            .putBoolean(KEY_PLAYBACK_EXPECTED, expected)
            .putLong(KEY_PLAYBACK_HEARTBEAT_AT, now)
            .apply()
    }

    private data class ServiceRecoveryPlan(
        val localPlayback: Boolean,
        val delaysMs: LongArray,
        val positionMs: Long
    )

    private fun discardIncompatiblePlaybackCache(error: PlaybackException) {
        if (isLiveRadioMediaItem(mediaSession?.player?.currentMediaItem ?: activePlayer?.currentMediaItem)) return
        val reason = playbackFailureReasonOf(error)
        val plan = playbackRecoveryPlanFor(classifyPlaybackFailureReason(reason))
        if (!plan.invalidateCache) return
        val track = queueEngine.state.value.currentTrack ?: return
        if (isLocalPlaybackTrack(track)) return
        val videoMode = mediaSession?.player?.currentMediaItem
            ?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true
        val keys = playbackCacheKeysToDiscard(
            streamKey = DerbisPlaybackCacheKey.stream(track),
            videoKey = DerbisPlaybackCacheKey.video(track),
            videoMode = videoMode,
            hasSeparateVideoStream = track.videoStreamUrl.isNotBlank()
        )
        serviceScope.launch(Dispatchers.IO) {
            val cache = runCatching { DerbisMediaCache.get(this@PlaybackService) }.getOrNull() ?: return@launch
            keys.forEach { key ->
                if (removePlaybackCacheResource(cache, key)) {
                    RuntimeHooks.cache(RuntimeSignal.CACHE_EVICTION)
                    Timber.w("Discarded unplayable cache entry key=%s", key)
                }
            }
        }
    }

    private fun scheduleServiceRecovery(error: PlaybackException) {
        val plan = serviceRecoveryPlan() ?: return
        RuntimeHooks.player(RuntimeSignal.PLAYER_RECOVERY)
        serviceRecoveryJob = serviceScope.launch {
            runServiceRecovery(error, plan)
        }
    }

    private fun serviceRecoveryPlan(): ServiceRecoveryPlan? {
        if (serviceRecoveryJob?.isActive == true) return null
        if (!isPlaybackRecoveryExpected()) return null
        val localPlayback = isCurrentPlaybackLocal()
        return ServiceRecoveryPlan(
            localPlayback = localPlayback,
            delaysMs = if (localPlayback) LOCAL_RECOVERY_DELAYS_MS else ONLINE_RECOVERY_DELAYS_MS,
            positionMs = mediaSession?.player?.currentPosition?.coerceAtLeast(0L) ?: 0L
        )
    }

    private suspend fun runServiceRecovery(error: PlaybackException, plan: ServiceRecoveryPlan) {
        if (awaitUiRecovery(plan.localPlayback)) return
        var awaitedConnectivity = false
        while (isPlaybackRecoveryExpected()) {
            if (finishHealthyServiceRecovery()) return
            if (awaitRecoveryConnectivity(plan.localPlayback)) {
                awaitedConnectivity = true
                continue
            }
            if (awaitedConnectivity) {
                awaitedConnectivity = false
            }
            if (finishExhaustedServiceRecovery(error, plan.delaysMs)) return
            if (attemptServiceRecovery(error, plan)) return
        }
        releasePlaybackWakeLock()
    }

    private fun isPlaybackRecoveryExpected(): Boolean =
        !playbackPausedByUser && playbackStateStore.getBoolean(KEY_PLAYBACK_EXPECTED, false)

    private suspend fun awaitUiRecovery(localPlayback: Boolean): Boolean {
        if (!uiRecoveryAvailable || localPlayback) return false
        releasePlaybackWakeLock()
        repeat(8) {
            delay(750L)
            if (finishHealthyServiceRecovery()) return true
        }
        val player = mediaSession?.player
        return player != null &&
            player.playWhenReady &&
            player.playbackState == Player.STATE_BUFFERING
    }

    private fun finishHealthyServiceRecovery(): Boolean {
        if (!isPlaybackHealthy(mediaSession?.player)) return false
        serviceRecoveryExhausted = false
        mediaSession?.player?.let(::updatePlaybackProtection)
        return true
    }

    private suspend fun awaitRecoveryConnectivity(localPlayback: Boolean): Boolean {
        if (localPlayback || hasInternetCapableNetwork()) return false
        releasePlaybackWakeLock()
        Timber.d("Background playback recovery waiting for network")
        val connectivity = getSystemService(ConnectivityManager::class.java)
        if (connectivity?.awaitInternetNetwork(RECOVERY_CONNECTIVITY_WAIT_MS) == null) {
            delay(RECOVERY_CONNECTIVITY_POLL_MS)
        }
        return true
    }

    private fun finishExhaustedServiceRecovery(
        error: PlaybackException,
        delaysMs: LongArray
    ): Boolean {
        if (serviceRecoveryAttempts < delaysMs.size) return false
        serviceRecoveryExhausted = true
        mediaSession?.player?.pause()
        markPlaybackExpected(false, force = true)
        releasePlaybackWakeLock()
        Timber.e(error, "Background playback recovery exhausted")
        skipUnrecoverableTrack()
        return true
    }

    private suspend fun attemptServiceRecovery(
        error: PlaybackException,
        plan: ServiceRecoveryPlan
    ): Boolean {
        val attempt = serviceRecoveryAttempts++
        acquirePlaybackWakeLock()
        delay(plan.delaysMs[attempt])
        if (stopServiceRecoveryPausedByUser()) return true
        val restored = restoreCurrentPlayback(
            positionMs = plan.positionMs,
            preferFreshResolution = !plan.localPlayback && hasInternetCapableNetwork(),
            activeRecovery = true
        )
        if (!restored && stopServiceRecoveryPausedByUser()) return true
        if (restored) {
            Timber.i(
                "Background playback recovery restored attempt=%d local=%s playWhenReady=%s positionMs=%d",
                attempt + 1,
                plan.localPlayback,
                mediaSession?.player?.playWhenReady,
                plan.positionMs
            )
            return true
        }
        Timber.w(error, "Background playback recovery attempt %d failed", attempt + 1)
        return false
    }

    private fun stopServiceRecoveryPausedByUser(): Boolean {
        if (mediaSession?.player?.playWhenReady != false) return false
        Timber.i("Background playback recovery stopped: paused during recovery")
        markPlaybackExpected(false, force = true)
        releasePlaybackWakeLock()
        return true
    }

    private fun isPlaybackHealthy(player: Player?): Boolean = player != null &&
        player.mediaItemCount > 0 &&
        player.playWhenReady &&
        player.playbackState == Player.STATE_READY

    private fun scheduleStickyPlaybackRestore(startId: Int): Boolean {
        if (!isPlaybackRecoveryExpected()) {
            stopSelfResult(startId)
            return false
        }
        if (isStickyRestoreExpired()) {
            stopStickyRestore(startId)
            return false
        }
        acquirePlaybackWakeLock()
        stickyRestoreJob?.cancel()
        stickyRestoreJob = serviceScope.launch {
            restoreStickyPlayback(startId)
        }
        return true
    }

    private suspend fun restoreStickyPlayback(startId: Int) {
        delay(250L)
        val player = mediaSession?.player ?: run {
            stopStickyRestore(startId)
            return
        }
        if (player.mediaItemCount > 0 || player.playWhenReady) return
        val snapshot = restoreQueueSnapshot()
        val currentTrack = snapshot.currentTrack ?: run {
            stopStickyRestore(startId)
            return
        }
        if (!awaitStickyRestoreConnectivity(currentTrack)) {
            if (isStickyRestoreExpired()) stopStickyRestore(startId)
            return
        }
        val restored = restoreCurrentPlayback(
            snapshot.positionMs,
            preferFreshResolution = !isLocalPlaybackTrack(currentTrack),
            activeRecovery = false
        )
        if (!restored) {
            Timber.w("Sticky background playback restore failed")
            stopStickyRestore(startId)
        }
    }

    private suspend fun restoreQueueSnapshot(): PlaybackQueueSnapshot = withContext(Dispatchers.IO) {
        if (queueEngine.state.value.tracks.isEmpty()) {
            queueEngine.restore(
                fallbackTracks = emptyList(),
                fallbackIndex = -1,
                fallbackPositionMs = 0L,
                fallbackRadioEnabled = true
            )
        } else {
            queueEngine.state.value
        }
    }

    private suspend fun awaitStickyRestoreConnectivity(
        track: com.yasuda.derbis.domain.Track
    ): Boolean {
        if (isLocalPlaybackTrack(track)) return true
        while (isPlaybackRecoveryExpected() && !isStickyRestoreExpired()) {
            if (hasInternetCapableNetwork()) {
                acquirePlaybackWakeLock()
                return true
            }
            releasePlaybackWakeLock()
            Timber.d("Sticky playback restore waiting for network")
            delay(5_000L)
        }
        return false
    }

    private fun isStickyRestoreExpired(): Boolean {
        val heartbeatAt = playbackStateStore.getLong(KEY_PLAYBACK_HEARTBEAT_AT, 0L)
        return heartbeatAt <= 0L ||
            System.currentTimeMillis() - heartbeatAt > STICKY_RESTORE_MAX_AGE_MS
    }

    private fun stopStickyRestore(startId: Int) {
        markPlaybackExpected(false, force = true)
        releasePlaybackWakeLock()
        stopSelfResult(startId)
    }

    private suspend fun hasCompletePlaybackCache(track: Track, videoMode: Boolean): Boolean {
        if (videoMode || isLocalPlaybackTrack(track) || track.streamUrl.isBlank()) return false
        return withContext(Dispatchers.IO) {
            val cache = runCatching { DerbisMediaCache.get(this@PlaybackService) }.getOrNull()
                ?: return@withContext false
            isPlaybackResourceFullyCached(cache, DerbisPlaybackCacheKey.stream(track))
        }
    }

    private suspend fun restoreCurrentPlayback(
        positionMs: Long,
        preferFreshResolution: Boolean,
        activeRecovery: Boolean
    ): Boolean {
        val player = mediaSession?.player ?: return false
        if (playbackPausedByUser || isLiveRadioMediaItem(player.currentMediaItem)) return false
        if (!playbackStateStore.getBoolean(KEY_PLAYBACK_EXPECTED, false)) return false
        val currentItem = player.currentMediaItem
        val queueSnapshot = queueEngine.state.value
        val queueTrack = queueSnapshot.currentTrack
        val videoMode = currentItem?.mediaMetadata?.extras?.getBoolean(EXTRA_VIDEO_MODE, false) ?: false
        val mediaItem = when {
            queueTrack != null && isLocalPlaybackTrack(queueTrack) -> {
                when {
                    isLocalPlaybackUri(queueTrack.streamUrl) -> DerbisMediaItemFactory.build(queueTrack, false)
                    isLocalMediaItem(currentItem) -> currentItem
                    else -> null
                }
            }
            isLocalMediaItem(currentItem) -> currentItem
            preferFreshResolution && queueTrack != null && hasCompletePlaybackCache(queueTrack, videoMode) -> {
                Timber.d("Background recovery replayed a complete cache entry")
                DerbisMediaItemFactory.build(queueTrack, videoMode)
            }
            preferFreshResolution && queueTrack != null && hasInternetCapableNetwork() -> {
                val resolved = withContext(Dispatchers.IO) {
                    runCatching { resolveQueueTrack(queueTrack) }
                        .onFailure { Timber.w(it, "Fresh background stream resolution failed") }
                        .getOrNull()
                }
                if (resolved != null) {
                    queueEngine.updateTrackAt(queueSnapshot.currentIndex, resolved)
                    DerbisMediaItemFactory.build(resolved, videoMode)
                } else {
                    currentItem
                }
            }
            else -> currentItem ?: queueTrack
                ?.takeIf { it.streamUrl.isNotBlank() }
                ?.let { DerbisMediaItemFactory.build(it, videoMode) }
        } ?: return false
        if (shouldAbortActiveRecoveryRestore(activeRecovery, player.playWhenReady)) {
            Timber.i("Playback recovery restore skipped: paused during stream resolution")
            return false
        }
        (player as? ExoPlayer)?.let { updatePlayerWakeMode(it, mediaItem) }
        acquirePlaybackWakeLock()
        player.setMediaItem(mediaItem, positionMs.coerceAtLeast(0L))
        RuntimeHooks.player(
            action = RuntimeSignal.PLAYER_PREPARE,
            mode = if (mediaItem.mediaMetadata.extras?.getBoolean(EXTRA_VIDEO_MODE, false) == true) {
                RuntimeSignal.MODE_VIDEO
            } else {
                RuntimeSignal.MODE_AUDIO
            }
        )
        RuntimeHooks.hot(RuntimeSignal.HOT_PLAYER_PREPARE)
        player.prepare()
        player.play()
        updatePlaybackProtection(player)
        return true
    }

    private fun startPlaybackWatchdog(player: ExoPlayer) {
        playbackWatchdogJob?.cancel()
        watchdogAdvancedAtMs = SystemClock.elapsedRealtime()
        playbackWatchdogJob = serviceScope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                inspectPlaybackWatchdog(player)
            }
        }
    }

    private fun inspectPlaybackWatchdog(player: ExoPlayer) {
        val now = SystemClock.elapsedRealtime()
        refreshAudioOutputProfile()
        if (!shouldPreservePlaybackExpectation(player)) markPlaybackExpected(isPlaybackExpected(player))
        if (resetWatchdogForExhaustedRecovery(now)) return
        if (resetWatchdogForInactivePlayer(player, now)) return
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        if (recordWatchdogProgress(positionMs, now)) return
        if (!isWatchdogStalled(now)) return
        scheduleWatchdogRecovery(positionMs)
        watchdogAdvancedAtMs = now
    }

    private fun isPlaybackExpected(player: ExoPlayer): Boolean = !serviceRecoveryExhausted &&
        !isLiveRadioMediaItem(player.currentMediaItem) &&
        player.mediaItemCount > 0 &&
        player.playWhenReady &&
        player.playbackState != Player.STATE_ENDED

    private fun resetWatchdogForExhaustedRecovery(now: Long): Boolean {
        if (!serviceRecoveryExhausted) return false
        resetWatchdogProgress(now)
        return true
    }

    private fun resetWatchdogForInactivePlayer(player: ExoPlayer, now: Long): Boolean {
        if (isPlayerActivelyPlaying(player)) return false
        resetWatchdogProgress(now)
        return true
    }

    private fun isPlayerActivelyPlaying(player: ExoPlayer): Boolean = player.mediaItemCount > 0 &&
        player.playWhenReady &&
        player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE &&
        (player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY)

    private fun resetWatchdogProgress(now: Long) {
        watchdogPositionMs = C.TIME_UNSET
        watchdogAdvancedAtMs = now
    }

    private fun recordWatchdogProgress(positionMs: Long, now: Long): Boolean {
        if (!hasWatchdogPositionAdvanced(positionMs)) return false
        if (isGenuineWatchdogProgress(watchdogPositionMs, positionMs)) watchdogRecoveryAllowance.reset()
        watchdogPositionMs = positionMs
        watchdogAdvancedAtMs = now
        return true
    }

    private fun hasWatchdogPositionAdvanced(positionMs: Long): Boolean =
        watchdogPositionMs == C.TIME_UNSET ||
            positionMs > watchdogPositionMs + WATCHDOG_PROGRESS_THRESHOLD_MS ||
            positionMs < watchdogPositionMs

    private fun isWatchdogStalled(now: Long): Boolean =
        now - watchdogAdvancedAtMs >= WATCHDOG_STALL_TIMEOUT_MS

    private fun scheduleWatchdogRecovery(positionMs: Long) {
        if (serviceRecoveryJob?.isActive == true) return
        if (isLiveRadioMediaItem(mediaSession?.player?.currentMediaItem)) return
        if (!watchdogRecoveryAllowance.isAvailable()) {
            Timber.d("Playback watchdog recovery deferred to load retries at %d ms", positionMs)
            return
        }
        if (shouldDeferWatchdogForConnectivity(isCurrentPlaybackLocal(), hasInternetCapableNetwork())) {
            Timber.d("Playback watchdog waiting for network at %d ms", positionMs)
            return
        }
        Timber.w("Playback watchdog detected a stalled player at %d ms", positionMs)
        serviceRecoveryJob = serviceScope.launch {
            val restored = restoreCurrentPlayback(
                positionMs,
                preferFreshResolution = !isCurrentPlaybackLocal() && hasInternetCapableNetwork(),
                activeRecovery = true
            )
            if (restored) watchdogRecoveryAllowance.consume() else Timber.w("Playback watchdog recovery failed")
        }
    }

    private fun isCurrentPlaybackLocal(): Boolean {
        val player = mediaSession?.player
        return isLocalMediaItem(player?.currentMediaItem) ||
            queueEngine.state.value.currentTrack?.let(::isLocalPlaybackTrack) == true
    }

    private fun isLocalPlaybackTrack(track: com.yasuda.derbis.domain.Track): Boolean =
        track.source.equals("Offline", ignoreCase = true) || isLocalPlaybackUri(track.streamUrl)

    private fun isLocalPlaybackUri(value: String): Boolean {
        val clean = value.trim()
        return clean.startsWith("content://", ignoreCase = true) ||
            clean.startsWith("file://", ignoreCase = true)
    }

    private fun isLocalMediaItem(mediaItem: MediaItem?): Boolean {
        val scheme = mediaItem?.localConfiguration?.uri?.scheme.orEmpty()
        if (scheme.equals("content", ignoreCase = true) || scheme.equals("file", ignoreCase = true)) return true
        return mediaItem?.mediaMetadata?.extras
            ?.getString("derbis.source")
            ?.equals("Offline", ignoreCase = true) == true
    }

    private fun updatePlayerWakeMode(player: ExoPlayer, mediaItem: MediaItem?) {
        val wakeMode = if (isLocalMediaItem(mediaItem)) C.WAKE_MODE_LOCAL else C.WAKE_MODE_NETWORK
        if (wakeMode == appliedPlayerWakeMode) return
        player.setWakeMode(wakeMode)
        appliedPlayerWakeMode = wakeMode
    }

    private fun hasInternetCapableNetwork(): Boolean {
        val connectivity = getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun libraryItemFuture(
        params: LibraryParams?,
        block: suspend () -> MediaItem
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val future = SettableFuture.create<LibraryResult<MediaItem>>()
        serviceScope.launch(Dispatchers.IO) {
            val result = runCatching { LibraryResult.ofItem(block(), params) }
                .getOrElse { error ->
                    Timber.w(error, "Android Auto item load failed")
                    LibraryResult.ofItem(autoLibrary.root(), params)
                }
            future.set(result)
        }
        return future
    }

    private fun libraryListFuture(
        params: LibraryParams?,
        block: suspend () -> List<MediaItem>
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return autoLibraryFuture {
            val items = withTimeout(15_000L) {
                withContext(Dispatchers.IO) {
                    com.yasuda.derbis.data.runCatchingPreservingCancellation { block() }.getOrThrow()
                }
            }
            LibraryResult.ofItemList(items, params)
        }
    }

    private fun <T : Any> autoLibraryFuture(block: suspend () -> LibraryResult<T>): ListenableFuture<LibraryResult<T>> {
        val future = SettableFuture.create<LibraryResult<T>>()
        val job = serviceScope.launch {
            try {
                future.set(block())
            } catch (timeout: TimeoutCancellationException) {
                Timber.w("Android Auto library request timed out")
                future.set(LibraryResult.ofError<T>(SessionError(SessionError.ERROR_IO,
                    com.yasuda.derbis.ui.i18n.DerbisStrings.forCode(DerbisPreferences(this@PlaybackService).languageCode()).networkTestTimeout,
                    Bundle().apply { putString(AndroidAutoLibrary.EXTRA_SEARCH_FAILURE, "timeout") })))
            } catch (cancelled: CancellationException) {
                future.cancel(false)
                throw cancelled
            } catch (error: Exception) {
                Timber.w("Android Auto library request failed: %s", error.javaClass.simpleName)
                future.set(LibraryResult.ofError<T>(SessionError(
                    if (error is IllegalArgumentException) SessionError.ERROR_BAD_VALUE else SessionError.ERROR_IO,
                    com.yasuda.derbis.ui.i18n.DerbisStrings.forCode(DerbisPreferences(this@PlaybackService).languageCode()).networkTestUnknown)))
            }
        }
        future.addListener({ if (future.isCancelled) job.cancel() }, com.google.common.util.concurrent.MoreExecutors.directExecutor())
        job.invokeOnCompletion { if (!future.isDone) future.cancel(false) }
        return future
    }

    private fun mediaItemsFuture(block: suspend () -> List<MediaItem>): ListenableFuture<List<MediaItem>> {
        val future = SettableFuture.create<List<MediaItem>>()
        serviceScope.launch(Dispatchers.IO) {
            val items = runCatching { block() }
                .getOrElse { error ->
                    Timber.w(error, "Android Auto media item resolve failed")
                    emptyList()
                }
            future.set(items)
        }
        return future
    }

    private fun paginate(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
        if (pageSize <= 0) return items
        val safePage = page.coerceAtLeast(0)
        val from = safePage.toLong() * pageSize.toLong()
        if (from >= items.size) return emptyList()
        val start = from.toInt()
        val end = (start + pageSize).coerceAtMost(items.size)
        return items.subList(start, end)
    }
}

@UnstableApi
private object DerbisPlaybackLoadErrorHandlingPolicy : LoadErrorHandlingPolicy {
    override fun getFallbackSelectionFor(
        fallbackOptions: LoadErrorHandlingPolicy.FallbackOptions,
        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
    ): LoadErrorHandlingPolicy.FallbackSelection? = null

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long =
        transientNetworkRetryDelayMs(loadErrorInfo.exception, loadErrorInfo.errorCount)

    override fun getMinimumLoadableRetryCount(dataType: Int): Int = TRANSIENT_NETWORK_LOAD_RETRIES
}

internal const val TRANSIENT_NETWORK_LOAD_RETRIES = 8

private object LiveRadioLoadErrorHandlingPolicy : LoadErrorHandlingPolicy {
    override fun getFallbackSelectionFor(
        fallbackOptions: LoadErrorHandlingPolicy.FallbackOptions,
        loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
    ): LoadErrorHandlingPolicy.FallbackSelection? = null

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long =
        liveRadioLoadRetryDelayMs(loadErrorInfo.exception, loadErrorInfo.errorCount)

    override fun getMinimumLoadableRetryCount(dataType: Int): Int = LIVE_RADIO_LOAD_RETRIES
}

internal const val LIVE_RADIO_LOAD_RETRIES = 2
internal const val LIVE_RADIO_READ_TIMEOUT_SECONDS = 15L

internal fun liveRadioLoadRetryDelayMs(error: Throwable, errorCount: Int): Long =
    if (errorCount > LIVE_RADIO_LOAD_RETRIES) C.TIME_UNSET else transientNetworkRetryDelayMs(error, errorCount)

internal const val WATCHDOG_RECOVERIES_WITHOUT_PROGRESS = 1
private const val WATCHDOG_PROGRESS_THRESHOLD_MS = 250L
private const val RETIRED_PLAYBACK_LIVE_UPDATE_CHANNEL_ID = "derbis_playback_live_update"

internal class WatchdogRecoveryAllowance(private val limit: Int = WATCHDOG_RECOVERIES_WITHOUT_PROGRESS) {
    private var used = 0

    fun isAvailable(): Boolean = used < limit

    fun consume() {
        used++
    }

    fun reset() {
        used = 0
    }
}

internal fun shouldDeferWatchdogForConnectivity(localPlayback: Boolean, hasInternet: Boolean): Boolean =
    !localPlayback && !hasInternet

internal fun crossfadeHandoffCanResync(secondaryState: Int): Boolean = secondaryState == Player.STATE_READY

internal fun isGenuineWatchdogProgress(previousPositionMs: Long, positionMs: Long): Boolean =
    previousPositionMs != C.TIME_UNSET && positionMs > previousPositionMs + WATCHDOG_PROGRESS_THRESHOLD_MS

internal fun shouldAbortActiveRecoveryRestore(activeRecovery: Boolean, playWhenReady: Boolean): Boolean =
    activeRecovery && !playWhenReady

internal fun transientNetworkRetryDelayMs(error: Throwable, errorCount: Int): Long {
    if (errorCount > TRANSIENT_NETWORK_LOAD_RETRIES) return C.TIME_UNSET
    if (!isTransientNetworkFailure(error)) return C.TIME_UNSET
    return (1_000L shl (errorCount - 1).coerceIn(0, 3)).coerceAtMost(5_000L)
}

internal fun truePeakLimiterRequired(
    settings: DerbisAudioSettings,
    parametricActive: Boolean,
    audioNormalization: Boolean
): Boolean = settings.limiterEnabled &&
    (settings.equalizerEnabled || parametricActive || settings.virtualizer > 0 ||
        settings.replayGainActive || audioNormalization)

internal fun isPersonalCollectionMediaItem(mediaItem: MediaItem?): Boolean {
    if (mediaItem == null) return false
    val extras = mediaItem.mediaMetadata.extras ?: mediaItem.requestMetadata.extras
    if (extras?.getBoolean(PlaybackService.EXTRA_PERSONAL_COLLECTION, false) != true) return false
    val scheme = mediaItem.localConfiguration?.uri?.scheme.orEmpty().lowercase()
    return scheme == "https" || scheme == "http"
}

internal fun isLiveRadioMediaItem(mediaItem: MediaItem?): Boolean {
    if (mediaItem == null) return false
    if (mediaItem.mediaId.startsWith("live-radio:")) return true
    if (mediaItem.mediaMetadata.mediaType == androidx.media3.common.MediaMetadata.MEDIA_TYPE_RADIO_STATION) return true
    val extras = mediaItem.mediaMetadata.extras ?: mediaItem.requestMetadata.extras
    return extras?.getBoolean(PlaybackService.EXTRA_LIVE_RADIO, false) == true ||
        extras?.getString("derbis.source") == LIVE_RADIO_SOURCE
}

@UnstableApi
private class DerbisMediaSourceFactory(
    private val delegate: DefaultMediaSourceFactory,
    private val dataSourceFactory: DataSource.Factory,
    private val localDataSourceFactory: DataSource.Factory,
    private val sabrDataSourceFactory: DataSource.Factory,
    private val liveRadioDataSourceFactory: DataSource.Factory,
    private val personalCollectionDataSourceFactory: (String) -> DataSource.Factory
) : MediaSource.Factory {
    private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy = DerbisPlaybackLoadErrorHandlingPolicy
    private var drmSessionManagerProvider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider? = null

    private val subtitleDataSourceFactory: DataSource.Factory by lazy {
        OkHttpDataSource.Factory(DerbisHttpClientFactory.externalIntegrations())
    }

    override fun getSupportedTypes(): IntArray = delegate.supportedTypes

    override fun setDrmSessionManagerProvider(
        provider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider
    ): MediaSource.Factory {
        drmSessionManagerProvider = provider
        delegate.setDrmSessionManagerProvider(provider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(
        policy: LoadErrorHandlingPolicy
    ): MediaSource.Factory {
        loadErrorHandlingPolicy = policy
        delegate.setLoadErrorHandlingPolicy(policy)
        return this
    }

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val videoUrl = mediaItem.mediaMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_URL)
            ?: mediaItem.requestMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_URL)

        if (videoUrl.isNullOrBlank()) {
            return mediaSourceFor(mediaItem)
        }

        val videoCacheKey = mediaItem.mediaMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_CACHE_KEY)
            ?: mediaItem.requestMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_CACHE_KEY)
        val videoMimeType = mediaItem.mediaMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_MIME_TYPE)
            ?: mediaItem.requestMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_MIME_TYPE)

        val audioSource = mediaSourceFor(mediaItem)
        val videoItem = MediaItem.Builder()
            .setUri(videoUrl)
            .apply {
                if (!videoCacheKey.isNullOrBlank()) setCustomCacheKey(videoCacheKey)
                if (!videoMimeType.isNullOrBlank()) setMimeType(videoMimeType)
            }
            .build()
        val videoSource = mediaSourceFor(videoItem)

        return MergingMediaSource(true, true, audioSource, videoSource)
    }

    private fun mediaSourceFor(mediaItem: MediaItem): MediaSource {
        val subtitleUris = mediaItem.localConfiguration?.subtitleConfigurations
            .orEmpty()
            .mapTo(hashSetOf()) { it.uri }
        val personalCollectionId = personalCollectionIdOf(mediaItem.mediaId)
        if (personalCollectionId != null && isPersonalCollectionMediaItem(mediaItem)) {
            val factory = DefaultMediaSourceFactory(personalCollectionDataSourceFactory(personalCollectionId))
                .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
            drmSessionManagerProvider?.let { factory.setDrmSessionManagerProvider(it) }
            return factory.createMediaSource(mediaItem)
        }
        val localUri = mediaItem.localConfiguration?.uri
        val inferredManifestMimeType = mediaItem.localConfiguration?.mimeType
            ?.takeIf { it.isNotBlank() }
            ?: DerbisMediaItemFactory.mimeTypeFor(localUri?.toString().orEmpty(), false)
                ?.takeIf { it == MimeTypes.APPLICATION_M3U8 || it == MimeTypes.APPLICATION_MPD }
        val liveRadio = isLiveRadioMediaItem(mediaItem)
        val routedFactory = when {
            liveRadio && inferredManifestMimeType == null -> LiveRadioPlaylistDataSource.Factory(liveRadioDataSourceFactory)
            liveRadio -> liveRadioDataSourceFactory
            else -> DerbisRoutingDataSourceFactory(
                dataSourceFactory = dataSourceFactory,
                localDataSourceFactory = localDataSourceFactory,
                sabrDataSourceFactory = sabrDataSourceFactory,
                subtitleDataSourceFactory = subtitleDataSourceFactory,
                subtitleUris = subtitleUris
            )
        }
        val factory = DefaultMediaSourceFactory(routedFactory).setLoadErrorHandlingPolicy(
            if (liveRadio) LiveRadioLoadErrorHandlingPolicy else loadErrorHandlingPolicy
        )
        drmSessionManagerProvider?.let { factory.setDrmSessionManagerProvider(it) }
        val item = if (
            SabrStreamSpec.isSabrUri(localUri?.toString().orEmpty()) ||
            localUri?.scheme.orEmpty().lowercase() == "content" ||
            localUri?.scheme.orEmpty().lowercase() == "file"
        ) {
            mediaItem.buildUpon().setCustomCacheKey(null).build()
        } else if (mediaItem.localConfiguration?.mimeType.isNullOrBlank() && inferredManifestMimeType != null) {
            mediaItem.buildUpon().setMimeType(inferredManifestMimeType).build()
        } else {
            mediaItem
        }
        return factory.createMediaSource(item)
    }

}

@UnstableApi
private class DerbisRoutingDataSourceFactory(
    private val dataSourceFactory: DataSource.Factory,
    private val localDataSourceFactory: DataSource.Factory,
    private val sabrDataSourceFactory: DataSource.Factory,
    private val subtitleDataSourceFactory: DataSource.Factory,
    private val subtitleUris: Set<Uri>
) : DataSource.Factory {
    override fun createDataSource(): DataSource = DerbisRoutingDataSource(
        dataSourceFactory = dataSourceFactory,
        localDataSourceFactory = localDataSourceFactory,
        sabrDataSourceFactory = sabrDataSourceFactory,
        subtitleDataSourceFactory = subtitleDataSourceFactory,
        subtitleUris = subtitleUris
    )
}

@UnstableApi
private class DerbisRoutingDataSource(
    private val dataSourceFactory: DataSource.Factory,
    private val localDataSourceFactory: DataSource.Factory,
    private val sabrDataSourceFactory: DataSource.Factory,
    private val subtitleDataSourceFactory: DataSource.Factory,
    private val subtitleUris: Set<Uri>
) : DataSource {
    private val transferListeners = mutableListOf<TransferListener>()
    private var delegate: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        if (transferListener !in transferListeners) {
            transferListeners += transferListener
        }
        delegate?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val previous = delegate
        if (previous != null) {
            delegate = null
            runCatching { previous.close() }
        }
        val uri = dataSpec.uri
        val scheme = uri.scheme.orEmpty().lowercase()
        if (scheme == "http") {
            throw IOException("Cleartext HTTP is only allowed for live radio")
        }
        val factory = when {
            uri in subtitleUris -> subtitleDataSourceFactory
            SabrStreamSpec.isSabrUri(uri.toString()) -> sabrDataSourceFactory
            scheme == "content" || scheme == "file" -> localDataSourceFactory
            else -> dataSourceFactory
        }
        val source = factory.createDataSource()
        transferListeners.forEach(source::addTransferListener)
        delegate = source
        return source.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(delegate).read(buffer, offset, length)

    override fun getUri(): Uri? = delegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        delegate?.responseHeaders.orEmpty()

    override fun close() {
        val source = delegate
        delegate = null
        source?.close()
    }
}
