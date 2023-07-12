// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childdiscussion

import fi.espoo.evaka.Audit
import fi.espoo.evaka.children.Child
import fi.espoo.evaka.shared.ChildDiscussionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/child-discussions")
class ChildDiscussionController(private val accessControl: AccessControl) {

    @PostMapping("/{childId}")
    fun createDiscussion(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: ChildDiscussionBody
    ): ChildDiscussionId {
        return db.connect { dbc ->
            dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.CREATE_CHILD_DISCUSSION,
                        childId
                    )
                    it.getChildDiscussionDataForChild(childId)?.let {
                        throw Conflict("Discussion data for child already exists")
                    }
                    it.createChildDiscussion(childId, body)
                }
                .also { discussionId ->
                    Audit.ChildDiscussionCreate.log(targetId = childId, objectId = discussionId)
                }
        }
    }

    @PutMapping("/{childId}")
    fun updateDiscussion(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: ChildDiscussionBody
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.UPDATE_CHILD_DISCUSSION,
                        childId
                    )
                    tx.getChildDiscussionDataForChild(childId)
                        ?: throw NotFound("Discussion data for child does not exist")
                    tx.updateChildDiscussion(childId, body)
                }
            }
            .also { Audit.ChildDiscussionUpdate.log(targetId = childId) }
    }

    @GetMapping("/{childId}")
    fun getDiscussionData(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): ChildDiscussion? {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.READ_CHILD_DISCUSSION,
                        childId
                    )
                    it.getChildDiscussionDataForChild(childId)
                }
            }
            .also { Audit.ChildDiscussionRead.log(targetId = childId) }
    }
}
