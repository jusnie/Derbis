package com.yasuda.derbis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import com.yasuda.derbis.ui.icons.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.ResonanceCommentSnippet
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.DerbisGlassIntensity
import com.yasuda.derbis.ui.components.DerbisPlayingIndicator
import com.yasuda.derbis.ui.components.derbisGlass
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.theme.*

internal val TorqueTagShape = GenericShape { size, _ ->
    val slant = size.height * 0.35f
    moveTo(slant, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width - slant, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
internal fun SkinSectionTitle(title: String, inset: Dp, actions: @Composable RowScope.() -> Unit = {}) {
    if (DerbisIsSpotify) {
        SpotifySectionTitle(title, inset, actions)
        return
    }
    if (DerbisIsIvory) {
        IvorySectionTitle(title, inset, actions)
        return
    }
    val torque = DerbisIsTorque
    Row(Modifier.fillMaxWidth().padding(horizontal = inset).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (torque) Box(Modifier.width(4.dp).height(22.dp).background(DerbisActivePalette.blue))
        Text(
            if (torque) title.uppercase() else title, color = DerbisText, style = MaterialTheme.typography.titleLarge,
            fontSize = if (torque) 18.sp else 22.sp, fontWeight = if (torque) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = if (torque) 2.sp else (-0.4).sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        actions()
    }
}

@Composable
private fun SkinPlayAll(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(onClickLabel = label, role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { PrismPlayDisc(playing = false, resolving = false, size = 36.dp) }
}

@Composable
internal fun TorqueRank(rank: Int, highlight: Boolean) {
    Box(
        Modifier.width(44.dp).height(26.dp).clip(TorqueTagShape).background(if (highlight) DerbisActivePalette.blue else DerbisPanelSoft),
        contentAlignment = Alignment.Center
    ) { Text(rank.toString().padStart(2, '0'), color = if (highlight) Color.White else DerbisText, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
}

@Composable
internal fun SkinChartSection(
    title: String, tracks: List<Track>, currentId: String?, isPlaying: Boolean,
    playAllLabel: String, changeMarketLabel: String, inset: Dp,
    onPlay: (Track) -> Unit, onPlayAll: () -> Unit, onChangeMarket: () -> Unit, modifier: Modifier = Modifier
) {
    val torque = DerbisIsTorque
    val ranked = remember(tracks) { tracks.take(50).withIndex().toList() }
    val pages = remember(ranked) { ranked.chunked(5) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkinSectionTitle(title, inset) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(onClickLabel = changeMarketLabel, role = Role.Button, onClick = onChangeMarket)
                    .semantics { contentDescription = changeMarketLabel },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Public, null, tint = DerbisMuted) }
            SkinPlayAll(playAllLabel, onPlayAll)
        }
        if (DerbisIsSpotify) SpotifyChartShelf(ranked, currentId, isPlaying, inset, onPlay)
        else if (DerbisIsIvory) IvoryChartShelf(ranked, currentId, isPlaying, inset, onPlay)
        else LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(pages, key = { index, _ -> "chart-page-$index" }) { _, page ->
                if (torque) Column(Modifier.width(318.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    page.forEach { (index, track) -> SkinChartRow(index, track, currentId, isPlaying, onPlay) }
                } else if (DerbisIsIvory) IvoryChartPage(page, currentId, isPlaying, onPlay)
                else PrismChartPage(page, currentId, isPlaying, onPlay)
            }
        }
    }
}

@Composable
internal fun SkinResonanceSection(
    title: String, tracks: List<Track>, comments: Map<String, ResonanceCommentSnippet>, currentId: String?, isPlaying: Boolean,
    playLabel: String, inset: Dp, onPlay: (Track) -> Unit, onOpenComments: (Track) -> Unit, modifier: Modifier = Modifier
) {
    val torque = DerbisIsTorque
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkinSectionTitle(title, inset)
        LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(tracks, key = { _, track -> "resonance-${track.id}" }) { _, track ->
                val snippet = comments[track.id]
                val current = track.id == currentId
                Column(
                    Modifier.width(280.dp).heightIn(min = 196.dp).clip(if (torque) RoundedCornerShape(2.dp) else PrismDesign.cut(PrismDesign.surfaceCorner))
                        .background(DerbisPanel)
                        .derbisPressable(onClick = { onOpenComments(track) }, onClickLabel = title, role = Role.Button)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(PrismDesign.tileCorner))) { CoverImage(track, Modifier.fillMaxSize()) }
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (torque) track.title.uppercase() else track.title, color = if (current) DerbisCyan else DerbisText,
                                style = MaterialTheme.typography.titleSmall, fontWeight = if (torque) FontWeight.Bold else null,
                                letterSpacing = if (torque) 1.sp else 0.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(track.artist, color = DerbisMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).clickable(onClickLabel = playLabel, role = Role.Button) { onPlay(track) }.semantics { contentDescription = playLabel },
                            contentAlignment = Alignment.Center
                        ) { PrismPlayDisc(playing = current && isPlaying, resolving = false, size = 34.dp) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.width(if (torque) 4.dp else 3.dp).height(54.dp).background(PrismAccent.gradient()))
                        Text(
                            snippet?.text?.takeIf { it.isNotBlank() } ?: track.artist, color = DerbisText.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, null, tint = DerbisCyan, modifier = Modifier.size(16.dp))
                        Text(
                            listOfNotNull(snippet?.countText?.takeIf { it.isNotBlank() }, snippet?.author?.takeIf { it.isNotBlank() }).joinToString(" · "),
                            color = DerbisMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun Track.leadArtistKey(): String = artist.split(',', '&', '·').first().trim().lowercase()

internal fun distinctCollectionCovers(collections: List<StudioCollection>, perCard: Int): Map<String, List<Track>> {
    val usedArtists = mutableSetOf<String>()
    val usedArtwork = mutableSetOf<String>()
    return collections.associate { collection ->
        val picked = mutableListOf<Track>()
        val cardArtists = mutableSetOf<String>()
        fun take(predicate: (Track) -> Boolean) {
            collection.tracks.forEach { track ->
                if (picked.size >= perCard) return
                val artist = track.leadArtistKey()
                val artwork = track.thumbnailUrl
                if (track in picked || artwork in usedArtwork || !predicate(track)) return@forEach
                picked += track
                cardArtists += artist
                usedArtists += artist
                if (artwork.isNotBlank()) usedArtwork += artwork
            }
        }
        take { it.leadArtistKey() !in usedArtists && it.leadArtistKey() !in cardArtists }
        take { it.leadArtistKey() !in cardArtists }
        take { true }
        collection.id to picked.ifEmpty { collection.tracks.take(perCard) }
    }
}

@Composable
internal fun SkinCollectionsShelf(title: String, collections: List<StudioCollection>, inset: Dp, onOpen: (String) -> Unit) {
    val torque = DerbisIsTorque
    val fourUp = torque || DerbisIsIvory || DerbisIsSpotify
    val covers = remember(collections, fourUp) { distinctCollectionCovers(collections, if (fourUp) 4 else 3) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkinSectionTitle(title, inset)
        LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(if (DerbisIsSpotify) SpotifyDesign.gutter else 14.dp)) {
            itemsIndexed(collections, key = { _, collection -> "collection-${collection.id}" }) { _, collection ->
                val tracks = covers[collection.id].orEmpty()
                when {
                    torque -> TorqueCollectionCard(collection, tracks, onOpen)
                    DerbisIsIvory -> IvoryCollectionCard(collection, tracks, onOpen)
                    DerbisIsSpotify -> SpotifyMixCard(collection, tracks, onOpen)
                    DerbisIsLiquidGlass -> LiquidGlassCollectionCard(collection, tracks, onOpen)
                    else -> PrismCollectionFanCard(collection, tracks, onOpen)
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassCollectionCard(collection: StudioCollection, tracks: List<Track>, onOpen: (String) -> Unit) {
    Column(
        Modifier.width(210.dp).derbisPressable(onClick = { onOpen(collection.id) }, onClickLabel = collection.title, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            tracks.indices.reversed().forEach { index ->
                val angle = when (index) { 0 -> 0f; 1 -> -7f; else -> 7f }
                val shift = when (index) { 0 -> 0.dp; 1 -> (-32).dp; else -> 32.dp }
                val size = if (index == 0) 150.dp else 126.dp
                val coverShape = RoundedCornerShape(22.dp)
                Box(
                    Modifier
                        .offset(x = shift, y = if (index == 0) 0.dp else 10.dp)
                        .size(size)
                        .rotate(angle)
                        .clip(coverShape)
                        .border(1.dp, Color.White.copy(alpha = 0.36f), coverShape)
                ) { CoverImage(tracks[index], Modifier.fillMaxSize()) }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(116.dp)
                    .height(6.dp)
                    .derbisGlass(
                        shape = RoundedCornerShape(3.dp),
                        baseTint = DerbisCyan,
                        fallbackColor = DerbisCyan.copy(alpha = 0.44f),
                        fallbackBorderColor = Color.White.copy(alpha = 0.42f),
                        intensity = DerbisGlassIntensity.Subtle,
                        artworkTint = DerbisActivePalette.cyan
                    )
            )
        }
        Text(collection.title, color = DerbisText, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(collection.subtitle, color = DerbisMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PrismCollectionFanCard(collection: StudioCollection, tracks: List<Track>, onOpen: (String) -> Unit) {
    Column(
        Modifier.width(210.dp).derbisPressable(onClick = { onOpen(collection.id) }, onClickLabel = collection.title, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            tracks.indices.reversed().forEach { index ->
                val angle = when (index) { 0 -> 0f; 1 -> -8f; else -> 8f }
                val shift = when (index) { 0 -> 0.dp; 1 -> (-34).dp; else -> 34.dp }
                Box(
                    Modifier.offset(x = shift, y = if (index == 0) 0.dp else 10.dp).size(if (index == 0) 150.dp else 126.dp).rotate(angle)
                        .clip(RoundedCornerShape(PrismDesign.controlCorner))
                ) { CoverImage(tracks[index], Modifier.fillMaxSize()) }
            }
            Box(Modifier.align(Alignment.BottomCenter).width(120.dp).height(3.dp).background(PrismAccent.gradient(), RoundedCornerShape(2.dp)))
        }
        Text(collection.title, color = DerbisText, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(collection.subtitle, color = DerbisMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TorqueCollectionCard(collection: StudioCollection, tracks: List<Track>, onOpen: (String) -> Unit) {
    Box(
        Modifier.size(204.dp).background(DerbisPanel)
            .derbisPressable(onClick = { onOpen(collection.id) }, onClickLabel = collection.title, role = Role.Button)
    ) {
        Column(Modifier.fillMaxSize()) {
            tracks.chunked(2).forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    row.forEach { track -> Box(Modifier.weight(1f).fillMaxHeight()) { CoverImage(track, Modifier.fillMaxSize()) } }
                }
            }
        }
        Box(Modifier.fillMaxSize().clip(TorqueRiseShape).background(DerbisActivePalette.blue))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Text(collection.title.uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(collection.subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun SkinShowcaseShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    if (DerbisIsSpotify) {
        SpotifyCarousel(items, isPlaying, inset, modifier)
        return
    }
    if (DerbisIsIvory) {
        IvoryShowcaseShelf(items, isPlaying, inset, modifier)
        return
    }
    val torque = DerbisIsTorque
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(if (torque) 10.dp else 16.dp)) {
        itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
            when {
                torque && item.round -> TorqueArtistCard(item)
                torque -> TorqueAlbumCard(item, index, isPlaying)
                DerbisIsLiquidGlass && item.round -> LiquidGlassArtistOrb(item)
                DerbisIsLiquidGlass -> LiquidGlassAlbumCard(item, isPlaying)
                item.round -> PrismArtistOrb(item)
                else -> PrismAlbumCard(item, isPlaying)
            }
        }
    }
}

@Composable
private fun LiquidGlassArtistOrb(item: StudioShelfItem) {
    Column(
        Modifier.width(132.dp).derbisPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(128.dp)
                .clip(CircleShape)
                .derbisGlass(
                    shape = CircleShape,
                    baseTint = Color.White,
                    fallbackColor = DerbisPanelSoft.copy(alpha = 0.72f),
                    fallbackBorderColor = Color.White.copy(alpha = 0.46f),
                    intensity = DerbisGlassIntensity.Standard,
                    artworkTint = DerbisActivePalette.cyan
                )
                .padding(3.dp)
                .clip(CircleShape)
        ) {
            item.artwork(Modifier.fillMaxSize(), true)
        }
        Text(item.title, color = if (item.current) DerbisCyan else DerbisText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LiquidGlassAlbumCard(item: StudioShelfItem, isPlaying: Boolean) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.width(164.dp).derbisPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .border(1.dp, Color.White.copy(alpha = 0.30f), shape)
        ) {
            item.artwork(Modifier.fillMaxSize(), false)
            if (item.current) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .derbisGlass(
                            shape = CircleShape,
                            baseTint = DerbisCyan,
                            fallbackColor = DerbisCyan.copy(alpha = 0.42f),
                            fallbackBorderColor = Color.White.copy(alpha = 0.44f),
                            intensity = DerbisGlassIntensity.Standard,
                            artworkTint = DerbisActivePalette.cyan
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    DerbisPlayingIndicator(playing = isPlaying, color = Color.White, size = 12.dp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(item.title, color = if (item.current) DerbisCyan else DerbisText, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (item.subtitle.isNotBlank()) Text(item.subtitle, color = DerbisMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PrismArtistOrb(item: StudioShelfItem) {
    Column(
        Modifier.width(132.dp).derbisPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(128.dp).clip(CircleShape).background(PrismAccent.gradient()).padding(3.dp).clip(CircleShape).background(DerbisBlack).padding(3.dp).clip(CircleShape)) {
            item.artwork(Modifier.fillMaxSize(), true)
        }
        Text(item.title, color = if (item.current) DerbisCyan else DerbisText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PrismAlbumCard(item: StudioShelfItem, isPlaying: Boolean) {
    Column(
        Modifier.width(164.dp).derbisPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.86f).height(18.dp).offset(y = 8.dp)
                    .background(PrismAccent.gradient(), RoundedCornerShape(PrismDesign.artworkCorner))
            )
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(PrismDesign.artworkCorner))) {
                item.artwork(Modifier.fillMaxSize(), false)
                if (item.current) Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).size(28.dp).clip(CircleShape).background(PrismAccent.gradient()), contentAlignment = Alignment.Center) {
                    DerbisPlayingIndicator(playing = isPlaying, color = Color.White, size = 12.dp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(item.title, color = if (item.current) DerbisCyan else DerbisText, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (item.subtitle.isNotBlank()) Text(item.subtitle, color = DerbisMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TorqueAlbumCard(item: StudioShelfItem, index: Int, isPlaying: Boolean) {
    Column(
        Modifier.width(170.dp).background(DerbisPanel)
            .derbisPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            item.artwork(Modifier.fillMaxSize(), false)
            Box(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 8.dp)) { TorqueRank(index + 1, item.current) }
            if (item.current) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                DerbisPlayingIndicator(playing = isPlaying, color = DerbisActivePalette.blue, size = 12.dp)
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(DerbisActivePalette.blue))
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(item.title.uppercase(), color = DerbisText, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.subtitle.isNotBlank()) Text(item.subtitle, color = DerbisMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TorqueArtistCard(item: StudioShelfItem) {
    Box(
        Modifier.width(150.dp).height(212.dp).background(DerbisPanel)
            .derbisPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button)
    ) {
        item.artwork(Modifier.fillMaxSize(), false)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f))))
        Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(DerbisActivePalette.blue))
        Text(
            item.title.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
        )
    }
}

@Composable
internal fun SkinCategoryCard(title: String, artworkUrl: String, modifier: Modifier, onLongClick: (() -> Unit)?, onClick: () -> Unit) {
    if (DerbisIsSpotify) {
        SpotifyBrowseTile(title, artworkUrl, modifier, onLongClick = onLongClick, onClick = onClick)
        return
    }
    if (DerbisIsIvory) {
        IvoryCategoryCard(title, artworkUrl, modifier, onLongClick, onClick)
        return
    }
    val torque = DerbisIsTorque
    Box(
        modifier.clip(if (torque) RoundedCornerShape(2.dp) else PrismDesign.cut(PrismDesign.surfaceCorner)).background(DerbisPanel)
            .derbisPressable(onClick = onClick, onLongClick = onLongClick, onClickLabel = title, role = Role.Button)
    ) {
        if (artworkUrl.isNotBlank()) {
            coil3.compose.AsyncImage(
                artworkUrl, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = if (torque) Modifier.fillMaxSize() else Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.62f)
            )
        }
        if (torque) {
            Box(Modifier.fillMaxSize().clip(TorqueStripShape).background(DerbisActivePalette.blue))
            Text(
                title.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0.32f to DerbisPanel, 0.62f to DerbisPanel.copy(alpha = 0.55f), 1f to Color.Transparent)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to DerbisCyan.copy(alpha = 0.18f))))
            Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.62f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = DerbisText, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.3).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Box(Modifier.width(24.dp).height(3.dp).background(PrismAccent.gradient(), RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun SkinChartRow(index: Int, track: Track, currentId: String?, isPlaying: Boolean, onPlay: (Track) -> Unit) {
    val torque = DerbisIsTorque
    val current = track.id == currentId
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(PrismDesign.controlCorner))
            .background(if (torque) { if (current) DerbisPanelSoft else DerbisPanel } else if (current) PrismAccent.Tint else Color.Transparent)
            .derbisPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (torque) TorqueRank(index + 1, index < 3)
        else Text(
            (index + 1).toString(), modifier = Modifier.width(34.dp),
            style = TextStyle(brush = if (index < 3) PrismAccent.gradient() else SolidColor(DerbisMuted), fontSize = 26.sp, fontWeight = FontWeight.Light)
        )
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(PrismDesign.tileCorner))) { CoverImage(track, Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = if (current) DerbisCyan else DerbisText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = DerbisMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (current) DerbisPlayingIndicator(playing = isPlaying, color = DerbisCyan, size = 12.dp)
    }
}

@Composable
private fun PrismChartPage(page: List<IndexedValue<Track>>, currentId: String?, playing: Boolean, onPlay: (Track) -> Unit) {
    Column(
        Modifier.width(330.dp).clip(PrismDesign.cut(24.dp)).background(DerbisPanel)
    ) {
        Box(Modifier.padding(start = 18.dp).width(48.dp).height(2.dp).background(PrismAccent.gradient()))
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            page.forEach { (index, track) ->
                PrismChartTrack(index, track, track.id == currentId, playing, onPlay)
            }
        }
    }
}

@Composable
private fun PrismChartTrack(index: Int, track: Track, current: Boolean, playing: Boolean, onPlay: (Track) -> Unit) {
    val first = index == 0
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(PrismDesign.tileCorner))
            .background(if (current) PrismAccent.Tint else Color.Transparent)
            .derbisPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            (index + 1).toString().padStart(2, '0'), color = if (first) DerbisCyan else DerbisMuted,
            fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(24.dp)
        )
        Box(Modifier.size(if (first) 72.dp else 48.dp).clip(RoundedCornerShape(PrismDesign.tileCorner))) {
            CoverImage(track, Modifier.fillMaxSize(), highRes = first)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(track.title, color = if (current) DerbisCyan else DerbisText,
                style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = DerbisMuted, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (current) DerbisPlayingIndicator(playing = playing, color = DerbisCyan, size = 12.dp)
    }
}

@Composable
internal fun SkinNameGreeting(greeting: String, dayPart: String, name: String, modifier: Modifier = Modifier) {
    val accessible = Modifier.clearAndSetSemantics { contentDescription = greeting; heading() }
    when {
        DerbisIsSpotify -> SpotifyNameGreeting(greeting, modifier)
        DerbisIsIvory -> Column(modifier.then(accessible)) {
            Text(dayPart, color = DerbisMuted, style = MaterialTheme.typography.bodyLarge, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(name, color = DerbisText, fontSize = 46.sp, lineHeight = 50.sp, fontWeight = FontWeight.Light, letterSpacing = (-1.2).sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Box(Modifier.padding(start = 4.dp, bottom = 12.dp).size(10.dp).clip(CircleShape).background(IvoryDesign.rose))
            }
        }
        DerbisIsTorque -> Column(modifier.then(accessible), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(dayPart.uppercase(), color = DerbisMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(name.uppercase(), color = DerbisText, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.width(48.dp).height(4.dp).clip(TorqueTagShape).background(DerbisActivePalette.blue))
        }
        DerbisIsLiquidGlass -> Column(modifier.then(accessible)) {
            Text(dayPart, color = DerbisMuted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                name, color = DerbisText,
                fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Normal, letterSpacing = (-1.4).sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        DerbisIsPrism -> Column(modifier.then(accessible)) {
            Text(dayPart, color = DerbisMuted, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                name, style = TextStyle(brush = Brush.horizontalGradient(listOf(DerbisText, DerbisText, DerbisPink))),
                fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Normal, letterSpacing = (-1.4).sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        else -> Column(modifier.then(accessible), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(dayPart.uppercase(), color = DerbisCyan, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 2.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(name, color = DerbisText, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 34.sp, lineHeight = 40.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
