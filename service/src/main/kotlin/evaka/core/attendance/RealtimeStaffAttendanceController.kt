// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.attendance

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.absence.getDaycareIdByGroup
import evaka.core.changes
import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.GroupId
import evaka.core.shared.StaffAttendanceExternalId
import evaka.core.shared.StaffAttendanceRealtimeId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.HelsinkiDateTimeRange
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.math.BigDecimal
import java.time.LocalDate
import org.jdbi.v3.core.JdbiException
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RealtimeStaffAttendanceController(private val accessControl: AccessControl) {
    @GetMapping("/employee/staff-attendances/realtime")
    fun getRealtimeStaffAttendances(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
    ): StaffAttendanceResponse {
        if (start.isAfter(end)) {
            throw BadRequest("Start must be before or equal to the end")
        }
        val range = FiniteDateRange(start, end)
        if (range.durationInDays() > 6 * 7) {
            throw BadRequest("Only 6 weeks of staff attendances may be fetched at once")
        }
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_STAFF_ATTENDANCES,
                        unitId,
                    )
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
                    val attendingEmployeeIds =
                        attendancesByEmployee.keys + staffForAttendanceCalendar.map { it.id }
                    val plannedAttendances =
                        tx.getPlannedStaffAttendanceForDays(attendingEmployeeIds, range)
                    val attendancesNotInGroups =
                        tx.getStaffAttendancesWithoutGroup(range, attendingEmployeeIds).groupBy {
                            it.employeeId
                        }

                    val allowedToEdit =
                        accessControl.checkPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Employee.UPDATE_STAFF_ATTENDANCES,
                            attendingEmployeeIds,
                        )

                    val rangeIncludingFollowingDay =
                        HelsinkiDateTimeRange(
                            HelsinkiDateTime.atStartOfDay(start),
                            HelsinkiDateTime.atStartOfDay(end.plusDays(2)),
                        )
                    val otherUnitAttendances =
                        tx.getStaffAttendancesInOtherUnits(
                                unitId,
                                allowedToEdit.filterValues { it.isPermitted() }.keys,
                                rangeIncludingFollowingDay,
                                clock.today(),
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
                                                type =
                                                    if (
                                                        allowedToEdit[employeeId]?.isPermitted() ==
                                                            true
                                                    )
                                                        att.type
                                                    else if (att.type.presentInGroup())
                                                        StaffAttendanceType.PRESENT
                                                    else null,
                                                att.departedAutomatically,
                                                att.arrivedAddedAt,
                                                att.arrivedAddedBy,
                                                att.arrivedModifiedAt,
                                                att.arrivedModifiedBy,
                                                att.departedAddedAt,
                                                att.departedAddedBy,
                                                att.departedModifiedAt,
                                                att.departedModifiedBy,
                                            )
                                        },
                                plannedAttendances = plannedAttendances[employeeId] ?: emptyList(),
                                otherUnitAttendances =
                                    otherUnitAttendances[employeeId] ?: emptyList(),
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
                                                att.arrivedAddedAt,
                                                att.arrivedAddedBy,
                                                att.arrivedModifiedAt,
                                                att.arrivedModifiedBy,
                                                att.departedAddedAt,
                                                att.departedAddedBy,
                                                att.departedModifiedAt,
                                                att.departedModifiedBy,
                                            )
                                        },
                                    plannedAttendances = plannedAttendances[emp.id] ?: emptyList(),
                                    otherUnitAttendances =
                                        otherUnitAttendances[emp.id] ?: emptyList(),
                                    allowedToEdit = allowedToEdit[emp.id]?.isPermitted() ?: false,
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
    ) {
        fun departsAfterArrival(): Boolean = departed == null || departed > arrived

        fun overlaps(other: StaffAttendanceUpsert): Boolean =
            (departed == null || other.arrived < departed) &&
                (other.departed == null || arrived < other.departed)
    }

    data class StaffAttendanceBody(
        val unitId: DaycareId,
        val employeeId: EmployeeId,
        val date: LocalDate,
        val entries: List<StaffAttendanceUpsert>,
    ) {
        fun anyAttendanceAfter(timestamp: HelsinkiDateTime): Boolean = entries.any {
            it.arrived > timestamp || (it.departed != null && it.departed > timestamp)
        }
    }

    @PostMapping("/employee/staff-attendances/realtime/upsert")
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
        if (body.entries.any { !it.departsAfterArrival() }) {
            throw BadRequest("Departure time must be after arrival time")
        }

        val (updates, changes) =
            try {
                db.connect { dbc ->
                    dbc.transaction { tx ->
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Unit.UPDATE_STAFF_ATTENDANCES,
                            body.unitId,
                        )
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Employee.UPDATE_STAFF_ATTENDANCES,
                            body.employeeId,
                        )
                        val groupIds = body.entries.mapNotNull { it.groupId }.distinct()
                        if (
                            !groupIds.all { groupId ->
                                tx.getDaycareIdByGroup(groupId) == body.unitId
                            }
                        ) {
                            throw BadRequest("Group is not in unit")
                        }
                        val existingIds =
                            tx.getEmployeeAttendancesByArrivalDateDate(
                                    body.unitId,
                                    body.employeeId,
                                    body.date,
                                )
                                .map { it.id }
                        if (
                            body.entries
                                .mapNotNull { it.id }
                                .any { id -> !existingIds.contains(id) }
                        ) {
                            throw BadRequest("Unknown id was given")
                        }
                        tx.execute { sql("SET CONSTRAINTS staff_attendance_no_overlaps DEFERRED") }
                        val deleted =
                            tx.deleteStaffAttendancesOnDateExcept(
                                body.unitId,
                                body.employeeId,
                                body.date,
                                body.entries.mapNotNull { it.id },
                            )

                        requireNoOverlapsBetween(body.entries)
                        requireNoOverlapsWithExistingStaffAttendances(
                            tx,
                            body.employeeId,
                            body.entries,
                            body.entries.mapNotNull { it.id }.toSet(),
                        )

                        val updates =
                            body.entries.mapNotNull { entry ->
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
                                    clock.now(),
                                    user.evakaUserId,
                                )
                            } + deleted.map { StaffAttendanceRealtimeChange(old = it) }
                        updates to
                            updates.map {
                                changes(it.old, it.new, StaffAttendanceRealtimeAudit.fields)
                            }
                    }
                }
            } catch (e: JdbiException) {
                throw mapStaffAttendanceWriteException(e)
            }
        Audit.StaffAttendanceUpdate.log(
            targetId = AuditId(body.unitId),
            objectId =
                AuditId(
                    (updates.mapNotNull { it.new.id } + updates.mapNotNull { it.old.id })
                        .distinct(),
                ),
            meta = mapOf("date" to body.date, "changes" to changes),
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
        fun anyAttendanceAfter(timestamp: HelsinkiDateTime): Boolean = entries.any {
            it.arrived > timestamp || (it.departed != null && it.departed > timestamp)
        }
    }

    @PostMapping("/employee/staff-attendances/realtime/upsert-external")
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

        val externalAttendanceIds = db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.UPDATE_STAFF_ATTENDANCES,
                    body.unitId,
                )
                val groupIds = body.entries.map { it.groupId }.distinct()
                if (!groupIds.all { groupId -> tx.getDaycareIdByGroup(groupId) == body.unitId }) {
                    throw BadRequest("Group is not in unit")
                }
                val existingIds =
                    tx.getExternalStaffAttendancesByDateRange(
                            body.unitId,
                            FiniteDateRange(body.date, body.date),
                        )
                        .filter { it.name == body.name && it.arrived.toLocalDate() == body.date }
                        .map { it.id }
                if (body.entries.mapNotNull { it.id }.any { id -> !existingIds.contains(id) }) {
                    throw BadRequest("Unknown id was given")
                }
                val deleted =
                    tx.deleteExternalAttendancesOnDateExcept(
                        body.unitId,
                        body.name,
                        body.date,
                        body.entries.mapNotNull { it.id },
                    )
                body.entries.mapNotNull {
                    tx.upsertExternalStaffAttendance(
                        it.id,
                        body.name,
                        it.groupId,
                        it.arrived,
                        it.departed,
                        if (it.hasStaffOccupancyEffect) occupancyCoefficientSeven
                        else occupancyCoefficientZero,
                        false,
                        clock.now(),
                        user.evakaUserId,
                    )
                } + deleted
            }
        }
        Audit.StaffAttendanceExternalUpdate.log(
            targetId = AuditId(body.unitId),
            objectId = AuditId(externalAttendanceIds),
            meta = mapOf("date" to body.date),
        )
    }

    @GetMapping("/employee/staff-attendances/realtime/open-attendance")
    fun getOpenGroupAttendance(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam userId: EmployeeId,
    ): OpenGroupAttendanceResponse {
        val openAttendance =
            db.connect { dbc ->
                    dbc.read { tx ->
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Employee.READ_OPEN_GROUP_ATTENDANCE,
                            userId,
                        )
                        tx.getOpenGroupAttendancesForEmployee(userId)
                    }
                }
                .also { Audit.StaffOpenAttendanceRead.log(targetId = AuditId(userId)) }
        return OpenGroupAttendanceResponse(openAttendance)
    }

    data class OpenGroupAttendanceResponse(val openGroupAttendance: OpenGroupAttendance?)
}
