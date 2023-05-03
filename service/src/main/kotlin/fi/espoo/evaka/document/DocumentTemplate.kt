// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.json.Json
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/document-templates")
class DocumentTemplateController(private val accessControl: AccessControl) {
    @PostMapping
    fun createTemplate(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: DocumentTemplateCreateRequest
    ): DocumentTemplate {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.CREATE_DOCUMENT_TEMPLATE
                )
                tx.insertTemplate(body)
            }
        }
    }

    @GetMapping
    fun getTemplates(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<DocumentTemplateSummary> {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.READ_DOCUMENT_TEMPLATE
                )
                tx.getTemplateSummaries()
            }
        }
    }

    @GetMapping("/{templateId}")
    fun getTemplate(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId
    ): DocumentTemplate {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.DocumentTemplate.READ,
                    templateId
                )
                tx.getTemplate(templateId)
            }
        }
            ?: throw NotFound("Document template $templateId not found")
    }

    @PostMapping("/{templateId}/duplicate")
    fun duplicateTemplate(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
        @RequestBody body: DocumentTemplateCreateRequest
    ): DocumentTemplate {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.DocumentTemplate.COPY,
                    templateId
                )

                tx.duplicateTemplate(templateId, body)
            }
        }
    }

    @PutMapping("/{templateId}/content")
    fun updateDraftTemplateContent(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
        @RequestBody body: DocumentTemplateContent
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.DocumentTemplate.UPDATE,
                    templateId
                )
                tx.getTemplate(templateId)?.also {
                    if (it.published)
                        throw BadRequest("Cannot update contents of published template")
                }
                    ?: throw NotFound("Template $templateId not found")
                assertUniqueIds(body)

                tx.updateDraftTemplateContent(templateId, body)
            }
        }
    }

    @PutMapping("/{templateId}/validity")
    fun updateTemplateValidity(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId,
        @RequestBody body: DateRange
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.DocumentTemplate.UPDATE,
                    templateId
                )
                tx.updateTemplateValidity(templateId, body)
            }
        }
    }

    @PutMapping("/{templateId}/publish")
    fun publishTemplate(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.DocumentTemplate.UPDATE,
                    templateId
                )
                tx.publishTemplate(templateId)
            }
        }
    }

    @DeleteMapping("/{templateId}")
    fun deleteDraftTemplate(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable templateId: DocumentTemplateId
    ) {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.DocumentTemplate.DELETE,
                    templateId
                )
                tx.getTemplate(templateId)?.also {
                    if (it.published) throw BadRequest("Cannot delete published template")
                }
                    ?: throw NotFound("Template $templateId not found")

                tx.deleteDraftTemplate(templateId)
            }
        }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface Question {
    val id: String

    @JsonTypeName("TEXT")
    data class TextQuestion(override val id: String, val label: String) : Question

    @JsonTypeName("CHECKBOX")
    data class CheckboxQuestion(override val id: String, val label: String) : Question

    @JsonTypeName("CHECKBOX_GROUP")
    data class CheckboxGroupQuestion(
        override val id: String,
        val label: String,
        val options: List<MultiselectOption>
    ) : Question
}

data class MultiselectOption(val id: String, val label: String)

data class Section(val id: String, val label: String, val questions: List<Question>)

@Json data class DocumentTemplateContent(val sections: List<Section>)

data class DocumentTemplate(
    val id: DocumentTemplateId,
    val name: String,
    val validity: DateRange,
    val published: Boolean,
    @Json val content: DocumentTemplateContent
)

data class DocumentTemplateCreateRequest(val name: String, val validity: DateRange)

data class DocumentTemplateSummary(
    val id: DocumentTemplateId,
    val name: String,
    val validity: DateRange,
    val published: Boolean
)

fun Database.Transaction.insertTemplate(template: DocumentTemplateCreateRequest): DocumentTemplate {
    return createQuery(
            """
        INSERT INTO document_template (name, validity, content) 
        VALUES (:name, :validity, :content::jsonb)
        RETURNING *
    """
                .trimIndent()
        )
        .bindKotlin(template)
        .bind("content", DocumentTemplateContent(sections = emptyList()))
        .mapTo<DocumentTemplate>()
        .one()
}

fun Database.Transaction.duplicateTemplate(
    id: DocumentTemplateId,
    template: DocumentTemplateCreateRequest
): DocumentTemplate {
    return createQuery(
            """
        INSERT INTO document_template (name, validity, content) 
        SELECT :name, :validity, content FROM document_template WHERE id = :id
        RETURNING *
    """
                .trimIndent()
        )
        .bind("id", id)
        .bindKotlin(template)
        .mapTo<DocumentTemplate>()
        .one()
}

fun Database.Transaction.getTemplateSummaries(): List<DocumentTemplateSummary> {
    return createQuery(
            """
        SELECT id, name, validity, published
        FROM document_template
    """
                .trimIndent()
        )
        .mapTo<DocumentTemplateSummary>()
        .list()
}

fun Database.Transaction.getTemplate(id: DocumentTemplateId): DocumentTemplate? {
    return createQuery("SELECT * FROM document_template WHERE id = :id")
        .bind("id", id)
        .mapTo<DocumentTemplate>()
        .firstOrNull()
}

fun Database.Transaction.updateDraftTemplateContent(
    id: DocumentTemplateId,
    content: DocumentTemplateContent
) {
    createUpdate(
            """
        UPDATE document_template
        SET content = :content
        WHERE id = :id AND published = false
    """
                .trimIndent()
        )
        .bind("id", id)
        .bind("content", content)
        .updateExactlyOne()
}

fun Database.Transaction.updateTemplateValidity(id: DocumentTemplateId, validity: DateRange) {
    createUpdate(
            """
        UPDATE document_template
        SET validity = :validity
        WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .bind("validity", validity)
        .updateExactlyOne()
}

fun Database.Transaction.publishTemplate(id: DocumentTemplateId) {
    createUpdate(
            """
        UPDATE document_template
        SET published = true
        WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .updateExactlyOne()
}

fun Database.Transaction.deleteDraftTemplate(id: DocumentTemplateId) {
    createUpdate(
            """
        DELETE FROM document_template 
        WHERE id = :id AND published = false
    """
                .trimIndent()
        )
        .bind("id", id)
        .updateExactlyOne()
}

private fun assertUniqueIds(content: DocumentTemplateContent) {
    val sectionIds = content.sections.map { it.id }
    if (sectionIds.size > sectionIds.distinct().size)
        throw BadRequest("Found non unique section ids")

    val questionIds = content.sections.flatMap { it.questions.map { q -> q.id } }
    if (questionIds.size > questionIds.distinct().size)
        throw BadRequest("Found non unique question ids")
}
