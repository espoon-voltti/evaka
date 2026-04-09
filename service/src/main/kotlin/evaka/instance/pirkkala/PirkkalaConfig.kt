// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.pirkkala

import evaka.core.EvakaEnv
import evaka.core.ScheduledJobsEnv
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
import evaka.instance.pirkkala.mealintegration.PirkkalaMealTypeMapper
import evaka.instance.pirkkala.security.PirkkalaActionRuleMapping
import evaka.trevaka.archival.tweb.RegionalTwebArchivalClient
import evaka.trevaka.security.TrevakaActionRuleMapping
import evaka.trevaka.titania.PrefixTitaniaEmployeeIdConverter
import evaka.trevaka.tomcat.tomcatAccessLoggingCustomizer
import java.time.MonthDay
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class PirkkalaConfig {

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
            postOffice = "PIRKKALA",
            municipalMessageAccountName = "Pirkkalan kunta",
            serviceWorkerMessageAccountName = "Varhaiskasvatuksen asiakaspalvelu",
            financeMessageAccountName = "Pirkkalan varhaiskasvatuksen asiakasmaksut",
            applyPlacementUnitFromDecision = true,
            preferredStartRelativeApplicationDueDate = true,
            fiveYearsOldDaycareEnabled = false,
            archiveMetadataOrganization = "Pirkkalan kunta, varhaiskasvatus",
            archiveMetadataConfigs = { type, year ->
                when (type) {
                    ArchiveProcessType.APPLICATION_DAYCARE ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "4801.101",
                            archiveDurationMonths = 12 * 12,
                        )

                    ArchiveProcessType.APPLICATION_PRESCHOOL -> null

                    ArchiveProcessType.APPLICATION_CLUB ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "4801.23",
                            archiveDurationMonths = 12 * 12,
                        )

                    ArchiveProcessType.FEE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "4801.102",
                            archiveDurationMonths = 15 * 12,
                        )

                    ArchiveProcessType.VOUCHER_VALUE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "4803.03",
                            archiveDurationMonths = 15 * 12,
                        )
                }
            },
            daycarePlacementPlanEndMonthDay = MonthDay.of(8, 15),
        )

    @Bean
    fun pirkkalaScheduledJobEnv(env: Environment): ScheduledJobsEnv<PirkkalaScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            PirkkalaScheduledJob.entries.associateWith { it.defaultSettings },
            "pirkkala.job",
            env,
        )

    @Bean
    fun pirkkalaScheduledJobs(
        properties: PirkkalaProperties,
        env: ScheduledJobsEnv<PirkkalaScheduledJob>,
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
    ): PirkkalaScheduledJobs = PirkkalaScheduledJobs(asyncJobRunner, properties, env)

    @Bean
    fun paymentIntegrationClient(): PaymentIntegrationClient =
        PaymentIntegrationClient.FailingClient()

    @Bean
    fun actionRuleMapping(): ActionRuleMapping =
        PirkkalaActionRuleMapping(TrevakaActionRuleMapping())

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        PrefixTitaniaEmployeeIdConverter("pir")

    @Bean fun accessLoggingCustomizer(env: Environment) = tomcatAccessLoggingCustomizer(env)

    @Bean fun mealTypeMapper(): MealTypeMapper = PirkkalaMealTypeMapper

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
        properties: PirkkalaProperties,
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
