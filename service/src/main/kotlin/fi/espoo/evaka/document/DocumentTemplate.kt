// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.json.Json

enum class QuestionType {
    TEXT,
    CHECKBOX,
    CHECKBOX_GROUP
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
sealed class Question(val type: QuestionType) {
    abstract val id: String

    @JsonTypeName("TEXT")
    data class TextQuestion(override val id: String, val label: String) :
        Question(QuestionType.TEXT)

    @JsonTypeName("CHECKBOX")
    data class CheckboxQuestion(override val id: String, val label: String) :
        Question(QuestionType.CHECKBOX)

    @JsonTypeName("CHECKBOX_GROUP")
    data class CheckboxGroupQuestion(
        override val id: String,
        val label: String,
        val options: List<MultiselectOption>
    ) : Question(QuestionType.CHECKBOX_GROUP)
}

data class MultiselectOption(val id: String, val label: String)

data class Section(val id: String, val label: String, val questions: List<Question>)

@Json data class DocumentTemplateContent(val sections: List<Section>)

enum class DocumentType {
    PEDAGOGICAL_REPORT,
    PEDAGOGICAL_ASSESSMENT
}

data class DocumentTemplate(
    val id: DocumentTemplateId,
    val name: String,
    val type: DocumentType,
    val validity: DateRange,
    val published: Boolean,
    @Json val content: DocumentTemplateContent
)

data class DocumentTemplateCreateRequest(
    val name: String,
    val type: DocumentType,
    val validity: DateRange
)

data class DocumentTemplateSummary(
    val id: DocumentTemplateId,
    val name: String,
    val type: DocumentType,
    val validity: DateRange,
    val published: Boolean
)
