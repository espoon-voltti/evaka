// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class Application {
    @Bean
    fun invoiceIntegrationClient(env: Environment, objectMapper: ObjectMapper): InvoiceIntegrationClient =
        if (env.getProperty("fi.espoo.integration.invoice.enabled", Boolean::class.java, true))
            InvoiceIntegrationClient.Client(env, objectMapper)
        else
            InvoiceIntegrationClient.MockClient(objectMapper)
}
