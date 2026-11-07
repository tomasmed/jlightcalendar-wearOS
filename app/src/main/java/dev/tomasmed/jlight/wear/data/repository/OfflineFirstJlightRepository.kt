package dev.tomasmed.jlight.wear.data.repository

import dev.tomasmed.jlight.wear.data.auth.AuthTokenStore
import dev.tomasmed.jlight.wear.data.auth.StoredAuthCredentials
import dev.tomasmed.jlight.wear.data.local.ChoreEntity
import dev.tomasmed.jlight.wear.data.local.EventEntity
import dev.tomasmed.jlight.wear.data.local.JlightWearDao
import dev.tomasmed.jlight.wear.data.local.ListItemEntity
import dev.tomasmed.jlight.wear.data.remote.JlightApiClient
import dev.tomasmed.jlight.wear.data.remote.JlightHandshakeException
import dev.tomasmed.jlight.wear.data.remote.JlightUnauthorizedException
import dev.tomasmed.jlight.wear.domain.model.CalendarEvent
import dev.tomasmed.jlight.wear.domain.model.ChoreItem
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import dev.tomasmed.jlight.wear.domain.model.PairingSessionState
import dev.tomasmed.jlight.wear.domain.model.SharedListItem
import dev.tomasmed.jlight.wear.domain.repository.JlightRepository
import dev.tomasmed.jlight.wear.domain.rrule.WallClockRecurrenceExpander
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Offline-first implementation of [JlightRepository] combining local Room SQLite persistence
 * ([JlightWearDao]) and reactive state with [WallClockRecurrenceExpander] and [JlightApiClient].
 */
@OptIn(ExperimentalCoroutinesApi::class)
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
    ): Flow<List<EventOccurrence>> {
        val masterFlow: Flow<List<CalendarEvent>> = if (wearDao != null) {
            pairingState.flatMapLatest { state ->
                when (state) {
                    is PairingSessionState.Paired -> wearDao.observeEvents(state.galaxyId).map { entities ->
                        entities.map(EventEntity::toDomain).ifEmpty {
                            cachedEvents.value.filter { it.galaxyId == state.galaxyId }
                        }
                    }
                    else -> cachedEvents
                }
            }
        } else {
            cachedEvents
        }

        return masterFlow.map { masters ->
            WallClockRecurrenceExpander.expandEventsInWindow(
                events = masters,
                windowStart = windowStart,
                windowEnd = windowEnd,
            )
        }
    }

    override fun observeChores(): Flow<List<ChoreItem>> {
        if (wearDao == null) return cachedChores.asStateFlow()
        return pairingState.flatMapLatest { state ->
            when (state) {
                is PairingSessionState.Paired -> wearDao.observeChores(state.galaxyId).map { entities ->
                    entities.map(ChoreEntity::toDomain).ifEmpty {
                        cachedChores.value.filter { it.galaxyId == state.galaxyId }
                    }
                }
                else -> flowOf(emptyList())
            }
        }
    }

    override fun observeSharedLists(): Flow<List<SharedListItem>> {
        if (wearDao == null) return cachedLists.asStateFlow()
        return pairingState.flatMapLatest { state ->
            when (state) {
                is PairingSessionState.Paired -> wearDao.observeListItems(state.galaxyId).map { entities ->
                    entities.map(ListItemEntity::toDomain).ifEmpty {
                        cachedLists.value.filter { it.galaxyId == state.galaxyId }
                    }
                }
                else -> flowOf(emptyList())
            }
        }
    }

    override suspend fun initiateDevicePairing(
        baseUrl: String,
        agentName: String,
    ): Result<PairingSessionState.AwaitingClaim> =
        runCatching {
            val response = apiClient.initiateDeviceCodeHandshake(baseUrl = baseUrl, agentName = agentName)
            val deviceCode = requireNotNull(response.deviceCode?.takeIf { it.isNotBlank() }) {
                "Server did not return a valid deviceCode."
            }
            val userCode = response.userCode?.takeIf { it.isNotBlank() } ?: "BOT-PAIR"
            val claimUrl = response.resolvedClaimUrl?.takeIf { it.isNotBlank() }
                ?: "${baseUrl.trimEnd('/')}/?claimBot=$userCode"
            val expiresAtIso = response.expiresAt
                ?: Instant.now().plusSeconds((response.expiresIn ?: 900).toLong()).toString()

            val awaitingState = PairingSessionState.AwaitingClaim(
                deviceCode = deviceCode,
                userCode = userCode,
                claimUrl = claimUrl,
                expiresAtIso = expiresAtIso,
            )
            pairingState.value = awaitingState
            awaitingState
        }

    override suspend fun pollDevicePairing(
        baseUrl: String,
        deviceCode: String,
        agentName: String,
    ): Result<PairingSessionState> =
        try {
            val response = apiClient.pollDeviceCodeStatus(baseUrl = baseUrl, deviceCode = deviceCode)
            if (response.status.equals("approved", ignoreCase = true) && !response.token.isNullOrBlank()) {
                val resolvedGalaxyId = response.galaxyId?.takeIf { it.isNotBlank() } ?: "default"
                tokenStore.saveCredentials(
                    StoredAuthCredentials(
                        baseUrl = baseUrl.trimEnd('/'),
                        galaxyId = resolvedGalaxyId,
                        agentPat = response.token,
                        agentName = agentName,
                    ),
                )
                val paired = PairingSessionState.Paired(
                    galaxyId = resolvedGalaxyId,
                    agentName = agentName,
                )
                pairingState.value = paired
                Result.success(paired)
            } else {
                Result.success(pairingState.value)
            }
        } catch (handshakeEx: JlightHandshakeException) {
            if (handshakeEx.statusCode == 403 || handshakeEx.statusCode == 404 || handshakeEx.statusCode == 410) {
                pairingState.value = PairingSessionState.Unpaired
            }
            Result.failure(handshakeEx)
        } catch (t: Throwable) {
            Result.failure(t)
        }

    override suspend fun pairWithJoinCode(
        baseUrl: String,
        joinCode: String,
        agentName: String,
    ): Result<PairingSessionState.Paired> =
        runCatching {
            val response = apiClient.joinWithGalaxyCode(
                baseUrl = baseUrl,
                joinCode = joinCode,
                agentName = agentName,
            )
            val token = requireNotNull(response.token?.takeIf { it.isNotBlank() }) {
                "Join code exchange did not return a valid bearer PAT."
            }
            val galaxyId = requireNotNull(response.galaxyId?.takeIf { it.isNotBlank() }) {
                "Join code exchange did not return a galaxyId."
            }
            tokenStore.saveCredentials(
                StoredAuthCredentials(
                    baseUrl = baseUrl.trimEnd('/'),
                    galaxyId = galaxyId,
                    agentPat = token,
                    agentName = agentName,
                ),
            )
            val paired = PairingSessionState.Paired(
                galaxyId = galaxyId,
                agentName = agentName,
            )
            pairingState.value = paired
            paired
        }

    override suspend fun unpair() {
        val previousGalaxyId = tokenStore.getCredentials()?.galaxyId
        tokenStore.clearCredentials()
        cachedEvents.value = emptyList()
        cachedChores.value = emptyList()
        cachedLists.value = emptyList()
        if (previousGalaxyId != null) {
            wearDao?.clearEventsForGalaxy(previousGalaxyId)
            wearDao?.clearChoresForGalaxy(previousGalaxyId)
            wearDao?.clearListItemsForGalaxy(previousGalaxyId)
        }
        pairingState.value = PairingSessionState.Unpaired
    }

    override suspend fun syncWithRemote(): Result<Unit> =
        runWithAuthGuard {
            pairingState.value = resolvePairingState()
            val galaxyId = tokenStore.getCredentials()?.galaxyId ?: "default"
            val events = apiClient.fetchEvents()
            val chores = apiClient.fetchChores()
            val lists = apiClient.fetchSharedLists()
            cachedEvents.value = events
            cachedChores.value = chores
            cachedLists.value = lists
            wearDao?.clearEventsForGalaxy(galaxyId)
            wearDao?.upsertEvents(events.map(EventEntity::fromDomain))
            wearDao?.clearChoresForGalaxy(galaxyId)
            wearDao?.upsertChores(chores.map(ChoreEntity::fromDomain))
            wearDao?.clearListItemsForGalaxy(galaxyId)
            wearDao?.upsertListItems(lists.map(ListItemEntity::fromDomain))
        }

    override suspend fun createEvent(
        title: String,
        date: LocalDate,
        startTime: LocalTime?,
        endTime: LocalTime?,
        memberIds: List<String>,
        location: String?,
        description: String?,
    ): Result<CalendarEvent> =
        runWithAuthGuard {
            val created = apiClient.createEvent(
                title = title,
                date = date,
                startTime = startTime,
                endTime = endTime,
                memberIds = memberIds,
                location = location,
                description = description,
            )
            cachedEvents.value = cachedEvents.value + created
            wearDao?.upsertEvents(listOf(EventEntity.fromDomain(created)))
            created
        }

    override suspend fun toggleChoreCompleted(
        choreId: String,
        completed: Boolean,
    ): Result<ChoreItem> =
        runWithAuthGuard {
            val galaxyId = tokenStore.getCredentials()?.galaxyId ?: "default"
            val remoteCompleted = apiClient.toggleChore(choreId, completed)
            val existingFromDao = wearDao
                ?.getChoresSnapshot(galaxyId)
                ?.firstOrNull { it.id == choreId }
                ?.toDomain()
            val current = cachedChores.value.firstOrNull { it.id == choreId }
                ?: existingFromDao
                ?: ChoreItem(
                    id = choreId,
                    galaxyId = galaxyId,
                    title = "",
                    memberId = "Everyone",
                    completed = remoteCompleted,
                )
            val updated = current.copy(completed = remoteCompleted)
            cachedChores.value = if (cachedChores.value.any { it.id == choreId }) {
                cachedChores.value.map { existing -> if (existing.id == choreId) updated else existing }
            } else {
                cachedChores.value + updated
            }
            wearDao?.upsertChores(listOf(ChoreEntity.fromDomain(updated)))
            updated
        }

    override suspend fun toggleListItemCompleted(
        itemId: String,
        completed: Boolean,
    ): Result<SharedListItem> =
        runWithAuthGuard {
            val galaxyId = tokenStore.getCredentials()?.galaxyId ?: "default"
            val remoteCompleted = apiClient.toggleListItem(itemId, completed)
            val existingFromDao = wearDao
                ?.getListItemsSnapshot(galaxyId)
                ?.firstOrNull { it.id == itemId }
                ?.toDomain()
            val current = cachedLists.value.firstOrNull { it.id == itemId }
                ?: existingFromDao
                ?: SharedListItem(
                    id = itemId,
                    galaxyId = galaxyId,
                    text = "",
                    completed = remoteCompleted,
                )
            val target = current.copy(completed = remoteCompleted)
            cachedLists.value = if (cachedLists.value.any { it.id == itemId }) {
                cachedLists.value.map { existing -> if (existing.id == itemId) target else existing }
            } else {
                cachedLists.value + target
            }
            wearDao?.upsertListItems(listOf(ListItemEntity.fromDomain(target)))
            target
        }

    private suspend fun <T> runWithAuthGuard(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (unauthorized: JlightUnauthorizedException) {
            unpair()
            Result.failure(unauthorized)
        } catch (t: Throwable) {
            Result.failure(t)
        }

    private fun resolvePairingState(): PairingSessionState {
        val creds = tokenStore.getCredentials() ?: return PairingSessionState.Unpaired
        return PairingSessionState.Paired(
            galaxyId = creds.galaxyId,
            agentName = creds.agentName,
        )
    }
}
