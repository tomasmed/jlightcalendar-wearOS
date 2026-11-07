package dev.tomasmed.jlight.wear.domain

import dev.tomasmed.jlight.wear.complication.NextEventComplicationService
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import dev.tomasmed.jlight.wear.domain.qr.QrCodeMatrixGenerator
import dev.tomasmed.jlight.wear.domain.voice.VoiceEventTranscriptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class QrVoiceAndComplicationTest {
    @Test
    fun `QrCodeMatrixGenerator encodes claimBot URL with valid finder patterns and dimensions`() {
        val url = "https://jlightcalendar.vercel.app/?claimBot=BOT-7F9A-22C1"
        val matrix = QrCodeMatrixGenerator.encode(url)

        // Version 4 (57 UTF-8 bytes fits in 80 codewords) -> size = 17 + 4*4 = 33
        assertEquals(33, matrix.size)

        // Top-left finder pattern 7x7 outer border and 3x3 center must be dark
        assertTrue(matrix.isDark(0, 0))
        assertTrue(matrix.isDark(0, 6))
        assertTrue(matrix.isDark(6, 0))
        assertTrue(matrix.isDark(3, 3))
        // White ring inside finder pattern
        assertFalse(matrix.isDark(1, 1))
        // Separator around top-left finder pattern must be light
        assertFalse(matrix.isDark(7, 0))
        assertFalse(matrix.isDark(0, 7))
    }

    @Test
    fun `VoiceEventTranscriptParser extracts wall-clock date and time from spoken transcripts`() {
        val refDate = LocalDate.of(2026, 11, 7)

        val tomorrowPm = VoiceEventTranscriptParser.parse(
            transcript = "Pediatrician visit tomorrow at 3:30 PM",
            referenceDate = refDate,
        )
        assertEquals("Pediatrician visit", tomorrowPm.title)
        assertEquals(LocalDate.of(2026, 11, 8), tomorrowPm.date)
        assertEquals(LocalTime.of(15, 30), tomorrowPm.startTime)

        val todayAm = VoiceEventTranscriptParser.parse(
            transcript = "Soccer practice today at 10am",
            referenceDate = refDate,
        )
        assertEquals("Soccer practice", todayAm.title)
        assertEquals(refDate, todayAm.date)
        assertEquals(LocalTime.of(10, 0), todayAm.startTime)

        val allDay = VoiceEventTranscriptParser.parse(
            transcript = "Grandma Birthday",
            referenceDate = refDate,
        )
        assertEquals("Grandma Birthday", allDay.title)
        assertEquals(refDate, allDay.date)
        assertNull(allDay.startTime)
    }

    @Test
    fun `NextEventComplicationService selects next upcoming occurrence and formats SHORT_TEXT summary`() {
        val today = LocalDate.of(2026, 11, 7)
        val now = LocalTime.of(11, 0)

        val pastMorning = EventOccurrence(
            masterEventId = "evt-1",
            occurrenceId = "evt-1",
            title = "Morning Huddle",
            occurrenceDate = today,
            startTime = LocalTime.of(8, 30),
            endTime = LocalTime.of(9, 0),
        )
        val upcomingAfternoon = EventOccurrence(
            masterEventId = "evt-2",
            occurrenceId = "evt-2",
            title = "Taco Night",
            occurrenceDate = today,
            startTime = LocalTime.of(12, 15),
            endTime = LocalTime.of(13, 0),
        )

        val selected = NextEventComplicationService.selectNextUpcomingOccurrence(
            occurrences = listOf(pastMorning, upcomingAfternoon),
            today = today,
            nowTime = now,
        )
        assertNotNull(selected)
        assertEquals("evt-2", selected?.occurrenceId)

        val summary = NextEventComplicationService.formatComplicationSummary(
            occurrence = selected,
            today = today,
            nowTime = now,
        )
        assertEquals("12:15P", summary.shortTimeLabel)
        assertEquals("Taco Ni", summary.shortTitleLabel)
        assertEquals(75L, summary.minutesUntil)
    }
}
