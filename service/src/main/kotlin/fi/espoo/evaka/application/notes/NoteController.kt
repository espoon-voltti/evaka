// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.notes

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.ApplicationNote
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ApplicationNoteId
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

@RestController
@RequestMapping("/note")
class NoteController(private val accessControl: AccessControl) {
    @GetMapping("/application/{applicationId}")
    fun getNotes(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId
    ): List<ApplicationNoteResponse> {
        val notesQuery: (Database.Read) -> List<ApplicationNote> = { tx ->
            if (
                accessControl.hasPermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Application.READ_NOTES,
                    applicationId
                )
            ) {
                tx.getApplicationNotes(applicationId)
            } else {
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Application.READ_SPECIAL_EDUCATION_TEACHER_NOTES,
                    applicationId
                )
                tx.getApplicationSpecialEducationTeacherNotes(applicationId)
            }
        }

        return db.connect { dbc ->
                dbc.read { tx ->
                    val notes = notesQuery(tx)
                    val permittedActions =
                        accessControl.getPermittedActions<
                            ApplicationNoteId,
                            Action.ApplicationNote
                        >(
                            tx,
                            user,
                            clock,
                            notes.map { it.id }
                        )

                    notes.map {
                        ApplicationNoteResponse(
                            note = it,
                            permittedActions = permittedActions[it.id] ?: setOf()
                        )
                    }
                }
            }
            .also { Audit.NoteRead.log(targetId = applicationId, meta = mapOf("count" to it.size)) }
    }

    @PostMapping("/application/{applicationId}")
    fun createNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody note: NoteRequest
    ): ApplicationNote {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Application.CREATE_NOTE,
                        applicationId
                    )
                    it.createApplicationNote(applicationId, note.text, user.evakaUserId)
                }
            }
            .also { Audit.NoteCreate.log(targetId = applicationId, objectId = it.id) }
    }

    @PutMapping("/{noteId}")
    fun updateNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: ApplicationNoteId,
        @RequestBody note: NoteRequest
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.ApplicationNote.UPDATE,
                    noteId
                )
                tx.updateApplicationNote(noteId, note.text, user.evakaUserId)
            }
        }
        Audit.NoteUpdate.log(targetId = noteId)
    }

    @DeleteMapping("/{noteId}")
    fun deleteNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable noteId: ApplicationNoteId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.ApplicationNote.DELETE,
                    noteId
                )
                tx.deleteApplicationNote(noteId)
            }
        }
        Audit.NoteDelete.log(targetId = noteId)
    }

    @PutMapping("/service-worker/application/{applicationId}")
    fun updateServiceWorkerNote(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody note: NoteRequest
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.WRITE_SERVICE_WORKER_APPLICATION_NOTES
                )
                tx.updateServiceWorkerApplicationNote(applicationId, note.text)
            }
        }
        Audit.ServiceWorkerNoteUpdate.log(targetId = applicationId)
    }
}

data class NoteRequest(val text: String)

data class ApplicationNoteResponse(
    val note: ApplicationNote,
    val permittedActions: Set<Action.ApplicationNote>
)
