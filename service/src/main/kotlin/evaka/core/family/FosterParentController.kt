// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.family

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.person.PersonSummary
import evaka.core.shared.FosterParentId
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.core.user.EvakaUser
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
        val audit = AuditContext().add(parentId)
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
            .also { relationships ->
                audit
                    .add(relationships.map { it.relationshipId })
                    .add(relationships.map { it.child.id })
                    .log(Audit.FosterParentReadChildren, clock)
            }
    }

    @GetMapping("/by-child/{childId}")
    fun getFosterParents(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: PersonId,
    ): List<FosterParentRelationship> {
        val audit = AuditContext().add(childId)
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
            .also { relationships ->
                audit
                    .add(relationships.map { it.relationshipId })
                    .add(relationships.map { it.parent.id })
                    .log(Audit.FosterParentReadParents, clock)
            }
    }

    @PostMapping
    fun createFosterParentRelationship(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: CreateFosterParentRelationshipBody,
    ) {
        val audit =
            AuditContext().add(body.parentId).add(body.childId).observeDate(body.validDuring.start)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.CREATE_FOSTER_PARENT_RELATIONSHIP,
                        body.parentId,
                    )
                    tx.createFosterParentRelationship(body, user, clock.now()).also {
                        audit.add(it)
                    }
                }
            }
            .also { audit.log(Audit.FosterParentCreateRelationship, clock) }
    }

    @PostMapping("/{id}")
    fun updateFosterParentRelationshipValidity(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: FosterParentId,
        @RequestBody validDuring: DateRange,
    ) {
        val audit =
            AuditContext()
                .add(id)
                .observeDate(validDuring.start)
                .addMeta("validDuring", validDuring)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.FosterParent.UPDATE,
                        id,
                    )
                    val old =
                        tx.getFosterParentRelationship(id)
                            ?: throw NotFound("No foster parent relationship found with id $id")
                    audit.add(old.parentId).add(old.childId).observeDate(old.validDuring.start)
                    tx.updateFosterParentRelationshipValidity(id, validDuring, user, clock.now())
                }
            }
            .also { audit.log(Audit.FosterParentUpdateRelationship, clock) }
    }

    @DeleteMapping("/{id}")
    fun deleteFosterParentRelationship(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: FosterParentId,
    ) {
        val audit = AuditContext().add(id)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.FosterParent.DELETE,
                        id,
                    )
                    val old =
                        tx.getFosterParentRelationship(id)
                            ?: throw NotFound("No foster parent relationship found with id $id")
                    audit
                        .add(old.parentId)
                        .add(old.childId)
                        .observeDate(old.validDuring.start)
                        .addMeta("validDuring", old.validDuring)
                    tx.deleteFosterParentRelationship(id)
                }
            }
            .also { audit.log(Audit.FosterParentDeleteRelationship, clock) }
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
