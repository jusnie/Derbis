package com.yasuda.derbis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.DerbisPersonalOrbit
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.DerbisPlayingIndicator
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.*

@Composable
internal fun SkinOrbitTracks(
    tracks: List<Track>, currentId: String?, playing: Boolean, resolving: Boolean,
    onPlay: (Track) -> Unit, onActions: (Track) -> Unit
) {
    if (DerbisIsTorque) {
        TorqueOrbitTracks(tracks, currentId, playing, resolving, onPlay, onActions)
        return
    }
    if (DerbisIsSpotify) {
        SpotifyTrackCarousel(tracks, currentId, playing, resolving, onPlay, onActions)
        return
    }
    if (DerbisIsIvory) {
        IvoryOrbitShelf(tracks, currentId, playing, resolving, onPlay, onActions)
        return
    }
    val strings = LocalDerbisStrings.current
    val corner = if (DerbisIsNocturne) Nocturne78Design.artworkCorner else PrismDesign.artworkCorner
    LazyRow(
        Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(tracks, key = { index, track -> "$index-${DerbisPersonalOrbit.identityKey(track)}" }) { _, track ->
            val current = track.id == currentId
            Column(
                Modifier.width(156.dp).derbisPressable(
                    onClick = { onPlay(track) }, onLongClick = { onActions(track) },
                    onClickLabel = track.title, onLongClickLabel = strings.songOptions, role = Role.Button
                ), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(corner))) {
                    CoverImage(track, Modifier.fillMaxSize(), highRes = true)
                    if (current) Box(
                        Modifier.align(Alignment.BottomEnd).padding(10.dp).size(32.dp).clip(CircleShape)
                            .background(DerbisBlack.copy(alpha = 0.88f)), contentAlignment = Alignment.Center
                    ) {
                        if (resolving) DiscoveryLoadingIndicator(Modifier.size(16.dp), color = DerbisCyan)
                        else DerbisPlayingIndicator(playing = playing, color = DerbisCyan, size = 14.dp)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(track.title, color = if (current) DerbisCyan else DerbisText,
                        style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = DerbisMuted, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun TorqueOrbitTracks(
    tracks: List<Track>, currentId: String?, playing: Boolean, resolving: Boolean,
    onPlay: (Track) -> Unit, onActions: (Track) -> Unit
) {
    val pages = remember(tracks) { tracks.withIndex().toList().chunked(3) }
    LazyRow(
        Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(pages, key = { pageIndex, page -> "$pageIndex-${DerbisPersonalOrbit.identityKey(page.first().value)}" }) { _, page ->
            Column(Modifier.width(316.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                page.forEach { (index, track) ->
                    TorqueOrbitRow(index, track, track.id == currentId, playing, resolving, onPlay, onActions)
                }
            }
        }
    }
}

@Composable
private fun TorqueOrbitRow(
    index: Int, track: Track, current: Boolean, playing: Boolean, resolving: Boolean,
    onPlay: (Track) -> Unit, onActions: (Track) -> Unit
) {
    val strings = LocalDerbisStrings.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 84.dp).background(DerbisPanel)
            .derbisPressable(onClick = { onPlay(track) }, onLongClick = { onActions(track) },
                onClickLabel = track.title, onLongClickLabel = strings.songOptions, role = Role.Button)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(64.dp)) {
            CoverImage(track, Modifier.fillMaxSize())
            if (current) Box(Modifier.matchParentSize().clip(TorqueStripShape).background(DerbisActivePalette.blue))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(track.title, color = DerbisText, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = DerbisMuted, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
            if (current) {
                if (resolving) DiscoveryLoadingIndicator(Modifier.size(16.dp), color = DerbisCyan)
                else DerbisPlayingIndicator(playing = playing, color = DerbisCyan, size = 16.dp)
            } else Text((index + 1).toString().padStart(2, '0'), color = DerbisMuted,
                fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}
