// TODO copyright stuff

package fi.espoo.evaka.finance.notes

import fi.espoo.evaka.shared.FinanceNoteId
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/note/finance")
class FinanceNoteController(private val accessControl: AccessControl) {
    // TODO get mapping
    //
    // TODO POST mapping
    //
    // TODO update? PUT?
    //
    // TODO delete

    @PostMapping("/")
    fun createNote(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody note: FinanceNoteRequest,// TODO create this type
    ): FinanceNote {
        return db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.FinanceNote.CREATE,
                    noteId, // TODO applicationId?
                )
                it.createFinanceNote(note.text, user.evakaUserId, clock.now())
            }
        }
        .also {
            // TODO audit line
        }
    }

    @DeleteMapping("/{noteId}")
    fun deleteNote(
        db: Database,
        user: AuthenticatedUser.Employee,// TODO is this right? Also import.
        clock: EvakaClock,
        @PathVariable noteId: FinanceNoteId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.FinanceNote.DELETE, // TODO import something?
                    noteId,
                )
                tx.deleteFinanceNote(noteId)
            }
        }
        Audit.NoteDelete.log(targetId = AuditId(noteId)) // TODO imports
    }
}
