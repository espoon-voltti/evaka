// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json
import java.time.LocalDate
import java.util.UUID

enum class VasuDocumentEventType {
    PUBLISHED,
    MOVED_TO_READY,
    RETURNED_TO_READY,
    MOVED_TO_REVIEWED,
    RETURNED_TO_REVIEWED,
    MOVED_TO_CLOSED
}

enum class VasuDocumentState {
    DRAFT,
    READY,
    REVIEWED,
    CLOSED
}

data class VasuDocumentEvent(
    val id: UUID,
    val created: HelsinkiDateTime,
    val eventType: VasuDocumentEventType
)

enum class VasuLanguage {
    FI,
    SV
}

data class VasuDocumentSummary(
    val id: VasuDocumentId,
    val name: String,
    val modifiedAt: HelsinkiDateTime,
    val events: List<VasuDocumentEvent> = listOf(),
)

data class VasuDocument(
    val id: VasuDocumentId,
    val modifiedAt: HelsinkiDateTime,
    @Json
    val events: List<VasuDocumentEvent> = listOf(),
    @Nested("child")
    val child: VasuDocumentChild,
    val templateName: String,
    @Json
    val content: VasuContent,
    @Json
    val authorsContent: AuthorsContent,
    @Json
    val vasuDiscussionContent: VasuDiscussionContent,
    @Json
    val evaluationDiscussionContent: EvaluationDiscussionContent
) {
    fun getState(): VasuDocumentState = events.fold(VasuDocumentState.DRAFT) { acc, event ->
        when (event.eventType) {
            VasuDocumentEventType.PUBLISHED -> acc
            VasuDocumentEventType.MOVED_TO_READY -> VasuDocumentState.READY
            VasuDocumentEventType.RETURNED_TO_READY -> VasuDocumentState.READY
            VasuDocumentEventType.MOVED_TO_REVIEWED -> VasuDocumentState.REVIEWED
            VasuDocumentEventType.RETURNED_TO_REVIEWED -> VasuDocumentState.REVIEWED
            VasuDocumentEventType.MOVED_TO_CLOSED -> VasuDocumentState.CLOSED
        }
    }
}

data class VasuDocumentChild(
    val id: UUID,
    val firstName: String,
    val lastName: String
)

@Json
data class VasuContent(
    val sections: List<VasuSection>
) {
    fun matchesStructurally(content: VasuContent): Boolean =
        this.sections.size == content.sections.size && this.sections.withIndex().all { (index, section) ->
            section.matchesStructurally(content.sections.getOrNull(index))
        }
}

data class VasuSection(
    val name: String,
    val questions: List<VasuQuestion>
) {
    fun matchesStructurally(section: VasuSection?): Boolean =
        section != null && section.name == this.name && section.questions.size == this.questions.size &&
            this.questions.withIndex()
                .all { (index, question) -> question.equalsIgnoringValue(section.questions.getOrNull(index)) }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = VasuQuestion.TextQuestion::class, name = "TEXT"),
    JsonSubTypes.Type(value = VasuQuestion.CheckboxQuestion::class, name = "CHECKBOX"),
    JsonSubTypes.Type(value = VasuQuestion.RadioGroupQuestion::class, name = "RADIO_GROUP"),
    JsonSubTypes.Type(value = VasuQuestion.MultiSelectQuestion::class, name = "MULTISELECT"),
)
sealed class VasuQuestion(
    val type: VasuQuestionType
) {
    abstract val name: String
    abstract val ophKey: OphQuestionKey?
    abstract val info: String
    abstract fun equalsIgnoringValue(question: VasuQuestion?): Boolean

    data class TextQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        val multiline: Boolean,
        val value: String
    ) : VasuQuestion(VasuQuestionType.TEXT) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is TextQuestion && question.copy(value = this.value) == this
        }
    }

    data class CheckboxQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        val value: Boolean
    ) : VasuQuestion(VasuQuestionType.CHECKBOX) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is CheckboxQuestion && question.copy(value = this.value) == this
        }
    }

    data class RadioGroupQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        val options: List<QuestionOption>,
        val value: String?
    ) : VasuQuestion(VasuQuestionType.RADIO_GROUP) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is RadioGroupQuestion && question.copy(value = this.value) == this
        }
    }

    data class MultiSelectQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        val options: List<QuestionOption>,
        val minSelections: Int,
        val maxSelections: Int?,
        val value: List<String>
    ) : VasuQuestion(VasuQuestionType.MULTISELECT) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is MultiSelectQuestion && question.copy(value = this.value) == this
        }
    }
}

data class QuestionOption(
    val key: String,
    val name: String
)

enum class VasuQuestionType {
    TEXT,
    CHECKBOX,
    RADIO_GROUP,
    MULTISELECT
}

@Json
data class AuthorsContent(
    val primaryAuthor: AuthorInfo,
    val otherAuthors: List<AuthorInfo>
)

data class AuthorInfo(
    val name: String = "",
    val title: String = "",
    val phone: String = ""
)

@Json
data class VasuDiscussionContent(
    val discussionDate: LocalDate? = null,
    val participants: String = "",
    val guardianViewsAndCollaboration: String = ""
)

@Json
data class EvaluationDiscussionContent(
    val discussionDate: LocalDate? = null,
    val participants: String = "",
    val guardianViewsAndCollaboration: String = "",
    val evaluation: String = ""
)
