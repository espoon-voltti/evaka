package fi.espoo.evaka.reports

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.DateMap
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class RegionalCensusReportRow(
    val childId: PersonId,
    val dateOfBirth: LocalDate,
    val validity: FiniteDateRange,
    val monthlyHours: Int?,
    val monthlyDays: Int?,
)

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
                    tx.getMunicipalDaycareMonthlyResults(range, reportingDays).associateBy {
                        it.month
                    }
                val familyDaycareResults =
                    tx.getFamilyMonthlyResults(range, reportingDays).associateBy { it.month }
                val municipalShiftCareResults =
                    tx.getMunicipalShiftCareMonthlyResults(range, reportingDays).associateBy {
                        it.month
                    }
                val assistanceResults =
                    tx.getMunicipalAssistanceMonthlyResults(reportingDays).associateBy { it.month }

                val monthlyResults =
                    (1..12).map {
                        RegionalSurveyMonthlyResults(
                            month = it,
                            familyDaycareResults =
                                familyDaycareResults[it] ?: MonthlyFamilyDaycareResult(month = it),
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
    }

    private fun Database.Read.getMunicipalDaycareMonthlyResults(
        range: FiniteDateRange,
        reportingDays: List<LocalDate>,
    ): List<MonthlyMunicipalDaycareResult> {
        val data =
            getYearlyRegionalReportRows(
                    range = range,
                    daycarePred =
                        Predicate {
                            where(
                                "$it.provider_type = ANY ('{MUNICIPAL}') AND $it.type && '{CENTRE}'::care_types[]"
                            )
                        },
                    placementPred =
                        Predicate {
                            where(
                                "$it.type = ANY ('{DAYCARE,PRESCHOOL_DAYCARE,PRESCHOOL_DAYCARE_ONLY}')"
                            )
                        },
                )
                .groupBy { it.childId }
                .mapValues { entry -> DateMap.of(entry.value.map { it.validity to it }) }

        return reportingDays.map { day ->
            val monthlyData = data.mapNotNull { it.value.getValue(day) }
            val (partTime, fullTime) = monthlyData.partition(::isPartTime)
            val (under3PartTime, over3PartTime) = partTime.partition { isUnder3(it, day) }
            val (under3FullTime, over3FullTime) = fullTime.partition { isUnder3(it, day) }
            MonthlyMunicipalDaycareResult(
                month = day.monthValue,
                municipalOver3PartTimeCount = over3PartTime.size,
                municipalUnder3PartTimeCount = under3PartTime.size,
                municipalOver3FullTimeCount = over3FullTime.size,
                municipalUnder3FullTimeCount = under3FullTime.size,
            )
        }
    }

    private fun Database.Read.getFamilyMonthlyResults(
        range: FiniteDateRange,
        reportingDays: List<LocalDate>,
    ): List<MonthlyFamilyDaycareResult> {
        val data =
            getYearlyRegionalReportRows(
                    range = range,
                    daycarePred =
                        Predicate { where("$it.type && '{FAMILY,GROUP_FAMILY}'::care_types[]") },
                    placementPred = Predicate.alwaysTrue(),
                )
                .groupBy { it.childId }
                .mapValues { entry -> DateMap.of(entry.value.map { it.validity to it }) }

        return reportingDays.map { day ->
            val monthlyData = data.mapNotNull { it.value.getValue(day) }
            val (under3, over3) = monthlyData.partition { isUnder3(it, day) }
            MonthlyFamilyDaycareResult(
                month = day.monthValue,
                familyOver3Count = over3.size,
                familyUnder3Count = under3.size,
            )
        }
    }

    private fun Database.Read.getMunicipalShiftCareMonthlyResults(
        range: FiniteDateRange,
        reportingDays: List<LocalDate>,
    ): List<MonthlyMunicipalShiftCareResult> {
        val data =
            getYearlyRegionalReportRows(
                    range = range,
                    daycarePred =
                        Predicate {
                            where(
                                "$it.provider_type = ANY ('{MUNICIPAL}') AND $it.type && '{CENTRE}'::care_types[]"
                            )
                        },
                    placementPred =
                        Predicate {
                            where(
                                "$it.type = ANY ('{DAYCARE,PRESCHOOL_DAYCARE,PRESCHOOL_DAYCARE_ONLY}')"
                            )
                        },
                    serviceNeedPred =
                        Predicate { where("$it.shift_care = ANY ('{FULL,INTERMITTENT}')") },
                )
                .groupBy { it.childId }
                .mapValues { entry -> DateMap.of(entry.value.map { it.validity to it }) }

        return reportingDays.map { day ->
            val monthlyData = data.mapNotNull { it.value.getValue(day) }
            MonthlyMunicipalShiftCareResult(
                month = day.monthValue,
                municipalShiftCareCount = monthlyData.size,
            )
        }
    }

    private fun Database.Read.getMunicipalAssistanceMonthlyResults(
        reportingDays: List<LocalDate>
    ): List<MonthlyAssistanceResult> {
        val data =
            getMonthlyAssistanceCountRows(
                    reportingDays = reportingDays,
                    placementPred =
                        Predicate {
                            where(
                                "$it.type = ANY ('{DAYCARE,PRESCHOOL_DAYCARE,PRESCHOOL_DAYCARE_ONLY}')"
                            )
                        },
                )
                .associateBy { it.month }

        return (1..12).map { month ->
            val monthlyData = data[month]
            MonthlyAssistanceResult(
                month = month,
                assistanceCount = monthlyData?.assistanceCount ?: 0,
            )
        }
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

    data class MonthlyMunicipalDaycareAssistanceRow(val month: Int, val assistanceCount: Int)

    private fun isPartTime(row: RegionalCensusReportRow) =
        (row.monthlyHours != null && row.monthlyHours < 86) ||
            (row.monthlyDays != null && row.monthlyDays < 11)

    private fun isUnder3(row: RegionalCensusReportRow, examDate: LocalDate) =
        Period.between(row.dateOfBirth, examDate).years < 3

    private fun Database.Read.getYearlyRegionalReportRows(
        range: FiniteDateRange,
        daycarePred: Predicate,
        placementPred: Predicate,
        serviceNeedPred: Predicate = Predicate.alwaysTrue(),
    ): List<RegionalCensusReportRow> {
        return createQuery {
                sql(
                    """
SELECT pl.child_id,
       p.date_of_birth,
       coalesce(sno.daycare_hours_per_month, default_sno.daycare_hours_per_month) as monthly_hours,
       coalesce(sno.contract_days_per_month, default_sno.contract_days_per_month) as monthly_days,
       CASE
           WHEN (sn.start_date IS NOT NULL)
               THEN daterange(sn.start_date, sn.end_date, '[]') * ${bind(range)}
           ELSE daterange(pl.start_date, pl.end_date, '[]') * ${bind(range)}
           END                                                                    as validity
FROM placement pl
         JOIN person p ON pl.child_id = p.id
         JOIN daycare d ON pl.unit_id = d.id
         LEFT JOIN service_need sn
                   ON sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') && ${bind(range)}
         LEFT JOIN service_need_option sno ON sn.option_id = sno.id
         LEFT JOIN service_need_option default_sno
                   ON pl.type = default_sno.valid_placement_type AND default_sno.default_option
WHERE ${predicate(daycarePred.forTable("d"))}
AND ${predicate(placementPred.forTable("pl"))}
AND ${predicate(serviceNeedPred.forTable("sn"))}
AND daterange(pl.start_date, pl.end_date, '[]') && ${bind(range)}
                """
                )
            }
            .toList<RegionalCensusReportRow>()
    }

    private fun Database.Read.getMonthlyAssistanceCountRows(
        reportingDays: List<LocalDate>,
        placementPred: Predicate,
    ): List<MonthlyMunicipalDaycareAssistanceRow> {
        return createQuery {
                sql(
                    """
SELECT extract(MONTH FROM day)     as month,
       count(distinct pl.child_id) as assistance_count
FROM unnest(${bind(reportingDays)}::date[]) day
         JOIN placement pl
              ON daterange(pl.start_date, pl.end_date, '[]') @> day
         JOIN daycare d ON pl.unit_id = d.id
WHERE ${predicate(placementPred.forTable("pl"))}
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
            .toList<MonthlyMunicipalDaycareAssistanceRow>()
    }
}
