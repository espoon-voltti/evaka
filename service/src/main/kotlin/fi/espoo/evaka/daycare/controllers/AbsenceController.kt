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
        clock: EvakaClock,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @PathVariable groupId: GroupId
    ): AbsenceGroup {
        accessControl.requirePermissionFor(user, clock, Action.Group.READ_ABSENCES, groupId)
        return db.connect { dbc -> dbc.read { absenceService.getAbsencesByMonth(it, groupId, year, month) } }.also {
            Audit.AbsenceRead.log(
                targetId = groupId,
                mapOf("year" to year, "month" to month)
            )
        }
    }

    @PostMapping("/{groupId}")
    fun upsertAbsences(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody absences: List<AbsenceUpsert>,
        @PathVariable groupId: GroupId
    ) {
        val children = absences.map { it.childId }
        accessControl.requirePermissionFor(user, clock, Action.Group.CREATE_ABSENCES, groupId)
        accessControl.requirePermissionFor(user, clock, Action.Child.CREATE_ABSENCE, children)

        val upserted = db.connect { dbc -> dbc.transaction { it.upsertAbsences(clock, absences, user.evakaUserId) } }
        Audit.AbsenceUpsert.log(
            targetId = groupId,
            objectId = upserted,
            mapOf("children" to children)
        )
    }

    @PostMapping("/{groupId}/delete")
    fun deleteAbsences(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody deletions: List<AbsenceDelete>,
        @PathVariable groupId: GroupId
    ) {
        val children = deletions.map { it.childId }
        accessControl.requirePermissionFor(user, clock, Action.Group.DELETE_ABSENCES, groupId)
        accessControl.requirePermissionFor(user, clock, Action.Child.DELETE_ABSENCE, children)

        val deleted = db.connect { dbc -> dbc.transaction { it.batchDeleteAbsences(deletions) } }
        Audit.AbsenceDelete.log(
            targetId = groupId,
            objectId = deleted,
            mapOf("children" to children)
        )
    }

    data class DeleteChildAbsenceBody(val date: LocalDate)

    @DeleteMapping("/by-child/{childId}")
    fun deleteAbsence(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: DeleteChildAbsenceBody
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Child.DELETE_ABSENCE, childId)
        val deleted = db.connect { dbc -> dbc.transaction { it.deleteChildAbsences(childId, body.date) } }
        Audit.AbsenceDelete.log(
            targetId = childId,
            objectId = deleted,
            mapOf("date" to body.date)
        )
    }

    @GetMapping("/by-child/{childId}")
    fun getAbsencesByChild(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): List<Absence> {
        accessControl.requirePermissionFor(user, clock, Action.Child.READ_ABSENCES, childId)
        return db.connect { dbc -> dbc.read { absenceService.getAbsencesByChild(it, childId, year, month) } }.also {
            Audit.AbsenceRead.log(
                targetId = childId,
                mapOf("year" to year, "month" to month)
            )
        }
    }

    @GetMapping("/by-child/{childId}/future")
    fun getFutureAbsencesByChild(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<Absence> {
        accessControl.requirePermissionFor(user, clock, Action.Child.READ_FUTURE_ABSENCES, childId)
        return db.connect { dbc -> dbc.read { absenceService.getFutureAbsencesByChild(it, clock, childId) } }.also {
            Audit.AbsenceRead.log(targetId = childId)
        }
    }
}
