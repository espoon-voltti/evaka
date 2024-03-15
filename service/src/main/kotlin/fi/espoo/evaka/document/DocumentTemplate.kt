// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.document.childdocument.AnsweredQuestion
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.HtmlBuilder
import fi.espoo.evaka.shared.HtmlElement
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.json.Json

@ConstList("questionTypes")
enum class QuestionType {
    TEXT,
    CHECKBOX,
    CHECKBOX_GROUP,
    RADIO_BUTTON_GROUP,
    STATIC_TEXT_DISPLAY,
    DATE,
    GROUPED_TEXT_FIELDS
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
sealed class Question(val type: QuestionType) {
    abstract val id: String

    abstract fun generateHtml(answer: AnsweredQuestion<*>, language: DocumentLanguage): HtmlElement

    fun htmlClassName() = "question question-${type.name.lowercase().replace('_', '-')}"

    @JsonTypeName("TEXT")
    data class TextQuestion(
        override val id: String,
        val label: String,
        val infoText: String = "",
        val multiline: Boolean = false
    ) : Question(QuestionType.TEXT) {
        override fun generateHtml(
            answer: AnsweredQuestion<*>,
            language: DocumentLanguage
        ): HtmlElement {
            if (answer !is AnsweredQuestion.TextAnswer || !answer.isStructurallyValid(this)) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }
            return HtmlBuilder.div(className = htmlClassName()) {
                listOf(
                    label(label),
                    div { answer.answer.split("\n").flatMap { listOf(text(it), br()) } }
                )
            }
        }
    }

    @JsonTypeName("CHECKBOX")
    data class CheckboxQuestion(
        override val id: String,
        val label: String,
        val infoText: String = ""
    ) : Question(QuestionType.CHECKBOX) {
        override fun generateHtml(
            answer: AnsweredQuestion<*>,
            language: DocumentLanguage
        ): HtmlElement {
            if (answer !is AnsweredQuestion.CheckboxAnswer || !answer.isStructurallyValid(this)) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }
            val answerText =
                when (language) {
                    DocumentLanguage.FI -> if (answer.answer) "Kyllä" else "Ei"
                    DocumentLanguage.SV -> if (answer.answer) "Ja" else "Nej"
                }
            return HtmlBuilder.div(className = htmlClassName()) {
                listOf(label(label), div(answerText))
            }
        }
    }

    @JsonTypeName("CHECKBOX_GROUP")
    data class CheckboxGroupQuestion(
        override val id: String,
        val label: String,
        val options: List<CheckboxGroupQuestionOption>,
        val infoText: String = ""
    ) : Question(QuestionType.CHECKBOX_GROUP) {
        override fun generateHtml(
            answer: AnsweredQuestion<*>,
            language: DocumentLanguage
        ): HtmlElement {
            if (
                answer !is AnsweredQuestion.CheckboxGroupAnswer || !answer.isStructurallyValid(this)
            ) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }
            return HtmlBuilder.div("TODO", className = htmlClassName())
        }
    }

    @JsonTypeName("RADIO_BUTTON_GROUP")
    data class RadioButtonGroupQuestion(
        override val id: String,
        val label: String,
        val options: List<RadioButtonGroupQuestionOption>,
        val infoText: String = ""
    ) : Question(QuestionType.RADIO_BUTTON_GROUP) {
        override fun generateHtml(
            answer: AnsweredQuestion<*>,
            language: DocumentLanguage
        ): HtmlElement {
            if (
                answer !is AnsweredQuestion.RadioButtonGroupAnswer ||
                    !answer.isStructurallyValid(this)
            ) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }
            return HtmlBuilder.div("TODO", className = htmlClassName())
        }
    }

    @JsonTypeName("STATIC_TEXT_DISPLAY")
    data class StaticTextDisplayQuestion(
        override val id: String,
        val label: String = "",
        val text: String = "",
        val infoText: String = ""
    ) : Question(QuestionType.STATIC_TEXT_DISPLAY) {
        override fun generateHtml(
            answer: AnsweredQuestion<*>,
            language: DocumentLanguage
        ): HtmlElement {
            if (
                answer !is AnsweredQuestion.StaticTextDisplayAnswer ||
                    !answer.isStructurallyValid(this)
            ) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }
            return HtmlBuilder.div("TODO", className = htmlClassName())
        }
    }

    @JsonTypeName("DATE")
    data class DateQuestion(override val id: String, val label: String, val infoText: String = "") :
        Question(QuestionType.DATE) {
        override fun generateHtml(
            answer: AnsweredQuestion<*>,
            language: DocumentLanguage
        ): HtmlElement {
            if (answer !is AnsweredQuestion.DateAnswer || !answer.isStructurallyValid(this)) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }
            return HtmlBuilder.div("TODO", className = htmlClassName())
        }
    }

    @JsonTypeName("GROUPED_TEXT_FIELDS")
    data class GroupedTextFieldsQuestion(
        override val id: String,
        val label: String,
        val fieldLabels: List<String>,
        val infoText: String = "",
        val allowMultipleRows: Boolean
    ) : Question(QuestionType.GROUPED_TEXT_FIELDS) {
        override fun generateHtml(
            answer: AnsweredQuestion<*>,
            language: DocumentLanguage
        ): HtmlElement {
            if (
                answer !is AnsweredQuestion.GroupedTextFieldsAnswer ||
                    !answer.isStructurallyValid(this)
            ) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }
            return HtmlBuilder.div("TODO", className = htmlClassName())
        }
    }
}

data class CheckboxGroupQuestionOption(
    val id: String,
    val label: String,
    val withText: Boolean = false
)

data class RadioButtonGroupQuestionOption(val id: String, val label: String)

data class Section(
    val id: String,
    val label: String,
    val questions: List<Question>,
    val infoText: String = ""
)

@Json data class DocumentTemplateContent(val sections: List<Section>)

/** statuses is an ordered list which defines a linear state machine */
@ConstList("documentTypes")
enum class DocumentType(val statuses: List<DocumentStatus>) {
    PEDAGOGICAL_REPORT(listOf(DocumentStatus.DRAFT, DocumentStatus.COMPLETED)),
    PEDAGOGICAL_ASSESSMENT(listOf(DocumentStatus.DRAFT, DocumentStatus.COMPLETED)),
    HOJKS(listOf(DocumentStatus.DRAFT, DocumentStatus.PREPARED, DocumentStatus.COMPLETED)),
    MIGRATED_VASU(listOf(DocumentStatus.COMPLETED)),
    MIGRATED_LEOPS(listOf(DocumentStatus.COMPLETED));

    fun isMigrated() = this in listOf(MIGRATED_VASU, MIGRATED_LEOPS)
}

enum class DocumentLanguage {
    FI,
    SV
}

data class DocumentTemplate(
    val id: DocumentTemplateId,
    val name: String,
    val type: DocumentType,
    val language: DocumentLanguage,
    val confidential: Boolean,
    val legalBasis: String,
    val validity: DateRange,
    val published: Boolean,
    @Json val content: DocumentTemplateContent
)

data class ExportedDocumentTemplate(
    val name: String,
    val type: DocumentType,
    val language: DocumentLanguage,
    val confidential: Boolean,
    val legalBasis: String,
    val validity: DateRange,
    @Json val content: DocumentTemplateContent
)

data class DocumentTemplateBasicsRequest(
    val name: String,
    val type: DocumentType,
    val language: DocumentLanguage,
    val confidential: Boolean,
    val legalBasis: String,
    val validity: DateRange
)

data class DocumentTemplateSummary(
    val id: DocumentTemplateId,
    val name: String,
    val type: DocumentType,
    val language: DocumentLanguage,
    val validity: DateRange,
    val published: Boolean
)
