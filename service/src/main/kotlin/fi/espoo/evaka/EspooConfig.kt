// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.emailclient.EvakaEmailMessageProvider
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.invoicing.integration.EspooInvoiceIntegrationClient
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.invoicing.service.EspooIncomeTypesProvider
import fi.espoo.evaka.invoicing.service.EspooInvoiceProducts
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.invoicing.service.InvoiceProductProvider
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.db.DevDataInitializer
import fi.espoo.evaka.shared.job.DefaultJobSchedule
import fi.espoo.evaka.shared.job.JobSchedule
import fi.espoo.evaka.shared.message.EvakaMessageProvider
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.template.EvakaTemplateProvider
import fi.espoo.evaka.shared.template.ITemplateProvider
import org.jdbi.v3.core.Jdbi
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment

@Configuration
@Profile("espoo_evaka")
class EspooConfig {
    @Bean
    fun espooEnv(env: Environment) = EspooEnv.fromEnvironment(env)

    @Bean
    fun invoiceIntegrationClient(env: EspooEnv, invoiceEnv: ObjectProvider<EspooInvoiceIntegrationEnv>, jsonMapper: JsonMapper): InvoiceIntegrationClient =
        when (env.invoiceIntegrationEnabled) {
            true -> EspooInvoiceIntegrationClient(invoiceEnv.getObject(), jsonMapper)
            false -> InvoiceIntegrationClient.MockClient(jsonMapper)
        }

    @Bean
    @Lazy
    fun espooInvoiceIntegrationEnv(env: Environment) = EspooInvoiceIntegrationEnv.fromEnvironment(env)

    @Bean
    fun messageProvider(): IMessageProvider = EvakaMessageProvider()

    @Bean
    fun emailMessageProvider(): IEmailMessageProvider = EvakaEmailMessageProvider()

    @Bean
    fun templateProvider(): ITemplateProvider = EvakaTemplateProvider()

    @Bean
    fun jobSchedule(env: ScheduledJobsEnv): JobSchedule = DefaultJobSchedule(env)

    @Bean
    @Profile("local")
    fun devDataInitializer(jdbi: Jdbi) = DevDataInitializer(jdbi)

    @Bean
    fun incomeTypesProvider(): IncomeTypesProvider = EspooIncomeTypesProvider()

    @Bean
    fun invoiceProductsProvider(): InvoiceProductProvider = EspooInvoiceProducts.Provider()

    @Bean
    fun featureConfig(): FeatureConfig = FeatureConfig(
        valueDecisionCapacityFactorEnabled = false,
        daycareApplicationServiceNeedOptionsEnabled = false,
        citizenReservationThresholdHours = 150,
        dailyFeeDivisorOperationalDaysOverride = null,
    )

    @Bean
    fun tomcatCustomizer() = WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
        it.setDisableMBeanRegistry(false)
    }
}

data class EspooEnv(val invoiceIntegrationEnabled: Boolean) {
    companion object {
        fun fromEnvironment(env: Environment): EspooEnv = EspooEnv(
            invoiceIntegrationEnabled = env.lookup("espoo.integration.invoice.enabled", "fi.espoo.integration.invoice.enabled") ?: true
        )
    }
}

data class EspooInvoiceIntegrationEnv(
    val url: String,
    val username: String,
    val password: Sensitive<String>,
    val sendCodebtor: Boolean
) {
    companion object {
        fun fromEnvironment(env: Environment) = EspooInvoiceIntegrationEnv(
            url = env.lookup("espoo.integration.invoice.url", "fi.espoo.integration.invoice.url"),
            username = env.lookup("espoo.integration.invoice.username", "fi.espoo.integration.invoice.username"),
            password = Sensitive(env.lookup("espoo.integration.invoice.password", "fi.espoo.integration.invoice.password")),
            sendCodebtor = env.lookup("espoo.integration.invoice.send_codebtor") ?: false
        )
    }
}
