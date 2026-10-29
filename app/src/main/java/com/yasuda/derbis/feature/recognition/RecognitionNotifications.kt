package com.yasuda.derbis.feature.recognition

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yasuda.derbis.DerbisLaunchActions
import com.yasuda.derbis.MainActivity
import com.yasuda.derbis.R
import com.yasuda.derbis.ui.i18n.DerbisStrings

class RecognitionNotifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.recognition_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun listening(strings: DerbisStrings): Notification = ongoing(strings, strings.recognitionListening)

    fun processing(strings: DerbisStrings): Notification = ongoing(strings, strings.recognitionProcessing)

    fun result(strings: DerbisStrings, result: RecognitionResult): Notification = base(strings)
        .setContentTitle(result.title)
        .setContentText(result.artist)
        .setAutoCancel(true)
        .setContentIntent(openResultIntent())
        .build()

    fun failure(strings: DerbisStrings, state: RecognitionState): Notification = base(strings)
        .setContentTitle(strings.recognizeMusic)
        .setContentText(failureText(strings, state))
        .setAutoCancel(true)
        .setTimeoutAfter(FAILURE_TIMEOUT_MS)
        .setContentIntent(openResultIntent())
        .build()

    @SuppressLint("MissingPermission")
    fun notify(notification: Notification) {
        if (!canPostNotifications()) return
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    @SuppressLint("MissingPermission")
    fun cancel() {
        runCatching { manager.cancel(NOTIFICATION_ID) }
    }

    private fun ongoing(strings: DerbisStrings, text: String): Notification = base(strings)
        .setContentTitle(strings.recognizeMusic)
        .setContentText(text)
        .setOngoing(true)
        .setProgress(0, 0, true)
        .addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_qs_recognize,
                strings.recognitionCancelAction,
                cancelIntent()
            ).build()
        )
        .build()

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun base(strings: DerbisStrings): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qs_recognize)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setTicker(strings.recognizeMusic)

    private fun failureText(strings: DerbisStrings, state: RecognitionState): String = when {
        state is RecognitionState.NoMatch -> strings.recognitionNoMatch
        state is RecognitionState.Error && state.kind == RecognitionErrorKind.PermissionDenied ->
            strings.recognitionPermissionRequired
        state is RecognitionState.Error && state.kind == RecognitionErrorKind.Unavailable ->
            strings.recognitionUnavailable
        else -> strings.recognitionFailed
    }

    private fun cancelIntent(): PendingIntent = PendingIntent.getService(
        context,
        REQUEST_CANCEL,
        MusicRecognitionService.cancelIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openResultIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(DerbisLaunchActions.EXTRA_SHORTCUT, DerbisLaunchActions.SHORTCUT_RECOGNITION)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN_RESULT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "derbis_recognition"
        const val NOTIFICATION_ID = 5417
        private const val REQUEST_CANCEL = 5418
        private const val REQUEST_OPEN_RESULT = 5419
        private const val FAILURE_TIMEOUT_MS = 20_000L
    }
}
