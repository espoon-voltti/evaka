// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.Wrapper
import fi.espoo.evaka.daycare.service.Absence
import fi.espoo.evaka.daycare.service.AbsenceChildMinimal
import fi.espoo.evaka.daycare.service.AbsenceGroup
import fi.espoo.evaka.daycare.service.AbsenceService
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole.ADMIN
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.STAFF
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
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
        user: AuthenticatedUser,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @PathVariable groupId: UUID
    ): ResponseEntity<Wrapper<AbsenceGroup>> {
        Audit.AbsenceRead.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF)
        val absences = absenceService.getAbsencesByMonth(groupId, year, month)
        return ResponseEntity.ok(Wrapper(absences))
    }

    @PostMapping("/{groupId}")
    fun upsertAbsences(
        user: AuthenticatedUser,
        @RequestBody absences: Wrapper<List<Absence>>,
        @PathVariable groupId: UUID
    ): ResponseEntity<Unit> {
        Audit.AbsenceUpdate.log(targetId = groupId)
        acl.getRolesForUnitGroup(user, groupId)
            .requireOneOfRoles(ADMIN, UNIT_SUPERVISOR, STAFF)
        absenceService.upsertAbsences(absences.data, groupId, user.id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/by-child/{childId}")
    fun getAbsencesByChild(
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<Wrapper<AbsenceChildMinimal>> {
        Audit.AbsenceRead.log(targetId = childId)
        user.requireOneOfRoles(ADMIN, UNIT_SUPERVISOR, FINANCE_ADMIN)
        val absences = absenceService.getAbscencesByChild(childId, year, month)
        return ResponseEntity.ok(Wrapper(absences))
    }

    @PostMapping("/child/{childId}")
    fun upsertAbsence(
        user: AuthenticatedUser,
        @RequestBody absenceType: AbsenceBody,
        @PathVariable childId: UUID
    ): ResponseEntity<Unit> {
        Audit.ChildAbsenceUpdate.log(targetId = childId)
        user.requireOneOfRoles(ADMIN)
        absenceService.upsertChildAbsence(childId, absenceType.absenceType, absenceType.careType, user.id)
        return ResponseEntity.noContent().build()
    }
}

data class AbsenceBody(
    val absenceType: AbsenceType,
    val careType: CareType
)
