// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.finance.notes

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.FinanceNoteId
import fi.espoo.evaka.shared.PersonId
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class FinanceNoteRequest(val personId: PersonId, val content: String)

data class FinanceNoteResponse(
    val note: FinanceNote,
    val permittedActions: Set<Action.FinanceNote>,
)

@RestController
@RequestMapping("/employee/finance-notes")
class FinanceNoteController(private val accessControl: AccessControl) {
    @GetMapping("/{personId}")
    fun getFinanceNotes(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable personId: PersonId,
    ): List<FinanceNoteResponse> {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_FINANCE_NOTES,
                        personId,
                    )
                    val notes = tx.getFinanceNotes(personId)
                    val permittedActions =
                        accessControl.getPermittedActions<FinanceNoteId, Action.FinanceNote>(
                            tx,
                            user,
                            clock,
                            notes.map { it.id },
                        )
                    notes.map {
                        FinanceNoteResponse(
                            note = it,
                            permittedActions = permittedActions[it.id] ?: setOf(),
                        )
                    }
                }
            }
            .also {
                Audit.FinanceNoteRead.log(
                    targetId = AuditId(personId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @PostMapping("/")
    fun createFinanceNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody note: FinanceNoteRequest,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.CREATE_FINANCE_NOTE,
                        note.personId,
                    )
                    tx.createFinanceNote(note.personId, note.content, user, clock.now())
                }
            }
            .also { financeNoteId ->
                Audit.FinanceNoteCreate.log(
                    targetId = AuditId(note.personId),
                    objectId = AuditId(financeNoteId),
                )
            }
    }

    @PutMapping("/{noteId}")
    fun updateFinanceNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable noteId: FinanceNoteId,
        @RequestBody note: FinanceNoteRequest,
    ) {
        db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.FinanceNote.UPDATE,
                        noteId,
                    )
                    it.updateFinanceNote(noteId, note.content, user, clock.now())
                }
            }
            .also { Audit.FinanceNoteUpdate.log(targetId = AuditId(noteId)) }
    }

    @DeleteMapping("/{noteId}")
    fun deleteFinanceNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable noteId: FinanceNoteId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.FinanceNote.DELETE,
                        noteId,
                    )
                    tx.deleteFinanceNote(noteId)
                }
            }
            .also { Audit.FinanceNoteDelete.log(targetId = AuditId(noteId)) }
    }
}
