// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/staff-attendances/realtime")
class RealtimeStaffAttendanceController(private val accessControl: AccessControl) {
    @GetMapping
    fun getRealtimeStaffAttendances(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate
    ): StaffAttendanceResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_STAFF_ATTENDANCES,
                        unitId
                    )
                    val range = FiniteDateRange(start, end)
                    val attendancesByEmployee =
                        tx.getStaffAttendancesForDateRange(unitId, range).groupBy { raw ->
                            raw.employeeId
                        }
                    val attendanceEmployeeToGroups =
                        tx.getGroupsForEmployees(unitId, attendancesByEmployee.keys)
                    val staffForAttendanceCalendar = tx.getCurrentStaffForAttendanceCalendar(unitId)
                    val noAttendanceEmployeeToGroups =
                        tx.getGroupsForEmployees(
                            unitId,
                            staffForAttendanceCalendar.map { emp -> emp.id }.toSet()
                        )
                    val plannedAttendances =
                        tx.getPlannedStaffAttendanceForDays(
                            attendancesByEmployee.keys + staffForAttendanceCalendar.map { it.id },
                            range
                        )
                    val attendancesNotInGroups =
                        tx.getStaffAttendancesWithoutGroup(
                                range,
                                attendancesByEmployee.keys +
                                    staffForAttendanceCalendar.map { it.id }
                            )
                            .groupBy { it.employeeId }
                    val staffWithAttendance =
                        attendancesByEmployee.entries.map { (employeeId, data) ->
                            EmployeeAttendance(
                                employeeId = employeeId,
                                groups = attendanceEmployeeToGroups[employeeId] ?: listOf(),
                                firstName = data[0].firstName,
                                lastName = data[0].lastName,
                                currentOccupancyCoefficient =
                                    data[0].currentOccupancyCoefficient ?: BigDecimal.ZERO,
                                attendances =
                                    (data + (attendancesNotInGroups[employeeId] ?: emptyList()))
                                        .map { att ->
                                            Attendance(
                                                att.id,
                                                att.groupId,
                                                att.arrived,
                                                att.departed,
                                                att.occupancyCoefficient,
                                                att.type,
                                                att.departedAutomatically
                                            )
                                        },
                                plannedAttendances = plannedAttendances[employeeId] ?: emptyList(),
                                allowedToEdit =
                                    accessControl
                                        .checkPermissionFor(
                                            tx,
                                            user,
                                            clock,
                                            Action.Employee.UPDATE_STAFF_ATTENDANCES,
                                            employeeId
                                        )
                                        .isPermitted()
                            )
                        }
                    val staffWithoutAttendance =
                        staffForAttendanceCalendar
                            .filter { emp -> !attendancesByEmployee.keys.contains(emp.id) }
                            .map { emp ->
                                EmployeeAttendance(
                                    employeeId = emp.id,
                                    groups = noAttendanceEmployeeToGroups[emp.id] ?: listOf(),
                                    firstName = emp.firstName,
                                    lastName = emp.lastName,
                                    currentOccupancyCoefficient =
                                        emp.currentOccupancyCoefficient ?: BigDecimal.ZERO,
                                    attendances =
                                        (attendancesNotInGroups[emp.id] ?: emptyList()).map { att ->
                                            Attendance(
                                                att.id,
                                                att.groupId,
                                                att.arrived,
                                                att.departed,
                                                att.occupancyCoefficient,
                                                att.type,
                                                att.departedAutomatically
                                            )
                                        },
                                    plannedAttendances = plannedAttendances[emp.id] ?: emptyList(),
                                    allowedToEdit = true
                                )
                            }
                    StaffAttendanceResponse(
                        staff = staffWithAttendance + staffWithoutAttendance,
                        extraAttendances = tx.getExternalStaffAttendancesByDateRange(unitId, range)
                    )
                }
            }
            .also {
                Audit.StaffAttendanceRead.log(
                    targetId = AuditId(unitId),
                    meta =
                        mapOf(
                            "staffCount" to it.staff.size,
                            "externalStaffCount" to it.extraAttendances.size
                        )
                )
            }
    }

    data class StaffAttendanceUpsert(
        val id: StaffAttendanceRealtimeId?,
        val groupId: GroupId?,
        val arrived: HelsinkiDateTime,
        val departed: HelsinkiDateTime?,
        val type: StaffAttendanceType,
        val hasStaffOccupancyEffect: Boolean
    )

    data class StaffAttendanceBody(
        val unitId: DaycareId,
        val employeeId: EmployeeId,
        val date: LocalDate,
        val entries: List<StaffAttendanceUpsert>
    ) {
        fun anyAttendanceAfter(timestamp: HelsinkiDateTime): Boolean =
            entries.any {
                it.arrived > timestamp || (it.departed != null && it.departed > timestamp)
            }
    }

    @PostMapping("/upsert")
    fun upsertDailyStaffRealtimeAttendances(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: StaffAttendanceBody
    ) {
        if (body.anyAttendanceAfter(clock.now())) {
            throw BadRequest("Attendances cannot be in the future")
        }
        if (body.date.isAfter(clock.today())) {
            throw BadRequest("Date cannot be in the future")
        }

        val staffAttendanceIds =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.UPDATE_STAFF_ATTENDANCES,
                        body.unitId
                    )
                    val wholeDay =
                        HelsinkiDateTimeRange(
                            HelsinkiDateTime.of(body.date, LocalTime.of(0, 0)),
                            HelsinkiDateTime.of(body.date.plusDays(1), LocalTime.of(0, 0))
                        )
                    tx.deleteStaffAttendancesInRangeExcept(
                        body.unitId,
                        body.employeeId,
                        wholeDay,
                        body.entries.mapNotNull { it.id }
                    )
                    body.entries.map { entry ->
                        val occupancyCoefficient =
                            if (entry.hasStaffOccupancyEffect) occupancyCoefficientSeven
                            else occupancyCoefficientZero
                        tx.upsertStaffAttendance(
                            entry.id,
                            body.employeeId,
                            entry.groupId,
                            entry.arrived,
                            entry.departed,
                            occupancyCoefficient,
                            entry.type,
                            false
                        )
                    }
                }
            }
        Audit.StaffAttendanceUpdate.log(
            targetId = AuditId(body.unitId),
            objectId = AuditId(staffAttendanceIds),
            meta = mapOf("date" to body.date)
        )
    }

    data class ExternalAttendanceUpsert(
        val id: StaffAttendanceExternalId?,
        val groupId: GroupId,
        val hasStaffOccupancyEffect: Boolean,
        val arrived: HelsinkiDateTime,
        val departed: HelsinkiDateTime?
    )

    data class ExternalAttendanceBody(
        val unitId: DaycareId,
        val name: String,
        val date: LocalDate,
        val entries: List<ExternalAttendanceUpsert>
    ) {
        fun anyAttendanceAfter(timestamp: HelsinkiDateTime): Boolean =
            entries.any {
                it.arrived > timestamp || (it.departed != null && it.departed > timestamp)
            }
    }

    @PostMapping("/upsert-external")
    fun upsertDailyExternalRealtimeAttendances(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: ExternalAttendanceBody
    ) {
        if (body.anyAttendanceAfter(clock.now())) {
            throw BadRequest("Attendances cannot be in the future")
        }
        if (body.date.isAfter(clock.today())) {
            throw BadRequest("Date cannot be in the future")
        }

        val externalAttendanceIds =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.UPDATE_STAFF_ATTENDANCES,
                        body.unitId
                    )
                    val wholeDay =
                        HelsinkiDateTimeRange(
                            HelsinkiDateTime.of(body.date, LocalTime.of(0, 0)),
                            HelsinkiDateTime.of(body.date.plusDays(1), LocalTime.of(0, 0))
                        )
                    tx.deleteExternalAttendancesInRangeExcept(
                        body.name,
                        wholeDay,
                        body.entries.mapNotNull { it.id }
                    )
                    body.entries.map {
                        tx.upsertExternalStaffAttendance(
                            it.id,
                            body.name,
                            it.groupId,
                            it.arrived,
                            it.departed,
                            if (it.hasStaffOccupancyEffect) occupancyCoefficientSeven
                            else occupancyCoefficientZero,
                            false
                        )
                    }
                }
            }
        Audit.StaffAttendanceExternalUpdate.log(
            targetId = AuditId(body.unitId),
            objectId = AuditId(externalAttendanceIds),
            meta = mapOf("date" to body.date)
        )
    }

    @DeleteMapping("/{unitId}/{attendanceId}")
    fun deleteStaffRealtimeAttendances(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable attendanceId: StaffAttendanceRealtimeId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.DELETE_STAFF_ATTENDANCES,
                    unitId
                )
                tx.deleteStaffAttendance(attendanceId)
            }
        }
        Audit.StaffAttendanceDelete.log(targetId = AuditId(attendanceId))
    }

    @DeleteMapping("/{unitId}/external/{attendanceId}")
    fun deleteExternalStaffRealtimeAttendances(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable attendanceId: StaffAttendanceExternalId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.DELETE_STAFF_ATTENDANCES,
                    unitId
                )
                tx.deleteExternalStaffAttendance(attendanceId)
            }
        }
        Audit.StaffAttendanceExternalDelete.log(targetId = AuditId(attendanceId))
    }
}
