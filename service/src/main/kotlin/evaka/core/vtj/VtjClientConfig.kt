// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.EvakaEnv
import evaka.core.VtjDvvMode
import evaka.core.dvv.DvvModificationsServiceClient
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
            true -> {
                val soapClient = SoapVtjClient(ctx.getBean(XroadVtjSoapTransport::class.java))
                when (evakaEnv.vtjDvvMode) {
                    VtjDvvMode.OFF -> soapClient
                    VtjDvvMode.COMPARE ->
                        ComparingVtjClient(
                            soapClient,
                            DvvVtjClient(ctx.getBean(DvvModificationsServiceClient::class.java)),
                        )
                }
            }
            false -> DummyIdpVtjClient(evakaEnv.vtjMockUrl, jsonMapper)
        }
}
