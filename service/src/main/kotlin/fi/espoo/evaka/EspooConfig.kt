// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.shared.daily.DailySchedule
import fi.espoo.evaka.shared.daily.DefaultDailySchedule
import fi.espoo.evaka.shared.message.EvakaMessageProvider
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.template.EvakaTemplateProvider
import fi.espoo.evaka.shared.template.ITemplateProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment

@Configuration
@Profile("espoo_evaka")
class EspooConfig {
    @Bean
    fun invoiceIntegrationClient(env: Environment, objectMapper: ObjectMapper): InvoiceIntegrationClient =
        if (env.getProperty("fi.espoo.integration.invoice.enabled", Boolean::class.java, true))
            InvoiceIntegrationClient.Client(env, objectMapper)
        else
            InvoiceIntegrationClient.MockClient(objectMapper)

    @Bean
    fun messageProvider(): IMessageProvider = EvakaMessageProvider()

    @Bean
    fun templateProvider(): ITemplateProvider = EvakaTemplateProvider()

    @Bean
    fun dailySchedule(): DailySchedule = DefaultDailySchedule()
}
