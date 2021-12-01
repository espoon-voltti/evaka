// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.getParentship
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.ParentshipService
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/parentships")
class ParentshipController(private val parentshipService: ParentshipService, private val accessControl: AccessControl) {
    @PostMapping
    fun createParentship(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: ParentshipRequest
    ) {
        Audit.ParentShipsCreate.log(targetId = body.headOfChildId, objectId = body.childId)
        accessControl.requirePermissionFor(user, Action.Person.CREATE_PARENTSHIP, body.headOfChildId)

        db.transaction {
            parentshipService.createParentship(
                it,
                body.childId.raw,
                body.headOfChildId.raw,
                body.startDate,
                body.endDate
            )
        }
    }

    @GetMapping
    fun getParentships(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam(value = "headOfChildId", required = false) headOfChildId: PersonId? = null,
        @RequestParam(value = "childId", required = false) childId: PersonId? = null
    ): List<Parentship> {
        Audit.ParentShipsRead.log(targetId = listOf(headOfChildId, childId))

        if ((childId != null) == (headOfChildId != null)) {
            throw BadRequest("One and only one of parameters headOfChildId and childId is required")
        }

        val personId = headOfChildId ?: childId
            ?: error("One of parameters headOfChildId and childId should be validated not to be null")

        accessControl.requirePermissionFor(user, Action.Person.READ_PARENTSHIPS, personId)

        return db.read {
            it.getParentships(
                headOfChildId = headOfChildId?.raw,
                childId = childId?.raw,
                includeConflicts = true
            )
        }
    }

    @GetMapping("/{id}")
    fun getParentship(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: ParentshipId
    ): Parentship {
        Audit.ParentShipsRead.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.Parentship.READ, id)

        return db.read { it.getParentship(id) }
            ?: throw NotFound("Not found")
    }

    @PutMapping("/{id}")
    fun updateParentship(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: ParentshipId,
        @RequestBody body: ParentshipUpdateRequest
    ) {
        Audit.ParentShipsUpdate.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.Parentship.UPDATE, id)

        return db.transaction {
            parentshipService.updateParentshipDuration(it, id, body.startDate, body.endDate)
        }
    }

    @PutMapping("/{id}/retry")
    fun retryPartnership(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") parentshipId: ParentshipId
    ) {
        Audit.ParentShipsRetry.log(targetId = parentshipId)
        accessControl.requirePermissionFor(user, Action.Parentship.RETRY, parentshipId)

        db.transaction { parentshipService.retryParentship(it, parentshipId) }
    }

    @DeleteMapping("/{id}")
    fun deleteParentship(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: ParentshipId
    ) {
        Audit.ParentShipsDelete.log(targetId = id)
        val parentship = db.transaction { it.getParentship(id) }

        if (parentship?.conflict == false) {
            accessControl.requirePermissionFor(user, Action.Parentship.DELETE, id)
        } else {
            accessControl.requirePermissionFor(user, Action.Parentship.DELETE_CONFLICTED_PARENTSHIP, id)
        }

        db.transaction { parentshipService.deleteParentship(it, id) }
    }

    data class ParentshipRequest(
        val headOfChildId: PersonId,
        val childId: PersonId,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    data class ParentshipUpdateRequest(
        val startDate: LocalDate,
        val endDate: LocalDate
    )
}
