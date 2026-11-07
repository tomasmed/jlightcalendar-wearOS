package dev.tomasmed.jlight.wear.data

import dev.tomasmed.jlight.wear.data.auth.InMemoryAuthTokenStore
import dev.tomasmed.jlight.wear.data.auth.StoredAuthCredentials
import dev.tomasmed.jlight.wear.data.remote.JlightApiClient
import dev.tomasmed.jlight.wear.data.repository.OfflineFirstJlightRepository
import dev.tomasmed.jlight.wear.domain.model.PairingSessionState
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
    fun `initiateDeviceCodeHandshake posts watch metadata and parses BOT code`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/agent/connect", request.url.encodedPath)
            respond(
                content = """
                    {
                      "status": "pending",
                      "deviceCode": "BOT-7F9A-22C1",
                      "claimUrl": "/?claimBot=BOT-7F9A-22C1",
                      "expiresAt": "2026-11-07T16:15:00Z"
                    }
                """.trimIndent(),
                status = HttpStatusCode.Accepted,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = JlightApiClient(InMemoryAuthTokenStore(), mockEngine)
        val response = client.initiateDeviceCodeHandshake("https://jlight.example.com/")

        assertEquals("pending", response.status)
        assertEquals("BOT-7F9A-22C1", response.deviceCode)
        assertEquals("/?claimBot=BOT-7F9A-22C1", response.claimUrl)
    }

    @Test
    fun `OfflineFirstJlightRepository syncs events with Bearer PAT and expands occurrences`() = runTest {
        val pat = "jlight_ag_fedcba9876543210fedcba9876543210fedcba9876543210"
        val tokenStore = InMemoryAuthTokenStore(
            StoredAuthCredentials(
                baseUrl = "https://jlight.example.com",
                galaxyId = "home-galaxy",
                agentPat = pat,
            ),
        )

        val mockEngine = MockEngine { request ->
            assertEquals("Bearer $pat", request.headers[HttpHeaders.Authorization])
            assertEquals("home-galaxy", request.url.parameters["galaxyId"])

            when (request.url.encodedPath) {
                "/api/events" -> respond(
                    content = """
                        [
                          {
                            "id": "evt-soccer",
                            "galaxyId": "home-galaxy",
                            "title": "Soccer Practice",
                            "date": "2026-11-07",
                            "time": "10:00",
                            "memberIds": ["Leo", "Tomas"],
                            "rrule": "FREQ=WEEKLY;INTERVAL=1",
                            "exdates": ["2026-11-14"]
                          }
                        ]
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                "/api/chores" -> respond(
                    content = """
                        [
                          {
                            "id": "chore-1",
                            "galaxyId": "home-galaxy",
                            "title": "Feed Dog",
                            "memberId": "Leo",
                            "completed": false,
                            "stars": 2,
                            "frequency": "daily"
                          }
                        ]
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                "/api/lists" -> respond(
                    content = """
                        [
                          {
                            "id": "list-1",
                            "galaxyId": "home-galaxy",
                            "text": "Oat Milk",
                            "completed": false,
                            "category": "Groceries"
                          }
                        ]
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> error("Unexpected path: ${request.url.encodedPath}")
            }
        }

        val repo = OfflineFirstJlightRepository(
            tokenStore = tokenStore,
            apiClient = JlightApiClient(tokenStore, mockEngine),
        )

        val syncResult = repo.syncWithRemote()
        assertTrue(syncResult.isSuccess)

        val pairing = repo.observePairingState().first()
        assertEquals(PairingSessionState.Paired(galaxyId = "home-galaxy", agentName = "Galaxy Watch"), pairing)

        val occurrences = repo.observeOccurrences(
            windowStart = LocalDate.of(2026, 11, 7),
            windowEnd = LocalDate.of(2026, 11, 21),
        ).first()

        // 2026-11-14 is excluded via exdates, leaving Nov 7 and Nov 21
        assertEquals(
            listOf(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 21)),
            occurrences.map { it.occurrenceDate },
        )
        assertEquals(1, repo.observeChores().first().size)
        assertEquals("Oat Milk", repo.observeSharedLists().first().single().text)
    }
}
