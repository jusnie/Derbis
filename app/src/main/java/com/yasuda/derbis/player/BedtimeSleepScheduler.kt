package com.yasuda.derbis.player

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yasuda.derbis.data.DerbisPreferences
import com.yasuda.derbis.domain.DerbisAutomationSettings
import com.yasuda.derbis.domain.nextBedtimeTrigger
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

object BedtimeSleepScheduler {
    private const val REQUEST_CODE = 0x1E5A
    private const val TRIGGER_WINDOW_MS = 60_000L

    fun apply(context: Context, settings: DerbisAutomationSettings) {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = pendingIntent(appContext) ?: return
        manager.cancel(intent)
        val next = nextBedtimeTrigger(settings.bedtime, ZonedDateTime.now()) ?: return
        runCatching {
            manager.setWindow(
                AlarmManager.RTC_WAKEUP,
                next.toInstant().toEpochMilli(),
                TRIGGER_WINDOW_MS,
                intent
            )
        }.onFailure { Timber.w(it, "Bedtime schedule failed") }
    }

    private fun pendingIntent(context: Context): PendingIntent? = runCatching {
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, BedtimeAlarmReceiver::class.java).setAction(BedtimeAlarmReceiver.ACTION_BEDTIME),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }.getOrNull()
}

class BedtimeAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val startTimer = intent.action == ACTION_BEDTIME
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val settings = DerbisPreferences(appContext).automationSettingsFlow.first()
                if (startTimer && settings.bedtime.isArmed) {
                    withContext(Dispatchers.Main) {
                        PlaybackService.startSleepTimer(settings.bedtime.durationMinutes)
                    }
                }
                BedtimeSleepScheduler.apply(appContext, settings)
            } catch (error: Throwable) {
                Timber.w(error, "Bedtime alarm handling failed")
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    companion object {
        const val ACTION_BEDTIME = "com.yasuda.derbis.action.BEDTIME_SLEEP_TIMER"
    }
}
