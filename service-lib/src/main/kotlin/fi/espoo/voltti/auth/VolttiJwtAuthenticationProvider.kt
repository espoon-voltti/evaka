// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.auth

import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.Verification
import com.auth0.spring.security.api.authentication.JwtAuthentication
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import java.security.interfaces.RSAPublicKey

interface VolttiJwtAuthenticationAdapter {
    fun configureVerification(verification: Verification): Verification
    fun configureAuthentication(authentication: Authentication): Authentication

    object NoopVolttiJwtAuthenticationAdapter : VolttiJwtAuthenticationAdapter {
        override fun configureVerification(verification: Verification): Verification = verification
        override fun configureAuthentication(authentication: Authentication): Authentication = authentication
    }
}

class VolttiJwtAuthenticationProvider(
    private val jwkProvider: JwkProvider,
    private val adapter: VolttiJwtAuthenticationAdapter
) : AuthenticationProvider {
    override fun supports(authentication: Class<*>): Boolean {
        return JwtAuthentication::class.java.isAssignableFrom(authentication)
    }

    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication? {
        if (!supports(authentication.javaClass)) {
            return null
        }
        try {
            val jwt = authentication as JwtAuthentication
            val jwtVerifier = createJwtVerifier(jwt)
            return adapter.configureAuthentication(jwt.verify(jwtVerifier))
        } catch (e: JWTVerificationException) {
            throw BadCredentialsException("Not a valid token", e)
        }
    }

    @Throws(AuthenticationException::class)
    private fun createJwtVerifier(authentication: JwtAuthentication): JWTVerifier {
        try {
            val builder = createVerifier(authentication.keyId)
            adapter.configureVerification(builder)
            return builder.build()
        } catch (e: JwkException) {
            throw AuthenticationServiceException("Cannot authenticate with jwt.", e)
        }
    }

    @Throws(AuthenticationException::class)
    private fun createVerifier(kid: String?): Verification {
        try {
            val jwk = jwkProvider.get(kid).publicKey as? RSAPublicKey
                ?: throw AuthenticationServiceException("No public RSA key found (kid: $kid).")
            return JWT.require(Algorithm.RSA256(jwk, null))
        } catch (e: SigningKeyNotFoundException) {
            throw AuthenticationServiceException("No JWK found for kid: $kid.")
        }
    }
}
