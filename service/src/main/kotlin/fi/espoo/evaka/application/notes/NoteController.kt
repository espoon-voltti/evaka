// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.notes

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.ApplicationNote
import fi.espoo.evaka.application.utils.toHelsinkiLocalDateTime
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ApplicationNoteId
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
import java.util.UUID

@RestController
@RequestMapping("/note")
class NoteController(private val accessControl: AccessControl) {
    @GetMapping("/application/{applicationId}")
    fun getNotes(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId
    ): ResponseEntity<List<NoteJSON>> {
        Audit.NoteRead.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.READ_NOTES, applicationId)

        val notes = db.read {
            it.getApplicationNotes(applicationId)
        }
        return ResponseEntity.ok(notes.map(NoteJSON.DomainMapping::toJSON))
    }

    @PostMapping("/application/{id}")
    fun createNote(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("id") applicationId: ApplicationId,
        @RequestBody note: NoteRequest
    ): ResponseEntity<NoteJSON> {
        Audit.NoteCreate.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.CREATE_NOTE, applicationId)

        val newNote = db.transaction {
            it.createApplicationNote(applicationId, note.text, user.id)
        }
        return ResponseEntity.ok(NoteJSON.toJSON(newNote))
    }

    @PutMapping("/{noteId}")
    fun updateNote(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("noteId") noteId: ApplicationNoteId,
        @RequestBody note: NoteRequest
    ): ResponseEntity<Unit> {
        Audit.NoteUpdate.log(targetId = noteId)
        accessControl.requirePermissionFor(user, Action.ApplicationNote.UPDATE, noteId)

        db.transaction { tx ->
            tx.updateApplicationNote(noteId, note.text, user.id)
        }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{noteId}")
    fun deleteNote(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("noteId") noteId: ApplicationNoteId
    ): ResponseEntity<Unit> {
        Audit.NoteDelete.log(targetId = noteId)
        accessControl.requirePermissionFor(user, Action.ApplicationNote.DELETE, noteId)
        db.transaction { tx ->
            tx.deleteApplicationNote(noteId)
        }
        return ResponseEntity.noContent().build()
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
    val createdBy: UUID,
    val createdByName: String,
    val updated: LocalDateTime,
    val updatedBy: UUID?,
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
