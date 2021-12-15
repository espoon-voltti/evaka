// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import org.jdbi.v3.core.JdbiException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalTime

@RestController
@RequestMapping("/realtime-staff-attendances")
class RealtimeStaffAttendanceController(
    private val acl: AccessControlList,
    private val ac: AccessControl
) {
    @GetMapping
    fun getAttendancesByUnit(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam unitId: DaycareId
    ): StaffAttendanceResponse {
        Audit.UnitStaffAttendanceRead.log(targetId = unitId)

        // todo: convert to action auth
        @Suppress("DEPRECATION")
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.MOBILE)

        return db.read {
            StaffAttendanceResponse(
                staff = it.getStaffAttendances(unitId, HelsinkiDateTime.now()),
                extraAttendances = it.getExternalStaffAttendances(unitId)
            )
        }
    }

    // todo: maybe employeeId and pinCode could be passed in headers as an authentication method
    // in which case the same endpoint would also work from desktop with normal auth
    data class StaffArrivalRequest(
        val employeeId: EmployeeId,
        val pinCode: String,
        val groupId: GroupId,
        val time: LocalTime
    )
    @PostMapping("/arrival")
    fun markArrival(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: StaffArrivalRequest
    ) {
        Audit.StaffAttendanceArrivalCreate.log(targetId = body.groupId, objectId = body.employeeId)

        // todo: convert to action auth
        @Suppress("DEPRECATION")
        acl.getRolesForUnitGroup(user, body.groupId).requireOneOfRoles(UserRole.MOBILE)
        ac.verifyPinCode(body.employeeId, body.pinCode)
        // todo: check that employee has access to a unit related to the group?

        try {
            db.transaction {
                it.markStaffArrival(
                    employeeId = body.employeeId,
                    groupId = body.groupId,
                    arrivalTime = HelsinkiDateTime.now().withTime(minOf(body.time, LocalTime.now()))
                )
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    data class StaffDepartureRequest(
        val pinCode: String,
        val time: LocalTime
    )
    @PostMapping("/{attendanceId}/departure")
    fun markDeparture(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable attendanceId: StaffAttendanceId,
        @RequestBody body: StaffDepartureRequest
    ) {
        Audit.StaffAttendanceDepartureCreate.log()

        // todo: convert to action auth
        @Suppress("DEPRECATION")
        val attendance = db.read { it.getStaffAttendance(attendanceId) }
            ?: throw NotFound("attendance not found")
        @Suppress("DEPRECATION")
        acl.getRolesForUnitGroup(user, attendance.groupId).requireOneOfRoles(UserRole.MOBILE)
        ac.verifyPinCode(attendance.employeeId, body.pinCode)

        db.transaction {
            it.markStaffDeparture(
                attendanceId = attendanceId,
                departureTime = HelsinkiDateTime.now().withTime(minOf(body.time, LocalTime.now()))
            )
        }
    }

    data class ExternalStaffArrivalRequest(
        val name: String,
        val groupId: GroupId,
        val arrived: LocalTime
    )
    @PostMapping("/arrival-external")
    fun markExternalArrival(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: ExternalStaffArrivalRequest
    ): StaffAttendanceExternalId {
        Audit.StaffAttendanceArrivalExternalCreate.log(targetId = body.groupId)
        // todo: convert to action auth
        @Suppress("DEPRECATION")
        acl.getRolesForUnitGroup(user, body.groupId).requireOneOfRoles(UserRole.MOBILE)
        return db.transaction {
            it.markExternalStaffArrival(
                ExternalStaffArrival(
                    name = body.name,
                    groupId = body.groupId, arrived = HelsinkiDateTime.now().withTime(minOf(body.arrived, LocalTime.now()))
                )
            )
        }
    }

    data class ExternalStaffDepartureRequest(
        val attendanceId: StaffAttendanceExternalId,
        val time: LocalTime
    )
    @PostMapping("/departure-external")
    fun markExternalDeparture(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: ExternalStaffDepartureRequest
    ) {
        Audit.StaffAttendanceDepartureExternalCreate.log(body.attendanceId)

        // todo: convert to action auth
        val attendance = db.read { it.getExternalStaffAttendance(body.attendanceId) }
            ?: throw NotFound("attendance not found")
        @Suppress("DEPRECATION")
        acl.getRolesForUnitGroup(user, attendance.groupId).requireOneOfRoles(UserRole.MOBILE)

        db.transaction {
            it.markExternalStaffDeparture(
                ExternalStaffDeparture(
                    id = body.attendanceId,
                    departed = HelsinkiDateTime.now().withTime(minOf(body.time, LocalTime.now()))
                )
            )
        }
    }
}
