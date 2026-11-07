package dev.tomasmed.jlight.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dev.tomasmed.jlight.wear.data.sync.WearSyncCoordinator
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import dev.tomasmed.jlight.wear.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Watch Face Complication data source exporting the next household event in `SHORT_TEXT`
 * and `RANGED_VALUE` formats, bound to the local Room repository.
 */
class NextEventComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        when (type) {
            ComplicationType.SHORT_TEXT -> buildShortTextComplication(
                timeLabel = "8:30A",
                titleLabel = "Huddle",
                contentDescription = "Next Jlight event: 8:30 AM Family Morning Huddle",
                tapAction = null,
            )
            ComplicationType.RANGED_VALUE -> buildRangedValueComplication(
                minutesUntilEvent = 45f,
                timeLabel = "8:30A",
                titleLabel = "Huddle",
                contentDescription = "Next Jlight event in 45 minutes: Family Morning Huddle",
                tapAction = null,
            )
            else -> null
        }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val today = LocalDate.now()
        val nowTime = LocalTime.now()
        val repository = WearSyncCoordinator.provideRepository(applicationContext)
        val occurrences = repository.observeOccurrences(
            windowStart = today,
            windowEnd = today.plusDays(1),
        ).first()

        val nextOccurrence = selectNextUpcomingOccurrence(
            occurrences = occurrences,
            today = today,
            nowTime = nowTime,
        )

        val tapPendingIntent = buildTapPendingIntent(nextOccurrence?.occurrenceId)
        val summary = formatComplicationSummary(nextOccurrence, today, nowTime)

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> buildShortTextComplication(
                timeLabel = summary.shortTimeLabel,
                titleLabel = summary.shortTitleLabel,
                contentDescription = summary.contentDescription,
                tapAction = tapPendingIntent,
            )
            ComplicationType.RANGED_VALUE -> buildRangedValueComplication(
                minutesUntilEvent = summary.minutesUntil.toFloat().coerceIn(0f, 180f),
                timeLabel = summary.shortTimeLabel,
                titleLabel = summary.shortTitleLabel,
                contentDescription = summary.contentDescription,
                tapAction = tapPendingIntent,
            )
            else -> null
        }
    }

    private fun buildTapPendingIntent(occurrenceId: String?): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!occurrenceId.isNullOrBlank()) {
                putExtra(MainActivity.EXTRA_OCCURRENCE_ID, occurrenceId)
            }
        }
        return PendingIntent.getActivity(
            applicationContext,
            occurrenceId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildShortTextComplication(
        timeLabel: String,
        titleLabel: String,
        contentDescription: String,
        tapAction: PendingIntent?,
    ): ShortTextComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(titleLabel).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build(),
        )
            .setTitle(PlainComplicationText.Builder(timeLabel).build())
            .setTapAction(tapAction)
            .build()

    private fun buildRangedValueComplication(
        minutesUntilEvent: Float,
        timeLabel: String,
        titleLabel: String,
        contentDescription: String,
        tapAction: PendingIntent?,
    ): RangedValueComplicationData =
        RangedValueComplicationData.Builder(
            value = (180f - minutesUntilEvent).coerceIn(0f, 180f),
            min = 0f,
            max = 180f,
            contentDescription = PlainComplicationText.Builder(contentDescription).build(),
        )
            .setTitle(PlainComplicationText.Builder(timeLabel).build())
            .setText(PlainComplicationText.Builder(titleLabel).build())
            .setTapAction(tapAction)
            .build()

    data class ComplicationSummary(
        val shortTimeLabel: String,
        val shortTitleLabel: String,
        val contentDescription: String,
        val minutesUntil: Long,
    )

    companion object {
        private val SHORT_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mma")

        internal fun selectNextUpcomingOccurrence(
            occurrences: List<EventOccurrence>,
            today: LocalDate,
            nowTime: LocalTime,
        ): EventOccurrence? =
            occurrences.firstOrNull { occ ->
                when {
                    occ.occurrenceDate.isAfter(today) -> true
                    occ.occurrenceDate == today -> {
                        val endOrStart = occ.endTime ?: occ.startTime
                        endOrStart == null || !endOrStart.isBefore(nowTime)
                    }
                    else -> false
                }
            }

        internal fun formatComplicationSummary(
            occurrence: EventOccurrence?,
            today: LocalDate,
            nowTime: LocalTime,
        ): ComplicationSummary {
            if (occurrence == null) {
                return ComplicationSummary(
                    shortTimeLabel = "Free",
                    shortTitleLabel = "Clear",
                    contentDescription = "No upcoming Jlight events",
                    minutesUntil = 180L,
                )
            }

            val timeLabel = occurrence.startTime
                ?.format(SHORT_TIME_FORMAT)
                ?.replace("AM", "A")
                ?.replace("PM", "P")
                ?: if (occurrence.occurrenceDate == today) "Today" else "Tmrw"

            val titleShort = occurrence.title.trim().take(7).ifEmpty { "Event" }
            val minutesUntil = if (occurrence.occurrenceDate == today && occurrence.startTime != null) {
                Duration.between(nowTime, occurrence.startTime).toMinutes().coerceAtLeast(0L)
            } else {
                120L
            }

            return ComplicationSummary(
                shortTimeLabel = timeLabel,
                shortTitleLabel = titleShort,
                contentDescription = "Next Jlight event: $timeLabel ${occurrence.title}",
                minutesUntil = minutesUntil,
            )
        }
    }
}
