// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.absence.getAbsences
import fi.espoo.evaka.attendance.occupancyCoefficientSeven
import fi.espoo.evaka.backupcare.getBackupCaresForDaycare
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.document.childdocument.ChildBasics
import fi.espoo.evaka.holidayperiod.getHolidayPeriod
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.getReservationBackupPlacements
import fi.espoo.evaka.reservations.getReservations
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import kotlin.math.ceil
import org.jdbi.v3.core.mapper.Nested
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class HolidayPeriodAttendanceReport(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/holiday-period-attendance")
    fun getHolidayPeriodAttendanceReport(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam unitId: DaycareId,
        @RequestParam periodId: HolidayPeriodId,
    ): List<HolidayPeriodAttendanceReportRow> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_HOLIDAY_PERIOD_ATTENDANCE_REPORT,
                        unitId,
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)

                    val unit = tx.getDaycare(unitId) ?: throw BadRequest("No such unit")
                    val unitOperationDays = unit.shiftCareOperationDays ?: unit.operationDays

                    val holidayPeriod =
                        tx.getHolidayPeriod(periodId) ?: throw BadRequest("No such holiday period")
                    val holidays = tx.getHolidays(holidayPeriod.period)

                    // report result days
                    val periodDays =
                        holidayPeriod.period
                            .dates()
                            .map { PeriodDay(it, holidays.contains(it)) }
                            .filter {
                                unitOperationDays.contains(it.date.dayOfWeek.value) &&
                                    (unit.shiftCareOpenOnHolidays || !it.isHoliday)
                            }
                            .toList()

                    // incoming back up children
                    val backupCareIncoming =
                        tx.getBackupCaresForDaycare(unit.id, holidayPeriod.period)
                    val backupChildrenInUnit = backupCareIncoming.map { it.child.id }.toSet()

                    val backupChildDataByChild =
                        tx.getServiceNeedOccupancyInfoOverRangeForChildren(
                                backupChildrenInUnit,
                                holidayPeriod.period,
                            )
                            .groupBy { it.child.id }

                    // directly placed children
                    val directlyPlacedChildData =
                        tx.getServiceNeedOccupancyInfoOverRangeForUnit(
                            unit.id,
                            holidayPeriod.period,
                        )
                    val directlyPlacedChildDataByChild =
                        directlyPlacedChildData.map { it.child.id }.toSet()

                    // outgoing backup children
                    // TODO: is RESERVATIONS unit feature requirement for the other unit ok?
                    val backupCareOutgoing =
                        tx.getReservationBackupPlacements(
                            directlyPlacedChildDataByChild,
                            holidayPeriod.period,
                        )

                    // full absence data
                    // TODO: RESERVATIONS unit feature does not matter for absences?
                    val fullAbsenceDataByDate =
                        tx.getAbsences(
                                Predicate {
                                    where(
                                        "between_start_and_end(${bind(holidayPeriod.period)}, $it.date) AND $it.child_id = ANY (${bind(directlyPlacedChildDataByChild + backupChildrenInUnit)})"
                                    )
                                }
                            )
                            .groupBy { r -> r.date }

                    // full reservation data
                    // TODO: RESERVATIONS unit feature is required for reservations?
                    val fullReservationDataByDateAndChild =
                        tx.getReservations(
                                Predicate {
                                    where(
                                        "between_start_and_end(${bind(holidayPeriod.period)}, $it.date) AND $it.child_id = ANY (${bind(directlyPlacedChildDataByChild + backupChildrenInUnit)})"
                                    )
                                }
                            )
                            .groupBy { Pair(it.date, it.childId) }

                    // full assistance factor data
                    val assistanceFactorsByChild =
                        tx.getAssistanceFactorsForChildrenOverPeriod(
                                directlyPlacedChildDataByChild + backupChildrenInUnit,
                                holidayPeriod.period,
                            )
                            .groupBy { it.childId }

                    // collect daily report values
                    periodDays.map { (date, isHoliday) ->
                        val dailyDirectlyPlacedData =
                            directlyPlacedChildData.filter { sn ->
                                sn.validity.includes(date) &&
                                    (backupCareOutgoing[sn.child.id] ?: emptyList()).none {
                                        it.range.includes(date)
                                    }
                            }
                        val dailyBackupPlacedData =
                            backupCareIncoming
                                .filter { it.period.includes(date) }
                                .mapNotNull {
                                    backupChildDataByChild[it.child.id]?.first { sn ->
                                        sn.validity.includes(date)
                                    }
                                }

                        val dailyPlacedData = dailyDirectlyPlacedData + dailyBackupPlacedData
                        val dailyPlacedDataByChild = dailyPlacedData.groupBy { it.child.id }

                        val dailyAbsencesByChild =
                            fullAbsenceDataByDate[date]
                                ?.groupBy { it.childId }
                                ?.filter { (key, childDailyAbsenceData) ->
                                    val childDailyPlacementData =
                                        dailyPlacedDataByChild[key]?.first {
                                            it.validity.includes(date)
                                        } ?: return@filter false
                                    childDailyAbsenceData.map { a -> a.category }.size ==
                                        childDailyPlacementData.placementType
                                            .absenceCategories()
                                            .size
                                } ?: emptyMap()

                        val dailyExpectedAtUnitData =
                            dailyPlacedData.filter { sn ->
                                !dailyAbsencesByChild.containsKey(sn.child.id)
                            }

                        val (confirmedPresent, noResponses) =
                            dailyExpectedAtUnitData.partition {
                                fullReservationDataByDateAndChild.containsKey(
                                    Pair(date, it.child.id)
                                )
                            }
                        val confirmedWithAssistanceFactors =
                            confirmedPresent.map { sn ->
                                val af =
                                    assistanceFactorsByChild[sn.child.id]
                                        ?.first { af -> af.period.includes(date) }
                                        ?.capacityFactor
                                Pair(sn, af)
                            }

                        val dailyOccupancyCoefficient =
                            confirmedWithAssistanceFactors.sumOf {
                                val ageAtDate =
                                    Period.between(it.first.child.dateOfBirth, date).years
                                val assistanceFactor = it.second ?: 1.0
                                if (ageAtDate < 3) it.first.coefficientUnder3y * assistanceFactor
                                else it.first.coefficient * assistanceFactor
                            }
                        val staffNeedAtDate =
                            ceil(
                                    dailyOccupancyCoefficient.div(
                                        occupancyCoefficientSeven.toDouble()
                                    )
                                )
                                .toInt()

                        HolidayPeriodAttendanceReportRow(
                            date = date,
                            presentChildren =
                                confirmedPresent.map { (child) ->
                                    ChildWithName(
                                        id = child.id,
                                        firstName = child.firstName,
                                        lastName = child.lastName,
                                    )
                                },
                            assistanceChildren =
                                confirmedWithAssistanceFactors
                                    .filter { it.second != null }
                                    .map { (data) ->
                                        ChildWithName(
                                            id = data.child.id,
                                            firstName = data.child.firstName,
                                            lastName = data.child.lastName,
                                        )
                                    },
                            presentOccupancyCoefficient = dailyOccupancyCoefficient,
                            absentCount = dailyAbsencesByChild.size,
                            requiredStaff = staffNeedAtDate,
                            noResponseChildren =
                                noResponses
                                    // expect a weekend/holiday response only if full shift care
                                    .filter {
                                        (it.shiftCareType != ShiftCareType.FULL &&
                                            date.dayOfWeek < DayOfWeek.SATURDAY &&
                                            !isHoliday) || it.shiftCareType == ShiftCareType.FULL
                                    }
                                    .map { (child) ->
                                        ChildWithName(
                                            id = child.id,
                                            firstName = child.firstName,
                                            lastName = child.lastName,
                                        )
                                    },
                        )
                    }
                }
            }
            .also {
                Audit.HolidayPeriodAttendanceReport.log(
                    meta = mapOf("unitId" to unitId, "periodId" to periodId)
                )
            }
    }
}

data class PeriodDay(val date: LocalDate, val isHoliday: Boolean)

data class ChildWithName(val id: PersonId, val firstName: String, val lastName: String)

data class HolidayPeriodAttendanceReportRow(
    val date: LocalDate,
    val presentChildren: List<ChildWithName>,
    val assistanceChildren: List<ChildWithName>,
    val presentOccupancyCoefficient: Double,
    val requiredStaff: Int,
    val absentCount: Int,
    val noResponseChildren: List<ChildWithName>,
)

private data class AssistanceFactorRow(
    val childId: PersonId,
    val capacityFactor: Double,
    val period: FiniteDateRange,
)

private fun Database.Read.getAssistanceFactorsForChildrenOverPeriod(
    childIds: Set<PersonId>,
    period: FiniteDateRange,
): List<AssistanceFactorRow> =
    createQuery {
            sql(
                """
SELECT af.child_id,
       af.capacity_factor,
       af.valid_during * ${bind(period)} AS period
FROM assistance_factor af
WHERE af.child_id = ANY(${bind(childIds)})
  AND af.valid_during && ${bind(period)}
        """
            )
        }
        .toList<AssistanceFactorRow>()

private data class ChildServiceNeedOccupancyInfo(
    @Nested("child_") val child: ChildBasics,
    val placementType: PlacementType,
    val coefficientUnder3y: Double,
    val coefficient: Double,
    val validity: FiniteDateRange,
    val shiftCareType: ShiftCareType,
)

private fun Database.Read.getServiceNeedOccupancyInfoOverRangeForUnit(
    daycareId: DaycareId,
    range: FiniteDateRange,
) =
    getServiceNeedOccupancyInfoOverRange(
        Predicate { where("pl.unit_id = ${bind(daycareId)}") },
        range,
    )

private fun Database.Read.getServiceNeedOccupancyInfoOverRangeForChildren(
    childIds: Set<PersonId>,
    range: FiniteDateRange,
) =
    if (childIds.isEmpty()) emptyList()
    else
        getServiceNeedOccupancyInfoOverRange(
            Predicate { where("pl.child_id = ANY (${bind(childIds)})") },
            range,
        )

private fun Database.Read.getServiceNeedOccupancyInfoOverRange(
    where: Predicate,
    period: FiniteDateRange,
): List<ChildServiceNeedOccupancyInfo> =
    createQuery {
            sql(
                """
SELECT p.id                                                          AS child_id,
       p.first_name                                                  AS child_first_name,
       p.last_name                                                   AS child_last_name,
       p.date_of_birth                                               AS child_date_of_birth,
       pl.type                                                       AS placement_type,
       coalesce(sno.realized_occupancy_coefficient_under_3y,
                default_sno.realized_occupancy_coefficient_under_3y) AS coefficient_under_3y,
       coalesce(sno.realized_occupancy_coefficient,
                default_sno.realized_occupancy_coefficient)          AS coefficient,
       CASE
           WHEN (sn.start_date IS NOT NULL)
               THEN daterange(sn.start_date, sn.end_date, '[]')
           ELSE daterange(pl.start_date, pl.end_date, '[]')
           END                                                       AS validity,
       coalesce(sn.shift_care, 'NONE')                               AS shift_care_type
FROM placement pl
         JOIN person p ON pl.child_id = p.id
         LEFT JOIN service_need sn
                   ON sn.placement_id = pl.id
                       AND daterange(sn.start_date, sn.end_date, '[]') && ${bind(period)}
         LEFT JOIN service_need_option sno ON sn.option_id = sno.id
         LEFT JOIN service_need_option default_sno
                   ON pl.type = default_sno.valid_placement_type
                       AND default_sno.default_option
WHERE ${predicate(where.forTable(""))} 
AND daterange(pl.start_date, pl.end_date, '[]') && ${bind(period)}
        """
            )
        }
        .toList<ChildServiceNeedOccupancyInfo>()
