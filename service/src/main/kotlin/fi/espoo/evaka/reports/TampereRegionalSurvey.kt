// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import java.time.YearMonth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class TampereRegionalSurvey(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/tampere-regional-survey/monthly")
    fun getTampereRegionalSurvey(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam year: Int,
    ): RegionalSurveyReportResult {
        val range = FiniteDateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_TAMPERE_REGIONAL_SURVEY_REPORT,
                    )

                    val reportingDays =
                        generateSequence(YearMonth.of(year, 1)) { it.plusMonths(1) }
                            .take(12)
                            .map { month -> month.atEndOfMonth() }
                            .toList()

                    val municipalDaycareResults =
                        tx.getMonthlyMunicipalPlacementsByAgeAndPartTime(range, reportingDays)
                            .associateBy { it.month }
                    val familyDaycareResults =
                        tx.getMonthlyFamilyDaycareCounts(reportingDays, range).associateBy {
                            it.month
                        }
                    val municipalShiftCareResults =
                        tx.getMonthlyShiftCareCounts(range, reportingDays).associateBy { it.month }
                    val assistanceResults =
                        tx.getMunicipalAssistanceMonthlyResults(reportingDays).associateBy {
                            it.month
                        }

                    val monthlyResults =
                        (1..12).map {
                            RegionalSurveyMonthlyResults(
                                month = it,
                                familyDaycareResults =
                                    familyDaycareResults[it]
                                        ?: MonthlyFamilyDaycareResult(month = it),
                                municipalDaycareResults =
                                    municipalDaycareResults[it]
                                        ?: MonthlyMunicipalDaycareResult(month = it),
                                municipalShiftCareResults =
                                    municipalShiftCareResults[it]
                                        ?: MonthlyMunicipalShiftCareResult(month = it),
                                assistanceResults =
                                    assistanceResults[it] ?: MonthlyAssistanceResult(month = it),
                            )
                        }

                    RegionalSurveyReportResult(year = year, monthlyCounts = monthlyResults)
                }
            }
            .also { Audit.TampereRegionalSurveyMonthly.log(meta = mapOf("year" to year)) }
    }

    private fun Database.Read.getMunicipalAssistanceMonthlyResults(
        reportingDays: List<LocalDate>
    ): List<MonthlyAssistanceResult> {
        val data =
            getMonthlyAssistanceCountRows(reportingDays = reportingDays).associateBy { it.month }

        return (1..12).map { month -> data[month] ?: MonthlyAssistanceResult(month = month) }
    }

    data class RegionalSurveyMonthlyResults(
        val month: Int,
        val municipalDaycareResults: MonthlyMunicipalDaycareResult,
        val familyDaycareResults: MonthlyFamilyDaycareResult,
        val municipalShiftCareResults: MonthlyMunicipalShiftCareResult,
        val assistanceResults: MonthlyAssistanceResult,
    )

    data class MonthlyMunicipalDaycareResult(
        val month: Int,
        val municipalOver3PartTimeCount: Int = 0,
        val municipalUnder3PartTimeCount: Int = 0,
        val municipalOver3FullTimeCount: Int = 0,
        val municipalUnder3FullTimeCount: Int = 0,
    )

    data class MonthlyAssistanceResult(val month: Int, val assistanceCount: Int = 0)

    data class MonthlyMunicipalShiftCareResult(
        val month: Int,
        val municipalShiftCareCount: Int = 0,
    )

    data class MonthlyFamilyDaycareResult(
        val month: Int = 0,
        val familyOver3Count: Int = 0,
        val familyUnder3Count: Int = 0,
    )

    data class RegionalSurveyReportResult(
        val year: Int,
        val monthlyCounts: List<RegionalSurveyMonthlyResults>,
    )

    private fun Database.Read.getMonthlyMunicipalPlacementsByAgeAndPartTime(
        range: FiniteDateRange,
        statDays: List<LocalDate>,
    ): List<MonthlyMunicipalDaycareResult> {
        return createQuery {
                sql(
                    """
WITH child_rows AS (SELECT pl.child_id,
                           p.date_of_birth,
                           CASE
                               WHEN (sn.start_date IS NOT NULL) THEN sno.daycare_hours_per_month
                               ELSE default_sno.daycare_hours_per_month END                          as monthly_hours,
                           CASE
                               WHEN (sn.start_date IS NOT NULL) THEN sno.contract_days_per_month
                               ELSE default_sno.contract_days_per_month END                          as monthly_days,
                           CASE
                               WHEN (sn.start_date IS NOT NULL)
                                   THEN daterange(sn.start_date, sn.end_date, '[]') * ${bind(range)}
                               ELSE daterange(pl.start_date, pl.end_date, '[]') * ${bind(range)} END as validity
                    FROM placement pl
                             JOIN person p ON pl.child_id = p.id
                             JOIN daycare d ON pl.unit_id = d.id
                             LEFT JOIN service_need sn
                                       ON pl.id = sn.placement_id
                                           AND daterange(sn.start_date, sn.end_date, '[]') && ${bind(range)}
                             LEFT JOIN service_need_option sno ON sn.option_id = sno.id
                             LEFT JOIN service_need_option default_sno
                                       ON pl.type = default_sno.valid_placement_type AND default_sno.default_option
                    WHERE pl.type = ANY ('{DAYCARE,PRESCHOOL_DAYCARE,PRESCHOOL_DAYCARE_ONLY}')
                      AND d.provider_type = ANY ('{MUNICIPAL}')
                      AND daterange(pl.start_date, pl.end_date, '[]') && ${bind(range)})
SELECT extract(MONTH FROM day)                                            AS month,
       count(DISTINCT row.child_id)
       FILTER ( WHERE date_part('year', age(day, row.date_of_birth)) < 3
           AND (row.monthly_hours < 86 OR row.monthly_days < 11))         AS municipal_under_3_part_time_count,
       count(DISTINCT row.child_id)
       FILTER ( WHERE date_part('year', age(day, row.date_of_birth)) < 3
           AND ((row.monthly_hours IS NULL OR row.monthly_hours >= 86)
               AND (row.monthly_days IS NULL OR row.monthly_days >= 11))) AS municipal_under_3_full_time_count,
       count(DISTINCT row.child_id)
       FILTER ( WHERE date_part('year', age(day, row.date_of_birth)) >= 3
           AND (row.monthly_hours < 86 OR row.monthly_days < 11))         AS municipal_over_3_part_time_count,
       count(DISTINCT row.child_id)
       FILTER ( WHERE date_part('year', age(day, row.date_of_birth)) >= 3
           AND ((row.monthly_hours IS NULL OR row.monthly_hours >= 86)
               AND (row.monthly_days IS NULL OR row.monthly_days >= 11))) AS municipal_over_3_full_time_count
FROM unnest(${bind(statDays)}::date[]) day
         LEFT JOIN child_rows row on validity @> day
GROUP BY month 
                """
                )
            }
            .toList<MonthlyMunicipalDaycareResult>()
    }

    private fun Database.Read.getMonthlyShiftCareCounts(
        range: FiniteDateRange,
        statDays: List<LocalDate>,
    ): List<MonthlyMunicipalShiftCareResult> {
        return createQuery {
                sql(
                    """
WITH child_rows AS (SELECT pl.child_id,
                           daterange(sn.start_date, sn.end_date, '[]') * ${bind(range)} AS validity
                    FROM placement pl
                             JOIN daycare d ON pl.unit_id = d.id
                             JOIN service_need sn
                                  ON pl.id = sn.placement_id
                                      AND daterange(sn.start_date, sn.end_date, '[]') && ${bind(range)}
                    WHERE pl.type = ANY ('{DAYCARE,PRESCHOOL_DAYCARE,PRESCHOOL_DAYCARE_ONLY}')
                      AND d.provider_type = ANY ('{MUNICIPAL}')
                      AND sn.shift_care = ANY ('{FULL,INTERMITTENT}')
                      AND daterange(pl.start_date, pl.end_date, '[]') && ${bind(range)})
SELECT extract(MONTH FROM day)      AS month,
       count(DISTINCT row.child_id) AS municipal_shift_care_count
FROM unnest(${bind(statDays)}::date[]) day
         LEFT JOIN child_rows row on validity @> day
GROUP BY month
                """
                )
            }
            .toList<MonthlyMunicipalShiftCareResult>()
    }

    private fun Database.Read.getMonthlyAssistanceCountRows(
        reportingDays: List<LocalDate>
    ): List<MonthlyAssistanceResult> {
        return createQuery {
                sql(
                    """
SELECT extract(MONTH FROM day)     as month,
       count(distinct pl.child_id) as assistance_count
FROM unnest(${bind(reportingDays)}::date[]) day
         JOIN placement pl
              ON daterange(pl.start_date, pl.end_date, '[]') @> day
         JOIN daycare d ON pl.unit_id = d.id
WHERE pl.type = ANY ('{DAYCARE,PRESCHOOL_DAYCARE,PRESCHOOL_DAYCARE_ONLY}')
  AND (EXISTS (SELECT FROM assistance_factor af WHERE af.child_id = pl.child_id AND af.valid_during @> day)
    OR EXISTS (SELECT
               FROM assistance_action an
                        JOIN assistance_action_option_ref anor ON anor.action_id = an.id
                        JOIN assistance_action_option aao ON anor.option_id = aao.id
               WHERE aao.value = ANY ('{10,40}')
                 AND an.child_id = pl.child_id))
GROUP BY day
                """
                )
            }
            .toList<MonthlyAssistanceResult>()
    }

    private fun Database.Read.getMonthlyFamilyDaycareCounts(
        statDays: List<LocalDate>,
        range: FiniteDateRange,
    ): List<MonthlyFamilyDaycareResult> {
        return createQuery {
                sql(
                    """
WITH child_rows AS (SELECT pl.child_id,
                           p.date_of_birth,
                           daterange(pl.start_date, pl.end_date, '[]') * ${bind(range)} AS validity
                    FROM placement pl
                             JOIN person p ON p.id = pl.child_id
                             JOIN daycare d ON pl.unit_id = d.id
                    WHERE pl.type = ANY ('{DAYCARE,PRESCHOOL_DAYCARE,PRESCHOOL_DAYCARE_ONLY}')
                      AND d.type && '{FAMILY,GROUP_FAMILY}'::care_types[]
                      AND daterange(pl.start_date, pl.end_date, '[]') && ${bind(range)})
SELECT extract(MONTH FROM day)                                             AS month,
       count(DISTINCT row.child_id)
       FILTER ( WHERE date_part('year', age(day, row.date_of_birth)) < 3)  AS family_under_3_count,
       count(DISTINCT row.child_id)
       FILTER ( WHERE date_part('year', age(day, row.date_of_birth)) >= 3) AS family_over_3_count
FROM unnest(${bind(statDays)}::date[]) day
         LEFT JOIN child_rows row on validity @> day
GROUP BY month
                """
                )
            }
            .toList<MonthlyFamilyDaycareResult>()
    }
}
