package com.yasuda.derbis.feature.systemintegration

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal fun derbisBackgroundSettingsComponents(family: DerbisDeviceFamily): List<ComponentName> = when (family) {
    DerbisDeviceFamily.XIAOMI -> listOf(
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
    )
    DerbisDeviceFamily.OPLUS -> listOf(
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
    )
    DerbisDeviceFamily.VIVO -> listOf(
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
    )
    DerbisDeviceFamily.SAMSUNG -> listOf(
        ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
    )
    DerbisDeviceFamily.HONOR -> listOf(
        ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
    )
    DerbisDeviceFamily.GOOGLE,
    DerbisDeviceFamily.GENERIC -> emptyList()
}

internal fun openDerbisBackgroundPlaybackSettings(context: Context, family: DerbisDeviceFamily): Boolean {
    val candidates = derbisBackgroundSettingsComponents(family).map { component ->
        Intent().setComponent(component)
    } + Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    return candidates.any { intent ->
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
