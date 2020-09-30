// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import fi.espoo.evaka.msg.service.sfi.DefaultSfiErrorResponseHandler
import fi.espoo.evaka.msg.service.sfi.IgnoreSpecificErrorsHandler
import fi.espoo.evaka.msg.service.sfi.SfiErrorResponseHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("production", "sfi-dev")
@Configuration
class SfiErrorResponseHandlerConfig {

    @Bean
    @Profile("sfi-dev")
    fun defaultDevResponseHandler(): SfiErrorResponseHandler = DefaultSfiErrorResponseHandler()

    @Bean
    @Profile("production")
    fun ignoreSpecificErrorsHandler(): SfiErrorResponseHandler = IgnoreSpecificErrorsHandler()
}
