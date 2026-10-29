package com.yasuda.derbis.data.youtubemusic

import android.content.Context
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.feature.scrobbling.ScrobbleListen
import com.yasuda.derbis.feature.scrobbling.ScrobbleProvider

internal class YoutubeMusicHistoryProvider(
    context: Context,
    private val videoId: (Track) -> String?,
    private val client: YoutubeMusicAccountClient = YoutubeMusicAccountClient()
) : ScrobbleProvider {
    private val sessions = YoutubeMusicSessionStore(context.applicationContext)

    override val id = "youtube-music-history"

    override fun isConfigured(): Boolean = sessions.load().active != null

    override suspend fun nowPlaying(listen: ScrobbleListen) {
        val session = sessions.load().active ?: return
        val id = videoId(listen.track)?.takeIf(String::isNotBlank) ?: return
        client.registerPlayback(session, id)
    }

    override suspend fun scrobble(listen: ScrobbleListen) = Unit

    override fun clear() = Unit
}
