// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu

import com.jcraft.jsch.JSch
import fi.espoo.evaka.ScheduledJobsEnv
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.document.archival.ArchivalIntegrationClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.espoo.DefaultPasswordSpecification
import fi.espoo.evaka.holidayperiod.QuestionnaireType
import fi.espoo.evaka.invoicing.domain.PaymentIntegrationClient
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.invoicing.service.DefaultInvoiceGenerationLogic
import fi.espoo.evaka.invoicing.service.DefaultInvoiceNumberProvider
import fi.espoo.evaka.invoicing.service.IncomeCoefficientMultiplierProvider
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.invoicing.service.InvoiceNumberProvider
import fi.espoo.evaka.invoicing.service.InvoiceProductProvider
import fi.espoo.evaka.logging.defaultAccessLoggingValve
import fi.espoo.evaka.mealintegration.DefaultMealTypeMapper
import fi.espoo.evaka.mealintegration.MealTypeMapper
import fi.espoo.evaka.oulu.database.DevDataInitializer
import fi.espoo.evaka.oulu.dw.DwExportClient
import fi.espoo.evaka.oulu.dw.DwExportJob
import fi.espoo.evaka.oulu.dw.FileDwExportClient
import fi.espoo.evaka.oulu.invoice.config.OuluIncomeCoefficientMultiplierProvider
import fi.espoo.evaka.oulu.invoice.config.OuluIncomeTypesProvider
import fi.espoo.evaka.oulu.invoice.config.OuluInvoiceProductProvider
import fi.espoo.evaka.oulu.invoice.service.OuluInvoiceClient
import fi.espoo.evaka.oulu.invoice.service.ProEInvoiceGenerator
import fi.espoo.evaka.oulu.invoice.service.SftpConnector
import fi.espoo.evaka.oulu.invoice.service.SftpSender
import fi.espoo.evaka.oulu.payment.service.BicMapper
import fi.espoo.evaka.oulu.payment.service.OuluPaymentIntegrationClient
import fi.espoo.evaka.oulu.payment.service.ProEPaymentGenerator
import fi.espoo.evaka.oulu.security.OuluActionRuleMapping
import fi.espoo.evaka.oulu.template.config.OuluTemplateProvider
import fi.espoo.evaka.oulu.util.FinanceDateProvider
import fi.espoo.evaka.shared.ArchiveProcessConfig
import fi.espoo.evaka.shared.ArchiveProcessType
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.PasswordConstraints
import fi.espoo.evaka.shared.auth.PasswordSpecification
import fi.espoo.evaka.shared.config.PDFConfig
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.template.ITemplateProvider
import fi.espoo.evaka.titania.TitaniaEmployeeIdConverter
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
