package com.yasuda.derbis.feature.recognition

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import com.yasuda.derbis.DerbisLaunchActions
import com.yasuda.derbis.MainActivity
import com.yasuda.derbis.data.DerbisPreferences
import com.yasuda.derbis.ui.i18n.DerbisStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class MusicRecognitionTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        renderTile(DerbisRecognitionCenter.get(this).state.value)
        stateJob?.cancel()
        stateJob = scope.launch {
            DerbisRecognitionCenter.get(this@MusicRecognitionTileService).state.collect(::renderTile)
        }
    }

    override fun onStopListening() {
        stateJob?.cancel()
        stateJob = null
        super.onStopListening()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        renderTile(DerbisRecognitionCenter.get(this).state.value)
    }

    override fun onDestroy() {
        stateJob?.cancel()
        stateJob = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        when (DerbisRecognitionCenter.get(this).state.value) {
            RecognitionState.Listening,
            RecognitionState.Identifying -> cancelRecognition()
            else -> startRecognition()
        }
        renderTile(DerbisRecognitionCenter.get(this).state.value)
    }

    private fun startRecognition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collapseAndLaunch(RecognitionProjectionActivity.intent(this))
            return
        }
        if (!hasRecordAudioPermission()) {
            collapseAndLaunch(permissionRequestIntent())
            return
        }
        runCatching { ContextCompat.startForegroundService(this, MusicRecognitionService.microphoneIntent(this)) }
            .onFailure { Timber.w(it, "Recognition service could not start from the tile") }
    }

    private fun cancelRecognition() {
        runCatching { ContextCompat.startForegroundService(this, MusicRecognitionService.cancelIntent(this)) }
            .onFailure { Timber.w(it, "Recognition cancel could not be delivered") }
        DerbisRecognitionCenter.cancel(this)
    }

    private fun renderTile(state: RecognitionState) {
        val tile = qsTile ?: return
        val strings = DerbisStrings.forCode(DerbisPreferences(this).snapshot().languageCode)
        tile.label = strings.recognizeMusic
        val projection = recognitionTileProjection(state)
        tile.state = if (projection.kind == RecognitionTileProjectionKind.Active) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        setStateDescription(
            when (projection.description) {
                RecognitionTileDescription.TapToListen -> strings.recognitionTapToListen
                RecognitionTileDescription.Listening -> strings.recognitionListening
                RecognitionTileDescription.Processing -> strings.recognitionProcessing
                RecognitionTileDescription.Result -> (state as? RecognitionState.Result)?.result?.title
                RecognitionTileDescription.None -> null
            }
        )
        tile.updateTile()
    }

    private fun setStateDescription(value: String?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        qsTile?.stateDescription = value
    }

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun permissionRequestIntent(): Intent = Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        putExtra(DerbisLaunchActions.EXTRA_SHORTCUT, DerbisLaunchActions.SHORTCUT_RECOGNITION)
        putExtra(DerbisLaunchActions.EXTRA_RECOGNITION_PERMISSION_REQUEST, true)
    }

    private fun collapseAndLaunch(intent: Intent) {
        TileServiceCompat.startActivityAndCollapse(
            this,
            PendingIntentActivityWrapper(
                this,
                REQUEST_TILE_LAUNCH,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
                false
            )
        )
    }

    private companion object {
        const val REQUEST_TILE_LAUNCH = 5420
    }
}
