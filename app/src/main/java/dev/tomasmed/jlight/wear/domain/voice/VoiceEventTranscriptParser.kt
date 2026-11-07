package dev.tomasmed.jlight.wear.domain.voice

import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * Parses spoken Wear OS voice transcripts (e.g. `"Dentist tomorrow at 3:30 PM"`,
 * `"Family Taco Night at 6pm"`, `"Soccer practice today at 10am"`) into structured
 * local wall-clock [VoiceEventIntent] values with zero UTC timezone drift.
 */
object VoiceEventTranscriptParser {
    data class VoiceEventIntent(
        val title: String,
        val date: LocalDate,
        val startTime: LocalTime?,
    )

    private val TIME_REGEX = Regex(
        pattern = """\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)\b""",
        option = RegexOption.IGNORE_CASE,
    )

    private val TWENTY_FOUR_HOUR_REGEX = Regex(
        pattern = """\bat\s+([01]?\d|2[0-3]):([0-5]\d)\b""",
        option = RegexOption.IGNORE_CASE,
    )

    fun parse(
        transcript: String,
        referenceDate: LocalDate = LocalDate.now(),
    ): VoiceEventIntent {
        val raw = transcript.trim()
        if (raw.isEmpty()) {
            return VoiceEventIntent(
                title = "Quick Watch Event",
                date = referenceDate,
                startTime = null,
            )
        }

        var working = raw
        var targetDate = referenceDate

        val tomorrowRegex = Regex("""\btomorrow\b""", RegexOption.IGNORE_CASE)
        val todayRegex = Regex("""\btoday\b""", RegexOption.IGNORE_CASE)

        if (tomorrowRegex.containsMatchIn(working)) {
            targetDate = referenceDate.plusDays(1)
            working = tomorrowRegex.replace(working, " ")
        } else if (todayRegex.containsMatchIn(working)) {
            targetDate = referenceDate
            working = todayRegex.replace(working, " ")
        }

        var parsedTime: LocalTime? = null
        val amPmMatch = TIME_REGEX.find(working)
        if (amPmMatch != null) {
            val hourRaw = amPmMatch.groupValues[1].toIntOrNull() ?: 9
            val minuteRaw = amPmMatch.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            val meridiem = amPmMatch.groupValues[3].lowercase(Locale.US).replace(".", "")
            val hour24 = when {
                meridiem == "pm" && hourRaw in 1..11 -> hourRaw + 12
                meridiem == "am" && hourRaw == 12 -> 0
                else -> hourRaw.coerceIn(0, 23)
            }
            parsedTime = LocalTime.of(hour24, minuteRaw.coerceIn(0, 59))
            working = working.removeRange(amPmMatch.range)
        } else {
            val h24Match = TWENTY_FOUR_HOUR_REGEX.find(working)
            if (h24Match != null) {
                val hour = h24Match.groupValues[1].toIntOrNull() ?: 9
                val minute = h24Match.groupValues[2].toIntOrNull() ?: 0
                parsedTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
                working = working.removeRange(h24Match.range)
            }
        }

        val cleanedTitle = working
            .replace(Regex("""\s+"""), " ")
            .trim()
            .removeSuffix("at")
            .removeSuffix("on")
            .trim()
            .ifEmpty { raw }

        return VoiceEventIntent(
            title = cleanedTitle,
            date = targetDate,
            startTime = parsedTime,
        )
    }
}
