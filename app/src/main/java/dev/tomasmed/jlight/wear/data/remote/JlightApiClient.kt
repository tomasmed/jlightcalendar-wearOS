package dev.tomasmed.jlight.wear.data.remote

import dev.tomasmed.jlight.wear.data.auth.AuthTokenStore
import dev.tomasmed.jlight.wear.domain.model.CalendarEvent
import dev.tomasmed.jlight.wear.domain.model.ChoreItem
import dev.tomasmed.jlight.wear.domain.model.SharedListItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class AgentConnectRequestDto(
    val agentName: String = "Galaxy Watch",
    val rune: String = "⌚",
    val requestedScope: String = "standard",
    val joinCode: String? = null,
)

@Serializable
data class AgentConnectResponseDto(
    val status: String,
    val deviceCode: String? = null,
    val claimUrl: String? = null,
    val expiresAt: String? = null,
    val token: String? = null,
    val galaxyId: String? = null,
)

@Serializable
data class RemoteEventDto(
    val id: String,
    val galaxyId: String = "default",
    val title: String,
    val date: String,
    val time: String? = null,
    val endDate: String? = null,
    val endTime: String? = null,
    val memberId: String? = null,
    val memberIds: List<String>? = null,
    val rrule: String? = null,
    val exdates: List<String>? = null,
    val location: String? = null,
    val description: String? = null,
) {
    fun toDomain(): CalendarEvent {
        val resolvedMembers = when {
            !memberIds.isNullOrEmpty() -> memberIds
            !memberId.isNullOrBlank() -> listOf(memberId)
            else -> listOf("Everyone")
        }
        return CalendarEvent(
            id = id,
            galaxyId = galaxyId,
            title = title,
            date = LocalDate.parse(date),
            time = time?.takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) },
            endDate = endDate?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
            endTime = endTime?.takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) },
            memberIds = resolvedMembers,
            rrule = rrule,
            exdates = exdates.orEmpty().mapNotNull { raw ->
                runCatching { LocalDate.parse(raw.trim()) }.getOrNull()
            }.toSet(),
            location = location,
            description = description,
        )
    }
}

@Serializable
data class RemoteChoreDto(
    val id: String,
    val galaxyId: String = "default",
    val title: String,
    val memberId: String = "Everyone",
    val completed: Boolean = false,
    val stars: Int = 1,
    val frequency: String = "daily",
) {
    fun toDomain(): ChoreItem =
        ChoreItem(
            id = id,
            galaxyId = galaxyId,
            title = title,
            memberId = memberId,
            completed = completed,
            stars = stars,
            frequency = frequency,
        )
}

@Serializable
data class ToggleCompletedRequestDto(
    val id: String,
    val completed: Boolean,
)

@Serializable
data class RemoteListItemDto(
    val id: String,
    val galaxyId: String = "default",
    val text: String,
    val completed: Boolean = false,
    val category: String = "General",
) {
    fun toDomain(): SharedListItem =
        SharedListItem(
            id = id,
            galaxyId = galaxyId,
            text = text,
            completed = completed,
            category = category,
        )
}

/**
 * Ktor REST client for upstream JlightCalendar endpoints (`/api/agent/connect`, `/api/events`, `/api/chores`, `/api/lists`).
 */
class JlightApiClient(
    private val tokenStore: AuthTokenStore,
    engine: HttpClientEngine = CIO.create(),
) {
    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(jsonSerializer)
        }
    }

    suspend fun initiateDeviceCodeHandshake(
        baseUrl: String,
        agentName: String = "Galaxy Watch",
    ): AgentConnectResponseDto {
        val cleanBase = baseUrl.trimEnd('/')
        return httpClient.post("$cleanBase/api/agent/connect") {
            contentType(ContentType.Application.Json)
            setBody(AgentConnectRequestDto(agentName = agentName))
        }.body()
    }

    suspend fun pollDeviceCodeStatus(
        baseUrl: String,
        deviceCode: String,
    ): AgentConnectResponseDto {
        val cleanBase = baseUrl.trimEnd('/')
        return httpClient.get("$cleanBase/api/agent/connect") {
            parameter("deviceCode", deviceCode)
        }.body()
    }

    suspend fun fetchEvents(): List<CalendarEvent> {
        val creds = requireNotNull(tokenStore.getCredentials()) { "Watch is not paired with a JlightCalendar galaxy." }
        val dtos: List<RemoteEventDto> = httpClient.get("${creds.baseUrl}/api/events") {
            header(HttpHeaders.Authorization, "Bearer ${creds.agentPat}")
            parameter("galaxyId", creds.galaxyId)
        }.body()
        return dtos.map { it.toDomain() }
    }

    suspend fun fetchChores(): List<ChoreItem> {
        val creds = requireNotNull(tokenStore.getCredentials()) { "Watch is not paired with a JlightCalendar galaxy." }
        val dtos: List<RemoteChoreDto> = httpClient.get("${creds.baseUrl}/api/chores") {
            header(HttpHeaders.Authorization, "Bearer ${creds.agentPat}")
            parameter("galaxyId", creds.galaxyId)
        }.body()
        return dtos.map { it.toDomain() }
    }

    suspend fun toggleChore(
        choreId: String,
        completed: Boolean,
    ): ChoreItem {
        val creds = requireNotNull(tokenStore.getCredentials()) { "Watch is not paired with a JlightCalendar galaxy." }
        val dto: RemoteChoreDto = httpClient.patch("${creds.baseUrl}/api/chores") {
            header(HttpHeaders.Authorization, "Bearer ${creds.agentPat}")
            contentType(ContentType.Application.Json)
            setBody(ToggleCompletedRequestDto(id = choreId, completed = completed))
        }.body()
        return dto.toDomain()
    }

    suspend fun fetchSharedLists(): List<SharedListItem> {
        val creds = requireNotNull(tokenStore.getCredentials()) { "Watch is not paired with a JlightCalendar galaxy." }
        val dtos: List<RemoteListItemDto> = httpClient.get("${creds.baseUrl}/api/lists") {
            header(HttpHeaders.Authorization, "Bearer ${creds.agentPat}")
            parameter("galaxyId", creds.galaxyId)
        }.body()
        return dtos.map { it.toDomain() }
    }
}
