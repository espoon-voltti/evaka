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
import fi.espoo.evaka.shared.auth.getDaycareAclRows
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RealtimeStaffAttendanceController(private val accessControl: AccessControl) {
    @GetMapping(
        "/staff-attendances/realtime", // deprecated
        "/employee/staff-attendances/realtime",
    )
    fun getRealtimeStaffAttendances(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
    ): StaffAttendanceResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_STAFF_ATTENDANCES,
                        unitId,
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
                            staffForAttendanceCalendar.map { emp -> emp.id }.toSet(),
                        )
                    val plannedAttendances =
                        tx.getPlannedStaffAttendanceForDays(
                            attendancesByEmployee.keys + staffForAttendanceCalendar.map { it.id },
                            range,
                        )
                    val attendancesNotInGroups =
                        tx.getStaffAttendancesWithoutGroup(
                                range,
                                attendancesByEmployee.keys +
                                    staffForAttendanceCalendar.map { it.id },
                            )
                            .groupBy { it.employeeId }

                    val allowedToEdit =
                        accessControl.checkPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Employee.UPDATE_STAFF_ATTENDANCES,
                            attendancesByEmployee.entries.map { (employeeId) -> employeeId },
                        )

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
                                                att.departedAutomatically,
                                            )
                                        },
                                plannedAttendances = plannedAttendances[employeeId] ?: emptyList(),
                                allowedToEdit = allowedToEdit[employeeId]?.isPermitted() ?: false,
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
                                                att.departedAutomatically,
                                            )
                                        },
                                    plannedAttendances = plannedAttendances[emp.id] ?: emptyList(),
                                    allowedToEdit = true,
                                )
                            }
                    StaffAttendanceResponse(
                        staff = staffWithAttendance + staffWithoutAttendance,
                        extraAttendances = tx.getExternalStaffAttendancesByDateRange(unitId, range),
                    )
                }
            }
            .also {
                Audit.StaffAttendanceRead.log(
                    targetId = AuditId(unitId),
                    meta =
                        mapOf(
                            "staffCount" to it.staff.size,
                            "externalStaffCount" to it.extraAttendances.size,
                        ),
                )
            }
    }

    data class StaffAttendanceUpsert(
        val id: StaffAttendanceRealtimeId?,
        val groupId: GroupId?,
        val arrived: HelsinkiDateTime,
        val departed: HelsinkiDateTime?,
        val type: StaffAttendanceType,
        val hasStaffOccupancyEffect: Boolean,
    )

    data class StaffAttendanceBody(
        val unitId: DaycareId,
        val employeeId: EmployeeId,
        val date: LocalDate,
        val entries: List<StaffAttendanceUpsert>,
    ) {
        fun anyAttendanceAfter(timestamp: HelsinkiDateTime): Boolean =
            entries.any {
                it.arrived > timestamp || (it.departed != null && it.departed > timestamp)
            }
    }

    @PostMapping(
        "/staff-attendances/realtime/upsert", // deprecated
        "/employee/staff-attendances/realtime/upsert",
    )
    fun upsertDailyStaffRealtimeAttendances(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: StaffAttendanceBody,
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
                        body.unitId,
                    )
                    tx.deleteStaffAttendancesOnDateExcept(
                        body.unitId,
                        body.employeeId,
                        body.date,
                        body.entries.mapNotNull { it.id },
                    )

                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Employee.UPDATE_STAFF_ATTENDANCES,
                        body.employeeId,
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
                            false,
                        )
                    }
                }
            }
        Audit.StaffAttendanceUpdate.log(
            targetId = AuditId(body.unitId),
            objectId = AuditId(staffAttendanceIds),
            meta = mapOf("date" to body.date),
        )
    }

    data class ExternalAttendanceUpsert(
        val id: StaffAttendanceExternalId?,
        val groupId: GroupId,
        val hasStaffOccupancyEffect: Boolean,
        val arrived: HelsinkiDateTime,
        val departed: HelsinkiDateTime?,
    )

    data class ExternalAttendanceBody(
        val unitId: DaycareId,
        val name: String,
        val date: LocalDate,
        val entries: List<ExternalAttendanceUpsert>,
    ) {
        fun anyAttendanceAfter(timestamp: HelsinkiDateTime): Boolean =
            entries.any {
                it.arrived > timestamp || (it.departed != null && it.departed > timestamp)
            }
    }

    @PostMapping(
        "/staff-attendances/realtime/upsert-external", // deprecated
        "/employee/staff-attendances/realtime/upsert-external",
    )
    fun upsertDailyExternalRealtimeAttendances(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ExternalAttendanceBody,
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
                        body.unitId,
                    )
                    tx.deleteExternalAttendancesOnDateExcept(
                        body.name,
                        body.date,
                        body.entries.mapNotNull { it.id },
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
                            false,
                        )
                    }
                }
            }
        Audit.StaffAttendanceExternalUpdate.log(
            targetId = AuditId(body.unitId),
            objectId = AuditId(externalAttendanceIds),
            meta = mapOf("date" to body.date),
        )
    }

    @GetMapping("/employee/staff-attendances/realtime/open-attendence")
    fun getOpenGroupAttendance(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam userId: EmployeeId,
        @RequestParam unitId: DaycareId,
    ): OpenGroupAttendanceResponse {
        val openAttendance =
            db.connect { dbc ->
                    dbc.transaction { tx ->
                        // Check if the authenticated user has permission to read staff attendances
                        // for this unit
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Unit.READ_STAFF_ATTENDANCES,
                            unitId,
                        )
                        // Also check that the given user belongs to this unit
                        val targetUserPartOfUnit =
                            tx.getDaycareAclRows(unitId, false).any { it.employee.id == userId }
                        if (!targetUserPartOfUnit) {
                            throw BadRequest("User doesn't belong to the unit")
                        }
                        // If the user has the permission to read staff attendances from this unit,
                        // then they are also permitted to check for open attendances of a colleague
                        // from all units
                        tx.getOpenGroupAttendancesForEmployee(userId)
                    }
                }
                .also { Audit.StaffOpenAttendanceRead.log(targetId = AuditId(userId)) }
        return OpenGroupAttendanceResponse(openAttendance)
    }

    data class OpenGroupAttendanceResponse(val openGroupAttendance: OpenGroupAttendance?)
}
