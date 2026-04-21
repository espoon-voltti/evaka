// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere

import evaka.core.EvakaEnv
import evaka.core.ScheduledJobsEnv
import evaka.core.application.ApplicationStatus
import evaka.core.bi.BiExportClient
import evaka.core.bi.BiExportJob
import evaka.core.document.archival.ArchivalIntegrationClient
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.mealintegration.DefaultMealTypeMapper
import evaka.core.mealintegration.MealTypeMapper
import evaka.core.shared.ArchiveProcessConfig
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.FeatureConfig
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.PasswordConstraints
import evaka.core.shared.auth.PasswordSpecification
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.titania.TitaniaEmployeeIdConverter
import evaka.instance.espoo.DefaultPasswordSpecification
import evaka.instance.tampere.archival.TampereArchivalClient
import evaka.instance.tampere.bi.FileBiExportS3Client
import evaka.instance.tampere.export.ExportUnitsAclService
import evaka.instance.tampere.payment.TamperePaymentClient
import evaka.instance.tampere.security.TampereActionRuleMapping
import evaka.trevaka.TrevakaProperties
import evaka.trevaka.export.ExportPreschoolChildDocumentsService
import evaka.trevaka.frends.basicAuthInterceptor
import evaka.trevaka.frends.frendsWebServiceMessageSender
import evaka.trevaka.frends.newFrendsHttpClient
import evaka.trevaka.security.TrevakaActionRuleMapping
import evaka.trevaka.titania.TrimStartTitaniaEmployeeIdConverter
import evaka.trevaka.tomcat.tomcatAccessLoggingCustomizer
import io.opentelemetry.api.trace.Tracer
import java.time.Duration
import java.time.MonthDay
import okhttp3.OkHttpClient
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.soap.SoapVersion
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory
import org.springframework.ws.transport.WebServiceMessageSender
import org.springframework.ws.transport.http.SimpleHttpComponents5MessageSender
import software.amazon.awssdk.services.s3.S3Client

internal val PAYMENT_SOAP_PACKAGES =
    arrayOf(
        "fi.tampere.messages.ipaas.commontypes.v1",
        "fi.tampere.messages.sapfico.payableaccounting.v05",
        "fi.tampere.services.sapfico.payableaccounting.v1",
    )

@Configuration
@Import(TampereAsyncJobRegistration::class)
class TampereConfig {

    @Bean
    fun featureConfig(): FeatureConfig =
        FeatureConfig(
            valueDecisionCapacityFactorEnabled = true,
            citizenReservationThresholdHours = 6 * 24, // Tue 00:00
            freeAbsenceGivesADailyRefund = false,
            alwaysUseDaycareFinanceDecisionHandler = true,
            paymentNumberSeriesStart = 1,
            unplannedAbsencesAreContractSurplusDays = true,
            maxContractDaySurplusThreshold = null,
            useContractDaysAsDailyFeeDivisor = true,
            requestedStartUpperLimit = 14,
            postOffice = "TAMPERE",
            municipalMessageAccountName = "Tampereen kaupunki",
            serviceWorkerMessageAccountName = "Varhaiskasvatuksen ja esiopetuksen asiakaspalvelu",
            financeMessageAccountName = "Tampereen varhaiskasvatuksen asiakasmaksut",
            applyPlacementUnitFromDecision = true,
            preferredStartRelativeApplicationDueDate = true,
            fiveYearsOldDaycareEnabled = false,
            temporaryDaycarePartDayAbsenceGivesADailyRefund = false,
            archiveMetadataOrganization = "Tampereen kaupunki, varhaiskasvatus ja esiopetus",
            archiveMetadataConfigs = { type, year ->
                when (type) {
                    ArchiveProcessType.APPLICATION_DAYCARE ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01.17",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.APPLICATION_PRESCHOOL ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01.17",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.APPLICATION_CLUB ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01.26",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.FEE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01.23",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.VOUCHER_VALUE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.06.01.23",
                            archiveDurationMonths = 10 * 12,
                        )
                }
            },
            daycarePlacementPlanEndMonthDay = MonthDay.of(8, 15),
            placementToolApplicationStatus = ApplicationStatus.WAITING_DECISION,
        )

    @Bean
    fun fileS3Client(s3Client: S3Client, properties: TampereProperties): BiExportClient =
        FileBiExportS3Client(s3Client, properties)

    @Bean
    fun tampereAsyncJobRunner(
        jdbi: Jdbi,
        tracer: Tracer,
        env: Environment,
    ): AsyncJobRunner<TampereAsyncJob> =
        AsyncJobRunner(TampereAsyncJob::class, listOf(TampereAsyncJob.pool), jdbi, tracer)

    @Bean
    fun tampereBiJob(biExportClient: BiExportClient): BiExportJob = BiExportJob(biExportClient)

    @Bean
    fun paymentIntegrationClient(properties: TampereProperties): PaymentIntegrationClient {
        val httpClient = newFrendsHttpClient(properties.financeApiKey)
        val messageFactory =
            SaajSoapMessageFactory().apply {
                setSoapVersion(SoapVersion.SOAP_12)
                afterPropertiesSet()
            }
        val marshaller =
            Jaxb2Marshaller().apply {
                setPackagesToScan(*PAYMENT_SOAP_PACKAGES)
                afterPropertiesSet()
            }
        val webServiceTemplate =
            WebServiceTemplate(messageFactory).apply {
                this.marshaller = marshaller
                unmarshaller = marshaller
                setMessageSender(SimpleHttpComponents5MessageSender(httpClient))
                afterPropertiesSet()
            }
        return TamperePaymentClient(webServiceTemplate, properties.payment)
    }

    @Bean
    fun webServiceMessageSender(trevakaProperties: TrevakaProperties): WebServiceMessageSender =
        frendsWebServiceMessageSender(trevakaProperties.vtjKyselyApiKey)

    @Bean
    fun actionRuleMapping(): ActionRuleMapping =
        TampereActionRuleMapping(TrevakaActionRuleMapping())

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        TrimStartTitaniaEmployeeIdConverter()

    @Bean fun accessLoggingCustomizer(env: Environment) = tomcatAccessLoggingCustomizer(env)

    @Bean fun mealTypeMapper(): MealTypeMapper = DefaultMealTypeMapper

    @Bean
    fun tampereScheduledJobEnv(env: Environment): ScheduledJobsEnv<TampereScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            TampereScheduledJob.entries.associateWith { it.defaultSettings },
            "tampere.job",
            env,
        )

    @Bean
    fun tampereScheduledJobs(
        exportPreschoolChildDocumentsService: ExportPreschoolChildDocumentsService,
        exportUnitsAclService: ExportUnitsAclService,
        tampereRunner: AsyncJobRunner<TampereAsyncJob>,
        properties: TampereProperties,
        env: ScheduledJobsEnv<TampereScheduledJob>,
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
    ): TampereScheduledJobs =
        TampereScheduledJobs(
            exportPreschoolChildDocumentsService,
            exportUnitsAclService,
            tampereRunner,
            asyncJobRunner,
            properties,
            env,
        )

    @Bean
    fun passwordSpecification(): PasswordSpecification =
        DefaultPasswordSpecification(
            PasswordConstraints.UNCONSTRAINED.copy(
                minLength = 10,
                minLowers = 1,
                minUppers = 1,
                minDigits = 1,
                minSymbols = 0,
            )
        )

    @Bean
    fun archivalIntegrationClient(
        evakaEnv: EvakaEnv,
        properties: TampereProperties,
    ): ArchivalIntegrationClient =
        if (evakaEnv.archivalEnabled) {
            val client =
                OkHttpClient.Builder()
                    .connectTimeout(Duration.ofMinutes(1))
                    .readTimeout(Duration.ofMinutes(1))
                    .writeTimeout(Duration.ofMinutes(1))
                    .addInterceptor(
                        basicAuthInterceptor(
                            properties.frends
                                ?: error("Frends properties not set (TAMPERE_FRENDS_*)")
                        )
                    )
                    .build()
            TampereArchivalClient(
                client,
                properties.archival ?: error("Archival properties not set (TAMPERE_ARCHIVAL_*)"),
            )
        } else {
            ArchivalIntegrationClient.FailingClient()
        }
}
