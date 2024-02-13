// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FamilyDaycareMealReport(private val accessControl: AccessControl) {
    @GetMapping("/reports/family-daycare-meal-count")
    fun getFamilyDaycareMealReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): FamilyDaycareMealReportResult {
        if (endDate < startDate) throw BadRequest("Start date must be before or equal to end date")
        if (endDate.minusMonths(6).isAfter(startDate)) throw BadRequest("Maximum date range is 6kk")
        val defaultMealTimes =
            MealReportConfig(
                breakfastTime = TimeRange(LocalTime.of(8, 0, 0, 0), LocalTime.of(8, 45, 0, 0)),
                lunchTime = TimeRange(LocalTime.of(10, 30, 0, 0), LocalTime.of(12, 30, 0, 0)),
                snackTime = TimeRange(LocalTime.of(13, 45, 0, 0), LocalTime.of(15, 0, 0, 0))
            )
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_FAMILY_DAYCARE_MEAL_REPORT
                        )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getFamilyDaycareMealReportRows(filter, startDate, endDate, defaultMealTimes)
                }
            }
            .also { Audit.FamilyDaycareMealReport.log() }
    }

    private fun Database.Read.getFamilyDaycareMealReportRows(
        daycareFilter: AccessControlFilter<DaycareId>,
        startDate: LocalDate,
        endDate: LocalDate,
        mealTimes: MealReportConfig
    ): FamilyDaycareMealReportResult {
        val resultRows =
            createQuery<DatabaseTable> {
                    sql(
                        """
select a.id                                                          as area_id,
       a.name                                                        as area_name,
       d.id                                                          as daycare_id,
       d.name                                                        as daycare_name,
       p.id                                                          as child_id,
       p.first_name,
       p.last_name,
       coalesce(sum(case when day_count.breakfastCount > 0 then 1 else 0 end), 0) as breakfastCount,
       coalesce(sum(case when day_count.lunchCount > 0 then 1 else 0 end), 0)     as lunchCount,
       coalesce(sum(case when day_count.snackCount > 0 then 1 else 0 end), 0)     as snackCount
from generate_series(${bind(startDate)}::date, ${bind(endDate)}::date, '1 day') day
         join LATERAL (
    select ca.child_id,
           ca.date,
           ca.unit_id,
           d.care_area_id,
           coalesce(count(ca.child_id)
                    FILTER (WHERE tsrange(ca.date + ca.start_time, ca.date + ca.end_time) &&
                                  tsrange(ca.date + ${bind(mealTimes.breakfastTime.start)}, ca.date + ${bind(mealTimes.breakfastTime.end)})),
                    0) as breakfastCount,
           coalesce(count(ca.child_id)
                    FILTER (WHERE tsrange(ca.date + ca.start_time, ca.date + ca.end_time) &&
                                  tsrange(ca.date + ${bind(mealTimes.lunchTime.start)}, ca.date + ${bind(mealTimes.lunchTime.end)})),
                    0) as lunchCount,
           coalesce(count(ca.child_id)
                    FILTER (WHERE tsrange(ca.date + ca.start_time, ca.date + ca.end_time) &&
                                  tsrange(ca.date + ${bind(mealTimes.snackTime.start)}, ca.date + ${bind(mealTimes.snackTime.end)})),
                    0) as snackCount
    from child_attendance ca
             join placement pl on ca.child_id = pl.child_id and
                                  daterange(pl.start_date, pl.end_date, '[]') @> ca.date and
                                  pl.unit_id = ca.unit_id
             join daycare d on ca.unit_id = d.id
             join person p on ca.child_id = p.id
    where 'FAMILY' = ANY (d.type)
      AND ${predicate(daycareFilter.forTable("d"))}
      and daterange(${bind(startDate)}, ${bind(endDate)}, '[]') @> ca.date
    group by ca.child_id, ca.date, ca.unit_id, d.care_area_id
    ) day_count on day::date = day_count.date
         join daycare d on d.id = day_count.unit_id
         join care_area a on d.care_area_id = a.id
         join person p on p.id = day_count.child_id
GROUP BY ROLLUP ((a.id, a.name), (d.id, d.name), (p.id, p.first_name, p.last_name))
ORDER BY a.name, d.name, p.last_name, p.first_name, p.id ASC;

            """
                            .trimIndent()
                    )
                }
                .toList<FamilyDaycareMealReportRow>()

        val childrenByDaycare = mutableMapOf<String, MutableList<FamilyDaycareMealChildResult>>()
        val daycaresByArea = mutableMapOf<String, MutableList<FamilyDaycareMealDaycareResult>>()
        val collectedAreaResults = mutableListOf<FamilyDaycareMealAreaResult>()
        var totalResult =
            FamilyDaycareMealReportResult(
                areaResults = emptyList(),
                breakfastCount = 0,
                lunchCount = 0,
                snackCount = 0
            )

        resultRows.forEach { row ->
            if (row.firstName != null && row.childId != null && row.lastName != null) {
                val childList =
                    childrenByDaycare.getOrPut(row.daycareId.toString()) { mutableListOf() }
                childList.add(
                    FamilyDaycareMealChildResult(
                        childId = row.childId,
                        firstName = row.firstName,
                        lastName = row.lastName,
                        breakfastCount = row.breakfastCount,
                        lunchCount = row.lunchCount,
                        snackCount = row.snackCount
                    )
                )
            } else if (row.daycareId != null && row.daycareName != null) {
                val daycareList = daycaresByArea.getOrPut(row.areaId.toString()) { mutableListOf() }
                daycareList.add(
                    FamilyDaycareMealDaycareResult(
                        daycareId = row.daycareId,
                        daycareName = row.daycareName,
                        breakfastCount = row.breakfastCount,
                        lunchCount = row.lunchCount,
                        snackCount = row.snackCount,
                        childResults = childrenByDaycare[row.daycareId.toString()].orEmpty()
                    )
                )
            } else if (row.areaId != null && row.areaName != null) {
                collectedAreaResults.add(
                    FamilyDaycareMealAreaResult(
                        areaId = row.areaId,
                        areaName = row.areaName,
                        breakfastCount = row.breakfastCount,
                        lunchCount = row.lunchCount,
                        snackCount = row.snackCount,
                        daycareResults = daycaresByArea[row.areaId.toString()].orEmpty()
                    )
                )
            } else {
                totalResult =
                    FamilyDaycareMealReportResult(
                        breakfastCount = row.breakfastCount,
                        lunchCount = row.lunchCount,
                        snackCount = row.snackCount,
                        areaResults = collectedAreaResults
                    )
            }
        }

        return totalResult
    }

    data class FamilyDaycareMealReportRow(
        val daycareName: String?,
        val firstName: String?,
        val lastName: String?,
        val areaName: String?,
        val childId: PersonId?,
        val daycareId: DaycareId?,
        val areaId: AreaId?,
        val breakfastCount: Int,
        val lunchCount: Int,
        val snackCount: Int
    )

    data class FamilyDaycareMealReportResult(
        val breakfastCount: Int,
        val lunchCount: Int,
        val snackCount: Int,
        val areaResults: List<FamilyDaycareMealAreaResult>
    )

    data class FamilyDaycareMealAreaResult(
        val areaId: AreaId,
        val areaName: String,
        val breakfastCount: Int,
        val lunchCount: Int,
        val snackCount: Int,
        val daycareResults: List<FamilyDaycareMealDaycareResult>
    )

    data class FamilyDaycareMealDaycareResult(
        val daycareId: DaycareId,
        val daycareName: String,
        val breakfastCount: Int,
        val lunchCount: Int,
        val snackCount: Int,
        val childResults: List<FamilyDaycareMealChildResult>
    )

    data class FamilyDaycareMealChildResult(
        val childId: PersonId,
        val firstName: String,
        val lastName: String,
        val breakfastCount: Int,
        val lunchCount: Int,
        val snackCount: Int,
    )

    data class MealReportConfig(
        val breakfastTime: TimeRange,
        val lunchTime: TimeRange,
        val snackTime: TimeRange
    )
}
