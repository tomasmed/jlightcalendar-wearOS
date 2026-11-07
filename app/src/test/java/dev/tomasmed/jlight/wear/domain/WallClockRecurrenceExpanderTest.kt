package dev.tomasmed.jlight.wear.domain

import dev.tomasmed.jlight.wear.domain.model.CalendarEvent
import dev.tomasmed.jlight.wear.domain.rrule.WallClockRecurrenceExpander
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WallClockRecurrenceExpanderTest {
    @Test
    fun `single non-recurring event inside window is returned with exact local time`() {
        val event = CalendarEvent(
            id = "evt-1",
            galaxyId = "galaxy-alpha",
            title = "Pediatrician Visit",
            date = LocalDate.of(2026, 11, 10),
            time = LocalTime.of(14, 30),
            endTime = LocalTime.of(15, 15),
            memberIds = listOf("Maya"),
        )

        val results = WallClockRecurrenceExpander.expandEventsInWindow(
            events = listOf(event),
            windowStart = LocalDate.of(2026, 11, 1),
            windowEnd = LocalDate.of(2026, 11, 30),
        )

        assertEquals(1, results.size)
        val occ = results.single()
        assertEquals("evt-1", occ.occurrenceId)
        assertEquals(LocalDate.of(2026, 11, 10), occ.occurrenceDate)
        assertEquals(LocalTime.of(14, 30), occ.startTime)
        assertEquals(listOf("Maya"), occ.memberIds)
    }

    @Test
    fun `weekly recurring event expands across window and filters out exdates`() {
        val startDate = LocalDate.of(2026, 11, 2) // Monday
        val skippedMonday = LocalDate.of(2026, 11, 9)
        val event = CalendarEvent(
            id = "evt-weekly",
            galaxyId = "galaxy-alpha",
            title = "Trash & Recycling Night",
            date = startDate,
            time = LocalTime.of(19, 0),
            rrule = "FREQ=WEEKLY;INTERVAL=1",
            exdates = setOf(skippedMonday),
        )

        val results = WallClockRecurrenceExpander.expandEventsInWindow(
            events = listOf(event),
            windowStart = LocalDate.of(2026, 11, 1),
            windowEnd = LocalDate.of(2026, 11, 23),
        )

        val expectedDates = listOf(
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 16),
            LocalDate.of(2026, 11, 23),
        )
        assertEquals(expectedDates, results.map { it.occurrenceDate })
        assertEquals(
            listOf("evt-weekly_2026-11-02", "evt-weekly_2026-11-16", "evt-weekly_2026-11-23"),
            results.map { it.occurrenceId },
        )
    }

    @Test
    fun `weekday BYDAY rule skips weekends and respects COUNT limit`() {
        val fridayStart = LocalDate.of(2026, 11, 6) // Friday
        val event = CalendarEvent(
            id = "evt-standup",
            galaxyId = "galaxy-alpha",
            title = "School Dropoff",
            date = fridayStart,
            time = LocalTime.of(7, 45),
            rrule = "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;COUNT=4",
        )

        val results = WallClockRecurrenceExpander.expandEventsInWindow(
            events = listOf(event),
            windowStart = LocalDate.of(2026, 11, 6),
            windowEnd = LocalDate.of(2026, 11, 20),
        )

        val expectedDates = listOf(
            LocalDate.of(2026, 11, 6),  // Fri
            LocalDate.of(2026, 11, 9),  // Mon
            LocalDate.of(2026, 11, 10), // Tue
            LocalDate.of(2026, 11, 11), // Wed
        )
        assertEquals(expectedDates, results.map { it.occurrenceDate })
    }

    @Test
    fun `biweekly INTERVAL=2 rule emits every second week only`() {
        val event = CalendarEvent(
            id = "evt-biweekly",
            galaxyId = "galaxy-alpha",
            title = "Biweekly Lawn Care",
            date = LocalDate.of(2026, 11, 3), // Tuesday
            rrule = "FREQ=WEEKLY;INTERVAL=2",
        )

        val results = WallClockRecurrenceExpander.expandEventsInWindow(
            events = listOf(event),
            windowStart = LocalDate.of(2026, 11, 1),
            windowEnd = LocalDate.of(2026, 12, 5),
        )

        val expectedDates = listOf(
            LocalDate.of(2026, 11, 3),
            LocalDate.of(2026, 11, 17),
            LocalDate.of(2026, 12, 1),
        )
        assertEquals(expectedDates, results.map { it.occurrenceDate })
    }

    @Test
    fun `monthly event on 31st skips shorter months without drifting day of month`() {
        val event = CalendarEvent(
            id = "evt-monthly-31",
            galaxyId = "galaxy-alpha",
            title = "End of Month Budget Check",
            date = LocalDate.of(2026, 1, 31),
            rrule = "FREQ=MONTHLY;INTERVAL=1",
        )

        val results = WallClockRecurrenceExpander.expandEventsInWindow(
            events = listOf(event),
            windowStart = LocalDate.of(2026, 1, 1),
            windowEnd = LocalDate.of(2026, 5, 31),
        )

        val expectedDates = listOf(
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 5, 31),
        )
        assertEquals(expectedDates, results.map { it.occurrenceDate })
    }

    @Test
    fun `invalid window or malformed rrule returns empty list safely`() {
        val event = CalendarEvent(
            id = "evt-bad",
            galaxyId = "galaxy-alpha",
            title = "Malformed",
            date = LocalDate.of(2026, 11, 7),
            rrule = "FREQ=UNKNOWN_RULE",
        )

        val malformedResult = WallClockRecurrenceExpander.expandEventsInWindow(
            events = listOf(event),
            windowStart = LocalDate.of(2026, 11, 1),
            windowEnd = LocalDate.of(2026, 11, 30),
        )
        assertTrue(malformedResult.isEmpty())

        val invertedWindowResult = WallClockRecurrenceExpander.expandEventsInWindow(
            events = listOf(event.copy(rrule = null)),
            windowStart = LocalDate.of(2026, 11, 30),
            windowEnd = LocalDate.of(2026, 11, 1),
        )
        assertTrue(invertedWindowResult.isEmpty())
    }
}
