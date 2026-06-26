// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.EvakaEnv
import evaka.core.ForcePlainGet
import evaka.core.absence.getDaycareIdByGroup
import evaka.core.daycare.getDaycare
import evaka.core.shared.ChildId
import evaka.core.shared.DocumentTemplateId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.GroupId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.UiLanguage
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
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
@RequestMapping("/employee/document-templates")
class DocumentTemplateController(
    private val accessControl: AccessControl,
    private val evakaEnv: EvakaEnv,
    private val featureConfig: FeatureConfig,
) {
    @PostMapping
    fun createTemplate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: DocumentTemplateBasicsRequest,
    ): DocumentTemplate {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.CREATE_DOCUMENT_TEMPLATE,
                    )
                    validateLanguage(body.language, body.type)
                    validateDecisionConfig(body.endDecisionWhenUnitChanges, body.type)
                    tx.insertTemplate(body)
                }
            }
            .also { Audit.DocumentTemplateCreate.log(targetId = AuditId(it.id)) }
    }

    @PostMapping("/import")
    fun importTemplate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ExportedDocumentTemplate,
    ): DocumentTemplate {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.CREATE_DOCUMENT_TEMPLATE,
                    )
                    validateLanguage(body.language, body.type)
                    validateDecisionConfig(body.endDecisionWhenUnitChanges, body.type)
                    tx.importTemplate(body)
                }
            }
            .also { Audit.DocumentTemplateCreate.log(targetId = AuditId(it.id)) }
    }

    @GetMapping
    fun getTemplates(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<DocumentTemplateSummary> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_DOCUMENT_TEMPLATE,
                    )
                    tx.getTemplateSummaries()
                }
            }
            .also { Audit.DocumentTemplateRead.log() }
    }

    @GetMapping("/active")
    fun getActiveTemplates(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam childId: ChildId,
    ): List<DocumentTemplateSummary> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_DOCUMENT_TEMPLATE,
                    )

                    val placement =
                        tx.getCurrentOrNextPlacement(
                            childId,
                            clock.today(),
                            CITIZEN_DOCUMENT_CREATION_DAYS_BEFORE_PLACEMENT,
                        ) ?: return@read emptyList()

                    tx.getTemplateSummaries().filter {
                        it.published &&
                            it.validity.includes(clock.today()) &&
                            isTemplateApplicableToPlacement(
                                it.type,
                                it.language,
                                it.placementTypes,
                                placement,
                                clock.today(),
                            )
                    }
                }
            }
            .also { Audit.DocumentTemplateRead.log() }
    }

    @GetMapping("/activeByGroupId")
    fun getActiveTemplatesByGroupId(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam groupId: GroupId,
        @RequestParam(required = false) types: Set<ChildDocumentType> = emptySet(),
    ): List<DocumentTemplateSummary> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_DOCUMENT_TEMPLATE,
                    )

                    val unit = tx.getDaycare(tx.getDaycareIdByGroup(groupId))!!

                    tx.getTemplateSummaries().filter {
                        it.published &&
                            it.validity.includes(clock.today()) &&
                            (types.isEmpty() || types.contains(it.type)) &&
                            languageMatches(it.language, it.type, unit.language) &&
                            isAllowedByPilotFeatures(it.type, unit.enabledPilotFeatures)
                    }
                }
            }
            .also { Audit.DocumentTemplateRead.log() }
    }

    @GetMapping("/{templateId}")
    fun getTemplate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
    ): DocumentTemplate {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DocumentTemplate.READ,
                        templateId,
                    )
                    tx.getTemplate(templateId)
                }
            }
            ?.also { Audit.DocumentTemplateRead.log(targetId = AuditId(templateId)) }
            ?: throw NotFound("Document template $templateId not found")
    }

    @ForcePlainGet
    @GetMapping("/{templateId}/export")
    fun exportTemplate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
    ): ResponseEntity<ExportedDocumentTemplate> =
        db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DocumentTemplate.READ,
                        templateId,
                    )
                    tx.exportTemplate(templateId)
                }
            }
            ?.let { template ->
                val sanitizedName = template.name.replace(Regex("[^\\p{L}0-9]+"), "_").take(60)
                val timestamp = clock.now().toInstant().epochSecond
                ResponseEntity.ok()
                    .headers(
                        HttpHeaders().apply {
                            contentDisposition =
                                ContentDisposition.attachment()
                                    .filename(
                                        "$sanitizedName.$timestamp.template.json",
                                        Charsets.UTF_8,
                                    )
                                    .build()
                        }
                    )
                    .body(template)
            }
            ?.also { Audit.DocumentTemplateRead.log(targetId = AuditId(templateId)) }
            ?: throw NotFound("Document template $templateId not found")

    @PostMapping("/{templateId}/duplicate")
    fun duplicateTemplate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
        @RequestBody body: DocumentTemplateBasicsRequest,
    ): DocumentTemplate {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DocumentTemplate.COPY,
                        templateId,
                    )

                    validateLanguage(body.language, body.type)
                    validateDecisionConfig(body.endDecisionWhenUnitChanges, body.type)

                    tx.duplicateTemplate(templateId, body)
                }
            }
            .also { Audit.DocumentTemplateCopy.log(targetId = AuditId(templateId)) }
    }

    @PutMapping("/{templateId}")
    fun updateDraftTemplateBasics(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
        @RequestBody body: DocumentTemplateBasicsRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DocumentTemplate.UPDATE,
                        templateId,
                    )
                    validateLanguage(body.language, body.type)
                    validateDecisionConfig(body.endDecisionWhenUnitChanges, body.type)

                    tx.getTemplate(templateId)?.also {
                        if (it.published) throw BadRequest("Cannot update published template")
                    } ?: throw NotFound("Template $templateId not found")

                    tx.updateDraftTemplateBasics(templateId, body)
                }
                .also { Audit.DocumentTemplateUpdateBasics.log(targetId = AuditId(templateId)) }
        }
    }

    @PutMapping("/{templateId}/content")
    fun updateDraftTemplateContent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
        @RequestBody body: DocumentTemplateContent,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DocumentTemplate.UPDATE,
                        templateId,
                    )
                    tx.getTemplate(templateId)?.also {
                        if (it.published)
                            throw BadRequest("Cannot update contents of published template")
                    } ?: throw NotFound("Template $templateId not found")
                    assertUniqueIds(body)

                    tx.updateDraftTemplateContent(templateId, body)
                }
                .also { Audit.DocumentTemplateUpdateContent.log(targetId = AuditId(templateId)) }
        }
    }

    @PutMapping("/{templateId}/validity")
    fun updateTemplateValidity(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
        @RequestBody body: DateRange,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DocumentTemplate.UPDATE,
                        templateId,
                    )
                    tx.updateTemplateValidity(templateId, body)
                }
            }
            .also { Audit.DocumentTemplateUpdateValidity.log(targetId = AuditId(templateId)) }
    }

    @PutMapping("/{templateId}/publish")
    fun publishTemplate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DocumentTemplate.UPDATE,
                        templateId,
                    )
                    tx.publishTemplate(templateId)
                }
            }
            .also { Audit.DocumentTemplatePublish.log(targetId = AuditId(templateId)) }
    }

    /**
     * Unpublishes a template and deletes all related child documents and archived processes. Must
     * NOT be enabled in production environment.
     */
    @PutMapping("/{templateId}/force-unpublish")
    fun forceUnpublishTemplate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
    ) {
        if (!evakaEnv.forceUnpublishDocumentTemplateEnabled) {
            throw Forbidden("endpoint not enabled in this environment")
        }
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DocumentTemplate.FORCE_UNPUBLISH,
                        templateId,
                    )
                    tx.forceUnpublishTemplate(templateId)
                }
            }
            .also { Audit.DocumentTemplateForceUnpublish.log(targetId = AuditId(templateId)) }
    }

    @DeleteMapping("/{templateId}")
    fun deleteDraftTemplate(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.DocumentTemplate.DELETE,
                        templateId,
                    )
                    if (tx.getTemplate(templateId) == null)
                        throw NotFound("Template $templateId not found")
                    if (tx.templateHasDocuments(templateId))
                        throw BadRequest("Cannot delete template that has documents")

                    tx.deleteUnusedTemplate(templateId)
                }
            }
            .also { Audit.DocumentTemplateDelete.log(targetId = AuditId(templateId)) }
    }

    private fun validateLanguage(lang: UiLanguage, type: ChildDocumentType) {
        if (
            lang == UiLanguage.EN &&
                type != ChildDocumentType.CITIZEN_BASIC &&
                !featureConfig.allowEnglishChildDocumentsForAllTypes
        ) {
            throw BadRequest("English is not supported for this document type")
        }
    }
}

private fun validateDecisionConfig(endDecisionWhenUnitChanges: Boolean?, type: ChildDocumentType) {
    if (type.decision && endDecisionWhenUnitChanges == null) {
        throw BadRequest("Missing mandatory field endDecisionWhenUnitChanges")
    }
    if (!type.decision && endDecisionWhenUnitChanges != null) {
        throw BadRequest("Decision config not allowed for this document type")
    }
}

private fun assertUniqueIds(content: DocumentTemplateContent) {
    val sectionIds = content.sections.map { it.id }
    if (sectionIds.size > sectionIds.distinct().size)
        throw BadRequest("Found non unique section ids")

    val questionIds = content.sections.flatMap { it.questions.map { q -> q.id } }
    if (questionIds.size > questionIds.distinct().size)
        throw BadRequest("Found non unique question ids")
}
