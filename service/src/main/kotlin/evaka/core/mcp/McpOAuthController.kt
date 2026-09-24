// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.ExcludeCodeGen
import evaka.core.shared.McpClientId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import java.net.URI
import java.util.Base64
import java.util.UUID
import org.springframework.context.annotation.Profile
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.JsonNode

private val logger = KotlinLogging.logger {}

/**
 * Minimal OAuth 2.1 authorization server for MCP clients:
 * - RFC 8414 / RFC 9728 metadata documents (served at the site root via apigw + nginx)
 * - RFC 7591 dynamic client registration
 * - authorization code grant with mandatory PKCE (S256), public clients
 *
 * The authorization endpoint itself is a page in the employee frontend where a logged-in admin
 * approves the request and chooses how long the access is valid. The page calls
 * [McpEmployeeController.createMcpAuthorization], which creates the authorization code.
 */
@Profile("enable_mcp")
@RestController
@ExcludeCodeGen
class McpOAuthController(private val config: McpServerConfig) {

    @GetMapping(
        "/mcp/.well-known/oauth-authorization-server",
        "/mcp/.well-known/oauth-authorization-server/**",
        "/mcp/.well-known/openid-configuration",
        "/mcp/.well-known/openid-configuration/**",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun authorizationServerMetadata(): Map<String, Any> =
        mapOf(
            "issuer" to config.issuer,
            "authorization_endpoint" to config.authorizationEndpoint,
            "token_endpoint" to config.tokenEndpoint,
            "registration_endpoint" to config.registrationEndpoint,
            "scopes_supported" to listOf(config.scope),
            "response_types_supported" to listOf("code"),
            "response_modes_supported" to listOf("query"),
            "grant_types_supported" to listOf("authorization_code"),
            "token_endpoint_auth_methods_supported" to
                listOf("none", "client_secret_post", "client_secret_basic"),
            "code_challenge_methods_supported" to listOf("S256"),
            "service_documentation" to "${config.baseUrl}/employee/mcp",
            "ui_locales_supported" to listOf("fi", "sv"),
        )

    @GetMapping(
        "/mcp/.well-known/oauth-protected-resource",
        "/mcp/.well-known/oauth-protected-resource/**",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun protectedResourceMetadata(): Map<String, Any> =
        mapOf(
            "resource" to config.resourceUrl,
            "authorization_servers" to listOf(config.issuer),
            "scopes_supported" to listOf(config.scope),
            "bearer_methods_supported" to listOf("header"),
            "resource_name" to "eVaka MCP (test data)",
            "resource_documentation" to "${config.baseUrl}/employee/mcp",
        )

    /** RFC 7591 dynamic client registration. Anyone can register; access is granted per user. */
    @PostMapping("/mcp/oauth/register", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun registerClient(
        db: Database,
        clock: EvakaClock,
        request: HttpServletRequest,
        @RequestBody body: JsonNode,
    ): ResponseEntity<Any> {
        fun text(field: String): String? =
            body.get(field)?.takeIf { it.isString }?.asString()?.trim()?.takeIf { it.isNotEmpty() }
        fun texts(field: String): List<String> =
            body
                .get(field)
                ?.takeIf { it.isArray }
                ?.mapNotNull { it.takeIf { n -> n.isString }?.asString()?.trim() }
                .orEmpty()

        val redirectUris = texts("redirect_uris").distinct()
        if (redirectUris.isEmpty()) {
            return oauthError(
                HttpStatus.BAD_REQUEST,
                "invalid_redirect_uri",
                "redirect_uris is required",
            )
        }
        redirectUris
            .firstOrNull { !isAcceptableRedirectUri(it) }
            ?.let {
                return oauthError(
                    HttpStatus.BAD_REQUEST,
                    "invalid_redirect_uri",
                    "Unacceptable redirect_uri: $it",
                )
            }
        val grantTypes = texts("grant_types").ifEmpty { listOf("authorization_code") }
        if ("authorization_code" !in grantTypes) {
            return oauthError(
                HttpStatus.BAD_REQUEST,
                "invalid_client_metadata",
                "grant_types must include authorization_code",
            )
        }
        val authMethod = text("token_endpoint_auth_method") ?: "none"
        if (authMethod !in setOf("none", "client_secret_post", "client_secret_basic")) {
            return oauthError(
                HttpStatus.BAD_REQUEST,
                "invalid_client_metadata",
                "Unsupported token_endpoint_auth_method: $authMethod",
            )
        }
        val clientSecret = if (authMethod == "none") null else generateMcpSecret()
        val clientName = (text("client_name") ?: "Unnamed MCP client").take(200)
        val clientUri = text("client_uri")?.take(500)
        val now = clock.now()
        val clientId = db.connect { dbc ->
            dbc.transaction { tx ->
                tx.insertMcpClient(
                    clientName = clientName,
                    clientUri = clientUri,
                    softwareId = text("software_id")?.take(200),
                    softwareVersion = text("software_version")?.take(100),
                    redirectUris = redirectUris,
                    tokenEndpointAuthMethod = authMethod,
                    clientSecretHash = clientSecret?.let(::sha256Base64Url),
                    registrationIp = request.getHeader("x-real-ip") ?: request.remoteAddr,
                    now = now,
                )
            }
        }
        Audit.McpClientRegister.log(
            targetId = AuditId(clientId),
            meta = mapOf("clientName" to clientName),
        )
        val response =
            linkedMapOf<String, Any>(
                "client_id" to clientId.toString(),
                "client_id_issued_at" to now.toInstant().epochSecond,
                "client_name" to clientName,
                "redirect_uris" to redirectUris,
                "grant_types" to listOf("authorization_code"),
                "response_types" to listOf("code"),
                "token_endpoint_auth_method" to authMethod,
                "scope" to config.scope,
            )
        clientUri?.let { response["client_uri"] = it }
        if (clientSecret != null) {
            response["client_secret"] = clientSecret
            response["client_secret_expires_at"] = 0
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noStore())
            .body(response)
    }

    /** RFC 6749 token endpoint, authorization code grant with PKCE */
    @PostMapping(
        "/mcp/oauth/token",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun token(
        db: Database,
        clock: EvakaClock,
        request: HttpServletRequest,
        @RequestParam params: Map<String, String>,
    ): ResponseEntity<Any> {
        val grantType = params["grant_type"]
        if (grantType != "authorization_code") {
            return oauthError(
                HttpStatus.BAD_REQUEST,
                "unsupported_grant_type",
                "Only authorization_code is supported",
            )
        }
        val code = params["code"]?.trim().orEmpty()
        val codeVerifier = params["code_verifier"]?.trim().orEmpty()
        if (code.isEmpty() || codeVerifier.isEmpty()) {
            return oauthError(
                HttpStatus.BAD_REQUEST,
                "invalid_request",
                "code and code_verifier are required",
            )
        }
        val (basicClientId, basicSecret) = parseBasicAuth(request)
        val clientIdParam = params["client_id"]?.trim() ?: basicClientId
        val clientSecretParam = params["client_secret"]?.trim() ?: basicSecret
        val clientId =
            clientIdParam?.let { runCatching { McpClientId(UUID.fromString(it)) }.getOrNull() }
                ?: return oauthError(HttpStatus.UNAUTHORIZED, "invalid_client", "Unknown client_id")

        val now = clock.now()
        val result = db.connect { dbc ->
            dbc.transaction { tx ->
                tx.deleteStaleMcpAuthorizations(now)
                val client =
                    tx.getMcpClient(clientId)
                        ?: return@transaction TokenResult.Error(
                            HttpStatus.UNAUTHORIZED,
                            "invalid_client",
                            "Unknown client_id",
                        )
                if (client.tokenEndpointAuthMethod != "none") {
                    if (
                        clientSecretParam == null ||
                            client.clientSecretHash != sha256Base64Url(clientSecretParam)
                    ) {
                        return@transaction TokenResult.Error(
                            HttpStatus.UNAUTHORIZED,
                            "invalid_client",
                            "Invalid client credentials",
                        )
                    }
                }
                val authorization =
                    tx.getMcpAuthorizationByCodeHash(sha256Base64Url(code))
                        ?: return@transaction TokenResult.Error(
                            HttpStatus.BAD_REQUEST,
                            "invalid_grant",
                            "Unknown or already used authorization code",
                        )
                if (authorization.clientId != clientId) {
                    return@transaction TokenResult.Error(
                        HttpStatus.BAD_REQUEST,
                        "invalid_grant",
                        "Authorization code was issued to another client",
                    )
                }
                if (
                    authorization.codeExpiresAt == null || !authorization.codeExpiresAt.isAfter(now)
                ) {
                    return@transaction TokenResult.Error(
                        HttpStatus.BAD_REQUEST,
                        "invalid_grant",
                        "Authorization code has expired",
                    )
                }
                if (!authorization.isActive(now)) {
                    return@transaction TokenResult.Error(
                        HttpStatus.BAD_REQUEST,
                        "invalid_grant",
                        "Authorization is no longer valid",
                    )
                }
                val redirectUri = params["redirect_uri"]?.trim()
                if (redirectUri != null && redirectUri != authorization.redirectUri) {
                    return@transaction TokenResult.Error(
                        HttpStatus.BAD_REQUEST,
                        "invalid_grant",
                        "redirect_uri does not match",
                    )
                }
                if (!verifyPkceS256(codeVerifier, authorization.codeChallenge)) {
                    return@transaction TokenResult.Error(
                        HttpStatus.BAD_REQUEST,
                        "invalid_grant",
                        "PKCE verification failed",
                    )
                }
                val accessToken = generateMcpSecret()
                tx.issueMcpAccessToken(authorization.id, sha256Base64Url(accessToken), now)
                TokenResult.Success(authorization, accessToken)
            }
        }
        return when (result) {
            is TokenResult.Error -> {
                Audit.McpTokenIssue.log(
                    targetId = AuditId(clientId),
                    meta = mapOf("error" to result.error, "description" to result.description),
                )
                oauthError(result.status, result.error, result.description)
            }
            is TokenResult.Success -> {
                Audit.McpTokenIssue.log(
                    targetId = AuditId(result.authorization.id),
                    objectId = AuditId(result.authorization.employeeId),
                    meta =
                        mapOf(
                            "clientId" to clientId,
                            "expiresAt" to result.authorization.expiresAt,
                        ),
                )
                val expiresIn = result.authorization.expiresAt.durationSince(now).seconds
                ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .body(
                        mapOf(
                            "access_token" to result.accessToken,
                            "token_type" to "Bearer",
                            "expires_in" to expiresIn,
                            "scope" to result.authorization.scope,
                        )
                    )
            }
        }
    }

    private sealed interface TokenResult {
        data class Success(val authorization: McpAuthorization, val accessToken: String) :
            TokenResult

        data class Error(val status: HttpStatus, val error: String, val description: String) :
            TokenResult
    }

    private fun parseBasicAuth(request: HttpServletRequest): Pair<String?, String?> {
        val header =
            request.getHeader(MCP_AUTHORIZATION_HEADER)
                ?: request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header == null || !header.startsWith("Basic ", ignoreCase = true)) return null to null
        return try {
            val decoded =
                String(Base64.getDecoder().decode(header.substring(6).trim()), Charsets.UTF_8)
            val idx = decoded.indexOf(':')
            if (idx < 0) null to null
            else
                java.net.URLDecoder.decode(decoded.substring(0, idx), Charsets.UTF_8) to
                    java.net.URLDecoder.decode(decoded.substring(idx + 1), Charsets.UTF_8)
        } catch (e: Exception) {
            logger.debug(e) { "Invalid basic auth header" }
            null to null
        }
    }

    private fun oauthError(
        status: HttpStatus,
        error: String,
        description: String,
    ): ResponseEntity<Any> =
        ResponseEntity.status(status)
            .cacheControl(CacheControl.noStore())
            .body(mapOf("error" to error, "error_description" to description))

    companion object {
        /**
         * Accepts https URLs, http URLs to the loopback interface (native apps, RFC 8252) and
         * custom scheme URLs (e.g. vscode://). Rejects everything else.
         */
        fun isAcceptableRedirectUri(value: String): Boolean {
            val uri = runCatching { URI(value) }.getOrNull() ?: return false
            val scheme = uri.scheme?.lowercase() ?: return false
            if (uri.fragment != null) return false
            return when (scheme) {
                "https" -> !uri.host.isNullOrEmpty()
                "http" -> uri.host in setOf("localhost", "127.0.0.1", "[::1]", "::1")
                "javascript",
                "data",
                "file",
                "vbscript" -> false
                // custom URI schemes used by native apps, e.g. vscode://
                else -> scheme.length > 1 && !value.contains(' ')
            }
        }

        /** Loopback redirect URIs may use any port (RFC 8252 section 7.3) */
        fun redirectUriMatches(registered: String, requested: String): Boolean {
            if (registered == requested) return true
            val a = runCatching { URI(registered) }.getOrNull() ?: return false
            val b = runCatching { URI(requested) }.getOrNull() ?: return false
            val loopback = setOf("localhost", "127.0.0.1", "[::1]", "::1")
            return a.scheme == "http" &&
                b.scheme == "http" &&
                a.host in loopback &&
                a.host == b.host &&
                a.path == b.path &&
                a.query == b.query
        }
    }
}
