// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import fi.espoo.evaka.msg.service.sfi.ISfiClientService
import fi.espoo.evaka.msg.service.sfi.MockClientService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SfiIntegrationTestConfig {

    @Bean
    fun sfiClientService(): ISfiClientService = MockClientService()
}
