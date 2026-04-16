// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.vesilahti

import evaka.core.OphEnv
import evaka.core.ScheduledJobsEnv
import evaka.core.document.archival.ArchivalIntegrationClient
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.mealintegration.DefaultMealTypeMapper
import evaka.core.mealintegration.MealTypeMapper
import evaka.core.shared.ArchiveProcessConfig
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.FeatureConfig
import evaka.core.shared.auth.PasswordConstraints
import evaka.core.shared.auth.PasswordSpecification
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.titania.TitaniaEmployeeIdConverter
import evaka.instance.espoo.DefaultPasswordSpecification
import evaka.instance.vesilahti.security.VesilahtiActionRuleMapping
import evaka.trevaka.security.TrevakaActionRuleMapping
import evaka.trevaka.titania.PrefixTitaniaEmployeeIdConverter
import evaka.trevaka.tomcat.tomcatAccessLoggingCustomizer
import java.time.MonthDay
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class VesilahtiConfig {

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
            postOffice = "VESILAHTI",
            municipalMessageAccountName = "Vesilahden kunta",
            serviceWorkerMessageAccountName = "Varhaiskasvatuksen asiakaspalvelu",
            financeMessageAccountName = "Vesilahden varhaiskasvatuksen asiakasmaksut",
            applyPlacementUnitFromDecision = true,
            preferredStartRelativeApplicationDueDate = true,
            fiveYearsOldDaycareEnabled = false,
            archiveMetadataOrganization = "Vesilahden kunta, varhaiskasvatus",
            archiveMetadataConfigs = { type, year ->
                when (type) {
                    ArchiveProcessType.APPLICATION_DAYCARE ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.07.02",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.APPLICATION_PRESCHOOL ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.01.00",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.APPLICATION_CLUB ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.01.09",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.FEE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.07.02",
                            archiveDurationMonths = 6 * 12,
                        )

                    ArchiveProcessType.VOUCHER_VALUE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.07.02",
                            archiveDurationMonths = 6 * 12,
                        )
                }
            },
            daycarePlacementPlanEndMonthDay = MonthDay.of(8, 15),
        )

    @Bean
    fun paymentIntegrationClient(): PaymentIntegrationClient =
        PaymentIntegrationClient.FailingClient()

    @Bean
    fun actionRuleMapping(): ActionRuleMapping =
        VesilahtiActionRuleMapping(TrevakaActionRuleMapping())

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        PrefixTitaniaEmployeeIdConverter("ves")

    @Bean fun accessLoggingCustomizer(env: Environment) = tomcatAccessLoggingCustomizer(env)

    @Bean fun mealTypeMapper(): MealTypeMapper = DefaultMealTypeMapper

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
    fun archivalIntegrationClient(): ArchivalIntegrationClient =
        ArchivalIntegrationClient.FailingClient()

    @Bean
    fun vesilahtiScheduledJobEnv(env: Environment): ScheduledJobsEnv<VesilahtiScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            VesilahtiScheduledJob.entries.associateWith { it.defaultSettings },
            "vesilahti.job",
            env,
        )

    @Bean
    fun vesilahtiScheduledJobs(
        ophEnv: OphEnv,
        properties: VesilahtiProperties,
        env: ScheduledJobsEnv<VesilahtiScheduledJob>,
    ): VesilahtiScheduledJobs = VesilahtiScheduledJobs(ophEnv, properties, env)
}
