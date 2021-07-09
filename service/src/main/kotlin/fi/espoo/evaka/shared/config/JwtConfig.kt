// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import fi.espoo.evaka.JwtEnv
import fi.espoo.voltti.auth.JwtKeys
import fi.espoo.voltti.auth.loadPrivateKey
import fi.espoo.voltti.auth.loadPublicKeys
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class JwtConfig {
    @Profile("production")
    @Bean
    fun rsaJwtAlgorithm(env: JwtEnv): Algorithm {
        @Suppress("deprecation")
        val privateKey = loadPrivateKey(env.privateKeyUrl)
        @Suppress("deprecation")
        val publicKeys = loadPublicKeys(env.publicKeysUrl)
        return Algorithm.RSA256(JwtKeys("evaka-service", privateKey, publicKeys))
    }

    @Profile("local")
    @Bean
    fun devRsaJwtAlgorithm(): Algorithm {
        val privateKey = this.javaClass.getResourceAsStream("/local-development/jwk_private_key.pem").use { loadPrivateKey(it) }
        val publicKeys = this.javaClass.getResourceAsStream("/local-development/jwks.json").use { loadPublicKeys(it) }
        return Algorithm.RSA256(JwtKeys("evaka-service", privateKey, publicKeys))
    }

    @Bean
    fun jwtVerifier(algorithm: Algorithm): JWTVerifier = JWT.require(algorithm).build()
}
