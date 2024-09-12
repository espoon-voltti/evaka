// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.getParentship
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.ParentshipDetailed
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
@RequestMapping("/employee/parentships")
class ParentshipController(
    private val parentshipService: ParentshipService,
    private val accessControl: AccessControl,
) {
    @PostMapping
    fun createParentship(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ParentshipRequest,
    ) {
        val parentship =
            db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.CREATE_PARENTSHIP,
                        body.headOfChildId,
                    )
                    parentshipService.createParentship(
                        it,
                        clock,
                        body.childId,
                        body.headOfChildId,
                        body.startDate,
                        body.endDate,
                        Creator.User(user.evakaUserId),
                    )
                }
            }
        Audit.ParentShipsCreate.log(
            targetId = AuditId(listOf(body.headOfChildId, body.childId)),
            objectId = AuditId(parentship.id),
        )
    }

    @GetMapping
    fun getParentships(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam headOfChildId: PersonId? = null,
        @RequestParam childId: PersonId? = null,
    ): List<ParentshipWithPermittedActions> {
        if ((childId != null) == (headOfChildId != null)) {
            throw BadRequest("One and only one of parameters headOfChildId and childId is required")
        }

        val personId =
            headOfChildId
                ?: childId
                ?: error(
                    "One of parameters headOfChildId and childId should be validated not to be null"
                )

        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_PARENTSHIPS,
                        personId,
                    )
                    val parentships =
                        tx.getParentships(
                            headOfChildId = headOfChildId,
                            childId = childId,
                            includeConflicts = true,
                        )
                    val permittedActions =
                        accessControl.getPermittedActions<ParentshipId, Action.Parentship>(
                            tx,
                            user,
                            clock,
                            parentships.map { it.id },
                        )
                    parentships.map {
                        ParentshipWithPermittedActions(it, permittedActions[it.id] ?: emptySet())
                    }
                }
            }
            .also {
                Audit.ParentShipsRead.log(
                    targetId = AuditId(listOfNotNull(headOfChildId, childId)),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @GetMapping("/{id}")
    fun getParentship(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ParentshipId,
    ): Parentship {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(it, user, clock, Action.Parentship.READ, id)
                    it.getParentship(id)
                } ?: throw NotFound()
            }
            .also { Audit.ParentShipsRead.log(targetId = AuditId(id)) }
    }

    @PutMapping("/{id}")
    fun updateParentship(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ParentshipId,
        @RequestBody body: ParentshipUpdateRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Parentship.UPDATE, id)
                parentshipService.updateParentshipDuration(
                    tx = it,
                    clock = clock,
                    user = user,
                    id = id,
                    startDate = body.startDate,
                    endDate = body.endDate,
                )
            }
        }
        Audit.ParentShipsUpdate.log(
            targetId = AuditId(id),
            meta = mapOf("startDate" to body.startDate, "endDate" to body.endDate),
        )
    }

    @PutMapping("/{id}/retry")
    fun retryParentship(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ParentshipId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Parentship.RETRY, id)
                parentshipService.retryParentship(it, user, clock, id)
            }
        }
        Audit.ParentShipsRetry.log(targetId = AuditId(id))
    }

    @DeleteMapping("/{id}")
    fun deleteParentship(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ParentshipId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val parentship = tx.getParentship(id)

                if (parentship?.conflict == false) {
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Parentship.DELETE,
                        id,
                    )
                } else {
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Parentship.DELETE_CONFLICTED_PARENTSHIP,
                        id,
                    )
                }

                parentshipService.deleteParentship(tx, clock, id)
            }
        }
        Audit.ParentShipsDelete.log(targetId = AuditId(id))
    }

    data class ParentshipRequest(
        val headOfChildId: PersonId,
        val childId: PersonId,
        val startDate: LocalDate,
        val endDate: LocalDate,
    )

    data class ParentshipUpdateRequest(val startDate: LocalDate, val endDate: LocalDate)

    data class ParentshipWithPermittedActions(
        val data: ParentshipDetailed,
        val permittedActions: Set<Action.Parentship>,
    )
}
