// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.config

import evaka.core.EvakaEnv
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.DummyIdpVtjClient
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
import evaka.core.vtjclient.service.persondetails.VtjClient
import evaka.core.vtjclient.service.vtjclient.XroadVtjSoapTransport
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VtjClientConfig {
    @Bean
    fun vtjClient(
        evakaEnv: EvakaEnv,
        jsonMapper: tools.jackson.databind.json.JsonMapper,
        ctx: ApplicationContext,
    ): VtjClient =
        when (evakaEnv.vtjEnabled) {
            true ->
                SoapVtjClient(
                    vtjSoapTransport = ctx.getBean(XroadVtjSoapTransport::class.java),
                    henkiloMapper = ctx.getBean(VtjHenkiloMapper::class.java),
                )
            false -> DummyIdpVtjClient(evakaEnv.vtjMockUrl, jsonMapper)
        }
}
