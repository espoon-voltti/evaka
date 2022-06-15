// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.ForceCodeGenType
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.europeHelsinki
import org.jdbi.v3.json.Json
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

enum class CurriculumType {
    DAYCARE,
    PRESCHOOL;
}

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
    @ForceCodeGenType(OffsetDateTime::class)
    val created: HelsinkiDateTime,
    val eventType: VasuDocumentEventType
)

private fun getStateFromEvents(events: List<VasuDocumentEvent>): VasuDocumentState {
    return events.fold(VasuDocumentState.DRAFT) { acc, event ->
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

enum class VasuLanguage {
    FI,
    SV
}

data class VasuDocumentSummary(
    val id: VasuDocumentId,
    val name: String,
    @ForceCodeGenType(OffsetDateTime::class)
    val modifiedAt: HelsinkiDateTime,
    val events: List<VasuDocumentEvent> = listOf(),
    @ForceCodeGenType(OffsetDateTime::class)
    val publishedAt: HelsinkiDateTime?,
    val guardiansThatHaveGivenPermissionToShare: List<PersonId>
) {
    val documentState: VasuDocumentState
        get() = getStateFromEvents(events)
}

data class VasuDocument(
    val id: VasuDocumentId,
    @ForceCodeGenType(OffsetDateTime::class)
    val modifiedAt: HelsinkiDateTime,
    val templateName: String,
    val templateRange: FiniteDateRange,
    val type: CurriculumType,
    val language: VasuLanguage,
    @Json
    val events: List<VasuDocumentEvent> = listOf(),
    @Json
    val basics: VasuBasics,
    @Json
    val content: VasuContent
) {
    val documentState: VasuDocumentState
        get() = getStateFromEvents(events)
}

@Json
data class VasuBasics(
    val child: VasuChild,
    val guardians: List<VasuGuardian>,
    val placements: List<VasuPlacement>?,
    val childLanguage: ChildLanguage?
)

@Json
data class VasuChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate
)

@Json
data class VasuGuardian(
    val id: PersonId,
    val firstName: String,
    val lastName: String,
    val hasGivenPermissionToShare: Boolean = false
)

@Json
data class VasuPlacement(
    val unitId: DaycareId,
    val unitName: String,
    val groupId: GroupId,
    val groupName: String,
    val range: FiniteDateRange
)

@Json
data class ChildLanguage(
    val nativeLanguage: String,
    val languageSpokenAtHome: String
)

@Json
data class VasuContent(
    val sections: List<VasuSection>
) {
    fun followupEntries(): Sequence<FollowupEntry> = this.sections.asSequence().flatMap { section ->
        section.questions.asSequence().flatMap { question ->
            when (question.type) {
                VasuQuestionType.FOLLOWUP -> (question as VasuQuestion.Followup).value.asSequence()
                else -> emptySequence()
            }
        }
    }
    fun matchesStructurally(content: VasuContent): Boolean =
        this.sections.size == content.sections.size && this.sections.withIndex().all { (index, section) ->
            section.matchesStructurally(content.sections.getOrNull(index))
        }

    fun containsModifiedFollowupEntries(content: VasuContent): Boolean {
        content.sections.forEach { section ->
            section.questions.forEach { question ->
                if (question.type === VasuQuestionType.FOLLOWUP) {
                    (question as VasuQuestion.Followup).value.forEach { entry ->
                        val thisEntry = this.followupEntries().find { it.id == entry.id }
                        if (thisEntry != null && thisEntry != entry)
                            return true
                    }
                }
            }
        }
        return false
    }

    /*
     * returns a copy of this VasuContent where the followup entry has been updated with the text and editor metadata
     */
    fun editFollowupEntry(entryId: UUID, evakaClock: EvakaClock, editor: Employee?, text: String): VasuContent {
        return this.copy(
            sections = this.sections.map { section ->
                section.copy(
                    questions = section.questions.map { question ->
                        when (question) {
                            is VasuQuestion.Followup -> question.copy(
                                value = question.value.map { entry ->
                                    if (entry.id == entryId) {
                                        entry.copy(
                                            text = text,
                                            edited = FollowupEntryEditDetails(
                                                editedAt = evakaClock.today(),
                                                editorId = editor?.id,
                                                editorName = "${editor?.firstName} ${editor?.lastName}"
                                            )
                                        )
                                    } else {
                                        entry.copy()
                                    }
                                }
                            )
                            else -> question
                        }
                    }
                )
            }
        )
    }
}

data class VasuSection(
    val name: String,
    val questions: List<VasuQuestion>,
    val hideBeforeReady: Boolean = false
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
    JsonSubTypes.Type(value = VasuQuestion.MultiField::class, name = "MULTI_FIELD"),
    JsonSubTypes.Type(value = VasuQuestion.MultiFieldList::class, name = "MULTI_FIELD_LIST"),
    JsonSubTypes.Type(value = VasuQuestion.DateQuestion::class, name = "DATE"),
    JsonSubTypes.Type(value = VasuQuestion.Followup::class, name = "FOLLOWUP"),
    JsonSubTypes.Type(value = VasuQuestion.Paragraph::class, name = "PARAGRAPH")
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
        val value: List<String>,
        val textValue: Map<String, String> = mapOf()
    ) : VasuQuestion(VasuQuestionType.MULTISELECT) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is MultiSelectQuestion && question.copy(value = this.value, textValue = this.textValue) == this
        }
    }

    data class MultiField(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        val keys: List<Field>,
        val value: List<String>
    ) : VasuQuestion(VasuQuestionType.MULTI_FIELD) {
        init {
            check(keys.size == value.size) { "MultiField keys ($keys) and value ($value) sizes do not match" }
        }

        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is MultiField && question.copy(value = this.value) == this
        }
    }

    data class MultiFieldList(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        val keys: List<Field>,
        val value: List<List<String>>
    ) : VasuQuestion(VasuQuestionType.MULTI_FIELD_LIST) {
        init {
            check(value.all { it.size == keys.size }) {
                "MultiFieldList keys ($keys) and value ($value) sizes do not match"
            }
        }

        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is MultiFieldList && question.copy(value = this.value) == this
        }
    }

    data class DateQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        val trackedInEvents: Boolean,
        val nameInEvents: String = "",
        val value: LocalDate?,
    ) : VasuQuestion(VasuQuestionType.DATE) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is DateQuestion && question.copy(value = this.value) == this
        }
    }

    data class Followup(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        val title: String = "",
        val value: List<FollowupEntry>
    ) : VasuQuestion(VasuQuestionType.FOLLOWUP) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is Followup && question.copy(value = this.value) == this
        }
    }

    data class Paragraph(
        val title: String,
        val paragraph: String
    ) : VasuQuestion(VasuQuestionType.PARAGRAPH) {
        override val name = ""
        override val info = ""
        override val ophKey: OphQuestionKey? = null

        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question == this
        }
    }
}

data class QuestionOption(
    val key: String,
    val name: String,
    val textAnswer: Boolean = false,
)

data class Field(val name: String)

enum class VasuQuestionType {
    TEXT,
    CHECKBOX,
    RADIO_GROUP,
    MULTISELECT,
    MULTI_FIELD,
    MULTI_FIELD_LIST,
    DATE,
    FOLLOWUP,
    PARAGRAPH
}

@Json
data class FollowupEntry(
    val date: LocalDate = LocalDate.now(europeHelsinki),
    val authorName: String = "",
    val text: String = "",
    val id: UUID = UUID.randomUUID(),
    val authorId: UUID? = null,
    val edited: FollowupEntryEditDetails? = null
)

@Json
data class FollowupEntryEditDetails(
    val editedAt: LocalDate = LocalDate.now(europeHelsinki),
    val editorName: String = "",
    val editorId: EmployeeId? = null
)
