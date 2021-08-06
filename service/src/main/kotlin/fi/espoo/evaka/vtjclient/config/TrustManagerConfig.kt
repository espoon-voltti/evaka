// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.config

import fi.espoo.evaka.VtjXroadEnv
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
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
    fun trustManagers(@Qualifier("trustStore") trustStore: ObjectProvider<KeyStoreFactoryBean>): TrustManagersFactoryBean? =
        trustStore.ifAvailable?.let {
            TrustManagersFactoryBean().apply { setKeyStore(it.`object`) }
        }

    @Bean("trustStore")
    fun trustStore(xroadEnv: VtjXroadEnv): KeyStoreFactoryBean? = xroadEnv.trustStore.location?.let { location ->
        KeyStoreFactoryBean()
            .apply {
                setLocation(UrlResource(location))
                setPassword(xroadEnv.trustStore.password.value)
                setType(xroadEnv.trustStore.type)
            }
    }
}
