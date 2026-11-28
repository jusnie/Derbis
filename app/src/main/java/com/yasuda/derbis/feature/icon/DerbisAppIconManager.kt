package com.yasuda.derbis.feature.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

enum class DerbisAppIcon(
    val storageId: String,
    val label: String,
    val aliasClassName: String,
    val mascotId: String? = null
) {
    Original("original", "Original", "com.yasuda.derbis.LauncherOriginal"),
    Pixelated("pixel", "Pixelated", "com.yasuda.derbis.LauncherPixelated"),
    Bear("bear", "Bear", "com.yasuda.derbis.LauncherBear", mascotId = "bear"),
    Bunny("bunny", "Bunny", "com.yasuda.derbis.LauncherBunny", mascotId = "bunny"),
    Cat("cat", "Cat", "com.yasuda.derbis.LauncherCat", mascotId = "cat"),
    Chick("chick", "Chick", "com.yasuda.derbis.LauncherChick", mascotId = "chick"),
    Frog("frog", "Frog", "com.yasuda.derbis.LauncherFrog", mascotId = "frog"),
    Penguin("penguin", "Penguin", "com.yasuda.derbis.LauncherPenguin", mascotId = "penguin"),
    Sheep("sheep", "Sheep", "com.yasuda.derbis.LauncherSheep", mascotId = "sheep"),
    Cloud("cloud", "Cloud", "com.yasuda.derbis.LauncherCloud", mascotId = "cloud");

    companion object {
        fun from(value: String?): DerbisAppIcon =
            entries.firstOrNull { it.storageId == value } ?: Original
    }
}

object DerbisAppIconManager {
    private const val PREFS_NAME = "derbis_app_icon_prefs"
    private const val KEY_SELECTED_ICON = "selected_app_icon"

    private val _selected = MutableStateFlow(DerbisAppIcon.Original)
    val selected: StateFlow<DerbisAppIcon> = _selected.asStateFlow()

    @Volatile
    private var initialized = false

    fun initialize(context: Context): DerbisAppIcon {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_SELECTED_ICON, null)
        val resolved = if (stored != null) {
            DerbisAppIcon.from(stored)
        } else {
            detectEnabledAlias(appContext)
        }
        _selected.value = resolved
        initialized = true
        return resolved
    }

    fun current(context: Context): DerbisAppIcon {
        if (!initialized) return initialize(context)
        return _selected.value
    }

    fun select(context: Context, icon: DerbisAppIcon) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_ICON, icon.storageId)
            .apply()
        _selected.value = icon
        initialized = true
        applyLauncherAlias(appContext, icon)
    }

    private fun detectEnabledAlias(context: Context): DerbisAppIcon {
        val pm = context.packageManager
        val pkg = context.packageName
        for (candidate in DerbisAppIcon.entries) {
            val state = runCatching {
                pm.getComponentEnabledSetting(ComponentName(pkg, candidate.aliasClassName))
            }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return candidate
            }
        }
        return DerbisAppIcon.Original
    }

    private fun applyLauncherAlias(context: Context, chosen: DerbisAppIcon) {
        val pm = context.packageManager
        val pkg = context.packageName
        runCatching {
            // Enable the chosen alias before disabling the rest so the package never has zero
            // enabled LAUNCHER components during the transition.
            val targetState = if (chosen == DerbisAppIcon.Original) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
            pm.setComponentEnabledSetting(
                ComponentName(pkg, chosen.aliasClassName),
                targetState,
                PackageManager.DONT_KILL_APP
            )
            for (entry in DerbisAppIcon.entries) {
                if (entry == chosen) continue
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, entry.aliasClassName),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }.onFailure { error ->
            Timber.w(error, "Failed to switch launcher icon alias to %s", chosen.storageId)
        }
    }
}
