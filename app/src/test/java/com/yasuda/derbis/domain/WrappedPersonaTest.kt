package com.yasuda.derbis.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WrappedPersonaTest {

    private fun snapshot(
        listenedMs: Long = 0L,
        plays: Int = 0,
        distinctArtists: Int = 0,
        activeDays: Int = 0,
        topArtistMs: Long = 0L,
        topTrackPlays: Int = 0
    ) = ListeningInsightsSnapshot(
        metrics = ListeningInsightsMetrics(
            listenedMs = listenedMs,
            countedPlays = plays,
            distinctArtists = distinctArtists
        ),
        topArtists = if (topArtistMs > 0L) {
            listOf(ListeningInsightsArtist(name = "A", listenedMs = topArtistMs, plays = 1))
        } else {
            emptyList()
        },
        topTracks = if (topTrackPlays > 0) {
            listOf(ListeningInsightsTrack(trackId = "t", title = "T", artist = "A", plays = topTrackPlays))
        } else {
            emptyList()
        },
        activeDays = activeDays
    )

    @Test
    fun aYearWithNoSignalIsStillTheRegular() {
        // Every axis is zero, so the tie falls through to declaration order rather than to null.
        assertEquals(WrappedPersona.THE_REGULAR, wrappedPersonaProfile(snapshot()).persona)
    }

    @Test
    fun musicMostDaysIsTheRegular() {
        val profile = wrappedPersonaProfile(
            snapshot(listenedMs = 100_000L, plays = 100, distinctArtists = 5, activeDays = 300),
            daysInYear = 365
        )
        assertEquals(WrappedPersona.THE_REGULAR, profile.persona)
        assertEquals(300f / 365f, profile.consistency, 0.0001f)
    }

    @Test
    fun manyArtistsIsTheOmnivore() {
        val profile = wrappedPersonaProfile(
            snapshot(listenedMs = 100_000L, plays = 100, distinctArtists = 45, activeDays = 40),
            daysInYear = 365
        )
        assertEquals(WrappedPersona.THE_OMNIVORE, profile.persona)
        // Saturates at the omnivore threshold instead of running past 1.
        assertEquals(1f, profile.diversity, 0.0001f)
    }

    @Test
    fun oneArtistOwningTheYearIsTheDevotee() {
        val profile = wrappedPersonaProfile(
            snapshot(listenedMs = 100_000L, plays = 100, distinctArtists = 6, activeDays = 60, topArtistMs = 90_000L),
            daysInYear = 365
        )
        assertEquals(WrappedPersona.THE_DEVOTEE, profile.persona)
        assertEquals(0.9f, profile.concentration, 0.0001f)
    }

    @Test
    fun theSameSongsAgainIsTheDeepDiver() {
        val profile = wrappedPersonaProfile(
            snapshot(listenedMs = 100_000L, plays = 100, distinctArtists = 6, activeDays = 60, topTrackPlays = 55),
            daysInYear = 365
        )
        assertEquals(WrappedPersona.THE_DEEP_DIVER, profile.persona)
        assertEquals(0.55f, profile.replay, 0.0001f)
    }

    @Test
    fun aLeapYearDoesNotFlatterConsistency() {
        val leap = wrappedPersonaProfile(
            snapshot(listenedMs = 100_000L, plays = 100, distinctArtists = 5, activeDays = 300),
            daysInYear = 366
        )
        assertEquals(300f / 366f, leap.consistency, 0.0001f)
    }

    @Test
    fun theAxesAreTheFourTheCardDraws() {
        val profile = WrappedPersonaProfile(
            persona = WrappedPersona.THE_OMNIVORE,
            consistency = 0.1f,
            diversity = 0.9f,
            concentration = 0.2f,
            replay = 0.3f
        )
        assertEquals(
            listOf(
                WrappedPersona.THE_REGULAR,
                WrappedPersona.THE_OMNIVORE,
                WrappedPersona.THE_DEVOTEE,
                WrappedPersona.THE_DEEP_DIVER
            ),
            profile.axes.map { it.first }
        )
        assertEquals(0.9f, profile.axes.maxByOrNull { it.second }!!.second, 0.0001f)
    }

    @Test
    fun theBiggestDayComparesAgainstTypicalDaysItself() {
        val day = ListeningInsightsBiggestDay(
            date = LocalDate.of(2025, 7, 12),
            listenedMs = 6L * 60L * 60L * 1000L,
            plays = 54,
            averageListenedMs = 2L * 60L * 60L * 1000L
        )
        assertEquals(3, day.multipleOfTypicalDay)

        // No baseline means no claim.
        assertNull(day.copy(averageListenedMs = 0L).multipleOfTypicalDay)
        // A day below the average never reads as a fraction of one.
        assertEquals(1, day.copy(listenedMs = 60_000L).multipleOfTypicalDay)
    }
}
