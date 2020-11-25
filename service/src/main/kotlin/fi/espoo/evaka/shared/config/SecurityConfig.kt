// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.algorithms.Algorithm
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUserSpringSupport
import fi.espoo.evaka.shared.auth.JwtKeys
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.loadPrivateKey
import fi.espoo.evaka.shared.auth.loadPublicKeys
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import java.net.URI
import java.nio.file.Paths

object Roles {
    val SERVICE_WORKER = UserRole.SERVICE_WORKER
    val END_USER = UserRole.END_USER
    val UNIT_SUPERVISOR = UserRole.UNIT_SUPERVISOR
    val FINANCE_ADMIN = UserRole.FINANCE_ADMIN
    val ADMIN = UserRole.ADMIN
    val DIRECTOR = UserRole.DIRECTOR
    val STAFF = UserRole.STAFF
}

@Configuration
@Import(AuthenticatedUserSpringSupport::class)
class SecurityConfig {
    @Bean
    fun accessControlList(jdbi: Jdbi): AccessControlList = AccessControlList(jdbi)

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
}

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 42)
class PublicResourcesConfigurerAdapter : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable() // csrf handled at API gateway
            .requestMatchers()
            .antMatchers(
                "/public/**",
                "/enduser/areas",
                "/location",
                "/report/offices",
                "/scheduled/varda/update",
                "/scheduled/varda/units/update-units",
                "/mock-integration/varda/api/**",
                "/scheduled/koski/update",
                "/mock-integration/koski/api/**",
                "/mock-integration/dvv/api/**",
                "/scheduled/dvv/update"
            )
            .and()
            .authorizeRequests()
            .anyRequest().permitAll()
    }
}
