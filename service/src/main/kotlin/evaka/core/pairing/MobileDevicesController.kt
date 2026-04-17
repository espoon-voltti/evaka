// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pairing

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.shared.DaycareId
import evaka.core.shared.MobileDeviceId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class MobileDevicesController(private val accessControl: AccessControl) {
    @GetMapping("/employee/mobile-devices")
    fun getMobileDevices(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
    ): List<MobileDevice> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Unit.READ_MOBILE_DEVICES,
                        unitId,
                    )
                    it.listSharedDevices(unitId)
                }
            }
            .also {
                Audit.MobileDevicesList.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @GetMapping("/employee/mobile-devices/personal")
    fun getPersonalMobileDevices(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<MobileDevice> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_PERSONAL_MOBILE_DEVICES,
                    )
                    it.listPersonalDevices(user.id)
                }
            }
            .also {
                Audit.MobileDevicesList.log(
                    targetId = AuditId(user.id),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    data class RenameRequest(val name: String)

    @PutMapping("/employee/mobile-devices/{id}/name")
    fun putMobileDeviceName(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: MobileDeviceId,
        @RequestBody body: RenameRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.MobileDevice.UPDATE_NAME,
                    id,
                )
                it.renameDevice(id, body.name)
            }
        }
        Audit.MobileDevicesRename.log(targetId = AuditId(id))
    }

    @DeleteMapping("/employee/mobile-devices/{id}")
    fun deleteMobileDevice(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: MobileDeviceId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.MobileDevice.DELETE, id)
                it.deleteDevice(id)
            }
        }
        Audit.MobileDevicesDelete.log(targetId = AuditId(id))
    }
}
