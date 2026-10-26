package com.luc4n3x.derbis.feature.cast

import android.content.Context

object RemotePlaybackBackendProvider {
    fun create(context: Context): RemotePlaybackBackend = NoOpCastBackend()
}
