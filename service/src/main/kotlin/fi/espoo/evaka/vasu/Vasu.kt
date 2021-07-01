package fi.espoo.evaka.vasu

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
    val id: UUID,
    val name: String,
    val modifiedAt: HelsinkiDateTime,
    val events: List<VasuDocumentEvent> = listOf(),
)

data class VasuDocument(
    val id: UUID,
    val modifiedAt: HelsinkiDateTime,
    @Json
    val events: List<VasuDocumentEvent> = listOf(),
    @Nested("child")
    val child: VasuDocumentChild,
    val templateName: String,
    @Json
    val content: VasuContent,
    val vasuDiscussionDate: LocalDate? = null,
    val evaluationDiscussionDate: LocalDate? = null,
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
)

data class VasuSection(
    val name: String,
    val questions: List<VasuQuestion>
)

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

    data class TextQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        val multiline: Boolean,
        val value: String
    ) : VasuQuestion(VasuQuestionType.TEXT)

    data class CheckboxQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        val value: Boolean
    ) : VasuQuestion(VasuQuestionType.CHECKBOX)

    data class RadioGroupQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        val options: List<QuestionOption>,
        val value: String?
    ) : VasuQuestion(VasuQuestionType.RADIO_GROUP)

    data class MultiSelectQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        val options: List<QuestionOption>,
        val minSelections: Int,
        val maxSelections: Int?,
        val value: List<String>
    ) : VasuQuestion(VasuQuestionType.MULTISELECT)
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
