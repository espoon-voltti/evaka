// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import fi.espoo.voltti.auth.JwtKeys
import fi.espoo.voltti.auth.loadPrivateKey
import fi.espoo.voltti.auth.loadPublicKeys
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import java.net.URI
import java.nio.file.Paths

@Configuration
class JwtConfig {
    @Profile("production")
    @Bean
    fun rsaJwtAlgorithm(env: Environment): Algorithm {
        // FIXME: Hack to support two different ways of formatting the property name. Should be renamed when possible.
        val privateKeyFile = env["fi.espoo.voltti.auth.jwt.provider.private.key.file"]
            ?: env["fi.espoo.voltti.auth.jwt.provider.privateKeyFile"]
            ?: error("Missing JWT private key file configuration")
        @Suppress("deprecation")
        val privateKey = loadPrivateKey(URI(privateKeyFile))
        @Suppress("deprecation")
        val publicKeys = loadPublicKeys(URI(env.getRequiredProperty("fi.espoo.voltti.auth.jwks.default.url")))
        return Algorithm.RSA256(JwtKeys("evaka-service", privateKey, publicKeys))
    }

    @Profile("local")
    @Bean
    fun devRsaJwtAlgorithm(): Algorithm {
        val privateKey =
            loadPrivateKey(Paths.get(SecurityConfig::class.java.getResource("/local-development/jwk_private_key.pem").path))
        val publicKeys =
            loadPublicKeys(Paths.get(SecurityConfig::class.java.getResource("/local-development/jwks.json").path))
        return Algorithm.RSA256(JwtKeys("evaka-service", privateKey, publicKeys))
    }

    @Bean
    fun jwtVerifier(algorithm: Algorithm): JWTVerifier = JWT.require(algorithm).build()
}
