// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu

import com.jcraft.jsch.JSch
import evaka.core.ScheduledJobsEnv
import evaka.core.application.ApplicationStatus
import evaka.core.document.archival.ArchivalIntegrationClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.holidayperiod.QuestionnaireType
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.invoicing.service.DefaultInvoiceGenerationLogic
import evaka.core.invoicing.service.DefaultInvoiceNumberProvider
import evaka.core.invoicing.service.IncomeCoefficientMultiplierProvider
import evaka.core.invoicing.service.IncomeTypesProvider
import evaka.core.invoicing.service.InvoiceNumberProvider
import evaka.core.invoicing.service.InvoiceProductProvider
import evaka.core.logging.defaultAccessLoggingValve
import evaka.core.mealintegration.DefaultMealTypeMapper
import evaka.core.mealintegration.MealTypeMapper
import evaka.core.shared.ArchiveProcessConfig
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.FeatureConfig
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.PasswordConstraints
import evaka.core.shared.auth.PasswordSpecification
import evaka.core.shared.config.PDFConfig
import evaka.core.shared.domain.RealEvakaClock
import evaka.core.shared.message.IMessageProvider
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.shared.template.ITemplateProvider
import evaka.core.titania.TitaniaEmployeeIdConverter
import evaka.instance.espoo.DefaultPasswordSpecification
import evaka.instance.oulu.database.DevDataInitializer
import evaka.instance.oulu.dw.DwExportClient
import evaka.instance.oulu.dw.DwExportJob
import evaka.instance.oulu.dw.FileDwExportClient
import evaka.instance.oulu.invoice.config.OuluIncomeCoefficientMultiplierProvider
import evaka.instance.oulu.invoice.config.OuluIncomeTypesProvider
import evaka.instance.oulu.invoice.config.OuluInvoiceProductProvider
import evaka.instance.oulu.invoice.service.OuluInvoiceClient
import evaka.instance.oulu.invoice.service.ProEInvoiceGenerator
import evaka.instance.oulu.invoice.service.SftpConnector
import evaka.instance.oulu.invoice.service.SftpSender
import evaka.instance.oulu.payment.service.BicMapper
import evaka.instance.oulu.payment.service.OuluPaymentIntegrationClient
import evaka.instance.oulu.payment.service.ProEPaymentGenerator
import evaka.instance.oulu.security.OuluActionRuleMapping
import evaka.instance.oulu.template.config.OuluTemplateProvider
import evaka.instance.oulu.util.FinanceDateProvider
import io.opentelemetry.api.trace.Tracer
import java.time.MonthDay
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.thymeleaf.ITemplateEngine
import software.amazon.awssdk.services.s3.S3Client

@Configuration
@Profile("oulu_evaka")
@Import(OuluAsyncJobRegistration::class)
class OuluConfig {
    @Bean fun ouluEnv(env: Environment): OuluEnv = OuluEnv.fromEnvironment(env)

    @Bean
    fun featureConfig(): FeatureConfig =
        FeatureConfig(
            valueDecisionCapacityFactorEnabled = false,
            // (7*24) - 3 = 165
            citizenReservationThresholdHours = 165,
            alwaysUseDaycareFinanceDecisionHandler = false,
            freeAbsenceGivesADailyRefund = true,
            paymentNumberSeriesStart = 1,
            serviceWorkerMessageAccountName = "Oulun kaupunki",
            applyPlacementUnitFromDecision = false,
            unplannedAbsencesAreContractSurplusDays = false,
            maxContractDaySurplusThreshold = 13,
            useContractDaysAsDailyFeeDivisor = false,
            requestedStartUpperLimit = 7,
            preferredStartRelativeApplicationDueDate = true,
            postOffice = "OULU",
            municipalMessageAccountName = "Oulun kaupunki",
            fiveYearsOldDaycareEnabled = false,
            financeMessageAccountName =
                "Varhaiskasvatuksen asiakasmaksut - Early childhood education fees",
            archiveMetadataOrganization = "Oulun kaupungin varhaiskasvatus",
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
                            processDefinitionNumber = "12.06.01",
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
                            processDefinitionNumber = "12.06.01",
                            archiveDurationMonths = 192,
                        )
                    }

                    ArchiveProcessType.VOUCHER_VALUE_DECISION -> {
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01",
                            archiveDurationMonths = 192,
                        )
                    }
                }
            },
            placementToolApplicationStatus = ApplicationStatus.WAITING_DECISION,
            holidayQuestionnaireType = QuestionnaireType.OPEN_RANGES,
            minimumInvoiceAmount = 800,
            daycarePlacementPlanEndMonthDay = MonthDay.of(8, 20),
        )

    @Bean fun actionRuleMapping(): ActionRuleMapping = OuluActionRuleMapping()

    @Bean
    @Profile("local")
    fun devDataInitializer(jdbi: Jdbi): DevDataInitializer = DevDataInitializer(jdbi)

    @Bean
    fun messageProvider(): IMessageProvider {
        val messageSource = YamlMessageSource(ClassPathResource("oulu/messages.yaml"))
        return OuluMessageProvider(messageSource)
    }

    @Bean fun emailMessageProvider(): IEmailMessageProvider = OuluEmailMessageProvider()

    @Bean fun templateProvider(): ITemplateProvider = OuluTemplateProvider()

    @Bean fun templateEngine(): ITemplateEngine = PDFConfig.templateEngine("oulu")

    @Bean fun invoiceGenerationLogicChooser() = DefaultInvoiceGenerationLogic // TODO: implement

    @Bean
    fun invoiceIntegrationClient(
        ouluEnv: OuluEnv,
        sftpConnector: SftpConnector,
    ): InvoiceIntegrationClient {
        val sftpSender = SftpSender(ouluEnv.intimeInvoices, sftpConnector)
        return OuluInvoiceClient(
            sftpSender,
            ProEInvoiceGenerator(FinanceDateProvider(RealEvakaClock())),
        )
    }

    @Bean fun sftpConnector(): SftpConnector = SftpConnector(JSch())

    @Bean fun incomeTypesProvider(): IncomeTypesProvider = OuluIncomeTypesProvider()

    @Bean
    fun incomeCoefficientMultiplierProvider(): IncomeCoefficientMultiplierProvider =
        OuluIncomeCoefficientMultiplierProvider()

    @Bean fun invoiceProductProvider(): InvoiceProductProvider = OuluInvoiceProductProvider()

    @Bean fun invoiceNumberProvider(): InvoiceNumberProvider = DefaultInvoiceNumberProvider(1)

    @Bean
    fun paymentIntegrationClient(
        ouluEnv: OuluEnv,
        sftpConnector: SftpConnector,
    ): PaymentIntegrationClient {
        val sftpSender = SftpSender(ouluEnv.intimePayments, sftpConnector)
        val paymentGenerator =
            ProEPaymentGenerator(FinanceDateProvider(RealEvakaClock()), BicMapper())
        return OuluPaymentIntegrationClient(paymentGenerator, sftpSender)
    }

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
        ouluEnv: OuluEnv,
    ): DwExportClient =
        FileDwExportClient(s3Client, SftpSender(ouluEnv.dwExport.sftp, sftpConnector), ouluEnv)

    @Bean
    fun OuluAsyncJobRunner(
        jdbi: Jdbi,
        tracer: Tracer,
        env: Environment,
    ): AsyncJobRunner<OuluAsyncJob> =
        AsyncJobRunner(OuluAsyncJob::class, listOf(OuluAsyncJob.pool), jdbi, tracer)

    @Bean fun evakaOuluDWJob(dwExportClient: DwExportClient) = DwExportJob(dwExportClient)

    @Bean
    fun OuluScheduledJobEnv(env: Environment): ScheduledJobsEnv<OuluScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            OuluScheduledJob.entries.associateWith { it.defaultSettings },
            "oulu.job",
            env,
        )

    @Bean
    fun ouluScheduledJobs(
        evakaOuluRunner: AsyncJobRunner<OuluAsyncJob>,
        env: ScheduledJobsEnv<OuluScheduledJob>,
    ): OuluScheduledJobs = OuluScheduledJobs(evakaOuluRunner, env)

    @Bean
    fun passwordSpecification(): PasswordSpecification =
        DefaultPasswordSpecification(
            PasswordConstraints.UNCONSTRAINED.copy(
                minLength = 14,
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
