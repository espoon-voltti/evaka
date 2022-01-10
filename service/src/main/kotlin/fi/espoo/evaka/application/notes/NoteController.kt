// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.notes

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.ApplicationNote
import fi.espoo.evaka.application.utils.toHelsinkiLocalDateTime
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

@RestController
@RequestMapping("/note")
class NoteController(private val accessControl: AccessControl) {
    @GetMapping("/application/{applicationId}")
    fun getNotes(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId
    ): ResponseEntity<List<NoteJSON>> {
        Audit.NoteRead.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.READ_NOTES, applicationId)

        val notes = db.connect { dbc ->
            dbc.read {
                it.getApplicationNotes(applicationId)
            }
        }
        return ResponseEntity.ok(notes.map(NoteJSON.DomainMapping::toJSON))
    }

    @PostMapping("/application/{id}")
    fun createNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("id") applicationId: ApplicationId,
        @RequestBody note: NoteRequest
    ): ResponseEntity<NoteJSON> {
        Audit.NoteCreate.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.CREATE_NOTE, applicationId)

        val newNote = db.connect { dbc ->
            dbc.transaction {
                it.createApplicationNote(applicationId, note.text, user.evakaUserId)
            }
        }
        return ResponseEntity.ok(NoteJSON.toJSON(newNote))
    }

    @PutMapping("/{noteId}")
    fun updateNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("noteId") noteId: ApplicationNoteId,
        @RequestBody note: NoteRequest
    ): ResponseEntity<Unit> {
        Audit.NoteUpdate.log(targetId = noteId)
        accessControl.requirePermissionFor(user, Action.ApplicationNote.UPDATE, noteId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.updateApplicationNote(noteId, note.text, user.evakaUserId)
            }
        }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{noteId}")
    fun deleteNote(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("noteId") noteId: ApplicationNoteId
    ): ResponseEntity<Unit> {
        Audit.NoteDelete.log(targetId = noteId)
        accessControl.requirePermissionFor(user, Action.ApplicationNote.DELETE, noteId)
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.deleteApplicationNote(noteId)
            }
        }
        return ResponseEntity.noContent().build()
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

data class NoteJSON(
    val applicationId: ApplicationId,
    val id: ApplicationNoteId,
    val text: String,
    val created: LocalDateTime,
    val createdBy: EvakaUserId,
    val createdByName: String,
    val updated: LocalDateTime,
    val updatedBy: EvakaUserId?,
    val updatedByName: String?
) {
    companion object DomainMapping {
        fun toJSON(note: ApplicationNote) = NoteJSON(
            id = note.id,
            applicationId = note.applicationId,
            created = OffsetDateTime.ofInstant(note.created, ZoneId.systemDefault()).toHelsinkiLocalDateTime(),
            createdBy = note.createdBy,
            createdByName = note.createdByName,
            updated = OffsetDateTime.ofInstant(note.updated, ZoneId.systemDefault()).toHelsinkiLocalDateTime(),
            updatedBy = note.updatedBy,
            updatedByName = note.updatedByName,
            text = note.content
        )
    }
}
