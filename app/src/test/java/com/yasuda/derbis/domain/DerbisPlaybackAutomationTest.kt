package com.yasuda.derbis.domain

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DerbisPlaybackAutomationTest {

    private val zone: ZoneId = ZoneId.of("Europe/Rome")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)

    @Test
    fun disabledScheduleHasNoTrigger() {
        val schedule = DerbisBedtimeSchedule(enabled = false)

        assertNull(nextBedtimeTrigger(schedule, at(2026, 9, 10, 20, 0)))
    }

    @Test
    fun scheduleWithoutDaysHasNoTrigger() {
        val schedule = DerbisBedtimeSchedule(enabled = true, days = emptySet())

        assertNull(nextBedtimeTrigger(schedule, at(2026, 9, 10, 20, 0)))
    }

    @Test
    fun sameDayTriggerIsUsedWhenStartIsStillAhead() {
        val schedule = DerbisBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 23 * 60 + 30,
            days = setOf(DayOfWeek.THURSDAY)
        )

        val next = nextBedtimeTrigger(schedule, at(2026, 9, 10, 20, 0))

        assertEquals(at(2026, 9, 10, 23, 30), next)
    }

    @Test
    fun startAlreadyPassedRollsOverToTheNextRepeatDay() {
        val schedule = DerbisBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 23 * 60 + 30,
            days = setOf(DayOfWeek.THURSDAY)
        )

        val next = nextBedtimeTrigger(schedule, at(2026, 9, 10, 23, 45))

        assertEquals(at(2026, 9, 17, 23, 30), next)
    }

    @Test
    fun afterMidnightStartUsesTheSameCalendarDay() {
        val schedule = DerbisBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 30,
            days = setOf(DayOfWeek.FRIDAY)
        )

        val next = nextBedtimeTrigger(schedule, at(2026, 9, 10, 23, 45))

        assertEquals(at(2026, 9, 11, 0, 30), next)
    }

    @Test
    fun weekdayScheduleSkipsTheWeekend() {
        val schedule = DerbisBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 23 * 60 + 30,
            days = DerbisBedtimeSchedule.DEFAULT_DAYS
        )

        val next = nextBedtimeTrigger(schedule, at(2026, 9, 12, 23, 45))

        assertEquals(at(2026, 9, 14, 23, 30), next)
    }

    @Test
    fun springForwardKeepsTheLocalWallClockStart() {
        val schedule = DerbisBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 4 * 60,
            days = setOf(DayOfWeek.SUNDAY)
        )

        val next = nextBedtimeTrigger(schedule, at(2026, 3, 28, 22, 0))

        assertEquals(4, next?.hour)
        assertEquals(DayOfWeek.SUNDAY, next?.dayOfWeek)
    }

    @Test
    fun fallBackKeepsTheLocalWallClockStart() {
        val schedule = DerbisBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 4 * 60,
            days = setOf(DayOfWeek.SUNDAY)
        )

        val next = nextBedtimeTrigger(schedule, at(2026, 10, 24, 22, 0))

        assertEquals(4, next?.hour)
        assertEquals(DayOfWeek.SUNDAY, next?.dayOfWeek)
    }

    @Test
    fun scheduleValuesAreClamped() {
        val schedule = DerbisBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 5_000,
            durationMinutes = 10_000
        ).normalized()

        assertEquals(DerbisBedtimeSchedule.MINUTES_PER_DAY - 1, schedule.startMinuteOfDay)
        assertEquals(DerbisBedtimeSchedule.MAX_DURATION_MINUTES, schedule.durationMinutes)
    }

    @Test
    fun fadeIsDisabledUntilTheSettingIsOn() {
        val settings = DerbisAutomationSettings(sleepFadeOutSeconds = 25)

        assertEquals(0L, settings.fadeMsFor(15 * 60_000L))
    }

    @Test
    fun fadeNeverExceedsHalfTheTimerDuration() {
        val settings = DerbisAutomationSettings(sleepFadeOutEnabled = true, sleepFadeOutSeconds = 30)

        assertEquals(30_000L, settings.fadeMsFor(15 * 60_000L))
        assertEquals(10_000L, settings.fadeMsFor(20_000L))
        assertEquals(0L, settings.fadeMsFor(0L))
    }

    @Test
    fun fadeSecondsAreClamped() {
        assertEquals(
            DerbisAutomationSettings.MAX_FADE_SECONDS,
            DerbisAutomationSettings(sleepFadeOutSeconds = 9_999).normalized().sleepFadeOutSeconds
        )
        assertEquals(
            DerbisAutomationSettings.MIN_FADE_SECONDS,
            DerbisAutomationSettings(sleepFadeOutSeconds = 0).normalized().sleepFadeOutSeconds
        )
    }
}
