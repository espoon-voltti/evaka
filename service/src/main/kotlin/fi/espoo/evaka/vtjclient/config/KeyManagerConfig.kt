// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.config

import fi.espoo.evaka.VtjXroadEnv
import org.springframework.beans.factory.ObjectProvider
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
    fun keyManagers(
        @Qualifier("keyStore") keyStore: ObjectProvider<KeyStoreFactoryBean>,
        xroadEnv: VtjXroadEnv,
    ): KeyManagersFactoryBean? =
        keyStore.ifAvailable?.let {
            KeyManagersFactoryBean().apply {
                setKeyStore(it.`object`)
                setPassword(xroadEnv.keyStore?.password?.value)
            }
        }

    @Bean("keyStore")
    @ConditionalOnExpression("'\${voltti.env}' == 'prod' || '\${voltti.env}' == 'staging'")
    fun keyStore(xroadEnv: VtjXroadEnv): KeyStoreFactoryBean? =
        xroadEnv.keyStore?.let { store ->
            KeyStoreFactoryBean().apply {
                setLocation(UrlResource(store.location))
                setPassword(xroadEnv.keyStore.password?.value)
                setType(xroadEnv.keyStore.type)
            }
        }
}
