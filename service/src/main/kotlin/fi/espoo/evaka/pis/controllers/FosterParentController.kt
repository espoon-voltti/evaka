// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.pis.PersonSummary
import fi.espoo.evaka.pis.createFosterParentRelationship
import fi.espoo.evaka.pis.deleteFosterParentRelationship
import fi.espoo.evaka.pis.getFosterChildren
import fi.espoo.evaka.pis.getFosterParents
import fi.espoo.evaka.pis.updateFosterParentRelationshipValidity
import fi.espoo.evaka.shared.FosterParentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.user.EvakaUser
import org.jdbi.v3.core.mapper.Nested
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/foster-parent")
class FosterParentController(private val accessControl: AccessControl) {
    @GetMapping("/by-parent/{parentId}")
    fun getFosterChildren(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable parentId: PersonId,
    ): List<FosterParentRelationship> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_FOSTER_CHILDREN,
                        parentId,
                    )
                    tx.getFosterChildren(parentId)
                }
            }
            .also { Audit.FosterParentReadChildren.log(targetId = AuditId(parentId)) }
    }

    @GetMapping("/by-child/{childId}")
    fun getFosterParents(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: PersonId,
    ): List<FosterParentRelationship> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_FOSTER_PARENTS,
                        childId,
                    )
                    tx.getFosterParents(childId)
                }
            }
            .also { Audit.FosterParentReadParents.log(targetId = AuditId(childId)) }
    }

    @PostMapping
    fun createFosterParentRelationship(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: CreateFosterParentRelationshipBody,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.CREATE_FOSTER_PARENT_RELATIONSHIP,
                        body.parentId,
                    )
                    tx.createFosterParentRelationship(body, user, clock.now())
                }
            }
            .also { id ->
                Audit.FosterParentCreateRelationship.log(
                    targetId = AuditId(body.parentId),
                    objectId = AuditId(body.childId),
                    meta = mapOf("fosterParentId" to id),
                )
            }
    }

    @PostMapping("/{id}")
    fun updateFosterParentRelationshipValidity(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: FosterParentId,
        @RequestBody validDuring: DateRange,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.FosterParent.UPDATE,
                        id,
                    )
                    tx.updateFosterParentRelationshipValidity(id, validDuring, user, clock.now())
                }
            }
            .also {
                Audit.FosterParentUpdateRelationship.log(
                    targetId = AuditId(id),
                    meta = mapOf("validDuring" to validDuring),
                )
            }
    }

    @DeleteMapping("/{id}")
    fun deleteFosterParentRelationship(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: FosterParentId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.FosterParent.DELETE,
                        id,
                    )
                    tx.deleteFosterParentRelationship(id)
                }
            }
            .also { Audit.FosterParentDeleteRelationship.log(targetId = AuditId(id)) }
    }
}

data class FosterParentRelationship(
    val relationshipId: FosterParentId,
    @Nested("child") val child: PersonSummary,
    @Nested("parent") val parent: PersonSummary,
    val validDuring: DateRange,
    val modifiedAt: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
)

data class CreateFosterParentRelationshipBody(
    val childId: PersonId,
    val parentId: PersonId,
    val validDuring: DateRange,
)
