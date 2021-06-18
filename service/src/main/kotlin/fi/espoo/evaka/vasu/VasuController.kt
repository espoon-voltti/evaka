package fi.espoo.evaka.vasu

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
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
class VasuController(
    private val acl: AccessControlList
) {

    data class CreateDocumentRequest(
        val childId: UUID,
        val templateId: UUID
    )
    @PostMapping("/vasu")
    fun createDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: CreateDocumentRequest
    ): UUID {
        Audit.VasuDocumentCreate.log(body.childId)
        acl.getRolesForChild(user, body.childId).requireOneOfRoles(
            UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.GROUP_STAFF
        )

        return db.transaction { tx ->
            tx.getVasuTemplate(body.templateId)?.let { template ->
                if (!template.valid.includes(HelsinkiDateTime.now().toLocalDate())) {
                    throw BadRequest("Template is not currently valid")
                }
            } ?: throw NotFound("template ${body.templateId} not found")

            tx.insertVasuDocument(body.childId, body.templateId)
        }
    }

    @GetMapping("/vasu/{id}")
    fun getDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): VasuDocumentResponse {
        Audit.VasuDocumentRead.log(id)
        acl.getRolesForVasuDocument(user, id).requireOneOfRoles(
            UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.GROUP_STAFF
        )

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
        Audit.VasuDocumentUpdate.log(id)
        acl.getRolesForVasuDocument(user, id).requireOneOfRoles(
            UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.GROUP_STAFF
        )

        db.transaction { tx -> tx.updateVasuDocument(id, body.content) }
    }

    @GetMapping("/children/{childId}/vasu-summaries")
    fun getVasuSummariesByChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): List<VasuDocumentSummary> {
        Audit.ChildVasuDocumentsRead.log(childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(
            UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.GROUP_STAFF
        )

        return db.read { tx -> tx.getVasuDocumentSummaries(childId) }
    }
}
