// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backupcare

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.JdbiException
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class BackupCareController(private val accessControl: AccessControl) {
    @GetMapping("/children/{childId}/backup-cares")
    fun getForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID
    ): ResponseEntity<ChildBackupCaresResponse> {
        Audit.ChildBackupCareRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_BACKUP_CARE, childId)
        val backupCares = db.read { it.getBackupCaresForChild(childId) }
        return ResponseEntity.ok(ChildBackupCaresResponse(backupCares))
    }

    @PostMapping("/children/{childId}/backup-cares")
    fun createForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID,
        @RequestBody body: NewBackupCare
    ): ResponseEntity<BackupCareCreateResponse> {
        Audit.ChildBackupCareCreate.log(targetId = childId, objectId = body.unitId)
        accessControl.requirePermissionFor(user, Action.Child.CREATE_BACKUP_CARE, childId)
        try {
            val id = db.transaction { it.createBackupCare(childId, body) }
            return ResponseEntity.ok(BackupCareCreateResponse(id))
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @PostMapping("/backup-cares/{id}")
    fun update(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") backupCareId: BackupCareId,
        @RequestBody body: BackupCareUpdateRequest
    ): ResponseEntity<Unit> {
        Audit.BackupCareUpdate.log(targetId = backupCareId, objectId = body.groupId)
        accessControl.requirePermissionFor(user, Action.BackupCare.UPDATE, backupCareId)
        try {
            db.transaction { it.updateBackupCare(backupCareId, body.period, body.groupId) }
            return ResponseEntity.noContent().build()
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @DeleteMapping("/backup-cares/{id}")
    fun delete(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") backupCareId: BackupCareId
    ): ResponseEntity<Unit> {
        Audit.BackupCareDelete.log(targetId = backupCareId)
        accessControl.requirePermissionFor(user, Action.BackupCare.DELETE, backupCareId)
        db.transaction { it.deleteBackupCare(backupCareId) }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/daycares/{daycareId}/backup-cares")
    fun getForDaycare(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: DaycareId,
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<UnitBackupCaresResponse> {
        Audit.DaycareBackupCareRead.log(targetId = daycareId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_BACKUP_CARE, daycareId)
        val backupCares = db.read { it.getBackupCaresForDaycare(daycareId, FiniteDateRange(startDate, endDate)) }
        return ResponseEntity.ok(UnitBackupCaresResponse(backupCares))
    }
}

data class ChildBackupCaresResponse(val backupCares: List<ChildBackupCare>)
data class UnitBackupCaresResponse(val backupCares: List<UnitBackupCare>)
data class BackupCareUpdateRequest(val period: FiniteDateRange, val groupId: GroupId?)
data class BackupCareCreateResponse(val id: BackupCareId)
