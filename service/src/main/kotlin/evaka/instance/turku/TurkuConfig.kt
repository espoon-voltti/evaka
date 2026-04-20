// SPDX-FileCopyrightText: 2023-2025 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku

import com.jcraft.jsch.JSch
import evaka.core.ScheduledJobsEnv
import evaka.core.VtjXroadEnv
import evaka.core.application.ApplicationStatus
import evaka.core.document.archival.ArchivalIntegrationClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.invoicing.service.DefaultInvoiceGenerationLogic
import evaka.core.invoicing.service.DefaultInvoiceNumberProvider
import evaka.core.invoicing.service.IncomeCoefficientMultiplierProvider
import evaka.core.invoicing.service.IncomeTypesProvider
import evaka.core.invoicing.service.InvoiceNumberProvider
import evaka.core.invoicing.service.InvoiceProductProvider
import evaka.core.logging.defaultAccessLoggingValve
import evaka.core.lookup
import evaka.core.mealintegration.DefaultMealTypeMapper
import evaka.core.mealintegration.MealTypeMapper
import evaka.core.shared.ArchiveProcessConfig
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.FeatureConfig
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.PasswordConstraints
import evaka.core.shared.auth.PasswordSpecification
import evaka.core.shared.config.pdfTemplateEngine
import evaka.core.shared.message.IMessageProvider
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.shared.template.ITemplateProvider
import evaka.core.titania.TitaniaEmployeeIdConverter
import evaka.core.vtjclient.config.httpsMessageSender
import evaka.instance.espoo.DefaultPasswordSpecification
import evaka.instance.turku.database.DevDataInitializer
import evaka.instance.turku.dw.DwExportClient
import evaka.instance.turku.dw.DwExportJob
import evaka.instance.turku.dw.FileDWExportClient
import evaka.instance.turku.invoice.config.TurkuIncomeCoefficientMultiplierProvider
import evaka.instance.turku.invoice.config.TurkuIncomeTypesProvider
import evaka.instance.turku.invoice.config.TurkuInvoiceProductProvider
import evaka.instance.turku.invoice.service.SapInvoiceGenerator
import evaka.instance.turku.invoice.service.SftpConnector
import evaka.instance.turku.invoice.service.SftpSender
import evaka.instance.turku.invoice.service.TurkuInvoiceClient
import evaka.instance.turku.payment.service.SapPaymentGenerator
import evaka.instance.turku.payment.service.TurkuPaymentIntegrationClient
import evaka.instance.turku.security.TurkuActionRuleMapping
import io.opentelemetry.api.trace.Tracer
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.ws.transport.WebServiceMessageSender
import org.thymeleaf.ITemplateEngine
import software.amazon.awssdk.services.s3.S3Client

@Configuration
@Import(TurkuAsyncJobRegistration::class)
class TurkuConfig {
    @Bean fun turkuEnv(env: Environment): TurkuEnv = TurkuEnv.fromEnvironment(env)

    @Bean
    fun featureConfig(env: Environment): FeatureConfig =
        FeatureConfig(
            valueDecisionCapacityFactorEnabled = false,
            // Mon 12:00
            citizenReservationThresholdHours = 6 * 24 + 12,
            alwaysUseDaycareFinanceDecisionHandler = true,
            freeAbsenceGivesADailyRefund = true,
            paymentNumberSeriesStart = 1,
            unplannedAbsencesAreContractSurplusDays = false,
            maxContractDaySurplusThreshold = 13,
            useContractDaysAsDailyFeeDivisor = false,
            requestedStartUpperLimit = 14,
            preferredStartRelativeApplicationDueDate = true,
            postOffice = "TURKU",
            municipalMessageAccountName = "Turun kaupunki",
            serviceWorkerMessageAccountName = "Turun kaupunki",
            applyPlacementUnitFromDecision = false,
            skipGuardianPreschoolDecisionApproval = true,
            fiveYearsOldDaycareEnabled =
                env.lookup("evaka.five_years_old_daycare.enabled") ?: false,
            financeMessageAccountName =
                "Varhaiskasvatuksen asiakasmaksut - Småbarnspedagogikens avgifter - Early childhood education fees",
            archiveMetadataOrganization = "Turun kaupungin varhaiskasvatus",
            archiveMetadataConfigs = { type: ArchiveProcessType, year: Int ->
                when (type) {
                    ArchiveProcessType.APPLICATION_DAYCARE -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01.02",
                            archiveDurationMonths = 12 * 12,
                        )
                    }

                    ArchiveProcessType.APPLICATION_PRESCHOOL -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01.02",
                            archiveDurationMonths = 12 * 12,
                        )
                    }

                    ArchiveProcessType.APPLICATION_CLUB -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01.02",
                            archiveDurationMonths = 12 * 12,
                        )
                    }

                    ArchiveProcessType.FEE_DECISION -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01.06",
                            archiveDurationMonths = 144,
                        )
                    }

                    ArchiveProcessType.VOUCHER_VALUE_DECISION -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01.06",
                            archiveDurationMonths = 144,
                        )
                    }
                }
            },
            placementToolApplicationStatus = ApplicationStatus.WAITING_DECISION,
        )

    @Bean
    @Profile("local")
    fun devDataInitializer(jdbi: Jdbi): DevDataInitializer = DevDataInitializer(jdbi)

    @Bean fun actionRuleMapping(): ActionRuleMapping = TurkuActionRuleMapping()

    @Bean
    fun messageProvider(): IMessageProvider {
        val messageSource = YamlMessageSource(ClassPathResource("turku/messages.yaml"))
        return TurkuMessageProvider(messageSource)
    }

    @Bean fun emailMessageProvider(): IEmailMessageProvider = TurkuEmailMessageProvider()

    @Bean fun templateProvider(): ITemplateProvider = TurkuTemplateProvider()

    @Bean fun invoiceGenerationLogicChooser() = DefaultInvoiceGenerationLogic

    @Bean fun templateEngine(): ITemplateEngine = pdfTemplateEngine("turku")

    @Bean
    fun invoiceIntegrationClient(
        properties: TurkuEnv,
        invoiceGenerator: SapInvoiceGenerator,
        sftpConnector: SftpConnector,
    ): InvoiceIntegrationClient {
        val sftpSender = SftpSender(properties.sapInvoicing, sftpConnector)
        return TurkuInvoiceClient(sftpSender, invoiceGenerator)
    }

    @Bean fun sftpConnector(): SftpConnector = SftpConnector(JSch())

    @Bean fun incomeTypesProvider(): IncomeTypesProvider = TurkuIncomeTypesProvider()

    @Bean
    fun incomeCoefficientMultiplierProvider(): IncomeCoefficientMultiplierProvider =
        TurkuIncomeCoefficientMultiplierProvider()

    @Bean fun invoiceProductProvider(): InvoiceProductProvider = TurkuInvoiceProductProvider()

    @Bean fun invoiceNumberProvider(): InvoiceNumberProvider = DefaultInvoiceNumberProvider(1)

    @Bean
    fun paymentIntegrationClient(
        evakaProperties: TurkuEnv,
        paymentGenerator: SapPaymentGenerator,
        sftpConnector: SftpConnector,
    ): PaymentIntegrationClient {
        val sftpSender = SftpSender(evakaProperties.sapPayments, sftpConnector)
        return TurkuPaymentIntegrationClient(paymentGenerator, sftpSender)
    }

    @Bean
    @Profile("production")
    fun webServiceMessageSender(xroadEnv: VtjXroadEnv): WebServiceMessageSender =
        httpsMessageSender(xroadEnv.trustStore, xroadEnv.keyStore)

    @Bean
    fun tomcatCustomizer(env: Environment) =
        WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
            it.addContextValves(defaultAccessLoggingValve(env))
        }

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        object : TitaniaEmployeeIdConverter {
            override fun fromTitania(employeeId: String): String = employeeId.trimStart('0')
        }

    @Bean fun mealTypeMapper(): MealTypeMapper = DefaultMealTypeMapper

    @Bean
    fun fileDwExportClient(
        s3Client: S3Client,
        sftpConnector: SftpConnector,
        properties: TurkuEnv,
    ): DwExportClient =
        FileDWExportClient(
            s3Client,
            SftpSender(properties.dwExport.sftp, sftpConnector),
            properties,
        )

    @Bean
    fun evakaTurkuAsyncJobRunner(
        jdbi: Jdbi,
        tracer: Tracer,
        env: Environment,
    ): AsyncJobRunner<TurkuAsyncJob> =
        AsyncJobRunner(TurkuAsyncJob::class, listOf(TurkuAsyncJob.pool), jdbi, tracer)

    @Bean fun evakaTurkuDwJob(dwExportClient: DwExportClient) = DwExportJob(dwExportClient)

    @Bean
    fun evakaTurkuScheduledJobEnv(env: Environment): ScheduledJobsEnv<TurkuScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            TurkuScheduledJob.entries.associateWith { it.defaultSettings },
            "turku.job",
            env,
        )

    @Bean
    fun evakaTurkuScheduledJobs(
        evakaTurkuRunner: AsyncJobRunner<TurkuAsyncJob>,
        env: ScheduledJobsEnv<TurkuScheduledJob>,
    ): TurkuScheduledJobs = TurkuScheduledJobs(evakaTurkuRunner, env)

    @Bean
    fun passwordSpecification(): PasswordSpecification =
        DefaultPasswordSpecification(
            PasswordConstraints.UNCONSTRAINED.copy(
                minLength = 16,
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
