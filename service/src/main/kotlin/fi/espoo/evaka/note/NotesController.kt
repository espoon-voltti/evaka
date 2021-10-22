// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note

import fi.espoo.evaka.Audit
import fi.espoo.evaka.note.child.daily.ChildDailyNote
import fi.espoo.evaka.note.child.daily.getLatestChildDailyNote
import fi.espoo.evaka.note.child.daily.getLatestChildDailyNotesInGroup
import fi.espoo.evaka.note.group.GroupNote
import fi.espoo.evaka.note.group.getGroupNotesForChild
import fi.espoo.evaka.note.group.getGroupNotesForGroup
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class NotesController(
    private val ac: AccessControl
) {
    data class NotesByChildResponse(
        val childDailyNote: ChildDailyNote?,
        val groupNotes: List<GroupNote>
    )
    @GetMapping("/children/{childId}/notes")
    fun getNotesByChild(
        user: AuthenticatedUser,
        db: Database.Connection,
        @PathVariable childId: UUID
    ): NotesByChildResponse {
        Audit.NotesByChildRead.log(childId)
        ac.requirePermissionFor(user, Action.Child.READ_NOTES, childId)

        return db.read {
            NotesByChildResponse(
                childDailyNote = it.getLatestChildDailyNote(childId),
                groupNotes = it.getGroupNotesForChild(childId)
            )
        }
    }

    data class NotesByGroupResponse(
        val childDailyNotes: List<ChildDailyNote>,
        val groupNotes: List<GroupNote>
    )
    @GetMapping("/daycare-groups/{groupId}/notes")
    fun getNotesByGroup(
        user: AuthenticatedUser,
        db: Database.Connection,
        @PathVariable groupId: GroupId
    ): NotesByGroupResponse {
        Audit.NotesByGroupRead.log(groupId)
        ac.requirePermissionFor(user, Action.Group.READ_NOTES, groupId)

        return db.read {
            NotesByGroupResponse(
                childDailyNotes = it.getLatestChildDailyNotesInGroup(groupId),
                groupNotes = it.getGroupNotesForGroup(groupId)
            )
        }
    }
}
