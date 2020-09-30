// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.auth

import com.auth0.jwk.GuavaCachedJwkProvider
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.UrlJwkProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URL

@Configuration
class VolttiJwtSecurityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("fi.espoo.voltti.auth.jwks.default.url")
    fun jwkProvider(@Value("\${fi.espoo.voltti.auth.jwks.default.url}") url: String): JwkProvider {
        return GuavaCachedJwkProvider(UrlJwkProvider(URL(url)))
    }

    @Bean
    @ConditionalOnMissingBean
    fun volttiJwtAuthenticationAdapter(): VolttiJwtAuthenticationAdapter {
        return VolttiJwtAuthenticationAdapter.NoopVolttiJwtAuthenticationAdapter
    }

    @Bean
    @Autowired
    fun volttiJwtAuthenticationProvider(
        jwkProvider: JwkProvider,
        adapter: VolttiJwtAuthenticationAdapter
    ): VolttiJwtAuthenticationProvider {
        return VolttiJwtAuthenticationProvider(jwkProvider, adapter)
    }
}
