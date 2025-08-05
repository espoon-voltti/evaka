// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.ForcePlainGet
import fi.espoo.evaka.absence.getDaycareIdByGroup
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.UiLanguage
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
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
                        tx.getChildActivePlacementInfo(childId, clock.today())
                            ?: return@read emptyList()

                    tx.getTemplateSummaries().filter {
                        it.published &&
                            it.validity.includes(clock.today()) &&
                            it.placementTypes.contains(placement.type) &&
                            (it.language.name.uppercase() ==
                                placement.unitLanguage.name.uppercase() ||
                                it.language == UiLanguage.EN)
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
                            (it.language.name.uppercase() == unit.language.name.uppercase())
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
                    tx.getTemplate(templateId)?.also {
                        if (it.published) throw BadRequest("Cannot delete published template")
                    } ?: throw NotFound("Template $templateId not found")

                    tx.deleteDraftTemplate(templateId)
                }
            }
            .also { Audit.DocumentTemplateDelete.log(targetId = AuditId(templateId)) }
    }
}

private fun validateLanguage(lang: UiLanguage, type: ChildDocumentType) {
    if (type != ChildDocumentType.CITIZEN_BASIC && lang == UiLanguage.EN) {
        throw BadRequest("English is not supported for this document type")
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

private data class ActivePlacementInfo(val type: PlacementType, val unitLanguage: Language)

private fun Database.Read.getChildActivePlacementInfo(
    childId: ChildId,
    date: LocalDate,
): ActivePlacementInfo? =
    createQuery {
            sql(
                """
SELECT pl.type, d.language AS unit_language
FROM placement pl
JOIN daycare d on d.id = pl.unit_id
WHERE pl.child_id = ${bind(childId)} AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
"""
            )
        }
        .exactlyOneOrNull<ActivePlacementInfo>()
