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
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/parentships")
class ParentshipController(
    private val parentshipService: ParentshipService,
    private val accessControl: AccessControl
) {
    @PostMapping
    fun createParentship(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: ParentshipRequest
    ) {
        Audit.ParentShipsCreate.log(targetId = body.headOfChildId, objectId = body.childId)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Person.CREATE_PARENTSHIP,
            body.headOfChildId
        )

        db.connect { dbc ->
            dbc.transaction {
                parentshipService.createParentship(
                    it,
                    clock,
                    body.childId,
                    body.headOfChildId,
                    body.startDate,
                    body.endDate
                )
            }
        }
    }

    @GetMapping
    fun getParentships(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam(value = "headOfChildId", required = false) headOfChildId: PersonId? = null,
        @RequestParam(value = "childId", required = false) childId: PersonId? = null
    ): List<ParentshipWithPermittedActions> {
        Audit.ParentShipsRead.log(targetId = listOf(headOfChildId, childId))

        if ((childId != null) == (headOfChildId != null)) {
            throw BadRequest("One and only one of parameters headOfChildId and childId is required")
        }

        val personId =
            headOfChildId
                ?: childId
                    ?: error(
                    "One of parameters headOfChildId and childId should be validated not to be null"
                )

        accessControl.requirePermissionFor(user, clock, Action.Person.READ_PARENTSHIPS, personId)

        return db.connect { dbc ->
            dbc.read { tx ->
                val parentships =
                    tx.getParentships(
                        headOfChildId = headOfChildId,
                        childId = childId,
                        includeConflicts = true
                    )
                val permittedActions =
                    accessControl.getPermittedActions<ParentshipId, Action.Parentship>(
                        tx,
                        user,
                        clock,
                        parentships.map { it.id }
                    )
                parentships.map {
                    ParentshipWithPermittedActions(it, permittedActions[it.id] ?: emptySet())
                }
            }
        }
    }

    @GetMapping("/{id}")
    fun getParentship(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "id") id: ParentshipId
    ): Parentship {
        Audit.ParentShipsRead.log(targetId = id)
        accessControl.requirePermissionFor(user, clock, Action.Parentship.READ, id)

        return db.connect { dbc -> dbc.read { it.getParentship(id) } } ?: throw NotFound()
    }

    @PutMapping("/{id}")
    fun updateParentship(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "id") id: ParentshipId,
        @RequestBody body: ParentshipUpdateRequest
    ) {
        Audit.ParentShipsUpdate.log(targetId = id)
        accessControl.requirePermissionFor(user, clock, Action.Parentship.UPDATE, id)

        return db.connect { dbc ->
            dbc.transaction {
                parentshipService.updateParentshipDuration(
                    it,
                    clock,
                    id,
                    body.startDate,
                    body.endDate
                )
            }
        }
    }

    @PutMapping("/{id}/retry")
    fun retryPartnership(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "id") parentshipId: ParentshipId
    ) {
        Audit.ParentShipsRetry.log(targetId = parentshipId)
        accessControl.requirePermissionFor(user, clock, Action.Parentship.RETRY, parentshipId)

        db.connect { dbc ->
            dbc.transaction { parentshipService.retryParentship(it, clock, parentshipId) }
        }
    }

    @DeleteMapping("/{id}")
    fun deleteParentship(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "id") id: ParentshipId
    ) {
        Audit.ParentShipsDelete.log(targetId = id)

        db.connect { dbc ->
            val parentship = dbc.transaction { it.getParentship(id) }

            if (parentship?.conflict == false) {
                accessControl.requirePermissionFor(user, clock, Action.Parentship.DELETE, id)
            } else {
                accessControl.requirePermissionFor(
                    user,
                    clock,
                    Action.Parentship.DELETE_CONFLICTED_PARENTSHIP,
                    id
                )
            }

            dbc.transaction { parentshipService.deleteParentship(it, clock, id) }
        }
    }

    data class ParentshipRequest(
        val headOfChildId: PersonId,
        val childId: PersonId,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    data class ParentshipUpdateRequest(val startDate: LocalDate, val endDate: LocalDate)

    data class ParentshipWithPermittedActions(
        val data: Parentship,
        val permittedActions: Set<Action.Parentship>
    )
}
