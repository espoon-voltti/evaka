// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.config

import fi.espoo.evaka.vtjclient.properties.XRoadProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.UrlResource
import org.springframework.ws.soap.security.support.KeyManagersFactoryBean
import org.springframework.ws.soap.security.support.KeyStoreFactoryBean

@Configuration
@Profile("production", "vtj-dev")
class KeyManagerConfig {

    @Bean
    @ConditionalOnExpression("'\${voltti.env}' == 'prod' || '\${voltti.env}' == 'staging'")
    fun keyManagers(@Qualifier("keyStore") keyStore: KeyStoreFactoryBean, xroadProps: XRoadProperties) = KeyManagersFactoryBean()
        .apply {
            setKeyStore(keyStore.`object`)
            setPassword(xroadProps.keyStore.password)
        }

    @Bean("keyStore")
    @ConditionalOnExpression("'\${voltti.env}' == 'prod' || '\${voltti.env}' == 'staging'")
    fun keyStore(xroadProps: XRoadProperties) = KeyStoreFactoryBean()
        .apply {
            val keyStore = checkNotNull(xroadProps.keyStore.location) { "Xroad security server client authentication key store location is not set" }
            setLocation(UrlResource(keyStore))
            setPassword(xroadProps.keyStore.password)
            setType(xroadProps.keyStore.type)
        }
}
