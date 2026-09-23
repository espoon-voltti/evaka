// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.EvakaEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment

private val logger = KotlinLogging.logger {}

/**
 * Configuration for the MCP (Model Context Protocol) server that lets AI assistants (Claude,
 * Copilot, etc.) generate and clean up test data in non-production environments.
 *
 * The whole feature is behind the `enable_mcp` Spring profile. It is enabled automatically in local
 * development and in `dev`/`test` deployments (see `Main.kt`). Other environments (e.g. staging)
 * must add the profile explicitly. As an extra safety net, startup fails if the profile is active
 * in a production deployment.
 */
@Configuration
@Profile("enable_mcp")
class McpConfig {
    @Bean
    fun mcpServerConfig(evakaEnv: EvakaEnv, env: Environment): McpServerConfig {
        val volttiEnv = env.getProperty("VOLTTI_ENV")?.lowercase()
        if (volttiEnv == "prod" || volttiEnv == "production") {
            error(
                "The MCP server (spring profile 'enable_mcp') must never be enabled in a production environment"
            )
        }
        val config = McpServerConfig(baseUrl = evakaEnv.frontendBaseUrlFi.trimEnd('/'))
        logger.warn {
            "MCP server is ENABLED (not for production). Server URL: ${config.resourceUrl}"
        }
        return config
    }
}

data class McpServerConfig(val baseUrl: String) {
    /**
     * OAuth 2.1 issuer identifier. Metadata is served from /.well-known/oauth-authorization-server
     */
    val issuer: String
        get() = baseUrl

    /** The MCP endpoint (Streamable HTTP transport) that AI clients connect to */
    val resourceUrl: String
        get() = "$baseUrl/api/mcp"

    val protectedResourceMetadataUrl: String
        get() = "$baseUrl/.well-known/oauth-protected-resource"

    /** The consent page in the employee frontend */
    val authorizationEndpoint: String
        get() = "$baseUrl/employee/mcp/authorize"

    val tokenEndpoint: String
        get() = "$baseUrl/api/mcp/oauth/token"

    val registrationEndpoint: String
        get() = "$baseUrl/api/mcp/oauth/register"

    val scope: String
        get() = MCP_SCOPE

    companion object {
        const val MCP_SCOPE = "evaka:mcp"
        const val MAX_VALIDITY_DAYS = 90
        const val DEFAULT_VALIDITY_DAYS = 30
        val validityOptionsDays = listOf(1, 7, 30, 90)
    }
}
