package com.yasuda.derbis.feature.systemintegration

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yasuda.derbis.MainActivity
import com.yasuda.derbis.R
import com.yasuda.derbis.domain.DerbisLanguageCatalog
import com.yasuda.derbis.ui.i18n.deviceIntegrationCopy

internal const val DERBIS_ISLAND_CHANNEL_ID = "derbis_device_island"
internal const val DERBIS_ISLAND_LOG_TAG = "DerbisDeviceIsland"
private const val VIVO_ISLAND_NOTIFICATION_ID = 0x4c564956
private const val VIVO_REMOVE_TIMEOUT_MS = 5_000L
private const val VIVO_MAX_DURATION_MS = 8L * 60L * 60L * 1_000L

private const val VIVO_OPERATION = "notification.superx.operation"
private const val VIVO_SHOW_NOTIFY = "notification.superx.showNotify"
private const val VIVO_TEMPLATE = "notification.superx.template"
private const val VIVO_CLICK_RESP = "notification.superx.clickResp"
private const val VIVO_SCENE = "notification.superx.scene"
private const val VIVO_BASE_INFOS = "notification.superx.baseInfos"
private const val VIVO_BASE_ICON = "notification.superx.baseInfos.icon"
private const val VIVO_BASE_TITLE = "notification.superx.baseInfos.title"
private const val VIVO_BASE_CONTENT = "notification.superx.baseInfos.content"
private const val HYPER_OS_FOCUS_PARAM = "miui.focus.param"

internal interface DerbisIslandAdapter {
    val feature: DerbisDeviceFeature
    fun publish(snapshot: DerbisIslandSnapshot)
    fun clear()
}

internal class HyperOsSuperIslandAdapter(private val context: Context) : DerbisIslandAdapter {
    override val feature = DerbisDeviceFeature.SUPER_ISLAND
    private var publishedKey: String? = null
    private var blockedKey: String? = null

    override fun publish(snapshot: DerbisIslandSnapshot) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            clear()
            return
        }
        val showing = DerbisSuperIslandService.isShowing
        if (!snapshot.active && !showing) return
        val content = hyperOsSuperIslandContent(snapshot)
        val key = hyperOsSuperIslandKey(snapshot, content)
        val sessionKey = "${snapshot.mediaId}\u0000${snapshot.active}"
        if (showing && key == publishedKey) return
        if (!showing && sessionKey == blockedKey) return
        val param = hyperOsSuperIslandFocusParam(content, snapshot.progressPercent, context.packageName)
        ensureDerbisIslandChannel(context)
        val notification = Notification.Builder(context, DERBIS_ISLAND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_lyrics)
            .setContentTitle(content.primary)
            .setContentText(content.detail)
            .setContentIntent(derbisIslandContentIntent(context))
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setOngoing(snapshot.active)
            .addExtras(Bundle().apply { putString(HYPER_OS_FOCUS_PARAM, param) })
            .build()
        if (DerbisSuperIslandService.show(context, notification)) {
            publishedKey = key
            blockedKey = null
        } else {
            publishedKey = null
            blockedKey = sessionKey
        }
    }

    override fun clear() {
        publishedKey = null
        blockedKey = null
        DerbisSuperIslandService.hide(context)
    }
}

internal class VivoAtomicIslandAdapter(private val context: Context) : DerbisIslandAdapter {
    override val feature = DerbisDeviceFeature.ATOMIC_ISLAND
    private var activeMediaId: String? = null
    private var publishedKey: String? = null

    override fun publish(snapshot: DerbisIslandSnapshot) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            clear()
            return
        }
        if (activeMediaId == null && !snapshot.active) return
        val key = vivoAtomicIslandKey(snapshot, System.currentTimeMillis())
        if (key == publishedKey) return
        val content = vivoAtomicIslandContent(snapshot, activeMediaId)
        notify(manager, content)
        activeMediaId = snapshot.mediaId
        publishedKey = key
    }

    override fun clear() {
        if (activeMediaId == null) return
        val manager = NotificationManagerCompat.from(context)
        runCatching {
            notify(
                manager,
                VivoAtomicIslandContent(VivoAtomicIslandOperation.REMOVE, "", "", playing = false, elapsedMs = 0L)
            )
        }.onFailure { runCatching { manager.cancel(VIVO_ISLAND_NOTIFICATION_ID) } }
        activeMediaId = null
        publishedKey = null
    }

    @Suppress("MissingPermission")
    private fun notify(manager: NotificationManagerCompat, content: VivoAtomicIslandContent) {
        ensureDerbisIslandChannel(context)
        val removing = content.operation == VivoAtomicIslandOperation.REMOVE
        val contentIntent = derbisIslandContentIntent(context)
        val extras = Bundle().apply {
            putInt(VIVO_OPERATION, content.operation.code)
            putBoolean(VIVO_SHOW_NOTIFY, true)
            putInt(VIVO_TEMPLATE, 2)
            putString(VIVO_SCENE, "MUSIC_PLAYBACK")
            if (!removing) putParcelable(VIVO_CLICK_RESP, contentIntent)
            putBundle(
                VIVO_BASE_INFOS,
                Bundle().apply {
                    putParcelable(VIVO_BASE_ICON, Icon.createWithResource(context, R.drawable.ic_widget_playing))
                    putCharSequence(VIVO_BASE_TITLE, content.title)
                    putCharSequence(VIVO_BASE_CONTENT, content.content)
                }
            )
        }
        val builder = NotificationCompat.Builder(context, DERBIS_ISLAND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_playing)
            .setContentTitle(content.title)
            .setContentText(content.content)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setLocalOnly(true)
            .setOngoing(!removing && content.playing)
            .setTimeoutAfter(if (removing) VIVO_REMOVE_TIMEOUT_MS else VIVO_MAX_DURATION_MS)
            .addExtras(extras)
        if (!removing && content.chronometer) {
            builder
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() - content.elapsedMs)
        } else {
            builder.setShowWhen(false)
        }
        manager.notify(VIVO_ISLAND_NOTIFICATION_ID, builder.build())
    }
}

internal fun ensureDerbisIslandChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(DERBIS_ISLAND_CHANNEL_ID) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            DERBIS_ISLAND_CHANNEL_ID,
            deviceIntegrationCopy(DerbisLanguageCatalog.deviceDefault()).title,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
    )
}

private fun derbisIslandContentIntent(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

internal fun logIslandFailure(feature: DerbisDeviceFeature, error: Throwable) {
    Log.w(DERBIS_ISLAND_LOG_TAG, "$feature disabled after ${error.javaClass.simpleName}")
}
