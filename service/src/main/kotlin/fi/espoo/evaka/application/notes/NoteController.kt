// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.notes

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.ApplicationNote
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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
        @PathVariable applicationId: ApplicationId
    ): List<ApplicationNoteResponse> {
        Audit.NoteRead.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.READ_NOTES, applicationId)

        val notes = db.connect { dbc ->
            dbc.read {
                it.getApplicationNotes(applicationId)
            }
        }

        val permittedActions = accessControl.getPermittedApplicationNoteActions(user, notes.map { it.id })

        return notes.map {
            ApplicationNoteResponse(
                note = it,
                permittedActions = permittedActions[it.id] ?: setOf()
            )
        }
    }

    @PostMapping("/application/{id}")
    fun createNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("id") applicationId: ApplicationId,
        @RequestBody note: NoteRequest
    ): ApplicationNote {
        Audit.NoteCreate.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.CREATE_NOTE, applicationId)

        return db.connect { dbc ->
            dbc.transaction {
                it.createApplicationNote(applicationId, note.text, user.evakaUserId)
            }
        }
    }

    @PutMapping("/{noteId}")
    fun updateNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("noteId") noteId: ApplicationNoteId,
        @RequestBody note: NoteRequest
    ) {
        Audit.NoteUpdate.log(targetId = noteId)
        accessControl.requirePermissionFor(user, Action.ApplicationNote.UPDATE, noteId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.updateApplicationNote(noteId, note.text, user.evakaUserId)
            }
        }
    }

    @DeleteMapping("/{noteId}")
    fun deleteNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("noteId") noteId: ApplicationNoteId
    ) {
        Audit.NoteDelete.log(targetId = noteId)
        accessControl.requirePermissionFor(user, Action.ApplicationNote.DELETE, noteId)
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.deleteApplicationNote(noteId)
            }
        }
    }

    @PutMapping("/service-worker/application/{applicationId}")
    fun updateServiceWorkerNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @RequestBody note: NoteRequest
    ) {
        Audit.ServiceWorkerNoteUpdate.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Global.WRITE_SERVICE_WORKER_APPLICATION_NOTES)

        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.updateServiceWorkerApplicationNote(applicationId, note.text)
            }
        }
    }
}

data class NoteRequest(
    val text: String
)

data class ApplicationNoteResponse(
    val note: ApplicationNote,
    val permittedActions: Set<Action.ApplicationNote>
)

data class ApplicationNote(
    val applicationId: ApplicationId,
    val id: ApplicationNoteId,
    val text: String,
    val created: HelsinkiDateTime,
    val createdBy: EvakaUserId,
    val createdByName: String,
    val updated: HelsinkiDateTime,
    val updatedBy: EvakaUserId?,
    val updatedByName: String?
)
