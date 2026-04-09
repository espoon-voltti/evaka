// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.kangasala

import evaka.core.EvakaEnv
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
import evaka.instance.kangasala.mealintegration.KangasalaMealTypeMapper
import evaka.instance.kangasala.security.KangasalaActionRuleMapping
import evaka.trevaka.archival.tweb.RegionalTwebArchivalClient
import evaka.trevaka.security.TrevakaActionRuleMapping
import evaka.trevaka.titania.PrefixTitaniaEmployeeIdConverter
import evaka.trevaka.tomcat.tomcatAccessLoggingCustomizer
import java.time.MonthDay
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class KangasalaConfig {

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
            postOffice = "KANGASALA",
            municipalMessageAccountName = "Kangasalan kaupunki",
            serviceWorkerMessageAccountName = "Varhaiskasvatuksen ja esiopetuksen asiakaspalvelu",
            financeMessageAccountName = "Kangasalan varhaiskasvatuksen asiakasmaksut",
            applyPlacementUnitFromDecision = true,
            preferredStartRelativeApplicationDueDate = true,
            fiveYearsOldDaycareEnabled = false,
            archiveMetadataOrganization = "Kangasalan kaupunki, varhaiskasvatus",
            archiveMetadataConfigs = { type, year ->
                when (type) {
                    ArchiveProcessType.APPLICATION_DAYCARE ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "46.00.63",
                            archiveDurationMonths = 120 * 12,
                        )

                    ArchiveProcessType.APPLICATION_PRESCHOOL ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "41.01.05",
                            archiveDurationMonths = 120 * 12,
                        )

                    ArchiveProcessType.APPLICATION_CLUB -> null

                    ArchiveProcessType.FEE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "46.00.64",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.VOUCHER_VALUE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "46.02.07",
                            archiveDurationMonths = 10 * 12,
                        )
                }
            },
            daycarePlacementPlanEndMonthDay = MonthDay.of(8, 15),
            placementToolApplicationStatus = ApplicationStatus.WAITING_DECISION,
        )

    @Bean
    fun kangasalaScheduledJobEnv(env: Environment): ScheduledJobsEnv<KangasalaScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            KangasalaScheduledJob.entries.associateWith { it.defaultSettings },
            "kangasala.job",
            env,
        )

    @Bean
    fun kangasalaScheduledJobs(
        properties: KangasalaProperties,
        env: ScheduledJobsEnv<KangasalaScheduledJob>,
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
    ): KangasalaScheduledJobs = KangasalaScheduledJobs(asyncJobRunner, properties, env)

    @Bean
    fun paymentIntegrationClient(): PaymentIntegrationClient =
        PaymentIntegrationClient.FailingClient()

    @Bean
    fun actionRuleMapping(): ActionRuleMapping =
        KangasalaActionRuleMapping(TrevakaActionRuleMapping())

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        PrefixTitaniaEmployeeIdConverter("kan")

    @Bean fun accessLoggingCustomizer(env: Environment) = tomcatAccessLoggingCustomizer(env)

    @Bean fun mealTypeMapper(): MealTypeMapper = KangasalaMealTypeMapper

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
        properties: KangasalaProperties,
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
