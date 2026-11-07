package dev.tomasmed.jlight.wear.data.repository

import dev.tomasmed.jlight.wear.data.auth.AuthTokenStore
import dev.tomasmed.jlight.wear.data.local.ChoreEntity
import dev.tomasmed.jlight.wear.data.local.EventEntity
import dev.tomasmed.jlight.wear.data.local.JlightWearDao
import dev.tomasmed.jlight.wear.data.local.ListItemEntity
import dev.tomasmed.jlight.wear.data.remote.JlightApiClient
import dev.tomasmed.jlight.wear.domain.model.CalendarEvent
import dev.tomasmed.jlight.wear.domain.model.ChoreItem
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import dev.tomasmed.jlight.wear.domain.model.PairingSessionState
import dev.tomasmed.jlight.wear.domain.model.SharedListItem
import dev.tomasmed.jlight.wear.domain.repository.JlightRepository
import dev.tomasmed.jlight.wear.domain.rrule.WallClockRecurrenceExpander
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Offline-first implementation of [JlightRepository] combining local Room SQLite persistence
 * ([JlightWearDao]) and reactive state with [WallClockRecurrenceExpander] and [JlightApiClient].
 */
class OfflineFirstJlightRepository(
    private val tokenStore: AuthTokenStore,
    private val apiClient: JlightApiClient,
    private val wearDao: JlightWearDao? = null,
    initialEvents: List<CalendarEvent> = emptyList(),
    initialChores: List<ChoreItem> = emptyList(),
    initialLists: List<SharedListItem> = emptyList(),
) : JlightRepository {
    private val pairingState = MutableStateFlow(resolvePairingState())
    private val cachedEvents = MutableStateFlow(initialEvents)
    private val cachedChores = MutableStateFlow(initialChores)
    private val cachedLists = MutableStateFlow(initialLists)

    override fun observePairingState(): Flow<PairingSessionState> = pairingState.asStateFlow()

    override fun observeOccurrences(
        windowStart: LocalDate,
        windowEnd: LocalDate,
    ): Flow<List<EventOccurrence>> =
        cachedEvents.map { masters ->
            WallClockRecurrenceExpander.expandEventsInWindow(
                events = masters,
                windowStart = windowStart,
                windowEnd = windowEnd,
            )
        }

    override fun observeChores(): Flow<List<ChoreItem>> = cachedChores.asStateFlow()

    override fun observeSharedLists(): Flow<List<SharedListItem>> = cachedLists.asStateFlow()

    override suspend fun syncWithRemote(): Result<Unit> =
        runCatching {
            pairingState.value = resolvePairingState()
            val events = apiClient.fetchEvents()
            val chores = apiClient.fetchChores()
            val lists = apiClient.fetchSharedLists()
            cachedEvents.value = events
            cachedChores.value = chores
            cachedLists.value = lists
            wearDao?.upsertEvents(events.map(EventEntity::fromDomain))
            wearDao?.upsertChores(chores.map(ChoreEntity::fromDomain))
            wearDao?.upsertListItems(lists.map(ListItemEntity::fromDomain))
        }

    override suspend fun toggleChoreCompleted(
        choreId: String,
        completed: Boolean,
    ): Result<ChoreItem> =
        runCatching {
            val remoteCompleted = apiClient.toggleChore(choreId, completed)
            val current = cachedChores.value.firstOrNull { it.id == choreId }
                ?: ChoreItem(
                    id = choreId,
                    galaxyId = tokenStore.getCredentials()?.galaxyId ?: "default",
                    title = "",
                    memberId = "Everyone",
                    completed = remoteCompleted,
                )
            val updated = current.copy(completed = remoteCompleted)
            cachedChores.value = cachedChores.value.map { existing ->
                if (existing.id == choreId) updated else existing
            }
            wearDao?.upsertChores(listOf(ChoreEntity.fromDomain(updated)))
            updated
        }

    override suspend fun toggleListItemCompleted(
        itemId: String,
        completed: Boolean,
    ): Result<SharedListItem> =
        runCatching {
            val remoteCompleted = apiClient.toggleListItem(itemId, completed)
            val target = cachedLists.value.first { it.id == itemId }.copy(completed = remoteCompleted)
            cachedLists.value = cachedLists.value.map { existing ->
                if (existing.id == itemId) target else existing
            }
            wearDao?.upsertListItems(listOf(ListItemEntity.fromDomain(target)))
            target
        }

    private fun resolvePairingState(): PairingSessionState {
        val creds = tokenStore.getCredentials() ?: return PairingSessionState.Unpaired
        return PairingSessionState.Paired(
            galaxyId = creds.galaxyId,
            agentName = creds.agentName,
        )
    }
}
