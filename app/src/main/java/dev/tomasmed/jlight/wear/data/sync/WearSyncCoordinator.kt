package dev.tomasmed.jlight.wear.data.sync

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import dev.tomasmed.jlight.wear.complication.NextEventComplicationService
import dev.tomasmed.jlight.wear.data.auth.EncryptedPrefsAuthTokenStore
import dev.tomasmed.jlight.wear.data.local.JlightWearDatabase
import dev.tomasmed.jlight.wear.data.remote.JlightApiClient
import dev.tomasmed.jlight.wear.data.repository.OfflineFirstJlightRepository
import dev.tomasmed.jlight.wear.domain.model.CalendarEvent
import dev.tomasmed.jlight.wear.domain.model.ChoreItem
import dev.tomasmed.jlight.wear.domain.repository.JlightRepository
import dev.tomasmed.jlight.wear.tile.AgendaTileService
import java.time.LocalDate
import java.time.LocalTime

/**
 * Provides the singleton [JlightRepository] and broadcasts refresh requests to
 * [AgendaTileService] and [NextEventComplicationService] whenever local Room data changes.
 */
object WearSyncCoordinator {
    @Volatile
    private var repositoryInstance: JlightRepository? = null

    fun provideRepository(context: Context): JlightRepository =
        repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: buildDefaultRepository(context.applicationContext)
                .also { repositoryInstance = it }
        }

    fun requestSurfaceUpdates(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            TileService.getUpdater(appContext)
                .requestUpdate(AgendaTileService::class.java)
        }
        runCatching {
            ComplicationDataSourceUpdateRequester.create(
                appContext,
                ComponentName(appContext, NextEventComplicationService::class.java),
            ).requestUpdateAll()
        }
    }

    private fun buildDefaultRepository(context: Context): JlightRepository {
        val tokenStore = EncryptedPrefsAuthTokenStore(context)
        val database = JlightWearDatabase.getInstance(context)
        val apiClient = JlightApiClient(tokenStore)
        val today = LocalDate.now()
        return OfflineFirstJlightRepository(
            tokenStore = tokenStore,
            apiClient = apiClient,
            wearDao = database.wearDao(),
            initialEvents = defaultSampleEvents(today),
            initialChores = defaultSampleChores(),
        )
    }

    fun defaultSampleEvents(today: LocalDate = LocalDate.now()): List<CalendarEvent> =
        listOf(
            CalendarEvent(
                id = "evt-morning-sync",
                galaxyId = "home-galaxy",
                title = "Family Morning Huddle",
                date = today,
                time = LocalTime.of(8, 30),
                endTime = LocalTime.of(9, 0),
                memberIds = listOf("Everyone"),
                rrule = "FREQ=DAILY",
                location = "Kitchen Kiosk",
                description = "Quick daily schedule check & chore star review.",
            ),
            CalendarEvent(
                id = "evt-dinner",
                galaxyId = "home-galaxy",
                title = "Taco Night & Chores Check",
                date = today,
                time = LocalTime.of(18, 0),
                endTime = LocalTime.of(19, 0),
                memberIds = listOf("Tomas", "Everyone"),
                location = "Dining Room",
                description = "Family dinner and evening checklist.",
            ),
            CalendarEvent(
                id = "evt-tomorrow-soccer",
                galaxyId = "home-galaxy",
                title = "Youth Soccer Practice",
                date = today.plusDays(1),
                time = LocalTime.of(16, 30),
                endTime = LocalTime.of(17, 45),
                memberIds = listOf("Leo", "Maya"),
                location = "North Park Field 3",
                description = "Bring water bottle and shin guards.",
            ),
        )

    private fun defaultSampleChores(): List<ChoreItem> =
        listOf(
            ChoreItem(
                id = "chore-sample-1",
                galaxyId = "home-galaxy",
                title = "Feed the Dog",
                memberId = "Leo",
                completed = false,
                stars = 2,
                frequency = "daily",
            ),
            ChoreItem(
                id = "chore-sample-2",
                galaxyId = "home-galaxy",
                title = "Clear Dinner Table",
                memberId = "Everyone",
                completed = false,
                stars = 3,
                frequency = "daily",
            ),
        )
}
