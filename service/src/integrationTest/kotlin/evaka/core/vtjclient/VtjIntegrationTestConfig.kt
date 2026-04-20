// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient

import evaka.core.VtjXroadEnv
import evaka.core.vtjclient.config.XroadSoapClientConfig
import evaka.core.vtjclient.config.httpsMessageSender
import evaka.core.vtjclient.service.persondetails.IPersonDetailsService
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.ws.transport.WebServiceMessageSender

@TestConfiguration
@Profile("integration-test")
@Import(XroadSoapClientConfig::class)
class VtjIntegrationTestConfig {
    @Bean fun personDetailsService(): IPersonDetailsService = MockPersonDetailsService()

    @Bean
    fun localWebServiceMessageSender(xroadEnv: VtjXroadEnv): WebServiceMessageSender =
        httpsMessageSender(xroadEnv.trustStore, null)
}
