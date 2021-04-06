// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backuppickup

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class BackupPickupController(private val acl: AccessControlList) {
    @GetMapping("/children/{childId}/backup-pickups")
    fun getForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID
    ): ResponseEntity<List<ChildBackupPickup>> {
        Audit.ChildBackupPickupRead.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)

        return db.transaction { tx ->
            tx.getBackupPickupsForChild(childId)
        }.let { ResponseEntity.ok(it) }
    }

    @PostMapping("/children/{childId}/backup-pickups")
    fun createForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID,
        @RequestBody body: NewChildBackupPickup
    ): ResponseEntity<ChildBackupPickupCreateResponse> {
        Audit.ChildBackupPickupCreate.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
        return db.transaction { tx ->
            tx.createBackupPickup(
                body
            )
        }.let {
            ResponseEntity.ok(ChildBackupPickupCreateResponse(it))
        }
    }

    @PutMapping("/children/{childId}/backup-pickups")
    fun updateForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID,
        @RequestBody body: ChildBackupPickup
    ): ResponseEntity<Unit> {
        Audit.ChildBackupPickupUpdate.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
        return db.transaction { tx ->
            tx.updateBackupPickup(
                body
            )
        }.let {
            ResponseEntity.noContent().build()
        }
    }

    @DeleteMapping("/children/{childId}/backup-pickups/{id}")
    fun delete(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") id: UUID,
        @PathVariable("childId") childId: UUID
    ): ResponseEntity<Unit> {
        Audit.ChildBackupPickupDelete.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
        db.transaction { tx ->
            tx.deleteBackupPickup(id)
        }
        return ResponseEntity.noContent().build()
    }
}

fun Database.Transaction.deleteBackupPickup(
    id: UUID
) {
    val deleted = this.createUpdate("DELETE FROM backup_pickup WHERE id = :id")
        .bind("id", id)
        .execute()

    if (deleted == 0) throw NotFound("No backup pickup $id found")
}

fun Database.Read.getBackupPickupsForChild(childId: UUID): List<ChildBackupPickup> {
    return createQuery("SELECT * FROM backup_pickup WHERE child_Id = :id")
        .bind("id", childId)
        .mapTo<ChildBackupPickup>()
        .list()
}

fun Database.Transaction.createBackupPickup(
    backupPickup: NewChildBackupPickup
): UUID {
    // language=sql
    val sql = """
        INSERT INTO backup_pickup (child_id, name, phone)
        VALUES (:childId, :name, :phone)
        RETURNING id
    """.trimIndent()
    return this.createQuery(sql)
        .bind("childId", backupPickup.childId)
        .bind("name", backupPickup.name)
        .bind("phone", backupPickup.phone)
        .mapTo<UUID>()
        .first()
}

fun Database.Transaction.updateBackupPickup(
    backupPickup: ChildBackupPickup
) {
    // language=sql
    val sql = """
        UPDATE backup_pickup
        SET
            name = :name,
            phone = :phone
        WHERE id  = :id
    """.trimIndent()
    val updated = this.createUpdate(sql)
        .bind("id", backupPickup.id)
        .bind("name", backupPickup.name)
        .bind("phone", backupPickup.phone)
        .execute()

    if (updated == 0) throw NotFound("No backup pickup ${backupPickup.id} found")
}

data class NewChildBackupPickup(
    val childId: UUID,
    val name: String,
    val phone: String
)
data class ChildBackupPickup(
    val id: UUID,
    val childId: UUID,
    val name: String,
    val phone: String
)
data class ChildBackupPickupCreateResponse(val id: UUID)
