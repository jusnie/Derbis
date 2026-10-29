package com.yasuda.derbis.feature.cast

import android.content.Context

object RemotePlaybackBackendProvider {
    fun create(context: Context): RemotePlaybackBackend = GoogleCastBackend(context.applicationContext)
}
