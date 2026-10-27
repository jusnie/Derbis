package com.luc4n3x.derbis.ui

import com.luc4n3x.derbis.domain.ExploreZone
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.derbis.domain.Track
import com.luc4n3x.derbis.ui.components.DerbisLoadingIndicator
import com.luc4n3x.derbis.ui.components.DerbisGlassIntensity
import com.luc4n3x.derbis.ui.components.DerbisGlassIconButton
import com.luc4n3x.derbis.ui.components.derbisGlass
import com.luc4n3x.derbis.ui.components.DerbisPlayingIndicator
import com.luc4n3x.derbis.ui.components.derbisPressable
import com.luc4n3x.derbis.ui.i18n.LocalDerbisStrings
import com.luc4n3x.derbis.ui.theme.*

@Composable
internal fun PrismPageHeader(title: String, subtitle: String = "", actions: @Composable RowScope.() -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, color = DerbisText, style = if (DerbisIsSpotify) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp),
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() }
            )
            actions()
        }
        if (subtitle.isNotBlank()) Text(subtitle, color = DerbisMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun PrismPlayDisc(playing: Boolean, resolving: Boolean, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .derbisGlass(
                shape = CircleShape,
                baseTint = DerbisCyan,
                fallbackColor = DerbisCyan,
                fallbackBorderColor = Color.White.copy(alpha = 0.42f),
                intensity = DerbisGlassIntensity.Standard
            ),
        contentAlignment = Alignment.Center
    ) {
        val glyph = if (DerbisIsIvory) IvoryDesign.onInk else if (DerbisIsSpotify) Color.Black else Color.White
        if (resolving) DerbisLoadingIndicator(color = glyph, modifier = Modifier.size(size * 0.42f))
        else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = glyph, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
internal fun PrismHomeHeader(
    greeting: String, date: String, hero: StudioHero?, inset: Dp, onSearch: () -> Unit, onSettings: () -> Unit,
    dayPart: String = "", name: String = "", onOpenZone: (ExploreZone) -> Unit = {}
) {
    if (DerbisIsSpotify) {
        SpotifyHomeHeader(hero, inset, onSettings, name, onOpenZone)
        return
    }
    if (DerbisIsIvory) {
        IvoryHomeHeader(greeting, date, hero, inset, onSearch, onSettings, dayPart, name)
        return
    }
    val strings = LocalDerbisStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = inset)) {
        val actions: @Composable RowScope.() -> Unit = {
            if (DerbisIsLiquidGlass) {
                DerbisGlassIconButton(
                    icon = Icons.Outlined.Search,
                    contentDescription = strings.search,
                    onClick = onSearch,
                    tint = DerbisText,
                    baseTint = Color.White,
                    fallbackColor = DerbisPanelSoft.copy(alpha = 0.78f),
                    fallbackBorderColor = Color.White.copy(alpha = 0.42f),
                    artworkTint = DerbisActivePalette.cyan
                )
                DerbisGlassIconButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = strings.settings,
                    onClick = onSettings,
                    tint = DerbisText,
                    baseTint = Color.White,
                    fallbackColor = DerbisPanelSoft.copy(alpha = 0.78f),
                    fallbackBorderColor = Color.White.copy(alpha = 0.42f),
                    artworkTint = DerbisActivePalette.cyan
                )
            } else {
                IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Search, strings.search, tint = DerbisText) }
                IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Settings, strings.settings, tint = DerbisText) }
            }
        }
        if (name.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SkinNameGreeting(greeting, dayPart, name, Modifier.weight(1f))
                actions()
            }
        } else PrismPageHeader(greeting, date, actions)
        if (hero != null) {
            Spacer(Modifier.height(8.dp))
            if (DerbisIsTorque) TorqueSoundtrackHero(hero, strings.play)
            else LiquidGlassSoundtrackHero(hero, strings.play)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun PrismCompactShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    if (DerbisIsSpotify) {
        SpotifyShortcutGrid(items, isPlaying, inset, modifier)
        return
    }
    if (DerbisIsIvory) {
        IvoryCompactShelf(items, isPlaying, inset, modifier)
        return
    }
    val pages = remember(items) { items.chunked(3) }
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = (inset - 8.dp).coerceAtLeast(0.dp)), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(pages, key = { index, page -> "$index:${page.first().key}" }) { _, page ->
            Column(Modifier.width(312.dp), verticalArrangement = Arrangement.spacedBy(if (DerbisIsTorque) 2.dp else 0.dp)) {
                page.forEach { item ->
                    val rowShape = RoundedCornerShape(if (DerbisIsLiquidGlass) 22.dp else PrismDesign.controlCorner)
                    val rowSurface = if (DerbisIsLiquidGlass) {
                        Modifier.derbisGlass(
                            shape = rowShape,
                            baseTint = if (item.current) DerbisCyan else Color.White,
                            fallbackColor = if (item.current) DerbisCyan.copy(alpha = 0.26f) else DerbisPanelSoft.copy(alpha = 0.74f),
                            fallbackBorderColor = Color.White.copy(alpha = if (item.current) 0.46f else 0.24f),
                            intensity = DerbisGlassIntensity.Subtle,
                            artworkTint = DerbisActivePalette.cyan
                        )
                    } else {
                        Modifier.background(if (DerbisIsTorque) { if (item.current) DerbisPanelSoft else DerbisPanel } else if (item.current) PrismAccent.Tint else Color.Transparent)
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(rowShape)
                            .then(rowSurface)
                            .derbisPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(Modifier.size(56.dp).clip(if (item.round) CircleShape else RoundedCornerShape(PrismDesign.tileCorner))) { item.artwork(Modifier.fillMaxSize(), item.round) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, color = if (item.current) DerbisCyan else DerbisText, style = MaterialTheme.typography.titleSmall, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (item.subtitle.isNotBlank()) Text(item.subtitle, color = DerbisMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (item.current) DerbisPlayingIndicator(playing = isPlaying, color = DerbisCyan, size = 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PrismCollectionCard(title: String, subtitle: String, artwork: Track?, onOpen: () -> Unit, modifier: Modifier) {
    val ivory = DerbisIsIvory
    val liquidGlass = DerbisIsLiquidGlass
    val content = if (ivory) IvoryDesign.onInk else DerbisText
    val cardShape = if (liquidGlass) RoundedCornerShape(28.dp) else PrismDesign.cut(PrismDesign.surfaceCorner)
    val cardSurface = if (liquidGlass) {
        Modifier.derbisGlass(
            shape = cardShape,
            baseTint = Color.White,
            fallbackColor = DerbisPanelSoft.copy(alpha = 0.76f),
            fallbackBorderColor = Color.White.copy(alpha = 0.28f),
            intensity = DerbisGlassIntensity.Subtle,
            artworkTint = DerbisActivePalette.cyan
        )
    } else {
        Modifier.background(if (ivory) IvoryDesign.ink else DerbisPanel)
    }
    Row(
        modifier.fillMaxWidth().clip(cardShape).then(cardSurface)
            .derbisPressable(onClick = onOpen, onClickLabel = title, role = Role.Button).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = content, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = if (ivory) content.copy(alpha = 0.7f) else DerbisMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            val actionModifier = if (liquidGlass) {
                Modifier.derbisGlass(
                    shape = CircleShape,
                    baseTint = DerbisCyan,
                    fallbackColor = DerbisCyan.copy(alpha = 0.44f),
                    fallbackBorderColor = Color.White.copy(alpha = 0.42f),
                    intensity = DerbisGlassIntensity.Strong,
                    artworkTint = DerbisActivePalette.cyan
                )
            } else {
                Modifier.background(if (ivory) IvoryDesign.onInk else PrismAccent.Tint, CircleShape)
            }
            Box(Modifier.size(36.dp).then(actionModifier), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = if (ivory) IvoryDesign.ink else if (liquidGlass) Color.White else DerbisCyan, modifier = Modifier.size(20.dp))
            }
        }
        if (artwork != null) Box(Modifier.size(96.dp).clip(if (ivory) IvoryDesign.coverShape else RoundedCornerShape(PrismDesign.controlCorner))) { CoverImage(artwork, Modifier.fillMaxSize()) }
    }
}

@Composable
internal fun PrismArtworkShelf(items: List<StudioShelfItem>, isPlaying: Boolean, inset: Dp, modifier: Modifier) {
    if (DerbisIsSpotify) {
        SpotifyCarousel(items, isPlaying, inset, modifier)
        return
    }
    if (DerbisIsIvory) {
        IvoryShowcaseShelf(items, isPlaying, inset, modifier)
        return
    }
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(items, key = { _, item -> item.key }) { _, item ->
            Column(
                Modifier.width(if (item.round) 124.dp else 148.dp)
                    .derbisPressable(onClick = item.onClick, onLongClick = item.onLongClick, onClickLabel = item.title, role = Role.Button),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = if (item.round) Alignment.CenterHorizontally else Alignment.Start
            ) {
                val artworkShape = if (item.round) CircleShape else RoundedCornerShape(if (DerbisIsLiquidGlass) 22.dp else PrismDesign.controlCorner)
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(artworkShape).then(if (DerbisIsLiquidGlass) Modifier.border(1.dp, Color.White.copy(alpha = 0.26f), artworkShape) else Modifier)) {
                    item.artwork(Modifier.fillMaxSize(), false)
                    if (item.current) {
                        val currentMarker = if (DerbisIsLiquidGlass) {
                            Modifier.derbisGlass(
                                shape = CircleShape,
                                baseTint = DerbisCyan,
                                fallbackColor = DerbisCyan.copy(alpha = 0.42f),
                                fallbackBorderColor = Color.White.copy(alpha = 0.42f),
                                intensity = DerbisGlassIntensity.Standard,
                                artworkTint = DerbisActivePalette.cyan
                            )
                        } else {
                            Modifier.background(PrismAccent.gradient(), CircleShape)
                        }
                        Box(
                            Modifier.align(Alignment.BottomEnd).padding(8.dp).size(28.dp).then(currentMarker),
                            contentAlignment = Alignment.Center
                        ) { DerbisPlayingIndicator(playing = isPlaying, color = Color.White, size = 12.dp) }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(item.title, color = if (item.current) DerbisCyan else DerbisText, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.subtitle.isNotBlank()) Text(item.subtitle, color = DerbisMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
