// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.pis.getEmployeeUser
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
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
        clock: EvakaClock,
        @RequestParam unitId: DaycareId
    ): List<MobileDevice> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Unit.READ_MOBILE_DEVICES,
                        unitId
                    )
                    it.listSharedDevices(unitId)
                }
            }
            .also {
                Audit.MobileDevicesList.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("count" to it.size)
                )
            }
    }

    @GetMapping("/mobile-devices/personal")
    fun getPersonalMobileDevices(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): List<MobileDevice> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_PERSONAL_MOBILE_DEVICES
                    )
                    it.listPersonalDevices(user.id)
                }
            }
            .also {
                Audit.MobileDevicesList.log(
                    targetId = AuditId(user.id),
                    meta = mapOf("count" to it.size)
                )
            }
    }

    data class RenameRequest(val name: String)

    @PutMapping("/mobile-devices/{id}/name")
    fun putMobileDeviceName(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: MobileDeviceId,
        @RequestBody body: RenameRequest
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.MobileDevice.UPDATE_NAME,
                    id
                )
                it.renameDevice(id, body.name)
            }
        }
        Audit.MobileDevicesRename.log(targetId = AuditId(id))
    }

    @DeleteMapping("/mobile-devices/{id}")
    fun deleteMobileDevice(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: MobileDeviceId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.MobileDevice.DELETE, id)
                it.deleteDevice(id)
            }
        }
        Audit.MobileDevicesDelete.log(targetId = AuditId(id))
    }

    @PostMapping("/mobile-devices/pin-login")
    fun pinLogin(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestBody params: PinLoginRequest
    ): PinLoginResponse =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val employee = tx.getEmployeeUser(params.employeeId)
                    if (employee?.active == false) {
                        throw Forbidden("User is not active")
                    }
                    when (accessControl.verifyPinCode(tx, params.employeeId, params.pin, clock)) {
                        AccessControl.PinError.PIN_LOCKED ->
                            PinLoginResponse(PinLoginStatus.PIN_LOCKED)
                        AccessControl.PinError.WRONG_PIN ->
                            PinLoginResponse(PinLoginStatus.WRONG_PIN)
                        null -> {
                            employee?.let {
                                PinLoginResponse(
                                    PinLoginStatus.SUCCESS,
                                    Employee(it.preferredFirstName ?: it.firstName, it.lastName)
                                )
                            } ?: PinLoginResponse(PinLoginStatus.WRONG_PIN)
                        }
                    }
                }
            }
            .also {
                Audit.PinLogin.log(
                    targetId = AuditId(params.employeeId),
                    meta = mapOf("status" to it.status)
                )
            }
}

data class PinLoginRequest(val pin: String, val employeeId: EmployeeId)

enum class PinLoginStatus {
    SUCCESS,
    WRONG_PIN,
    PIN_LOCKED
}

data class Employee(val firstName: String, val lastName: String)

data class PinLoginResponse(val status: PinLoginStatus, val employee: Employee? = null)
