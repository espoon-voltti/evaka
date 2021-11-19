// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.getEmployeeUser
import fi.espoo.evaka.pis.markEmployeeLastLogin
import fi.espoo.evaka.pis.resetEmployeePinFailureCount
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class MobileDevicesController(
    private val acl: AccessControlList
) {
    @GetMapping("/mobile-devices")
    fun getMobileDevices(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam unitId: DaycareId
    ): ResponseEntity<List<SharedMobileDevice>> {
        Audit.MobileDevicesList.log(targetId = unitId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
        return db
            .read { it.listSharedDevices(unitId) }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping("/system/mobile-devices/{id}")
    fun getMobileDevice(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: MobileDeviceId
    ): ResponseEntity<MobileDevice> {
        Audit.MobileDevicesRead.log(targetId = id)
        user.assertSystemInternalUser()

        return db
            .read { it.getDevice(id) }
            .let { ResponseEntity.ok(it) }
    }

    data class RenameRequest(
        val name: String
    )
    @PutMapping("/mobile-devices/{id}/name")
    fun putMobileDeviceName(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: MobileDeviceId,
        @RequestBody body: RenameRequest
    ): ResponseEntity<Unit> {
        Audit.MobileDevicesRename.log(targetId = id)
        acl.getRolesForMobileDevice(user, id).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction { it.renameDevice(id, body.name) }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/mobile-devices/{id}")
    fun deleteMobileDevice(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: MobileDeviceId
    ): ResponseEntity<Unit> {
        Audit.MobileDevicesDelete.log(targetId = id)
        acl.getRolesForMobileDevice(user, id).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction { it.softDeleteDevice(id) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/mobile-devices/pin-login")
    fun pinLogin(
        db: Database.Connection,
        user: AuthenticatedUser.MobileDevice,
        @RequestBody params: PinLoginRequest
    ): PinLoginResponse {
        return db.transaction { tx ->
            if (tx.employeePinIsCorrect(params.employeeId, params.pin)) {
                tx.markEmployeeLastLogin(params.employeeId)
                tx.resetEmployeePinFailureCount(params.employeeId)
                tx.getEmployeeUser(params.employeeId)
                    ?.let { PinLoginResponse(PinLoginStatus.SUCCESS, Employee(it.firstName, it.lastName)) }
                    ?: PinLoginResponse(PinLoginStatus.WRONG_PIN)
            } else {
                if (tx.updateEmployeePinFailureCountAndCheckIfLocked(params.employeeId)) {
                    PinLoginResponse(PinLoginStatus.PIN_LOCKED)
                } else {
                    PinLoginResponse(PinLoginStatus.WRONG_PIN)
                }
            }
        }
    }
}

data class PinLoginRequest(
    val pin: String,
    val employeeId: EmployeeId
)

enum class PinLoginStatus {
    SUCCESS, WRONG_PIN, PIN_LOCKED
}

data class Employee(
    val firstName: String,
    val lastName: String
)

data class PinLoginResponse(
    val status: PinLoginStatus,
    val employee: Employee? = null
)
