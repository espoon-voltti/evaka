// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.getAuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.domain.Unauthorized
import fi.espoo.evaka.shared.utils.asArgumentResolver
import fi.espoo.evaka.shared.utils.convertFrom
import io.opentelemetry.api.trace.Tracer
import jakarta.servlet.http.HttpServletRequest
import java.time.ZonedDateTime
import java.util.UUID
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.format.FormatterRegistry
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.WebRequest.SCOPE_REQUEST
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.function.ServerRequest

/**
 * Adds support for using the following types as REST function parameters:
 * - `Database`: a request-scoped database instance
 * - `Database.Connection`: a request-scoped database connection, closed automatically at request
 *   completion (regardless of success or failure)
 * - `AuthenticatedUser`: user performing the request
 * - `ExternalId`: an external id, is automatically parsed from a string value (e.g. path variable /
 *   query parameter, depending on annotations)
 * - `Id<*>`: a type-safe identifier, which is serialized/deserialized as UUID (= string)
 */
@Configuration
class SpringMvcConfig(
    private val jdbi: Jdbi,
    private val tracer: Tracer,
    private val env: EvakaEnv,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(asArgumentResolver<AuthenticatedUser.Citizen?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver<AuthenticatedUser.Employee?>(::resolveAuthenticatedUser))
        resolvers.add(
            asArgumentResolver<AuthenticatedUser.Integration?>(::resolveAuthenticatedUser)
        )
        resolvers.add(
            asArgumentResolver<AuthenticatedUser.MobileDevice?>(::resolveAuthenticatedUser)
        )
        resolvers.add(
            asArgumentResolver<AuthenticatedUser.SystemInternalUser?>(::resolveAuthenticatedUser)
        )
        resolvers.add(asArgumentResolver<AuthenticatedUser?>(::resolveAuthenticatedUser))
        resolvers.add(asArgumentResolver { _, webRequest -> webRequest.getDatabaseInstance() })
        resolvers.add(asArgumentResolver { _, webRequest -> webRequest.getEvakaClock() })
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(convertFrom<String, ExternalId> { ExternalId.parse(it) })
        registry.addConverter(convertFrom<String, Id<*>> { Id<DatabaseTable>(UUID.fromString(it)) })
    }

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON)
    }

    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        // If the response body is a string, we want it to be converted as JSON, not directly
        // serialized as string
        converters.removeIf { it is StringHttpMessageConverter }
    }

    private fun WebRequest.getDatabaseInstance(): Database =
        getDatabase() ?: Database(jdbi, tracer).also(::setDatabase)

    private inline fun <reified T : AuthenticatedUser> resolveAuthenticatedUser(
        parameter: MethodParameter,
        webRequest: NativeWebRequest,
    ): T? {
        val user =
            webRequest.getNativeRequest(HttpServletRequest::class.java)?.getAuthenticatedUser()
                as? T
        if (user == null && !parameter.isOptional) {
            throw Unauthorized("Unauthorized request (${webRequest.getDescription(false)})")
        }
        return user
    }

    private fun WebRequest.getEvakaClock(): EvakaClock =
        if (!env.mockClock) {
            RealEvakaClock()
        } else {
            val mockTime =
                this.getHeader("EvakaMockedTime")?.let {
                    HelsinkiDateTime.from(ZonedDateTime.parse(it))
                }
            MockEvakaClock(mockTime ?: HelsinkiDateTime.now())
        }
}

private const val ATTR_DB = "evaka.database"

private fun WebRequest.getDatabase() = getAttribute(ATTR_DB, SCOPE_REQUEST) as Database?

private fun WebRequest.setDatabase(db: Database) = setAttribute(ATTR_DB, db, SCOPE_REQUEST)

inline fun <reified T : AuthenticatedUser?> ServerRequest.getAuthenticatedUser(): T =
    if (null is T) {
        servletRequest().getAuthenticatedUser() as? T ?: (null as T)
    } else {
        servletRequest().getAuthenticatedUser() as? T ?: throw Unauthorized("Unauthorized request")
    }
