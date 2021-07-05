// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.getAuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Unauthorized
import fi.espoo.evaka.shared.utils.asArgumentResolver
import fi.espoo.evaka.shared.utils.convertFromString
import fi.espoo.evaka.shared.utils.runAfterCompletion
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.format.FormatterRegistry
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.WebRequest.SCOPE_REQUEST
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.servlet.http.HttpServletRequest

/**
 * Adds support for using the following types as REST function parameters:
 *
 * - `Database`: a request-scoped database instance
 * - `Database.Connection`: a request-scoped database connection, closed automatically at request completion (regardless of success or failure)
 * - `AuthenticatedUser`: user performing the request
 * - `ExternalId`: an external id, is automatically parsed from a string value (e.g. path variable / query parameter, depending on annotations)
 */
@Configuration
class SpringMvcConfig(private val jdbi: Jdbi) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(asArgumentResolver<AuthenticatedUser.Citizen?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver<AuthenticatedUser.Employee?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver<AuthenticatedUser.MobileDevice?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver<AuthenticatedUser.SystemInternalUser?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver<AuthenticatedUser.WeakCitizen?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver<AuthenticatedUser?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver { _, webRequest -> webRequest.getDatabaseInstance() })
        resolvers.add(asArgumentResolver { _, webRequest -> webRequest.getOrOpenConnection() })
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addWebRequestInterceptor(runAfterCompletion { webRequest, _ -> webRequest.getConnection()?.close() })
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(convertFromString { ExternalId.parse(it) })
    }

    private fun WebRequest.getOrOpenConnection(): Database.Connection = getConnection()
        ?: getDatabaseInstance().connect().also(::setConnection)

    private fun WebRequest.getDatabaseInstance(): Database = getDatabase()
        ?: Database(jdbi).also(::setDatabase)

    private inline fun <reified T : AuthenticatedUser> resolveAuthenticatedUser(parameter: MethodParameter, webRequest: NativeWebRequest): T? {
        val user = webRequest.getNativeRequest(HttpServletRequest::class.java)?.getAuthenticatedUser() as? T
        if (user == null && !parameter.isOptional) {
            throw Unauthorized("Unauthorized request (${webRequest.getDescription(false)})")
        }
        return user
    }
}

private const val ATTR_DB = "evaka.database"

private fun WebRequest.getDatabase() = getAttribute(ATTR_DB, SCOPE_REQUEST) as Database?
private fun WebRequest.setDatabase(db: Database) = setAttribute(ATTR_DB, db, SCOPE_REQUEST)

private const val ATTR_DB_CONNECTION = "evaka.database.connection"

private fun WebRequest.getConnection() = getAttribute(ATTR_DB_CONNECTION, SCOPE_REQUEST) as Database.Connection?
private fun WebRequest.setConnection(db: Database.Connection) = setAttribute(ATTR_DB_CONNECTION, db, SCOPE_REQUEST)
