// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.assistance.getAssistanceFactorsForChildrenOverRange
import fi.espoo.evaka.attendance.occupancyCoefficientSeven
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.holidayperiod.getHolidayPeriod
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.reservations.getReservationBackupPlacements
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.DateMap
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.getOperationalDatesForChildren
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.Period
import kotlin.math.ceil
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
        @RequestParam(required = false) groupIds: Set<GroupId> = emptySet(),
        @RequestParam unitId: DaycareId,
        @RequestParam periodId: HolidayPeriodId,
    ): List<HolidayReportRow> {
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
                            .map { HolidayReportDay(it, holidays.contains(it)) }
                            .filter {
                                unitOperationDays.contains(it.date.dayOfWeek.value) &&
                                    (unit.shiftCareOpenOnHolidays || !it.isHoliday)
                            }
                            .toList()

                    // incoming back up children
                    val incomingBackupCaresByChild =
                        tx.getIncomingBackupCaresOverPeriodForGroupsInUnit(
                                unitId = unitId,
                                groupIds = groupIds,
                                period = holidayPeriod.period,
                            )
                            .groupBy { it.childId }
                            .mapValues { entry ->
                                DateMap.of(entry.value.map { it.validDuring to it })
                            }

                    val backupChildrenInUnit = incomingBackupCaresByChild.keys

                    val backupChildDataByChild =
                        tx.getServiceNeedOccupancyInfoOverRangeForChildren(
                                backupChildrenInUnit,
                                holidayPeriod.period,
                            )
                            .groupBy { it.child.id }

                    // directly placed children
                    val directlyPlacedChildData =
                        tx.getServiceNeedOccupancyInfoOverRangeForGroups(
                            range = holidayPeriod.period,
                            groupIds = groupIds,
                            unitId = unitId,
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
                            .mapValues { entry ->
                                DateMap.of(entry.value.map { it.validDuring to it.capacityFactor })
                            }

                    val assistanceRangesByChild =
                        tx.getAssistanceRanges(
                                directlyPlacedChildren + backupChildrenInUnit,
                                holidayPeriod.period,
                            )
                            .groupBy { it.childId }
                            .mapValues { entry -> DateSet.of(entry.value.map { it.validDuring }) }

                    val operationDaysByChild =
                        tx.getOperationalDatesForChildren(
                            holidayPeriod.period,
                            directlyPlacedChildren + backupChildrenInUnit,
                        )

                    // collect daily report values
                    periodDays.map { h ->
                        val date = h.date
                        val dailyDirectlyPlacedData =
                            directlyPlacedChildData.filter { sn ->
                                sn.validity.includes(date) &&
                                    (backupCareOutgoingDataByChild[sn.child.id] ?: emptyList())
                                        .none { it.range.includes(date) }
                            }
                        val dailyBackupPlacedData =
                            incomingBackupCaresByChild.mapNotNull { (key, value) ->
                                val bc = value.getValue(date) ?: return@mapNotNull null
                                backupChildDataByChild[key]
                                    ?.firstOrNull { sn -> sn.validity.includes(date) }
                                    ?.copy(groupId = bc.groupId)
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

                        val dailyOccupancyCoefficient =
                            confirmedPresent.sumOf {
                                val ageAtDate = Period.between(it.child.dateOfBirth, date).years
                                val assistanceFactor =
                                    assistanceFactorsByChild[it.child.id]?.getValue(date) ?: 1.0
                                if (ageAtDate < 3) it.coefficientUnder3y * assistanceFactor
                                else it.coefficient * assistanceFactor
                            }
                        val staffNeedAtDate =
                            ceil(
                                    dailyOccupancyCoefficient.div(
                                        occupancyCoefficientSeven.toDouble()
                                    )
                                )
                                .toInt()

                        HolidayReportRow(
                            date = date,
                            presentChildren =
                                confirmedPresent.map { info ->
                                    ChildWithName(
                                        id = info.child.id,
                                        firstName = info.child.firstName,
                                        lastName = info.child.lastName,
                                    )
                                },
                            assistanceChildren =
                                confirmedPresent
                                    .filter {
                                        assistanceRangesByChild[it.child.id]?.includes(date) == true
                                    }
                                    .map { info ->
                                        ChildWithName(
                                            id = info.child.id,
                                            firstName = info.child.firstName,
                                            lastName = info.child.lastName,
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
                                    .map { info ->
                                        ChildWithName(
                                            id = info.child.id,
                                            firstName = info.child.firstName,
                                            lastName = info.child.lastName,
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
