// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.JdbiException
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@RestController
@RequestMapping("/staff-attendances/realtime")
class RealtimeStaffAttendanceController(
    private val accessControl: AccessControl
) {
    @GetMapping
    fun getAttendances(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        start: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        end: LocalDate
    ): StaffAttendanceResponse {
        accessControl.requirePermissionFor(user, clock, Action.Unit.READ_STAFF_ATTENDANCES, unitId)

        return db.connect { dbc ->
            dbc.read {
                val range = FiniteDateRange(start, end)
                val attendancesByEmployee = it.getStaffAttendancesForDateRange(unitId, range).groupBy { raw -> raw.employeeId }
                val attendanceEmployeeToGroups = it.getGroupsForEmployees(attendancesByEmployee.keys)
                val staffForAttendanceCalendar = it.getCurrentStaffForAttendanceCalendar(unitId, range.start, range.end)
                val noAttendanceEmployeeToGroups = it.getGroupsForEmployees(staffForAttendanceCalendar.map { emp -> emp.id }.toSet())
                val plannedAttendances = it.getPlannedStaffAttendanceForDays(attendancesByEmployee.keys + staffForAttendanceCalendar.map { it.id }, range)
                val staffWithAttendance = attendancesByEmployee.entries.map { (employeeId, data) ->
                    EmployeeAttendance(
                        employeeId = employeeId,
                        groups = attendanceEmployeeToGroups[employeeId] ?: listOf(),
                        firstName = data[0].firstName,
                        lastName = data[0].lastName,
                        currentOccupancyCoefficient = data[0].currentOccupancyCoefficient ?: BigDecimal.ZERO,
                        attendances = data.map { att ->
                            Attendance(
                                att.id,
                                att.groupId,
                                att.arrived,
                                att.departed,
                                att.occupancyCoefficient,
                                att.type
                            )
                        },
                        hasFutureAttendances = data[0].hasFutureAttendances,
                        plannedAttendances = plannedAttendances[employeeId] ?: emptyList()
                    )
                }
                val staffWithoutAttendance = staffForAttendanceCalendar
                    .filter { emp -> !attendancesByEmployee.keys.contains(emp.id) }
                    .map { emp ->
                        EmployeeAttendance(
                            employeeId = emp.id,
                            groups = noAttendanceEmployeeToGroups[emp.id] ?: listOf(),
                            firstName = emp.firstName,
                            lastName = emp.lastName,
                            currentOccupancyCoefficient = emp.currentOccupancyCoefficient ?: BigDecimal.ZERO,
                            listOf(),
                            hasFutureAttendances = emp.hasFutureAttendances,
                            plannedAttendances = plannedAttendances[emp.id] ?: emptyList()
                        )
                    }
                StaffAttendanceResponse(
                    staff = staffWithAttendance + staffWithoutAttendance,
                    extraAttendances = it.getExternalStaffAttendancesByDateRange(unitId, range)
                )
            }
        }.also {
            Audit.StaffAttendanceRead.log(
                targetId = unitId,
                args = mapOf("staffCount" to it.staff.size, "externalStaffCount" to it.extraAttendances.size)
            )
        }
    }

    data class UpsertStaffAttendance(
        val attendanceId: StaffAttendanceId?,
        val employeeId: EmployeeId,
        val groupId: GroupId,
        val arrived: HelsinkiDateTime,
        val departed: HelsinkiDateTime?,
        val type: StaffAttendanceType
    )

    data class UpsertExternalAttendance(
        val attendanceId: StaffAttendanceExternalId?,
        val name: String?,
        val groupId: GroupId,
        val arrived: HelsinkiDateTime,
        val departed: HelsinkiDateTime?
    )

    data class UpsertStaffAndExternalAttendanceRequest(
        val staffAttendances: List<UpsertStaffAttendance>,
        val externalAttendances: List<UpsertExternalAttendance>
    ) {
        fun isArrivedBeforeDeparted() =
            staffAttendances.all { it.departed == null || it.arrived < it.departed } &&
                externalAttendances.all { it.departed == null || it.arrived < it.departed }
    }

    @PostMapping("/{unitId}/upsert")
    fun upsertStaffAttendances(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestBody body: UpsertStaffAndExternalAttendanceRequest
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Unit.UPDATE_STAFF_ATTENDANCES, unitId)

        if (!body.isArrivedBeforeDeparted()) {
            throw BadRequest("Arrival time must be before departure time for all entries")
        }

        val objectId = db.connect { dbc ->
            dbc.transaction { tx ->
                try {
                    val occupancyCoefficients = body.staffAttendances.associate {
                        Pair(
                            it.employeeId,
                            tx.getOccupancyCoefficientForEmployee(it.employeeId, it.groupId) ?: BigDecimal.ZERO
                        )
                    }
                    val staffAttendanceIds = body.staffAttendances.map {
                        tx.upsertStaffAttendance(
                            it.attendanceId,
                            it.employeeId,
                            it.groupId,
                            it.arrived,
                            it.departed,
                            occupancyCoefficients[it.employeeId],
                            it.type
                        )
                    }

                    val externalStaffAttendanceIds = body.externalAttendances.map {
                        tx.upsertExternalStaffAttendance(
                            it.attendanceId,
                            it.name,
                            it.groupId,
                            it.arrived,
                            it.departed,
                            occupancyCoefficientSeven
                        )
                    }
                    listOf(staffAttendanceIds, externalStaffAttendanceIds)
                } catch (e: JdbiException) {
                    throw mapPSQLException(e)
                }
            }
        }
        Audit.StaffAttendanceUpdate.log(targetId = unitId, objectId = objectId)
    }

    data class SingleDayStaffAttendanceUpsert(
        val attendanceId: StaffAttendanceId?,
        val groupId: GroupId,
        val arrived: HelsinkiDateTime,
        val departed: HelsinkiDateTime?,
        val type: StaffAttendanceType
    )

    @PostMapping("/{unitId}/{employeeId}/{date}")
    fun upsertDailyStaffAttendances(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable employeeId: EmployeeId,
        @PathVariable
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate,
        @RequestBody body: List<SingleDayStaffAttendanceUpsert>
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Unit.UPDATE_STAFF_ATTENDANCES, unitId)

        val staffAttendanceIds = db.connect { dbc ->
            dbc.transaction { tx ->
                val occupancyCoefficients = body.map { it.groupId }.distinct().associateWith {
                    tx.getOccupancyCoefficientForEmployee(employeeId, it) ?: BigDecimal.ZERO
                }
                val wholeDay = HelsinkiDateTimeRange(
                    HelsinkiDateTime.of(date, LocalTime.of(0, 0)),
                    HelsinkiDateTime.of(date.plusDays(1), LocalTime.of(0, 0))
                )
                tx.deleteStaffAttendanceWithoutIds(employeeId, wholeDay, body.mapNotNull { it.attendanceId })
                body.map {
                    tx.upsertStaffAttendance(
                        it.attendanceId,
                        employeeId,
                        it.groupId,
                        it.arrived,
                        it.departed,
                        occupancyCoefficients[it.groupId],
                        it.type
                    )
                }
            }
        }
        Audit.StaffAttendanceUpdate.log(targetId = unitId, objectId = staffAttendanceIds, args = mapOf("date" to date))
    }

    @DeleteMapping("/{unitId}/{attendanceId}")
    fun deleteStaffAttendances(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable attendanceId: StaffAttendanceId
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Unit.DELETE_STAFF_ATTENDANCES, unitId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.deleteStaffAttendance(attendanceId)
            }
        }
        Audit.StaffAttendanceDelete.log(targetId = attendanceId)
    }

    @DeleteMapping("/{unitId}/external/{attendanceId}")
    fun deleteExternalStaffAttendances(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable attendanceId: StaffAttendanceExternalId
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Unit.DELETE_STAFF_ATTENDANCES, unitId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.deleteExternalStaffAttendance(attendanceId)
            }
        }
        Audit.StaffAttendanceExternalDelete.log(targetId = attendanceId)
    }
}
