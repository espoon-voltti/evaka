// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backupcare

import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole.ADMIN
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.STAFF
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.config.Roles.FINANCE_ADMIN
import fi.espoo.evaka.shared.config.Roles.SERVICE_WORKER
import fi.espoo.evaka.shared.config.Roles.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.ClosedPeriod
import org.jdbi.v3.core.Jdbi
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
class BackupCareController(private val jdbi: Jdbi, private val acl: AccessControlList) {
    @GetMapping("/children/{childId}/backup-cares")
    fun getForChild(
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID
    ): ResponseEntity<ChildBackupCaresResponse> {
        acl.getRolesForChild(user, childId).requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF)
        val backupCares = jdbi.handle { it.getBackupCaresForChild(childId) }
        return ResponseEntity.ok(ChildBackupCaresResponse(backupCares))
    }

    @PostMapping("/children/{childId}/backup-cares")
    fun createForChild(
        user: AuthenticatedUser,
        @PathVariable("childId") childId: UUID,
        @RequestBody body: NewBackupCare
    ): ResponseEntity<BackupCareCreateResponse> {
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)
        try {
            val id = jdbi.transaction { it.createBackupCare(childId, body) }
            return ResponseEntity.ok(BackupCareCreateResponse(id))
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @PostMapping("/backup-cares/{id}")
    fun update(
        user: AuthenticatedUser,
        @PathVariable("id") id: UUID,
        @RequestBody body: BackupCareUpdateRequest
    ): ResponseEntity<Unit> {
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)
        try {
            jdbi.transaction { it.updateBackupCare(id, body.period, body.groupId) }
            return ResponseEntity.noContent().build()
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @DeleteMapping("/backup-cares/{id}")
    fun delete(user: AuthenticatedUser, @PathVariable("id") id: UUID): ResponseEntity<Unit> {
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR)
        jdbi.transaction { it.deleteBackupCare(id) }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/daycares/{daycareId}/backup-cares")
    fun getForDaycare(
        user: AuthenticatedUser,
        @PathVariable("daycareId") daycareId: UUID,
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<UnitBackupCaresResponse> {
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF)
        val backupCares = jdbi.transaction { it.getBackupCaresForDaycare(daycareId, ClosedPeriod(startDate, endDate)) }
        return ResponseEntity.ok(UnitBackupCaresResponse(backupCares))
    }
}

data class ChildBackupCaresResponse(val backupCares: List<ChildBackupCare>)
data class UnitBackupCaresResponse(val backupCares: List<UnitBackupCare>)
data class BackupCareUpdateRequest(val period: ClosedPeriod, val groupId: UUID?)
data class BackupCareCreateResponse(val id: UUID)
