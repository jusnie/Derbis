package com.yasuda.derbis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.DerbisLoadingIndicator
import com.yasuda.derbis.ui.components.DerbisPlayingIndicator
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.PixelDesign
import com.yasuda.derbis.ui.theme.PixelIcons
import com.yasuda.derbis.ui.theme.PixelPalette
import com.yasuda.derbis.ui.theme.PixelifyFont

internal fun Modifier.pixelHardShadow(
    offset: Dp = PixelDesign.shadowOffset,
    color: androidx.compose.ui.graphics.Color = PixelPalette.outline
): Modifier = drawBehind {
    val px = offset.toPx()
    drawRect(
        color = color,
        topLeft = Offset(px, px),
        size = Size(size.width, size.height)
    )
}

internal fun Modifier.pixelPaperGrid(): Modifier = drawBehind {
    val step = 8.dp.toPx()
    val line = PixelPalette.outline.copy(alpha = 0.07f)
    var x = 0f
    while (x < size.width) {
        drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

@Composable
private fun PixelSquareIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    val p = PixelPalette
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(p.ink, RectangleShape)
            .border(PixelDesign.border, p.outline, RectangleShape)
            .derbisPressable(onClick = onClick, onClickLabel = contentDesc, role = Role.Button)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = p.text, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun PixelHomeHeader(
    greeting: String,
    date: String,
    hero: StudioHero?,
    inset: Dp,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    dayPart: String = "",
    name: String = ""
) {
    val strings = LocalDerbisStrings.current
    val p = PixelPalette
    Column(Modifier.fillMaxWidth().background(p.black).pixelPaperGrid()) {
        // Pixel top app bar — band fill (#FFD9E2), 2dp plum/rose bottom rule, rotated pink diamond mark, square icon plates.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(p.panel)
                .padding(horizontal = inset, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .graphicsLayer { rotationZ = 45f }
                    .background(p.pink, RectangleShape)
                    .border(2.dp, p.text, RectangleShape)
            )
            Text(
                text = "DERBIS",
                color = p.text,
                fontFamily = PixelifyFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.4.sp,
                modifier = Modifier.weight(1f)
            )
            PixelSquareIconButton(
                icon = PixelIcons.Search,
                contentDesc = strings.search,
                onClick = onSearch
            )
            PixelSquareIconButton(
                icon = PixelIcons.Settings,
                contentDesc = strings.settings,
                onClick = onSettings
            )
        }
        HorizontalDivider(color = p.outline, thickness = PixelDesign.border)

        Column(Modifier.fillMaxWidth().padding(horizontal = inset, vertical = 14.dp)) {
            if (date.isNotBlank()) {
                Text(
                    text = date.uppercase(),
                    color = p.cyan,
                    fontFamily = PixelifyFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
            }
            if (name.isNotBlank()) {
                SkinNameGreeting(greeting, dayPart, name, Modifier.fillMaxWidth())
            } else {
                Text(
                    text = greeting,
                    color = p.text,
                    fontFamily = PixelifyFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 31.sp,
                    modifier = Modifier.fillMaxWidth().semantics { heading() }
                )
            }
            if (hero != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = hero.label.uppercase(),
                    color = p.cyan,
                    fontFamily = PixelifyFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.1.sp
                )
                if (hero.lead.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = hero.lead,
                        color = p.muted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = PixelDesign.shadowOffset, bottom = PixelDesign.shadowOffset)
                        .pixelHardShadow()
                        .background(p.ink, RectangleShape)
                        .border(PixelDesign.border, p.outline, RectangleShape)
                        .derbisPressable(onClick = hero.onOpen, onClickLabel = hero.track.title, role = Role.Button)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .border(1.5.dp, p.text, RectangleShape)
                    ) {
                        CoverImage(hero.track, Modifier.fillMaxSize())
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(hero.track.title, color = p.text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(hero.track.artist, color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(p.cyan, RectangleShape)
                            .border(PixelDesign.border, p.text, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hero.isResolving && hero.isCurrent) {
                            DerbisLoadingIndicator(color = p.ink, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(
                                imageVector = if (hero.isCurrent && hero.isPlaying) PixelIcons.Pause else PixelIcons.Play,
                                contentDescription = null,
                                tint = p.ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PixelCollectionCard(title: String, subtitle: String, artwork: Track?, onOpen: () -> Unit, modifier: Modifier) {
    val p = PixelPalette
    Row(
        modifier
            .fillMaxWidth()
            .padding(end = PixelDesign.shadowOffset, bottom = PixelDesign.shadowOffset)
            .pixelHardShadow()
            .background(p.ink, RectangleShape)
            .border(PixelDesign.border, p.outline, RectangleShape)
            .derbisPressable(onClick = onOpen, onClickLabel = title, role = Role.Button)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (artwork != null) {
            Box(Modifier.size(72.dp).border(1.5.dp, p.text, RectangleShape)) {
                CoverImage(artwork, Modifier.fillMaxSize())
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = title,
                color = p.text,
                fontFamily = PixelifyFont,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = p.cyan, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun PixelCompactShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    val pages = remember(items) { items.chunked(3) }
    val p = PixelPalette
    LazyRow(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = inset),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(pages, key = { index, page -> "$index:${page.first().key}" }) { _, page ->
            Column(
                Modifier
                    .width(300.dp)
                    .padding(end = PixelDesign.shadowOffset, bottom = PixelDesign.shadowOffset)
                    .pixelHardShadow()
                    .background(p.ink, RectangleShape)
                    .border(PixelDesign.border, p.outline, RectangleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                page.forEachIndexed { rowIdx, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .derbisPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.size(46.dp).border(1.dp, p.text, RectangleShape)) {
                            item.artwork(Modifier.fillMaxSize(), false)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, color = if (item.current) p.cyan else p.text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (item.current) DerbisPlayingIndicator(playing = isPlaying, color = p.cyan, size = 12.dp)
                    }
                    if (rowIdx < page.lastIndex) {
                        HorizontalDivider(color = p.outline.copy(alpha = 0.45f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PixelChartShelf(tracks: List<Track>, currentId: String?, isPlaying: Boolean, onPlay: (Track) -> Unit, inset: Dp) {
    val pages = remember(tracks) { tracks.chunked(5) }
    val p = PixelPalette
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = inset),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(pages, key = { index, page -> "$index:${page.first().id}" }) { pageIndex, page ->
            Column(
                Modifier
                    .width(300.dp)
                    .padding(end = PixelDesign.shadowOffset, bottom = PixelDesign.shadowOffset)
                    .pixelHardShadow()
                    .background(p.ink, RectangleShape)
                    .border(PixelDesign.border, p.outline, RectangleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                page.forEachIndexed { rowIndex, track ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .derbisPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "%02d".format(pageIndex * 5 + rowIndex + 1),
                            color = p.cyan,
                            fontFamily = PixelifyFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.width(24.dp)
                        )
                        Box(Modifier.size(44.dp).border(1.dp, p.text, RectangleShape)) {
                            CoverImage(track, Modifier.fillMaxSize())
                        }
                        Column(Modifier.weight(1f)) {
                            Text(track.title, color = if (track.id == currentId) p.cyan else p.text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (track.id == currentId) DerbisPlayingIndicator(playing = isPlaying, color = p.cyan, size = 12.dp)
                    }
                    if (rowIndex < page.lastIndex) {
                        HorizontalDivider(color = p.outline.copy(alpha = 0.45f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}
