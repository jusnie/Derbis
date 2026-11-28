package com.yasuda.derbis.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.yasuda.derbis.MainActivity
import com.yasuda.derbis.R
import com.yasuda.derbis.domain.Track

/**
 * Whether the lyric line is allowed in the status bar.
 *
 * A plain volatile flag rather than a stored value, because it is read from the playback tick every
 * two seconds and a preference lookup there would mean doing I/O on the audio path. The settings
 * layer writes it once whenever the switch changes, the same way the artwork ceiling is published.
 */
object LyricStatusLineSettings {
    @Volatile
    var enabled: Boolean = false

    fun update(value: Boolean) {
        enabled = value
    }
}

/**
 * The one-line lyric notification.
 *
 * This is deliberately not part of the media notification. The media notification is built by
 * media3 from the current media item's metadata, and rewriting that metadata once per lyric line
 * would mean telling the media session that the track changed several dozen times a song — which
 * thrashes every listener of the session, from the lock screen to Android Auto. A second, silent,
 * ongoing notification carrying only the lyric is a much smaller claim on the system: it has no
 * actions, no audio focus, no session, and it is cancelled the moment playback stops.
 *
 * The channel is IMPORTANCE_LOW with sound, vibration and badges all refused, so it never makes a
 * noise, never buzzes, and never counts as an unread notification.
 */
class LyricStatusNotifier(private val context: Context) {
    private val appContext = context.applicationContext

    /** What is currently drawn, so an unchanged line costs no work at all. */
    private var lastLine: String = ""
    private var lastTrackId: String = ""

    fun update(track: Track, line: String) {
        if (line.isBlank()) {
            clear()
            return
        }
        if (line == lastLine && track.id == lastTrackId) return
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)
        lastLine = line
        lastTrackId = track.id
        runCatching { manager.notify(NOTIFICATION_ID, build(track, line)) }
    }

    fun clear() {
        if (lastLine.isEmpty() && lastTrackId.isEmpty()) return
        lastLine = ""
        lastTrackId = ""
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        runCatching { manager.cancel(NOTIFICATION_ID) }
    }

    private fun build(track: Track, line: String): Notification {
        val open = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val byline = listOf(track.title, track.artist)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_lyrics)
            .setContentTitle(line)
            .setContentText(byline)
            .setStyle(NotificationCompat.BigTextStyle().bigText(line).setSummaryText(byline))
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // onlyAlertOnce plus the silent channel: the line changes, the phone stays quiet.
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setLocalOnly(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Lyric line",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "The current lyric line, shown while a song plays"
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    private companion object {
        const val CHANNEL_ID = "derbis_lyric_line"

        /**
         * A fixed id outside the media notification's range.
         *
         * media3 derives the media notification's id from the session, so a small constant here
         * cannot collide with it — and if some future version does, the lyric notification simply
         * replaces itself rather than the player's.
         */
        const val NOTIFICATION_ID = 0x0DEB1
    }
}
