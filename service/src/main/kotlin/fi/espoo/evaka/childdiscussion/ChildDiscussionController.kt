// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childdiscussion

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildDiscussionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
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
                    it.createChildDiscussion(childId, body)
                }
                .also { discussionId ->
                    Audit.ChildDiscussionCreate.log(targetId = childId, objectId = discussionId)
                }
        }
    }

    @PutMapping("/{discussionId}")
    fun updateDiscussion(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable discussionId: ChildDiscussionId,
        @RequestBody body: ChildDiscussionBody
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDiscussion.UPDATE,
                        discussionId
                    )
                    tx.updateChildDiscussion(discussionId, body)
                }
            }
            .also { Audit.ChildDiscussionUpdate.log(targetId = discussionId) }
    }

    @DeleteMapping("/{discussionId}")
    fun deleteDiscussion(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable discussionId: ChildDiscussionId
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDiscussion.DELETE,
                        discussionId
                    )
                    tx.deleteChildDiscussion(discussionId)
                }
            }
            .also { Audit.ChildDiscussionDelete.log(targetId = discussionId) }
    }

    @GetMapping("/{childId}")
    fun getDiscussions(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<ChildDiscussionWithPermittedActions> {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_CHILD_DISCUSSION,
                        childId
                    )
                    val discussionData = tx.getChildDiscussions(childId)
                    val permittedActions =
                        accessControl.getPermittedActions<
                            ChildDiscussionId, Action.ChildDiscussion
                        >(
                            tx,
                            user,
                            clock,
                            discussionData.map { it.id }
                        )

                    discussionData.map { discussion ->
                        ChildDiscussionWithPermittedActions(
                            discussion,
                            permittedActions[discussion.id] ?: emptySet()
                        )
                    }
                }
            }
            .also { Audit.ChildDiscussionRead.log(targetId = childId) }
    }
}
