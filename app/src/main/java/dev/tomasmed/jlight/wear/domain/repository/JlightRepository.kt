package dev.tomasmed.jlight.wear.domain.repository

import dev.tomasmed.jlight.wear.domain.model.ChoreItem
import dev.tomasmed.jlight.wear.domain.model.EventOccurrence
import dev.tomasmed.jlight.wear.domain.model.PairingSessionState
import dev.tomasmed.jlight.wear.domain.model.SharedListItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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

    suspend fun syncWithRemote(): Result<Unit>

    suspend fun toggleChoreCompleted(
        choreId: String,
        completed: Boolean,
    ): Result<ChoreItem>

    suspend fun toggleListItemCompleted(
        itemId: String,
        completed: Boolean,
    ): Result<SharedListItem>
}
