package com.yasuda.derbis.viewmodel

import com.yasuda.derbis.domain.DerbisAudioSettings

internal class AudioSettingsPersistenceCoordinator(
    private val write: (DerbisAudioSettings) -> Unit
) {
    private val lock = Any()
    private var pending: DerbisAudioSettings? = null

    fun schedule(value: DerbisAudioSettings) {
        synchronized(lock) { pending = value }
    }

    fun persist(value: DerbisAudioSettings) {
        synchronized(lock) {
            if (pending != value) return
            write(value)
            pending = null
        }
    }

    fun flush() {
        synchronized(lock) {
            pending?.let(write)
            pending = null
        }
    }
}
