package com.yasuda.derbis.domain

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ListeningInsightsPeriod {
    Day,
    Week,
    Month,
    HalfYear,
    AllTime
}

@Immutable
data class ListeningInsightsRange(
    val fromMs: Long,
    val toMs: Long,
    val previousFromMs: Long? = null,
    val previousToMs: Long? = null
)

@Immutable
data class ListeningInsightsMetrics(
    val listenedMs: Long = 0L,
    val countedPlays: Int = 0,
    val distinctTracks: Int = 0,
    val distinctArtists: Int = 0,
    val completionRate: Int = 0,
    val discoveryCount: Int = 0,
    val discoveryRate: Int = 0,
    val peakHour: Int = -1,
    val peakDayEpochMs: Long = 0L,
    val trendPercent: Int? = null
)

@Immutable
data class ListeningInsightsActivityPoint(
    val epochMs: Long,
    val listenedMs: Long,
    val plays: Int
)

@Immutable
data class ListeningInsightsTrack(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val listenedMs: Long = 0L,
    val plays: Int = 0
)

@Immutable
data class ListeningInsightsArtist(
    val name: String,
    val artworkUrl: String = "",
    val listenedMs: Long = 0L,
    val plays: Int = 0,
    val trackCount: Int = 0
)

@Immutable
data class ListeningInsightsAlbum(
    val album: String,
    val artist: String,
    val artworkUrl: String = "",
    val listenedMs: Long = 0L,
    val plays: Int = 0
)

/**
 * The single local day that took the most listening.
 *
 * @property averageListenedMs the typical day, so the card can say "three times a normal day"
 *   instead of a figure with nothing to compare against. Averaged over [activeDays], not over
 *   the calendar: a year with two listening days has a very high typical day.
 */
@Immutable
data class ListeningInsightsBiggestDay(
    val date: LocalDate,
    val listenedMs: Long,
    val plays: Int,
    val averageListenedMs: Long = 0L,
    val trackTitle: String = "",
    val trackArtist: String = ""
) {
    /** How many typical days this one was worth; null when there is no baseline to divide by. */
    val multipleOfTypicalDay: Int?
        get() = averageListenedMs.takeIf { it > 0L }?.let { (listenedMs / it).toInt().coerceAtLeast(1) }
}

@Immutable
data class ListeningInsightsHistoryItem(
    val id: Long,
    val track: ListeningInsightsTrack,
    val startedAt: Long,
    val listenedMs: Long,
    val completed: Boolean
)

@Immutable
data class ListeningInsightsSnapshot(
    val period: ListeningInsightsPeriod = ListeningInsightsPeriod.Week,
    val metrics: ListeningInsightsMetrics = ListeningInsightsMetrics(),
    val activity: List<ListeningInsightsActivityPoint> = emptyList(),
    val hourBuckets: List<Long> = List(24) { 0L },
    val topTracks: List<ListeningInsightsTrack> = emptyList(),
    val topArtists: List<ListeningInsightsArtist> = emptyList(),
    val detailedFromMs: Long = 0L,
    /** Only filled for a Wrapped load; every other period leaves these empty. */
    val topAlbums: List<ListeningInsightsAlbum> = emptyList(),
    val biggestDay: ListeningInsightsBiggestDay? = null,
    /** Days with at least one counted listen, for the consistency axis. */
    val activeDays: Int = 0
) {
    val hasSignal: Boolean get() = metrics.listenedMs > 0L || metrics.countedPlays > 0
}

object ListeningInsightsRanges {
    fun current(period: ListeningInsightsPeriod, nowMs: Long, zone: ZoneId): ListeningInsightsRange {
        if (period == ListeningInsightsPeriod.AllTime) {
            return ListeningInsightsRange(0L, nowMs + 1L)
        }
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        val from = when (period) {
            ListeningInsightsPeriod.Day -> now.minusHours(24)
            ListeningInsightsPeriod.Week -> now.toLocalDate().minusDays(6).atStartOfDay(zone)
            ListeningInsightsPeriod.Month -> now.toLocalDate().minusDays(29).atStartOfDay(zone)
            ListeningInsightsPeriod.HalfYear -> now.toLocalDate().minusMonths(6).atStartOfDay(zone)
            ListeningInsightsPeriod.AllTime -> error("Handled above")
        }
        val fromMs = from.toInstant().toEpochMilli()
        val duration = nowMs + 1L - fromMs
        return ListeningInsightsRange(
            fromMs = fromMs,
            toMs = nowMs + 1L,
            previousFromMs = (fromMs - duration).coerceAtLeast(0L),
            previousToMs = fromMs
        )
    }

    fun year(year: Int, nowMs: Long, zone: ZoneId): ListeningInsightsRange {
        val start = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = (nowMs + 1L).coerceIn(start + 1L, end)
        val previousStart = LocalDate.of(year - 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        return ListeningInsightsRange(
            fromMs = start,
            toMs = toMs,
            previousFromMs = previousStart,
            previousToMs = (previousStart + (toMs - start)).coerceAtMost(start)
        )
    }

    fun trendPercent(currentMs: Long, previousMs: Long): Int? {
        if (previousMs <= 0L) return null
        val percent = (currentMs - previousMs) * 100.0 / previousMs.toDouble()
        val truncated = percent.toInt()
        val adjusted = if (currentMs < previousMs && truncated == 0) -1 else truncated
        return adjusted.coerceIn(-999, 999)
    }

    fun aggregateActivity(
        period: ListeningInsightsPeriod,
        daily: List<ListeningInsightsActivityPoint>,
        zone: ZoneId
    ): List<ListeningInsightsActivityPoint> {
        if (period != ListeningInsightsPeriod.HalfYear && period != ListeningInsightsPeriod.AllTime) return daily
        val grouped = daily.groupBy { point ->
            val date = Instant.ofEpochMilli(point.epochMs).atZone(zone).toLocalDate()
            if (period == ListeningInsightsPeriod.HalfYear) {
                date.minusDays(date.dayOfWeek.value.toLong() - 1L)
            } else {
                date.withDayOfMonth(1)
            }
        }
        return grouped.entries.sortedBy { it.key }.map { (date, points) ->
            ListeningInsightsActivityPoint(
                epochMs = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                listenedMs = points.sumOf { it.listenedMs },
                plays = points.sumOf { it.plays }
            )
        }
    }

    fun fillDailyActivity(
        fromMs: Long,
        toMs: Long,
        daily: List<ListeningInsightsActivityPoint>,
        zone: ZoneId
    ): List<ListeningInsightsActivityPoint> {
        if (daily.isEmpty()) return emptyList()
        val start = Instant.ofEpochMilli(fromMs).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli((toMs - 1L).coerceAtLeast(fromMs)).atZone(zone).toLocalDate()
        val byDate = daily.associateBy { Instant.ofEpochMilli(it.epochMs).atZone(zone).toLocalDate() }
        return generateSequence(start) { date -> date.plusDays(1).takeIf { it <= end } }
            .map { date ->
                byDate[date] ?: ListeningInsightsActivityPoint(
                    epochMs = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                    listenedMs = 0L,
                    plays = 0
                )
            }
            .toList()
    }

    fun fillHourlyActivity(
        fromMs: Long,
        toMs: Long,
        hourly: List<ListeningInsightsActivityPoint>,
        zone: ZoneId
    ): List<ListeningInsightsActivityPoint> {
        val start = Instant.ofEpochMilli(fromMs).atZone(zone).truncatedTo(ChronoUnit.HOURS)
        val end = Instant.ofEpochMilli((toMs - 1L).coerceAtLeast(fromMs)).atZone(zone).truncatedTo(ChronoUnit.HOURS)
        val byHourEpoch = hourly.associateBy { point ->
            Instant.ofEpochMilli(point.epochMs).atZone(zone).truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli()
        }
        return generateSequence(start) { time ->
            time.plusHours(1).takeIf { !it.isAfter(end) }
        }.map { hourTime ->
            val epochMs = hourTime.toInstant().toEpochMilli()
            byHourEpoch[epochMs] ?: ListeningInsightsActivityPoint(
                epochMs = epochMs,
                listenedMs = 0L,
                plays = 0
            )
        }.toList()
    }

    fun dayEpoch(dayKey: String, zone: ZoneId): Long =
        LocalDate.parse(dayKey).atStartOfDay(zone).toInstant().toEpochMilli()
}

/**
 * The four shapes a listening year can take, ported from SimpMusic's Wrapped archetypes.
 *
 * SimpMusic picks one per fingerprint axis, and its fifth — the explorer — needs a "new artist"
 * counter Derbis does not keep, so the axis is left out rather than guessed at. Every axis here
 * is bounded 0..1 by construction, which is what makes them comparable enough for an argmax.
 */
enum class WrappedPersona {
    /** Music nearly every day, evenly spread. */
    THE_REGULAR,

    /** Many artists, none of them dominant. */
    THE_OMNIVORE,

    /** Most of the year went to a handful of artists. */
    THE_DEVOTEE,

    /** The same songs, again. */
    THE_DEEP_DIVER
}

@Immutable
data class WrappedPersonaProfile(
    val persona: WrappedPersona,
    val consistency: Float,
    val diversity: Float,
    val concentration: Float,
    val replay: Float
) {
    /** The four axes in the order the card draws them, already 0..1. */
    val axes: List<Pair<WrappedPersona, Float>>
        get() = listOf(
            WrappedPersona.THE_REGULAR to consistency,
            WrappedPersona.THE_OMNIVORE to diversity,
            WrappedPersona.THE_DEVOTEE to concentration,
            WrappedPersona.THE_DEEP_DIVER to replay
        )
}

/** Artists past this count read as a full diversity score: thirty is an omnivore's year. */
internal const val WRAPPED_OMNIVORE_ARTISTS = 30f

/**
 * Days of listening before the reel says anything at all.
 *
 * Below this the cards would be describing a fortnight, not a year — the same floor SimpMusic
 * puts on its own Wrapped, and the reason the story says "not enough yet" instead of printing
 * "three artists, two songs" as if it were a summary of twelve months.
 */
internal const val WRAPPED_MIN_ACTIVE_DAYS = 30

/**
 * The year's archetype, from the four things Derbis can actually measure.
 *
 * Ties are broken by declaration order, so the same year always produces the same name — the
 * card must not rename itself between two openings of the same story.
 *
 * @param daysInYear 365 or 366, never hardcoded, so a leap year does not flatter consistency.
 */
internal fun wrappedPersonaProfile(
    snapshot: ListeningInsightsSnapshot,
    daysInYear: Int = 365
): WrappedPersonaProfile {
    val totalMs = snapshot.metrics.listenedMs
    val plays = snapshot.metrics.countedPlays
    val consistency = (snapshot.activeDays.toFloat() / daysInYear.coerceAtLeast(1)).coerceIn(0f, 1f)
    val diversity = (snapshot.metrics.distinctArtists / WRAPPED_OMNIVORE_ARTISTS).coerceIn(0f, 1f)
    val concentration = if (totalMs > 0L) {
        ((snapshot.topArtists.firstOrNull()?.listenedMs ?: 0L).toFloat() / totalMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val replay = if (plays > 0) {
        ((snapshot.topTracks.firstOrNull()?.plays ?: 0) / plays.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val profile = WrappedPersonaProfile(
        persona = WrappedPersona.THE_REGULAR,
        consistency = consistency,
        diversity = diversity,
        concentration = concentration,
        replay = replay
    )
    return profile.copy(persona = profile.axes.maxByOrNull { it.second }?.first ?: WrappedPersona.THE_REGULAR)
}
