// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.absence.getAbsences
import fi.espoo.evaka.assistance.getAssistanceFactorsForChildrenOverRange
import fi.espoo.evaka.attendance.occupancyCoefficientSeven
import fi.espoo.evaka.backupcare.getBackupCaresForDaycare
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.document.childdocument.ChildBasics
import fi.espoo.evaka.holidayperiod.getHolidayPeriod
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
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
import fi.espoo.evaka.shared.domain.getOperationalDatesForChildren
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
                    val holidays = getHolidays(holidayPeriod.period)

                    val preschoolTerms = tx.getPreschoolTerms()

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
                    val directlyPlacedChildren = directlyPlacedChildData.map { it.child.id }.toSet()

                    // outgoing backup children
                    val backupCareOutgoingDataByChild =
                        tx.getReservationBackupPlacements(
                            directlyPlacedChildren,
                            holidayPeriod.period,
                        )

                    // all period absence data
                    val fullAbsenceDataByDate =
                        tx.getAbsencesForChildrenOverRange(
                            directlyPlacedChildren + backupChildrenInUnit,
                            holidayPeriod.period,
                        )

                    // all period reservation data
                    val fullReservationDataByDateAndChild =
                        tx.getReservationsForChildrenOverRange(
                            directlyPlacedChildren + backupChildrenInUnit,
                            holidayPeriod.period,
                        )

                    // all period assistance factor data
                    val assistanceFactorsByChild =
                        tx.getAssistanceFactorsForChildrenOverRange(
                                directlyPlacedChildren + backupChildrenInUnit,
                                holidayPeriod.period,
                            )
                            .groupBy { it.childId }

                    val operationDaysByChild =
                        tx.getOperationalDatesForChildren(
                            holidayPeriod.period,
                            directlyPlacedChildren + backupChildrenInUnit,
                        )

                    // collect daily report values
                    periodDays.map { (date) ->
                        val dailyDirectlyPlacedData =
                            directlyPlacedChildData.filter { sn ->
                                sn.validity.includes(date) &&
                                    (backupCareOutgoingDataByChild[sn.child.id] ?: emptyList())
                                        .none { it.range.includes(date) }
                            }
                        val dailyBackupPlacedData =
                            backupCareIncoming
                                .filter { it.period.includes(date) }
                                .mapNotNull {
                                    backupChildDataByChild[it.child.id]?.firstOrNull { sn ->
                                        sn.validity.includes(date)
                                    }
                                }
                        // splits placed children's service need info into two groups based on
                        // placement type
                        // - children that require reservations to inform holiday period attendance
                        // (RESERVATION_REQUIRED)
                        // - children that do not reserve attendance and won't have service on
                        // holiday periods (FIXED_SCHEDULE, TERM_BREAK)
                        val (dailyPlacedData, fixedScheduleServiceNeeds) =
                            (dailyDirectlyPlacedData + dailyBackupPlacedData).partition { sni ->
                                sni.placementType.scheduleType(
                                    date = date,
                                    clubTerms = emptyList(),
                                    preschoolTerms = preschoolTerms,
                                ) == ScheduleType.RESERVATION_REQUIRED
                            }
                        val fixedSchedulePlacedChildren =
                            fixedScheduleServiceNeeds.map { it.child.id }.toSet()
                        val dailyPlacedDataByChild = dailyPlacedData.groupBy { it.child.id }

                        val dailyAbsencesByChild =
                            fullAbsenceDataByDate[date]
                                ?.groupBy { it.childId }
                                ?.filter { (key, childDailyAbsenceData) ->
                                    val childDailyPlacementData =
                                        dailyPlacedDataByChild[key]?.firstOrNull {
                                            it.validity.includes(date)
                                        } ?: return@filter false
                                    childDailyAbsenceData.map { a -> a.category }.toSet() ==
                                        childDailyPlacementData.placementType.absenceCategories()
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
                                        ?.firstOrNull { af -> af.validDuring.includes(date) }
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
                            absentCount =
                                dailyAbsencesByChild.size + fixedSchedulePlacedChildren.size,
                            requiredStaff = staffNeedAtDate,
                            noResponseChildren =
                                noResponses
                                    .filter {
                                        operationDaysByChild[it.child.id]?.contains(date) == true
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

private data class ChildServiceNeedOccupancyInfo(
    @Nested("child_") val child: ChildBasics,
    val placementType: PlacementType,
    val coefficientUnder3y: Double,
    val coefficient: Double,
    val validity: FiniteDateRange,
    val shiftCareType: ShiftCareType,
)

private fun Database.Read.getAbsencesForChildrenOverRange(
    childIds: Set<PersonId>,
    range: FiniteDateRange,
) =
    getAbsences(
            Predicate {
                where(
                    "between_start_and_end(${bind(range)}, $it.date) AND $it.child_id = ANY (${bind(childIds)})"
                )
            }
        )
        .groupBy { r -> r.date }

private fun Database.Read.getReservationsForChildrenOverRange(
    childIds: Set<PersonId>,
    range: FiniteDateRange,
) =
    getReservations(
            Predicate {
                where(
                    "between_start_and_end(${bind(range)}, $it.date) AND $it.child_id = ANY (${bind(childIds)})"
                )
            }
        )
        .groupBy { Pair(it.date, it.childId) }

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
