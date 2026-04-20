// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokia

import evaka.core.EvakaEnv
import evaka.core.OphEnv
import evaka.core.ScheduledJobsEnv
import evaka.core.application.ApplicationStatus
import evaka.core.document.archival.ArchivalIntegrationClient
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.mealintegration.MealTypeMapper
import evaka.core.shared.ArchiveProcessConfig
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.FeatureConfig
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.PasswordConstraints
import evaka.core.shared.auth.PasswordSpecification
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.shared.sftp.SftpClient
import evaka.core.titania.TitaniaEmployeeIdConverter
import evaka.instance.espoo.DefaultPasswordSpecification
import evaka.instance.nokia.mealintegration.NokiaMealTypeMapper
import evaka.instance.nokia.security.NokiaActionRuleMapping
import evaka.trevaka.TrevakaProperties
import evaka.trevaka.archival.tweb.RegionalTwebArchivalClient
import evaka.trevaka.frends.frendsWebServiceMessageSender
import evaka.trevaka.security.TrevakaActionRuleMapping
import evaka.trevaka.titania.PrefixTitaniaEmployeeIdConverter
import evaka.trevaka.tomcat.tomcatAccessLoggingCustomizer
import java.time.MonthDay
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.ws.transport.WebServiceMessageSender

@Configuration
class NokiaConfig {

    @Bean
    fun featureConfig() =
        FeatureConfig(
            valueDecisionCapacityFactorEnabled = true,
            citizenReservationThresholdHours = 7 * 24 - 9, // Mon 09:00
            freeAbsenceGivesADailyRefund = true,
            alwaysUseDaycareFinanceDecisionHandler = true,
            paymentNumberSeriesStart = null,
            unplannedAbsencesAreContractSurplusDays = true,
            maxContractDaySurplusThreshold = null,
            useContractDaysAsDailyFeeDivisor = true,
            requestedStartUpperLimit = 14,
            postOffice = "NOKIA",
            municipalMessageAccountName = "Nokian kaupunki",
            serviceWorkerMessageAccountName = "Varhaiskasvatuksen ja esiopetuksen asiakaspalvelu",
            financeMessageAccountName = "Nokian varhaiskasvatuksen asiakasmaksut",
            applyPlacementUnitFromDecision = true,
            preferredStartRelativeApplicationDueDate = true,
            fiveYearsOldDaycareEnabled = false,
            archiveMetadataOrganization = "Nokian kaupunki, varhaiskasvatus ja esiopetus",
            archiveMetadataConfigs = { type, year ->
                when (type) {
                    ArchiveProcessType.APPLICATION_DAYCARE ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "04.01.00.11",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.APPLICATION_PRESCHOOL ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "04.01.03.41",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.APPLICATION_CLUB -> null

                    ArchiveProcessType.FEE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "04.01.00.12",
                            archiveDurationMonths = 15 * 12,
                        )

                    ArchiveProcessType.VOUCHER_VALUE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "04.01.02.59",
                            archiveDurationMonths = 15 * 12,
                        )
                }
            },
            daycarePlacementPlanEndMonthDay = MonthDay.of(8, 15),
            placementToolApplicationStatus = ApplicationStatus.WAITING_DECISION,
        )

    @Bean
    fun nokiaScheduledJobEnv(env: Environment): ScheduledJobsEnv<NokiaScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            NokiaScheduledJob.entries.associateWith { it.defaultSettings },
            "nokia.job",
            env,
        )

    @Bean
    fun nokiaScheduledJobs(
        ophEnv: OphEnv,
        properties: NokiaProperties,
        env: ScheduledJobsEnv<NokiaScheduledJob>,
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
    ): NokiaScheduledJobs = NokiaScheduledJobs(ophEnv, asyncJobRunner, properties, env)

    @Bean
    fun paymentIntegrationClient(): PaymentIntegrationClient =
        PaymentIntegrationClient.FailingClient()

    @Bean
    fun webServiceMessageSender(trevakaProperties: TrevakaProperties): WebServiceMessageSender =
        frendsWebServiceMessageSender(trevakaProperties.vtjKyselyApiKey)

    @Bean
    fun actionRuleMapping(): ActionRuleMapping = NokiaActionRuleMapping(TrevakaActionRuleMapping())

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        PrefixTitaniaEmployeeIdConverter("nok")

    @Bean fun accessLoggingCustomizer(env: Environment) = tomcatAccessLoggingCustomizer(env)

    @Bean fun mealTypeMapper(): MealTypeMapper = NokiaMealTypeMapper

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
        properties: NokiaProperties,
        featureConfig: FeatureConfig,
    ): ArchivalIntegrationClient =
        if (evakaEnv.archivalEnabled && properties.archival != null) {
            RegionalTwebArchivalClient(
                SftpClient(properties.archival.sftp.toSftpEnv()),
                properties.archival,
                featureConfig,
            )
        } else {
            ArchivalIntegrationClient.FailingClient()
        }
}
