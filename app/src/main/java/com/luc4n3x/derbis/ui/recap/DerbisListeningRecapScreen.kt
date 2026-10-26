package com.luc4n3x.derbis.ui.recap

import com.luc4n3x.derbis.ui.overlayScreenBottomPadding
import com.luc4n3x.derbis.ui.components.DerbisBackButton
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.derbis.domain.recap.Daypart
import com.luc4n3x.derbis.domain.recap.ListeningPulsePoint
import com.luc4n3x.derbis.domain.recap.ListeningRecapPeriod
import com.luc4n3x.derbis.domain.recap.ListeningRecapSummary
import com.luc4n3x.derbis.domain.recap.RecapHighlightStat
import com.luc4n3x.derbis.domain.recap.TopAlbumStat
import com.luc4n3x.derbis.domain.recap.TopArtistStat
import com.luc4n3x.derbis.domain.recap.TopTrackStat
import com.luc4n3x.derbis.ui.components.DerbisArtistAvatar
import com.luc4n3x.derbis.ui.components.DerbisConnectedDefaults
import com.luc4n3x.derbis.ui.components.DerbisConnectedPosition
import com.luc4n3x.derbis.ui.components.DerbisConnectedStyle
import com.luc4n3x.derbis.ui.components.DerbisPressScale
import com.luc4n3x.derbis.ui.components.DerbisSkeletonBlock
import com.luc4n3x.derbis.ui.components.derbisArtistAccent
import com.luc4n3x.derbis.ui.components.derbisConnectedRowSurface
import com.luc4n3x.derbis.ui.components.derbisConnectedSurface
import com.luc4n3x.derbis.ui.components.derbisPressable
import com.luc4n3x.derbis.ui.i18n.DerbisStrings
import com.luc4n3x.derbis.ui.i18n.LocalDerbisStrings
import com.luc4n3x.derbis.ui.theme.DerbisBlack
import com.luc4n3x.derbis.ui.theme.DerbisCyan
import com.luc4n3x.derbis.ui.theme.DerbisInk
import com.luc4n3x.derbis.ui.theme.DerbisMuted
import com.luc4n3x.derbis.ui.theme.DerbisOrange
import com.luc4n3x.derbis.ui.theme.DerbisPanel
import com.luc4n3x.derbis.ui.theme.DerbisPink
import com.luc4n3x.derbis.ui.theme.DerbisText
import com.luc4n3x.derbis.ui.theme.DerbisTypeRhythm
import com.luc4n3x.derbis.ui.theme.DerbisViolet
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.format.TextStyle as DayTextStyle

private val RecapPeriods = listOf(
    ListeningRecapPeriod.Days7,
    ListeningRecapPeriod.Days30,
    ListeningRecapPeriod.Days365,
    ListeningRecapPeriod.AllTime
)

@Composable
fun DerbisListeningRecapOverlay(
    recap: ListeningRecapSummary,
    period: ListeningRecapPeriod,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onSelectPeriod: (ListeningRecapPeriod) -> Unit,
    onPlayTrack: ((TopTrackStat) -> Unit)? = null,
    onOpenArtist: ((String) -> Unit)? = null,
    onClose: () -> Unit
) {
    val strings = LocalDerbisStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val isDark = MaterialTheme.colorScheme.background.run { (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5 }
    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(DerbisInk, DerbisBlack))
    } else {
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background))
    }

    val style = DerbisConnectedDefaults.style(accent = DerbisCyan)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = overlayScreenBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(contentType = "recap-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DerbisBackButton(onClick = onClose, contentDescription = strings.back)
                    Column(modifier = Modifier.padding(start = 6.dp)) {
                        Text(
                            text = strings.listeningRecap,
                            color = DerbisText,
                            fontSize = 24.sp,
                            lineHeight = DerbisTypeRhythm.lineHeight(24.sp),
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = strings.recapSubtitle,
                            color = DerbisMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item(contentType = "recap-period-selector") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(style.gap)
                ) {
                    RecapPeriods.forEachIndexed { index, entry ->
                        RecapPeriodChip(
                            label = periodLabel(entry, strings),
                            selected = entry == period,
                            position = DerbisConnectedPosition.of(index, RecapPeriods.size),
                            style = style,
                            accent = DerbisCyan,
                            onClick = { onSelectPeriod(entry) }
                        )
                    }
                }
            }

            if (loading && !recap.hasSignal) {
                item(contentType = "recap-loading") {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DerbisSkeletonBlock(width = 240.dp, height = 32.dp, accent = DerbisCyan)
                        DerbisSkeletonBlock(width = 340.dp, height = 110.dp, accent = DerbisCyan)
                        DerbisSkeletonBlock(width = 340.dp, height = 80.dp, accent = DerbisCyan)
                        DerbisSkeletonBlock(width = 340.dp, height = 80.dp, accent = DerbisCyan)
                    }
                }
            } else if (!recap.hasSignal) {
                item(contentType = "recap-empty") {
                    RecapEmptyView(strings = strings)
                }
            } else {
                item(contentType = "recap-hero") {
                    RecapHeroCard(
                        recap = recap,
                        period = period,
                        strings = strings,
                        number = number,
                        isDark = isDark
                    )
                }

                item(contentType = "recap-highlights-title") {
                    RecapSectionTitle(title = strings.highlightsTitle, icon = Icons.Rounded.AutoAwesome, accent = DerbisViolet)
                }
                item(contentType = "recap-highlights") {
                    RecapHighlightsGrid(
                        recap = recap,
                        strings = strings,
                        locale = locale,
                        number = number,
                        isDark = isDark
                    )
                }

                if (recap.topTracks.isNotEmpty()) {
                    item(contentType = "recap-tracks-title") {
                        RecapSectionTitle(title = strings.topTracksTitle, icon = Icons.Rounded.Headphones, accent = DerbisCyan)
                    }
                    itemsIndexed(
                        recap.topTracks,
                        key = { _, track -> "recap-track-${track.trackId}-${track.rank}" },
                        contentType = { _, _ -> "recap-track-row" }
                    ) { index, track ->
                        TopTrackRow(
                            track = track,
                            position = DerbisConnectedPosition.of(index, recap.topTracks.size),
                            style = style,
                            strings = strings,
                            number = number,
                            onPlay = { onPlayTrack?.invoke(track) }
                        )
                    }
                }

                if (recap.topArtists.isNotEmpty()) {
                    item(contentType = "recap-artists-title") {
                        RecapSectionTitle(title = strings.topArtistsTitle, icon = Icons.Rounded.Person, accent = DerbisPink)
                    }
                    itemsIndexed(
                        recap.topArtists,
                        key = { _, artist -> "recap-artist-${artist.name}-${artist.rank}" },
                        contentType = { _, _ -> "recap-artist-row" }
                    ) { index, artist ->
                        TopArtistRow(
                            artist = artist,
                            position = DerbisConnectedPosition.of(index, recap.topArtists.size),
                            style = style,
                            strings = strings,
                            number = number,
                            onOpen = { onOpenArtist?.invoke(artist.name) }
                        )
                    }
                }

                if (recap.topAlbums.isNotEmpty()) {
                    item(contentType = "recap-albums-title") {
                        RecapSectionTitle(title = strings.topAlbumsTitle, icon = Icons.Rounded.Album, accent = DerbisOrange)
                    }
                    itemsIndexed(
                        recap.topAlbums,
                        key = { _, album -> "recap-album-${album.title}-${album.rank}" },
                        contentType = { _, _ -> "recap-album-row" }
                    ) { index, album ->
                        TopAlbumRow(
                            album = album,
                            position = DerbisConnectedPosition.of(index, recap.topAlbums.size),
                            style = style,
                            strings = strings,
                            number = number
                        )
                    }
                }

                if (recap.dailyActivity.any { it.listenedMs > 0L }) {
                    item(contentType = "recap-pulse-title") {
                        val chartTitle = if (period == ListeningRecapPeriod.Days365 || period == ListeningRecapPeriod.AllTime) {
                            strings.recapRecentActivity
                        } else {
                            strings.pulseProActivity
                        }
                        val chartSubtitle = if (period == ListeningRecapPeriod.Days365 || period == ListeningRecapPeriod.AllTime) {
                            strings.recapActivityLast30Days
                        } else {
                            null
                        }
                        RecapSectionTitle(
                            title = chartTitle,
                            subtitle = chartSubtitle,
                            icon = Icons.Rounded.CalendarMonth,
                            accent = DerbisCyan
                        )
                    }
                    item(contentType = "recap-pulse-chart") {
                        RecapTimelineChart(
                            points = recap.dailyActivity,
                            locale = locale,
                            strings = strings,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.RecapPeriodChip(
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
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) accent else DerbisMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecapHeroCard(
    recap: ListeningRecapSummary,
    period: ListeningRecapPeriod,
    strings: DerbisStrings,
    number: NumberFormat,
    isDark: Boolean
) {
    val shape = RoundedCornerShape(26.dp)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.08f)
    val panelBg = if (isDark) DerbisPanel.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = panelBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            DerbisCyan.copy(alpha = if (isDark) 0.12f else 0.08f),
                            DerbisViolet.copy(alpha = if (isDark) 0.08f else 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DerbisCyan.copy(alpha = 0.14f))
                        .border(1.dp, DerbisCyan.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = periodLabel(period, strings).uppercase(),
                        color = DerbisCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = formatRecapDuration(recap.totalListenMs, strings, number),
                        color = DerbisText,
                        fontSize = 38.sp,
                        lineHeight = DerbisTypeRhythm.lineHeight(38.sp),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                    Text(
                        text = strings.pulseMinutes,
                        color = DerbisMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecapMiniStat(
                        icon = Icons.Rounded.PlayArrow,
                        value = number.format(recap.totalPlays),
                        label = strings.pulsePlays,
                        accent = DerbisCyan,
                        isDark = isDark
                    )
                    RecapMiniStat(
                        icon = Icons.Rounded.MusicNote,
                        value = number.format(recap.uniqueTracks),
                        label = strings.statTracks,
                        accent = DerbisViolet,
                        isDark = isDark
                    )
                    RecapMiniStat(
                        icon = Icons.Rounded.Person,
                        value = number.format(recap.uniqueArtists),
                        label = strings.statArtists,
                        accent = DerbisPink,
                        isDark = isDark
                    )
                    if (recap.uniqueAlbums > 0) {
                        RecapMiniStat(
                            icon = Icons.Rounded.Album,
                            value = number.format(recap.uniqueAlbums),
                            label = strings.albumsPlain,
                            accent = DerbisOrange,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.RecapMiniStat(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    isDark: Boolean = true
) {
    val bg = if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.03f)
    val border = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
        Text(text = value, color = DerbisText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(
            text = label,
            color = DerbisMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecapHighlightsGrid(
    recap: ListeningRecapSummary,
    strings: DerbisStrings,
    locale: Locale,
    number: NumberFormat,
    isDark: Boolean
) {
    val highlights = recap.highlights
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }
    val percentFormat = remember(locale) {
        NumberFormat.getPercentInstance(locale).apply {
            maximumFractionDigits = 0
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RecapHighlightCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.LocalFireDepartment,
                accent = DerbisOrange,
                title = strings.streakHighlight,
                headline = "${number.format(highlights.currentStreakDays)} ${strings.recapUnitDays}",
                subtitle = if (highlights.bestStreakDays > 0) {
                    strings.recapStreakMax.replace("%s", number.format(highlights.bestStreakDays))
                } else "",
                isDark = isDark
            )
            val daypartName = highlights.favoriteDaypart?.let { formatDaypart(it, strings) }.orEmpty()
            val hourText = if (highlights.favoriteHour in 0..23) {
                LocalTime.of(highlights.favoriteHour, 0).format(timeFormatter)
            } else ""
            if (daypartName.isNotBlank()) {
                RecapHighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Schedule,
                    accent = DerbisViolet,
                    title = strings.favoriteTimeHighlight,
                    headline = daypartName,
                    subtitle = if (hourText.isNotBlank()) "${strings.pulseProPeak}: $hourText" else "",
                    isDark = isDark
                )
            } else {
                RecapHighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.CheckCircle,
                    accent = DerbisViolet,
                    title = strings.recapCompletionRate,
                    headline = percentFormat.format(recap.completionRate / 100.0),
                    subtitle = "${number.format(recap.totalPlays)} ${strings.pulsePlays}",
                    isDark = isDark
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val mostActiveText = highlights.mostActiveDayDate?.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            ) ?: ""
            RecapHighlightCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.CalendarMonth,
                accent = DerbisCyan,
                title = strings.mostActiveDayHighlight,
                headline = if (highlights.mostActiveDayMinutes > 0) "${number.format(highlights.mostActiveDayMinutes)} ${strings.recapUnitMinutes}" else "—",
                subtitle = mostActiveText,
                isDark = isDark
            )
            if (highlights.discoveryRate >= 0) {
                RecapHighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Explore,
                    accent = DerbisPink,
                    title = strings.discoveryHighlight,
                    headline = percentFormat.format(highlights.discoveryRate / 100.0),
                    subtitle = "${percentFormat.format(highlights.repeatRate / 100.0)} ${strings.recapRepeatLabel}",
                    isDark = isDark
                )
            } else {
                RecapHighlightCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Schedule,
                    accent = DerbisPink,
                    title = strings.pulseProAverage,
                    headline = "${number.format(highlights.averageMinutesPerDay)} ${strings.recapUnitMinutes}",
                    subtitle = periodLabel(recap.period, strings),
                    isDark = isDark
                )
            }
        }

        if (highlights.mostReplayedTrack != null) {
            val track = highlights.mostReplayedTrack
            RecapHighlightCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Replay,
                accent = Color(0xFFFFC857),
                title = strings.replayHighlight,
                headline = track.title,
                subtitle = "${track.artist} · ${number.format(track.plays)} ${strings.pulsePlays}",
                isDark = isDark
            )
        }
    }
}

@Composable
private fun RecapHighlightCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    accent: Color,
    title: String,
    headline: String,
    subtitle: String,
    isDark: Boolean
) {
    val cardShape = RoundedCornerShape(18.dp)
    val panelBg = if (isDark) DerbisPanel.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)

    Surface(
        modifier = modifier,
        shape = cardShape,
        color = panelBg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Text(
                    text = title,
                    color = DerbisMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = headline,
                color = DerbisText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = DerbisMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TopTrackRow(
    track: TopTrackStat,
    position: DerbisConnectedPosition,
    style: DerbisConnectedStyle,
    strings: DerbisStrings,
    number: NumberFormat,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .derbisConnectedSurface(position, style)
            .derbisPressable(
                onClick = onPlay,
                pressedScale = DerbisPressScale.Row,
                role = Role.Button,
                onClickLabel = track.title
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "#${number.format(track.rank)}",
            color = if (track.rank == 1) DerbisCyan else DerbisMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            if (track.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = DerbisMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

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
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "${number.format(track.plays)} ${strings.pulsePlays}",
                color = DerbisCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${number.format(track.totalMinutes)} ${strings.recapUnitMinutes}",
                color = DerbisMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TopArtistRow(
    artist: TopArtistStat,
    position: DerbisConnectedPosition,
    style: DerbisConnectedStyle,
    strings: DerbisStrings,
    number: NumberFormat,
    onOpen: () -> Unit
) {
    val artistAccents = remember(artist.name) { derbisArtistAccent(artist.name) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .derbisConnectedSurface(position, style)
            .derbisPressable(
                onClick = onOpen,
                pressedScale = DerbisPressScale.Row,
                role = Role.Button,
                onClickLabel = artist.name
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "#${number.format(artist.rank)}",
            color = if (artist.rank == 1) DerbisPink else DerbisMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        DerbisArtistAvatar(
            name = artist.name,
            thumbnailUrl = artist.thumbnailUrl,
            accentStart = artistAccents.first,
            accentEnd = artistAccents.second,
            size = 44.dp
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = artist.name,
                color = DerbisText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (artist.trackCount > 0) {
                Text(
                    text = strings.formatTrackCount(artist.trackCount),
                    color = DerbisMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "${number.format(artist.plays)} ${strings.pulsePlays}",
                color = DerbisPink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${number.format(artist.totalMinutes)} ${strings.recapUnitMinutes}",
                color = DerbisMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TopAlbumRow(
    album: TopAlbumStat,
    position: DerbisConnectedPosition,
    style: DerbisConnectedStyle,
    strings: DerbisStrings,
    number: NumberFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .derbisConnectedSurface(position, style)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "#${number.format(album.rank)}",
            color = if (album.rank == 1) DerbisOrange else DerbisMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DerbisOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (album.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = album.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    tint = DerbisOrange,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = album.title,
                color = DerbisText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artist,
                color = DerbisMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "${number.format(album.plays)} ${strings.pulsePlays}",
                color = DerbisOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${number.format(album.totalMinutes)} ${strings.recapUnitMinutes}",
                color = DerbisMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RecapTimelineChart(
    points: List<ListeningPulsePoint>,
    locale: Locale,
    strings: DerbisStrings,
    isDark: Boolean
) {
    val peakMs = points.maxOfOrNull { it.listenedMs } ?: 0L
    val panelBg = if (isDark) DerbisPanel.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = panelBg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { point ->
                    val fraction = if (peakMs > 0L) {
                        (point.listenedMs.toFloat() / peakMs.toFloat()).coerceIn(0.08f, 1f)
                    } else {
                        0.08f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (point.listenedMs == peakMs && peakMs > 0L) {
                                    DerbisCyan
                                } else if (point.listenedMs > 0L) {
                                    DerbisCyan.copy(alpha = 0.65f)
                                } else {
                                    Color.White.copy(alpha = 0.06f)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun RecapSectionTitle(
    title: String,
    icon: ImageVector,
    accent: Color,
    subtitle: String? = null
) {
    Column(modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(
                text = title,
                color = DerbisText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = DerbisMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 24.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun RecapEmptyView(strings: DerbisStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DerbisCyan.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Headphones,
                contentDescription = null,
                tint = DerbisCyan,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = strings.emptyRecapTitle,
            color = DerbisText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = strings.emptyRecapSubtitle,
            color = DerbisMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

private fun periodLabel(period: ListeningRecapPeriod, strings: DerbisStrings): String = when (period) {
    ListeningRecapPeriod.Days7 -> strings.recapPeriod7Days
    ListeningRecapPeriod.Days30 -> strings.recapPeriod30Days
    ListeningRecapPeriod.Days365 -> strings.recapPeriod365Days
    ListeningRecapPeriod.AllTime -> strings.recapPeriodAllTime
}

private fun formatRecapDuration(listenedMs: Long, strings: DerbisStrings, number: NumberFormat): String {
    val totalMinutes = listenedMs / 60_000L
    val hours = totalMinutes / 60L
    val remainingMinutes = totalMinutes % 60L
    return when {
        hours > 0 && remainingMinutes > 0 -> "${number.format(hours)} ${strings.recapUnitHours} ${number.format(remainingMinutes)} ${strings.recapUnitMinutes}"
        hours > 0 -> "${number.format(hours)} ${strings.recapUnitHours}"
        else -> "${number.format(totalMinutes)} ${strings.recapUnitMinutes}"
    }
}

private fun formatDaypart(daypart: Daypart, strings: DerbisStrings): String = when (daypart) {
    Daypart.Morning -> strings.daypartMorning
    Daypart.Afternoon -> strings.daypartAfternoon
    Daypart.Evening -> strings.daypartEvening
    Daypart.Night -> strings.daypartNight
}
