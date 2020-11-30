// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backupcare

import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.ClosedPeriod
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
class BackupCareController(private val acl: AccessControlList) {
    @GetMapping("/children/{childId}/backup-cares")
    fun getForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID
    ): ResponseEntity<ChildBackupCaresResponse> {
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
        val backupCares = db.read { it.handle.getBackupCaresForChild(childId) }
        return ResponseEntity.ok(ChildBackupCaresResponse(backupCares))
    }

    @PostMapping("/children/{childId}/backup-cares")
    fun createForChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID,
        @RequestBody body: NewBackupCare
    ): ResponseEntity<BackupCareCreateResponse> {
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        try {
            val id = db.transaction { it.handle.createBackupCare(childId, body) }
            return ResponseEntity.ok(BackupCareCreateResponse(id))
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @PostMapping("/backup-cares/{id}")
    fun update(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") id: UUID,
        @RequestBody body: BackupCareUpdateRequest
    ): ResponseEntity<Unit> {
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        try {
            db.transaction { it.handle.updateBackupCare(id, body.period, body.groupId) }
            return ResponseEntity.noContent().build()
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @DeleteMapping("/backup-cares/{id}")
    fun delete(db: Database.Connection, user: AuthenticatedUser, @PathVariable("id") id: UUID): ResponseEntity<Unit> {
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        db.transaction { it.handle.deleteBackupCare(id) }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/daycares/{daycareId}/backup-cares")
    fun getForDaycare(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<UnitBackupCaresResponse> {
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)
        val backupCares = db.read { it.handle.getBackupCaresForDaycare(daycareId, ClosedPeriod(startDate, endDate)) }
        return ResponseEntity.ok(UnitBackupCaresResponse(backupCares))
    }
}

data class ChildBackupCaresResponse(val backupCares: List<ChildBackupCare>)
data class UnitBackupCaresResponse(val backupCares: List<UnitBackupCare>)
data class BackupCareUpdateRequest(val period: ClosedPeriod, val groupId: UUID?)
data class BackupCareCreateResponse(val id: UUID)
