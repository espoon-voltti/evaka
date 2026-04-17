// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi

import evaka.core.OphEnv
import evaka.core.ScheduledJobsEnv
import evaka.core.application.ApplicationStatus
import evaka.core.document.archival.ArchivalIntegrationClient
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.mealintegration.MealTypeMapper
import evaka.core.shared.ArchiveProcessConfig
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.FeatureConfig
import evaka.core.shared.auth.PasswordConstraints
import evaka.core.shared.auth.PasswordSpecification
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.titania.TitaniaEmployeeIdConverter
import evaka.instance.espoo.DefaultPasswordSpecification
import evaka.instance.ylojarvi.mealintegration.YlojarviMealTypeMapper
import evaka.instance.ylojarvi.security.YlojarviActionRuleMapping
import evaka.trevaka.security.TrevakaActionRuleMapping
import evaka.trevaka.titania.PrefixTitaniaEmployeeIdConverter
import evaka.trevaka.tomcat.tomcatAccessLoggingCustomizer
import java.time.MonthDay
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class YlojarviConfig {

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
            postOffice = "YLÖJÄRVI",
            municipalMessageAccountName = "Ylöjärven kaupunki",
            serviceWorkerMessageAccountName = "Varhaiskasvatuksen ja esiopetuksen asiakaspalvelu",
            financeMessageAccountName = "Ylöjärven varhaiskasvatuksen asiakasmaksut",
            applyPlacementUnitFromDecision = true,
            preferredStartRelativeApplicationDueDate = true,
            fiveYearsOldDaycareEnabled = false,
            archiveMetadataOrganization = "Ylöjärven kaupunki, varhaiskasvatus",
            archiveMetadataConfigs = { type, year ->
                when (type) {
                    ArchiveProcessType.APPLICATION_DAYCARE ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.07.01",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.APPLICATION_PRESCHOOL ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.01.00",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.APPLICATION_CLUB ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.07.01",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.FEE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.07.01",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.VOUCHER_VALUE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.07.01",
                            archiveDurationMonths = 10 * 12,
                        )
                }
            },
            daycarePlacementPlanEndMonthDay = MonthDay.of(8, 15),
            placementToolApplicationStatus = ApplicationStatus.WAITING_DECISION,
        )

    @Bean
    fun paymentIntegrationClient(): PaymentIntegrationClient =
        PaymentIntegrationClient.FailingClient()

    @Bean
    fun actionRuleMapping(): ActionRuleMapping =
        YlojarviActionRuleMapping(TrevakaActionRuleMapping())

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        PrefixTitaniaEmployeeIdConverter("ylo")

    @Bean fun accessLoggingCustomizer(env: Environment) = tomcatAccessLoggingCustomizer(env)

    @Bean fun mealTypeMapper(): MealTypeMapper = YlojarviMealTypeMapper

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
    fun ylojarviScheduledJobEnv(env: Environment): ScheduledJobsEnv<YlojarviScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            YlojarviScheduledJob.entries.associateWith { it.defaultSettings },
            "ylojarvi.job",
            env,
        )

    @Bean
    fun ylojarviScheduledJobs(
        ophEnv: OphEnv,
        properties: YlojarviProperties,
        env: ScheduledJobsEnv<YlojarviScheduledJob>,
    ): YlojarviScheduledJobs = YlojarviScheduledJobs(ophEnv, properties, env)
}
