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

    @Test
    fun `device code polling and join code exchange transition repository to Paired and store PAT`() = runTest {
        val tokenStore = InMemoryAuthTokenStore()
        val fakeDao = FakeJlightWearDao()
        var pollCalls = 0

        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath == "/api/agent/connect" && request.method == HttpMethod.Post -> {
                    val bodyText = request.body.toByteArray().decodeToString()
                    if (bodyText.contains("GLX-FAST1")) {
                        respond(
                            content = """
                                {
                                  "status": "approved",
                                  "token": "jlight_ag_fastjoin0123456789abcdef0123456789abcdef",
                                  "galaxyId": "galaxy-fast",
                                  "galaxyName": "Fast Household"
                                }
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        respond(
                            content = """
                                {
                                  "deviceCode": "dvc_poll_1",
                                  "userCode": "BOT-1234-ABCD",
                                  "verificationUri": "https://jlight.example.com/?claimBot=BOT-1234-ABCD",
                                  "expiresIn": 900,
                                  "interval": 3
                                }
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
                request.url.encodedPath == "/api/agent/connect" && request.method == HttpMethod.Get -> {
                    pollCalls++
                    if (pollCalls == 1) {
                        respond(
                            content = """{"status":"pending","userCode":"BOT-1234-ABCD"}""",
                            status = HttpStatusCode.Accepted,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        respond(
                            content = """
                                {
                                  "status": "approved",
                                  "token": "jlight_ag_polledtoken0123456789abcdef0123456789",
                                  "galaxyId": "galaxy-polled"
                                }
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
                else -> error("Unexpected request: ${request.url}")
            }
        }

        val repo = OfflineFirstJlightRepository(
            tokenStore = tokenStore,
            apiClient = JlightApiClient(tokenStore, mockEngine),
            wearDao = fakeDao,
        )

        val awaiting = repo.initiateDevicePairing("https://jlight.example.com").getOrThrow()
        assertEquals("BOT-1234-ABCD", awaiting.userCode)
        assertEquals("dvc_poll_1", awaiting.deviceCode)

        // First poll -> still pending
        val firstPoll = repo.pollDevicePairing("https://jlight.example.com", "dvc_poll_1").getOrThrow()
        assertTrue(firstPoll is PairingSessionState.AwaitingClaim)

        // Second poll -> approved and credentials persisted
        val secondPoll = repo.pollDevicePairing("https://jlight.example.com", "dvc_poll_1").getOrThrow()
        assertEquals(PairingSessionState.Paired(galaxyId = "galaxy-polled", agentName = "Galaxy Watch"), secondPoll)
        assertEquals("galaxy-polled", tokenStore.getCredentials()?.galaxyId)

        // Unpair and test fast join code exchange
        repo.unpair()
        assertEquals(PairingSessionState.Unpaired, repo.observePairingState().first())

        val fastPaired = repo.pairWithJoinCode("https://jlight.example.com", "GLX-FAST1").getOrThrow()
        assertEquals("galaxy-fast", fastPaired.galaxyId)
        assertEquals("jlight_ag_fastjoin0123456789abcdef0123456789abcdef", tokenStore.getCredentials()?.agentPat)
    }

    @Test
    fun `HTTP 401 or 403 from upstream revokes stored PAT and transitions watch to Unpaired`() = runTest {
        val tokenStore = InMemoryAuthTokenStore(
            StoredAuthCredentials(
                baseUrl = "https://jlight.example.com",
                galaxyId = "home-galaxy",
                agentPat = "jlight_ag_revoked_token",
            ),
        )
        val fakeDao = FakeJlightWearDao()

        val mockEngine = MockEngine {
            respond(
                content = """{"error":"Agent key revoked"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val repo = OfflineFirstJlightRepository(
            tokenStore = tokenStore,
            apiClient = JlightApiClient(tokenStore, mockEngine),
            wearDao = fakeDao,
        )

        val syncResult = repo.syncWithRemote()
        assertTrue(syncResult.isFailure)
        assertEquals(null, tokenStore.getCredentials())
        assertEquals(PairingSessionState.Unpaired, repo.observePairingState().first())
    }

    @Test
    fun `createEvent posts wall-clock startTime and endTime and upserts into local Room DAO`() = runTest {
        val tokenStore = InMemoryAuthTokenStore(
            StoredAuthCredentials(
                baseUrl = "https://jlight.example.com",
                galaxyId = "home-galaxy",
                agentPat = "jlight_ag_valid_pat",
            ),
        )
        val fakeDao = FakeJlightWearDao()
        var postedBody = ""

        val mockEngine = MockEngine { request ->
            assertEquals("/api/events", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            postedBody = request.body.toByteArray().decodeToString()
            respond(
                content = """
                    {
                      "success": true,
                      "event": {
                        "id": "evt-created-99",
                        "galaxyId": "home-galaxy",
                        "title": "Pediatric Dentist",
                        "startTime": "2026-11-08T15:30:00",
                        "endTime": "2026-11-08T16:30:00",
                        "isAllDay": false,
                        "memberIds": "[\"Everyone\"]"
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val repo = OfflineFirstJlightRepository(
            tokenStore = tokenStore,
            apiClient = JlightApiClient(tokenStore, mockEngine),
            wearDao = fakeDao,
        )

        val created = repo.createEvent(
            title = "Pediatric Dentist",
            date = LocalDate.of(2026, 11, 8),
            startTime = LocalTime.of(15, 30),
        ).getOrThrow()

        assertEquals("evt-created-99", created.id)
        assertEquals(LocalDate.of(2026, 11, 8), created.date)
        assertEquals(LocalTime.of(15, 30), created.time)
        assertTrue(postedBody.contains("2026-11-08T15:30"))
        assertEquals(1, fakeDao.eventsState.value.size)
    }

    @Test
    fun `HTTP 500 server error during sync preserves existing offline Room cache and does not unpair`() = runTest {
        val tokenStore = InMemoryAuthTokenStore(
            StoredAuthCredentials(
                baseUrl = "https://jlight.example.com",
                galaxyId = "galaxy-real",
                agentPat = "jlight_ag_valid_pat",
            ),
        )
        val fakeDao = FakeJlightWearDao()
        fakeDao.upsertEvents(
            listOf(
                EventEntity(
                    id = "evt-cached-1",
                    galaxyId = "galaxy-real",
                    title = "Cached Offline Dentist",
                    dateIso = "2026-11-07",
                    timeIso = "09:00",
                    endDateIso = null,
                    endTimeIso = "10:00",
                    memberIdsCsv = "Leo",
                    rrule = null,
                    exdatesCsv = "",
                    location = null,
                    description = null,
                ),
            ),
        )

        val mockEngine = MockEngine {
            respond(
                content = """{"error":"Failed to fetch events"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val repo = OfflineFirstJlightRepository(
            tokenStore = tokenStore,
            apiClient = JlightApiClient(tokenStore, mockEngine),
            wearDao = fakeDao,
        )

        val syncResult = repo.syncWithRemote()
        assertTrue(syncResult.isFailure)
        // Credentials must remain intact (500 is transient, not 401/403)
        assertEquals("galaxy-real", tokenStore.getCredentials()?.galaxyId)
        // Offline Room cache must NOT be wiped!
        assertEquals(1, fakeDao.eventsState.value.size)
        assertEquals("Cached Offline Dentist", fakeDao.eventsState.value.single().title)
    }

    @Test
    fun `expired or denied device handshake resets state to Unpaired and toggling Room-backed chores preserves title`() = runTest {
        val tokenStore = InMemoryAuthTokenStore()
        val fakeDao = FakeJlightWearDao()

        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath == "/api/agent/connect" && request.method == HttpMethod.Post -> {
                    val bodyText = request.body.toByteArray().decodeToString()
                    if (bodyText.contains("GLX-8P9A-2M4K")) {
                        respond(
                            content = """
                                {
                                  "status": "approved",
                                  "token": "jlight_ag_spokenjoin0123456789abcdef0123456789",
                                  "galaxyId": "galaxy-spoken"
                                }
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        respond(
                            content = """
                                {
                                  "deviceCode": "dvc_expiring",
                                  "userCode": "BOT-EXP1-0000",
                                  "verificationUri": "https://jlight.example.com/?claimBot=BOT-EXP1-0000",
                                  "expiresIn": 900
                                }
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
                request.url.encodedPath == "/api/agent/connect" && request.method == HttpMethod.Get -> {
                    respond(
                        content = """{"error":"Handshake session has expired."}""",
                        status = HttpStatusCode.Gone,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                request.url.encodedPath == "/api/chores" && request.method == HttpMethod.Patch -> {
                    respond(
                        content = """{"success":true,"completed":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                else -> error("Unexpected request: ${request.url}")
            }
        }

        val repo = OfflineFirstJlightRepository(
            tokenStore = tokenStore,
            apiClient = JlightApiClient(tokenStore, mockEngine),
            wearDao = fakeDao,
            initialEvents = listOf(
                dev.tomasmed.jlight.wear.domain.model.CalendarEvent(
                    id = "demo-evt",
                    galaxyId = "home-galaxy",
                    title = "Demo Sample Event",
                    date = LocalDate.of(2026, 11, 7),
                ),
            ),
        )
        val viewModel = dev.tomasmed.jlight.wear.ui.screens.PairingViewModel(
            repository = repo,
            baseUrl = "https://jlight.example.com",
        )

        viewModel.startDeviceCodeHandshake().getOrThrow()
        assertTrue(repo.observePairingState().first() is PairingSessionState.AwaitingClaim)

        // Polling an expired (410 Gone) code must transition state back to Unpaired so 3s polling stops
        val expiredPoll = viewModel.pollDeviceCodeStatus("dvc_expiring")
        assertTrue(expiredPoll.isFailure)
        assertEquals(PairingSessionState.Unpaired, repo.observePairingState().first())
        assertTrue(viewModel.statusMessage.value.orEmpty().contains("410"))

        // Pair via spoken transcript with "dash" words
        val pairResult = viewModel.pairWithSpokenJoinCode("glx dash 8p9a dash 2m4k")
        assertTrue(pairResult.isSuccess)
        assertEquals(
            PairingSessionState.Paired(galaxyId = "galaxy-spoken", agentName = "Galaxy Watch"),
            repo.observePairingState().first(),
        )

        // Paired galaxy with 0 events in Room must NOT leak "home-galaxy" demo sample events
        val pairedOccurrences = repo.observeOccurrences(
            windowStart = LocalDate.of(2026, 11, 7),
            windowEnd = LocalDate.of(2026, 11, 8),
        ).first()
        assertTrue("Paired empty galaxy must not leak demo events", pairedOccurrences.isEmpty())

        // Pre-populate Room DAO directly (simulating process restart before syncWithRemote)
        fakeDao.upsertChores(
            listOf(
                ChoreEntity(
                    id = "chore-room-only",
                    galaxyId = "galaxy-spoken",
                    title = "Walk Java the Dog",
                    memberId = "Maya",
                    completed = false,
                    stars = 4,
                    frequency = "daily",
                ),
            ),
        )
        val toggled = repo.toggleChoreCompleted("chore-room-only", true).getOrThrow()
        assertEquals("Walk Java the Dog", toggled.title)
        assertEquals("Maya", toggled.memberId)
        assertEquals(4, toggled.stars)
        assertTrue(toggled.completed)
    }

    private class FakeJlightWearDao : JlightWearDao {
        val eventsState = MutableStateFlow<List<EventEntity>>(emptyList())
        val choresState = MutableStateFlow<List<ChoreEntity>>(emptyList())
        val listsState = MutableStateFlow<List<ListItemEntity>>(emptyList())

        override fun observeEvents(galaxyId: String): Flow<List<EventEntity>> = eventsState

        override suspend fun getEventsSnapshot(galaxyId: String): List<EventEntity> = eventsState.value

        override suspend fun upsertEvents(events: List<EventEntity>) {
            eventsState.value = events
        }

        override suspend fun clearEventsForGalaxy(galaxyId: String) {
            eventsState.value = emptyList()
        }

        override fun observeChores(galaxyId: String): Flow<List<ChoreEntity>> = choresState

        override suspend fun getChoresSnapshot(galaxyId: String): List<ChoreEntity> = choresState.value

        override suspend fun upsertChores(chores: List<ChoreEntity>) {
            choresState.value = chores
        }

        override suspend fun clearChoresForGalaxy(galaxyId: String) {
            choresState.value = emptyList()
        }

        override fun observeListItems(galaxyId: String): Flow<List<ListItemEntity>> = listsState

        override suspend fun getListItemsSnapshot(galaxyId: String): List<ListItemEntity> = listsState.value

        override suspend fun upsertListItems(items: List<ListItemEntity>) {
            listsState.value = items
        }

        override suspend fun clearListItemsForGalaxy(galaxyId: String) {
            listsState.value = emptyList()
        }
    }
}
