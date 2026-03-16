// SPDX-FileCopyrightText: 2023-2025 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.ScheduledJobsEnv
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.document.archival.ArchivalIntegrationClient
import fi.espoo.evaka.espoo.DefaultPasswordSpecification
import fi.espoo.evaka.invoicing.domain.PaymentIntegrationClient
import fi.espoo.evaka.invoicing.service.DefaultInvoiceGenerationLogic
import fi.espoo.evaka.logging.defaultAccessLoggingValve
import fi.espoo.evaka.lookup
import fi.espoo.evaka.mealintegration.DefaultMealTypeMapper
import fi.espoo.evaka.mealintegration.MealTypeMapper
import fi.espoo.evaka.shared.ArchiveProcessConfig
import fi.espoo.evaka.shared.ArchiveProcessType
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.PasswordConstraints
import fi.espoo.evaka.shared.auth.PasswordSpecification
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.template.ITemplateProvider
import fi.espoo.evaka.titania.TitaniaEmployeeIdConverter
import fi.espoo.evaka.turku.database.DevDataInitializer
import fi.espoo.evaka.turku.dw.DwExportClient
import fi.espoo.evaka.turku.dw.DwExportJob
import fi.espoo.evaka.turku.dw.FileDWExportClient
import fi.espoo.evaka.turku.invoice.service.SftpConnector
import fi.espoo.evaka.turku.invoice.service.SftpSender
import fi.espoo.evaka.turku.message.config.TurkuMessageProvider
import fi.espoo.evaka.turku.message.config.YamlMessageSource
import fi.espoo.evaka.turku.payment.service.SapPaymentGenerator
import fi.espoo.evaka.turku.payment.service.TurkuPaymentIntegrationClient
import fi.espoo.evaka.turku.security.TurkuActionRuleMapping
import fi.espoo.evaka.turku.template.config.TurkuTemplateProvider
import io.opentelemetry.api.trace.Tracer
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient

@Configuration
@Profile("turku_evaka")
@Import(TurkuAsyncJobRegistration::class)
@EnableConfigurationProperties(TurkuProperties::class)
class TurkuConfig {
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

    @Bean fun templateProvider(): ITemplateProvider = TurkuTemplateProvider()

    @Bean fun invoiceGenerationLogicChooser() = DefaultInvoiceGenerationLogic

    @Bean
    fun paymentIntegrationClient(
        evakaProperties: TurkuProperties,
        paymentGenerator: SapPaymentGenerator,
        sftpConnector: SftpConnector,
    ): PaymentIntegrationClient {
        val sftpSender = SftpSender(evakaProperties.sapPayments, sftpConnector)
        return TurkuPaymentIntegrationClient(paymentGenerator, sftpSender)
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
    @Profile("production")
    fun productionS3AsyncClient(
        bucketEnv: BucketEnv,
        credentialsProvider: AwsCredentialsProvider,
    ): S3AsyncClient = S3AsyncClient.crtBuilder().credentialsProvider(credentialsProvider).build()

    @Bean
    @Profile("local")
    fun localS3AsyncClient(
        bucketEnv: BucketEnv,
        credentialsProvider: AwsCredentialsProvider,
    ): S3AsyncClient =
        S3AsyncClient.crtBuilder()
            .region(Region.EU_WEST_1)
            .credentialsProvider(credentialsProvider)
            .build()

    @Bean
    fun fileDwExportClient(
        asyncClient: S3AsyncClient,
        sftpConnector: SftpConnector,
        properties: TurkuProperties,
    ): DwExportClient =
        FileDWExportClient(
            asyncClient,
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

    @Bean
    fun turkuTemplateResolver(): ITemplateResolver =
        ClassLoaderTemplateResolver().apply {
            prefix = "turku/templates/"
            suffix = ".html"
            setTemplateMode("HTML")
            checkExistence = true
            order = 1
        }
}
