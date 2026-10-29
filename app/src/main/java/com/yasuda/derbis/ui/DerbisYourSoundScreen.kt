package com.yasuda.derbis.ui

import com.yasuda.derbis.ui.components.DerbisBackButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.yasuda.derbis.domain.DnaArtist
import com.yasuda.derbis.domain.ListeningDna
import com.yasuda.derbis.domain.ListeningDnaPeriod
import com.yasuda.derbis.domain.PulseTrack
import com.yasuda.derbis.ui.components.DerbisConnectedDefaults
import com.yasuda.derbis.ui.components.DerbisConnectedPosition
import com.yasuda.derbis.ui.components.DerbisConnectedStyle
import com.yasuda.derbis.ui.components.DerbisPressScale
import com.yasuda.derbis.ui.components.DerbisSkeletonBlock
import com.yasuda.derbis.ui.components.derbisConnectedRowSurface
import com.yasuda.derbis.ui.components.derbisConnectedSurface
import com.yasuda.derbis.ui.components.derbisPressable
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.theme.DerbisBlack
import com.yasuda.derbis.ui.theme.DerbisInk
import com.yasuda.derbis.ui.theme.DerbisMuted
import com.yasuda.derbis.ui.theme.DerbisPlayerDesign
import com.yasuda.derbis.ui.theme.DerbisText

private val DnaPeriods = listOf(
    ListeningDnaPeriod.Week,
    ListeningDnaPeriod.Month,
    ListeningDnaPeriod.HalfYear,
    ListeningDnaPeriod.AllTime
)

@Composable
internal fun DerbisYourSoundOverlay(
    dna: ListeningDna,
    period: ListeningDnaPeriod,
    loading: Boolean,
    accent: Color,
    onSelectPeriod: (ListeningDnaPeriod) -> Unit,
    onStartArtistMix: (DnaArtist) -> Unit,
    onDiscoverArtist: (DnaArtist) -> Unit,
    onPlayTrack: (PulseTrack) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val style = DerbisConnectedDefaults.style(accent = accent)
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val percent = remember(locale) {
        NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 0 }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DerbisInk, DerbisBlack)))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = overlayScreenBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(style.gap)
        ) {
            item(contentType = "dna-header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DerbisBackButton(onClick = onClose, contentDescription = strings.back)
                    Column(modifier = Modifier.padding(start = 6.dp)) {
                        Text(strings.yourSound, color = DerbisText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text(strings.yourSoundSubtitle, color = DerbisMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item(contentType = "dna-period") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(style.gap)
                ) {
                    DnaPeriods.forEachIndexed { index, entry ->
                        DnaPeriodChip(
                            label = dnaPeriodLabel(entry, strings.dnaPeriodWeek, strings.dnaPeriodMonth, strings.dnaPeriodHalfYear, strings.dnaPeriodAll),
                            selected = entry == period,
                            position = DerbisConnectedPosition.of(index, DnaPeriods.size),
                            style = style,
                            accent = accent,
                            onClick = { onSelectPeriod(entry) }
                        )
                    }
                }
            }
            if (loading && !dna.hasSignal) {
                item(contentType = "dna-loading") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DerbisSkeletonBlock(width = 220.dp, height = 26.dp, accent = accent)
                        DerbisSkeletonBlock(width = 320.dp, height = 64.dp, accent = accent)
                        DerbisSkeletonBlock(width = 320.dp, height = 64.dp, accent = accent)
                    }
                }
            } else if (!dna.hasSignal) {
                item(contentType = "dna-empty") {
                    Text(
                        text = strings.dnaEmpty,
                        color = DerbisMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                item(contentType = "dna-stats") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(style.gap)
                    ) {
                        DnaStatTile(dna.totalMinutes.toString(), strings.pulseMinutes, DerbisConnectedPosition.Top, style, accent)
                        DnaStatTile(dna.plays.toString(), strings.statPlays, DerbisConnectedPosition.Middle, style, accent)
                        DnaStatTile(dna.distinctArtists.toString(), strings.statArtists, DerbisConnectedPosition.Middle, style, accent)
                        if (dna.period == ListeningDnaPeriod.AllTime) {
                            DnaStatTile(percent.format(dna.completionRate / 100.0), strings.pulseCompletion, DerbisConnectedPosition.Bottom, style, accent)
                        } else {
                            DnaStatTile(percent.format(dna.discoveryRate / 100.0), strings.dnaDiscovery, DerbisConnectedPosition.Bottom, style, accent)
                        }
                    }
                }
                if (dna.artists.isNotEmpty()) {
                    item(contentType = "dna-artists-title") { DnaSectionTitle(strings.pulseTopArtists) }
                    itemsIndexed(
                        dna.artists,
                        key = { index, _ -> "dna-artist-$index" },
                        contentType = { _, _ -> "dna-artist" }
                    ) { index, artist ->
                        DnaArtistRow(
                            artist = artist,
                            accent = accent,
                            position = DerbisConnectedPosition.of(index, dna.artists.size),
                            style = style,
                            mixLabel = strings.mixStartRadio,
                            discoverLabel = strings.discoverMore,
                            percent = percent,
                            onStartMix = { onStartArtistMix(artist) },
                            onDiscover = { onDiscoverArtist(artist) }
                        )
                    }
                }
                item(contentType = "dna-rhythm-title") { DnaSectionTitle(strings.dnaRhythm) }
                item(contentType = "dna-rhythm") {
                    DnaRhythmChart(
                        buckets = dna.hourBuckets,
                        accent = accent,
                        style = style,
                        peakLabel = strings.pulsePeakHour,
                        peakHour = dna.peakHour,
                        locale = locale
                    )
                }
                if (dna.tracks.isNotEmpty()) {
                    item(contentType = "dna-tracks-title") { DnaSectionTitle(strings.songs) }
                    itemsIndexed(
                        dna.tracks,
                        key = { index, _ -> "dna-track-$index" },
                        contentType = { _, _ -> "dna-track" }
                    ) { index, track ->
                        DnaTrackRow(
                            track = track,
                            position = DerbisConnectedPosition.of(index, dna.tracks.size),
                            style = style,
                            accent = accent,
                            onClick = { onPlayTrack(track) }
                        )
                    }
                }
            }
        }
    }
}

private fun dnaPeriodLabel(
    period: ListeningDnaPeriod,
    week: String,
    month: String,
    halfYear: String,
    allTime: String
): String = when (period) {
    ListeningDnaPeriod.Week -> week
    ListeningDnaPeriod.Month -> month
    ListeningDnaPeriod.HalfYear -> halfYear
    ListeningDnaPeriod.AllTime -> allTime
}

@Composable
private fun RowScope.DnaPeriodChip(
    label: String,
    selected: Boolean,
    position: DerbisConnectedPosition,
    style: DerbisConnectedStyle,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .derbisConnectedRowSurface(position, style, selected = selected)
            .derbisPressable(
                onClick = onClick,
                pressedScale = DerbisPressScale.Row,
                role = Role.Tab,
                onClickLabel = label
            )
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) accent else DerbisMuted,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.DnaStatTile(
    value: String,
    label: String,
    position: DerbisConnectedPosition,
    style: DerbisConnectedStyle,
    accent: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .derbisConnectedRowSurface(position, style)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, color = accent, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(
            text = label,
            color = DerbisMuted,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DnaSectionTitle(title: String) {
    Text(
        text = title,
        color = DerbisText,
        fontSize = 15.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun DnaArtistRow(
    artist: DnaArtist,
    accent: Color,
    position: DerbisConnectedPosition,
    style: DerbisConnectedStyle,
    mixLabel: String,
    discoverLabel: String,
    percent: NumberFormat,
    onStartMix: () -> Unit,
    onDiscover: () -> Unit
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val weight by animateFloatAsState(
        targetValue = artist.weight.coerceIn(0f, 1f),
        animationSpec = if (animationsEnabled) tween(620) else snap(),
        label = "dna-artist-weight"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .derbisConnectedSurface(position, style)
            .derbisPressable(
                onClick = onStartMix,
                pressedScale = DerbisPressScale.Row,
                role = Role.Button,
                onClickLabel = mixLabel,
                onLongClick = onDiscover,
                onLongClickLabel = discoverLabel
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = artist.name,
                color = DerbisText,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = percent.format(artist.weight.toDouble()),
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(5.dp)) {
            val trackHeight = size.height
            val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            drawRoundRect(
                color = DerbisPlayerDesign.TrackInactive,
                topLeft = Offset.Zero,
                size = Size(size.width, trackHeight),
                cornerRadius = radius
            )
            val filled = size.width * weight
            if (filled > 0f) {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset.Zero,
                    size = Size(filled, trackHeight),
                    cornerRadius = radius
                )
            }
        }
    }
}

@Composable
private fun DnaRhythmChart(
    buckets: List<Long>,
    accent: Color,
    style: DerbisConnectedStyle,
    peakLabel: String,
    peakHour: Int,
    locale: Locale
) {
    if (buckets.isEmpty()) return
    val peak = buckets.maxOrNull() ?: 0L
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .derbisConnectedSurface(DerbisConnectedPosition.Single, style)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            if (peak <= 0L) return@Canvas
            val count = buckets.size
            val slot = size.width / count
            val barWidth = (slot * 0.6f).coerceAtLeast(1f)
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            for (index in 0 until count) {
                val ratio = (buckets[index].toFloat() / peak.toFloat()).coerceIn(0f, 1f)
                val barHeight = (size.height * ratio).coerceAtLeast(barWidth)
                drawRoundRect(
                    color = if (index == peakHour) accent else accent.copy(alpha = 0.32f),
                    topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius
                )
            }
        }
        if (peakHour in 0..23) {
            Text(
                text = peakLabel + " " + localizedHour(peakHour, locale),
                color = DerbisMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DnaTrackRow(
    track: PulseTrack,
    position: DerbisConnectedPosition,
    style: DerbisConnectedStyle,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .derbisConnectedSurface(position, style)
            .derbisPressable(
                onClick = onClick,
                pressedScale = DerbisPressScale.Row,
                role = Role.Button,
                onClickLabel = track.title
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = track.title,
                color = DerbisText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = DerbisMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = track.plays.toString(),
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}

private fun localizedHour(hour: Int, locale: Locale): String {
    val safeHour = hour.coerceIn(0, 23)
    return LocalTime.of(safeHour, 0)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
}
