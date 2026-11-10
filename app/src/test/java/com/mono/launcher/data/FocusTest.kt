package com.mono.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class FocusTest {
    private val zone = ZoneId.of("UTC")

    // 2026-11-09 is a Monday.
    private fun at(day: Int, h: Int, m: Int = 0) = ZonedDateTime.of(2026, 11, 8 + day, h, m, 0, 0, zone)

    private fun settings(start: Int, end: Int, days: Set<Int> = setOf(1, 2, 3, 4, 5), until: Long = 0) =
        Settings(focusSchedule = FocusSchedule(true, start, end, days), focusUntil = until)

    @Test fun daytimeWindow() {
        val s = settings(9 * 60, 17 * 60)
        assertTrue(Focus.state(s, at(1, 10)).active)
        assertFalse(Focus.state(s, at(1, 17)).active) // end is exclusive
        assertFalse(Focus.state(s, at(1, 8, 59)).active)
        assertEquals(LocalDateTime.of(2026, 11, 9, 17, 0), Focus.state(s, at(1, 10)).until)
    }

    @Test fun respectsDays() {
        val s = settings(9 * 60, 17 * 60)
        assertFalse(Focus.state(s, at(6, 10)).active) // Saturday
    }

    @Test fun overnightWindowBelongsToStartDay() {
        val s = settings(22 * 60, 7 * 60, days = setOf(5)) // Friday night
        assertTrue(Focus.state(s, at(5, 23)).active)
        assertTrue(Focus.state(s, at(6, 6)).active) // Saturday morning, started Friday
        assertFalse(Focus.state(s, at(6, 23)).active) // Saturday night is not scheduled
        assertEquals(LocalDateTime.of(2026, 11, 14, 7, 0), Focus.state(s, at(5, 23)).until)
    }

    @Test fun manualFocusWins() {
        val now = at(6, 12)
        val s = settings(9 * 60, 17 * 60).copy(focusUntil = now.plusHours(1).toInstant().toEpochMilli())
        val st = Focus.state(s, now)
        assertTrue(st.active)
        assertTrue(st.manual)
    }

    @Test fun disabledScheduleIsOff() {
        val s = Settings(focusSchedule = FocusSchedule(enabled = false))
        assertFalse(Focus.state(s, at(1, 10)).active)
    }
}
