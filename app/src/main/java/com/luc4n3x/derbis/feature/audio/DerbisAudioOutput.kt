package com.luc4n3x.derbis.feature.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter
import android.media.MediaRouter2
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DerbisAudioOutputRoute(
    val stableKey: String?,
    val displayName: String,
    val type: Int,
    val bluetooth: Boolean,
    val deviceId: Int = -1,
    val sampleRates: List<Int> = emptyList(),
    val channelCounts: List<Int> = emptyList(),
    val encodings: List<Int> = emptyList()
) {
    val routeKey: String get() = stableKey ?: "device:$deviceId:$type"
    val wired: Boolean get() = type in wiredOutputTypes
    val external: Boolean get() = type in externalOutputTypes
    val speaker: Boolean get() = type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
}

enum class DerbisAudioRouteSelectionState {
    Idle,
    Applying,
    Failed
}

data class DerbisAudioOutputState(
    val active: DerbisAudioOutputRoute? = null,
    val connected: List<DerbisAudioOutputRoute> = emptyList(),
    val volumePercent: Int = 0,
    val systemSwitcherAvailable: Boolean = false,
    val directSelectionAvailable: Boolean = false,
    val requestedRouteKey: String? = null,
    val selectionState: DerbisAudioRouteSelectionState = DerbisAudioRouteSelectionState.Idle
)

internal fun isDerbis360HeadphoneOutput(route: DerbisAudioOutputRoute?): Boolean {
    if (route == null || !isDerbis360HeadphoneOutputType(route.type)) return false
    if (!route.bluetooth) return true
    val name = route.displayName.trim().lowercase(Locale.ROOT)
    return name.isNotBlank() && name !in derbis360UnknownBluetoothNames &&
        derbis360SpeakerNameMarkers.none { marker -> name.contains(marker) }
}

internal fun isDerbis360HeadphoneOutputType(type: Int): Boolean = when (type) {
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> true
    else -> false
}

object DerbisAudioOutputRepository {
    private val _state = MutableStateFlow(DerbisAudioOutputState())
    val state: StateFlow<DerbisAudioOutputState> = _state.asStateFlow()

    internal fun publish(state: DerbisAudioOutputState) {
        _state.value = state
    }

    internal fun reset() {
        _state.value = DerbisAudioOutputState()
    }
}

internal fun selectDerbisAudioOutputRoute(
    routes: List<DerbisAudioOutputRoute>,
    systemOrdered: Boolean
): DerbisAudioOutputRoute? {
    if (systemOrdered) return routes.firstOrNull()
    return routes.maxByOrNull { route ->
        when {
            route.bluetooth -> 4
            route.type in wiredOutputTypes -> 3
            route.type in externalOutputTypes -> 2
            route.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 1
            else -> 0
        }
    }
}

internal fun stableDerbisAudioRouteKey(type: Int, address: String?): String? {
    val cleanAddress = address?.trim()?.takeIf(String::isNotBlank) ?: return null
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("$type:$cleanAddress".toByteArray(Charsets.UTF_8))
        .take(12)
        .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
    return "audio-$type-$digest"
}

@Composable
fun rememberDerbisAudioOutputState(): DerbisAudioOutputState {
    val state by DerbisAudioOutputRepository.state.collectAsState()
    return state
}

fun openDerbisSystemOutputSwitcher(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val shown = runCatching {
            MediaRouter2.getInstance(context).showSystemOutputSwitcher()
        }.getOrDefault(false)
        if (shown) return true
    }
    return runCatching {
        context.startActivity(
            Intent(Settings.ACTION_SOUND_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrDefault(false)
}

internal fun queryDerbisAudioOutputState(
    audioManager: AudioManager,
    mediaRouter: MediaRouter,
    directSelectionAvailable: Boolean,
    requestedRouteKey: String?,
    selectionState: DerbisAudioRouteSelectionState
): DerbisAudioOutputState {
    val systemOrdered = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val devices = queryOutputDevices(audioManager)
    val routes = devices
        .filter(AudioDeviceInfo::isSink)
        .map(::toDerbisAudioOutputRoute)
        .distinctBy(DerbisAudioOutputRoute::routeKey)
    val active = if (systemOrdered) {
        val routed = queryRoutedOutputDevices(audioManager)
        routed.firstNotNullOfOrNull { device ->
            routes.firstOrNull { route -> route.deviceId == device.id }
                ?: toDerbisAudioOutputRoute(device)
        }
    } else {
        val selected = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
        resolveDerbisPreTiramisuAudioOutputRoute(routes, selected.name?.toString(), selected.deviceType)
    }
    val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, maximum)
    return DerbisAudioOutputState(
        active = active,
        connected = routes,
        volumePercent = (current.toFloat() / maximum.toFloat() * 100f).toInt().coerceIn(0, 100),
        systemSwitcherAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        directSelectionAvailable = directSelectionAvailable,
        requestedRouteKey = requestedRouteKey,
        selectionState = selectionState
    )
}

private fun queryOutputDevices(audioManager: AudioManager): List<AudioDeviceInfo> =
    runCatching {
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
    }.getOrDefault(emptyList())

private fun queryRoutedOutputDevices(audioManager: AudioManager): List<AudioDeviceInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching {
            audioManager.getAudioDevicesForAttributes(mediaAudioAttributes)
        }.getOrDefault(emptyList())
    } else {
        emptyList()
    }

internal fun findDerbisAudioOutputDevice(
    audioManager: AudioManager,
    routeKey: String
): AudioDeviceInfo? = queryOutputDevices(audioManager)
    .firstOrNull { device -> toDerbisAudioOutputRoute(device).routeKey == routeKey }

internal fun resolveDerbisPreTiramisuAudioOutputRoute(
    routes: List<DerbisAudioOutputRoute>,
    selectedRouteName: String?,
    selectedDeviceType: Int
): DerbisAudioOutputRoute? {
    val cleanSelectedName = selectedRouteName?.trim()
    if (!cleanSelectedName.isNullOrBlank()) {
        routes.firstOrNull { route ->
            route.displayName.equals(cleanSelectedName, ignoreCase = true)
        }?.let { return it }
    }
    return when (selectedDeviceType) {
        MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH -> {
            val bluetoothRoutes = routes.filter { it.bluetooth }
            when (bluetoothRoutes.size) {
                1 -> bluetoothRoutes.single()
                else -> bluetoothRoutes.takeIf { it.isNotEmpty() }?.let {
                    DerbisAudioOutputRoute(
                        stableKey = null,
                        displayName = cleanSelectedName?.ifBlank { "Bluetooth" } ?: "Bluetooth",
                        type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        bluetooth = true
                    )
                }
            }
        }
        MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER ->
            routes.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        else -> {
            val bluetoothRoutes = routes.filter { it.bluetooth }
            if (bluetoothRoutes.size > 1) {
                selectDerbisAudioOutputRoute(routes.filter { !it.bluetooth }, systemOrdered = false)
            } else {
                selectDerbisAudioOutputRoute(routes, systemOrdered = false)
            }
        }
    }
}

internal fun toDerbisAudioOutputRoute(device: AudioDeviceInfo): DerbisAudioOutputRoute {
    val name = device.productName.toString().trim()
    val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching { device.address }.getOrNull().orEmpty()
    } else {
        ""
    }
    return DerbisAudioOutputRoute(
        stableKey = stableDerbisAudioRouteKey(device.type, address),
        displayName = name,
        type = device.type,
        bluetooth = device.type in bluetoothOutputTypes,
        deviceId = device.id,
        sampleRates = device.sampleRates.filter { it > 0 }.distinct().sorted(),
        channelCounts = device.channelCounts.filter { it > 0 }.distinct().sorted(),
        encodings = device.encodings.filter { it > 0 }.distinct().sorted()
    )
}

private val mediaAudioAttributes by lazy {
    AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
}

private val derbis360UnknownBluetoothNames = setOf("bluetooth", "unknown")
private val derbis360SpeakerNameMarkers = listOf(
    "speaker", "soundbar", "television", " tv", "receiver", "homepod", "sonos",
    "boombox", "partybox", "car audio", "android auto", "echo", "nest audio"
)

private val bluetoothOutputTypes = setOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER,
    AudioDeviceInfo.TYPE_BLE_BROADCAST
)

private val wiredOutputTypes = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_LINE_ANALOG,
    AudioDeviceInfo.TYPE_LINE_DIGITAL
)

private val externalOutputTypes = setOf(
    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_HDMI,
    AudioDeviceInfo.TYPE_HDMI_ARC,
    AudioDeviceInfo.TYPE_HDMI_EARC
)
