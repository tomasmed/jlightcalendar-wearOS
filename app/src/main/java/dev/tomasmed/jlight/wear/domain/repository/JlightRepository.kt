package dev.tomasmed.jlight.wear.domain.repository

import dev.tomasmed.jlight.wear.domain.model.CalendarEvent
import dev.tomasmed.jlight.wear.domain.model.ChoreItem
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import dev.tomasmed.jlight.wear.domain.model.PairingSessionState
import dev.tomasmed.jlight.wear.domain.model.SharedListItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * Single Source of Truth contract for Wear OS UI screens, ProtoLayout Tiles, and Complications.
 */
interface JlightRepository {
    fun observePairingState(): Flow<PairingSessionState>

    fun observeOccurrences(
        windowStart: LocalDate,
        windowEnd: LocalDate,
    ): Flow<List<EventOccurrence>>

    fun observeChores(): Flow<List<ChoreItem>>

    fun observeSharedLists(): Flow<List<SharedListItem>>

    suspend fun initiateDevicePairing(
        baseUrl: String,
        agentName: String = "Galaxy Watch",
    ): Result<PairingSessionState.AwaitingClaim>

    suspend fun pollDevicePairing(
        baseUrl: String,
        deviceCode: String,
        agentName: String = "Galaxy Watch",
    ): Result<PairingSessionState>

    suspend fun pairWithJoinCode(
        baseUrl: String,
        joinCode: String,
        agentName: String = "Galaxy Watch",
    ): Result<PairingSessionState.Paired>

    suspend fun unpair()

    suspend fun syncWithRemote(): Result<Unit>

    suspend fun createEvent(
        title: String,
        date: LocalDate,
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
        memberIds: List<String> = listOf("Everyone"),
        location: String? = null,
        description: String? = null,
    ): Result<CalendarEvent>

    suspend fun toggleChoreCompleted(
        choreId: String,
        completed: Boolean,
    ): Result<ChoreItem>

    suspend fun toggleListItemCompleted(
        itemId: String,
        completed: Boolean,
    ): Result<SharedListItem>
}
