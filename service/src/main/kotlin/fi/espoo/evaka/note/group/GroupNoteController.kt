// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.group

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
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
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupNoteController(
    private val ac: AccessControl
) {
    @GetMapping("/daycare-groups/{groupId}/group-notes")
    fun getGroupNotes(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable groupId: GroupId
    ): List<GroupNote> =
        db
            .connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(it, user, clock, Action.Group.READ_NOTES, groupId)
                    it.getGroupNotesForGroup(groupId)
                }
            }.also { notes ->
                Audit.GroupNoteRead.log(
                    targetId = AuditId(groupId),
                    objectId = AuditId(notes.map { it.id })
                )
            }

    @PostMapping("/daycare-groups/{groupId}/group-notes")
    fun createGroupNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupNoteBody
    ): GroupNoteId =
        db
            .connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(it, user, clock, Action.Group.CREATE_NOTE, groupId)

                    it.createGroupNote(groupId, body)
                }
            }.also { noteId ->
                Audit.GroupNoteCreate.log(targetId = AuditId(groupId), objectId = AuditId(noteId))
            }

    @PutMapping("/group-notes/{noteId}")
    fun updateGroupNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: GroupNoteId,
        @RequestBody body: GroupNoteBody
    ): GroupNote =
        db
            .connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(it, user, clock, Action.GroupNote.UPDATE, noteId)
                    it.updateGroupNote(clock, noteId, body)
                }
            }.also { Audit.GroupNoteUpdate.log(targetId = AuditId(noteId)) }

    @DeleteMapping("/group-notes/{noteId}")
    fun deleteGroupNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: GroupNoteId
    ) = db
        .connect { dbc ->
            dbc.transaction {
                ac.requirePermissionFor(it, user, clock, Action.GroupNote.DELETE, noteId)
                it.deleteGroupNote(noteId)
            }
        }.also { Audit.GroupNoteDelete.log(targetId = AuditId(noteId)) }
}
