package dev.tomasmed.jlight.wear.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/**
 * Watch Face Complication data source exporting the next household event in `SHORT_TEXT` format.
 */
class NextEventComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return buildShortTextComplication(
            timeLabel = "8:30A",
            titleLabel = "Huddle",
            contentDescription = "Next Jlight event: 8:30 AM Family Morning Huddle",
        )
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        return buildShortTextComplication(
            timeLabel = "Next",
            titleLabel = "Jlight",
            contentDescription = "Jlight Calendar Upcoming Event",
        )
    }

    private fun buildShortTextComplication(
        timeLabel: String,
        titleLabel: String,
        contentDescription: String,
    ): ShortTextComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(titleLabel).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build(),
        )
            .setTitle(PlainComplicationText.Builder(timeLabel).build())
            .build()
}
