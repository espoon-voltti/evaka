package fi.espoo.evaka.pairing

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MobileDevicesController(
    private val acl: AccessControlList
) {
    @GetMapping("/mobile-devices")
    fun getMobileDevices(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam unitId: UUID
    ): ResponseEntity<List<MobileDevice>> {
        Audit.MobileDevicesRead.log(targetId = unitId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
        return db
            .read { tx -> listDevices(tx, unitId) }
            .let { ResponseEntity.ok(it) }
    }

    data class RenameRequest(
        val name: String
    )
    @PutMapping("/mobile-devices/{id}/name")
    fun putMobileDeviceName(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: RenameRequest
    ): ResponseEntity<Unit> {
        Audit.MobileDevicesRename.log(targetId = id)
        acl.getRolesForMobileDevice(user, id).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction { tx -> renameDevice(tx, id, body.name) }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/apigw/mobile-devices/{id}")
    fun deleteMobileDevice(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        Audit.MobileDevicesDelete.log(targetId = id)

        db.transaction { tx -> softDeleteDevice(tx, id) }
        return ResponseEntity.noContent().build()
    }
}
