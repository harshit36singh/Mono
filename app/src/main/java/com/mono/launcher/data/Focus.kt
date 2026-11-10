package com.mono.launcher.data

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Whether focus is on right now, and until when (for the Home hint). */
data class FocusState(val active: Boolean, val until: LocalDateTime? = null, val manual: Boolean = false)

object Focus {
    /**
     * Manual focus ("focus now") wins while it lasts. Otherwise the schedule applies: a window
     * that ends before it starts runs past midnight and belongs to the day it started on.
     */
    fun state(settings: Settings, now: ZonedDateTime = ZonedDateTime.now()): FocusState {
        val nowMs = now.toInstant().toEpochMilli()
        if (settings.focusUntil > nowMs) {
            val until = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(settings.focusUntil), now.zone)
            return FocusState(true, until, manual = true)
        }
        val s = settings.focusSchedule
        if (!s.enabled || s.days.isEmpty() || s.start == s.end) return FocusState(false)

        val minute = now.hour * 60 + now.minute
        val today = now.dayOfWeek.value
        val yesterday = if (today == 1) 7 else today - 1
        val date = now.toLocalDate()
        return if (s.start < s.end) {
            if (today in s.days && minute >= s.start && minute < s.end) {
                FocusState(true, date.atStartOfDay().plusMinutes(s.end.toLong()))
            } else {
                FocusState(false)
            }
        } else {
            when {
                // Started today, runs into tomorrow.
                today in s.days && minute >= s.start -> FocusState(true, date.plusDays(1).atStartOfDay().plusMinutes(s.end.toLong()))
                // Started yesterday, still running this morning.
                yesterday in s.days && minute < s.end -> FocusState(true, date.atStartOfDay().plusMinutes(s.end.toLong()))
                else -> FocusState(false)
            }
        }
    }

    fun zone(): ZoneId = ZoneId.systemDefault()
}
