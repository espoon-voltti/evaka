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
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
class AbsenceController(private val absenceService: AbsenceService, private val accessControl: AccessControl) {
    @GetMapping("/{groupId}")
    fun getAbsencesByGroupAndMonth(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @PathVariable groupId: GroupId
    ): ResponseEntity<Wrapper<AbsenceGroup>> {
        Audit.AbsenceRead.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.READ_ABSENCES, groupId)
        val absences = db.connect { dbc -> dbc.read { absenceService.getAbsencesByMonth(it, groupId, year, month) } }
        return ResponseEntity.ok(Wrapper(absences))
    }

    @PostMapping("/{groupId}")
    fun upsertAbsences(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody absences: Wrapper<List<Absence>>,
        @PathVariable groupId: GroupId
    ): ResponseEntity<Unit> {
        Audit.AbsenceUpdate.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.CREATE_ABSENCES, groupId)
        absences.data.forEach {
            accessControl.requirePermissionFor(user, Action.Child.CREATE_ABSENCE, it.childId)
        }

        db.connect { dbc -> dbc.transaction { absenceService.upsertAbsences(it, absences.data, user.id) } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{groupId}/delete")
    fun deleteAbsences(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody deletions: List<AbsenceDelete>,
        @PathVariable groupId: GroupId
    ): ResponseEntity<Unit> {
        Audit.AbsenceUpdate.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.DELETE_ABSENCES, groupId)
        deletions.forEach {
            accessControl.requirePermissionFor(user, Action.Child.DELETE_ABSENCE, it.childId)
        }

        db.connect { dbc -> dbc.transaction { it.batchDeleteAbsences(deletions) } }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/by-child/{childId}")
    fun getAbsencesByChild(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<Wrapper<AbsenceChildMinimal>> {
        Audit.AbsenceRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_ABSENCES, childId)
        val absences = db.connect { dbc -> dbc.read { absenceService.getAbsencesByChild(it, childId, year, month) } }
        return ResponseEntity.ok(Wrapper(absences))
    }

    @GetMapping("/by-child/{childId}/future")
    fun getFutureAbsencesByChild(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<List<Absence>> {
        Audit.AbsenceRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_FUTURE_ABSENCES, childId)
        val absences = db.connect { dbc -> dbc.read { absenceService.getFutureAbsencesByChild(it, childId) } }
        return ResponseEntity.ok(absences)
    }
}
