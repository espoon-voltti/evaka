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
class GroupNoteController(private val ac: AccessControl) {
    @GetMapping(
        "/daycare-groups/{groupId}/group-notes", // deprecated
        "/employee-mobile/daycare-groups/{groupId}/group-notes",
    )
    fun getGroupNotes(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable groupId: GroupId,
    ): List<GroupNote> {
        return db.connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(it, user, clock, Action.Group.READ_NOTES, groupId)
                    it.getGroupNotesForGroup(groupId)
                }
            }
            .also { notes ->
                Audit.GroupNoteRead.log(
                    targetId = AuditId(groupId),
                    objectId = AuditId(notes.map { it.id }),
                )
            }
    }

    @PostMapping("/employee/daycare-groups/{groupId}/group-notes")
    fun createGroupNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupNoteBody,
    ): GroupNoteId = createGroupNote(db, user as AuthenticatedUser, clock, groupId, body)

    @PostMapping("/employee-mobile/daycare-groups/{groupId}/group-notes")
    fun createGroupNote(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupNoteBody,
    ): GroupNoteId = createGroupNote(db, user as AuthenticatedUser, clock, groupId, body)

    @PostMapping("/daycare-groups/{groupId}/group-notes") // deprecated
    fun createGroupNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupNoteBody,
    ): GroupNoteId {
        return db.connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(it, user, clock, Action.Group.CREATE_NOTE, groupId)

                    it.createGroupNote(groupId, body)
                }
            }
            .also { noteId ->
                Audit.GroupNoteCreate.log(targetId = AuditId(groupId), objectId = AuditId(noteId))
            }
    }

    @PutMapping("/employee/group-notes/{noteId}")
    fun updateGroupNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable noteId: GroupNoteId,
        @RequestBody body: GroupNoteBody,
    ): GroupNote = updateGroupNote(db, user as AuthenticatedUser, clock, noteId, body)

    @PutMapping("/employee-mobile/group-notes/{noteId}")
    fun updateGroupNote(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable noteId: GroupNoteId,
        @RequestBody body: GroupNoteBody,
    ): GroupNote = updateGroupNote(db, user as AuthenticatedUser, clock, noteId, body)

    @PutMapping("/group-notes/{noteId}") // deprecated
    fun updateGroupNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: GroupNoteId,
        @RequestBody body: GroupNoteBody,
    ): GroupNote {
        return db.connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(it, user, clock, Action.GroupNote.UPDATE, noteId)
                    it.updateGroupNote(clock, noteId, body)
                }
            }
            .also { Audit.GroupNoteUpdate.log(targetId = AuditId(noteId)) }
    }

    @DeleteMapping("/employee/group-notes/{noteId}")
    fun deleteGroupNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable noteId: GroupNoteId,
    ) = deleteGroupNote(db, user as AuthenticatedUser, clock, noteId)

    @DeleteMapping("/employee-mobile/group-notes/{noteId}")
    fun deleteGroupNote(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable noteId: GroupNoteId,
    ) = deleteGroupNote(db, user as AuthenticatedUser, clock, noteId)

    @DeleteMapping("/group-notes/{noteId}") // deprecated
    fun deleteGroupNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: GroupNoteId,
    ) {
        return db.connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(it, user, clock, Action.GroupNote.DELETE, noteId)
                    it.deleteGroupNote(noteId)
                }
            }
            .also { Audit.GroupNoteDelete.log(targetId = AuditId(noteId)) }
    }
}
