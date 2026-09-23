// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.shared.McpAuthorizationId
import evaka.core.shared.McpClientId
import evaka.core.shared.McpTestDataBatchId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.net.URLEncoder
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private const val REREGISTERED_CLIENT_NAME = "Tekoälytyökalu (rekisteröity uudelleen)"

/**
 * Employee-facing endpoints for the MCP server: the OAuth consent page, listing and revoking the
 * user's own authorizations, and reviewing / deleting test data created via MCP.
 */
@Profile("enable_mcp")
@RestController
@RequestMapping("/employee/mcp")
class McpEmployeeController(
    private val accessControl: AccessControl,
    private val config: McpServerConfig,
) {
    data class McpConfigResponse(
        val serverUrl: String,
        val validityOptionsDays: List<Int>,
        val defaultValidityDays: Int,
    )

    @GetMapping("/config")
    fun getMcpConfig(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): McpConfigResponse {
        db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Global.MCP_PAGE)
            }
        }
        return McpConfigResponse(
            serverUrl = config.resourceUrl,
            validityOptionsDays = McpServerConfig.validityOptionsDays,
            defaultValidityDays = McpServerConfig.DEFAULT_VALIDITY_DAYS,
        )
    }

    data class McpOAuthClientInfo(
        val id: McpClientId,
        val clientName: String,
        val clientUri: String?,
        val redirectUris: List<String>,
        /** Whether the requested redirect_uri is registered for the client */
        val redirectUriAccepted: Boolean,
    )

    /**
     * Client details for the consent page. The redirect_uri of the authorization request is checked
     * here so the page never redirects the browser to an unregistered address, not even when the
     * user denies the request.
     */
    @GetMapping("/oauth-clients/{clientId}")
    fun getMcpOAuthClient(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable clientId: McpClientId,
        @RequestParam redirectUri: String,
    ): McpOAuthClientInfo {
        return db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.MANAGE_MCP_AUTHORIZATIONS,
                )
                val client =
                    tx.getMcpClient(clientId)
                        ?: if (McpOAuthController.isAcceptableRedirectUri(redirectUri))
                            return@read McpOAuthClientInfo(
                                clientId,
                                REREGISTERED_CLIENT_NAME,
                                clientUri = null,
                                listOf(redirectUri),
                                redirectUriAccepted = true,
                            )
                        else throw NotFound("Unknown MCP client")
                McpOAuthClientInfo(
                    client.id,
                    client.clientName,
                    client.clientUri,
                    client.redirectUris,
                    redirectUriAccepted =
                        client.redirectUris.any {
                            McpOAuthController.redirectUriMatches(it, redirectUri)
                        },
                )
            }
        }
    }

    data class McpAuthorizationRequest(
        val clientId: McpClientId,
        val redirectUri: String,
        val codeChallenge: String,
        val codeChallengeMethod: String,
        val state: String?,
        val scope: String?,
        val resource: String?,
        val validityDays: Int,
    )

    data class McpAuthorizationResponse(val redirectUrl: String)

    /**
     * Approves an OAuth authorization request: creates an authorization code and the redirect URL
     */
    @PostMapping("/authorizations")
    fun createMcpAuthorization(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: McpAuthorizationRequest,
    ): McpAuthorizationResponse {
        if (body.codeChallengeMethod != "S256")
            throw BadRequest("Only PKCE method S256 is supported")
        if (body.codeChallenge.length !in 43..128) throw BadRequest("Invalid code_challenge")
        if (body.validityDays !in 1..McpServerConfig.MAX_VALIDITY_DAYS) {
            throw BadRequest(
                "validityDays must be between 1 and ${McpServerConfig.MAX_VALIDITY_DAYS}"
            )
        }
        body.scope
            ?.split(' ')
            ?.filter { it.isNotBlank() }
            ?.firstOrNull { it != config.scope }
            ?.let { throw BadRequest("Unsupported scope: $it") }
        val now = clock.now()
        val code = generateMcpSecret()
        val authorizationId = db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.MANAGE_MCP_AUTHORIZATIONS,
                )
                val client =
                    tx.getMcpClient(body.clientId)
                        ?: reRegisterClient(tx, body.clientId, body.redirectUri, now)
                if (
                    client.redirectUris.none {
                        McpOAuthController.redirectUriMatches(it, body.redirectUri)
                    }
                ) {
                    throw BadRequest("redirect_uri is not registered for this client")
                }
                tx.insertMcpAuthorization(
                    clientId = client.id,
                    employeeId = user.id,
                    scope = config.scope,
                    expiresAt = now.plusDays(body.validityDays.toLong()),
                    codeHash = sha256Base64Url(code),
                    codeExpiresAt = now.plusMinutes(10),
                    codeChallenge = body.codeChallenge,
                    redirectUri = body.redirectUri,
                    resource = body.resource,
                    now = now,
                )
            }
        }
        Audit.McpAuthorizationCreate.log(
            targetId = AuditId(authorizationId),
            objectId = AuditId(body.clientId),
            meta = mapOf("validityDays" to body.validityDays),
        )
        val redirectUrl = buildString {
            append(body.redirectUri)
            append(if (body.redirectUri.contains('?')) '&' else '?')
            append("code=").append(URLEncoder.encode(code, Charsets.UTF_8))
            body.state?.let { append("&state=").append(URLEncoder.encode(it, Charsets.UTF_8)) }
            append("&iss=").append(URLEncoder.encode(config.issuer, Charsets.UTF_8))
        }
        return McpAuthorizationResponse(redirectUrl)
    }

    /**
     * An AI tool can keep using a client id that no longer exists (e.g. after the database of a
     * test environment was reset) without a way to register again. The client is then registered
     * again with the same id as a public client. This is no weaker than the open dynamic client
     * registration: anyone could register a client with the same redirect URI, and the consent page
     * shows it to the user in both cases.
     */
    private fun reRegisterClient(
        tx: Database.Transaction,
        clientId: McpClientId,
        redirectUri: String,
        now: HelsinkiDateTime,
    ): McpClient {
        if (!McpOAuthController.isAcceptableRedirectUri(redirectUri))
            throw NotFound("Unknown MCP client")
        val id =
            tx.insertMcpClient(
                clientName = REREGISTERED_CLIENT_NAME,
                clientUri = null,
                softwareId = null,
                softwareVersion = null,
                redirectUris = listOf(redirectUri),
                tokenEndpointAuthMethod = "none",
                clientSecretHash = null,
                registrationIp = null,
                now = now,
                id = clientId,
            )
        return tx.getMcpClient(id)!!
    }

    @GetMapping("/authorizations")
    fun getMcpAuthorizations(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<McpAuthorizationSummary> {
        return db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.MCP_PAGE,
                )
                tx.getMcpAuthorizationsOfEmployee(user.id, clock.now())
            }
        }
    }

    @DeleteMapping("/authorizations/{id}")
    fun revokeMcpAuthorization(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: McpAuthorizationId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.MANAGE_MCP_AUTHORIZATIONS,
                )
                val authorization = tx.getMcpAuthorization(id)
                if (authorization == null || authorization.employeeId != user.id)
                    throw NotFound("Authorization not found")
                if (!tx.revokeMcpAuthorization(id, user.id, clock.now())) {
                    throw BadRequest("Authorization has already been revoked")
                }
            }
        }
        Audit.McpAuthorizationRevoke.log(targetId = AuditId(id))
    }

    @GetMapping("/test-data/batches")
    fun getMcpTestDataBatches(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<McpTestDataService.BatchSummary> {
        return db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.MCP_PAGE,
                )
                McpTestDataService.getBatchSummaries(tx)
            }
        }
    }

    data class McpTestDataBatchDetails(
        val batch: McpTestDataBatch,
        val entities: List<McpTestDataEntity>,
    )

    @GetMapping("/test-data/batches/{id}")
    fun getMcpTestDataBatch(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: McpTestDataBatchId,
    ): McpTestDataBatchDetails {
        return db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.MCP_PAGE,
                )
                val batch = tx.getMcpTestDataBatch(id) ?: throw NotFound("Batch not found")
                McpTestDataBatchDetails(batch, tx.getMcpTestDataEntities(id))
            }
        }
    }

    /**
     * Reports what deleting the batch would remove, so the admin can review it before confirming
     */
    @GetMapping("/test-data/batches/{id}/deletion-preview")
    fun getMcpTestDataBatchDeletionPreview(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: McpTestDataBatchId,
    ): McpTestDataService.DeletionResult {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.MANAGE_MCP_TEST_DATA,
                )
                McpTestDataService.previewDeletion(tx, id)
            }
        }
    }

    @DeleteMapping("/test-data/batches/{id}")
    fun deleteMcpTestDataBatch(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: McpTestDataBatchId,
    ): McpTestDataService.DeletionResult {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.MANAGE_MCP_TEST_DATA,
                    )
                    // The UI shows the deletion preview, untracked rows included, before the
                    // admin confirms
                    McpTestDataService.deleteBatch(tx, id, allowUntrackedRows = true)
                }
            }
            .also {
                Audit.McpTestDataBatchDelete.log(
                    targetId = AuditId(id),
                    meta = mapOf("deletedRowCounts" to it.deletedRowCounts),
                )
            }
    }
}
