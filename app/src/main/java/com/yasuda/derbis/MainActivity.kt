package com.yasuda.derbis

import android.Manifest
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.yasuda.derbis.data.DerbisArtworkCache
import com.yasuda.derbis.data.DerbisBackupManager
import com.yasuda.derbis.data.spotify.DerbisSpotifyConnection
import com.yasuda.derbis.feature.discord.DerbisDiscordRichPresence
import com.yasuda.derbis.domain.AppUpdateInfo
import com.yasuda.derbis.domain.ChartsCatalog
import com.yasuda.derbis.domain.DerbisFontPreset
import com.yasuda.derbis.feature.recognition.DerbisRecognitionCenter
import com.yasuda.derbis.feature.recognition.MusicRecognitionService
import com.yasuda.derbis.player.DerbisPipBridge
import com.yasuda.derbis.ui.DerbisApp
import com.yasuda.derbis.ui.DerbisGlassStyle
import com.yasuda.derbis.ui.LocalDerbisGlassStyle
import com.yasuda.derbis.ui.components.LocalDerbisLiquidGlassEnabled
import com.yasuda.derbis.ui.i18n.DerbisStrings
import com.yasuda.derbis.ui.support.RemoteAnnouncementGate
import com.yasuda.derbis.ui.support.RemoteAnnouncementPromptPolicy
import com.yasuda.derbis.ui.theme.DerbisTheme
import com.yasuda.derbis.ui.theme.DerbisThemeController
import com.yasuda.derbis.ui.theme.DerbisThemes
import com.yasuda.derbis.ui.update.DerbisUpdateBanner
import com.yasuda.derbis.ui.update.DerbisUpdatePhase
import com.yasuda.derbis.ui.update.DerbisUpdateScreen
import com.yasuda.derbis.update.AppUpdateContract
import com.yasuda.derbis.update.AppUpdateInstaller
import com.yasuda.derbis.update.AppUpdateSpeedTracker
import com.yasuda.derbis.update.PreparedAppUpdate
import com.yasuda.derbis.viewmodel.DerbisUiState
import com.yasuda.derbis.viewmodel.DerbisViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber


private data class MainActivityUiSlice(
    val fontPreset: DerbisFontPreset,
    val animationsEnabled: Boolean,
    val liquidGlassEnabled: Boolean,
    val forceHighRefreshRate: Boolean,
    val glassStyle: DerbisGlassStyle,
    val isPlaying: Boolean,
    val showSettings: Boolean,
    val showOnboarding: Boolean,
    val languageCode: String,
    val recentListenCount: Int,
    val updateInfo: AppUpdateInfo?,
    val showUpdatePrompt: Boolean
)

private fun DerbisUiState.toMainActivityUiSlice(): MainActivityUiSlice = MainActivityUiSlice(
    fontPreset = interfaceSettings.fontPreset,
    animationsEnabled = if (interfaceSettings.liteMode) false else animationsEnabled,
    liquidGlassEnabled = if (interfaceSettings.liteMode) false else
        (interfaceSettings.liquidGlassEnabled || interfaceSettings.skin == com.yasuda.derbis.domain.DerbisSkin.LiquidGlass),
    forceHighRefreshRate = interfaceSettings.forceHighRefreshRate,
    glassStyle = DerbisGlassStyle(
        blur = interfaceSettings.glassBlur,
        transparency = interfaceSettings.glassTransparency,
        tint = interfaceSettings.glassTint,
        refraction = interfaceSettings.glassRefraction
    ),
    isPlaying = isPlaying,
    showSettings = showSettings,
    showOnboarding = showOnboarding,
    languageCode = languageCode,
    recentListenCount = recentListens.size,
    updateInfo = updateInfo,
    showUpdatePrompt = showUpdatePrompt
)

class MainActivity : ComponentActivity() {
    private var exitApplicationRequested = false
    private val pipMode = mutableStateOf(false)
    private val viewModel: DerbisViewModel by viewModels()
    private val updatePhase = mutableStateOf<DerbisUpdatePhase>(DerbisUpdatePhase.Idle)
    private val updateSpeedTracker = AppUpdateSpeedTracker()
    private val updateInstaller by lazy { AppUpdateInstaller(applicationContext) }
    private var updateJob: Job? = null
    private var updateRequestToken = 0L
    private var pendingUpdate: PreparedAppUpdate? = null

    private val unknownSourcesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        resumePendingUpdateInstall()
    }

    private val recognitionPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        DerbisLaunchActions.pendingShortcut.value = DerbisLaunchActions.SHORTCUT_SEARCH
        if (granted) startMicrophoneRecognitionService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyOrientationPolicy()
        configureFastImageLoader()
        requestNotificationPermission()
        requestLegacyStoragePermission()
        val startPalette = DerbisThemes.byId(DerbisThemes.APPLE_MUSIC)
        DerbisThemeController.apply(startPalette.id)
        WindowCompat.enableEdgeToEdge(window)
        window.setBackgroundDrawable(ColorDrawable(if (startPalette.isLight) Color.WHITE else Color.BLACK))
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = startPalette.isLight
            isAppearanceLightNavigationBars = startPalette.isLight
        }
        runCatching {
            DerbisSpotifyConnection.initialize(applicationContext)
            DerbisSpotifyConnection.handleRedirect(applicationContext, intent.data)
        }.onFailure { Timber.w(it, "Spotify startup attachment failed") }
        runCatching {
            DerbisDiscordRichPresence.attach(this)
        }.onFailure { Timber.w(it, "Discord startup attachment failed") }
        // PlaybackService owns ongoing presence publication. This Activity only supplies the
        // Discord SDK with the foreground engine Activity needed for account linking.
        DerbisLaunchActions.consumeFrom(intent)
        handleRecognitionLaunchRequest()
        if (Build.VERSION.SDK_INT >= 28) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }
        // The ViewModel snapshot is loaded from persistent preferences before Activity creation.
        // Request it here as well as from Compose so an app relaunch never starts at system rate.
        applyForceHighRefreshRate(viewModel.state.value.interfaceSettings.forceHighRefreshRate)
        DerbisPipBridge.bind(
            enter = ::enterPictureInPicture,
            update = ::updatePictureInPictureParams
        )
        val initialActivityUiState = viewModel.state.value.toMainActivityUiSlice()
        setContent {
            val activityStateFlow = remember(viewModel) {
                viewModel.state
                    .map(DerbisUiState::toMainActivityUiSlice)
                    .distinctUntilChanged()
            }
            val activityUiState by activityStateFlow.collectAsStateWithLifecycle(
                initialValue = initialActivityUiState
            )

            DerbisTheme(
                fontPreset = activityUiState.fontPreset,
                animationsEnabled = activityUiState.animationsEnabled
            ) {
                var listenedPlaybackMs by rememberSaveable { mutableLongStateOf(0L) }
                var previousLanguageCode by remember { mutableStateOf(activityUiState.languageCode) }

                LaunchedEffect(activityUiState.languageCode) {
                    val currentLanguageCode = activityUiState.languageCode
                    if (currentLanguageCode == previousLanguageCode) return@LaunchedEffect
                    previousLanguageCode = currentLanguageCode
                    val defaultRegionId = ChartsCatalog.defaultRegionForLanguage(currentLanguageCode).id
                    if (viewModel.state.value.selectedChartId != defaultRegionId) {
                        viewModel.selectChart(defaultRegionId)
                    }
                }

                LaunchedEffect(activityUiState.isPlaying) {
                    if (!activityUiState.isPlaying) return@LaunchedEffect
                    var lastTickMs = SystemClock.elapsedRealtime()
                    while (true) {
                        delay(1_000L)
                        val nowMs = SystemClock.elapsedRealtime()
                        listenedPlaybackMs += (nowMs - lastTickMs).coerceAtLeast(0L)
                        lastTickMs = nowMs
                    }
                }

                LaunchedEffect(activityUiState.forceHighRefreshRate) {
                    applyForceHighRefreshRate(activityUiState.forceHighRefreshRate)
                }

                CompositionLocalProvider(
                    LocalDerbisLiquidGlassEnabled provides activityUiState.liquidGlassEnabled,
                    LocalDerbisGlassStyle provides activityUiState.glassStyle
                ) {
                    DerbisApp(
                        viewModel = viewModel,
                        onExitApplication = {
                            viewModel.exitApplication { terminateProcess ->
                                exitApplicationRequested = terminateProcess
                                finishAndRemoveTask()
                            }
                        },
                        isInPictureInPicture = pipMode.value,
                        onRetryPreUpdateBackup = ::retryPreUpdateBackup,
                        onContinueUpdateWithoutBackup = ::continueUpdateWithoutBackup
                    )
                }
                RemoteAnnouncementGate(
                    enabled = !activityUiState.showOnboarding && !pipMode.value && !activityUiState.showSettings,
                    languageCode = activityUiState.languageCode,
                    hasPositiveListeningMoment = RemoteAnnouncementPromptPolicy.hasPositiveListeningMoment(
                        recentListenCount = activityUiState.recentListenCount,
                        listenedPlaybackMs = listenedPlaybackMs
                    )
                )
                if (
                    BuildConfig.UPSTREAM_UPDATES_ENABLED &&
                    !pipMode.value &&
                    !activityUiState.showOnboarding
                ) {
                    val updateStrings = DerbisStrings.forCode(activityUiState.languageCode)
                    val phase = updatePhase.value
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        AnimatedVisibility(
                            visible = phase !is DerbisUpdatePhase.Idle,
                            enter = fadeIn() + slideInVertically { it / 2 },
                            exit = fadeOut() + slideOutVertically { it / 2 }
                        ) {
                            DerbisUpdateBanner(
                                phase = phase,
                                strings = updateStrings,
                                onUpdate = {
                                    activityUiState.updateInfo?.let(::startUpdateFromBanner)
                                },
                                onCancel = ::cancelInAppUpdate,
                                onRetry = ::retryInAppUpdate,
                                onDismiss = ::dismissUpdateBanner,
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                    val available = activityUiState.updateInfo
                        ?.takeIf { activityUiState.showUpdatePrompt && it.isNewer }
                        ?.takeIf { phase is DerbisUpdatePhase.Idle }
                    AnimatedVisibility(
                        visible = available != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        available?.let { info ->
                            BackHandler { viewModel.dismissUpdatePrompt() }
                            DerbisUpdateScreen(
                                update = info,
                                strings = updateStrings,
                                languageCode = activityUiState.languageCode,
                                onUpdate = { startUpdateFromBanner(info) },
                                onLater = viewModel::dismissUpdatePrompt
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Android/OEM display policy may drop a window display-mode request while this Activity
        // is stopped. Reassert the user's persisted choice whenever the app returns to foreground.
        applyForceHighRefreshRate(viewModel.state.value.interfaceSettings.forceHighRefreshRate)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // At this point decorView is attached, which covers cold launches where no Display was
            // available during onCreate.
            applyForceHighRefreshRate(viewModel.state.value.interfaceSettings.forceHighRefreshRate)
        }
    }

    /**
     * Applies a per-window high-refresh request without changing the saved preference. The
     * request is cleared only when the user has explicitly turned the setting off.
     */
    private fun applyForceHighRefreshRate(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (!enabled) {
            // No display lookup is needed to honor an explicit user opt-out immediately.
            updatePreferredDisplayModeId(0)
            return
        }
        val display = window.decorView.display ?: return
        val supportedModes = display.supportedModes
        val highModeId = supportedModes
            .asSequence()
            .filter { it.refreshRate >= 89.0f }
            .maxByOrNull { it.refreshRate }
            ?.modeId
            ?: supportedModes.maxByOrNull { it.refreshRate }?.modeId
            ?: return
        updatePreferredDisplayModeId(highModeId)
    }

    private fun updatePreferredDisplayModeId(modeId: Int) {
        val params = window.attributes
        if (params.preferredDisplayModeId != modeId) {
            params.preferredDisplayModeId = modeId
            window.attributes = params
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DerbisSpotifyConnection.handleRedirect(applicationContext, intent.data)
        DerbisLaunchActions.consumeFrom(intent)
        handleRecognitionLaunchRequest()
    }

    private fun handleRecognitionLaunchRequest() {
        if (DerbisLaunchActions.pendingShortcut.value != DerbisLaunchActions.SHORTCUT_RECOGNITION) return
        val permissionRequest = intent.getBooleanExtra(
            DerbisLaunchActions.EXTRA_RECOGNITION_PERMISSION_REQUEST,
            false
        )
        intent.removeExtra(DerbisLaunchActions.EXTRA_RECOGNITION_PERMISSION_REQUEST)

        DerbisLaunchActions.pendingShortcut.value = DerbisLaunchActions.SHORTCUT_SEARCH
        if (!DerbisRecognitionCenter.isAvailable || !permissionRequest) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            recognitionPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startMicrophoneRecognitionService()
        }
    }

    private fun startMicrophoneRecognitionService() {
        runCatching {
            ContextCompat.startForegroundService(this, MusicRecognitionService.microphoneIntent(this))
        }.onFailure { Timber.w(it, "Recognition service could not start") }
    }

    override fun startActivity(intent: Intent) {
        if (handleInAppUpdateIntent(intent)) return
        super.startActivity(intent)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        if (handleInAppUpdateIntent(intent)) return
        super.startActivity(intent, options)
    }

    private fun handleInAppUpdateIntent(intent: Intent): Boolean {
        if (!AppUpdateContract.matches(intent)) return false
        if (BuildConfig.UPSTREAM_UPDATES_ENABLED) beginInAppUpdate()
        return true
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.R && DerbisPipBridge.current().playing) {
            enterPictureInPicture()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipMode.value = isInPictureInPictureMode
        DerbisPipBridge.updatePictureInPictureMode(isInPictureInPictureMode)
        applyOrientationPolicy()
    }

    override fun onDestroy() {
        updateJob?.cancel()
        // PlaybackService performs the ordered stop when a recents swipe removes the task.
        // Keep the free-threaded SDK process-owned until then so Activity teardown cannot race a
        // live native callback during a configuration change or task removal.
        DerbisPipBridge.unbind()
        super.onDestroy()
        if (exitApplicationRequested) android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun beginInAppUpdate(skipBackup: Boolean = false) {
        if (!BuildConfig.UPSTREAM_UPDATES_ENABLED || updateJob?.isActive == true) return
        val fallbackVersion = viewModel.state.value.updateInfo?.latestVersionName.orEmpty()
        updateSpeedTracker.reset()
        val requestToken = ++updateRequestToken
        updatePhase.value = DerbisUpdatePhase.Preparing(fallbackVersion)
        val startedAtMs = SystemClock.elapsedRealtime()
        lateinit var job: Job
        job = lifecycleScope.launch {
            try {
                if (!skipBackup && !runPreUpdateBackup(requestToken)) return@launch
                val prepared = updateInstaller.prepareLatestUpdate { versionName, downloaded, total ->
                    publishUpdateProgress(requestToken, startedAtMs, versionName, downloaded, total)
                }
                if (!isCurrentUpdateRequest(requestToken)) return@launch
                pendingUpdate = prepared
                updatePhase.value = DerbisUpdatePhase.Ready(prepared.versionName)
                requestPackageInstall(prepared)
            } catch (cancelled: CancellationException) {
                if (isCurrentUpdateRequest(requestToken)) updatePhase.value = DerbisUpdatePhase.Idle
                throw cancelled
            } catch (error: Throwable) {
                Timber.w(error, "In-app update failed")
                if (isCurrentUpdateRequest(requestToken)) {
                    pendingUpdate = null
                    updatePhase.value = DerbisUpdatePhase.Failed(fallbackVersion)
                }
            } finally {
                if (updateJob === job) updateJob = null
            }
        }
        updateJob = job
    }

    private fun isCurrentUpdateRequest(requestToken: Long): Boolean = updateRequestToken == requestToken

    private suspend fun runPreUpdateBackup(requestToken: Long): Boolean {
        val backupSettings = viewModel.state.value.backupSettings
        if (!backupSettings.preUpdate) return true
        return try {
            DerbisBackupManager(applicationContext).exportAutomatic(backupSettings.retentionCount)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Timber.w(error, "Pre-update backup failed")
            if (isCurrentUpdateRequest(requestToken)) {
                updatePhase.value = DerbisUpdatePhase.Idle
                viewModel.setPreUpdateBackupFailed(true)
            }
            false
        }
    }

    private fun publishUpdateProgress(
        requestToken: Long,
        startedAtMs: Long,
        versionName: String,
        downloadedBytes: Long,
        totalBytes: Long?
    ) {
        runOnUiThread {
            if (!isCurrentUpdateRequest(requestToken)) return@runOnUiThread
            val nowMs = SystemClock.elapsedRealtime()
            val speed = updateSpeedTracker.sample(downloadedBytes, nowMs)
            updatePhase.value = DerbisUpdatePhase.Downloading(
                versionName = versionName,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                bytesPerSecond = speed,
                elapsedMs = nowMs - startedAtMs
            )
        }
    }

    private fun retryPreUpdateBackup() {
        viewModel.setPreUpdateBackupFailed(false)
        beginInAppUpdate()
    }

    private fun continueUpdateWithoutBackup() {
        viewModel.setPreUpdateBackupFailed(false)
        beginInAppUpdate(skipBackup = true)
    }

    private fun cancelInAppUpdate() {
        val job = updateJob
        updateJob = null
        updateRequestToken++
        pendingUpdate = null
        updateSpeedTracker.reset()
        updatePhase.value = DerbisUpdatePhase.Idle
        job?.cancel()
    }

    private fun startUpdateFromBanner(update: AppUpdateInfo) {
        if (AppUpdateContract.matches(Intent.ACTION_VIEW, update.downloadUrl)) {
            beginInAppUpdate()
            return
        }
        val target = update.downloadUrl.trim().takeIf { it.startsWith("https://", ignoreCase = true) }
        if (target == null) {
            showUpdateFailure()
            return
        }
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target))) }
            .onFailure {
                Timber.w(it, "Unable to open update page")
                showUpdateFailure()
            }
    }

    private fun dismissUpdateBanner() {
        when (updatePhase.value) {
            is DerbisUpdatePhase.Idle,
            is DerbisUpdatePhase.Available -> viewModel.dismissUpdatePrompt()
            else -> {
                pendingUpdate = null
                updatePhase.value = DerbisUpdatePhase.Idle
                viewModel.dismissUpdatePrompt()
            }
        }
    }

    private fun retryInAppUpdate() {
        val prepared = pendingUpdate
        if (prepared != null) {
            requestPackageInstall(prepared)
            return
        }
        updatePhase.value = DerbisUpdatePhase.Idle
        beginInAppUpdate()
    }

    private fun requestPackageInstall(prepared: PreparedAppUpdate) {
        if (!packageManager.canRequestPackageInstalls()) {
            pendingUpdate = prepared
            updatePhase.value = DerbisUpdatePhase.PermissionRequired(prepared.versionName)
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")
            )
            runCatching { unknownSourcesLauncher.launch(settingsIntent) }
                .onFailure {
                    Timber.w(it, "Unable to open unknown-source settings")
                    pendingUpdate = null
                    updatePhase.value = DerbisUpdatePhase.Failed(prepared.versionName)
                }
            return
        }
        launchPackageInstaller(prepared)
    }

    private fun resumePendingUpdateInstall() {
        if (!packageManager.canRequestPackageInstalls()) {
            updatePhase.value = pendingUpdate
                ?.let { DerbisUpdatePhase.PermissionRequired(it.versionName) }
                ?: DerbisUpdatePhase.Idle
            return
        }
        val prepared = pendingUpdate
        if (prepared == null) {
            if (BuildConfig.UPSTREAM_UPDATES_ENABLED) beginInAppUpdate()
            return
        }
        launchPackageInstaller(prepared)
    }

    private fun launchPackageInstaller(prepared: PreparedAppUpdate) {
        val apkUri = runCatching {
            FileProvider.getUriForFile(this, "$packageName.update-files", prepared.apkFile)
        }.getOrElse {
            Timber.w(it, "Unable to expose update APK")
            pendingUpdate = null
            updatePhase.value = DerbisUpdatePhase.Failed(prepared.versionName)
            return
        }
        updatePhase.value = DerbisUpdatePhase.Installing(prepared.versionName)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            clipData = ClipData.newRawUri("Derbis update", apkUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(installIntent) }
            .onSuccess { updatePhase.value = DerbisUpdatePhase.Idle }
            .onFailure {
                Timber.w(it, "Unable to launch Android package installer")
                updatePhase.value = DerbisUpdatePhase.Failed(prepared.versionName)
            }
        pendingUpdate = null
    }

    private fun showUpdateFailure() {
        val strings = DerbisStrings.forCode(viewModel.state.value.languageCode)
        Toast.makeText(this, strings.cannotOpenDownload, Toast.LENGTH_LONG).show()
    }

    private fun enterPictureInPicture(): Boolean {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return false
        val state = DerbisPipBridge.current()
        if (!state.canEnter) return false
        val params = buildPictureInPictureParams(state)
        setPictureInPictureParams(params)
        return try {
            enterPictureInPictureMode(params)
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: IllegalStateException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun updatePictureInPictureParams(state: DerbisPipBridge.State) {
        setPictureInPictureParams(buildPictureInPictureParams(state))
    }

    private fun buildPictureInPictureParams(state: DerbisPipBridge.State): PictureInPictureParams {
        val aspect = state.aspectRatio.coerceIn(0.42f, 2.39f)
        val denominator = 1000
        val numerator = (aspect * denominator).roundToInt().coerceAtLeast(1)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(numerator, denominator))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder
                .setAutoEnterEnabled(state.videoMode && state.playing)
                .setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun applyOrientationPolicy() {
        val target = if (isInPictureInPictureMode) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        if (requestedOrientation != target) requestedOrientation = target
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private fun requestLegacyStoragePermission() {
        if (Build.VERSION.SDK_INT > 28) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1002)
        }
    }

    private fun configureFastImageLoader() {
        DerbisArtworkCache.configure(this)
    }
}
