package dev.tomasmed.jlight.wear.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Master or single calendar event stored in local wall-clock time (upstream ADR 0013).
 */
data class CalendarEvent(
    val id: String,
    val galaxyId: String,
    val title: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val endDate: LocalDate? = null,
    val endTime: LocalTime? = null,
    val memberIds: List<String> = listOf("Everyone"),
    val rrule: String? = null,
    val exdates: Set<LocalDate> = emptySet(),
    val location: String? = null,
    val description: String? = null,
)

/**
 * Concrete expanded occurrence of a [CalendarEvent] on a specific local [occurrenceDate].
 */
data class EventOccurrence(
    val masterEventId: String,
    val occurrenceId: String,
    val title: String,
    val occurrenceDate: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val memberIds: List<String> = listOf("Everyone"),
    val isRecurring: Boolean = false,
    val location: String? = null,
)

/**
 * Household chore item with star reward value.
 */
data class ChoreItem(
    val id: String,
    val galaxyId: String,
    val title: String,
    val memberId: String,
    val completed: Boolean,
    val stars: Int = 1,
    val frequency: String = "daily",
)

/**
 * Shared household checklist item (e.g. Groceries, Todo).
 */
data class SharedListItem(
    val id: String,
    val galaxyId: String,
    val text: String,
    val completed: Boolean,
    val category: String = "General",
)

/**
 * Represents the watch's authentication state with the JlightCalendar host (ADR 0003).
 */
sealed interface PairingSessionState {
    data object Unpaired : PairingSessionState

    data class AwaitingClaim(
        val deviceCode: String,
        val claimUrl: String,
        val expiresAtIso: String,
    ) : PairingSessionState

    data class Paired(
        val galaxyId: String,
        val agentName: String = "Galaxy Watch",
    ) : PairingSessionState
}
