// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.application.notes

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.application.ApplicationNote
import evaka.core.application.fetchApplicationDetails
import evaka.core.application.getApplicationOtherGuardians
import evaka.core.shared.ApplicationId
import evaka.core.shared.ApplicationNoteId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/note")
class NoteController(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig,
) {
    @GetMapping("/application/{applicationId}")
    fun getNotes(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ): List<ApplicationNoteResponse> {
        val audit = AuditContext().add(applicationId)
        val notesQuery: (Database.Read) -> List<ApplicationNote> = { tx ->
            if (
                accessControl.hasPermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Application.READ_NOTES,
                    applicationId,
                )
            ) {
                tx.getApplicationNotes(
                    applicationId,
                    deletedMessageBody = featureConfig.deletedMessagePlaceholderBody,
                )
            } else {
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Application.READ_SPECIAL_EDUCATION_TEACHER_NOTES,
                    applicationId,
                )
                tx.getApplicationSpecialEducationTeacherNotes(
                    applicationId,
                    deletedMessageBody = featureConfig.deletedMessagePlaceholderBody,
                )
            }
        }

        return db.connect { dbc ->
                dbc.read { tx ->
                    val notes = notesQuery(tx)
                    val permittedActions =
                        accessControl.getPermittedActions<
                            ApplicationNoteId,
                            Action.ApplicationNote,
                        >(
                            tx,
                            user,
                            clock,
                            notes.map { it.id },
                        )

                    notes.map {
                        ApplicationNoteResponse(
                            note = it,
                            permittedActions = permittedActions[it.id] ?: setOf(),
                        )
                    }
                }
            }
            .also { notes ->
                audit
                    .add(notes.map { it.note.id })
                    .observeDate(notes.minOfOrNull { it.note.createdAt }?.toLocalDate())
                audit.log(Audit.NoteRead, clock)
            }
    }

    @PostMapping("/application/{applicationId}")
    fun createNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody note: NoteRequest,
    ): ApplicationNote {
        val audit = AuditContext().add(applicationId)
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Application.CREATE_NOTE,
                        applicationId,
                    )
                    tx.fetchApplicationDetails(applicationId)?.also { application ->
                        audit.add(application.childId).add(application.guardianId)
                    }
                    tx.getApplicationOtherGuardians(applicationId).also { audit.add(it) }
                    tx.createApplicationNote(
                        clock.now(),
                        applicationId,
                        note.text,
                        user.evakaUserId,
                    )
                }
            }
            .also { createdNote ->
                audit.add(createdNote.id)
                audit.log(Audit.NoteCreate, clock)
            }
    }

    @PutMapping("/{noteId}")
    fun updateNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable noteId: ApplicationNoteId,
        @RequestBody note: NoteRequest,
    ) {
        val audit = AuditContext().add(noteId)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ApplicationNote.UPDATE,
                        noteId,
                    )
                    val updatedNote =
                        tx.updateApplicationNote(noteId, note.text, user.evakaUserId, clock.now())
                    audit
                        .add(updatedNote.applicationId)
                        .observeDate(updatedNote.createdAt.toLocalDate())
                    tx.fetchApplicationDetails(updatedNote.applicationId)?.also { application ->
                        audit.add(application.childId).add(application.guardianId)
                    }
                    tx.getApplicationOtherGuardians(updatedNote.applicationId).also {
                        audit.add(it)
                    }
                }
            }
            .also { audit.log(Audit.NoteUpdate, clock) }
    }

    @DeleteMapping("/{noteId}")
    fun deleteNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable noteId: ApplicationNoteId,
    ) {
        val audit = AuditContext().add(noteId)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ApplicationNote.DELETE,
                        noteId,
                    )
                    tx.deleteApplicationNote(noteId)?.also { deletedNote ->
                        audit
                            .add(deletedNote.applicationId)
                            .add(deletedNote.createdBy)
                            .add(listOfNotNull(deletedNote.messageContentId))
                            .observeDate(deletedNote.createdAt.toLocalDate())
                        tx.fetchApplicationDetails(deletedNote.applicationId)?.also { application ->
                            audit.add(application.childId).add(application.guardianId)
                        }
                        tx.getApplicationOtherGuardians(deletedNote.applicationId).also {
                            audit.add(it)
                        }
                    }
                }
            }
            .also { audit.log(Audit.NoteDelete, clock) }
    }

    @PutMapping("/service-worker/application/{applicationId}")
    fun updateServiceWorkerNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody note: NoteRequest,
    ) {
        val audit = AuditContext().add(applicationId)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.WRITE_SERVICE_WORKER_APPLICATION_NOTES,
                    )
                    tx.fetchApplicationDetails(applicationId)?.also { application ->
                        audit.add(application.childId).add(application.guardianId)
                    }
                    tx.getApplicationOtherGuardians(applicationId).also { audit.add(it) }
                    tx.updateServiceWorkerApplicationNote(applicationId, note.text)
                }
            }
            .also { audit.log(Audit.ServiceWorkerNoteUpdate, clock) }
    }
}

data class NoteRequest(val text: String)

data class ApplicationNoteResponse(
    val note: ApplicationNote,
    val permittedActions: Set<Action.ApplicationNote>,
)
