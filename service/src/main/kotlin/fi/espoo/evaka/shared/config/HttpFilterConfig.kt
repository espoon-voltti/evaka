// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.interfaces.JWTVerifier
import fi.espoo.evaka.shared.Tracing
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.RequestToAuthenticatedUser
import fi.espoo.evaka.shared.auth.getAuthenticatedUser
import fi.espoo.evaka.shared.randomTracingId
import fi.espoo.voltti.auth.JwtTokenDecoder
import fi.espoo.voltti.logging.MdcKey
import io.opentracing.Tracer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class HttpFilterConfig {
    @Bean
    fun basicMdcFilter(tracer: Tracer) =
        FilterRegistrationBean(BasicMdcFilter(tracer)).apply {
            setName("basicMdcFilter")
            urlPatterns = listOf("/*")
            order = Int.MIN_VALUE
        }

    @Bean
    fun jwtTokenParser(jwtVerifier: JWTVerifier) =
        FilterRegistrationBean(JwtTokenDecoder(jwtVerifier)).apply {
            setName("jwtTokenParser")
            urlPatterns = listOf("/*")
            order = -10
        }

    @Bean
    fun requestToAuthenticatedUser(tracer: Tracer) =
        FilterRegistrationBean(RequestToAuthenticatedUser(tracer)).apply {
            setName("jwtToAuthenticatedUser")
            urlPatterns = listOf("/*")
            order = -9
        }

    @Bean
    fun httpAccessControl(env: Environment) =
        FilterRegistrationBean(HttpAccessControl(env)).apply {
            setName("httpAccessControl")
            urlPatterns = listOf("/*")
            order = -8
        }

    class HttpAccessControl(
        env: Environment
    ) : HttpFilter() {
        private val devApiEnabled = env.activeProfiles.contains("enable_dev_api")
        private val mockIntegrationEnabled =
            env.activeProfiles.contains("enable_varda_mock_integration_endpoint")

        override fun doFilter(
            request: HttpServletRequest,
            response: HttpServletResponse,
            chain: FilterChain
        ) {
            if (request.requiresAuthentication()) {
                val user = request.getAuthenticatedUser()
                if (user == null) {
                    return response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                } else if (!request.isAuthorized(user)) {
                    return response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")
                }
            }
            chain.doFilter(request, response)
        }

        private fun HttpServletRequest.requiresAuthentication(): Boolean =
            when {
                isHealthCheck() -> false
                requestURI.startsWith("/citizen/public/") -> false
                requestURI.startsWith("/employee/public/") -> false
                requestURI.startsWith("/employee-mobile/public/") -> false
                requestURI.startsWith("/public/") -> false
                mockIntegrationEnabled && requestURI.startsWith("/mock-integration/") -> false
                devApiEnabled && requestURI.startsWith("/dev-api/") -> false
                else -> true
            }

        private fun HttpServletRequest.isAuthorized(user: AuthenticatedUser): Boolean =
            when {
                requestURI.startsWith("/system/") -> user is AuthenticatedUser.SystemInternalUser
                requestURI.startsWith("/citizen/") -> user is AuthenticatedUser.Citizen
                requestURI.startsWith("/employee/") -> user is AuthenticatedUser.Employee
                requestURI.startsWith("/employee-mobile/") -> user is AuthenticatedUser.MobileDevice
                requestURI.startsWith("/integration/") -> user is AuthenticatedUser.Integration
                else -> true
            }
    }

    class BasicMdcFilter(
        private val tracer: Tracer
    ) : HttpFilter() {
        override fun doFilter(
            request: HttpServletRequest,
            response: HttpServletResponse,
            chain: FilterChain
        ) {
            MdcKey.HTTP_METHOD.set(request.method)
            MdcKey.PATH.set(request.requestURI)
            MdcKey.QUERY_STRING.set(request.queryString ?: "")
            val (traceId, spanId) =
                request.getHeader("x-request-id")?.let { Pair(it, randomTracingId()) }
                    ?: randomTracingId().let { Pair(it, it) }
            MdcKey.TRACE_ID.set(traceId)
            tracer.activeSpan()?.setTag(Tracing.evakaTraceId, traceId)
            MdcKey.SPAN_ID.set(spanId)
            MdcKey.REQ_IP.set(request.getHeader("x-real-ip") ?: request.remoteAddr)
            try {
                chain.doFilter(request, response)
            } finally {
                MdcKey.REQ_IP.unset()
                MdcKey.SPAN_ID.unset()
                MdcKey.TRACE_ID.unset()
                MdcKey.QUERY_STRING.unset()
                MdcKey.PATH.unset()
                MdcKey.HTTP_METHOD.unset()
            }
        }
    }
}

private fun HttpServletRequest.isHealthCheck() = requestURI == "/health" || requestURI == "/actuator/health"
