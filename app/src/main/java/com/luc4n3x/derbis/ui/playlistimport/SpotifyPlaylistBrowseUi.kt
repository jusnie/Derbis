package com.luc4n3x.derbis.ui.playlistimport

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.derbis.data.spotify.DerbisSpotifyConnection
import com.luc4n3x.derbis.data.spotify.SpotifyConnectionStatus
import com.luc4n3x.derbis.data.spotify.SpotifyPlaylistDetail
import com.luc4n3x.derbis.data.spotify.SpotifyPlaylistSearchResult
import com.luc4n3x.derbis.data.spotify.SpotifyPlaylistTrackPreview
import com.luc4n3x.derbis.ui.theme.DerbisCyan
import com.luc4n3x.derbis.ui.theme.DerbisMuted
import com.luc4n3x.derbis.ui.theme.DerbisOnAccent
import com.luc4n3x.derbis.ui.theme.DerbisPanel
import com.luc4n3x.derbis.ui.theme.DerbisPink
import com.luc4n3x.derbis.ui.theme.DerbisText
import com.luc4n3x.derbis.viewmodel.PlaylistImportController
import com.luc4n3x.derbis.viewmodel.SpotifyPlaylistBrowseUiState

@Composable
internal fun SpotifySearchEntryPoint(controller: PlaylistImportController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val connection by DerbisSpotifyConnection.state.collectAsStateWithLifecycle()
    LaunchedEffect(context) { DerbisSpotifyConnection.initialize(context) }
    Surface(color = DerbisPanel.copy(alpha = 0.72f), shape = ImportShape) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = DerbisCyan.copy(alpha = 0.16f), shape = ImportRowShape) {
                    Icon(
                        Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = DerbisCyan,
                        modifier = Modifier.padding(9.dp).size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Search Spotify playlists", color = DerbisText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        if (connection.isConnected) "Browse Spotify, view available songs, then import."
                        else "Connect Spotify to browse playlists from its catalog.",
                        color = DerbisMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            if (connection.isConnected) {
                OutlinedButton(
                    onClick = controller::openSpotifyBrowse,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = ImportRowShape
                ) { Text("Search Spotify") }
            } else {
                Button(
                    onClick = {
                        val authorization = DerbisSpotifyConnection.begin(context)
                        if (authorization != null) {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, authorization)) }
                                .onFailure { DerbisSpotifyConnection.disconnect(context) }
                        }
                    },
                    enabled = connection.status != SpotifyConnectionStatus.Connecting &&
                        connection.status != SpotifyConnectionStatus.Unavailable,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DerbisCyan, contentColor = DerbisOnAccent),
                    shape = ImportRowShape
                ) {
                    Text(if (connection.status == SpotifyConnectionStatus.Connecting) "Connecting Spotify…" else "Connect Spotify")
                }
                if (connection.detail.isNotBlank()) {
                    Text(connection.detail, color = DerbisMuted, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            Text(
                "Spotify permits song lists here only for playlists you own or collaborate on. You can still paste a playlist link for Spotify or another supported service below.",
                color = DerbisMuted.copy(alpha = 0.82f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
internal fun SpotifyPlaylistBrowseScreen(state: SpotifyPlaylistBrowseUiState, controller: PlaylistImportController) {
    val detail = state.selected
    if (state.selectedPlaylist != null) {
        SpotifyPlaylistDetailScreen(state, detail, controller)
    } else {
        SpotifyPlaylistSearchScreen(state, controller)
    }
}

@Composable
private fun SpotifyPlaylistSearchScreen(state: SpotifyPlaylistBrowseUiState, controller: PlaylistImportController) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp)
        ) {
            ImportTopBar("Search Spotify playlists", controller::closeSpotifyBrowse, "Back to playlist import")
            Text(
                "Search Spotify’s playlist catalog. Song previews and import are available for playlists you own or collaborate on.",
                color = DerbisMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = controller::updateSpotifySearchQuery,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Playlist, curator, or mood") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    colors = spotifySearchFieldColors()
                )
                Button(
                    onClick = controller::searchSpotifyPlaylists,
                    enabled = state.query.trim().length >= 2 && !state.searching,
                    modifier = Modifier.heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DerbisCyan, contentColor = DerbisOnAccent),
                    shape = ImportRowShape
                ) { Text("Search") }
            }
            if (state.error.isNotBlank()) SpotifyBrowseError(state.error)
            if (state.searching) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DerbisCyan, strokeWidth = 2.dp)
                    Text("Searching Spotify…", color = DerbisMuted, fontSize = 13.sp)
                }
            } else if (state.results.isEmpty()) {
                Text(
                    "Search for a playlist to see results here.",
                    color = DerbisMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Text(
                    "Spotify results",
                    color = DerbisText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(state.results, key = SpotifyPlaylistSearchResult::id) { playlist ->
                        SpotifyPlaylistSearchRow(playlist, onClick = { controller.openSpotifyPlaylist(playlist.id) })
                    }
                    item { Spacer(Modifier.size(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SpotifyPlaylistDetailScreen(
    state: SpotifyPlaylistBrowseUiState,
    detail: SpotifyPlaylistDetail?,
    controller: PlaylistImportController
) {
    val header = state.selectedPlaylist ?: return
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp)
        ) {
            item { ImportTopBar("Spotify playlist", controller::closeSpotifyPlaylistPreview, "Back to Spotify results") }
            if (state.loadingPlaylist) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DerbisCyan, strokeWidth = 2.dp)
                        Text("Loading songs from Spotify…", color = DerbisMuted, fontSize = 14.sp)
                    }
                }
            } else {
                item { SpotifyPlaylistHeader(header, detail) }
                if (state.error.isNotBlank()) {
                    item { SpotifyBrowseError(state.error) }
                }
                if (detail != null) {
                    item {
                        Button(
                            onClick = controller::importSpotifyPlaylist,
                            enabled = detail.tracks.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DerbisCyan, contentColor = DerbisOnAccent),
                            shape = ImportShape
                        ) { Text("Import playlist", fontWeight = FontWeight.SemiBold) }
                    }
                    item {
                        Text(
                            "${detail.tracks.size} songs ready to match with Derbis playback sources",
                            color = DerbisMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    itemsIndexed(detail.tracks, key = { index, track -> "$index:${track.title}:${track.artist}" }) { _, track ->
                        SpotifyPlaylistTrackRow(track)
                    }
                    item { Spacer(Modifier.size(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SpotifyPlaylistHeader(header: SpotifyPlaylistSearchResult, detail: SpotifyPlaylistDetail?) {
    Row(
        modifier = Modifier.padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkBox(header.artworkUrl, Modifier.size(104.dp).clip(ImportShape))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("SPOTIFY", color = DerbisCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Text(header.title, color = DerbisText, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            listOfNotNull(
                header.owner.takeIf(String::isNotBlank),
                (detail?.tracks?.size ?: header.trackCount)?.let { "$it songs" }
            ).joinToString(" · ").takeIf(String::isNotBlank)?.let {
                Text(it, color = DerbisMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    if (header.description.isNotBlank()) {
        Text(header.description, color = DerbisMuted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SpotifyPlaylistSearchRow(playlist: SpotifyPlaylistSearchResult, onClick: () -> Unit) {
    Surface(
        color = DerbisPanel.copy(alpha = 0.66f),
        shape = ImportRowShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(ImportRowShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkBox(playlist.artworkUrl, Modifier.size(56.dp).clip(ImportRowShape))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(playlist.title, color = DerbisText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(
                    playlist.owner.takeIf(String::isNotBlank),
                    playlist.trackCount?.let { "$it songs" }
                ).joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, color = DerbisMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (playlist.description.isNotBlank()) {
                    Text(playlist.description, color = DerbisMuted.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SpotifyPlaylistTrackRow(track: SpotifyPlaylistTrackPreview) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkBox(track.artworkUrl, Modifier.size(46.dp).clip(ImportRowShape))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = DerbisText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = listOf(track.artist, track.album).filter(String::isNotBlank).joinToString(" · ")
            if (subtitle.isNotBlank()) Text(subtitle, color = DerbisMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (track.explicit) Text("E", color = DerbisMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(spotifyDuration(track.durationMs), color = DerbisMuted, fontSize = 11.sp)
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
}

@Composable
private fun SpotifyBrowseError(message: String) {
    Row(
        modifier = Modifier.padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = DerbisPink, modifier = Modifier.size(18.dp))
        Text(message, color = DerbisMuted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun spotifySearchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DerbisText,
    unfocusedTextColor = DerbisText,
    focusedBorderColor = DerbisCyan.copy(alpha = 0.7f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
    focusedContainerColor = DerbisPanel.copy(alpha = 0.6f),
    unfocusedContainerColor = DerbisPanel.copy(alpha = 0.45f),
    cursorColor = DerbisCyan
)

private fun spotifyDuration(durationMs: Long): String {
    val seconds = (durationMs / 1_000L).coerceAtLeast(0L)
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
