// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.json.Json

@ConstList("questionTypes")
enum class QuestionType {
    TEXT,
    CHECKBOX,
    CHECKBOX_GROUP,
    RADIO_BUTTON_GROUP,
    STATIC_TEXT_DISPLAY
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
sealed class Question(val type: QuestionType) {
    abstract val id: String

    @JsonTypeName("TEXT")
    data class TextQuestion(
        override val id: String,
        val label: String,
        val infoText: String = "",
        val multiline: Boolean = false
    ) : Question(QuestionType.TEXT)

    @JsonTypeName("CHECKBOX")
    data class CheckboxQuestion(
        override val id: String,
        val label: String,
        val infoText: String = ""
    ) : Question(QuestionType.CHECKBOX)

    @JsonTypeName("CHECKBOX_GROUP")
    data class CheckboxGroupQuestion(
        override val id: String,
        val label: String,
        val options: List<CheckboxGroupQuestionOption>,
        val infoText: String = ""
    ) : Question(QuestionType.CHECKBOX_GROUP)

    @JsonTypeName("RADIO_BUTTON_GROUP")
    data class RadioButtonGroupQuestion(
        override val id: String,
        val label: String,
        val options: List<RadioButtonGroupQuestionOption>,
        val infoText: String = ""
    ) : Question(QuestionType.RADIO_BUTTON_GROUP)

    @JsonTypeName("STATIC_TEXT_DISPLAY")
    data class StaticTextDisplayQuestion(
        override val id: String,
        val label: String = "",
        val text: String = "",
        val infoText: String = ""
    ) : Question(QuestionType.STATIC_TEXT_DISPLAY)
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
    HOJKS(listOf(DocumentStatus.DRAFT, DocumentStatus.PREPARED, DocumentStatus.COMPLETED))
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

data class DocumentTemplateCreateRequest(
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
