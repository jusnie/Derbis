package com.yasuda.derbis.ui.youtubemusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.yasuda.derbis.ui.icons.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yasuda.derbis.data.youtubemusic.YoutubeMusicLibraryEntry
import com.yasuda.derbis.data.youtubemusic.YoutubeMusicLibraryShelf
import com.yasuda.derbis.domain.AlbumHit
import com.yasuda.derbis.domain.ReleaseType
import com.yasuda.derbis.ui.components.DerbisSectionShowAll
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.i18n.youtubeMusicHomeCopy
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisIsNocturne
import com.yasuda.derbis.ui.theme.DerbisIsTorque
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisPanelSoft
import com.yasuda.derbis.viewmodel.youtubemusic.YoutubeMusicAccountError
import java.util.Locale

internal data class YoutubeMusicHomeShelfUi(
    val accountName: String,
    val avatarUrl: String,
    val albums: List<AlbumHit>,
    val loading: Boolean,
    val error: YoutubeMusicAccountError?,
    val onOpen: (AlbumHit) -> Unit,
    val onRefresh: () -> Unit,
    val onShowAll: () -> Unit,
    val onReconnect: () -> Unit
)

internal fun youtubeMusicShelfAlbums(shelf: YoutubeMusicLibraryShelf, createdByYou: String): List<AlbumHit> =
    shelf.playlists.map { entry ->
        entry.asShelfAlbum(subtitle = if (entry.owned) createdByYou else entry.subtitle, type = ReleaseType.Unknown)
    } + shelf.albums.map { entry -> entry.asShelfAlbum(subtitle = entry.subtitle, type = ReleaseType.Album) }

internal fun YoutubeMusicLibraryShelf.entryFor(album: AlbumHit): YoutubeMusicLibraryEntry? =
    playlists.firstOrNull { it.browseId == album.browseId } ?: albums.firstOrNull { it.browseId == album.browseId }

private fun YoutubeMusicLibraryEntry.asShelfAlbum(subtitle: String, type: ReleaseType) = AlbumHit(
    title = title,
    artist = subtitle,
    year = "",
    thumbnailUrl = thumbnailUrl,
    query = listOf(title, this.subtitle).filter(String::isNotBlank).joinToString(" "),
    browseId = browseId,
    releaseType = type
)

@Composable
internal fun YoutubeMusicHomeFooter(ui: YoutubeMusicHomeShelfUi, inset: Dp) {
    val copy = youtubeMusicHomeCopy(LocalDerbisStrings.current.code)
    val upper = DerbisIsTorque || DerbisIsNocturne
    val avatarShape = if (DerbisIsTorque) RoundedCornerShape(2.dp) else CircleShape
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(avatarShape).background(DerbisPanelSoft),
            contentAlignment = Alignment.Center
        ) {
            if (ui.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = ui.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = listOf(YOUTUBE_MUSIC_LABEL, ui.accountName).filter(String::isNotBlank).joinToString(" · ")
                .let { if (upper) it.uppercase(LocalLocale.current.platformLocale) else it },
            color = DerbisMuted,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = listOf(copy.source, ui.accountName).filter(String::isNotBlank).joinToString(", ") }
        )
        IconButton(onClick = ui.onRefresh, enabled = !ui.loading) {
            Icon(Icons.Rounded.Refresh, contentDescription = copy.refresh, tint = DerbisMuted)
        }
        DerbisSectionShowAll(onClick = ui.onShowAll)
    }
}

@Composable
internal fun YoutubeMusicHomeNotice(ui: YoutubeMusicHomeShelfUi, inset: Dp) {
    val error = ui.error ?: return
    val copy = youtubeMusicHomeCopy(LocalDerbisStrings.current.code)
    val signIn = error == YoutubeMusicAccountError.SignIn
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
            .clip(RoundedCornerShape(if (DerbisIsTorque) 2.dp else 16.dp))
            .background(DerbisPanelSoft)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (signIn) copy.sessionExpired else copy.loadFailed,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        TextButton(onClick = if (signIn) ui.onReconnect else ui.onRefresh) {
            Text(if (signIn) copy.reconnect else copy.retry, color = DerbisCyan, fontWeight = FontWeight.Bold)
        }
    }
}

private const val YOUTUBE_MUSIC_LABEL = "YouTube Music"
