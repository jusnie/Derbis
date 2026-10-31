package com.yasuda.derbis.ui

import com.yasuda.derbis.ui.components.rememberDerbisAvatarPressShape
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.components.derbisGroupedListShape
import com.yasuda.derbis.ui.components.DerbisSectionHeader
import com.yasuda.derbis.ui.components.DerbisPressScale
import com.yasuda.derbis.ui.components.DerbisGlassIntensity
import com.yasuda.derbis.ui.components.derbisGlass
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yasuda.derbis.domain.ArtistHit
import com.yasuda.derbis.ui.theme.*

@Composable
internal fun SearchArtistSuggestions(
    title: String,
    artists: List<ArtistHit>,
    fallbackNames: List<String>,
    loading: Boolean,
    onArtistClick: (ArtistHit) -> Unit,
    onFallbackClick: (String) -> Unit,
) {
    val visibleArtists = artists
        .asSequence()
        .filter { it.name.isNotBlank() && it.thumbnailUrl.isNotBlank() }
        .distinctBy { artist -> artist.browseId.ifBlank { artist.name.trim().lowercase() } }
        .take(7)
        .toList()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (DerbisSkinManagesLayout) SkinSectionTitle(title, 0.dp) else DerbisSectionHeader(
            title = title,
            titleColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        when {
            visibleArtists.isNotEmpty() -> {
                visibleArtists.forEachIndexed { index, artist ->
                    SearchArtistRow(
                        index = index,
                        count = visibleArtists.size,
                        name = artist.name,
                        detail = searchArtistAudienceDetail(artist.subscribers),
                        onClick = { onArtistClick(artist) },
                    ) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }
                }
            }

            loading -> {
                repeat(5) {
                    SearchArtistSuggestionSkeleton()
                }
            }

            else -> {
                val fallback = fallbackNames.take(7)
                fallback.forEachIndexed { index, name ->
                    SearchArtistRow(
                        index = index,
                        count = fallback.size,
                        name = name,
                        detail = "",
                        onClick = { onFallbackClick(name) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchArtistRow(
    index: Int,
    count: Int,
    name: String,
    detail: String,
    onClick: () -> Unit,
    avatar: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val avatarShape = rememberDerbisAvatarPressShape(pressed = pressed, animated = LocalAnimationsEnabled.current)
    val rowShape = if (DerbisIsPrism) PrismDesign.cut(PrismDesign.controlCorner) else derbisGroupedListShape(index, count)
    val rowSurface = if (DerbisIsLiquidGlass) {
        Modifier.derbisGlass(
            shape = rowShape,
            baseTint = Color.White,
            fallbackColor = DerbisPanel.copy(alpha = 0.70f),
            fallbackBorderColor = Color.White.copy(alpha = 0.28f),
            intensity = DerbisGlassIntensity.Subtle,
            artworkTint = DerbisActivePalette.cyan
        )
    } else {
        Modifier.background(
            if (DerbisIsSpotify) Color.Transparent
            else if (DerbisIsPrism) DerbisPanel
            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.62f)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SearchArtistRowHeight)
            .clip(rowShape)
            .then(rowSurface)
            .derbisPressable(
                onClick = onClick,
                role = Role.Button,
                pressedScale = DerbisPressScale.Row,
                interactionSource = interaction
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SearchArtistAvatarSize)
                .clip(searchArtistAvatarShape(avatarShape)),
        ) {
            avatar()
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                style = DerbisType.contentTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = DerbisType.metadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun searchArtistAvatarShape(default: androidx.compose.ui.graphics.Shape): androidx.compose.ui.graphics.Shape = when {
    DerbisIsTorque -> RoundedCornerShape(2.dp)
    DerbisIsNocturne -> RoundedCornerShape(Nocturne78Design.artworkCorner)
    else -> default
}

@Composable
private fun SearchArtistSuggestionSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SearchArtistRowHeight)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SearchArtistAvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.46f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.28f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            )
        }
    }
}

private val SearchArtistAvatarSize = 56.dp
private val SearchArtistRowHeight = 72.dp

private val SearchArtistLeadingLabel = Regex("^[^•·|]{1,24}\\s*[•·|]\\s*")

internal fun searchArtistAudienceDetail(detail: String): String {
    val trimmed = detail.trim()
    val stripped = trimmed.replaceFirst(SearchArtistLeadingLabel, "").trim()
    return stripped.ifBlank { trimmed }
}
