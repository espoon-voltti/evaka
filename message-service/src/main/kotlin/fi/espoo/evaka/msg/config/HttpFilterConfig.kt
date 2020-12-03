// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import com.auth0.jwt.interfaces.JWTVerifier
import fi.espoo.voltti.auth.JwtTokenDecoder
import fi.espoo.voltti.auth.getDecodedJwt
import fi.espoo.voltti.logging.filter.BasicMdcFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
    fun requireJwtToken() = FilterRegistrationBean(RequireJwtToken()).apply {
        setName("requireJwtToken")
        urlPatterns = listOf("/*")
        order = -9
    }

    class RequireJwtToken : HttpFilter() {
        override fun doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
            if (!request.isHealthCheck() && request.getDecodedJwt() == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                return
            }
            chain.doFilter(request, response)
        }
    }
}

private fun HttpServletRequest.isHealthCheck() = requestURI == "/health" || requestURI == "/actuator/health"
