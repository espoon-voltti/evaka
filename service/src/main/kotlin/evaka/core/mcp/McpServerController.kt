// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.ExcludeCodeGen
import evaka.core.pis.getEmployee
import evaka.core.pis.getEmployeeRoles
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import fi.espoo.voltti.logging.MdcKey
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode

private val logger = KotlinLogging.logger {}

/** Header used by apigw to forward the AI client's `Authorization` header to the service */
const val MCP_AUTHORIZATION_HEADER = "X-Evaka-Mcp-Authorization"

/**
 * MCP (Model Context Protocol) server endpoint using the Streamable HTTP transport in stateless
 * mode: every request is a JSON-RPC 2.0 message (or batch) and the response is returned as JSON.
 * Server-initiated streams (GET / SSE) are not supported, which is allowed by the specification.
 *
 * Authentication uses OAuth 2.1 bearer tokens issued by [McpOAuthController].
 */
@Profile("enable_mcp")
@RestController
@ExcludeCodeGen
class McpServerController(
    private val config: McpServerConfig,
    private val tools: McpTools,
    private val accessControl: AccessControl,
    private val jsonMapper: JsonMapper,
) {
    private val supportedProtocolVersions = listOf("2025-11-25", "2025-06-18", "2025-03-26")
    private val defaultProtocolVersion = "2025-06-18"

    @GetMapping("/mcp")
    fun getNotSupported(): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .header(HttpHeaders.ALLOW, "POST")
            .build()

    @DeleteMapping("/mcp")
    fun deleteSession(): ResponseEntity<Void> = ResponseEntity.noContent().build()

    @PostMapping("/mcp", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun handle(
        db: Database,
        clock: EvakaClock,
        request: HttpServletRequest,
        @RequestBody body: JsonNode,
    ): ResponseEntity<Any> {
        val session =
            when (val auth = authenticate(db, clock, request)) {
                is AuthResult.Failure -> return unauthorized(auth)
                is AuthResult.Success -> auth
            }

        // apigw sends no X-User for MCP requests, so the user identity is added to the logs here
        // like RequestToAuthenticatedUser does for normal requests
        MdcKey.USER_ID.set(session.user.rawId().toString())
        MdcKey.USER_ID_HASH.set(session.user.rawIdHash.toString())
        MdcKey.USER_ROLES.set(
            (session.user.globalRoles + session.user.allScopedRoles)
                .map { it.name }
                .sorted()
                .joinToString("|", "|", "|")
        )
        try {
            return when {
                body.isArray -> {
                    val responses =
                        (body as ArrayNode).mapNotNull { handleMessage(db, clock, session, it) }
                    if (responses.isEmpty()) ResponseEntity.accepted().build()
                    else ResponseEntity.ok(responses)
                }
                body.isObject -> {
                    val response = handleMessage(db, clock, session, body)
                    if (response == null) ResponseEntity.accepted().build()
                    else ResponseEntity.ok(response)
                }
                else ->
                    ResponseEntity.badRequest()
                        .body(
                            errorResponse(
                                null,
                                INVALID_REQUEST,
                                "Request body must be a JSON-RPC message",
                            )
                        )
            }
        } finally {
            MdcKey.USER_ROLES.unset()
            MdcKey.USER_ID_HASH.unset()
            MdcKey.USER_ID.unset()
        }
    }

    private sealed interface AuthResult {
        data class Success(
            val user: AuthenticatedUser.Employee,
            val authorization: McpAuthorization,
            val clientName: String,
        ) : AuthResult

        data class Failure(val status: HttpStatus, val error: String?, val description: String) :
            AuthResult
    }

    private fun authenticate(
        db: Database,
        clock: EvakaClock,
        request: HttpServletRequest,
    ): AuthResult {
        val header =
            request.getHeader(MCP_AUTHORIZATION_HEADER)
                ?: request.getHeader(HttpHeaders.AUTHORIZATION)
        val token =
            header?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }?.substring(7)?.trim()
        if (token.isNullOrEmpty()) {
            return AuthResult.Failure(HttpStatus.UNAUTHORIZED, null, "Missing bearer token")
        }
        val now = clock.now()
        return db.connect { dbc ->
            dbc.transaction { tx ->
                val authorization =
                    tx.getMcpAuthorizationByAccessTokenHash(sha256Base64Url(token))
                        ?: return@transaction AuthResult.Failure(
                            HttpStatus.UNAUTHORIZED,
                            "invalid_token",
                            "Unknown access token",
                        )
                if (!authorization.isActive(now)) {
                    return@transaction AuthResult.Failure(
                        HttpStatus.UNAUTHORIZED,
                        "invalid_token",
                        if (authorization.revokedAt != null) "Authorization has been revoked"
                        else "Authorization has expired",
                    )
                }
                val employee = tx.getEmployee(authorization.employeeId)
                if (employee == null || !employee.active) {
                    return@transaction AuthResult.Failure(
                        HttpStatus.UNAUTHORIZED,
                        "invalid_token",
                        "Employee account is not active",
                    )
                }
                val user =
                    AuthenticatedUser.Employee(
                        authorization.employeeId,
                        tx.getEmployeeRoles(authorization.employeeId),
                    )
                if (
                    !accessControl.hasPermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.MANAGE_MCP_AUTHORIZATIONS,
                    )
                ) {
                    return@transaction AuthResult.Failure(
                        HttpStatus.FORBIDDEN,
                        "insufficient_scope",
                        "Only admin users may use the MCP server",
                    )
                }
                if (
                    authorization.lastUsedAt == null ||
                        now.durationSince(authorization.lastUsedAt).toMinutes() >= 1
                ) {
                    tx.updateMcpAuthorizationLastUsed(authorization.id, now)
                }
                val client = tx.getMcpClient(authorization.clientId)
                AuthResult.Success(user, authorization, client?.clientName ?: "unknown")
            }
        }
    }

    private fun unauthorized(failure: AuthResult.Failure): ResponseEntity<Any> {
        Audit.McpUnauthorizedRequest.log(meta = mapOf("reason" to failure.description))
        val challenge = buildString {
            append("Bearer realm=\"evaka-mcp\"")
            append(", resource_metadata=\"${config.protectedResourceMetadataUrl}\"")
            failure.error?.let { append(", error=\"$it\"") }
            append(", error_description=\"${failure.description}\"")
        }
        return ResponseEntity.status(failure.status)
            .header(HttpHeaders.WWW_AUTHENTICATE, challenge)
            .body(
                mapOf(
                    "error" to (failure.error ?: "unauthorized"),
                    "error_description" to failure.description,
                )
            )
    }

    /** Returns null for notifications (no response) */
    private fun handleMessage(
        db: Database,
        clock: EvakaClock,
        session: AuthResult.Success,
        message: JsonNode,
    ): ObjectNode? {
        val id = message.get("id")?.takeUnless { it.isNull }
        val method = message.get("method")?.takeIf { it.isString }?.asString()
        if (message.get("jsonrpc")?.takeIf { it.isString }?.asString() != "2.0" || method == null) {
            // A response from the client (e.g. to a server request) or garbage: nothing to answer
            return if (id == null) null
            else errorResponse(id, INVALID_REQUEST, "Not a valid JSON-RPC 2.0 request")
        }
        val params = message.get("params")?.takeIf { it.isObject } as ObjectNode?
        val isNotification = id == null

        return try {
            val result: Any? =
                when (method) {
                    "initialize" -> initialize(params, session)
                    "ping" -> emptyMap<String, Any>()
                    "tools/list" -> listTools()
                    "tools/call" -> callTool(db, clock, session, params)
                    "resources/list" -> mapOf("resources" to emptyList<Any>())
                    "resources/templates/list" -> mapOf("resourceTemplates" to emptyList<Any>())
                    "prompts/list" -> mapOf("prompts" to emptyList<Any>())
                    "logging/setLevel" -> emptyMap<String, Any>()
                    else -> {
                        if (method.startsWith("notifications/")) return null
                        return if (isNotification) null
                        else errorResponse(id, METHOD_NOT_FOUND, "Method not found: $method")
                    }
                }
            if (isNotification) null else successResponse(id, result)
        } catch (e: BadRequest) {
            if (isNotification) null else errorResponse(id, INVALID_PARAMS, e.message)
        } catch (e: Exception) {
            logger.error(e) { "MCP request failed: $method" }
            if (isNotification) null
            else errorResponse(id, INTERNAL_ERROR, e.message ?: "Internal error")
        }
    }

    private fun initialize(params: ObjectNode?, session: AuthResult.Success): Any {
        val requested = params?.get("protocolVersion")?.takeIf { it.isString }?.asString()
        val version =
            if (requested != null && requested in supportedProtocolVersions) requested
            else defaultProtocolVersion
        return mapOf(
            "protocolVersion" to version,
            "capabilities" to mapOf("tools" to mapOf("listChanged" to false)),
            "serverInfo" to
                mapOf("name" to "evaka-mcp", "title" to "eVaka test data", "version" to "1.0.0"),
            "instructions" to
                """
This is a NON-PRODUCTION eVaka (early childhood education) environment at ${config.baseUrl}.
You act on behalf of the eVaka admin who authorized the client "${session.clientName}".
You are expected to have the eVaka source code available: read it at the commit given by
get_environment_info (appCommit) instead of relying on the tool descriptions. Put all data of
one scenario in one batch. Insert rows with insert_rows (the Dev* fixture classes); send larger
data sets with create_upload_url. delete_test_data removes a batch.
"""
                    .trimIndent(),
        )
    }

    private fun listTools(): Any =
        mapOf(
            "tools" to
                tools.tools.map { tool ->
                    mapOf(
                        "name" to tool.name,
                        "description" to tool.description,
                        "inputSchema" to tool.inputSchema,
                        "annotations" to
                            mapOf(
                                "readOnlyHint" to tool.readOnly,
                                "destructiveHint" to tool.destructive,
                                "idempotentHint" to tool.readOnly,
                                "openWorldHint" to false,
                            ),
                    )
                }
        )

    private fun callTool(
        db: Database,
        clock: EvakaClock,
        session: AuthResult.Success,
        params: ObjectNode?,
    ): Any {
        val name =
            params?.get("name")?.takeIf { it.isString }?.asString()
                ?: throw BadRequest("Missing tool name")
        val tool = tools.findTool(name) ?: throw BadRequest("Unknown tool: $name")
        val arguments = params.get("arguments")?.takeUnless { it.isNull }
        val audit = AuditContext().add(session.authorization.id)
        val result: Any =
            try {
                db.connect { dbc ->
                    dbc.transaction { tx ->
                        val ctx =
                            McpToolContext(
                                tx,
                                session.user,
                                clock,
                                session.authorization.id,
                                config,
                                audit,
                            )
                        tool.call(ctx, arguments, jsonMapper)
                    }
                }
            } catch (e: BadRequest) {
                return toolError("Invalid input: ${e.message}")
            } catch (e: NotFound) {
                return toolError("Not found: ${e.message}")
            } catch (e: Conflict) {
                return toolError("Conflict: ${e.message}")
            } catch (e: Forbidden) {
                return toolError("Forbidden: ${e.message}")
            } catch (e: Exception) {
                logger.warn(e) { "MCP tool $name failed" }
                return toolError("Tool failed: ${e.message}")
            } finally {
                audit
                    .addMeta("tool", name)
                    .addMeta("client", session.clientName)
                    .log(Audit.McpToolCall, clock)
            }
        val json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)
        return mapOf(
            "content" to listOf(mapOf("type" to "text", "text" to json)),
            "structuredContent" to jsonMapper.valueToTree<JsonNode>(result),
            "isError" to false,
        )
    }

    private fun toolError(message: String): Any =
        mapOf("content" to listOf(mapOf("type" to "text", "text" to message)), "isError" to true)

    private fun successResponse(id: JsonNode?, result: Any?): ObjectNode =
        jsonMapper.createObjectNode().apply {
            put("jsonrpc", "2.0")
            set("id", id ?: jsonMapper.nullNode())
            set("result", jsonMapper.valueToTree<JsonNode>(result))
        }

    private fun errorResponse(id: JsonNode?, code: Int, message: String): ObjectNode =
        jsonMapper.createObjectNode().apply {
            put("jsonrpc", "2.0")
            set("id", id ?: jsonMapper.nullNode())
            set(
                "error",
                jsonMapper.createObjectNode().apply {
                    put("code", code)
                    put("message", message)
                },
            )
        }

    companion object {
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
        const val INTERNAL_ERROR = -32603
    }
}
