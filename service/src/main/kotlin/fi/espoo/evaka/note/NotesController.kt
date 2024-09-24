// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.note.child.daily.ChildDailyNote
import fi.espoo.evaka.note.child.daily.getChildDailyNotesForGroup
import fi.espoo.evaka.note.child.sticky.ChildStickyNote
import fi.espoo.evaka.note.child.sticky.getChildStickyNotesForGroup
import fi.espoo.evaka.note.group.GroupNote
import fi.espoo.evaka.note.group.getGroupNotesForGroup
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
