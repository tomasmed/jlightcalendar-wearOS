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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
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
    val status: String = "pending",
    val deviceCode: String? = null,
    val userCode: String? = null,
    val verificationUri: String? = null,
    val claimUrl: String? = null,
    val expiresIn: Int? = null,
    val interval: Int? = null,
    val expiresAt: String? = null,
    val token: String? = null,
    val galaxyId: String? = null,
    val galaxyName: String? = null,
    val memberId: String? = null,
    val boundMember: String? = null,
) {
    val resolvedClaimUrl: String?
        get() = verificationUri ?: claimUrl
}

@Serializable
data class RemoteEventDto(
    val id: String,
    val galaxyId: String = "default",
    val title: String,
    val date: String? = null,
    val time: String? = null,
    val startTime: String? = null,
    val endDate: String? = null,
    val endTime: String? = null,
    val isAllDay: Boolean = false,
    val memberId: String? = null,
    val memberIds: JsonElement? = null,
    val rrule: String? = null,
    val exdates: JsonElement? = null,
    val location: String? = null,
    val description: String? = null,
) {
    fun toDomain(json: Json = Json): CalendarEvent {
        val parsedMemberIds = parseStringListElement(memberIds, json)
        val resolvedMembers = when {
            parsedMemberIds.isNotEmpty() -> parsedMemberIds
            !memberId.isNullOrBlank() -> listOf(memberId)
            else -> listOf("Everyone")
        }

        val resolvedDate = when {
            !date.isNullOrBlank() -> LocalDate.parse(date.trim().take(10))
            !startTime.isNullOrBlank() -> LocalDate.parse(startTime.trim().take(10))
            else -> error("Event '$id' is missing both 'date' and 'startTime'")
        }

        val resolvedStartTime = if (isAllDay) {
            null
        } else {
            when {
                !time.isNullOrBlank() -> parseWallClockTime(time)
                !startTime.isNullOrBlank() && startTime.contains('T') -> parseWallClockTime(startTime.substringAfter('T'))
                else -> null
            }
        }

        val resolvedEndDate = endDate?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { LocalDate.parse(raw.trim().take(10)) }.getOrNull()
        } ?: endTime?.takeIf { it.isNotBlank() && it.contains('T') }?.let { raw ->
            runCatching { LocalDate.parse(raw.trim().substringBefore('T')) }.getOrNull()
        }

        val resolvedEndTime = if (isAllDay) {
            null
        } else {
            endTime?.takeIf { it.isNotBlank() }?.let { raw ->
                parseWallClockTime(if (raw.contains('T')) raw.substringAfter('T') else raw)
            }
        }

        val parsedExdates = parseStringListElement(exdates, json)
            .mapNotNull { raw -> runCatching { LocalDate.parse(raw.trim().take(10)) }.getOrNull() }
            .toSet()

        return CalendarEvent(
            id = id,
            galaxyId = galaxyId,
            title = title,
            date = resolvedDate,
            time = resolvedStartTime,
            endDate = resolvedEndDate,
            endTime = resolvedEndTime,
            memberIds = resolvedMembers,
            rrule = rrule,
            exdates = parsedExdates,
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
    val points: Int? = null,
    val stars: Int? = null,
    val recurrenceType: String? = null,
    val frequency: String = "daily",
) {
    fun toDomain(fallbackGalaxyId: String = galaxyId): ChoreItem =
        ChoreItem(
            id = id,
            galaxyId = if (galaxyId == "default") fallbackGalaxyId else galaxyId,
            title = title,
            memberId = memberId,
            completed = completed,
            stars = points ?: stars ?: 1,
            frequency = recurrenceType ?: frequency,
        )
}

@Serializable
data class ToggleChoreRequestDto(
    val choreId: String,
    val id: String = choreId,
    val completed: Boolean,
    val date: String,
)

@Serializable
data class ToggleCompletedRequestDto(
    val id: String,
    val completed: Boolean,
)

@Serializable
data class RemoteListItemDto(
    val id: String,
    val galaxyId: String = "default",
    val title: String? = null,
    val text: String? = null,
    val completed: Boolean = false,
    val category: String = "General",
) {
    fun toDomain(fallbackGalaxyId: String = galaxyId): SharedListItem =
        SharedListItem(
            id = id,
            galaxyId = if (galaxyId == "default") fallbackGalaxyId else galaxyId,
            text = (title ?: text).orEmpty(),
            completed = completed,
            category = category,
        )
}

private fun parseWallClockTime(raw: String): LocalTime? =
    runCatching {
        val cleaned = raw.trim().removeSuffix("Z").substringBefore('+').let { value ->
            // Strip trailing timezone offset like -05:00 if present after HH:mm:ss
            if (value.count { it == ':' } >= 2 && value.contains('-')) value.substringBefore('-') else value
        }
        LocalTime.parse(cleaned)
    }.getOrNull()

private fun parseStringListElement(
    element: JsonElement?,
    json: Json,
): List<String> {
    if (element == null) return emptyList()
    return when (element) {
        is JsonArray -> element.mapNotNull { item ->
            (item as? JsonPrimitive)?.content?.trim()?.takeIf { it.isNotEmpty() }
        }
        is JsonPrimitive -> {
            val raw = element.content.trim()
            if (raw.isEmpty() || raw.equals("null", ignoreCase = true)) {
                emptyList()
            } else if (raw.startsWith("[") && raw.endsWith("]")) {
                runCatching { json.decodeFromString<List<String>>(raw) }
                    .getOrDefault(emptyList())
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            } else {
                raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        else -> emptyList()
    }
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

    suspend fun joinWithGalaxyCode(
        baseUrl: String,
        joinCode: String,
        agentName: String = "Galaxy Watch",
    ): AgentConnectResponseDto {
        val cleanBase = baseUrl.trimEnd('/')
        return httpClient.post("$cleanBase/api/agent/connect") {
            contentType(ContentType.Application.Json)
            setBody(
                AgentConnectRequestDto(
                    agentName = agentName,
                    joinCode = joinCode.trim(),
                ),
            )
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
        val root: JsonElement = httpClient.get("${creds.baseUrl}/api/events") {
            header(HttpHeaders.Authorization, "Bearer ${creds.agentPat}")
            parameter("galaxyId", creds.galaxyId)
        }.body()
        val arrayElement = when (root) {
            is JsonObject -> root["events"] ?: JsonArray(emptyList())
            is JsonArray -> root
            else -> JsonArray(emptyList())
        }
        val dtos = jsonSerializer.decodeFromJsonElement<List<RemoteEventDto>>(arrayElement)
        return dtos.map { it.toDomain(jsonSerializer) }
    }

    suspend fun fetchChores(): List<ChoreItem> {
        val creds = requireNotNull(tokenStore.getCredentials()) { "Watch is not paired with a JlightCalendar galaxy." }
        val root: JsonElement = httpClient.get("${creds.baseUrl}/api/chores") {
            header(HttpHeaders.Authorization, "Bearer ${creds.agentPat}")
            parameter("galaxyId", creds.galaxyId)
        }.body()
        val arrayElement = when (root) {
            is JsonObject -> root["chores"] ?: JsonArray(emptyList())
            is JsonArray -> root
            else -> JsonArray(emptyList())
        }
        val dtos = jsonSerializer.decodeFromJsonElement<List<RemoteChoreDto>>(arrayElement)
        return dtos.map { it.toDomain(fallbackGalaxyId = creds.galaxyId) }
    }

    suspend fun toggleChore(
        choreId: String,
        completed: Boolean,
        date: LocalDate = LocalDate.now(),
    ): Boolean {
        val creds = requireNotNull(tokenStore.getCredentials()) { "Watch is not paired with a JlightCalendar galaxy." }
        val root: JsonElement = httpClient.patch("${creds.baseUrl}/api/chores") {
            header(HttpHeaders.Authorization, "Bearer ${creds.agentPat}")
            contentType(ContentType.Application.Json)
            setBody(
                ToggleChoreRequestDto(
                    choreId = choreId,
                    id = choreId,
                    completed = completed,
                    date = date.toString(),
                ),
            )
        }.body()
        if (root is JsonObject) {
            root["completed"]?.jsonPrimitive?.booleanOrNull?.let { return it }
        }
        return completed
    }

    suspend fun fetchSharedLists(): List<SharedListItem> {
        val creds = requireNotNull(tokenStore.getCredentials()) { "Watch is not paired with a JlightCalendar galaxy." }
        val root: JsonElement = httpClient.get("${creds.baseUrl}/api/lists") {
            header(HttpHeaders.Authorization, "Bearer ${creds.agentPat}")
            parameter("galaxyId", creds.galaxyId)
        }.body()
        val arrayElement = when (root) {
            is JsonObject -> root["items"] ?: JsonArray(emptyList())
            is JsonArray -> root
            else -> JsonArray(emptyList())
        }
        val dtos = jsonSerializer.decodeFromJsonElement<List<RemoteListItemDto>>(arrayElement)
        return dtos.map { it.toDomain(fallbackGalaxyId = creds.galaxyId) }
    }

    suspend fun toggleListItem(
        itemId: String,
        completed: Boolean,
    ): Boolean {
        val creds = requireNotNull(tokenStore.getCredentials()) { "Watch is not paired with a JlightCalendar galaxy." }
        httpClient.patch("${creds.baseUrl}/api/lists") {
            header(HttpHeaders.Authorization, "Bearer ${creds.agentPat}")
            contentType(ContentType.Application.Json)
            setBody(ToggleCompletedRequestDto(id = itemId, completed = completed))
        }
        return completed
    }
}
