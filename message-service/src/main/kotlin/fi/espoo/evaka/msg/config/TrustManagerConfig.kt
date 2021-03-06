// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import fi.espoo.evaka.msg.properties.SfiSoapProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.UrlResource
import org.springframework.ws.soap.security.support.KeyStoreFactoryBean
import org.springframework.ws.soap.security.support.TrustManagersFactoryBean

@Configuration
@Profile("production", "sfi-dev")
class TrustManagerConfig {

    @Bean
    fun trustManagers(@Qualifier("trustStore") trustStore: KeyStoreFactoryBean) = TrustManagersFactoryBean()
        .apply { setKeyStore(trustStore.`object`) }

    @Bean("trustStore")
    fun trustStore(sfiProps: SfiSoapProperties) = KeyStoreFactoryBean()
        .apply {
            val trustStore = checkNotNull(sfiProps.trustStore.location) {
                "SFI messages API " +
                    "trust store location is not set"
            }
            setLocation(UrlResource(trustStore))
            setPassword(sfiProps.trustStore.password)
            setType(sfiProps.trustStore.type)
        }
}
