package com.yasuda.derbis.feature.cast

import com.yasuda.derbis.domain.RepeatMode

data class RemoteDevice(
    val id: String,
    val name: String
)

enum class RemotePlaybackAvailability {
    Unavailable,
    Idle,
    Connecting,
    Connected
}

data class RemotePlaybackState(
    val availability: RemotePlaybackAvailability = RemotePlaybackAvailability.Unavailable,
    val connected: Boolean = false,
    val deviceName: String? = null,
    val queueIds: List<String> = emptyList(),
    val currentIndex: Int = -1,
    val positionMs: Long = 0L,
    val playing: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off
)
