// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pdfgen

import evaka.core.caseprocess.DocumentConfidentiality
import evaka.core.document.CheckboxGroupQuestionOption
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentDeletionBasis
import evaka.core.document.DocumentTemplate
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.Question
import evaka.core.document.RadioButtonGroupQuestionOption
import evaka.core.document.Section
import evaka.core.document.childdocument.AnsweredQuestion
import evaka.core.document.childdocument.CheckboxGroupAnswerContent
import evaka.core.document.childdocument.ChildBasics
import evaka.core.document.childdocument.ChildDocumentDetails
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.document.childdocument.generateChildDocumentHtml
import evaka.core.identity.ExternalIdentifier
import evaka.core.pis.service.PersonDTO
import evaka.core.pis.service.createAddressPagePdf
import evaka.core.placement.PlacementType
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.DocumentTemplateId
import evaka.core.shared.PersonId
import evaka.core.shared.config.pdfTemplateEngine
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.Rectangle
import evaka.core.shared.domain.UiLanguage
import evaka.core.shared.template.EvakaTemplateProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Test

private val logger = KotlinLogging.logger {}

private val testAdult = DevPerson(ssn = "010180-1232", dateOfBirth = LocalDate.of(1980, 1, 1))

private val guardian =
    PersonDTO(
        testAdult.id,
        null,
        ExternalIdentifier.SSN.getInstance(testAdult.ssn!!),
        false,
        "Kyösti Taavetinpoika",
        "Pöysti",
        "",
        "kyostipoysti@example.com",
        "+358914822",
        "+358914829",
        null,
        testAdult.dateOfBirth,
        null,
        "Kuusikallionrinne 26 A 4",
        "02270",
        "Espoo",
        "",
        "",
    )

class PdfGeneratorTest {
    private val pdfGenerator = PdfGenerator(EvakaTemplateProvider(), pdfTemplateEngine("espoo"))

    @Test
    fun createAddressPagePdfTest() {
        val document =
            createAddressPagePdf(
                pdfGenerator,
                LocalDate.of(2024, 1, 1),
                Rectangle.iPostWindowPosition,
                guardian,
            )

        val file = File.createTempFile("address_page_", ".pdf")
        FileOutputStream(file).use { it.write(document.bytes) }

        logger.debug { "Generated address page PDF to ${file.absolutePath}" }
    }

    @Test
    fun createChildDocumentPdf() {
        val content =
            DocumentContent(
                answers =
                    listOf(
                        AnsweredQuestion.TextAnswer(questionId = "s1q1", answer = "Ihan jees"),
                        AnsweredQuestion.TextAnswer(
                            questionId = "s1q2",
                            answer =
                                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus euismod turpis leo. \n" +
                                    "in commodo velit interdum vitae. Vivamus eu felis libero. Aliquam non pellentesque ex. \n" +
                                    "vitae suscipit libero. Ut varius turpis non faucibus iaculis. Curabitur mi mi, suscipit.\n\n" +
                                    "Quis maximus in, blandit sit amet purus. Nam aliquam pellentesque magna, eu sodales neque" +
                                    "luctus id. Mauris blandit sed enim sit amet iaculis. Praesent dapibus vehicula augue, " +
                                    "sed porta magna pulvinar at. Sed dui orci, pharetra nec orci at, ultricies semper quam.",
                        ),
                        AnsweredQuestion.CheckboxAnswer(questionId = "s2q1", answer = true),
                        AnsweredQuestion.CheckboxAnswer(questionId = "s2q2", answer = false),
                        AnsweredQuestion.StaticTextDisplayAnswer(
                            questionId = "s2q3",
                            answer = null,
                        ),
                        AnsweredQuestion.CheckboxGroupAnswer(
                            questionId = "s2q4",
                            answer =
                                listOf(
                                    CheckboxGroupAnswerContent(optionId = "2"),
                                    CheckboxGroupAnswerContent(optionId = "3", "Lohikäärme"),
                                ),
                        ),
                        AnsweredQuestion.RadioButtonGroupAnswer(questionId = "s2q6", answer = "2"),
                        AnsweredQuestion.DateAnswer(
                            questionId = "s3q1",
                            answer = LocalDate.of(2023, 5, 20),
                        ),
                        AnsweredQuestion.GroupedTextFieldsAnswer(
                            questionId = "s3q2",
                            answer =
                                listOf(
                                    listOf(
                                        "Roope",
                                        "Ankka",
                                        "Triljonääri",
                                        "roope.ankka@ankkalinna.fi",
                                        "+358 99 765 4321",
                                    )
                                ),
                        ),
                        AnsweredQuestion.GroupedTextFieldsAnswer(
                            questionId = "s3q3",
                            answer =
                                listOf(
                                    listOf("Aku Kalevi Uolevi", "Ankka", "Isä"),
                                    listOf("Hilda", "Hanhivaara", "Ilkeä äitipuoli"),
                                ),
                        ),
                    )
            )
        val document =
            ChildDocumentDetails(
                id = ChildDocumentId(UUID.randomUUID()),
                status = DocumentStatus.COMPLETED,
                publishedAt = HelsinkiDateTime.now(),
                pdfAvailable = false,
                child =
                    ChildBasics(
                        id = PersonId(UUID.randomUUID()),
                        firstName = "Tessa Tiina-Tellervo",
                        lastName = "Testaaja-Meikäläinen",
                        dateOfBirth = LocalDate.now().minusYears(4),
                    ),
                template =
                    DocumentTemplate(
                        id = DocumentTemplateId(UUID.randomUUID()),
                        type = ChildDocumentType.HOJKS,
                        placementTypes = PlacementType.entries.toSet(),
                        name = "Varhaiskasvatussuunnitelma 2023-2024",
                        language = UiLanguage.FI,
                        confidentiality = DocumentConfidentiality(100, "§ 999"),
                        legalBasis =
                            "§3.2b varhaiskasvatuslaki, varhaiskasvatuslautakunnan päätös ja määräys 11.3.2017",
                        validity =
                            DateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1)),
                        processDefinitionNumber = null,
                        archiveDurationMonths = null,
                        published = true,
                        archiveExternally = false,
                        endDecisionWhenUnitChanges = null,
                        deletionRetentionDays = 10 * 365,
                        deletionRetentionBasis = DocumentDeletionBasis.PLACEMENT_END,
                        content =
                            DocumentTemplateContent(
                                sections =
                                    listOf(
                                        Section(
                                            id = "s1",
                                            label = "Eka osio",
                                            questions =
                                                listOf(
                                                    Question.TextQuestion(
                                                        id = "s1q1",
                                                        label = "Mitä kuuluu?",
                                                    ),
                                                    Question.TextQuestion(
                                                        id = "s1q2",
                                                        label = "Kerro lisää",
                                                        multiline = true,
                                                    ),
                                                ),
                                        ),
                                        Section(
                                            id = "s2",
                                            label = "Toka osio",
                                            questions =
                                                listOf(
                                                    Question.CheckboxQuestion(
                                                        id = "s2q1",
                                                        label = "Hyväksyn käyttöehdot",
                                                    ),
                                                    Question.CheckboxQuestion(
                                                        id = "s2q2",
                                                        label = "Minulle saa lähettää mainoksia",
                                                    ),
                                                    Question.StaticTextDisplayQuestion(
                                                        id = "s2q3",
                                                        label = "Ylimääräinen väliotsikko",
                                                    ),
                                                    Question.CheckboxGroupQuestion(
                                                        id = "s2q4",
                                                        label = "Suosikkielämet",
                                                        options =
                                                            listOf(
                                                                CheckboxGroupQuestionOption(
                                                                    id = "1",
                                                                    label = "Kissa",
                                                                ),
                                                                CheckboxGroupQuestionOption(
                                                                    id = "2",
                                                                    label = "Koira",
                                                                ),
                                                                CheckboxGroupQuestionOption(
                                                                    id = "3",
                                                                    label = "Muu, mikä?",
                                                                    withText = true,
                                                                ),
                                                            ),
                                                    ),
                                                    Question.StaticTextDisplayQuestion(
                                                        id = "s2q5",
                                                        text = "Staattinen tekstikappale.",
                                                    ),
                                                    Question.RadioButtonGroupQuestion(
                                                        id = "s2q6",
                                                        label = "Laskiaispullaan kuuluu?",
                                                        options =
                                                            listOf(
                                                                RadioButtonGroupQuestionOption(
                                                                    id = "1",
                                                                    label = "Hillo",
                                                                ),
                                                                RadioButtonGroupQuestionOption(
                                                                    id = "2",
                                                                    label = "Mantelimassa",
                                                                ),
                                                            ),
                                                    ),
                                                    Question.StaticTextDisplayQuestion(
                                                        id = "s2q7",
                                                        label = "Valitusoikeus",
                                                        text =
                                                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus euismod turpis leo. " +
                                                                "in commodo velit interdum vitae. Vivamus eu felis libero. Aliquam non pellentesque ex. " +
                                                                "vitae suscipit libero. Ut varius turpis non faucibus iaculis. Curabitur mi mi, suscipit.\n\n" +
                                                                "Quis maximus in, blandit sit amet purus. Nam aliquam pellentesque magna, eu sodales neque" +
                                                                "luctus id. Mauris blandit sed enim sit amet iaculis. Praesent dapibus vehicula augue, " +
                                                                "sed porta magna pulvinar at. Sed dui orci, pharetra nec orci at, ultricies semper quam.",
                                                    ),
                                                ),
                                        ),
                                        Section(
                                            id = "s3",
                                            label = "Kolmas osio",
                                            questions =
                                                listOf(
                                                    Question.DateQuestion(
                                                        id = "s3q1",
                                                        label = "Keskustelun päivämäärä",
                                                    ),
                                                    Question.GroupedTextFieldsQuestion(
                                                        id = "s3q2",
                                                        label = "Päätöksen tekijä",
                                                        fieldLabels =
                                                            listOf(
                                                                "Etunimi",
                                                                "Sukunimi",
                                                                "Titteli",
                                                                "Sähköpoti",
                                                                "Puhelinnumero",
                                                            ),
                                                        allowMultipleRows = false,
                                                    ),
                                                    Question.GroupedTextFieldsQuestion(
                                                        id = "s3q3",
                                                        label = "Osallistujat",
                                                        fieldLabels =
                                                            listOf("Etunimi", "Sukunimi", "Rooli"),
                                                        allowMultipleRows = true,
                                                    ),
                                                ),
                                        ),
                                    )
                            ),
                    ),
                content = content,
                publishedContent = content,
                archivedAt = null,
            )

        val html = generateChildDocumentHtml(document)
        val bytes = pdfGenerator.render(html)
        val file = File.createTempFile("child_document_", ".pdf")
        FileOutputStream(file).use { it.write(bytes) }

        logger.debug { "Generated child document PDF to ${file.absolutePath}" }
    }
}
