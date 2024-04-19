// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.europeHelsinki
import java.time.LocalDate
import java.util.UUID
import org.jdbi.v3.json.Json

@ConstList("curriculumTypes")
enum class CurriculumType : DatabaseEnum {
    DAYCARE,
    PRESCHOOL;

    override val sqlType: String = "curriculum_type"
}

enum class VasuDocumentEventType : DatabaseEnum {
    PUBLISHED,
    MOVED_TO_READY,
    RETURNED_TO_READY,
    MOVED_TO_REVIEWED,
    RETURNED_TO_REVIEWED,
    MOVED_TO_CLOSED;

    override val sqlType: String = "curriculum_document_event_type"
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
    val eventType: VasuDocumentEventType,
    val createdBy: EvakaUserId
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

@ConstList("curriculumLanguages")
enum class VasuLanguage : DatabaseEnum {
    FI,
    SV;

    override val sqlType: String = "curriculum_language"
}

data class VasuDocumentSummary(
    val id: VasuDocumentId,
    val name: String,
    val modifiedAt: HelsinkiDateTime,
    val events: List<VasuDocumentEvent> = listOf(),
    val publishedAt: HelsinkiDateTime?,
    val guardiansThatHaveGivenPermissionToShare: List<PersonId>,
    val type: CurriculumType
) {
    val documentState: VasuDocumentState
        get() = getStateFromEvents(events)
}

data class VasuDocument(
    val id: VasuDocumentId,
    val modifiedAt: HelsinkiDateTime,
    val templateId: VasuTemplateId,
    val templateName: String,
    val templateRange: FiniteDateRange,
    val type: CurriculumType,
    val language: VasuLanguage,
    @Json val events: List<VasuDocumentEvent> = listOf(),
    @Json val basics: VasuBasics,
    @Json val content: VasuContent,
    val publishedAt: HelsinkiDateTime?
) {
    val documentState: VasuDocumentState
        get() = getStateFromEvents(events)

    fun redact() = this.copy(content = this.content.redact())
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

@Json data class ChildLanguage(val nativeLanguage: String, val languageSpokenAtHome: String)

@Json
data class VasuContent(
    val hasDynamicFirstSection: Boolean? = false,
    val sections: List<VasuSection>
) {
    fun matchesStructurally(content: VasuContent): Boolean =
        this.sections.size == content.sections.size &&
            this.sections.withIndex().all { (index, section) ->
                section.matchesStructurally(content.sections.getOrNull(index))
            }

    fun followupEntriesMissing(content: VasuContent): Boolean =
        content.sections.withIndex().any { section ->
            section.value.questions.withIndex().any { question ->
                if (question.value.type == VasuQuestionType.FOLLOWUP) {
                    val followup = question.value as VasuQuestion.Followup
                    val storedFollowup =
                        this.sections[section.index].questions[question.index]
                            as VasuQuestion.Followup

                    followup.value.size < storedFollowup.value.size
                } else {
                    false
                }
            }
        }

    fun redact() = this.copy(sections = sections.map { it.redact() })

    fun mapQuestions(
        transform: (question: VasuQuestion, sectionIndex: Int, questionIndex: Int) -> VasuQuestion
    ) =
        this.copy(
            sections =
                this.sections.mapIndexed { sectionIndex, section ->
                    section.copy(
                        questions =
                            section.questions.mapIndexed { questionIndex, question ->
                                transform(question, sectionIndex, questionIndex)
                            }
                    )
                }
        )
}

data class VasuSection(
    val name: String,
    val questions: List<VasuQuestion>,
    val hideBeforeReady: Boolean = false
) {
    fun matchesStructurally(section: VasuSection?): Boolean =
        section != null &&
            section.name == this.name &&
            section.questions.size == this.questions.size &&
            this.questions.withIndex().all { (index, currentQuestion) ->
                val newQuestion = section.questions[index]
                val equals = currentQuestion.equalsIgnoringValue(newQuestion)
                val isValid = newQuestion.isValid(section)
                equals && isValid
            }

    fun redact() = this.copy(questions = this.questions.map { it.redact() })
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = VasuQuestion.TextQuestion::class, name = "TEXT"),
    JsonSubTypes.Type(value = VasuQuestion.CheckboxQuestion::class, name = "CHECKBOX"),
    JsonSubTypes.Type(value = VasuQuestion.RadioGroupQuestion::class, name = "RADIO_GROUP"),
    JsonSubTypes.Type(value = VasuQuestion.MultiSelectQuestion::class, name = "MULTISELECT"),
    JsonSubTypes.Type(value = VasuQuestion.MultiField::class, name = "MULTI_FIELD"),
    JsonSubTypes.Type(value = VasuQuestion.MultiFieldList::class, name = "MULTI_FIELD_LIST"),
    JsonSubTypes.Type(value = VasuQuestion.DateQuestion::class, name = "DATE"),
    JsonSubTypes.Type(value = VasuQuestion.Followup::class, name = "FOLLOWUP"),
    JsonSubTypes.Type(value = VasuQuestion.Paragraph::class, name = "PARAGRAPH"),
    JsonSubTypes.Type(
        value = VasuQuestion.StaticInfoSubSection::class,
        name = "STATIC_INFO_SUBSECTION"
    )
)
sealed class VasuQuestion(val type: VasuQuestionType) {
    abstract val name: String
    abstract val ophKey: OphQuestionKey?
    abstract val info: String
    abstract val id: String?
    abstract val dependsOn: List<String>?

    abstract fun equalsIgnoringValue(question: VasuQuestion?): Boolean

    open fun isValid(section: VasuSection): Boolean {
        return dependsOn?.all { dep -> section.questions.any { it.id == dep && it != this } }
            ?: true
    }

    open fun redact(): VasuQuestion = this

    data class StaticInfoSubSection(
        override val name: String = "static_subsection",
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        override val id: String? = null,
        override val dependsOn: List<String>? = null
    ) : VasuQuestion(VasuQuestionType.STATIC_INFO_SUBSECTION) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return true
        }
    }

    data class TextQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        override val id: String? = null,
        override val dependsOn: List<String>? = null,
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
        override val id: String? = null,
        override val dependsOn: List<String>? = null,
        val value: Boolean,
        val label: String? = null,
        val notNumbered: Boolean? = false
    ) : VasuQuestion(VasuQuestionType.CHECKBOX) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is CheckboxQuestion && question.copy(value = this.value) == this
        }
    }

    data class RadioGroupQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        override val id: String? = null,
        override val dependsOn: List<String>? = null,
        val options: List<QuestionOption>,
        val value: String?,
        val dateRange: FiniteDateRange? = null
    ) : VasuQuestion(VasuQuestionType.RADIO_GROUP) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is RadioGroupQuestion &&
                question.copy(value = this.value, dateRange = this.dateRange) == this
        }

        override fun isValid(section: VasuSection): Boolean {
            if (!super.isValid(section)) return false

            if (value == null) return true

            val option = options.find { it.key == value } ?: return false
            return !option.isIntervention && (!option.dateRange || dateRange != null)
        }
    }

    data class MultiSelectQuestion(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        override val id: String? = null,
        override val dependsOn: List<String>? = null,
        val options: List<QuestionOption>,
        val minSelections: Int,
        val maxSelections: Int?,
        val value: List<String>,
        val textValue: Map<String, String> = mapOf(),
        val dateValue: Map<String, LocalDate>? = mapOf(),
        val dateRangeValue: Map<String, FiniteDateRange>? = mapOf()
    ) : VasuQuestion(VasuQuestionType.MULTISELECT) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is MultiSelectQuestion &&
                question.copy(
                    value = this.value,
                    textValue = this.textValue,
                    dateValue = this.dateValue,
                    dateRangeValue = this.dateRangeValue
                ) == this
        }

        override fun isValid(section: VasuSection): Boolean {
            if (!super.isValid(section)) return false

            if (maxSelections != null && value.size > maxSelections) return false

            val selectedOptionsExist =
                this.value.all { v -> options.any { opt -> !opt.isIntervention && v == opt.key } }
            val datesAreDateOptions =
                this.dateValue?.keys?.all { key -> options.find { it.key == key }?.date == true }
                    ?: true
            val dateRangesAreDateRangeOptions =
                this.dateRangeValue?.keys?.all { key ->
                    options.find { it.key == key }?.dateRange == true
                } ?: true
            return selectedOptionsExist && datesAreDateOptions && dateRangesAreDateRangeOptions
        }
    }

    data class MultiField(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        override val id: String? = null,
        override val dependsOn: List<String>? = null,
        val keys: List<Field>,
        val value: List<String>,
        val separateRows: Boolean = false
    ) : VasuQuestion(VasuQuestionType.MULTI_FIELD) {
        init {
            check(keys.size == value.size) {
                "MultiField keys ($keys) and value ($value) sizes do not match"
            }
        }

        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is MultiField && question.copy(value = this.value) == this
        }
    }

    data class MultiFieldList(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        override val id: String? = null,
        override val dependsOn: List<String>? = null,
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
        override val id: String? = null,
        override val dependsOn: List<String>? = null,
        val trackedInEvents: Boolean,
        val nameInEvents: String = "",
        val value: LocalDate?
    ) : VasuQuestion(VasuQuestionType.DATE) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is DateQuestion && question.copy(value = this.value) == this
        }
    }

    data class Followup(
        override val name: String,
        override val ophKey: OphQuestionKey? = null,
        override val info: String = "",
        override val id: String? = null,
        override val dependsOn: List<String>? = null,
        val title: String = "",
        val value: List<FollowupEntry>,
        val continuesNumbering: Boolean = false
    ) : VasuQuestion(VasuQuestionType.FOLLOWUP) {
        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question is Followup && question.copy(value = this.value) == this
        }

        override fun redact(): VasuQuestion =
            this.copy(
                value =
                    this.value.map {
                        it.copy(
                            authorName = null,
                            authorId = null,
                            edited = null,
                            createdDate = null
                        )
                    }
            )

        fun withEmployeeNames(nameMap: Map<EmployeeId, String>) =
            this.copy(
                value =
                    this.value.map { entry ->
                        entry.copy(
                            authorName = entry.authorId?.let { nameMap[it] },
                            edited =
                                entry.edited?.let { edited ->
                                    edited.copy(editorName = edited.editorId?.let { nameMap[it] })
                                }
                        )
                    }
            )
    }

    data class Paragraph(val title: String, val paragraph: String) :
        VasuQuestion(VasuQuestionType.PARAGRAPH) {
        override val name = ""
        override val info = ""
        override val ophKey: OphQuestionKey? = null
        override val id: String? = null
        override val dependsOn: List<String>? = null

        override fun equalsIgnoringValue(question: VasuQuestion?): Boolean {
            return question == this
        }
    }
}

data class QuestionOption(
    val key: String,
    val name: String,
    val textAnswer: Boolean = false,
    val dateRange: Boolean = false,
    val date: Boolean = false,
    val isIntervention: Boolean = false,
    val info: String = "",
    val subText: String? = null
)

data class Field(val name: String, val info: String? = null)

enum class VasuQuestionType {
    TEXT,
    CHECKBOX,
    RADIO_GROUP,
    MULTISELECT,
    MULTI_FIELD,
    MULTI_FIELD_LIST,
    DATE,
    FOLLOWUP,
    PARAGRAPH,
    STATIC_INFO_SUBSECTION
}

@Json
data class FollowupEntry(
    val id: UUID = UUID.randomUUID(),
    val date: LocalDate = LocalDate.now(europeHelsinki),
    val authorName: String? = null,
    val text: String = "",
    val authorId: EmployeeId? = null,
    val edited: FollowupEntryEditDetails? = null,
    val createdDate: HelsinkiDateTime? = HelsinkiDateTime.now()
)

@Json
data class FollowupEntryEditDetails(
    val editedAt: LocalDate = LocalDate.now(europeHelsinki),
    val editorId: EmployeeId? = null,
    val editorName: String? = null
)

/** Returns true if an email notification needs to be sent */
fun updateVasuDocumentState(
    tx: Database.Transaction,
    now: HelsinkiDateTime,
    createdBy: EvakaUserId,
    id: VasuDocumentId,
    eventType: VasuDocumentEventType
): Boolean {
    val events =
        if (
            eventType in
                listOf(
                    VasuDocumentEventType.MOVED_TO_READY,
                    VasuDocumentEventType.MOVED_TO_REVIEWED
                )
        ) {
            listOf(VasuDocumentEventType.PUBLISHED, eventType)
        } else {
            listOf(eventType)
        }

    val document =
        tx.getVasuDocumentMaster(now.toLocalDate(), id) ?: throw NotFound("Vasu was not found")

    val currentState = document.documentState
    events.forEach {
        when (it) {
                VasuDocumentEventType.PUBLISHED -> currentState != VasuDocumentState.CLOSED
                VasuDocumentEventType.MOVED_TO_READY -> currentState == VasuDocumentState.DRAFT
                VasuDocumentEventType.RETURNED_TO_READY ->
                    currentState == VasuDocumentState.REVIEWED
                VasuDocumentEventType.MOVED_TO_REVIEWED -> currentState == VasuDocumentState.READY
                VasuDocumentEventType.RETURNED_TO_REVIEWED ->
                    currentState == VasuDocumentState.CLOSED
                VasuDocumentEventType.MOVED_TO_CLOSED -> true
            }
            .exhaust()
            .let { valid -> if (!valid) throw Conflict("Invalid state transition") }
    }

    if (events.contains(VasuDocumentEventType.PUBLISHED)) {
        tx.publishVasuDocument(now, id)
    }

    if (events.contains(VasuDocumentEventType.MOVED_TO_CLOSED)) {
        tx.freezeVasuPlacements(now.toLocalDate(), id)
    }

    events.forEach {
        tx.insertVasuDocumentEvent(documentId = id, eventType = it, createdBy = createdBy)
    }

    // An email notification needs to be sent if the document was published
    return events.contains(VasuDocumentEventType.PUBLISHED)
}

fun closeVasusWithExpiredTemplate(tx: Database.Transaction, now: HelsinkiDateTime) {
    tx.getOpenVasusWithExpiredTemplate(now.toLocalDate()).forEach { documentId ->
        updateVasuDocumentState(
            tx,
            now,
            AuthenticatedUser.SystemInternalUser.evakaUserId,
            documentId,
            VasuDocumentEventType.MOVED_TO_CLOSED
        )
    }
}
