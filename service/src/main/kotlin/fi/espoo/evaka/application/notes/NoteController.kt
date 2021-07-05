// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.notes

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.ApplicationNote
import fi.espoo.evaka.application.utils.toHelsinkiLocalDateTime
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
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
class NoteController(private val acl: AccessControlList) {
    @PostMapping("/search")
    fun getNotes(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody search: NoteSearchDTO
    ): ResponseEntity<List<NoteJSON>> {
        Audit.NoteRead.log(targetId = search.applicationIds)
        search.applicationIds.forEach { applicationId ->
            acl.getRolesForApplication(user, applicationId).requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.SPECIAL_EDUCATION_TEACHER)
        }

        val notes = db.read {
            it.getApplicationNotes(search.applicationIds.first())
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
        acl.getRolesForApplication(user, applicationId).requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)

        val newNote = db.transaction {
            it.createApplicationNote(applicationId, note.text, user.id)
        }
        return ResponseEntity.ok(NoteJSON.toJSON(newNote))
    }

    @PutMapping("/update")
    @Deprecated("use updateNote instead (PUT /note/:id)")
    fun updateNotes(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody notes: List<NoteJSON>
    ): ResponseEntity<NotesWrapperJSON> {
        Audit.NoteUpdate.log(targetId = notes.map { it.id })
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        // This endpoint is never used with multiple notes in reality
        val note = notes.first()
        val updatedNote = db.transaction { tx ->
            if (userIsAllowedToEditNote(tx, user, note.id)) {
                tx.updateApplicationNote(note.id, note.text, user.id)
            } else {
                throw Forbidden("User is not allowed to edit the note")
            }
        }
        return ResponseEntity.ok(NotesWrapperJSON(listOf(NoteJSON.toJSON(updatedNote))))
    }

    @PutMapping("/{noteId}")
    fun updateNote(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("noteId") noteId: ApplicationNoteId,
        @RequestBody note: NoteRequest
    ): ResponseEntity<Unit> {
        Audit.NoteUpdate.log(targetId = noteId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)

        db.transaction { tx ->
            if (userIsAllowedToEditNote(tx, user, noteId)) {
                tx.updateApplicationNote(noteId, note.text, user.id)
            } else {
                throw Forbidden("User is not allowed to edit the note")
            }
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
        db.transaction { tx ->
            if (userIsAllowedToEditNote(tx, user, noteId)) {
                tx.deleteApplicationNote(noteId)
            }
        }
        return ResponseEntity.noContent().build()
    }
}

private fun userIsAllowedToEditNote(tx: Database.Read, user: AuthenticatedUser, noteId: ApplicationNoteId): Boolean {
    return if (user.hasOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)) {
        true
    } else {
        val createdBy = tx.getApplicationNoteCreatedBy(noteId)
        if (user.hasOneOfRoles(UserRole.UNIT_SUPERVISOR)) {
            createdBy == user.id
        } else {
            false
        }
    }
}

data class NoteSearchDTO(
    val applicationIds: Set<ApplicationId> = emptySet()
) {
    companion object {
        val All = NoteSearchDTO()
    }
}

data class NoteRequest(
    val text: String
)

data class NotesWrapperJSON(
    val notes: List<NoteJSON>
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
