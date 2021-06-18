// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.Wrapper
import fi.espoo.evaka.daycare.service.Absence
import fi.espoo.evaka.daycare.service.AbsenceChildMinimal
import fi.espoo.evaka.daycare.service.AbsenceDelete
import fi.espoo.evaka.daycare.service.AbsenceGroup
import fi.espoo.evaka.daycare.service.AbsenceService
import fi.espoo.evaka.daycare.service.batchDeleteAbsences
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/absences")
class AbsenceController(private val absenceService: AbsenceService, private val acl: AccessControlList) {
    @GetMapping("/{groupId}")
    fun getAbsencesByGroupAndMonth(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @PathVariable groupId: UUID
    ): ResponseEntity<Wrapper<AbsenceGroup>> {
        Audit.AbsenceRead.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
        val absences = db.read { absenceService.getAbsencesByMonth(it, groupId, year, month) }
        return ResponseEntity.ok(Wrapper(absences))
    }

    @PostMapping("/{groupId}")
    fun upsertAbsences(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody absences: Wrapper<List<Absence>>,
        @PathVariable groupId: UUID
    ): ResponseEntity<Unit> {
        Audit.AbsenceUpdate.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
        absences.data.map { it.childId }.forEach {
            acl.getRolesForChild(user, it).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
        }

        db.transaction { absenceService.upsertAbsences(it, absences.data, user.id) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{groupId}/delete")
    fun deleteAbsences(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody deletions: List<AbsenceDelete>,
        @PathVariable groupId: UUID
    ): ResponseEntity<Unit> {
        Audit.AbsenceUpdate.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)
        deletions.map { it.childId }.forEach {
            acl.getRolesForChild(user, it).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)
        }

        db.transaction { it.batchDeleteAbsences(deletions) }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/by-child/{childId}")
    fun getAbsencesByChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<Wrapper<AbsenceChildMinimal>> {
        Audit.AbsenceRead.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)
        val absences = db.read { absenceService.getAbscencesByChild(it, childId, year, month) }
        return ResponseEntity.ok(Wrapper(absences))
    }

    @GetMapping("/by-child/{childId}/future")
    fun getFutureAbsencesByChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<List<Absence>> {
        Audit.AbsenceRead.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN, UserRole.MOBILE)
        val absences = db.read { absenceService.getFutureAbsencesByChild(it, childId) }
        return ResponseEntity.ok(absences)
    }
}
