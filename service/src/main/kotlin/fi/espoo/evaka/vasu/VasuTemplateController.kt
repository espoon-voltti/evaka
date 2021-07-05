package fi.espoo.evaka.vasu

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/vasu/templates")
class VasuTemplateController {
    data class CreateTemplateRequest(
        val name: String,
        val valid: FiniteDateRange,
        val language: VasuLanguage
    )

    @PostMapping
    fun postTemplate(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: CreateTemplateRequest
    ): UUID {
        Audit.VasuTemplateCreate.log()
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.transaction {
            it.insertVasuTemplate(
                name = body.name,
                valid = body.valid,
                language = body.language,
                content = getDefaultTemplateContent(body.language)
            )
        }
    }

    @PutMapping("/{id}")
    fun editTemplate(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: VasuTemplateUpdate
    ) {
        Audit.VasuTemplateEdit.log()
        user.requireOneOfRoles(UserRole.ADMIN)

        db.transaction { tx ->
            val template = tx.getVasuTemplateForUpdate(id) ?: throw NotFound("Template not found")
            validateTemplateUpdate(template, body)
            tx.updateVasuTemplate(id, body)
        }
    }

    data class CopyTemplateRequest(
        val name: String,
        val valid: FiniteDateRange
    )

    @PostMapping("/{id}/copy")
    fun copyTemplate(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: CopyTemplateRequest
    ): UUID {
        Audit.VasuTemplateCopy.log(id)
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.transaction {
            val template = it.getVasuTemplate(id) ?: throw NotFound("template not found")
            it.insertVasuTemplate(
                name = body.name,
                valid = body.valid,
                language = template.language,
                content = copyTemplateContentWithCurrentlyValidOphSections(template)
            )
        }
    }

    @GetMapping
    fun getTemplates(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam(required = false) validOnly: Boolean = false
    ): List<VasuTemplateSummary> {
        Audit.VasuTemplateRead.log()
        user.requireOneOfRoles(
            UserRole.ADMIN,
            UserRole.UNIT_SUPERVISOR,
            UserRole.SPECIAL_EDUCATION_TEACHER,
            UserRole.STAFF
        )

        return db.read { tx -> tx.getVasuTemplates(validOnly) }
    }

    @GetMapping("/{id}")
    fun getTemplate(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): VasuTemplate {
        Audit.VasuTemplateRead.log(id)
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.read { tx -> tx.getVasuTemplate(id) } ?: throw NotFound("template $id not found")
    }

    @DeleteMapping("/{id}")
    fun deleteTemplate(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ) {
        Audit.VasuTemplateDelete.log(id)
        user.requireOneOfRoles(UserRole.ADMIN)

        db.transaction { it.deleteUnusedVasuTemplate(id) }
    }

    @PutMapping("/{id}/content")
    fun putTemplateContent(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody content: VasuContent
    ) {
        Audit.VasuTemplateUpdate.log(id)
        user.requireOneOfRoles(UserRole.ADMIN)

        db.transaction { tx -> tx.updateVasuTemplateContent(id, content) }
    }
}
