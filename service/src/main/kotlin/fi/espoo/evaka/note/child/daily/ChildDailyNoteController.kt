// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.daily

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import mu.KotlinLogging
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class ChildDailyNoteController(
    private val ac: AccessControl
) {
    @PostMapping("/children/{childId}/child-daily-notes")
    fun createChildDailyNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @RequestBody body: ChildDailyNoteBody
    ): ChildDailyNoteId {
        Audit.ChildDailyNoteCreate.log(childId)
        ac.requirePermissionFor(user, Action.Child.CREATE_DAILY_NOTE, childId)

        try {
            return db.connect { dbc -> dbc.transaction { it.createChildDailyNote(childId, body) } }
        } catch (e: Exception) {
            val error = mapPSQLException(e)
            // monitor if there is issues with stale data or need for upsert
            if (error is Conflict) logger.warn("User tried to create a duplicate note for child")
            throw error
        }
    }

    @PutMapping("/child-daily-notes/{noteId}")
    fun updateChildDailyNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable noteId: ChildDailyNoteId,
        @RequestBody body: ChildDailyNoteBody
    ): ChildDailyNote {
        Audit.ChildDailyNoteUpdate.log(noteId, noteId)
        ac.requirePermissionFor(user, Action.ChildDailyNote.UPDATE, noteId)

        return db.connect { dbc -> dbc.transaction { it.updateChildDailyNote(noteId, body) } }
    }

    @DeleteMapping("/child-daily-notes/{noteId}")
    fun deleteChildDailyNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable noteId: ChildDailyNoteId
    ) {
        Audit.ChildDailyNoteDelete.log(noteId)
        ac.requirePermissionFor(user, Action.ChildDailyNote.DELETE, noteId)

        return db.connect { dbc -> dbc.transaction { it.deleteChildDailyNote(noteId) } }
    }
}
