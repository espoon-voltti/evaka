// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.spring.security.api.authentication.JwtAuthentication
import fi.espoo.voltti.auth.VolttiJwtAuthenticationAdapter
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication

@TestConfiguration
class MockedJwtConfig {
    @Bean
    fun mockVolttiJwtAuthenticationProvider(): AuthenticationProvider = object : AuthenticationProvider {
        override fun authenticate(authentication: Authentication?): Authentication? {
            return (authentication as JwtAuthentication?)?.let {
                it.verify(JWT.require(Algorithm.none()).build())
            }?.let(VolttiJwtAuthenticationUUIDAdapter()::configureAuthentication)
        }

        override fun supports(authentication: Class<*>): Boolean {
            return JwtAuthentication::class.java.isAssignableFrom(authentication)
        }
    }

    @Bean
    fun jwtAuthenticationAdapter(): VolttiJwtAuthenticationAdapter = VolttiJwtAuthenticationUUIDAdapter()
}
