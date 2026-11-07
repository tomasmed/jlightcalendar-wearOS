package dev.tomasmed.jlight.wear.domain.rrule

import dev.tomasmed.jlight.wear.domain.model.CalendarEvent
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Expands [CalendarEvent] records (including RFC 5545 `RRULE` strings and `exdates` exclusions)
 * strictly in local wall-clock time ([LocalDate] and [java.time.LocalTime]) with zero UTC conversion,
 * preserving exact schedule parity with upstream JlightCalendar (ADR 0013 / ADR 0004).
 */
object WallClockRecurrenceExpander {
    private const val MAX_SAFETY_ITERATIONS = 1_500

    fun expandEventsInWindow(
        events: List<CalendarEvent>,
        windowStart: LocalDate,
        windowEnd: LocalDate,
    ): List<EventOccurrence> {
        if (windowEnd.isBefore(windowStart)) return emptyList()

        return events
            .flatMap { event -> expandSingleEvent(event, windowStart, windowEnd) }
            .sortedWith(
                compareBy<EventOccurrence> { it.occurrenceDate }
                    .thenBy { it.startTime ?: java.time.LocalTime.MIN }
                    .thenBy { it.title },
            )
    }

    fun expandSingleEvent(
        event: CalendarEvent,
        windowStart: LocalDate,
        windowEnd: LocalDate,
    ): List<EventOccurrence> {
        val rawRule = event.rrule?.trim().orEmpty()
        if (rawRule.isEmpty() || rawRule.equals("NONE", ignoreCase = true)) {
            val fallsInWindow = !event.date.isBefore(windowStart) && !event.date.isAfter(windowEnd)
            val isExcluded = event.exdates.contains(event.date)
            if (!fallsInWindow || isExcluded) return emptyList()
            return listOf(event.toOccurrence(event.date, isRecurring = false))
        }

        val parsed = parseRRule(rawRule) ?: return emptyList()
        if (event.date.isAfter(windowEnd)) return emptyList()

        val occurrences = mutableListOf<EventOccurrence>()
        var emittedTotalCount = 0
        var iterations = 0

        when (parsed.freq) {
            Frequency.DAILY -> {
                var cursor = event.date
                while (!cursor.isAfter(windowEnd) && iterations < MAX_SAFETY_ITERATIONS) {
                    iterations++
                    if (parsed.until != null && cursor.isAfter(parsed.until)) break
                    if (parsed.byDay.isEmpty() || parsed.byDay.contains(cursor.dayOfWeek)) {
                        emittedTotalCount++
                        if (parsed.count != null && emittedTotalCount > parsed.count) break
                        if (!cursor.isBefore(windowStart) && !event.exdates.contains(cursor)) {
                            occurrences.add(event.toOccurrence(cursor, isRecurring = true))
                        }
                    }
                    cursor = cursor.plusDays(parsed.interval.toLong())
                }
            }

            Frequency.WEEKLY -> {
                val targetDays = parsed.byDay.ifEmpty { setOf(event.date.dayOfWeek) }
                val effectiveWkst = parsed.wkst
                    ?: if (event.date.dayOfWeek == DayOfWeek.SUNDAY && targetDays.size > 1) {
                        DayOfWeek.SUNDAY
                    } else {
                        DayOfWeek.MONDAY
                    }
                val startWeekAnchor = startOfWeek(event.date, effectiveWkst)
                var cursor = event.date
                while (!cursor.isAfter(windowEnd) && iterations < MAX_SAFETY_ITERATIONS) {
                    iterations++
                    if (parsed.until != null && cursor.isAfter(parsed.until)) break

                    val weeksSinceStart = ChronoUnit.WEEKS.between(
                        startWeekAnchor,
                        startOfWeek(cursor, effectiveWkst),
                    )
                    if (weeksSinceStart >= 0 && weeksSinceStart % parsed.interval == 0L && targetDays.contains(cursor.dayOfWeek)) {
                        emittedTotalCount++
                        if (parsed.count != null && emittedTotalCount > parsed.count) break
                        if (!cursor.isBefore(windowStart) && !event.exdates.contains(cursor)) {
                            occurrences.add(event.toOccurrence(cursor, isRecurring = true))
                        }
                    }
                    cursor = cursor.plusDays(1)
                }
            }

            Frequency.MONTHLY -> {
                val dayOfMonth = event.date.dayOfMonth
                var monthStep = 0L
                while (iterations < MAX_SAFETY_ITERATIONS) {
                    iterations++
                    val candidateMonth = event.date.plusMonths(monthStep * parsed.interval)
                    if (candidateMonth.dayOfMonth != dayOfMonth) {
                        // Skip months that do not contain the target day (e.g. Feb 30)
                        monthStep++
                        if (candidateMonth.isAfter(windowEnd)) break
                        continue
                    }
                    if (candidateMonth.isAfter(windowEnd)) break
                    if (parsed.until != null && candidateMonth.isAfter(parsed.until)) break

                    emittedTotalCount++
                    if (parsed.count != null && emittedTotalCount > parsed.count) break
                    if (!candidateMonth.isBefore(windowStart) && !event.exdates.contains(candidateMonth)) {
                        occurrences.add(event.toOccurrence(candidateMonth, isRecurring = true))
                    }
                    monthStep++
                }
            }
        }

        return occurrences
    }

    private fun startOfWeek(
        date: LocalDate,
        wkst: DayOfWeek,
    ): LocalDate {
        val diff = (date.dayOfWeek.value - wkst.value + 7) % 7
        return date.minusDays(diff.toLong())
    }

    private fun CalendarEvent.toOccurrence(
        occurrenceDate: LocalDate,
        isRecurring: Boolean,
    ): EventOccurrence =
        EventOccurrence(
            masterEventId = id,
            occurrenceId = if (isRecurring) "${id}_$occurrenceDate" else id,
            title = title,
            occurrenceDate = occurrenceDate,
            startTime = time,
            endTime = endTime,
            memberIds = memberIds.ifEmpty { listOf("Everyone") },
            isRecurring = isRecurring,
            location = location,
            description = description,
        )

    private enum class Frequency {
        DAILY,
        WEEKLY,
        MONTHLY,
    }

    private data class ParsedRRule(
        val freq: Frequency,
        val interval: Int,
        val byDay: Set<DayOfWeek>,
        val count: Int?,
        val until: LocalDate?,
        val wkst: DayOfWeek? = null,
    )

    private val WEEKDAY_SET = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

    private fun parseRRule(rrule: String): ParsedRRule? {
        val cleaned = rrule.removePrefix("RRULE:").trim()
        if (cleaned.isEmpty()) return null

        when (cleaned.uppercase()) {
            "DAILY" -> return ParsedRRule(Frequency.DAILY, interval = 1, byDay = emptySet(), count = null, until = null)
            "WEEKDAY" -> return ParsedRRule(Frequency.WEEKLY, interval = 1, byDay = WEEKDAY_SET, count = null, until = null)
            "WEEKLY" -> return ParsedRRule(Frequency.WEEKLY, interval = 1, byDay = emptySet(), count = null, until = null)
            "BIWEEKLY" -> return ParsedRRule(Frequency.WEEKLY, interval = 2, byDay = emptySet(), count = null, until = null)
            "MONTHLY" -> return ParsedRRule(Frequency.MONTHLY, interval = 1, byDay = emptySet(), count = null, until = null)
        }

        val parts = cleaned.split(";").mapNotNull { token ->
            val idx = token.indexOf('=')
            if (idx <= 0) null else token.substring(0, idx).trim().uppercase() to token.substring(idx + 1).trim().uppercase()
        }.toMap()

        val rawFreq = parts["FREQ"] ?: return null
        val freq = when (rawFreq) {
            "DAILY" -> Frequency.DAILY
            "WEEKLY", "WEEKDAY", "BIWEEKLY" -> Frequency.WEEKLY
            "MONTHLY" -> Frequency.MONTHLY
            else -> return null
        }

        val defaultInterval = if (rawFreq == "BIWEEKLY") 2 else 1
        val interval = parts["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: defaultInterval
        val count = parts["COUNT"]?.toIntOrNull()?.coerceAtLeast(1)
        val until = parts["UNTIL"]?.let { parseUntilDate(it) }
        val wkst = parts["WKST"]?.let { parseDayOfWeek(it) }
        val parsedByDay = parts["BYDAY"]
            ?.split(",")
            ?.mapNotNull { code -> parseDayOfWeek(code.trim()) }
            ?.toSet()
            .orEmpty()
        val byDay = if (rawFreq == "WEEKDAY" && parsedByDay.isEmpty()) WEEKDAY_SET else parsedByDay

        return ParsedRRule(
            freq = freq,
            interval = interval,
            byDay = byDay,
            count = count,
            until = until,
            wkst = wkst,
        )
    }

    private fun parseUntilDate(rawUntil: String): LocalDate? =
        runCatching {
            val digits = rawUntil.filter { it.isDigit() }
            if (digits.length >= 8) {
                val year = digits.substring(0, 4).toInt()
                val month = digits.substring(4, 6).toInt()
                val day = digits.substring(6, 8).toInt()
                LocalDate.of(year, month, day)
            } else {
                LocalDate.parse(rawUntil)
            }
        }.getOrNull()

    private fun parseDayOfWeek(code: String): DayOfWeek? =
        when (code) {
            "MO" -> DayOfWeek.MONDAY
            "TU" -> DayOfWeek.TUESDAY
            "WE" -> DayOfWeek.WEDNESDAY
            "TH" -> DayOfWeek.THURSDAY
            "FR" -> DayOfWeek.FRIDAY
            "SA" -> DayOfWeek.SATURDAY
            "SU" -> DayOfWeek.SUNDAY
            else -> null
        }
}
