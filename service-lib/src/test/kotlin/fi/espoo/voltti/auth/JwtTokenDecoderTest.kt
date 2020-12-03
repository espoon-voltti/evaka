// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Instant
import java.util.Date

class JwtTokenDecoderTest {
    private val issuer = "test"
    private val algorithm = Algorithm.none()
    private val decoder = JwtTokenDecoder(JWT.require(algorithm).build())

    private lateinit var req: MockHttpServletRequest
    private lateinit var res: MockHttpServletResponse
    private lateinit var chain: MockFilterChain

    @Before
    fun beforeEach() {
        req = MockHttpServletRequest()
        res = MockHttpServletResponse()
        chain = MockFilterChain()
    }

    @Test
    fun testValidToken() {
        val token = JWT.create().withIssuer(issuer).sign(algorithm)

        req.addHeader("Authorization", "Bearer $token")
        decoder.doFilter(req, res, chain)

        val decoded = req.getDecodedJwt()
        Assert.assertNotNull(decoded)
        Assert.assertEquals(issuer, decoded?.issuer)
    }

    @Test
    fun testInvalidToken() {
        req.addHeader("Authorization", "Bearer nope")
        decoder.doFilter(req, res, chain)

        val decoded = req.getDecodedJwt()
        Assert.assertNull(decoded)
    }

    @Test
    fun testExpiredToken() {
        val token = JWT.create().withIssuer(issuer).withExpiresAt(Date.from(Instant.EPOCH)).sign(algorithm)

        req.addHeader("Authorization", "Bearer $token")
        decoder.doFilter(req, res, chain)

        val decoded = req.getDecodedJwt()
        Assert.assertNull(decoded)
    }
}
