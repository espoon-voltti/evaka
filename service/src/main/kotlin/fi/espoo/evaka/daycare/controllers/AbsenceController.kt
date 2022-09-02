// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.service.Absence
import fi.espoo.evaka.daycare.service.AbsenceDelete
import fi.espoo.evaka.daycare.service.AbsenceGroup
import fi.espoo.evaka.daycare.service.AbsenceService
import fi.espoo.evaka.daycare.service.AbsenceUpsert
import fi.espoo.evaka.daycare.service.batchDeleteAbsences
import fi.espoo.evaka.daycare.service.deleteChildAbsences
import fi.espoo.evaka.daycare.service.upsertAbsences
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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
    ): AbsenceGroup {
        Audit.AbsenceRead.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.READ_ABSENCES, groupId)
        return db.connect { dbc -> dbc.read { absenceService.getAbsencesByMonth(it, groupId, year, month) } }
    }

    @PostMapping("/{groupId}")
    fun upsertAbsences(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody absences: List<AbsenceUpsert>,
        @PathVariable groupId: GroupId
    ) {
        Audit.AbsenceUpdate.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.CREATE_ABSENCES, groupId)
        accessControl.requirePermissionFor(user, Action.Child.CREATE_ABSENCE, absences.map { it.childId })

        db.connect { dbc -> dbc.transaction { it.upsertAbsences(absences, user.evakaUserId) } }
    }

    @PostMapping("/{groupId}/delete")
    fun deleteAbsences(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody deletions: List<AbsenceDelete>,
        @PathVariable groupId: GroupId
    ) {
        Audit.AbsenceUpdate.log(targetId = groupId)
        accessControl.requirePermissionFor(user, Action.Group.DELETE_ABSENCES, groupId)
        accessControl.requirePermissionFor(user, Action.Child.DELETE_ABSENCE, deletions.map { it.childId })

        db.connect { dbc -> dbc.transaction { it.batchDeleteAbsences(deletions) } }
    }

    data class DeleteChildAbsenceBody(val date: LocalDate)

    @DeleteMapping("/by-child/{childId}")
    fun deleteAbsence(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @RequestBody body: DeleteChildAbsenceBody
    ) {
        Audit.AbsenceDelete.log(targetId = childId, objectId = body.date)
        accessControl.requirePermissionFor(user, Action.Child.DELETE_ABSENCE, childId)
        db.connect { dbc -> dbc.transaction { it.deleteChildAbsences(childId, body.date) } }
    }

    @GetMapping("/by-child/{childId}")
    fun getAbsencesByChild(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): List<Absence> {
        Audit.AbsenceRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_ABSENCES, childId)
        return db.connect { dbc -> dbc.read { absenceService.getAbsencesByChild(it, childId, year, month) } }
    }

    @GetMapping("/by-child/{childId}/future")
    fun getFutureAbsencesByChild(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<Absence> {
        Audit.AbsenceRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_FUTURE_ABSENCES, childId)
        return db.connect { dbc -> dbc.read { absenceService.getFutureAbsencesByChild(it, evakaClock, childId) } }
    }
}
