package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class VasuController {

    data class CreateDocumentRequest(
        val childId: UUID
    )
    @PostMapping("/vasu")
    fun createDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: CreateDocumentRequest
    ): VasuDocumentResponse {
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.transaction { tx ->
            val template = tx.getVasuTemplateForDate(HelsinkiDateTime.now().toLocalDate())
                ?: throw NotFound("template not found")

            val id = tx.insertVasuDocument(body.childId, template.id)
            tx.getVasuDocumentResponse(id)!!
        }
    }

    @GetMapping("/vasu/{id}")
    fun getDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): VasuDocumentResponse {
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.read { tx ->
            tx.getVasuDocumentResponse(id) ?: throw NotFound("template $id not found")
        }
    }

    data class UpdateDocumentRequest(
        val content: VasuContent
    )
    @PutMapping("/vasu/{id}")
    fun putDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: UpdateDocumentRequest
    ) {
        user.requireOneOfRoles(UserRole.ADMIN)

        db.transaction { tx -> tx.updateVasuDocument(id, body.content) }
    }

    @GetMapping("/children/{childId}/vasu-summaries")
    fun getVasuSummariesByChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): List<VasuDocumentSummary> {
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.read { tx -> tx.getVasuDocumentSummaries(childId) }
    }
}
