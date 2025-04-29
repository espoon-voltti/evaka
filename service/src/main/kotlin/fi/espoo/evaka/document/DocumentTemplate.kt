// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.document.childdocument.AnsweredQuestion
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.process.DocumentConfidentiality
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.HtmlBuilder
import fi.espoo.evaka.shared.HtmlElement
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.UiLanguage
import java.time.format.DateTimeFormatter
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

private data class Translations(val yes: String, val no: String)

private val translationsFi = Translations(yes = "Kyllä", no = "Ei")

private val translationsSv = Translations(yes = "Ja", no = "Nej")

private val translationsEn = Translations(yes = "Yes", no = "No")

private fun getTranslations(language: UiLanguage) =
    when (language) {
        UiLanguage.FI -> translationsFi
        UiLanguage.SV -> translationsSv
        UiLanguage.EN -> translationsEn
    }

@ConstList("questionTypes")
enum class QuestionType {
    TEXT,
    CHECKBOX,
    CHECKBOX_GROUP,
    RADIO_BUTTON_GROUP,
    STATIC_TEXT_DISPLAY,
    DATE,
    GROUPED_TEXT_FIELDS,
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
)
sealed class Question(val type: QuestionType) {
    abstract val id: String

    abstract fun generateHtml(
        answeredQuestion: AnsweredQuestion<*>?,
        language: UiLanguage,
    ): HtmlElement

    fun htmlClassName() = "question question-${type.name.lowercase().replace('_', '-')}"

    @JsonTypeName("TEXT")
    data class TextQuestion(
        override val id: String,
        val label: String,
        val infoText: String = "",
        val multiline: Boolean = false,
    ) : Question(QuestionType.TEXT) {
        override fun generateHtml(
            answeredQuestion: AnsweredQuestion<*>?,
            language: UiLanguage,
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
                    },
                )
            }
        }
    }

    @JsonTypeName("CHECKBOX")
    data class CheckboxQuestion(
        override val id: String,
        val label: String,
        val infoText: String = "",
    ) : Question(QuestionType.CHECKBOX) {
        override fun generateHtml(
            answeredQuestion: AnsweredQuestion<*>?,
            language: UiLanguage,
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
                    div(if (answeredQuestion.answer) translations.yes else translations.no),
                )
            }
        }
    }

    @JsonTypeName("CHECKBOX_GROUP")
    data class CheckboxGroupQuestion(
        override val id: String,
        val label: String,
        val options: List<CheckboxGroupQuestionOption>,
        val infoText: String = "",
    ) : Question(QuestionType.CHECKBOX_GROUP) {
        override fun generateHtml(
            answeredQuestion: AnsweredQuestion<*>?,
            language: UiLanguage,
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
                    },
                )
            }
        }
    }

    @JsonTypeName("RADIO_BUTTON_GROUP")
    data class RadioButtonGroupQuestion(
        override val id: String,
        val label: String,
        val options: List<RadioButtonGroupQuestionOption>,
        val infoText: String = "",
    ) : Question(QuestionType.RADIO_BUTTON_GROUP) {
        override fun generateHtml(
            answeredQuestion: AnsweredQuestion<*>?,
            language: UiLanguage,
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
                    },
                )
            }
        }
    }

    @JsonTypeName("STATIC_TEXT_DISPLAY")
    data class StaticTextDisplayQuestion(
        override val id: String,
        val label: String = "",
        val text: String = "",
        val infoText: String = "",
    ) : Question(QuestionType.STATIC_TEXT_DISPLAY) {
        override fun generateHtml(
            answeredQuestion: AnsweredQuestion<*>?,
            language: UiLanguage,
        ): HtmlElement {
            return HtmlBuilder.div(className = htmlClassName()) {
                listOfNotNull(
                    if (label.isNotBlank()) label(label) else null,
                    if (text.isNotBlank()) div { multilineText(text) } else null,
                )
            }
        }
    }

    @JsonTypeName("DATE")
    data class DateQuestion(override val id: String, val label: String, val infoText: String = "") :
        Question(QuestionType.DATE) {
        override fun generateHtml(
            answeredQuestion: AnsweredQuestion<*>?,
            language: UiLanguage,
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
                    ),
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
        val allowMultipleRows: Boolean,
    ) : Question(QuestionType.GROUPED_TEXT_FIELDS) {
        override fun generateHtml(
            answeredQuestion: AnsweredQuestion<*>?,
            language: UiLanguage,
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
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}

data class CheckboxGroupQuestionOption(
    val id: String,
    val label: String,
    val withText: Boolean = false,
)

data class RadioButtonGroupQuestionOption(val id: String, val label: String)

data class Section(
    val id: String,
    val label: String,
    val questions: List<Question>,
    val infoText: String = "",
)

@Json data class DocumentTemplateContent(val sections: List<Section>)

/**
 * statuses: an ordered list of statuses, which defines a linear state machine.
 *
 * manuallyPublishable: if the document can be manually published to guardian (even as a draft)
 *
 * decision: if the document is a decision with accept/reject/annul functionality as the final step
 *
 * autoCompleteAtEndOfValidity: if the document is automatically completed and published when the
 * template validity ends
 */
@ConstList("documentTypes")
enum class DocumentType(
    val statuses: List<DocumentStatus>,
    val manuallyPublishable: Boolean,
    val decision: Boolean,
    val autoCompleteAtEndOfValidity: Boolean,
) : DatabaseEnum {
    PEDAGOGICAL_REPORT(
        statuses = listOf(DocumentStatus.DRAFT, DocumentStatus.COMPLETED),
        manuallyPublishable = true,
        decision = false,
        autoCompleteAtEndOfValidity = false,
    ),
    PEDAGOGICAL_ASSESSMENT(
        statuses = listOf(DocumentStatus.DRAFT, DocumentStatus.COMPLETED),
        manuallyPublishable = true,
        decision = false,
        autoCompleteAtEndOfValidity = false,
    ),
    HOJKS(
        statuses = listOf(DocumentStatus.DRAFT, DocumentStatus.PREPARED, DocumentStatus.COMPLETED),
        manuallyPublishable = true,
        decision = false,
        autoCompleteAtEndOfValidity = true,
    ),
    MIGRATED_VASU(
        statuses = listOf(DocumentStatus.COMPLETED),
        manuallyPublishable = false,
        decision = false,
        autoCompleteAtEndOfValidity = false,
    ),
    MIGRATED_LEOPS(
        statuses = listOf(DocumentStatus.COMPLETED),
        manuallyPublishable = false,
        decision = false,
        autoCompleteAtEndOfValidity = false,
    ),
    VASU(
        statuses = listOf(DocumentStatus.DRAFT, DocumentStatus.PREPARED, DocumentStatus.COMPLETED),
        manuallyPublishable = true,
        decision = false,
        autoCompleteAtEndOfValidity = true,
    ),
    LEOPS(
        statuses = listOf(DocumentStatus.DRAFT, DocumentStatus.PREPARED, DocumentStatus.COMPLETED),
        manuallyPublishable = true,
        decision = false,
        autoCompleteAtEndOfValidity = true,
    ),
    CITIZEN_BASIC(
        statuses =
            listOf(DocumentStatus.DRAFT, DocumentStatus.CITIZEN_DRAFT, DocumentStatus.COMPLETED),
        manuallyPublishable = false,
        decision = false,
        autoCompleteAtEndOfValidity = false,
    ),
    OTHER_DECISION(
        statuses =
            listOf(
                DocumentStatus.DRAFT,
                DocumentStatus.DECISION_PROPOSAL,
                DocumentStatus.COMPLETED,
            ),
        manuallyPublishable = false,
        decision = true,
        autoCompleteAtEndOfValidity = false,
    ),
    OTHER(
        statuses = listOf(DocumentStatus.DRAFT, DocumentStatus.COMPLETED),
        manuallyPublishable = true,
        decision = false,
        autoCompleteAtEndOfValidity = false,
    );

    override val sqlType: String = "document_template_type"
}

data class DocumentTemplate(
    val id: DocumentTemplateId,
    val name: String,
    val type: DocumentType,
    val placementTypes: Set<PlacementType>,
    val language: UiLanguage,
    @Nested("confidentiality") val confidentiality: DocumentConfidentiality?,
    val legalBasis: String,
    val validity: DateRange,
    val published: Boolean,
    val processDefinitionNumber: String?,
    val archiveDurationMonths: Int?,
    val archiveExternally: Boolean,
    @Json val content: DocumentTemplateContent,
)

data class ExportedDocumentTemplate(
    val name: String,
    val type: DocumentType,
    val placementTypes: Set<PlacementType>,
    val language: UiLanguage,
    val confidentiality: DocumentConfidentiality?,
    val legalBasis: String,
    val validity: DateRange,
    val processDefinitionNumber: String?,
    val archiveDurationMonths: Int?,
    val archiveExternally: Boolean,
    @Json val content: DocumentTemplateContent,
)

data class DocumentTemplateBasicsRequest(
    val name: String,
    val type: DocumentType,
    val placementTypes: Set<PlacementType>,
    val language: UiLanguage,
    val confidentiality: DocumentConfidentiality?,
    val legalBasis: String,
    val validity: DateRange,
    val processDefinitionNumber: String?,
    val archiveDurationMonths: Int?,
    val archiveExternally: Boolean,
)

data class DocumentTemplateSummary(
    val id: DocumentTemplateId,
    val name: String,
    val type: DocumentType,
    val placementTypes: Set<PlacementType>,
    val language: UiLanguage,
    val validity: DateRange,
    val published: Boolean,
    val documentCount: Int,
)
