package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/vasu")
class VasuController {

    data class CreateDocumentRequest(
        val childId: UUID,
        val templateId: UUID
    )
    @PostMapping
    fun createDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: CreateDocumentRequest
    ): ResponseEntity<VasuDocumentResponse> {
        user.requireOneOfRoles(UserRole.ADMIN)

        val document = db.transaction { tx ->
            tx.getVasuTemplate(body.templateId)?.let { template ->
                if (!template.valid.includes(LocalDate.now())) {
                    throw BadRequest("Template is not currently valid")
                }
            } ?: throw NotFound("template ${body.templateId} not found")

            val id = tx.insertVasuDocument(body.childId, body.templateId)
            tx.getVasuDocumentResponse(id)!!
        }

        return ResponseEntity.created(URI.create("/vasu/${document.id}")).body(document)
    }

    @GetMapping("/{id}")
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
    @PutMapping("/{id}")
    fun putDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: UpdateDocumentRequest
    ) {
        user.requireOneOfRoles(UserRole.ADMIN)

        db.transaction { tx -> tx.updateVasuDocument(id, body.content) }
    }

    @GetMapping("/templates")
    fun getTemplates(
        db: Database.Connection,
        user: AuthenticatedUser
    ): List<VasuTemplateSummary> {
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.read { tx -> tx.getVasuTemplates() }
    }
}
