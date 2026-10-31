package com.yasuda.derbis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import com.yasuda.derbis.ui.LocalAnimationsEnabled
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.ui.theme.DerbisActivePalette
import com.yasuda.derbis.ui.theme.DerbisCardDesign
import com.yasuda.derbis.ui.theme.DerbisGlassBorder
import com.yasuda.derbis.ui.theme.DerbisCyan
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisPanel
import com.yasuda.derbis.ui.theme.DerbisPink
import com.yasuda.derbis.ui.theme.DerbisText
import com.yasuda.derbis.ui.theme.DerbisType
import com.yasuda.derbis.ui.theme.DerbisTypeRhythm

@Composable
internal fun Modifier.derbisMarquee(enabled: Boolean = true): Modifier =
    if (enabled && LocalAnimationsEnabled.current) basicMarquee() else this

@Composable
internal fun Modifier.derbisDockSurface(): Modifier {
    val fill = DerbisPanel.copy(alpha = 0.98f)
    val shadowColor = Color.Black.copy(alpha = if (DerbisActivePalette.isLight) 0.18f else 0.55f)
    val material = if (LocalDerbisLiquidGlassEnabled.current) {
        // This compact Now Playing / bulk-action dock is floating functional chrome. Give it the
        // same material vocabulary as the main navigation instead of an unrelated opaque bubble.
        Modifier.derbisGlass(
            shape = CircleShape,
            baseTint = DerbisPanel,
            fallbackColor = fill,
            fallbackBorderColor = DerbisGlassBorder,
            intensity = DerbisGlassIntensity.Standard,
            artworkTint = DerbisActivePalette.cyan
        )
    } else {
        Modifier
            .clip(CircleShape)
            .background(fill)
    }
    return this
        .shadow(
            elevation = 18.dp,
            shape = CircleShape,
            clip = false,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .then(material)
}

@Composable
internal fun DerbisDockProgress(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, bottom = 4.dp)
            .height(3.dp)
            .clip(CircleShape)
            .background(DerbisText.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(DerbisCyan, DerbisPink)))
        )
    }
}

@Composable
internal fun DerbisCardCaption(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleColor: Color = DerbisText,
    subtitleColor: Color = DerbisMuted,
    titleLines: Int = 2,
    reserveTitleLines: Boolean = true,
    marqueeTitle: Boolean = false
) {
    val effectiveLines = if (marqueeTitle) 1 else titleLines
    val titleStyle = DerbisType.cardTitle
    val subtitleStyle = DerbisType.caption
    val minHeight = if (reserveTitleLines) {
        derbisCaptionHeight(effectiveLines)
    } else {
        0.dp
    }
    Column(
        modifier = modifier.heightIn(min = minHeight),
        verticalArrangement = Arrangement.spacedBy(DerbisCardDesign.CaptionLineGap)
    ) {
        Text(
            text = title,
            color = titleColor,
            style = titleStyle,
            maxLines = effectiveLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.derbisMarquee(enabled = marqueeTitle)
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = subtitleColor,
                style = subtitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun derbisCaptionHeight(titleLines: Int): Dp {
    val density = LocalDensity.current
    return with(density) {
        DerbisTypeRhythm.lineHeight(DerbisCardDesign.CardTitleSize).toDp() * titleLines +
            DerbisTypeRhythm.lineHeight(DerbisCardDesign.CardSubtitleSize).toDp() +
            DerbisCardDesign.CaptionLineGap
    }
}

@Composable
internal fun DerbisRowText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleColor: Color = DerbisText,
    subtitleColor: Color = DerbisMuted,
    downloaded: Boolean = false,
    favorite: Boolean = false,
    statusTint: Color = DerbisCyan,
    downloadedLabel: String? = null,
    favoriteLabel: String? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DerbisCardDesign.RowLineGap)
    ) {
        Text(
            text = title,
            color = titleColor,
            style = DerbisType.contentTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank() || downloaded || favorite) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (favorite) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = favoriteLabel,
                        tint = DerbisPink,
                        modifier = Modifier.size(DerbisCardDesign.StatusGlyph)
                    )
                }
                if (downloaded) {
                    Icon(
                        imageVector = Icons.Rounded.DownloadDone,
                        contentDescription = downloadedLabel,
                        tint = statusTint,
                        modifier = Modifier.size(DerbisCardDesign.StatusGlyph)
                    )
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = subtitleColor,
                        style = DerbisType.metadata,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
