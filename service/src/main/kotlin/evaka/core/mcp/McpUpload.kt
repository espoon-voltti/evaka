// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.ExcludeCodeGen
import evaka.core.shared.McpAuthorizationId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.JsonNode

private const val UPLOAD_TOOL = "insert_rows"

val createUploadUrlTool =
    mcpTool<EmptyInput>(
        name = "create_upload_url",
        description =
            "Returns a one-time URL, valid for 10 minutes, that runs $UPLOAD_TOOL with the POSTed JSON body as its arguments and responds with its result. Use it for large data sets: write the arguments to a file and upload it with the returned curl command, so that the rows don't pass through the conversation.",
    ) { ctx, _ ->
        val token = generateMcpSecret()
        val expiresAt = ctx.now.plusMinutes(10)
        ctx.tx.insertMcpUpload(ctx.authorizationId, sha256Base64Url(token), ctx.now, expiresAt)
        val url = "${ctx.config.resourceUrl}/uploads/$token"
        mapOf(
            "url" to url,
            "expiresAt" to expiresAt,
            "usage" to
                "curl -X POST -H 'Content-Type: application/json' --data-binary @rows.json $url",
        )
    }

@Profile("enable_mcp")
@RestController
@ExcludeCodeGen
class McpUploadController(private val server: McpServerController, private val tools: McpTools) {
    @PostMapping("/mcp/uploads/{token}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun upload(
        db: Database,
        clock: EvakaClock,
        @PathVariable token: String,
        @RequestBody body: JsonNode,
    ): ResponseEntity<Any> {
        val authorizationId =
            db.connect { dbc ->
                dbc.transaction { it.useMcpUpload(sha256Base64Url(token), clock.now()) }
            }
                ?: return server.unauthorized(
                    McpServerController.AuthResult.Failure(
                        HttpStatus.NOT_FOUND,
                        "invalid_token",
                        "Unknown, expired or already used upload URL",
                    )
                )
        val session =
            when (
                val auth = server.authenticate(db, clock) { getMcpAuthorization(authorizationId) }
            ) {
                is McpServerController.AuthResult.Failure -> return server.unauthorized(auth)
                is McpServerController.AuthResult.Success -> auth
            }
        val tool = tools.findTool(UPLOAD_TOOL) ?: error("Tool $UPLOAD_TOOL not found")
        server.setUserMdc(session.user)
        return try {
            ResponseEntity.ok(server.runTool(db, clock, session, tool, body, via = "upload"))
        } catch (e: McpServerController.McpToolFailure) {
            ResponseEntity.status(e.status).body(mapOf("error" to e.message))
        } finally {
            server.clearUserMdc()
        }
    }
}

private fun Database.Transaction.insertMcpUpload(
    authorizationId: McpAuthorizationId,
    tokenHash: String,
    now: HelsinkiDateTime,
    expiresAt: HelsinkiDateTime,
) {
    execute {
        sql(
            """
WITH expired AS (DELETE FROM mcp_upload WHERE expires_at < ${bind(now)})
INSERT INTO mcp_upload (created_at, authorization_id, token_hash, expires_at)
VALUES (${bind(now)}, ${bind(authorizationId)}, ${bind(tokenHash)}, ${bind(expiresAt)})
"""
        )
    }
}

private fun Database.Transaction.useMcpUpload(
    tokenHash: String,
    now: HelsinkiDateTime,
): McpAuthorizationId? = createUpdate {
    sql(
        """
UPDATE mcp_upload SET used_at = ${bind(now)}
WHERE token_hash = ${bind(tokenHash)} AND used_at IS NULL AND expires_at > ${bind(now)}
RETURNING authorization_id
"""
    )
}
    .executeAndReturnGeneratedKeys()
    .exactlyOneOrNull()
