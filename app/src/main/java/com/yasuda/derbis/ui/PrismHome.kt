package com.yasuda.derbis.ui

import com.yasuda.derbis.domain.ExploreZone
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasuda.derbis.domain.Track
import com.yasuda.derbis.ui.components.DerbisLoadingIndicator
import com.yasuda.derbis.ui.components.DerbisGlassIntensity
import com.yasuda.derbis.ui.components.DerbisGlassIconButton
import com.yasuda.derbis.ui.components.derbisGlass
import com.yasuda.derbis.ui.components.DerbisPlayingIndicator
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.*

@Composable
internal fun PrismPageHeader(title: String, subtitle: String = "", actions: @Composable RowScope.() -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = DerbisText,
                // Taurus parity: iOS screen titles are 22-26 bold (Library/Settings = 26,
                // home wordmark = 22). Glass keeps that scale instead of the Prism 32pt
                // announcement.
                style = when {
                    DerbisIsSpotify -> MaterialTheme.typography.headlineMedium
                    DerbisIsLiquidGlass -> MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                    else -> MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp)
                },
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
    dayPart: String = "", name: String = "", onOpenZone: (ExploreZone) -> Unit = {},
    jamActive: Boolean = false, jamCount: Int = 1,
    onOpenJam: (() -> Unit)? = null, onOpenThemeStudio: (() -> Unit)? = null
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
        if (DerbisIsLiquidGlass) {
            // Taurus parity: the iOS home header is the 3-diamond mark, "Derbis" in 22 bold,
            // a "greeting · skin" line in 11.5 medium secondary text, then a Jam glass capsule
            // and a swatchpalette glass circle (skin/deck/typography quick menu = Theme Studio
            // here). Android keeps its Search and Settings glass circles after those — the
            // settings entry point lives nowhere else on the bottom chrome.
            val secondary = name.takeIf { it.isNotBlank() } ?: dayPart
            Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LiquidGlassDiamondMark(color = DerbisActivePalette.cyan, size = 24.dp)
                Column(Modifier.padding(start = 9.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Derbis", color = DerbisText, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("$secondary · $DerbisSkinLabel", color = DerbisMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                onOpenJam?.let { LiquidGlassJamCapsule(active = jamActive, count = jamCount, onOpenJam = it) }
                onOpenThemeStudio?.let { themeStudio ->
                    Spacer(Modifier.width(8.dp))
                    DerbisGlassIconButton(
                        icon = Icons.Rounded.Palette,
                        contentDescription = strings.themeStudio,
                        onClick = themeStudio,
                        size = 34.dp,
                        iconSize = 15.dp,
                        tint = DerbisText,
                        baseTint = Color.White,
                        fallbackColor = DerbisPanel.copy(alpha = 0.72f),
                        fallbackBorderColor = Color.White.copy(alpha = 0.42f),
                        artworkTint = DerbisActivePalette.cyan,
                        intensity = DerbisGlassIntensity.Standard
                    )
                }
                actions()
            }
        } else if (name.isNotBlank()) {
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


@Composable
private fun LiquidGlassJamCapsule(active: Boolean, count: Int, onOpenJam: () -> Unit) {
    // iOS: person.2.wave.2 capsule, glass with accent film when the Jam is live and a
    // participant-count badge; quiet glass otherwise.
    val circle = CircleShape
    val accent = DerbisActivePalette.cyan
    Row(
        modifier = Modifier
            .derbisGlass(
                shape = circle,
                baseTint = if (active) accent else Color.White,
                fallbackColor = if (active) accent.copy(alpha = 0.86f) else DerbisPanel.copy(alpha = 0.72f),
                fallbackBorderColor = Color.White.copy(alpha = if (active) 0.42f else 0.24f),
                intensity = DerbisGlassIntensity.Standard,
                artworkTint = accent
            )
            .derbisPressable(onClick = onOpenJam, onClickLabel = "Jam", role = androidx.compose.ui.semantics.Role.Button)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (active) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34C759)))
        }
        Icon(
            imageVector = Icons.Rounded.Groups,
            contentDescription = null,
            tint = if (active) Color.White else DerbisText,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = if (active) count.coerceAtLeast(1).toString() else "Jam",
            color = if (active) Color.White else DerbisText,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
private fun LiquidGlassDiamondMark(color: Color, size: Dp) {
    // Exact port of the iOS DerbisDiamondMark (HomeView.swift): three sharp filled diamonds at
    // (0.36, 0.26), (0.66, 0.50) and (0.36, 0.74) of the canvas side, half-diagonal 0.25s.
    Canvas(Modifier.size(size)) {
        val s = this.size.width
        val d = s * 0.25f
        val centers = listOf(
            Offset(s * 0.36f, s * 0.26f),
            Offset(s * 0.66f, s * 0.50f),
            Offset(s * 0.36f, s * 0.74f)
        )
        centers.forEach { center ->
            drawPath(
                path = Path().apply {
                    moveTo(center.x, center.y - d)
                    lineTo(center.x + d, center.y)
                    lineTo(center.x, center.y + d)
                    lineTo(center.x - d, center.y)
                    close()
                },
                color = color
            )
        }
    }
}
