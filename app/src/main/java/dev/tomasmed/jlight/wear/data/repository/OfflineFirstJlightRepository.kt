package dev.tomasmed.jlight.wear.data.repository

import dev.tomasmed.jlight.wear.data.auth.AuthTokenStore
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
 * Offline-first implementation of [JlightRepository] combining local cached state with
 * [WallClockRecurrenceExpander] and [JlightApiClient].
 */
class OfflineFirstJlightRepository(
    private val tokenStore: AuthTokenStore,
    private val apiClient: JlightApiClient,
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
        }

    override suspend fun toggleChoreCompleted(
        choreId: String,
        completed: Boolean,
    ): Result<ChoreItem> =
        runCatching {
            val updated = apiClient.toggleChore(choreId, completed)
            cachedChores.value = cachedChores.value.map { existing ->
                if (existing.id == choreId) updated else existing
            }
            updated
        }

    override suspend fun toggleListItemCompleted(
        itemId: String,
        completed: Boolean,
    ): Result<SharedListItem> =
        runCatching {
            val target = cachedLists.value.first { it.id == itemId }.copy(completed = completed)
            cachedLists.value = cachedLists.value.map { existing ->
                if (existing.id == itemId) target else existing
            }
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
