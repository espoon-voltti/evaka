// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backuppickup

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BackupPickupController(private val accessControl: AccessControl) {
    @PostMapping("/children/{childId}/backup-pickups")
    fun createBackupPickup(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("childId") childId: ChildId,
        @RequestBody body: ChildBackupPickupContent
    ): ChildBackupPickupCreateResponse {
        return ChildBackupPickupCreateResponse(
            db.connect { dbc ->
                    dbc.transaction { tx ->
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Child.CREATE_BACKUP_PICKUP,
                            childId
                        )
                        tx.createBackupPickup(childId, body)
                    }
                }
                .also { backupPickupId ->
                    Audit.ChildBackupPickupCreate.log(targetId = childId, objectId = backupPickupId)
                }
        )
    }

    @GetMapping("/children/{childId}/backup-pickups")
    fun getBackupPickups(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("childId") childId: ChildId
    ): List<ChildBackupPickup> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_BACKUP_PICKUP,
                        childId
                    )
                    tx.getBackupPickupsForChild(childId)
                }
            }
            .also {
                Audit.ChildBackupPickupRead.log(
                    targetId = childId,
                    meta = mapOf("count" to it.size)
                )
            }
    }

    @PutMapping("/backup-pickups/{id}")
    fun updateBackupPickup(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("id") id: BackupPickupId,
        @RequestBody body: ChildBackupPickupContent
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.BackupPickup.UPDATE, id)
                tx.updateBackupPickup(id, body)
            }
        }
        Audit.ChildBackupPickupUpdate.log(targetId = id)
    }

    @DeleteMapping("/backup-pickups/{id}")
    fun deleteBackupPickup(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable("id") id: BackupPickupId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.BackupPickup.DELETE, id)
                tx.deleteBackupPickup(id)
            }
        }
        Audit.ChildBackupPickupDelete.log(targetId = id)
    }
}

fun Database.Transaction.createBackupPickup(
    childId: ChildId,
    data: ChildBackupPickupContent
): BackupPickupId {
    // language=sql
    val sql =
        """
        INSERT INTO backup_pickup (child_id, name, phone)
        VALUES (:childId, :name, :phone)
        RETURNING id
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    return this.createQuery(sql)
        .bind("childId", childId)
        .bind("name", data.name)
        .bind("phone", data.phone)
        .exactlyOne<BackupPickupId>()
}

fun Database.Read.getBackupPickupsForChild(childId: ChildId): List<ChildBackupPickup> {
    @Suppress("DEPRECATION")
    return createQuery("SELECT id, child_id, name, phone FROM backup_pickup WHERE child_Id = :id")
        .bind("id", childId)
        .toList<ChildBackupPickup>()
}

fun Database.Transaction.updateBackupPickup(id: BackupPickupId, data: ChildBackupPickupContent) {
    // language=sql
    val sql =
        """
        UPDATE backup_pickup
        SET name = :name, phone = :phone
        WHERE id  = :id
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    this.createUpdate(sql)
        .bind("id", id)
        .bind("name", data.name)
        .bind("phone", data.phone)
        .updateExactlyOne()
}

fun Database.Transaction.deleteBackupPickup(id: BackupPickupId) {
    @Suppress("DEPRECATION")
    this.createUpdate("DELETE FROM backup_pickup WHERE id = :id").bind("id", id).updateExactlyOne()
}

data class ChildBackupPickupContent(val name: String, val phone: String)

data class ChildBackupPickup(
    val id: BackupPickupId,
    val childId: ChildId,
    val name: String,
    val phone: String
)

data class ChildBackupPickupCreateResponse(val id: BackupPickupId)
