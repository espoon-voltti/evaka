// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalTime

/**
 * This controller will gradually replace fi.espoo.evaka.daycare.controllers.StaffAttendanceController
 */
@RestController
@RequestMapping("/v2/staff-attendances")
class StaffAttendanceController2(
    private val acl: AccessControlList
) {
    @GetMapping
    fun getAttendancesByUnit(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam unitId: DaycareId
    ): StaffAttendanceResponse {
        Audit.UnitStaffAttendanceRead.log(targetId = unitId)
        // todo: convert to action auth
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.MOBILE)
        return db.read {
            StaffAttendanceResponse(
                staff = it.getStaffAttendances(unitId, HelsinkiDateTime.now()),
                extraAttendances = it.getExternalStaffAttendances(unitId)
            )
        }
    }

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
        acl.getRolesForUnitGroup(user, body.groupId).requireOneOfRoles(UserRole.MOBILE)
        val errorCode = db.transaction {
            if (it.employeePinIsCorrect(body.employeeId.raw, body.pinCode)) {
                it.markStaffArrival(
                    employeeId = body.employeeId,
                    groupId = body.groupId,
                    arrivalTime = HelsinkiDateTime.now().withTime(minOf(body.time, LocalTime.now()))
                )
                null
            } else {
                val locked = it.updateEmployeePinFailureCountAndCheckIfLocked(body.employeeId.raw)
                if (locked) "PIN_LOCKED" else "WRONG_PIN"
            }
        }
        if (errorCode != null) {
            throw Forbidden("Invalid pin code", errorCode)
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
        val attendance = db.read { it.getStaffAttendance(attendanceId) }
            ?: throw NotFound("attendance not found")
        acl.getRolesForUnitGroup(user, attendance.groupId).requireOneOfRoles(UserRole.MOBILE)

        val errorCode = db.transaction {
            if (it.employeePinIsCorrect(attendance.employeeId.raw, body.pinCode)) {
                it.markStaffDeparture(
                    attendanceId = attendanceId,
                    departureTime = HelsinkiDateTime.now().withTime(minOf(body.time, LocalTime.now()))
                )
                null
            } else {
                val locked = it.updateEmployeePinFailureCountAndCheckIfLocked(attendance.employeeId.raw)
                if (locked) "PIN_LOCKED" else "WRONG_PIN"
            }
        }
        if (errorCode != null) {
            throw Forbidden("Invalid pin code", errorCode)
        }
    }
}
