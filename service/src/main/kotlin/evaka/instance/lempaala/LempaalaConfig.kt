// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.lempaala

import evaka.core.OphEnv
import evaka.core.ScheduledJobsEnv
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
import evaka.instance.lempaala.mealintegration.LempaalaMealTypeMapper
import evaka.instance.lempaala.security.LempaalaActionRuleMapping
import evaka.trevaka.security.TrevakaActionRuleMapping
import evaka.trevaka.titania.PrefixTitaniaEmployeeIdConverter
import evaka.trevaka.tomcat.tomcatAccessLoggingCustomizer
import java.time.MonthDay
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class LempaalaConfig {

    @Bean
    fun featureConfig() =
        FeatureConfig(
            valueDecisionCapacityFactorEnabled = true,
            citizenReservationThresholdHours =
                (7 + 3) * 24 - 9, // Fri 09:00 (1 week + 3 days before)
            freeAbsenceGivesADailyRefund = true,
            alwaysUseDaycareFinanceDecisionHandler = true,
            paymentNumberSeriesStart = null,
            unplannedAbsencesAreContractSurplusDays = true,
            maxContractDaySurplusThreshold = null,
            useContractDaysAsDailyFeeDivisor = true,
            requestedStartUpperLimit = 14,
            postOffice = "LEMPÄÄLÄ",
            municipalMessageAccountName = "Lempäälän kunta",
            serviceWorkerMessageAccountName = "Varhaiskasvatuksen asiakaspalvelu",
            financeMessageAccountName = "Lempäälän varhaiskasvatuksen asiakasmaksut",
            applyPlacementUnitFromDecision = true,
            preferredStartRelativeApplicationDueDate = true,
            fiveYearsOldDaycareEnabled = false,
            archiveMetadataOrganization = "Lempäälän kunta, varhaiskasvatus",
            archiveMetadataConfigs = { type, year ->
                when (type) {
                    ArchiveProcessType.APPLICATION_DAYCARE ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.07.01.00",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.APPLICATION_PRESCHOOL -> null

                    ArchiveProcessType.APPLICATION_CLUB -> null

                    ArchiveProcessType.FEE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "12.07.01.01",
                            archiveDurationMonths = 10 * 12,
                        )

                    ArchiveProcessType.VOUCHER_VALUE_DECISION ->
                        ArchiveProcessConfig(
                            processDefinitionNumber = "02.07.01.01",
                            archiveDurationMonths = 10 * 12,
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
        LempaalaActionRuleMapping(TrevakaActionRuleMapping())

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        PrefixTitaniaEmployeeIdConverter("lem")

    @Bean fun accessLoggingCustomizer(env: Environment) = tomcatAccessLoggingCustomizer(env)

    @Bean fun mealTypeMapper(): MealTypeMapper = LempaalaMealTypeMapper

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
    fun lempaalaScheduledJobEnv(env: Environment): ScheduledJobsEnv<LempaalaScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            LempaalaScheduledJob.entries.associateWith { it.defaultSettings },
            "lempaala.job",
            env,
        )

    @Bean
    fun lempaalaScheduledJobs(
        ophEnv: OphEnv,
        properties: LempaalaProperties,
        env: ScheduledJobsEnv<LempaalaScheduledJob>,
    ): LempaalaScheduledJobs = LempaalaScheduledJobs(ophEnv, properties, env)
}
