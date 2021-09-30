package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalTime

/**
 * This controller will gradually replace fi.espoo.evaka.daycare.controllers.StaffAttendanceController
 */
@RestController
@RequestMapping("/v2")
class StaffAttendanceController2(
    private val acl: AccessControlList
) {
    @GetMapping("/units/{unitId}/staff-attendances")
    fun getAttendancesByUnit(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId
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
        val time: LocalTime,
        val pinCode: String
    )
    @PostMapping("/units/{unitId}/staff-attendances/arrivals")
    fun markArrival(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @RequestBody body: StaffArrivalRequest
    ) {
        Audit.StaffAttendanceArrivalCreate.log(targetId = unitId, objectId = body.employeeId)
        // todo: convert to action auth
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.MOBILE)
        db.transaction {
            if (it.employeePinIsCorrect(body.employeeId.raw, body.pinCode)) {
                it.markStaffArrival(
                    employeeId = body.employeeId,
                    arrivalTime = HelsinkiDateTime.now().withTime(body.time)
                )
            } else {
                it.updateEmployeePinFailureCountAndCheckIfLocked(body.employeeId.raw)
                throw Forbidden("Invalid pin code")
            }
        }
    }

    data class StaffDepartureRequest(
        val employeeId: EmployeeId,
        val time: LocalTime,
        val pinCode: String
    )
    @PostMapping("/units/{unitId}/staff-attendances/departures")
    fun markDeparture(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @RequestBody body: StaffDepartureRequest
    ) {
        Audit.StaffAttendanceDepartureCreate.log(targetId = unitId, objectId = body.employeeId)
        // todo: convert to action auth
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.MOBILE)
        db.transaction {
            if (it.employeePinIsCorrect(body.employeeId.raw, body.pinCode)) {
                it.markStaffDeparture(
                    employeeId = body.employeeId,
                    departureTime = HelsinkiDateTime.now().withTime(body.time)
                )
            } else {
                it.updateEmployeePinFailureCountAndCheckIfLocked(body.employeeId.raw)
                throw Forbidden("Invalid pin code")
            }
        }
    }
}
