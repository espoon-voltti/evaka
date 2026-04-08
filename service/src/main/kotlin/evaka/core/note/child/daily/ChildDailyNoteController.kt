// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.note.child.daily

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.shared.ChildDailyNoteId
import evaka.core.shared.ChildId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.db.mapPSQLException
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class ChildDailyNoteController(private val ac: AccessControl) {
    @PostMapping("/employee/children/{childId}/child-daily-notes")
    fun createChildDailyNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: ChildDailyNoteBody,
    ): ChildDailyNoteId = createChildDailyNote(db, user as AuthenticatedUser, clock, childId, body)

    @PostMapping("/employee-mobile/children/{childId}/child-daily-notes")
    fun createChildDailyNote(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: ChildDailyNoteBody,
    ): ChildDailyNoteId = createChildDailyNote(db, user as AuthenticatedUser, clock, childId, body)

    private fun createChildDailyNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: ChildDailyNoteBody,
    ): ChildDailyNoteId {
        try {
            return db.connect { dbc ->
                    dbc.transaction {
                        ac.requirePermissionFor(
                            it,
                            user,
                            clock,
                            Action.Child.CREATE_DAILY_NOTE,
                            childId,
                        )
                        it.createChildDailyNote(childId, body)
                    }
                }
                .also { noteId ->
                    Audit.ChildDailyNoteCreate.log(
                        targetId = AuditId(childId),
                        objectId = AuditId(noteId),
                    )
                }
        } catch (e: Exception) {
            val error = mapPSQLException(e)
            // monitor if there is issues with stale data or need for upsert
            if (error is Conflict) logger.warn { "User tried to create a duplicate note for child" }
            throw error
        }
    }

    @PutMapping("/employee/child-daily-notes/{noteId}")
    fun updateChildDailyNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable noteId: ChildDailyNoteId,
        @RequestBody body: ChildDailyNoteBody,
    ): ChildDailyNote = updateChildDailyNote(db, user as AuthenticatedUser, clock, noteId, body)

    @PutMapping("/employee-mobile/child-daily-notes/{noteId}")
    fun updateChildDailyNote(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable noteId: ChildDailyNoteId,
        @RequestBody body: ChildDailyNoteBody,
    ): ChildDailyNote = updateChildDailyNote(db, user as AuthenticatedUser, clock, noteId, body)

    private fun updateChildDailyNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: ChildDailyNoteId,
        @RequestBody body: ChildDailyNoteBody,
    ): ChildDailyNote {
        return db.connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(it, user, clock, Action.ChildDailyNote.UPDATE, noteId)
                    it.updateChildDailyNote(clock, noteId, body)
                }
            }
            .also { Audit.ChildDailyNoteUpdate.log(targetId = AuditId(noteId)) }
    }

    @DeleteMapping("/employee/child-daily-notes/{noteId}")
    fun deleteChildDailyNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable noteId: ChildDailyNoteId,
    ) = deleteChildDailyNote(db, user as AuthenticatedUser, clock, noteId)

    @DeleteMapping("/employee-mobile/child-daily-notes/{noteId}")
    fun deleteChildDailyNote(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable noteId: ChildDailyNoteId,
    ) = deleteChildDailyNote(db, user as AuthenticatedUser, clock, noteId)

    private fun deleteChildDailyNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: ChildDailyNoteId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                ac.requirePermissionFor(it, user, clock, Action.ChildDailyNote.DELETE, noteId)
                it.deleteChildDailyNote(noteId)
            }
        }
        Audit.ChildDailyNoteDelete.log(targetId = AuditId(noteId))
    }
}
