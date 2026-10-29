package com.yasuda.derbis.ui.theme

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.yasuda.derbis.domain.DerbisVisualPerformance

@Immutable
data class DerbisVisualCapabilities(
    val decorativeMotion: Boolean,
    val depthTransitions: Boolean,
    val microMotion: Boolean,
    val heavyBlur: Boolean
) {
    companion object {
        val Full = DerbisVisualCapabilities(
            decorativeMotion = true,
            depthTransitions = true,
            microMotion = true,
            heavyBlur = true
        )
    }
}

val LocalDerbisVisualCapabilities = staticCompositionLocalOf { DerbisVisualCapabilities.Full }

fun resolveVisualPerformance(
    selected: DerbisVisualPerformance,
    lowRamDevice: Boolean,
    powerSaveMode: Boolean
): DerbisVisualPerformance = when (selected) {
    DerbisVisualPerformance.Full -> DerbisVisualPerformance.Full
    DerbisVisualPerformance.Smooth -> DerbisVisualPerformance.Smooth
    DerbisVisualPerformance.Auto -> if (lowRamDevice || powerSaveMode) {
        DerbisVisualPerformance.Smooth
    } else {
        DerbisVisualPerformance.Full
    }
}

fun resolveVisualCapabilities(
    selected: DerbisVisualPerformance,
    animationsEnabled: Boolean,
    lowRamDevice: Boolean,
    powerSaveMode: Boolean
): DerbisVisualCapabilities {
    val full = resolveVisualPerformance(selected, lowRamDevice, powerSaveMode) == DerbisVisualPerformance.Full
    val motion = animationsEnabled && full
    return DerbisVisualCapabilities(
        decorativeMotion = motion,
        depthTransitions = motion,
        microMotion = motion,
        heavyBlur = full
    )
}

@Composable
fun rememberDerbisVisualCapabilities(
    selected: DerbisVisualPerformance,
    animationsEnabled: Boolean
): DerbisVisualCapabilities {
    val context = LocalContext.current.applicationContext
    val lowRamDevice = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val powerSaveMode = rememberPowerSaveMode()
    return remember(selected, animationsEnabled, lowRamDevice, powerSaveMode) {
        resolveVisualCapabilities(selected, animationsEnabled, lowRamDevice, powerSaveMode)
    }
}

@Composable
fun rememberPowerSaveMode(): Boolean {
    val context = LocalContext.current.applicationContext
    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }
    var powerSave by remember(powerManager) { mutableStateOf(powerManager?.isPowerSaveMode == true) }
    DisposableEffect(context, powerManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                powerSave = powerManager?.isPowerSaveMode == true
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        powerSave = powerManager?.isPowerSaveMode == true
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return powerSave
}
