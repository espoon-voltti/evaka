// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.sticky

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

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
        validateExpiration(clock, body.expires)

        return db
            .connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.CREATE_STICKY_NOTE,
                        childId
                    )
                    it.createChildStickyNote(childId, body)
                }
            }.also { noteId ->
                Audit.ChildStickyNoteCreate.log(
                    targetId = AuditId(childId),
                    objectId = AuditId(noteId)
                )
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
        validateExpiration(clock, body.expires)

        return db
            .connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(it, user, clock, Action.ChildStickyNote.UPDATE, noteId)
                    it.updateChildStickyNote(clock, noteId, body)
                }
            }.also { Audit.ChildStickyNoteUpdate.log(targetId = AuditId(noteId)) }
    }

    @DeleteMapping("/child-sticky-notes/{noteId}")
    fun deleteChildStickyNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: ChildStickyNoteId
    ) = db
        .connect { dbc ->
            dbc.transaction {
                ac.requirePermissionFor(it, user, clock, Action.ChildStickyNote.DELETE, noteId)
                it.deleteChildStickyNote(noteId)
            }
        }.also { Audit.ChildStickyNoteDelete.log(targetId = AuditId(noteId)) }

    private fun validateExpiration(
        evakaClock: EvakaClock,
        expires: LocalDate
    ) {
        val validRange = FiniteDateRange(evakaClock.today(), evakaClock.today().plusDays(7))
        if (!validRange.includes(expires)) {
            throw BadRequest("Expiration date was invalid")
        }
    }
}
