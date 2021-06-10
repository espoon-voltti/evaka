package fi.espoo.evaka.vasu

import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.json.Json
import java.util.UUID

data class VasuTemplate(
    val id: UUID,
    val name: String,
    val valid: DateRange,
    val content: VasuContent
)

data class VasuTemplateSummary(
    val id: UUID,
    val name: String,
    val valid: DateRange
)

data class VasuDocument(
    val id: UUID,
    val content: VasuContent
)

@Json
data class VasuContent(
    val sections: List<VasuSection>
)

data class VasuSection(
    val name: String,
    val questions: List<VasuQuestion>
)

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, property = "type")
sealed class VasuQuestion(
    val type: VasuQuestionType,
) {
    abstract val name: String

    data class TextQuestion(
        override val name: String,
        val multiline: Boolean,
        val value: String
    ) : VasuQuestion(VasuQuestionType.TEXT)

    data class CheckboxQuestion(
        override val name: String,
        val value: Boolean
    ) : VasuQuestion(VasuQuestionType.CHECKBOX)

    data class RadioGroupQuestion(
        override val name: String,
        val optionNames: List<String>,
        val value: Int?
    ) : VasuQuestion(VasuQuestionType.RADIO_GROUP)

    data class MultiSelectQuestion(
        override val name: String,
        val optionNames: List<String>,
        val minSelections: Int,
        val maxSelections: Int?,
        val value: List<Int>
    ) : VasuQuestion(VasuQuestionType.MULTISELECT)
}

enum class VasuQuestionType {
    TEXT,
    CHECKBOX,
    RADIO_GROUP,
    MULTISELECT
}
