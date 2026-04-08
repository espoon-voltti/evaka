// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.note

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.note.child.daily.ChildDailyNote
import evaka.core.note.child.daily.getChildDailyNotesForGroup
import evaka.core.note.child.sticky.ChildStickyNote
import evaka.core.note.child.sticky.getChildStickyNotesForGroup
import evaka.core.note.group.GroupNote
import evaka.core.note.group.getGroupNotesForGroup
import evaka.core.shared.GroupId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class NotesController(private val ac: AccessControl) {
    data class NotesByGroupResponse(
        val childDailyNotes: List<ChildDailyNote>,
        val childStickyNotes: List<ChildStickyNote>,
        val groupNotes: List<GroupNote>,
    )

    @GetMapping("/employee/daycare-groups/{groupId}/notes")
    fun getNotesByGroup(
        user: AuthenticatedUser.Employee,
        db: Database,
        clock: EvakaClock,
        @PathVariable groupId: GroupId,
    ): NotesByGroupResponse {
        return db.connect { dbc ->
                dbc.read {
                    ac.requirePermissionFor(it, user, clock, Action.Group.READ_NOTES, groupId)
                    NotesByGroupResponse(
                        childDailyNotes = it.getChildDailyNotesForGroup(groupId, clock.today()),
                        childStickyNotes = it.getChildStickyNotesForGroup(groupId, clock.today()),
                        groupNotes = it.getGroupNotesForGroup(groupId),
                    )
                }
            }
            .also {
                Audit.NotesByGroupRead.log(
                    targetId = AuditId(groupId),
                    meta =
                        mapOf(
                            "childDailyNoteCount" to it.childDailyNotes.size,
                            "childStickyNoteCount" to it.childStickyNotes.size,
                            "groupNoteCount" to it.groupNotes.size,
                        ),
                )
            }
    }
}
