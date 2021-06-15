package fi.espoo.evaka.vasu

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json
import java.util.UUID

enum class VasuDocumentState {
    DRAFT,
    CREATED,
    REVIEWED,
    CLOSED
}

enum class VasuLanguage {
    FI,
    SV
}

data class VasuDocumentSummary(
    val id: UUID,
    val name: String,
    val state: VasuDocumentState,
    val modifiedAt: HelsinkiDateTime,
    val publishedAt: HelsinkiDateTime? = null
)

data class VasuDocumentResponse(
    val id: UUID,
    @Nested("child")
    val child: VasuDocumentResponseChild,
    val templateName: String,
    @Json
    val content: VasuContent
)
data class VasuDocumentResponseChild(
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
