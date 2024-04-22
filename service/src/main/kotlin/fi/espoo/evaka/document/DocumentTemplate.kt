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
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.DateRange
import java.time.format.DateTimeFormatter
import org.jdbi.v3.json.Json

private data class Translations(val yes: String, val no: String)

private val translationsFi = Translations(yes = "Kyllä", no = "Ei")

private val translationsSv = Translations(yes = "Ja", no = "Nej")

private fun getTranslations(language: DocumentLanguage) =
    when (language) {
        DocumentLanguage.FI -> translationsFi
        DocumentLanguage.SV -> translationsSv
    }

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

    abstract fun generateHtml(
        answeredQuestion: AnsweredQuestion<*>?,
        language: DocumentLanguage
    ): HtmlElement

    fun htmlClassName() = "question question-${type.name.lowercase().replace('_', '-')}"

    @JsonTypeName("TEXT")
    data class TextQuestion(
        override val id: String,
        val label: String,
        val infoText: String = "",
        val multiline: Boolean = false
    ) : Question(QuestionType.TEXT) {
        override fun generateHtml(
            answeredQuestion: AnsweredQuestion<*>?,
            language: DocumentLanguage
        ): HtmlElement {
            if (answeredQuestion == null)
                return HtmlBuilder.div(className = htmlClassName()) {
                    listOf(label(label), span("-"))
                }

            if (
                answeredQuestion !is AnsweredQuestion.TextAnswer ||
                    !answeredQuestion.isStructurallyValid(this)
            ) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }

            return HtmlBuilder.div(className = htmlClassName()) {
                listOf(
                    label(label),
                    if (answeredQuestion.answer.isBlank()) {
                        span("-")
                    } else {
                        div { multilineText(answeredQuestion.answer) }
                    }
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
            answeredQuestion: AnsweredQuestion<*>?,
            language: DocumentLanguage
        ): HtmlElement {
            if (answeredQuestion == null)
                return HtmlBuilder.div(className = htmlClassName()) {
                    listOf(label(label), span("-"))
                }

            if (
                answeredQuestion !is AnsweredQuestion.CheckboxAnswer ||
                    !answeredQuestion.isStructurallyValid(this)
            ) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }
            val translations = getTranslations(language)
            return HtmlBuilder.div(className = htmlClassName()) {
                listOf(
                    label(label),
                    div(if (answeredQuestion.answer) translations.yes else translations.no)
                )
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
            answeredQuestion: AnsweredQuestion<*>?,
            language: DocumentLanguage
        ): HtmlElement {
            if (answeredQuestion == null)
                return HtmlBuilder.div(className = htmlClassName()) {
                    listOf(label(label), span("-"))
                }

            if (
                answeredQuestion !is AnsweredQuestion.CheckboxGroupAnswer ||
                    !answeredQuestion.isStructurallyValid(this)
            ) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }

            return HtmlBuilder.div(className = htmlClassName()) {
                listOf(
                    label(label),
                    if (answeredQuestion.answer.isEmpty()) {
                        span("-")
                    } else {
                        ul {
                            answeredQuestion.answer.map { answerOption ->
                                val option = options.find { it.id == answerOption.optionId }!!
                                li(
                                    "${option.label}${if (option.withText) " : ${answerOption.extra}" else ""}"
                                )
                            }
                        }
                    }
                )
            }
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
            answeredQuestion: AnsweredQuestion<*>?,
            language: DocumentLanguage
        ): HtmlElement {
            if (answeredQuestion == null)
                return HtmlBuilder.div(className = htmlClassName()) {
                    listOf(label(label), span("-"))
                }

            if (
                answeredQuestion !is AnsweredQuestion.RadioButtonGroupAnswer ||
                    !answeredQuestion.isStructurallyValid(this)
            ) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }

            return HtmlBuilder.div(className = htmlClassName()) {
                listOf(
                    label(label),
                    if (answeredQuestion.answer == null) {
                        span("-")
                    } else {
                        div(options.find { it.id == answeredQuestion.answer }!!.label)
                    }
                )
            }
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
            answeredQuestion: AnsweredQuestion<*>?,
            language: DocumentLanguage
        ): HtmlElement {
            return HtmlBuilder.div(className = htmlClassName()) {
                listOfNotNull(
                    if (label.isNotBlank()) label(label) else null,
                    if (text.isNotBlank()) div { multilineText(text) } else null
                )
            }
        }
    }

    @JsonTypeName("DATE")
    data class DateQuestion(override val id: String, val label: String, val infoText: String = "") :
        Question(QuestionType.DATE) {
        override fun generateHtml(
            answeredQuestion: AnsweredQuestion<*>?,
            language: DocumentLanguage
        ): HtmlElement {
            if (answeredQuestion == null)
                return HtmlBuilder.div(className = htmlClassName()) {
                    listOf(label(label), span("-"))
                }

            if (
                answeredQuestion !is AnsweredQuestion.DateAnswer ||
                    !answeredQuestion.isStructurallyValid(this)
            ) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }

            return HtmlBuilder.div(className = htmlClassName()) {
                listOf(
                    label(label),
                    div(
                        answeredQuestion.answer?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            ?: "-"
                    )
                )
            }
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
            answeredQuestion: AnsweredQuestion<*>?,
            language: DocumentLanguage
        ): HtmlElement {
            val headerRow = HtmlBuilder.tr { fieldLabels.map { th(it) } }
            val emptyRow = HtmlBuilder.tr { fieldLabels.map { td("-") } }
            val emptyAnswer =
                HtmlBuilder.div(className = htmlClassName()) {
                    listOf(label(label), table { listOf(headerRow, emptyRow) })
                }

            if (answeredQuestion == null) return emptyAnswer

            if (
                answeredQuestion !is AnsweredQuestion.GroupedTextFieldsAnswer ||
                    !answeredQuestion.isStructurallyValid(this)
            ) {
                throw IllegalArgumentException("Invalid answer to question $id")
            }

            if (answeredQuestion.answer.isEmpty()) return emptyAnswer

            return HtmlBuilder.div(className = htmlClassName()) {
                listOf(
                    label(label),
                    table {
                        answeredQuestion.answer.flatMap { answerRow ->
                            listOf(
                                headerRow,
                                tr {
                                    answerRow.map { answer ->
                                        td(answer.takeIf { it.isNotBlank() } ?: "-")
                                    }
                                }
                            )
                        }
                    }
                )
            }
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
enum class DocumentType(val statuses: List<DocumentStatus>) : DatabaseEnum {
    PEDAGOGICAL_REPORT(listOf(DocumentStatus.DRAFT, DocumentStatus.COMPLETED)),
    PEDAGOGICAL_ASSESSMENT(listOf(DocumentStatus.DRAFT, DocumentStatus.COMPLETED)),
    HOJKS(listOf(DocumentStatus.DRAFT, DocumentStatus.PREPARED, DocumentStatus.COMPLETED)),
    MIGRATED_VASU(listOf(DocumentStatus.COMPLETED)),
    MIGRATED_LEOPS(listOf(DocumentStatus.COMPLETED)),
    VASU(listOf(DocumentStatus.DRAFT, DocumentStatus.PREPARED, DocumentStatus.COMPLETED)),
    LEOPS(listOf(DocumentStatus.DRAFT, DocumentStatus.PREPARED, DocumentStatus.COMPLETED));

    fun isMigrated() = this in listOf(MIGRATED_VASU, MIGRATED_LEOPS)

    override val sqlType: String = "document_template_type"
}

enum class DocumentLanguage : DatabaseEnum {
    FI,
    SV;

    override val sqlType: String = "document_language"
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
