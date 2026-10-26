package com.luc4n3x.derbis.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.luc4n3x.derbis.data.network.DerbisHttpClientFactory

@UnstableApi
object PlaybackNetworkStack {
    private const val USER_AGENT = "DerbisPlayer/1.13 Android Music"

    fun initialize(context: Context) {
        DerbisHttpClientFactory.media(context.applicationContext)
    }

    fun playbackFactory(context: Context): HttpDataSource.Factory {
        return createOkHttpFactory(context)
    }

    fun warmupFactory(context: Context): HttpDataSource.Factory {
        return createOkHttpFactory(context)
    }

    private fun createOkHttpFactory(context: Context): HttpDataSource.Factory {
        return OkHttpDataSource.Factory(DerbisHttpClientFactory.streaming(context.applicationContext))
            .setUserAgent(USER_AGENT)
    }
}
