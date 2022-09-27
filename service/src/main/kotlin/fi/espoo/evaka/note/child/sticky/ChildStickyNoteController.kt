// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.sticky

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class ChildStickyNoteController(
    private val ac: AccessControl
) {
    @PostMapping("/children/{childId}/child-sticky-notes")
    fun createChildStickyNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: ChildStickyNoteBody
    ): ChildStickyNoteId {
        ac.requirePermissionFor(user, clock, Action.Child.CREATE_STICKY_NOTE, childId)

        validateExpiration(clock, body.expires)

        return db.connect { dbc -> dbc.transaction { it.createChildStickyNote(childId, body) } }.also { noteId ->
            Audit.ChildStickyNoteCreate.log(targetId = childId, objectId = noteId)
        }
    }

    @PutMapping("/child-sticky-notes/{noteId}")
    fun updateChildStickyNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: ChildStickyNoteId,
        @RequestBody body: ChildStickyNoteBody
    ): ChildStickyNote {
        ac.requirePermissionFor(user, clock, Action.ChildStickyNote.UPDATE, noteId)

        validateExpiration(clock, body.expires)

        return db.connect { dbc -> dbc.transaction { it.updateChildStickyNote(clock, noteId, body) } }.also {
            Audit.ChildStickyNoteUpdate.log(targetId = noteId)
        }
    }

    @DeleteMapping("/child-sticky-notes/{noteId}")
    fun deleteChildStickyNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: ChildStickyNoteId
    ) {
        ac.requirePermissionFor(user, clock, Action.ChildStickyNote.DELETE, noteId)

        return db.connect { dbc -> dbc.transaction { it.deleteChildStickyNote(noteId) } }.also {
            Audit.ChildStickyNoteDelete.log(targetId = noteId)
        }
    }

    private fun validateExpiration(evakaClock: EvakaClock, expires: LocalDate) {
        val validRange = FiniteDateRange(
            evakaClock.today(),
            evakaClock.today().plusDays(7)
        )
        if (!validRange.includes(expires)) {
            throw BadRequest("Expiration date was invalid")
        }
    }
}
