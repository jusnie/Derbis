package com.luc4n3x.derbis.ui

import com.luc4n3x.derbis.ui.theme.DerbisIsNocturne

import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.derbis.domain.Track
import com.luc4n3x.derbis.ui.components.DerbisCardCaption
import com.luc4n3x.derbis.ui.components.DerbisExpressiveIconButton
import com.luc4n3x.derbis.ui.components.DerbisLoadingIndicator
import com.luc4n3x.derbis.ui.components.DerbisPlayingIndicator
import com.luc4n3x.derbis.ui.components.DerbisPressScale
import com.luc4n3x.derbis.ui.components.derbisExpressiveCorner
import com.luc4n3x.derbis.ui.components.derbisPressable
import com.luc4n3x.derbis.ui.i18n.LocalDerbisStrings
import com.luc4n3x.derbis.ui.theme.DerbisCardDesign
import com.luc4n3x.derbis.ui.theme.DerbisHomeDesign
import com.luc4n3x.derbis.ui.theme.DerbisType
import com.luc4n3x.derbis.ui.theme.derbisTonalRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.materialkolor.hct.Hct
import kotlin.math.abs

@Composable
internal fun DiscoveryTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    onActions: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val strings = LocalDerbisStrings.current
    val interaction = remember { MutableInteractionSource() }
    val corner = if (LocalAnimationsEnabled.current) {
        derbisExpressiveCorner(
            interactionSource = interaction,
            rest = DerbisCardDesign.EditorialCorner,
            pressed = DerbisCardDesign.ArtworkCorner,
            label = "discoveryArtworkCorner"
        )
    } else {
        DerbisCardDesign.EditorialCorner
    }
    val colors = MaterialTheme.colorScheme
    val scrim = remember { Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))) }
    Column(
        modifier = modifier
            .semantics { selected = isCurrent }
            .derbisPressable(
                onClick = onPlay,
                interactionSource = interaction,
                pressedScale = DerbisPressScale.Tile,
                role = Role.Button,
                onClickLabel = strings.playNow,
                onLongClick = onActions,
                onLongClickLabel = if (onActions != null) strings.songOptions else null
            ),
        verticalArrangement = Arrangement.spacedBy(DerbisCardDesign.CaptionTopGap)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (wide) DerbisHomeDesign.EditorialArtworkRatio else 1f)
                .clip(RoundedCornerShape(corner))
                .background(colors.surfaceContainerHigh)
        ) {
            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = wide)
            if (wide || isCurrent) {
                Box(Modifier.fillMaxSize().background(scrim))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(DerbisHomeDesign.HorizontalInset)
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(if (isCurrent) colors.primaryContainer else colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isResolving -> DiscoveryLoadingIndicator(Modifier.size(24.dp), colors.onPrimaryContainer)
                        isCurrent -> DerbisPlayingIndicator(playing = isPlaying, color = colors.onPrimaryContainer)
                        else -> Icon(Icons.Rounded.PlayArrow, null, tint = colors.onSurface)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            DerbisCardCaption(
                title = track.title,
                subtitle = track.artist,
                titleColor = if (isCurrent) colors.primary else colors.onSurface,
                subtitleColor = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
    }
}

@Composable
internal fun DiscoveryLoadingIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    if (LocalAnimationsEnabled.current) {
        DerbisLoadingIndicator(modifier = modifier, color = color)
    } else {
        Box(modifier = modifier.progressSemantics(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = color)
        }
    }
}

@Composable
internal fun DiscoveryTrackActions(
    trackTitle: String,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenAlbum: (() -> Unit)? = null
) {
    val strings = LocalDerbisStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        DerbisExpressiveIconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.MoreHoriz, "${strings.songOptions}, $trackTitle", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(if (isFavorite) strings.removeFromFavorites else strings.addToFavorites) },
                onClick = {
                    expanded = false
                    onFavorite()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.addToPlaylist) },
                onClick = {
                    expanded = false
                    onAddToPlaylist()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.share) },
                onClick = {
                    expanded = false
                    onShare()
                }
            )
            onOpenAlbum?.let { action ->
                DropdownMenuItem(
                    text = { Text(strings.openAlbum) },
                    onClick = {
                        expanded = false
                        action()
                    }
                )
            }
        }
    }
}

@Composable
internal fun DiscoveryEditorialCard(
    title: String,
    subtitle: String,
    artwork: Track?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    accentStart: Color = Color(0xFF7C3AED),
    accentEnd: Color = Color(0xFF4C1D95),
    artworkShape: Shape? = null
) {
    val colors = MaterialTheme.colorScheme
    val isLight = DerbisIsLight
    if (com.luc4n3x.derbis.ui.theme.DerbisIsPrism) {
        PrismCollectionCard(title, subtitle, artwork, onOpen, modifier)
        return
    }
    if (DerbisIsNocturne) {
        NocturneCollectionCard(title, subtitle, artwork, onOpen, modifier)
        return
    }
    val tonal = remember(accentStart, isLight) {
        derbisTonalRole(accentStart, isLight, maxChroma = 46.0)
    }
    val backgroundColor = lerp(colors.surfaceContainerHigh, tonal.container, 0.94f)
    val contrastPalette = remember(backgroundColor) {
        editorialContrastPalette(backgroundColor, backgroundColor)
    }
    val interaction = remember { MutableInteractionSource() }
    val corner = if (LocalAnimationsEnabled.current) {
        derbisExpressiveCorner(
            interactionSource = interaction,
            rest = EditorialCardRestCorner,
            pressed = EditorialCardPressedCorner,
            label = "editorialCollectionCorner"
        )
    } else {
        EditorialCardRestCorner
    }
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(contrastPalette.start)
            .derbisPressable(
                onClick = onOpen,
                interactionSource = interaction,
                role = Role.Button,
                pressedScale = DerbisPressScale.Tile
            )
    ) {
        val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.4f
        if (stacked) {
            Column(
                modifier = Modifier.padding(DerbisHomeDesign.EditorialPadding),
                verticalArrangement = Arrangement.spacedBy(DerbisHomeDesign.EditorialPadding)
            ) {
                DiscoveryEditorialText(title, subtitle, contrastPalette.text)
                if (artwork != null) {
                    CoverImage(
                        track = artwork,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(DerbisHomeDesign.EditorialArtworkRatio)
                            .clip(DerbisCardDesign.ArtworkShape)
                    )
                }
            }
        } else {
            val artworkSize = DerbisHomeDesign.EditorialThumb + 22.dp
            val textWidth = if (artwork == null) {
                maxWidth - DerbisHomeDesign.EditorialPadding * 2
            } else {
                (maxWidth - artworkSize - DerbisHomeDesign.EditorialPadding - 16.dp)
                    .coerceAtLeast(112.dp)
            }
            DiscoveryEditorialText(
                title = title,
                subtitle = subtitle,
                textColor = contrastPalette.text,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = DerbisHomeDesign.EditorialPadding,
                        top = DerbisHomeDesign.EditorialPadding
                    )
                    .width(textWidth)
            )
            if (artwork != null) {
                CoverImage(
                    track = artwork,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 16.dp, y = 18.dp)
                        .size(artworkSize)
                        .rotate(12f)
                        .clip(artworkShape ?: DerbisCardDesign.ArtworkShape)
                )
            }
        }
    }
}

@Composable
private fun DiscoveryEditorialText(
    title: String,
    subtitle: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        Text(
            title,
            style = DerbisType.sectionTitle.copy(fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.7).sp),
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = DerbisType.metadata,
                color = textColor.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal fun distinctEditorialAccents(seeds: List<Color>): List<Pair<Color, Color>> {
    val usedHues = mutableListOf<Double>()
    return seeds.map { seed ->
        val sourceHue = Hct.fromInt(seed.toArgb()).hue
        val unused = EditorialCollectionHues.filterNot { it in usedHues }
        val candidates = unused.ifEmpty {
            EditorialCollectionHues.filterNot { it == usedHues.lastOrNull() }
        }
        val hue = candidates.minBy { editorialHueDistance(it, sourceHue) }
        usedHues += hue
        Color(Hct.from(hue, EditorialAccentChroma, EditorialAccentTone).toInt()) to
            Color(Hct.from(hue, EditorialAccentChroma, EditorialAccentDeepTone).toInt())
    }
}

private fun editorialHueDistance(first: Double, second: Double): Double {
    val delta = abs(first - second) % 360.0
    return minOf(delta, 360.0 - delta)
}

private val EditorialCollectionHues = listOf(52.0, 290.0, 175.0, 345.0, 255.0, 140.0, 20.0, 215.0)
private const val EditorialAccentChroma = 48.0
private const val EditorialAccentTone = 50.0
private const val EditorialAccentDeepTone = 30.0

private val EditorialCardRestCorner = 32.dp
private val EditorialCardPressedCorner = 24.dp

private data class EditorialContrastPalette(
    val start: Color,
    val end: Color,
    val text: Color
)

private fun editorialContrastPalette(start: Color, end: Color): EditorialContrastPalette {
    var safeStart = start.copy(alpha = 1f)
    var safeEnd = end.copy(alpha = 1f)
    repeat(10) {
        editorialTextColor(safeStart, safeEnd)?.let { textColor ->
            return EditorialContrastPalette(safeStart, safeEnd, textColor)
        }
        safeStart = Color.Black.copy(alpha = 0.10f).compositeOver(safeStart)
        safeEnd = Color.Black.copy(alpha = 0.10f).compositeOver(safeEnd)
    }
    return EditorialContrastPalette(safeStart, safeEnd, Color.White)
}

private fun editorialTextColor(start: Color, end: Color): Color? {
    val whiteContrast = minOf(
        contrastRatio(Color.White, start),
        contrastRatio(Color.White, end)
    )
    if (whiteContrast >= EditorialMinimumContrast) return Color.White

    val effectiveStart = Color.Black.copy(alpha = 0.20f).compositeOver(start)
    val effectiveEnd = Color.Black.copy(alpha = 0.12f).compositeOver(end)
    val blackContrast = minOf(
        contrastRatio(Color.Black, effectiveStart),
        contrastRatio(Color.Black, effectiveEnd)
    )
    return Color.Black.takeIf { blackContrast >= EditorialMinimumContrast }
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val foregroundLuminance = foreground.luminance()
    val backgroundLuminance = background.luminance()
    val lighter = maxOf(foregroundLuminance, backgroundLuminance)
    val darker = minOf(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private const val EditorialMinimumContrast = 4.5f
