// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.export

import evaka.core.document.CheckboxGroupQuestionOption
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.Question.CheckboxGroupQuestion
import evaka.core.document.Question.CheckboxQuestion
import evaka.core.document.Question.DateQuestion
import evaka.core.document.Question.GroupedTextFieldsQuestion
import evaka.core.document.Question.RadioButtonGroupQuestion
import evaka.core.document.Question.StaticTextDisplayQuestion
import evaka.core.document.Question.TextQuestion
import evaka.core.document.QuestionType
import evaka.core.document.RadioButtonGroupQuestionOption
import evaka.core.document.Section
import evaka.core.document.childdocument.AnsweredQuestion
import evaka.core.document.childdocument.CheckboxGroupAnswerContent
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.instance.tampere.AbstractTampereIntegrationTest
import evaka.trevaka.export.ExportPreschoolChildDocumentsService
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

private val timestamp = HelsinkiDateTime.of(LocalDate.of(2025, 8, 1), LocalTime.of(5, 1))

class TampereExportPreschoolChildDocumentsServiceTest : AbstractTampereIntegrationTest() {
    @Autowired
    private lateinit var exportPreschoolChildDocumentsService: ExportPreschoolChildDocumentsService

    @Test
    fun `no template throws`() {
        val exception =
            assertThrows<IllegalStateException> {
                db.read { tx ->
                    exportPreschoolChildDocumentsService.exportPreschoolChildDocuments(
                        tx,
                        timestamp,
                        properties.bucket.export,
                    )
                }
            }
        assertEquals("Expected exactly one result, got none", exception.message)
    }

    @Test
    fun `template without documents returns empty`() {
        db.transaction { tx ->
            tx.insert(
                DevDocumentTemplate(
                    name = "Tiedonsiirto esiopetuksesta perusopetukseen 2024-2025",
                    validity = DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 7, 31)),
                    content = DocumentTemplateContent(sections = emptyList()),
                )
            )
        }

        val (bucket, key) =
            db.read { tx ->
                exportPreschoolChildDocumentsService.exportPreschoolChildDocuments(
                    tx,
                    timestamp,
                    properties.bucket.export,
                )
            }

        val (data, contentType) =
            getS3Object(bucket, key).use {
                it.readAllBytes().toString(Charsets.UTF_8) to it.response().contentType()
            }
        assertEquals("[]", data)
        assertEquals("application/json", contentType)
    }

    @Test
    fun `latest template is selected`() {
        db.transaction { tx ->
            tx.insert(
                DevDocumentTemplate(
                    name = "Tiedonsiirto esiopetuksesta perusopetukseen 2023-2024",
                    validity = DateRange(LocalDate.of(2023, 8, 1), LocalDate.of(2024, 7, 31)),
                    content = DocumentTemplateContent(sections = emptyList()),
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    name = "Tiedonsiirto esiopetuksesta perusopetukseen 2024-2025",
                    validity = DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 7, 31)),
                    content = DocumentTemplateContent(sections = emptyList()),
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    name = "Tiedonsiirto esiopetuksesta perusopetukseen 2025-2026",
                    validity = DateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 7, 31)),
                    content = DocumentTemplateContent(sections = emptyList()),
                )
            )
            tx.insert(
                DevDocumentTemplate(
                    name = "Varhaiskasvatussuunnitelma 2024-2025",
                    validity = DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 7, 31)),
                    content = DocumentTemplateContent(sections = emptyList()),
                )
            )
        }

        val (bucket, key) =
            db.read { tx ->
                exportPreschoolChildDocumentsService.exportPreschoolChildDocuments(
                    tx,
                    timestamp,
                    properties.bucket.export,
                )
            }

        assertEquals("reporting/preschool/837_evaka_child_documents_2025-08-01.json", key)
        val (data, contentType) =
            getS3Object(bucket, key).use {
                it.readAllBytes().toString(Charsets.UTF_8) to it.response().contentType()
            }
        assertEquals("[]", data)
        assertEquals("application/json", contentType)
    }

    @ParameterizedTest
    @EnumSource(QuestionType::class)
    fun `question type works`(questionType: QuestionType) {
        val questionId = "1.1"
        val answer =
            when (questionType) {
                QuestionType.TEXT -> AnsweredQuestion.TextAnswer(questionId, answer = "vastaus")

                QuestionType.CHECKBOX -> AnsweredQuestion.CheckboxAnswer(questionId, answer = true)

                QuestionType.CHECKBOX_GROUP ->
                    AnsweredQuestion.CheckboxGroupAnswer(
                        questionId,
                        answer =
                            listOf(
                                CheckboxGroupAnswerContent(optionId = "1"),
                                CheckboxGroupAnswerContent(optionId = "2"),
                            ),
                    )

                QuestionType.RADIO_BUTTON_GROUP ->
                    AnsweredQuestion.RadioButtonGroupAnswer(questionId, answer = "1")

                QuestionType.STATIC_TEXT_DISPLAY ->
                    AnsweredQuestion.StaticTextDisplayAnswer(questionId, answer = null)

                QuestionType.DATE ->
                    AnsweredQuestion.DateAnswer(questionId, answer = LocalDate.of(2025, 1, 2))

                QuestionType.GROUPED_TEXT_FIELDS ->
                    AnsweredQuestion.GroupedTextFieldsAnswer(
                        questionId,
                        answer =
                            listOf(
                                listOf(
                                    "vastaus rivi 1 sarake 1",
                                    "vastaus rivi 1 sarake 2",
                                    "vastaus rivi 1 sarake 3",
                                ),
                                listOf(
                                    "vastaus rivi 2 sarake 1",
                                    "vastaus rivi 2 sarake 2",
                                    "vastaus rivi 2 sarake 3",
                                ),
                            ),
                    )
            }

        db.transaction { tx ->
            val employee = DevEmployee().also { tx.insert(it) }
            val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
            val templateId =
                tx.insert(
                    DevDocumentTemplate(
                        name = "Tiedonsiirto esiopetuksesta perusopetukseen 2024-2025",
                        validity = DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 7, 31)),
                        content =
                            DocumentTemplateContent(
                                sections =
                                    listOf(
                                        Section(
                                            id = "1",
                                            label = questionType.name,
                                            questions = listOf(answer.toQuestion("kysymys")),
                                        )
                                    )
                            ),
                    )
                )
            val content = DocumentContent(answers = listOf(answer))
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    childId = childId,
                    templateId = templateId,
                    content = content,
                    modifiedAt = timestamp,
                    modifiedBy = employee.evakaUserId,
                    contentLockedAt = timestamp,
                    contentLockedBy = null,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = timestamp,
                                createdBy = employee.evakaUserId,
                                publishedContent = content,
                            )
                        ),
                )
            )
        }

        val (bucket, key) =
            db.read { tx ->
                exportPreschoolChildDocumentsService.exportPreschoolChildDocuments(
                    tx,
                    timestamp,
                    properties.bucket.export,
                )
            }

        val (data, contentType) =
            getS3Object(bucket, key).use {
                it.readAllBytes().toString(Charsets.UTF_8) to it.response().contentType()
            }
        if (questionType == QuestionType.STATIC_TEXT_DISPLAY) {
            assertEquals("[]", data)
        } else {
            assertEquals(
                "[{\"child\": {\"oid\": null, \"last_name\": \"Person\", \"first_name\": \"Test\", \"date_of_birth\": \"1980-01-01\"}, \"document\": {\"kysymys\": ${answer.toExpectedJson()}}}]",
                data,
            )
        }
        assertEquals("application/json", contentType)
    }
}

private fun AnsweredQuestion<*>.toQuestion(label: String) =
    when (this) {
        is AnsweredQuestion.CheckboxAnswer -> CheckboxQuestion(questionId, label)

        is AnsweredQuestion.CheckboxGroupAnswer ->
            CheckboxGroupQuestion(
                questionId,
                label,
                options =
                    answer.map {
                        CheckboxGroupQuestionOption(
                            id = it.optionId,
                            label = "vastaus ${it.optionId}",
                        )
                    } +
                        listOf(
                            CheckboxGroupQuestionOption(id = "a", label = "vastaus a"),
                            CheckboxGroupQuestionOption(id = "b", label = "vastaus b"),
                            CheckboxGroupQuestionOption(id = "c", label = "vastaus c"),
                        ),
            )

        is AnsweredQuestion.DateAnswer -> DateQuestion(questionId, label)

        is AnsweredQuestion.GroupedTextFieldsAnswer ->
            GroupedTextFieldsQuestion(
                questionId,
                label,
                fieldLabels =
                    if (answer.isNotEmpty()) {
                        answer.first().mapIndexed { index, _ -> "sarake ${index + 1}" }
                    } else {
                        emptyList()
                    },
                allowMultipleRows = answer.size > 1,
            )

        is AnsweredQuestion.RadioButtonGroupAnswer ->
            RadioButtonGroupQuestion(
                questionId,
                label,
                options =
                    listOfNotNull(
                        answer?.let { RadioButtonGroupQuestionOption(it, "vastaus $it") },
                        RadioButtonGroupQuestionOption("a", "vastaus a"),
                        RadioButtonGroupQuestionOption("b", "vastaus b"),
                        RadioButtonGroupQuestionOption("c", "vastaus c"),
                    ),
            )

        is AnsweredQuestion.StaticTextDisplayAnswer -> StaticTextDisplayQuestion(questionId, label)

        is AnsweredQuestion.TextAnswer -> TextQuestion(questionId, label)
    }

private fun AnsweredQuestion<*>.toExpectedJson(): String =
    when (this) {
        is AnsweredQuestion.CheckboxAnswer -> answer.toString()

        is AnsweredQuestion.CheckboxGroupAnswer ->
            "[${answer.joinToString(", ") { "\"vastaus ${it.optionId}\"" }}]"

        is AnsweredQuestion.DateAnswer ->
            answer?.let { "\"${it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}\"" } ?: "null"

        is AnsweredQuestion.GroupedTextFieldsAnswer ->
            "${
            answer.map { row ->
                "{${
                    row.mapIndexed { index, column -> "\"sarake ${index + 1}\": \"$column\"" }.joinToString(", ")
                }}"
            }
        }"

        is AnsweredQuestion.RadioButtonGroupAnswer -> answer?.let { "\"vastaus $it\"" } ?: "null"

        is AnsweredQuestion.StaticTextDisplayAnswer -> "should not be displayed"

        is AnsweredQuestion.TextAnswer -> "\"$answer\""
    }
