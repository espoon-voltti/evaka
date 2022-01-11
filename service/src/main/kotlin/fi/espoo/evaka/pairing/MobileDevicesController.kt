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
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class MobileDevicesController(private val accessControl: AccessControl) {
    @GetMapping("/mobile-devices")
    fun getMobileDevices(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam unitId: DaycareId
    ): List<MobileDevice> {
        Audit.MobileDevicesList.log(targetId = unitId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_MOBILE_DEVICES, unitId)
        return db.connect { dbc -> dbc.read { it.listSharedDevices(unitId) } }
    }

    @GetMapping("/mobile-devices/personal")
    fun getMobileDevices(
        db: Database,
        user: AuthenticatedUser.Employee,
    ): List<MobileDevice> {
        Audit.MobileDevicesList.log(targetId = user.id)
        accessControl.requirePermissionFor(user, Action.Global.READ_PERSONAL_MOBILE_DEVICES)
        return db.connect { dbc -> dbc.read { it.listPersonalDevices(EmployeeId(user.id)) } }
    }

    @GetMapping("/system/mobile-devices/{id}")
    fun getMobileDevice(
        db: Database,
        user: AuthenticatedUser.SystemInternalUser,
        @PathVariable id: MobileDeviceId
    ): MobileDeviceDetails {
        Audit.MobileDevicesRead.log(targetId = id)
        // TODO access control
        return db.connect { dbc -> dbc.read { it.getDevice(id) } }
    }

    data class RenameRequest(
        val name: String
    )
    @PutMapping("/mobile-devices/{id}/name")
    fun putMobileDeviceName(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: MobileDeviceId,
        @RequestBody body: RenameRequest
    ) {
        Audit.MobileDevicesRename.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.MobileDevice.UPDATE_NAME, id)
        db.connect { dbc -> dbc.transaction { it.renameDevice(id, body.name) } }
    }

    @DeleteMapping("/mobile-devices/{id}")
    fun deleteMobileDevice(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: MobileDeviceId
    ) {
        Audit.MobileDevicesDelete.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.MobileDevice.DELETE, id)
        db.connect { dbc -> dbc.transaction { it.softDeleteDevice(id) } }
    }

    @PostMapping("/mobile-devices/pin-login")
    fun pinLogin(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        @RequestBody params: PinLoginRequest
    ): PinLoginResponse {
        return db.connect { dbc ->
            dbc.transaction { tx ->
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
