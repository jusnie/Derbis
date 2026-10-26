package com.luc4n3x.derbis

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import com.luc4n3x.derbis.feature.jam.JamSessionCode
import com.luc4n3x.derbis.feature.sharedmedia.SharedMediaIntentParser
import com.luc4n3x.derbis.feature.sharedmedia.SharedMediaRequest

object DerbisLaunchActions {
    private const val EXTRA_SHARED_MEDIA_CONSUMED = "derbis.shared_media_consumed"
    const val EXTRA_SHORTCUT = "derbis.shortcut"
    const val EXTRA_ARTIST = "derbis.open_artist"
    const val EXTRA_RELEASE_ID = "derbis.open_release_id"
    const val EXTRA_RELEASE_TITLE = "derbis.open_release_title"
    const val EXTRA_RELEASE_ARTIST = "derbis.open_release_artist"
    const val EXTRA_RELEASE_ARTWORK = "derbis.open_release_artwork"
    const val EXTRA_RELEASE_YEAR = "derbis.open_release_year"
    const val EXTRA_RECOGNITION_PERMISSION_REQUEST = "derbis.recognition_permission_request"
    const val SHORTCUT_FAVORITES = "favorites"
    const val SHORTCUT_FLOW = "flow"
    const val SHORTCUT_OFFLINE = "offline"
    const val SHORTCUT_LYRICS = "lyrics"
    const val SHORTCUT_SEARCH = "search"
    const val SHORTCUT_LIBRARY = "library"
    const val SHORTCUT_RECOGNITION = "recognition"
    const val SHORTCUT_AMBIENT = "ambient"
    const val SHORTCUT_RESUME = "resume"
    const val SHORTCUT_PLAYER = "player"
    const val SHORTCUT_QUEUE = "queue"

    private val knownShortcuts = setOf(
        SHORTCUT_FAVORITES,
        SHORTCUT_FLOW,
        SHORTCUT_OFFLINE,
        SHORTCUT_LYRICS,
        SHORTCUT_SEARCH,
        SHORTCUT_LIBRARY,
        SHORTCUT_RECOGNITION,
        SHORTCUT_AMBIENT,
        SHORTCUT_RESUME,
        SHORTCUT_PLAYER,
        SHORTCUT_QUEUE
    )

    val pendingShortcut = mutableStateOf<String?>(null)
    val pendingArtist = mutableStateOf<String?>(null)
    val pendingRelease = mutableStateOf<ReleaseLaunchRequest?>(null)
    val pendingSharedMedia = mutableStateOf<SharedMediaRequest?>(null)
    val pendingJamCode = mutableStateOf<String?>(null)

    fun consumeFrom(intent: Intent?) {
        intent ?: return
        val shortcut = intent.getStringExtra(EXTRA_SHORTCUT)
        intent.removeExtra(EXTRA_SHORTCUT)
        shortcut?.takeIf { it in knownShortcuts }?.let { value ->
            pendingShortcut.value = value
        }
        intent.getStringExtra(EXTRA_ARTIST)
            ?.takeIf { it.isNotBlank() && intent.getStringExtra(EXTRA_RELEASE_ID).isNullOrBlank() }
            ?.let { value ->
            pendingArtist.value = value
            intent.removeExtra(EXTRA_ARTIST)
        }
        intent.getStringExtra(EXTRA_RELEASE_ID)?.takeIf { it.isNotBlank() }?.let { browseId ->
            pendingRelease.value = ReleaseLaunchRequest(
                browseId = browseId,
                title = intent.getStringExtra(EXTRA_RELEASE_TITLE).orEmpty(),
                artist = intent.getStringExtra(EXTRA_RELEASE_ARTIST).orEmpty(),
                artworkUrl = intent.getStringExtra(EXTRA_RELEASE_ARTWORK).orEmpty(),
                year = intent.getStringExtra(EXTRA_RELEASE_YEAR).orEmpty()
            )
            intent.removeExtra(EXTRA_RELEASE_ID)
            intent.removeExtra(EXTRA_RELEASE_TITLE)
            intent.removeExtra(EXTRA_RELEASE_ARTIST)
            intent.removeExtra(EXTRA_RELEASE_ARTWORK)
            intent.removeExtra(EXTRA_RELEASE_YEAR)
            intent.removeExtra(EXTRA_ARTIST)
        }
        val jamLink = intent.takeIf { it.action == Intent.ACTION_VIEW }?.data
            ?.takeIf { it.scheme.equals("derbis", ignoreCase = true) && it.host.equals("jam", ignoreCase = true) }
            ?.toString()
        if (jamLink != null && JamSessionCode.parse(jamLink) != null) {
            pendingJamCode.value = jamLink
            intent.setDataAndType(null, null)
            return
        }
        if (
            !intent.getBooleanExtra(EXTRA_SHARED_MEDIA_CONSUMED, false) &&
            (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND_MULTIPLE)
        ) {
            SharedMediaIntentParser.parse(intent)?.let { request ->
                pendingSharedMedia.value = request
                intent.putExtra(EXTRA_SHARED_MEDIA_CONSUMED, true)
                intent.removeExtra(Intent.EXTRA_TEXT)
                intent.removeExtra(Intent.EXTRA_HTML_TEXT)
                intent.removeExtra(Intent.EXTRA_SUBJECT)
                intent.removeExtra(Intent.EXTRA_STREAM)
                intent.clipData = null
                intent.setDataAndType(null, null)
            }
        }
    }
}

data class ReleaseLaunchRequest(
    val browseId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val year: String
)
