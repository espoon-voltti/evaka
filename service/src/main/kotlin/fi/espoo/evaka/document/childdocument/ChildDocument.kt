// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.QuestionType
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

data class CheckboxGroupAnswerContent(val optionId: String, val extra: String = "")

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
sealed class AnsweredQuestion<Answer>(val type: QuestionType) {
    abstract val questionId: String
    abstract val answer: Answer

    abstract fun isStructurallyValid(question: Question): Boolean

    @JsonTypeName("TEXT")
    data class TextAnswer(override val questionId: String, override val answer: String) :
        AnsweredQuestion<String>(QuestionType.TEXT) {
        override fun isStructurallyValid(question: Question): Boolean {
            return question is Question.TextQuestion
        }
    }

    @JsonTypeName("CHECKBOX")
    data class CheckboxAnswer(override val questionId: String, override val answer: Boolean) :
        AnsweredQuestion<Boolean>(QuestionType.CHECKBOX) {
        override fun isStructurallyValid(question: Question): Boolean {
            return question is Question.CheckboxQuestion
        }
    }

    @JsonTypeName("CHECKBOX_GROUP")
    data class CheckboxGroupAnswer(
        override val questionId: String,
        override val answer: List<CheckboxGroupAnswerContent>
    ) : AnsweredQuestion<List<CheckboxGroupAnswerContent>>(QuestionType.CHECKBOX_GROUP) {
        override fun isStructurallyValid(question: Question): Boolean {
            if (question !is Question.CheckboxGroupQuestion) return false

            return answer.all { selected -> question.options.any { it.id == selected.optionId } }
        }
    }

    @JsonTypeName("RADIO_BUTTON_GROUP")
    data class RadioButtonGroupAnswer(
        override val questionId: String,
        override val answer: String?
    ) : AnsweredQuestion<String?>(QuestionType.RADIO_BUTTON_GROUP) {
        override fun isStructurallyValid(question: Question): Boolean {
            if (question !is Question.RadioButtonGroupQuestion) return false

            return answer == null || question.options.any { it.id == answer }
        }
    }

    @JsonTypeName("STATIC_TEXT_DISPLAY")
    data class StaticTextDisplayAnswer(
        override val questionId: String,
        override val answer: Nothing?
    ) : AnsweredQuestion<Nothing?>(QuestionType.STATIC_TEXT_DISPLAY) {
        override fun isStructurallyValid(question: Question): Boolean {
            return question is Question.StaticTextDisplayQuestion
        }
    }

    @JsonTypeName("DATE")
    data class DateAnswer(override val questionId: String, override val answer: LocalDate?) :
        AnsweredQuestion<LocalDate?>(QuestionType.DATE) {
        override fun isStructurallyValid(question: Question): Boolean {
            return question is Question.DateQuestion
        }
    }

    @JsonTypeName("GROUPED_TEXT_FIELDS")
    data class GroupedTextFieldsAnswer(
        override val questionId: String,
        override val answer: List<List<String>>
    ) : AnsweredQuestion<List<List<String>>>(QuestionType.GROUPED_TEXT_FIELDS) {
        override fun isStructurallyValid(question: Question): Boolean {
            if (question !is Question.GroupedTextFieldsQuestion) return false
            if (answer.isEmpty()) return false
            if (!question.allowMultipleRows && answer.size > 1) return false
            return answer.all { row -> row.size == question.fieldLabels.size }
        }
    }
}

@Json
data class DocumentContent(val answers: List<AnsweredQuestion<*>>) {
    init {
        // input sanity check since list element nullability is not fully guaranteed in jackson
        @Suppress("SENSELESS_COMPARISON")
        if (answers.any { it == null }) error("Document content must not contain null answers")
    }
}

enum class DocumentStatus(val editable: Boolean) : DatabaseEnum {
    DRAFT(editable = true),
    PREPARED(editable = true),
    COMPLETED(editable = false);

    override val sqlType: String = "child_document_status"
}

data class ChildDocumentSummary(
    val id: ChildDocumentId,
    val status: DocumentStatus,
    val type: DocumentType,
    val templateId: DocumentTemplateId,
    val templateName: String,
    val modifiedAt: HelsinkiDateTime,
    val publishedAt: HelsinkiDateTime?
)

data class ChildBasics(
    val id: PersonId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate?
)

data class ChildDocumentDetails(
    val id: ChildDocumentId,
    val status: DocumentStatus,
    val publishedAt: HelsinkiDateTime?,
    @Json val content: DocumentContent,
    @Json val publishedContent: DocumentContent?,
    @Nested("child") val child: ChildBasics,
    @Nested("template") val template: DocumentTemplate
)

data class ChildDocumentWithPermittedActions(
    val data: ChildDocumentDetails,
    val permittedActions: Set<Action.ChildDocument>
)

data class ChildDocumentCreateRequest(val childId: PersonId, val templateId: DocumentTemplateId)
