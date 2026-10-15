// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.FullApplicationTest
import evaka.core.shared.McpAuthorizationId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import tools.jackson.databind.JsonNode

class McpUploadIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var serverController: McpServerController
    @Autowired private lateinit var uploadController: McpUploadController

    private val now = HelsinkiDateTime.of(LocalDateTime.of(2026, 9, 24, 12, 0))
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val accessToken = generateMcpSecret()
    private val body =
        """{"batch":"b","rows":[{"type":"care_area","rows":[{"name":"Upload area","shortName":"upload"}]}]}"""
    private lateinit var authorizationId: McpAuthorizationId

    @BeforeEach
    fun beforeEach() {
        authorizationId = db.transaction { tx ->
            tx.insert(admin)
            val clientId =
                tx.insertMcpClient("test", null, null, null, listOf(""), "none", null, null, now)
            tx.insertMcpAuthorization(
                    clientId,
                    admin.id,
                    McpServerConfig.MCP_SCOPE,
                    now.plusDays(1),
                    "code",
                    now.plusMinutes(1),
                    "challenge",
                    "",
                    null,
                    now,
                )
                .also { tx.issueMcpAccessToken(it, sha256Base64Url(accessToken), now) }
        }
    }

    @Test
    fun `upload URL works once`() {
        val url = createUploadUrl()
        assertEquals(HttpStatus.OK, upload(url).statusCode)
        assertEquals(
            1,
            db.read { it.createQuery { sql("SELECT count(*) FROM care_area") }.exactlyOne<Int>() },
        )
        assertEquals(HttpStatus.NOT_FOUND, upload(url).statusCode)
    }

    @Test
    fun `expired upload URL fails`() {
        assertEquals(
            HttpStatus.NOT_FOUND,
            upload(createUploadUrl(), now.plusMinutes(11)).statusCode,
        )
    }

    @Test
    fun `upload URL of a revoked authorization fails`() {
        val url = createUploadUrl()
        db.transaction { it.revokeMcpAuthorization(authorizationId, admin.id, now) }
        assertEquals(HttpStatus.UNAUTHORIZED, upload(url).statusCode)
    }

    @Test
    fun `unknown upload token fails`() {
        assertEquals(HttpStatus.NOT_FOUND, upload("${createUploadUrl()}x").statusCode)
    }

    private fun createUploadUrl(): String {
        val response =
            serverController.handle(
                dbInstance(),
                MockEvakaClock(now),
                MockHttpServletRequest().apply {
                    addHeader(MCP_AUTHORIZATION_HEADER, "Bearer $accessToken")
                },
                jsonMapper.readTree(
                    """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"create_upload_url"}}"""
                ),
            )
        return jsonMapper
            .valueToTree<JsonNode>(response.body)
            .path("result")
            .path("structuredContent")
            .path("url")
            .asString()
    }

    private fun upload(url: String, at: HelsinkiDateTime = now) =
        uploadController.upload(
            dbInstance(),
            MockEvakaClock(at),
            url.substringAfterLast("/"),
            jsonMapper.readTree(body),
        )
}
