// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.group

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupNoteController(
    private val ac: AccessControl
) {
    @PostMapping("/daycare-groups/{groupId}/group-notes")
    fun createGroupNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupNoteBody
    ): GroupNoteId {
        Audit.GroupNoteCreate.log(groupId)
        ac.requirePermissionFor(user, Action.Group.CREATE_NOTE, groupId)

        return db.connect { dbc -> dbc.transaction { it.createGroupNote(groupId, body) } }
    }

    @PutMapping("/group-notes/{noteId}")
    fun updateGroupNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable noteId: GroupNoteId,
        @RequestBody body: GroupNoteBody
    ): GroupNote {
        Audit.GroupNoteUpdate.log(noteId, noteId)
        ac.requirePermissionFor(user, Action.GroupNote.UPDATE, noteId)

        return db.connect { dbc -> dbc.transaction { it.updateGroupNote(noteId, body) } }
    }

    @DeleteMapping("/group-notes/{noteId}")
    fun deleteGroupNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable noteId: GroupNoteId
    ) {
        Audit.GroupNoteDelete.log(noteId)
        ac.requirePermissionFor(user, Action.GroupNote.DELETE, noteId)

        return db.connect { dbc -> dbc.transaction { it.deleteGroupNote(noteId) } }
    }
}
