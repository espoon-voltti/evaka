// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient

import evaka.core.vtjclient.config.TrustManagerConfig
import evaka.core.vtjclient.config.XroadSoapClientConfig
import evaka.core.vtjclient.service.persondetails.IPersonDetailsService
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile

@TestConfiguration
@Profile("integration-test")
@Import(XroadSoapClientConfig::class, TrustManagerConfig::class)
class VtjIntegrationTestConfig {
    @Bean fun personDetailsService(): IPersonDetailsService = MockPersonDetailsService()
}
