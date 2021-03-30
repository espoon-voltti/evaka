// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.config

import fi.espoo.evaka.vtjclient.properties.XRoadProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.UrlResource
import org.springframework.ws.soap.security.support.KeyStoreFactoryBean
import org.springframework.ws.soap.security.support.TrustManagersFactoryBean

@Configuration
@Profile("production", "vtj-dev", "integration-test")
class TrustManagerConfig {

    @Bean
    @ConditionalOnProperty(prefix = "fi.espoo.voltti.vtj.xroad", name = ["trust-store.location"])
    fun trustManagers(@Qualifier("trustStore") trustStore: KeyStoreFactoryBean) = TrustManagersFactoryBean()
        .apply { setKeyStore(trustStore.`object`) }

    @Bean("trustStore")
    @ConditionalOnProperty(prefix = "fi.espoo.voltti.vtj.xroad", name = ["trust-store.location"])
    fun trustStore(xroadProps: XRoadProperties) = KeyStoreFactoryBean()
        .apply {
            val trustStore = checkNotNull(xroadProps.trustStore.location) { "Xroad security server trust store location is not set" }
            setLocation(UrlResource(trustStore))
            setPassword(xroadProps.trustStore.password)
            setType(xroadProps.trustStore.type)
        }
}
