// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.config

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.vtjclient.mapper.VtjHenkiloMapper
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.VTJPersonDetailsService
import fi.espoo.evaka.vtjclient.service.vtjclient.VtjClientService
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
            true ->
                VTJPersonDetailsService(
                    vtjClientService = ctx.getBean(VtjClientService::class.java),
                    henkiloMapper = ctx.getBean(VtjHenkiloMapper::class.java),
                )
            false -> MockPersonDetailsService()
        }
}
