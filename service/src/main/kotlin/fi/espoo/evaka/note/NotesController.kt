// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note

import fi.espoo.evaka.Audit
import fi.espoo.evaka.note.child.daily.ChildDailyNote
import fi.espoo.evaka.note.child.daily.getChildDailyNote
import fi.espoo.evaka.note.child.daily.getChildDailyNotesInGroup
import fi.espoo.evaka.note.child.sticky.ChildStickyNote
import fi.espoo.evaka.note.child.sticky.getChildStickyNotesForChild
import fi.espoo.evaka.note.child.sticky.getChildStickyNotesForGroup
import fi.espoo.evaka.note.group.GroupNote
import fi.espoo.evaka.note.group.getGroupNotesForChild
import fi.espoo.evaka.note.group.getGroupNotesForGroup
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class NotesController(
    private val ac: AccessControl
) {
    data class NotesByChildResponse(
        val childDailyNote: ChildDailyNote?,
        val childStickyNotes: List<ChildStickyNote>,
        val groupNotes: List<GroupNote>
    )
    @GetMapping("/children/{childId}/notes")
    fun getNotesByChild(
        user: AuthenticatedUser,
        db: Database,
        evakaClock: EvakaClock,
        @PathVariable childId: ChildId
    ): NotesByChildResponse {
        Audit.NotesByChildRead.log(childId)
        ac.requirePermissionFor(user, Action.Child.READ_NOTES, childId)

        return db.connect { dbc ->
            dbc.read {
                NotesByChildResponse(
                    childDailyNote = it.getChildDailyNote(childId),
                    childStickyNotes = it.getChildStickyNotesForChild(childId),
                    groupNotes = it.getGroupNotesForChild(childId, evakaClock.today())
                )
            }
        }
    }

    data class NotesByGroupResponse(
        val childDailyNotes: List<ChildDailyNote>,
        val childStickyNotes: List<ChildStickyNote>,
        val groupNotes: List<GroupNote>
    )
    @GetMapping("/daycare-groups/{groupId}/notes")
    fun getNotesByGroup(
        user: AuthenticatedUser,
        db: Database,
        @PathVariable groupId: GroupId
    ): NotesByGroupResponse {
        Audit.NotesByGroupRead.log(groupId)
        ac.requirePermissionFor(user, Action.Group.READ_NOTES, groupId)

        return db.connect { dbc ->
            dbc.read {
                NotesByGroupResponse(
                    childDailyNotes = it.getChildDailyNotesInGroup(groupId),
                    childStickyNotes = it.getChildStickyNotesForGroup(groupId),
                    groupNotes = it.getGroupNotesForGroup(groupId)
                )
            }
        }
    }
}
