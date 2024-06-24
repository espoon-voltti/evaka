// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.auth

import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging

// RFC6750 - The OAuth 2.0 Authorization Framework: Bearer Token Usage
// Authorization Request Header Field
// https://tools.ietf.org/html/rfc6750#section-2.1
private fun HttpServletRequest.getBearerToken(): String? = getHeader("Authorization")?.substringAfter("Bearer ", missingDelimiterValue = "")

private const val ATTR_JWT = "evaka.jwt"

fun HttpServletRequest.getDecodedJwt(): DecodedJWT? = getAttribute(ATTR_JWT) as DecodedJWT?

fun HttpServletRequest.setDecodedJwt(jwt: DecodedJWT) = setAttribute(ATTR_JWT, jwt)

class JwtTokenDecoder(
    private val jwtVerifier: JWTVerifier
) : HttpFilter() {
    private val logger = KotlinLogging.logger {}

    override fun doFilter(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        try {
            request
                .getBearerToken()
                ?.takeIf { it.isNotEmpty() }
                ?.let { request.setDecodedJwt(jwtVerifier.verify(it)) }
        } catch (e: JWTVerificationException) {
            logger.error(e) { "JWT token verification failed" }
        }
        chain.doFilter(request, response)
    }
}
