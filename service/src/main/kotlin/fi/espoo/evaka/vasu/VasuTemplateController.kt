// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/vasu/templates")
class VasuTemplateController(
    private val accessControl: AccessControl
) {
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
    ): VasuTemplateId {
        Audit.VasuTemplateCreate.log()
        accessControl.requirePermissionFor(user, Action.Global.CREATE_VASU_TEMPLATE)

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
        @PathVariable id: VasuTemplateId,
        @RequestBody body: VasuTemplateUpdate
    ) {
        Audit.VasuTemplateEdit.log()
        accessControl.requirePermissionFor(user, Action.VasuTemplate.UPDATE, id)

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
        @PathVariable id: VasuTemplateId,
        @RequestBody body: CopyTemplateRequest
    ): VasuTemplateId {
        Audit.VasuTemplateCopy.log(id)
        accessControl.requirePermissionFor(user, Action.VasuTemplate.COPY, id)

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
        accessControl.requirePermissionFor(user, Action.Global.READ_VASU_TEMPLATE)

        return db.read { tx -> tx.getVasuTemplates(validOnly) }
    }

    @GetMapping("/{id}")
    fun getTemplate(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: VasuTemplateId
    ): VasuTemplate {
        Audit.VasuTemplateRead.log(id)
        user.requireOneOfRoles(UserRole.ADMIN)
        accessControl.requirePermissionFor(user, Action.VasuTemplate.READ, id)

        return db.read { tx -> tx.getVasuTemplate(id) } ?: throw NotFound("template $id not found")
    }

    @DeleteMapping("/{id}")
    fun deleteTemplate(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: VasuTemplateId
    ) {
        Audit.VasuTemplateDelete.log(id)
        accessControl.requirePermissionFor(user, Action.VasuTemplate.DELETE, id)

        db.transaction { it.deleteUnusedVasuTemplate(id) }
    }

    @PutMapping("/{id}/content")
    fun putTemplateContent(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: VasuTemplateId,
        @RequestBody content: VasuContent
    ) {
        Audit.VasuTemplateUpdate.log(id)
        accessControl.requirePermissionFor(user, Action.VasuTemplate.UPDATE, id)

        db.transaction { tx ->
            val template = tx.getVasuTemplateForUpdate(id) ?: throw NotFound("template $id not found")
            if (template.documentCount > 0) throw Conflict("Template with documents cannot be updated", "TEMPLATE_IN_USE")
            tx.updateVasuTemplateContent(id, content)
        }
    }
}
