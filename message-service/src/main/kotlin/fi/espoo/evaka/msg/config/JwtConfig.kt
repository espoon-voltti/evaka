// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import fi.espoo.voltti.auth.JwtKeys
import fi.espoo.voltti.auth.loadPublicKeys
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import java.net.URI
import java.nio.file.Paths

@Configuration
class JwtConfig {
    @Profile("production")
    @Bean
    fun rsaJwtAlgorithm(env: Environment): Algorithm {
        @Suppress("deprecation")
        val publicKeys = loadPublicKeys(URI(env.getRequiredProperty("fi.espoo.voltti.auth.jwks.default.url")))
        return Algorithm.RSA256(JwtKeys(privateKeyId = null, privateKey = null, publicKeys = publicKeys))
    }

    @Profile("local")
    @Bean
    fun devRsaJwtAlgorithm(): Algorithm {
        val publicKeys =
            loadPublicKeys(Paths.get(this.javaClass.getResource("/local-development/jwks.json").path))
        return Algorithm.RSA256(JwtKeys(privateKeyId = null, privateKey = null, publicKeys = publicKeys))
    }

    @Bean
    fun jwtVerifier(algorithm: Algorithm): JWTVerifier = JWT.require(algorithm).build()
}
