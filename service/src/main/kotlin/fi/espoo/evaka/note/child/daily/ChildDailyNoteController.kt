// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.daily

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.EvakaClock
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
            if (error is Conflict) logger.warn("User tried to create a duplicate note for child")
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
