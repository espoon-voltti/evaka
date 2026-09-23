// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.FullApplicationTest
import evaka.core.shared.Id
import evaka.core.shared.McpClientId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.net.URI
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import tools.jackson.databind.JsonNode

class McpIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var oauthController: McpOAuthController
    @Autowired private lateinit var employeeController: McpEmployeeController
    @Autowired private lateinit var serverController: McpServerController

    private val today = LocalDate.of(2026, 9, 23)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val otherAdmin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))

    private val redirectUri = "http://localhost:53000/callback"
    private val codeVerifier = "a".repeat(43) + "verifier-0123456789"
    private val codeChallenge = sha256Base64Url(codeVerifier)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(otherAdmin)
            tx.insert(serviceWorker)
        }
    }

    @Test
    fun `full OAuth flow, tool calls and cleanup`() {
        val clientId = registerClient()

        // Consent
        val redirectUrl =
            employeeController
                .createMcpAuthorization(
                    dbInstance(),
                    admin.user,
                    clock,
                    authorizationRequest(clientId),
                )
                .redirectUrl
        assertTrue(redirectUrl.startsWith("$redirectUri?code="))
        val query =
            URI(redirectUrl).query.split("&").associate {
                it.substringBefore("=") to it.substringAfter("=")
            }
        assertEquals("xyz", query["state"])
        val code = query["code"]!!

        // Authorization is listed as pending until the token is exchanged
        employeeController.getMcpAuthorizations(dbInstance(), admin.user, clock).single().let {
            assertFalse(it.tokenIssued)
            assertFalse(it.active)
            assertEquals("Claude Code", it.clientName)
        }

        // Wrong PKCE verifier is rejected
        exchangeCode(clientId, code, "wrong".repeat(10)).also {
            assertEquals(HttpStatus.BAD_REQUEST, it.statusCode)
            assertEquals("invalid_grant", (it.body as Map<*, *>)["error"])
        }

        // Token exchange
        val tokenResponse = exchangeCode(clientId, code, codeVerifier)
        assertEquals(HttpStatus.OK, tokenResponse.statusCode)
        val token = (tokenResponse.body as Map<*, *>)["access_token"] as String
        assertEquals(30L * 24 * 60 * 60, (tokenResponse.body as Map<*, *>)["expires_in"])

        // Code can be used only once
        assertEquals(HttpStatus.BAD_REQUEST, exchangeCode(clientId, code, codeVerifier).statusCode)

        employeeController.getMcpAuthorizations(dbInstance(), admin.user, clock).single().let {
            assertTrue(it.tokenIssued)
            assertTrue(it.active)
        }
        // MCP: initialize + tools/list
        val init =
            rpc(
                token,
                "initialize",
                """{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}""",
            )
        assertEquals("2025-06-18", init.path("result").path("protocolVersion").asString())
        val toolNames: List<String> =
            rpc(token, "tools/list", "{}").path("result").path("tools").toList().map {
                it.path("name").asString()
            }
        assertTrue(
            toolNames.containsAll(
                listOf("get_environment_info", "list_test_data", "delete_test_data")
            )
        )

        // Notifications get no response
        val notificationResponse =
            serverController.handle(
                dbInstance(),
                clock,
                mcpRequest(token),
                jsonMapper.readTree("""{"jsonrpc":"2.0","method":"notifications/initialized"}"""),
            )
        assertEquals(HttpStatus.ACCEPTED, notificationResponse.statusCode)

        val env = callTool(token, "get_environment_info", "{}")
        assertEquals(today.toString(), env.path("today").asString())
        assertTrue(env.has("appCommit"))

        // Tool errors are reported as tool results, not protocol errors
        assertTrue(
            callToolExpectingError(token, "delete_test_data", "{}").startsWith("Invalid input:")
        )

        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id)
        val child = DevPerson()
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = unit.id,
                startDate = today,
                endDate = today.plusYears(1),
            )
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(unit)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.track(
                "demo",
                "care_area" to area.id,
                "daycare" to unit.id,
                "person" to child.id,
                "child" to child.id,
                "placement" to placement.id,
            )
        }
        val batch =
            employeeController.getMcpTestDataBatches(dbInstance(), admin.user, clock).single()
        assertEquals(5, batch.totalEntities)

        val dryRun = callTool(token, "delete_test_data", """{"batchName":"demo","dryRun":true}""")
        assertEquals(1, dryRun.path("deletedRowCounts").path("placement").asInt())
        assertTrue(dryRun.path("untrackedRowCounts").isEmpty, dryRun.toString())
        assertEquals(1, countRows("daycare"))

        // A tester adds a placement in the UI on top of the test data: the tool refuses to take it
        // along unless explicitly allowed
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = unit.id,
                    startDate = today.plusYears(2),
                    endDate = today.plusYears(3),
                )
            )
        }
        val refused = callToolExpectingError(token, "delete_test_data", """{"batchName":"demo"}""")
        assertTrue(refused.startsWith("Conflict:") && refused.contains("placement: 1"), refused)
        assertEquals(2, countRows("placement"), "the refused deletion was rolled back")

        val deleted =
            callTool(
                token,
                "delete_test_data",
                """{"batchName":"demo","allowUntrackedRows":true}""",
            )
        assertEquals(2, deleted.path("deletedRowCounts").path("placement").asInt())
        listOf("care_area", "daycare", "person", "placement", "mcp_test_data_entity").forEach {
            assertEquals(0, countRows(it), "table $it should be empty")
        }

        // Revocation
        val authorizationId =
            employeeController.getMcpAuthorizations(dbInstance(), admin.user, clock).single().id
        employeeController.revokeMcpAuthorization(dbInstance(), admin.user, clock, authorizationId)
        val revoked =
            serverController.handle(
                dbInstance(),
                clock,
                mcpRequest(token),
                jsonMapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"ping"}"""),
            )
        assertEquals(HttpStatus.UNAUTHORIZED, revoked.statusCode)
        assertTrue(revoked.headers.getFirst("WWW-Authenticate")!!.contains("resource_metadata="))
    }

    @Test
    fun `test data tools act only on the caller's own batches`() {
        val clientId = registerClient()
        val token = authorizeAndGetToken(clientId)
        val otherToken = authorizeAndGetToken(clientId, user = otherAdmin.user)
        val employee = DevEmployee()
        db.transaction { tx ->
            tx.insert(employee)
            tx.track("mine", "employee" to employee.id)
        }
        val batchId =
            employeeController.getMcpTestDataBatches(dbInstance(), admin.user, clock).single().id

        assertEquals(
            listOf("mine"),
            callTool(token, "get_environment_info", "{}").path("testDataBatches").toList().map {
                it.path("name").asString()
            },
        )
        assertTrue(
            callTool(otherToken, "get_environment_info", "{}").path("testDataBatches").isEmpty
        )
        // ...but all batches are listed with their owners, so shared data is visible
        assertEquals(
            listOf("mine"),
            callTool(otherToken, "list_test_data", "{}").path("batches").toList().map {
                it.path("name").asString()
            },
        )
        assertEquals(
            1,
            callTool(token, "list_test_data", """{"batchName":"mine"}""")
                .path("entityPage")
                .path("totalEntities")
                .asInt(),
        )

        // Another user cannot delete the batch via MCP, not even by id
        assertTrue(
            callToolExpectingError(otherToken, "delete_test_data", """{"batchId":"$batchId"}""")
                .startsWith("Forbidden:")
        )
        assertTrue(
            callToolExpectingError(otherToken, "delete_test_data", """{"batchName":"mine"}""")
                .startsWith("Not found:")
        )
        callTool(token, "delete_test_data", """{"batchName":"mine"}""")
        assertEquals(3, countRows("employee"))
    }

    @Test
    fun `requests without a valid token are rejected with a challenge`() {
        val response =
            serverController.handle(
                dbInstance(),
                clock,
                MockHttpServletRequest(),
                jsonMapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"ping"}"""),
            )
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        val challenge = response.headers.getFirst("WWW-Authenticate")!!
        assertTrue(challenge.startsWith("Bearer "))
        assertTrue(challenge.contains("/.well-known/oauth-protected-resource"))

        val bogus =
            serverController.handle(
                dbInstance(),
                clock,
                mcpRequest("nonsense"),
                jsonMapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"ping"}"""),
            )
        assertEquals(HttpStatus.UNAUTHORIZED, bogus.statusCode)
    }

    @Test
    fun `only admins can authorize clients, and losing admin rights disables the token`() {
        val clientId = registerClient()
        assertThrows<Forbidden> {
            employeeController.createMcpAuthorization(
                dbInstance(),
                serviceWorker.user,
                clock,
                authorizationRequest(clientId),
            )
        }

        val token = authorizeAndGetToken(clientId)
        assertEquals(
            HttpStatus.OK,
            serverController
                .handle(
                    dbInstance(),
                    clock,
                    mcpRequest(token),
                    jsonMapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"ping"}"""),
                )
                .statusCode,
        )

        db.transaction { tx ->
            tx.execute {
                sql("UPDATE employee SET roles = '{SERVICE_WORKER}' WHERE id = ${bind(admin.id)}")
            }
        }
        val response =
            serverController.handle(
                dbInstance(),
                clock,
                mcpRequest(token),
                jsonMapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"ping"}"""),
            )
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `expired authorizations are rejected`() {
        val clientId = registerClient()
        val token = authorizeAndGetToken(clientId, validityDays = 1)
        val later = MockEvakaClock(now.plusDays(2))
        val response =
            serverController.handle(
                dbInstance(),
                later,
                mcpRequest(token),
                jsonMapper.readTree("""{"jsonrpc":"2.0","id":1,"method":"ping"}"""),
            )
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `authorization request validation`() {
        val clientId = registerClient()
        assertThrows<BadRequest> {
            employeeController.createMcpAuthorization(
                dbInstance(),
                admin.user,
                clock,
                authorizationRequest(clientId).copy(validityDays = 365),
            )
        }
        assertThrows<BadRequest> {
            employeeController.createMcpAuthorization(
                dbInstance(),
                admin.user,
                clock,
                authorizationRequest(clientId)
                    .copy(redirectUri = "https://evil.example.com/callback"),
            )
        }
        assertThrows<BadRequest> {
            employeeController.createMcpAuthorization(
                dbInstance(),
                admin.user,
                clock,
                authorizationRequest(clientId).copy(codeChallengeMethod = "plain"),
            )
        }
        // loopback redirect URIs may use any port
        employeeController.createMcpAuthorization(
            dbInstance(),
            admin.user,
            clock,
            authorizationRequest(clientId).copy(redirectUri = "http://localhost:61000/callback"),
        )
    }

    @Test
    fun `client registration validates redirect uris`() {
        val response =
            oauthController.registerClient(
                dbInstance(),
                clock,
                MockHttpServletRequest(),
                jsonMapper.readTree(
                    """{"client_name":"x","redirect_uris":["http://example.com/callback"]}"""
                ),
            )
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNull(
            employeeController.getMcpAuthorizations(dbInstance(), admin.user, clock).firstOrNull()
        )
    }

    @Test
    fun `metadata documents point to the right endpoints`() {
        val authServer = oauthController.authorizationServerMetadata()
        assertEquals("https://foo.espoonvarhaiskasvatus.fi", authServer["issuer"])
        assertEquals(
            "https://foo.espoonvarhaiskasvatus.fi/employee/mcp/authorize",
            authServer["authorization_endpoint"],
        )
        assertEquals(
            "https://foo.espoonvarhaiskasvatus.fi/api/mcp/oauth/token",
            authServer["token_endpoint"],
        )
        assertEquals(listOf("S256"), authServer["code_challenge_methods_supported"])
        val resource = oauthController.protectedResourceMetadata()
        assertEquals("https://foo.espoonvarhaiskasvatus.fi/api/mcp", resource["resource"])
        assertEquals(
            listOf("https://foo.espoonvarhaiskasvatus.fi"),
            resource["authorization_servers"],
        )
    }

    private fun registerClient(): McpClientId {
        val response =
            oauthController.registerClient(
                dbInstance(),
                clock,
                MockHttpServletRequest(),
                jsonMapper.readTree(
                    """{"client_name":"Claude Code","redirect_uris":["$redirectUri"],"grant_types":["authorization_code"],"response_types":["code"],"token_endpoint_auth_method":"none"}"""
                ),
            )
        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("none", body["token_endpoint_auth_method"])
        return McpClientId(UUID.fromString(body["client_id"] as String))
    }

    private fun authorizationRequest(clientId: McpClientId) =
        McpEmployeeController.McpAuthorizationRequest(
            clientId = clientId,
            redirectUri = redirectUri,
            codeChallenge = codeChallenge,
            codeChallengeMethod = "S256",
            state = "xyz",
            scope = McpServerConfig.MCP_SCOPE,
            resource = "https://foo.espoonvarhaiskasvatus.fi/api/mcp",
            validityDays = 30,
        )

    private fun exchangeCode(clientId: McpClientId, code: String, verifier: String) =
        oauthController.token(
            dbInstance(),
            clock,
            MockHttpServletRequest(),
            mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "code_verifier" to verifier,
                "client_id" to clientId.toString(),
                "redirect_uri" to redirectUri,
            ),
        )

    private fun authorizeAndGetToken(
        clientId: McpClientId,
        validityDays: Int = 30,
        user: AuthenticatedUser.Employee = admin.user,
    ): String {
        val redirectUrl =
            employeeController
                .createMcpAuthorization(
                    dbInstance(),
                    user,
                    clock,
                    authorizationRequest(clientId).copy(validityDays = validityDays),
                )
                .redirectUrl
        val code =
            URI(redirectUrl).query.split("&").first { it.startsWith("code=") }.substringAfter("=")
        val response = exchangeCode(clientId, code, codeVerifier)
        assertEquals(HttpStatus.OK, response.statusCode)
        return (response.body as Map<*, *>)["access_token"] as String
    }

    private fun mcpRequest(token: String) =
        MockHttpServletRequest().apply { addHeader(MCP_AUTHORIZATION_HEADER, "Bearer $token") }

    private fun rpc(token: String, method: String, params: String): JsonNode {
        val response =
            serverController.handle(
                dbInstance(),
                clock,
                mcpRequest(token),
                jsonMapper.readTree(
                    """{"jsonrpc":"2.0","id":"${UUID.randomUUID()}","method":"$method","params":$params}"""
                ),
            )
        assertEquals(HttpStatus.OK, response.statusCode)
        return jsonMapper.valueToTree(response.body)
    }

    private fun callTool(token: String, name: String, arguments: String): JsonNode {
        val response = rpc(token, "tools/call", """{"name":"$name","arguments":$arguments}""")
        val result = response.path("result")
        assertFalse(
            result.path("isError").asBoolean(),
            "Tool $name failed: ${result.path("content")}",
        )
        return result.path("structuredContent")
    }

    private fun callToolExpectingError(token: String, name: String, arguments: String): String {
        val response = rpc(token, "tools/call", """{"name":"$name","arguments":$arguments}""")
        val result = response.path("result")
        assertTrue(result.path("isError").asBoolean(), "Tool $name should have failed")
        return result.path("content").get(0).path("text").asString()
    }

    private fun Database.Transaction.track(
        batchName: String,
        vararg rows: Pair<String, Id<*>>,
    ) {
        val batchId =
            McpTestDataService.getOrCreateBatch(this, batchName, admin.evakaUserId, null, now)
        rows.forEach { (table, id) -> McpTestDataService.track(this, batchId, table, id, "", now) }
    }

    private fun countRows(table: String): Int = db.read { tx ->
        tx.createQuery { sql("SELECT count(*) FROM $table") }.exactlyOne<Int>()
    }
}
