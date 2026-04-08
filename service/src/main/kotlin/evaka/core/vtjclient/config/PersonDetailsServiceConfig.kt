// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.config

import evaka.core.EvakaEnv
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.IPersonDetailsService
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import evaka.core.vtjclient.service.persondetails.VTJPersonDetailsService
import evaka.core.vtjclient.service.vtjclient.VtjClientService
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PersonDetailsServiceConfig {
    @Bean
    fun pisPersonDetailsService(
        evakaEnv: EvakaEnv,
        ctx: ApplicationContext,
    ): IPersonDetailsService =
        when (evakaEnv.vtjEnabled) {
            true -> {
                VTJPersonDetailsService(
                    vtjClientService = ctx.getBean(VtjClientService::class.java),
                    henkiloMapper = ctx.getBean(VtjHenkiloMapper::class.java),
                )
            }

            false -> {
                MockPersonDetailsService()
            }
        }
}
