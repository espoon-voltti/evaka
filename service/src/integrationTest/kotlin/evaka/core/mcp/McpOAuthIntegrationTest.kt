// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.FullApplicationTest
import evaka.core.shared.McpClientId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import java.net.URI
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalTime
import java.util.Base64
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

/** Edge cases of the OAuth endpoints that the happy path in McpIntegrationTest does not reach */
class McpOAuthIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var oauthController: McpOAuthController
    @Autowired private lateinit var employeeController: McpEmployeeController

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2026, 9, 23), LocalTime.of(12, 0)))
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))

    private val redirectUri = "http://localhost:53000/callback"
    private val codeVerifier = "a".repeat(43) + "verifier-0123456789"
    private val codeChallenge = sha256Base64Url(codeVerifier)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(admin) }
    }

    @Test
    fun `the consent page learns whether the requested redirect uri is registered`() {
        val clientId = registerClient()

        fun accepted(uri: String) =
            employeeController
                .getMcpOAuthClient(dbInstance(), admin.user, clock, clientId, uri)
                .redirectUriAccepted

        assertTrue(accepted(redirectUri))
        // loopback redirect URIs may use any port
        assertTrue(accepted("http://localhost:61000/callback"))
        assertFalse(accepted("https://evil.example.com/callback"))
        assertFalse(accepted("http://localhost:53000/other"))
        assertFalse(accepted("javascript:alert(1)"))
    }

    @Test
    fun `an unknown client id is registered again on approval`() {
        // e.g. an AI tool that still uses a client id registered before the database was reset
        val staleClientId = McpClientId(UUID.randomUUID())
        fun info(clientId: McpClientId, uri: String) =
            employeeController.getMcpOAuthClient(dbInstance(), admin.user, clock, clientId, uri)

        assertTrue(info(staleClientId, redirectUri).redirectUriAccepted)
        assertTrue(
            info(McpClientId(UUID.randomUUID()), "https://claude.ai/callback").redirectUriAccepted
        )
        assertThrows<NotFound> { info(McpClientId(UUID.randomUUID()), "javascript:alert(1)") }

        val code = authorize(staleClientId)
        assertEquals(HttpStatus.OK, exchangeCode(staleClientId, code, codeVerifier).statusCode)
    }

    @Test
    fun `token exchange rejects a redirect uri that differs from the authorization request`() {
        val clientId = registerClient()
        val code = authorize(clientId)

        val mismatch =
            exchangeCode(
                clientId,
                code,
                codeVerifier,
                extraParams = mapOf("redirect_uri" to "http://localhost:53000/other"),
            )
        assertEquals(HttpStatus.BAD_REQUEST, mismatch.statusCode)
        assertEquals("invalid_grant", (mismatch.body as Map<*, *>)["error"])

        // the failed attempt did not consume the code
        assertEquals(HttpStatus.OK, exchangeCode(clientId, code, codeVerifier).statusCode)
    }

    @Test
    fun `confidential clients must present their secret in the form body`() {
        val (clientId, secret) = registerConfidentialClient("client_secret_post")

        val withoutSecret = exchangeCode(clientId, authorize(clientId), codeVerifier)
        assertEquals(HttpStatus.UNAUTHORIZED, withoutSecret.statusCode)
        assertEquals("invalid_client", (withoutSecret.body as Map<*, *>)["error"])

        val wrongSecret =
            exchangeCode(
                clientId,
                authorize(clientId),
                codeVerifier,
                extraParams = mapOf("client_secret" to "wrong"),
            )
        assertEquals(HttpStatus.UNAUTHORIZED, wrongSecret.statusCode)

        val correct =
            exchangeCode(
                clientId,
                authorize(clientId),
                codeVerifier,
                extraParams = mapOf("client_secret" to secret),
            )
        assertEquals(HttpStatus.OK, correct.statusCode)
    }

    @Test
    fun `confidential clients can present their secret with HTTP basic auth`() {
        val (clientId, secret) = registerConfidentialClient("client_secret_basic")
        val code = authorize(clientId)

        fun basic(id: String, secret: String) =
            "Basic " +
                Base64.getEncoder()
                    .encodeToString(
                        "${URLEncoder.encode(id, Charsets.UTF_8)}:${URLEncoder.encode(secret, Charsets.UTF_8)}"
                            .toByteArray()
                    )

        val wrong =
            exchangeCode(
                clientId,
                code,
                codeVerifier,
                request =
                    MockHttpServletRequest().apply {
                        addHeader(MCP_AUTHORIZATION_HEADER, basic(clientId.toString(), "wrong"))
                    },
            )
        assertEquals(HttpStatus.UNAUTHORIZED, wrong.statusCode)

        val correct =
            exchangeCode(
                clientId,
                code,
                codeVerifier,
                request =
                    MockHttpServletRequest().apply {
                        addHeader(MCP_AUTHORIZATION_HEADER, basic(clientId.toString(), secret))
                    },
            )
        assertEquals(HttpStatus.OK, correct.statusCode)
    }

    private fun registerClient(): McpClientId {
        val response =
            oauthController.registerClient(
                dbInstance(),
                clock,
                MockHttpServletRequest(),
                jsonMapper.readTree(
                    """{"client_name":"Claude Code","redirect_uris":["$redirectUri"],"token_endpoint_auth_method":"none"}"""
                ),
            )
        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body as Map<*, *>
        assertNull(body["client_secret"])
        return McpClientId(UUID.fromString(body["client_id"] as String))
    }

    private fun registerConfidentialClient(authMethod: String): Pair<McpClientId, String> {
        val response =
            oauthController.registerClient(
                dbInstance(),
                clock,
                MockHttpServletRequest(),
                jsonMapper.readTree(
                    """{"client_name":"Server app","redirect_uris":["$redirectUri"],"token_endpoint_auth_method":"$authMethod"}"""
                ),
            )
        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals(authMethod, body["token_endpoint_auth_method"])
        return McpClientId(UUID.fromString(body["client_id"] as String)) to
            (body["client_secret"] as String)
    }

    /** Approves an authorization request as the admin and returns the authorization code */
    private fun authorize(clientId: McpClientId): String {
        val redirectUrl =
            employeeController
                .createMcpAuthorization(
                    dbInstance(),
                    admin.user,
                    clock,
                    McpEmployeeController.McpAuthorizationRequest(
                        clientId = clientId,
                        redirectUri = redirectUri,
                        codeChallenge = codeChallenge,
                        codeChallengeMethod = "S256",
                        state = null,
                        scope = null,
                        resource = null,
                        validityDays = 30,
                    ),
                )
                .redirectUrl
        return URI(redirectUrl)
            .query
            .split("&")
            .first { it.startsWith("code=") }
            .substringAfter("=")
    }

    private fun exchangeCode(
        clientId: McpClientId,
        code: String,
        verifier: String,
        extraParams: Map<String, String> = emptyMap(),
        request: MockHttpServletRequest = MockHttpServletRequest(),
    ) =
        oauthController.token(
            dbInstance(),
            clock,
            request,
            mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "code_verifier" to verifier,
                "client_id" to clientId.toString(),
                "redirect_uri" to redirectUri,
            ) + extraParams,
        )
}
