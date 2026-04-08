// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo

import evaka.core.ChildDocumentArchivalEnv
import evaka.core.EvakaEnv
import evaka.core.ScheduledJobsEnv
import evaka.core.Sensitive
import evaka.core.document.archival.ArchivalIntegrationClient
import evaka.core.emailclient.EvakaEmailMessageProvider
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.holidayperiod.QuestionnaireType
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.invoicing.integration.EspooInvoiceIntegrationClient
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.invoicing.service.DefaultInvoiceGenerationLogic
import evaka.core.invoicing.service.DefaultInvoiceNumberProvider
import evaka.core.invoicing.service.EspooIncomeTypesProvider
import evaka.core.invoicing.service.EspooInvoiceProducts
import evaka.core.invoicing.service.IncomeCoefficientMultiplierProvider
import evaka.core.invoicing.service.IncomeTypesProvider
import evaka.core.invoicing.service.InvoiceNumberProvider
import evaka.core.invoicing.service.InvoiceProductProvider
import evaka.core.logging.defaultAccessLoggingValve
import evaka.core.lookup
import evaka.core.mealintegration.DefaultMealTypeMapper
import evaka.core.mealintegration.MealTypeMapper
import evaka.core.reports.patu.EspooPatuIntegrationClient
import evaka.core.reports.patu.PatuAsyncJobProcessor
import evaka.core.reports.patu.PatuReportingService
import evaka.core.shared.ArchiveProcessConfig
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.FeatureConfig
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.PasswordConstraints
import evaka.core.shared.auth.PasswordSpecification
import evaka.core.shared.config.PDFConfig
import evaka.core.shared.db.DevDataInitializer
import evaka.core.shared.message.EvakaMessageProvider
import evaka.core.shared.message.IMessageProvider
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.shared.template.EvakaTemplateProvider
import evaka.core.shared.template.ITemplateProvider
import evaka.core.titania.TitaniaEmployeeIdConverter
import evaka.instance.espoo.bi.EspooBiHttpClient
import evaka.instance.espoo.bi.EspooBiJob
import evaka.instance.espoo.invoicing.EspooIncomeCoefficientMultiplierProvider
import io.opentelemetry.api.trace.Tracer
import java.net.URI
import org.jdbi.v3.core.Jdbi
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.thymeleaf.ITemplateEngine
import tools.jackson.databind.json.JsonMapper

@Configuration
@Import(EspooAsyncJobRegistration::class)
class EspooConfig {
    @Bean fun espooEnv(env: Environment) = EspooEnv.fromEnvironment(env)

    @Bean
    fun invoiceIntegrationClient(
        env: EspooEnv,
        invoiceEnv: ObjectProvider<EspooInvoiceIntegrationEnv>,
        jsonMapper: JsonMapper,
    ): InvoiceIntegrationClient =
        when (env.invoiceIntegrationEnabled) {
            true -> EspooInvoiceIntegrationClient(invoiceEnv.getObject(), jsonMapper)
            false -> InvoiceIntegrationClient.MockClient(jsonMapper)
        }

    @Bean
    fun patuIntegrationClient(
        env: EspooEnv,
        patuEnv: ObjectProvider<EspooPatuIntegrationEnv>,
        jsonMapper: JsonMapper,
    ): EspooPatuIntegrationClient? =
        if (env.patuIntegrationEnabled) EspooPatuIntegrationClient(patuEnv.getObject(), jsonMapper)
        else null

    @Bean
    fun patuReportingService(client: EspooPatuIntegrationClient?): PatuReportingService =
        PatuReportingService(client)

    @Bean
    @Lazy
    fun espooInvoiceIntegrationEnv(env: Environment) =
        EspooInvoiceIntegrationEnv.fromEnvironment(env)

    @Bean fun espooInvoiceGenerationLogicChooser() = DefaultInvoiceGenerationLogic

    @Bean
    @Lazy
    fun espooPatuIntegrationEnv(env: Environment) = EspooPatuIntegrationEnv.fromEnvironment(env)

    @Bean
    fun espooPatuAsyncJobProcessor(
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
        patuReportingService: PatuReportingService,
    ) = PatuAsyncJobProcessor(asyncJobRunner, patuReportingService)

    @Bean
    @Profile("local")
    fun paymentIntegrationMockClient(jsonMapper: JsonMapper): PaymentIntegrationClient =
        PaymentIntegrationClient.MockClient(jsonMapper)

    @Bean
    @Profile("production")
    fun paymentIntegrationClient(): PaymentIntegrationClient =
        PaymentIntegrationClient.FailingClient()

    @Bean fun messageProvider(): IMessageProvider = EvakaMessageProvider()

    @Bean
    fun emailMessageProvider(env: EvakaEnv): IEmailMessageProvider = EvakaEmailMessageProvider(env)

    @Bean fun templateProvider(): ITemplateProvider = EvakaTemplateProvider()

    @Bean fun templateEngine(): ITemplateEngine = PDFConfig.templateEngine("espoo")

    @Bean
    fun espooScheduledJobEnv(env: Environment): ScheduledJobsEnv<EspooScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            EspooScheduledJob.entries.associateWith { it.defaultSettings },
            "espoo.job",
            env,
        )

    @Bean
    @Profile("local")
    fun devDataInitializer(jdbi: Jdbi, tracer: Tracer) = DevDataInitializer(jdbi, tracer)

    @Bean fun incomeTypesProvider(): IncomeTypesProvider = EspooIncomeTypesProvider()

    @Bean
    fun coefficientMultiplierProvider(): IncomeCoefficientMultiplierProvider =
        EspooIncomeCoefficientMultiplierProvider()

    @Bean fun invoiceProductsProvider(): InvoiceProductProvider = EspooInvoiceProducts.Provider()

    @Bean
    fun invoiceNumberProvider(): InvoiceNumberProvider = DefaultInvoiceNumberProvider(5000000000)

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        object : TitaniaEmployeeIdConverter {
            override fun fromTitania(employeeId: String): String = employeeId.trimStart('0')
        }

    @Bean
    fun espooAsyncJobRunner(
        jdbi: Jdbi,
        tracer: Tracer,
        env: Environment,
    ): AsyncJobRunner<EspooAsyncJob> =
        AsyncJobRunner(EspooAsyncJob::class, listOf(EspooAsyncJob.pool), jdbi, tracer)

    @Bean @Lazy fun espooBiEnv(env: Environment) = EspooBiEnv.fromEnvironment(env)

    @Bean
    fun espooBiJob(env: EspooEnv, biEnv: ObjectProvider<EspooBiEnv>): EspooBiJob? =
        when (env.biIntegrationEnabled) {
            true -> EspooBiJob(EspooBiHttpClient(biEnv.getObject()))
            false -> null
        }

    @Bean
    fun linkityEnv(espooEnv: EspooEnv, env: Environment): LinkityEnv? =
        when (espooEnv.linkityEnabled) {
            true -> LinkityEnv.fromEnvironment(env)
            false -> null
        }

    @Bean
    fun featureConfig(): FeatureConfig =
        FeatureConfig(
            valueDecisionCapacityFactorEnabled = false,
            citizenReservationThresholdHours = 150,
            freeAbsenceGivesADailyRefund = true,
            alwaysUseDaycareFinanceDecisionHandler = false, // Doesn't affect Espoo
            paymentNumberSeriesStart = 1, // Payments are not yet in use in Espoo
            unplannedAbsencesAreContractSurplusDays = false, // Doesn't affect Espoo
            maxContractDaySurplusThreshold = null, // Doesn't affect Espoo
            useContractDaysAsDailyFeeDivisor = true, // Doesn't affect Espoo
            requestedStartUpperLimit = 14,
            postOffice = "ESPOO",
            municipalMessageAccountName = "Espoon kaupunki - Esbo stad - City of Espoo",
            serviceWorkerMessageAccountName =
                "Varhaiskasvatuksen palveluohjaus - Småbarnspedagogikens servicehandledning - Early childhood education service guidance",
            financeMessageAccountName =
                "Varhaiskasvatuksen asiakasmaksut - Småbarnspedagogikens avgifter - Early childhood education fees",
            applyPlacementUnitFromDecision = false,
            preferredStartRelativeApplicationDueDate = false,
            fiveYearsOldDaycareEnabled = true,
            freeJulyStartOnSeptember = true,
            archiveMetadataOrganization = "Espoon kaupungin esiopetus ja varhaiskasvatus",
            archiveMetadataConfigs = { type: ArchiveProcessType, year: Int ->
                when (type) {
                    ArchiveProcessType.APPLICATION_DAYCARE -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01",
                            archiveDurationMonths = 10 * 12,
                        )
                    }

                    ArchiveProcessType.APPLICATION_PRESCHOOL -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = if (year >= 2026) "12.06.02" else "12.06.01",
                            archiveDurationMonths = 10 * 12,
                        )
                    }

                    ArchiveProcessType.APPLICATION_CLUB -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01",
                            archiveDurationMonths = 10 * 12,
                        )
                    }

                    ArchiveProcessType.FEE_DECISION -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = if (year >= 2026) "02.09.01" else "12.06.07",
                            archiveDurationMonths = 10 * 12,
                        )
                    }

                    ArchiveProcessType.VOUCHER_VALUE_DECISION -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = if (year >= 2026) "02.09.01" else "12.06.08",
                            archiveDurationMonths = 10 * 12,
                        )
                    }
                }
            },
            holidayQuestionnaireType = QuestionnaireType.FIXED_PERIOD,
        )

    @Bean
    fun tomcatCustomizer(env: Environment) =
        WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
            it.addContextValves(defaultAccessLoggingValve(env))
            it.setDisableMBeanRegistry(false)
        }

    @Bean fun actionRuleMapping(): ActionRuleMapping = EspooActionRuleMapping()

    @Bean
    fun espooScheduledJobs(
        patuReportingService: PatuReportingService,
        espooAsyncJobRunner: AsyncJobRunner<EspooAsyncJob>,
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
        env: ScheduledJobsEnv<EspooScheduledJob>,
        linkityEnv: LinkityEnv?,
        jsonMapper: JsonMapper,
        childDocumentArchivalEnv: ChildDocumentArchivalEnv,
        espooBiJob: EspooBiJob?,
        evakaEnv: EvakaEnv,
    ): EspooScheduledJobs =
        EspooScheduledJobs(
            patuReportingService,
            espooAsyncJobRunner,
            env,
            linkityEnv,
            jsonMapper,
            espooBiJob,
            evakaEnv,
        )

    @Bean fun espooMealTypeMapper(): MealTypeMapper = DefaultMealTypeMapper

    @Bean
    fun espooPasswordSpecification(): PasswordSpecification =
        DefaultPasswordSpecification(
            PasswordConstraints.UNCONSTRAINED.copy(
                minLength = 15,
                minLowers = 1,
                minUppers = 1,
                minDigits = 1,
                minSymbols = 1,
            )
        )

    @Bean
    fun archivalIntegrationClient(): ArchivalIntegrationClient =
        ArchivalIntegrationClient.FailingClient()
}

data class EspooEnv(
    val invoiceIntegrationEnabled: Boolean,
    val patuIntegrationEnabled: Boolean,
    val biIntegrationEnabled: Boolean,
    val linkityEnabled: Boolean,
) {
    companion object {
        fun fromEnvironment(env: Environment): EspooEnv =
            EspooEnv(
                invoiceIntegrationEnabled =
                    env.lookup(
                        "espoo.integration.invoice.enabled",
                        "fi.espoo.integration.invoice.enabled",
                    ) ?: true,
                patuIntegrationEnabled = env.lookup("espoo.integration.patu.enabled") ?: false,
                biIntegrationEnabled = env.lookup("espoo.integration.bi.enabled") ?: false,
                linkityEnabled = env.lookup("espoo.integration.linkity.enabled") ?: false,
            )
    }
}

data class EspooInvoiceIntegrationEnv(
    val url: String,
    val username: String,
    val password: Sensitive<String>,
    val sendCodebtor: Boolean,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            EspooInvoiceIntegrationEnv(
                url =
                    env.lookup("espoo.integration.invoice.url", "fi.espoo.integration.invoice.url"),
                username =
                    env.lookup(
                        "espoo.integration.invoice.username",
                        "fi.espoo.integration.invoice.username",
                    ),
                password =
                    Sensitive(
                        env.lookup(
                            "espoo.integration.invoice.password",
                            "fi.espoo.integration.invoice.password",
                        )
                    ),
                sendCodebtor = env.lookup("espoo.integration.invoice.send_codebtor") ?: false,
            )
    }
}

data class EspooBiEnv(val url: String, val username: String, val password: Sensitive<String>) {
    companion object {
        fun fromEnvironment(env: Environment) =
            EspooBiEnv(
                url = env.lookup("espoo.integration.bi.url"),
                username = env.lookup("espoo.integration.bi.username"),
                password = Sensitive(env.lookup("espoo.integration.bi.password")),
            )
    }
}

data class EspooPatuIntegrationEnv(
    val url: String,
    val username: String,
    val password: Sensitive<String>,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            EspooPatuIntegrationEnv(
                url = env.lookup("fi.espoo.integration.patu.url"),
                username = env.lookup("fi.espoo.integration.patu.username"),
                password = Sensitive(env.lookup("fi.espoo.integration.patu.password")),
            )
    }
}

data class LinkityEnv(val url: URI, val apikey: Sensitive<String>) {
    companion object {
        fun fromEnvironment(env: Environment) =
            LinkityEnv(
                url = URI.create(env.lookup("espoo.integration.linkity.url")),
                apikey = Sensitive(env.lookup("espoo.integration.linkity.apikey")),
            )
    }
}
