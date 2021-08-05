// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backuppickup

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
class BackupPickupController(private val accessControl: AccessControl) {
    @PostMapping("/children/{childId}/backup-pickups")
    fun createForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID,
        @RequestBody body: ChildBackupPickupContent
    ): ResponseEntity<ChildBackupPickupCreateResponse> {
        Audit.ChildBackupPickupCreate.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.CREATE_BACKUP_PICKUP, childId)
        return db.transaction { tx ->
            tx.createBackupPickup(childId, body)
        }.let {
            ResponseEntity.ok(ChildBackupPickupCreateResponse(it))
        }
    }

    @GetMapping("/children/{childId}/backup-pickups")
    fun getForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID
    ): ResponseEntity<List<ChildBackupPickup>> {
        Audit.ChildBackupPickupRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_BACKUP_PICKUP, childId)

        return db.transaction { tx ->
            tx.getBackupPickupsForChild(childId)
        }.let { ResponseEntity.ok(it) }
    }

    @PutMapping("/backup-pickups/{id}")
    fun update(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") id: BackupPickupId,
        @RequestBody body: ChildBackupPickupContent
    ): ResponseEntity<Unit> {
        Audit.ChildBackupPickupUpdate.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.BackupPickup.UPDATE, id)
        return db.transaction { tx ->
            tx.updateBackupPickup(id, body)
        }.let {
            ResponseEntity.noContent().build()
        }
    }

    @DeleteMapping("/backup-pickups/{id}")
    fun delete(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") id: BackupPickupId
    ): ResponseEntity<Unit> {
        Audit.ChildBackupPickupDelete.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.BackupPickup.DELETE, id)
        db.transaction { tx ->
            tx.deleteBackupPickup(id)
        }
        return ResponseEntity.noContent().build()
    }
}

fun Database.Transaction.createBackupPickup(
    childId: UUID,
    data: ChildBackupPickupContent
): BackupPickupId {
    // language=sql
    val sql = """
        INSERT INTO backup_pickup (child_id, name, phone)
        VALUES (:childId, :name, :phone)
        RETURNING id
    """.trimIndent()

    return this.createQuery(sql)
        .bind("childId", childId)
        .bind("name", data.name)
        .bind("phone", data.phone)
        .mapTo<BackupPickupId>()
        .one()
}

fun Database.Read.getBackupPickupsForChild(childId: UUID): List<ChildBackupPickup> {
    return createQuery("SELECT id, child_id, name, phone FROM backup_pickup WHERE child_Id = :id")
        .bind("id", childId)
        .mapTo<ChildBackupPickup>()
        .list()
}

fun Database.Transaction.updateBackupPickup(
    id: BackupPickupId,
    data: ChildBackupPickupContent
) {
    // language=sql
    val sql = """
        UPDATE backup_pickup
        SET name = :name, phone = :phone
        WHERE id  = :id
    """.trimIndent()

    this.createUpdate(sql)
        .bind("id", id)
        .bind("name", data.name)
        .bind("phone", data.phone)
        .updateExactlyOne()
}

fun Database.Transaction.deleteBackupPickup(
    id: BackupPickupId
) {
    this.createUpdate("DELETE FROM backup_pickup WHERE id = :id")
        .bind("id", id)
        .updateExactlyOne()
}

data class ChildBackupPickupContent(
    val name: String,
    val phone: String
)

data class ChildBackupPickup(
    val id: BackupPickupId,
    val childId: UUID,
    val name: String,
    val phone: String
)

data class ChildBackupPickupCreateResponse(val id: BackupPickupId)
