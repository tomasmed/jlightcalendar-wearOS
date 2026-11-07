package dev.tomasmed.jlight.wear.data

import dev.tomasmed.jlight.wear.data.auth.InMemoryAuthTokenStore
import dev.tomasmed.jlight.wear.data.auth.StoredAuthCredentials
import dev.tomasmed.jlight.wear.data.local.ChoreEntity
import dev.tomasmed.jlight.wear.data.local.EventEntity
import dev.tomasmed.jlight.wear.data.local.JlightWearDao
import dev.tomasmed.jlight.wear.data.local.ListItemEntity
import dev.tomasmed.jlight.wear.data.remote.JlightApiClient
import dev.tomasmed.jlight.wear.data.repository.OfflineFirstJlightRepository
import dev.tomasmed.jlight.wear.domain.model.PairingSessionState
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class JlightApiClientTest {
    @Test
    fun `StoredAuthCredentials redacts PAT in toString to prevent Logcat leaks`() {
        val secretPat = "jlight_ag_0123456789abcdef0123456789abcdef0123456789abcdef"
        val creds = StoredAuthCredentials(
            baseUrl = "https://jlight.example.com",
            galaxyId = "galaxy-123",
            agentPat = secretPat,
        )

        val serialized = creds.toString()
        assertFalse("PAT must never appear in toString()", serialized.contains(secretPat))
        assertTrue(serialized.contains("<REDACTED>"))
    }

    @Test
    fun `initiateDeviceCodeHandshake parses real upstream JlightCalendar connect response`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/agent/connect", request.url.encodedPath)
            respond(
                content = """
                    {
                      "deviceCode": "dvc_987654321",
                      "userCode": "BOT-7F9A-22C1",
                      "verificationUri": "https://jlight.example.com/?claimBot=BOT-7F9A-22C1",
                      "expiresIn": 900,
                      "interval": 3,
                      "instruction": "Visit verificationUri"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = JlightApiClient(InMemoryAuthTokenStore(), mockEngine)
        val response = client.initiateDeviceCodeHandshake("https://jlight.example.com/")

        assertEquals("pending", response.status)
        assertEquals("dvc_987654321", response.deviceCode)
        assertEquals("BOT-7F9A-22C1", response.userCode)
        assertEquals("https://jlight.example.com/?claimBot=BOT-7F9A-22C1", response.resolvedClaimUrl)
        assertEquals(900, response.expiresIn)
    }

    @Test
    fun `OfflineFirstJlightRepository syncs upstream envelope payloads into DAO and executes PATCH mutations`() = runTest {
        val pat = "jlight_ag_fedcba9876543210fedcba9876543210fedcba9876543210"
        val tokenStore = InMemoryAuthTokenStore(
            StoredAuthCredentials(
                baseUrl = "https://jlight.example.com",
                galaxyId = "home-galaxy",
                agentPat = pat,
            ),
        )
        val fakeDao = FakeJlightWearDao()
        var lastChorePatchBody = ""
        var lastListPatchBody = ""

        val mockEngine = MockEngine { request ->
            assertEquals("Bearer $pat", request.headers[HttpHeaders.Authorization])

            when {
                request.url.encodedPath == "/api/events" && request.method == HttpMethod.Get -> {
                    assertEquals("home-galaxy", request.url.parameters["galaxyId"])
                    respond(
                        content = """
                            {
                              "events": [
                                {
                                  "id": "evt-soccer",
                                  "galaxyId": "home-galaxy",
                                  "title": "Soccer Practice",
                                  "startTime": "2026-11-07T10:00:00",
                                  "endTime": "2026-11-07T11:30:00",
                                  "isAllDay": false,
                                  "memberIds": "[\"Leo\",\"Tomas\"]",
                                  "rrule": "FREQ=WEEKLY;INTERVAL=1",
                                  "exdates": "[\"2026-11-14\"]"
                                }
                              ]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                request.url.encodedPath == "/api/chores" && request.method == HttpMethod.Get -> {
                    respond(
                        content = """
                            {
                              "chores": [
                                {
                                  "id": "chore-1",
                                  "memberId": "Leo",
                                  "memberName": "Leo",
                                  "title": "Feed Dog",
                                  "points": 3,
                                  "completed": false,
                                  "recurrenceType": "daily"
                                }
                              ],
                              "starBalances": { "Leo": 5 }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                request.url.encodedPath == "/api/chores" && request.method == HttpMethod.Patch -> {
                    lastChorePatchBody = request.body.toByteArray().decodeToString()
                    respond(
                        content = """{"success":true,"completed":true,"starBalances":{"Leo":8}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                request.url.encodedPath == "/api/lists" && request.method == HttpMethod.Get -> {
                    respond(
                        content = """
                            {
                              "items": [
                                {
                                  "id": "list-1",
                                  "title": "Oat Milk",
                                  "completed": false,
                                  "category": "General"
                                }
                              ]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                request.url.encodedPath == "/api/lists" && request.method == HttpMethod.Patch -> {
                    lastListPatchBody = request.body.toByteArray().decodeToString()
                    respond(
                        content = """{"success":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                else -> error("Unexpected request: ${request.method.value} ${request.url.encodedPath}")
            }
        }

        val repo = OfflineFirstJlightRepository(
            tokenStore = tokenStore,
            apiClient = JlightApiClient(tokenStore, mockEngine),
            wearDao = fakeDao,
        )

        val syncResult = repo.syncWithRemote()
        assertTrue("Sync failed: ${syncResult.exceptionOrNull()}", syncResult.isSuccess)

        val pairing = repo.observePairingState().first()
        assertEquals(PairingSessionState.Paired(galaxyId = "home-galaxy", agentName = "Galaxy Watch"), pairing)

        val occurrences = repo.observeOccurrences(
            windowStart = LocalDate.of(2026, 11, 7),
            windowEnd = LocalDate.of(2026, 11, 21),
        ).first()

        // 2026-11-14 is excluded via exdates, leaving Nov 7 and Nov 21 with exact local times
        assertEquals(
            listOf(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 21)),
            occurrences.map { it.occurrenceDate },
        )
        assertEquals(LocalTime.of(10, 0), occurrences.first().startTime)
        assertEquals(LocalTime.of(11, 30), occurrences.first().endTime)
        assertEquals(listOf("Leo", "Tomas"), occurrences.first().memberIds)

        val chore = repo.observeChores().first().single()
        assertEquals(3, chore.stars)
        assertFalse(chore.completed)

        val toggledChore = repo.toggleChoreCompleted("chore-1", true).getOrThrow()
        assertTrue(toggledChore.completed)
        assertTrue(lastChorePatchBody.contains("\"choreId\":\"chore-1\""))

        val toggledList = repo.toggleListItemCompleted("list-1", true).getOrThrow()
        assertEquals("Oat Milk", toggledList.text)
        assertTrue(toggledList.completed)
        assertTrue(lastListPatchBody.contains("\"id\":\"list-1\""))

        // Verify Room DAO received upserts
        assertEquals(1, fakeDao.eventsState.value.size)
        assertTrue(fakeDao.choresState.value.single().completed)
        assertTrue(fakeDao.listsState.value.single().completed)
    }

    private class FakeJlightWearDao : JlightWearDao {
        val eventsState = MutableStateFlow<List<EventEntity>>(emptyList())
        val choresState = MutableStateFlow<List<ChoreEntity>>(emptyList())
        val listsState = MutableStateFlow<List<ListItemEntity>>(emptyList())

        override fun observeEvents(galaxyId: String): Flow<List<EventEntity>> = eventsState

        override suspend fun upsertEvents(events: List<EventEntity>) {
            eventsState.value = events
        }

        override fun observeChores(galaxyId: String): Flow<List<ChoreEntity>> = choresState

        override suspend fun upsertChores(chores: List<ChoreEntity>) {
            choresState.value = chores
        }

        override fun observeListItems(galaxyId: String): Flow<List<ListItemEntity>> = listsState

        override suspend fun upsertListItems(items: List<ListItemEntity>) {
            listsState.value = items
        }
    }
}
