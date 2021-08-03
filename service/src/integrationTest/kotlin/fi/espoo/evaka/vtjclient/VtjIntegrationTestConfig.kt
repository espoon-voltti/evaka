// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient

import fi.espoo.evaka.vtjclient.config.TrustManagerConfig
import fi.espoo.evaka.vtjclient.config.XroadSoapClientConfig
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile

@TestConfiguration
@Profile("integration-test")
@Import(XroadSoapClientConfig::class, TrustManagerConfig::class)
class VtjIntegrationTestConfig {
    @Bean
    fun personDetailsService(): IPersonDetailsService = MockPersonDetailsService()
}
