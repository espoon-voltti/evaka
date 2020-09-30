// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient

import fi.espoo.evaka.vtjclient.config.TrustManagerConfig
import fi.espoo.evaka.vtjclient.config.XroadSoapClientConfig
import fi.espoo.evaka.vtjclient.properties.XRoadProperties
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import redis.clients.jedis.JedisPool

@TestConfiguration
@Profile("integration-test")
@Import(XRoadProperties::class, XroadSoapClientConfig::class, TrustManagerConfig::class)
class VtjIntegrationTestConfig {

    @MockBean
    lateinit var redisPool: JedisPool

    @Bean
    fun personDetailsService(): IPersonDetailsService = MockPersonDetailsService()
}
