package dev.tomasmed.jlight.wear.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.tomasmed.jlight.wear.data.sync.WearSyncCoordinator
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import dev.tomasmed.jlight.wear.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Glanceable ProtoLayout Tile displaying the next 3 upcoming JlightCalendar household events
 * from the offline-first Room repository and deep-linking into [MainActivity] / `EventDetailScreen`.
 */
class AgendaTileService : TileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        serviceScope.future {
            val today = LocalDate.now()
            val repository = WearSyncCoordinator.provideRepository(applicationContext)
            val occurrences = repository.observeOccurrences(
                windowStart = today,
                windowEnd = today.plusDays(2),
            ).first().take(3)

            val rootLayout = buildAgendaTileLayout(
                packageName = packageName,
                occurrences = occurrences,
            )

            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MS)
                .setTileTimeline(
                    TimelineBuilders.Timeline.fromLayoutElement(rootLayout),
                )
                .build()
        }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build(),
        )

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val RESOURCES_VERSION = "2"
        private const val FRESHNESS_INTERVAL_MS = 15L * 60L * 1000L // 15 minutes
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a")

        internal fun buildAgendaTileLayout(
            packageName: String,
            occurrences: List<EventOccurrence>,
        ): LayoutElementBuilders.LayoutElement {
            val columnBuilder = LayoutElementBuilders.Column.Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

            val header = LayoutElementBuilders.Text.Builder()
                .setText("⌚ Jlight Agenda")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(13f))
                        .setColor(argb(0xFF38BDF8.toInt()))
                        .build(),
                )
                .build()
            columnBuilder.addContent(header)
            columnBuilder.addContent(
                LayoutElementBuilders.Spacer.Builder()
                    .setHeight(dp(6f))
                    .build(),
            )

            if (occurrences.isEmpty()) {
                columnBuilder.addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText("No upcoming events")
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(sp(12f))
                                .setColor(argb(0xFF94A3B8.toInt()))
                                .build(),
                        )
                        .build(),
                )
            } else {
                occurrences.forEach { occ ->
                    val timeLabel = occ.startTime?.format(TIME_FORMAT) ?: "All Day"
                    val launchAction = ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(
                            ActionBuilders.AndroidActivity.Builder()
                                .setPackageName(packageName)
                                .setClassName(MainActivity::class.java.name)
                                .addKeyToExtraMapping(
                                    MainActivity.EXTRA_OCCURRENCE_ID,
                                    ActionBuilders.stringExtra(occ.occurrenceId),
                                )
                                .build(),
                        )
                        .build()

                    val rowText = LayoutElementBuilders.Text.Builder()
                        .setText("$timeLabel • ${occ.title}")
                        .setMaxLines(1)
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(sp(12f))
                                .setColor(argb(0xFFF8FAFC.toInt()))
                                .build(),
                        )
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setClickable(
                                    ModifiersBuilders.Clickable.Builder()
                                        .setId(occ.occurrenceId)
                                        .setOnClick(launchAction)
                                        .build(),
                                )
                                .setPadding(
                                    ModifiersBuilders.Padding.Builder()
                                        .setTop(dp(3f))
                                        .setBottom(dp(3f))
                                        .build(),
                                )
                                .build(),
                        )
                        .build()

                    columnBuilder.addContent(rowText)
                }
            }

            return LayoutElementBuilders.Box.Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .addContent(columnBuilder.build())
                .build()
        }
    }
}
