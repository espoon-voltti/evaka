// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.absence.getAbsences
import fi.espoo.evaka.attendance.ChildAttendanceRow
import fi.espoo.evaka.attendance.getChildAttendances
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getGroupPlacementsByChildren
import fi.espoo.evaka.reservations.ReservationRow
import fi.espoo.evaka.reservations.computeUsedService
import fi.espoo.evaka.reservations.getReservations
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ExceededServiceNeedsReportController(
    private val accessControl: AccessControl,
    private val env: EvakaEnv
) {
    @GetMapping(
        "/reports/exceeded-service-need/units", // deprecated
        "/employee/reports/exceeded-service-need/units"
    )
    fun getExceededServiceNeedReportUnits(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): List<ExceededServiceNeedReportUnit> {
        return db.connect { dbc ->
            dbc.read { tx ->
                val filter =
                    accessControl.requireAuthorizationFilter(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_EXCEEDED_SERVICE_NEEDS_REPORT
                    )
                tx.getExceededServiceNeedReportUnits(filter)
            }
        }
    }

    @GetMapping("/reports/exceeded-service-need/rows")
    fun getExceededServiceNeedReportRows(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): List<ExceededServiceNeedReportRow> {
        return db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.READ_EXCEEDED_SERVICE_NEEDS_REPORT,
                    unitId
                )
                exceededServiceNeedReport(
                    tx,
                    clock.today(),
                    unitId,
                    year,
                    month,
                    env.forceMajeureAbsenceDaysCalculatedAsUsedServiceNeed
                )
            }
        }
    }
}

data class ExceededServiceNeedReportUnit(val id: DaycareId, val name: String)

private fun Database.Read.getExceededServiceNeedReportUnits(
    unitFilter: AccessControlFilter<DaycareId>
): List<ExceededServiceNeedReportUnit> =
    createQuery {
            sql(
                """
                SELECT id, name
                FROM daycare
                WHERE ${predicate(unitFilter.forTable("daycare"))}
                ORDER BY name
                """
            )
        }
        .toList()

data class ExceededServiceNeedReportRow(
    val childId: ChildId,
    val childFirstName: String,
    val childLastName: String,
    val groupId: GroupId?,
    val unitId: DaycareId,
    val serviceNeedHoursPerMonth: Int,
    val usedServiceHours: Int,
    val excessHours: Int,
    val groupName: String?
)

private fun exceededServiceNeedReport(
    tx: Database.Read,
    today: LocalDate,
    unitId: DaycareId,
    year: Int,
    month: Int,
    forceMajeureAbsenceDaysCalculatedAsUsedServiceNeed: Boolean
): List<ExceededServiceNeedReportRow> {
    val range = FiniteDateRange.ofMonth(year, Month.of(month))

    val daycare = tx.getDaycare(unitId) ?: throw BadRequest("Daycare $unitId not found")

    val holidays = tx.getHolidays(range)
    val serviceNeeds = tx.getServiceNeedsByRange(unitId, range)
    val childIds = serviceNeeds.keys

    val childGroups = tx.getGroupPlacementsByChildren(childIds, range)
    val children = tx.getChildren(childIds)
    val absences = tx.getAbsencesByRange(childIds, range)
    val reservations = tx.getReservationsByRange(childIds, range)
    val attendances = tx.getAttendancesByRange(childIds, range)

    val serviceUsagesByChild =
        range
            .dates()
            .flatMap { date ->
                children.mapNotNull { child ->
                    val serviceNeed =
                        serviceNeeds[child.id]?.find { it.period.includes(date) }
                            ?: return@mapNotNull null
                    val childAbsences = absences[child.id to date] ?: emptyList()
                    val childReservations =
                        (reservations[child.id to date] ?: emptyList()).mapNotNull {
                            it.reservation.asTimeRange()
                        }
                    val childAttendances =
                        (attendances[child.id to date] ?: emptyList()).map { it.asTimeInterval() }
                    val isOperationDayForChild =
                        when (serviceNeed.shiftCare) {
                            ShiftCareType.NONE ->
                                daycare.operationDays.contains(date.dayOfWeek.value) &&
                                    !holidays.contains(date)
                            ShiftCareType.INTERMITTENT,
                            ShiftCareType.FULL ->
                                (daycare.shiftCareOperationDays ?: daycare.operationDays).contains(
                                    date.dayOfWeek.value
                                ) && (daycare.shiftCareOpenOnHolidays || !holidays.contains(date))
                        }
                    child.id to
                        computeUsedService(
                            today = today,
                            date = date,
                            serviceNeedHours = serviceNeed.daycareHoursPerMonth,
                            placementType = serviceNeed.placementType,
                            preschoolTime = serviceNeed.dailyPreschoolTime,
                            preparatoryTime = serviceNeed.dailyPreparatoryTime,
                            isOperationDay = isOperationDayForChild,
                            shiftCareType = serviceNeed.shiftCare,
                            absences = childAbsences,
                            reservations = childReservations,
                            attendances = childAttendances,
                            forceMajeureAbsenceDaysCalculatedAsUsedServiceNeed
                        )
                }
            }
            .fold(mutableMapOf<ChildId, Long>()) { acc, (childId, usedService) ->
                val cur = acc[childId] ?: 0
                acc[childId] = cur + usedService.usedServiceMinutes
                acc
            }

    return children
        .map { child ->
            val serviceNeedHoursPerMonth =
                serviceNeeds[child.id]?.first()?.daycareHoursPerMonth ?: 0
            val usedServiceHours =
                BigDecimal(serviceUsagesByChild[child.id] ?: 0)
                    .divide(BigDecimal(60), RoundingMode.FLOOR)
                    .toInt()
            val excessHours = usedServiceHours - serviceNeedHoursPerMonth
            ExceededServiceNeedReportRow(
                childId = child.id,
                childFirstName = child.firstName,
                childLastName = child.lastName,
                unitId = unitId,
                groupId = childGroups[child.id]?.groupId,
                groupName = childGroups[child.id]?.groupName,
                serviceNeedHoursPerMonth = serviceNeedHoursPerMonth,
                usedServiceHours = usedServiceHours,
                excessHours = excessHours
            )
        }
        .filter { it.usedServiceHours > it.serviceNeedHoursPerMonth }
        .sortedBy { -it.excessHours }
}

private data class ReportChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
)

private fun Database.Read.getChildren(childIds: Set<ChildId>): List<ReportChild> =
    createQuery {
            sql(
                """
                SELECT id, first_name, last_name
                FROM person
                WHERE id = ANY (${bind(childIds)})
                """
            )
        }
        .toList<ReportChild>()

private data class ReportServiceNeed(
    val childId: ChildId,
    val period: FiniteDateRange,
    val placementType: PlacementType,
    val dailyPreschoolTime: TimeRange?,
    val dailyPreparatoryTime: TimeRange?,
    val shiftCare: ShiftCareType,
    val daycareHoursPerMonth: Int,
)

private fun Database.Read.getServiceNeedsByRange(
    unitId: DaycareId,
    range: FiniteDateRange
): Map<ChildId, List<ReportServiceNeed>> =
    createQuery {
            sql(
                """
                SELECT 
                    pl.child_id,
                    daterange(sn.start_date, sn.end_date, '[]') AS period,
                    pl.type AS placement_type,
                    u.daily_preschool_time,
                    u.daily_preparatory_time,
                    sn.shift_care,
                    sno.daycare_hours_per_month
                FROM daycare u
                JOIN placement pl ON pl.unit_id = u.id
                JOIN service_need sn ON sn.placement_id = pl.id
                JOIN service_need_option sno ON sno.id = sn.option_id
                WHERE
                    u.id = ${bind(unitId)} AND
                    daterange(sn.start_date, sn.end_date) && ${bind(range)} AND
                    sno.daycare_hours_per_month IS NOT NULL
                ORDER BY sn.start_date
                """
            )
        }
        .toList<ReportServiceNeed>()
        .groupBy { it.childId }

private fun Database.Read.getAbsencesByRange(
    childIds: Set<ChildId>,
    range: FiniteDateRange
): Map<Pair<ChildId, LocalDate>, List<Pair<AbsenceType, AbsenceCategory>>> =
    getAbsences(
            Predicate {
                where(
                    """
                        $it.child_id = ANY (${bind(childIds)}) AND
                        between_start_and_end(${bind(range)}, $it.date)
                        """
                )
            }
        )
        .groupBy({ it.childId to it.date }, { it.absenceType to it.category })

fun Database.Read.getAttendancesByRange(
    childIds: Set<ChildId>,
    range: FiniteDateRange
): Map<Pair<ChildId, LocalDate>, List<ChildAttendanceRow>> =
    getChildAttendances(
            Predicate {
                where(
                    // There's no filtering by unit, because we want to include e.g. backup care
                    // attendances
                    """
                        $it.child_id = ANY (${bind(childIds)}) AND
                        between_start_and_end(${bind(range)}, $it.date)
                        """
                )
            }
        )
        .groupBy { it.childId to it.date }

fun Database.Read.getReservationsByRange(
    childIds: Set<ChildId>,
    range: FiniteDateRange
): Map<Pair<ChildId, LocalDate>, List<ReservationRow>> =
    getReservations(
            Predicate {
                where(
                    """
                    $it.child_id = ANY (${bind(childIds)}) AND
                    between_start_and_end(${bind(range)}, $it.date)
                    """
                )
            }
        )
        .groupBy { it.childId to it.date }
