// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.QuestionType
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.PersonId
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
sealed class AnsweredQuestion<Answer>(val type: QuestionType) {
    abstract val questionId: String
    abstract val answer: Answer

    @JsonTypeName("TEXT")
    data class TextAnswer(override val questionId: String, override val answer: String) :
        AnsweredQuestion<String>(QuestionType.TEXT)

    @JsonTypeName("CHECKBOX")
    data class CheckboxAnswer(override val questionId: String, override val answer: Boolean) :
        AnsweredQuestion<Boolean>(QuestionType.CHECKBOX)

    @JsonTypeName("CHECKBOX_GROUP")
    data class CheckboxGroupAnswer(
        override val questionId: String,
        override val answer: List<String>
    ) : AnsweredQuestion<List<String>>(QuestionType.CHECKBOX_GROUP)

    @JsonTypeName("RADIO_BUTTON_GROUP")
    data class RadioButtonGroupAnswer(
        override val questionId: String,
        override val answer: String?
    ) : AnsweredQuestion<String?>(QuestionType.RADIO_BUTTON_GROUP)
}

@Json data class DocumentContent(val answers: List<AnsweredQuestion<*>>)

data class ChildDocumentSummary(
    val id: ChildDocumentId,
    val type: DocumentType,
    val templateName: String,
    val published: Boolean
)

data class ChildBasics(
    val id: PersonId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate?
)

data class ChildDocumentDetails(
    val id: ChildDocumentId,
    val published: Boolean,
    @Json val content: DocumentContent,
    @Nested("child") val child: ChildBasics,
    @Nested("template") val template: DocumentTemplate
)

data class ChildDocumentCreateRequest(val childId: PersonId, val templateId: DocumentTemplateId)
