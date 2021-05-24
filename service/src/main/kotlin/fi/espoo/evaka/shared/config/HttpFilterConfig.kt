// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.interfaces.JWTVerifier
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.JwtToAuthenticatedUser
import fi.espoo.evaka.shared.auth.getAuthenticatedUser
import fi.espoo.voltti.auth.JwtTokenDecoder
import fi.espoo.voltti.logging.filter.BasicMdcFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.servlet.FilterChain
import javax.servlet.http.HttpFilter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class HttpFilterConfig {
    @Bean
    fun basicMdcFilter() = FilterRegistrationBean(BasicMdcFilter()).apply {
        setName("basicMdcFilter")
        urlPatterns = listOf("/*")
        order = Int.MIN_VALUE
    }

    @Bean
    fun jwtTokenParser(jwtVerifier: JWTVerifier) = FilterRegistrationBean(JwtTokenDecoder(jwtVerifier)).apply {
        setName("jwtTokenParser")
        urlPatterns = listOf("/*")
        order = -10
    }

    @Bean
    fun jwtToAuthenticatedUser() = FilterRegistrationBean(JwtToAuthenticatedUser()).apply {
        setName("jwtToAuthenticatedUser")
        urlPatterns = listOf("/*")
        order = -9
    }

    @Bean
    fun httpAccessControl(env: Environment) = FilterRegistrationBean(HttpAccessControl(env)).apply {
        setName("httpAccessControl")
        urlPatterns = listOf("/*")
        order = -8
    }

    class HttpAccessControl(env: Environment) : HttpFilter() {
        private val devApiEnabled = env.activeProfiles.contains("enable_dev_api")
        private val mockIntegrationEnabled = env.activeProfiles.contains("enable_varda_mock_integration_endpoint")

        override fun doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
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

        private fun HttpServletRequest.requiresAuthentication(): Boolean {
            if (isHealthCheck()) return false
            if (requestURI.startsWith("/public/")) return false
            if (requestURI.startsWith("/varda-dev/")) return false
            if (requestURI.startsWith("/scheduled/")) return false
            if (mockIntegrationEnabled && requestURI.startsWith("/mock-integration/")) return false
            if (devApiEnabled && requestURI.startsWith("/dev-api/")) return false
            return true
        }

        private fun HttpServletRequest.isAuthorized(user: AuthenticatedUser): Boolean {
            if (requestURI.startsWith("/system/")) return user.isSystemInternalUser
            return true
        }
    }
}

private fun HttpServletRequest.isHealthCheck() = requestURI == "/health" || requestURI == "/actuator/health"
