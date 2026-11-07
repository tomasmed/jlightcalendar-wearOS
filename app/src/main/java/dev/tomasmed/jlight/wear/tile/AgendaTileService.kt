package dev.tomasmed.jlight.wear.tile

import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Glanceable ProtoLayout Tile displaying upcoming JlightCalendar household events.
 */
class AgendaTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val textElement = LayoutElementBuilders.Text.Builder()
            .setText("Jlight Family Agenda")
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setColor(argb(0xFF38BDF8.toInt()))
                    .build(),
            )
            .build()

        val rootLayout = LayoutElementBuilders.Box.Builder()
            .addContent(textElement)
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(rootLayout),
            )
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build(),
        )

    private companion object {
        const val RESOURCES_VERSION = "1"
    }
}
