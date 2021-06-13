package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@RestController
class VasuController {

    data class CreateDocumentRequest(
        val childId: UUID,
        val templateId: UUID
    )
    @PostMapping("/vasu")
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

    data class CreateTemplateRequest(
        val name: String,
        val valid: FiniteDateRange
    )
    @PostMapping("/vasu/templates")
    fun postTemplate(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: CreateTemplateRequest
    ): ResponseEntity<UUID> {
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.transaction {
            it.insertVasuTemplate(
                name = body.name,
                valid = body.valid,
                content = defaultTemplateContent
            )
        }.let { ResponseEntity.created(URI.create("/vasu/templates/$it")).body(it) }
    }

    @GetMapping("/vasu/templates")
    fun getTemplates(
        db: Database.Connection,
        user: AuthenticatedUser
    ): List<VasuTemplateSummary> {
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.read { tx -> tx.getVasuTemplates() }
    }

    @GetMapping("/vasu/templates/{id}")
    fun getTemplate(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): VasuTemplate {
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.read { tx -> tx.getVasuTemplate(id) } ?: throw NotFound("template $id not found")
    }

    @PutMapping("/vasu/templates/{id}/content")
    fun putTemplateContent(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody content: VasuContent
    ) {
        user.requireOneOfRoles(UserRole.ADMIN)

        db.transaction { tx -> tx.updateVasuTemplateContent(id, content) }
    }

    @GetMapping("/children/{childId}/vasu-summaries")
    fun getVasuSummariesByChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): List<VasuDocumentSummary> {
        user.requireOneOfRoles(UserRole.ADMIN)

        return listOf(
            VasuDocumentSummary(
                id = UUID.randomUUID(),
                name = "Varhaiskasvatussuunnitelma 2021-2022",
                state = VasuDocumentState.DRAFT,
                modifiedAt = HelsinkiDateTime.now(),
            ),
            VasuDocumentSummary(
                id = UUID.randomUUID(),
                name = "Varhaiskasvatussuunnitelma 2020-2021",
                state = VasuDocumentState.CREATED,
                modifiedAt = HelsinkiDateTime.of(
                    LocalDateTime.of(2020, 10, 5, 7, 13)
                ),
                publishedAt = HelsinkiDateTime.of(
                    LocalDateTime.of(2020, 9, 1, 12, 0)
                ),
            ),
            VasuDocumentSummary(
                id = UUID.randomUUID(),
                name = "Varhaiskasvatussuunnitelma 2019-2020",
                state = VasuDocumentState.CLOSED,
                modifiedAt = HelsinkiDateTime.of(
                    LocalDateTime.of(2019, 9, 24, 12, 45)
                ),
                publishedAt = HelsinkiDateTime.of(
                    LocalDateTime.of(2020, 8, 7, 15, 0)
                ),
            )
        )
    }
}
