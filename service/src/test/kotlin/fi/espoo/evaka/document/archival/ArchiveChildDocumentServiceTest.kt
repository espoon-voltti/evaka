// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.document.*
import fi.espoo.evaka.document.archival.ArchiveChildDocumentService.Companion.createDocumentMetadata
import fi.espoo.evaka.document.archival.ArchiveChildDocumentService.Companion.marshalMetadata
import fi.espoo.evaka.document.childdocument.*
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.process.ArchivedProcess
import fi.espoo.evaka.process.ArchivedProcessHistoryRow
import fi.espoo.evaka.process.ArchivedProcessState
import fi.espoo.evaka.process.ProcessMetadataController
import fi.espoo.evaka.process.ProcessMetadataController.DocumentMetadata
import fi.espoo.evaka.shared.*
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.user.EvakaUser
import fi.espoo.evaka.user.EvakaUserType
import java.io.StringWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import java.io.File

fun prettyPrintXml(xml: String): String {
    val transformerFactory = TransformerFactory.newInstance()
    val transformer =
        transformerFactory.newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }
    val source = StreamSource(xml.reader())
    val result = StringWriter()
    transformer.transform(source, StreamResult(result))
    return result.toString()
}

class ArchiveChildDocumentServiceTest {
    private val documentId =
        ChildDocumentId(UUID.fromString("c3cc95f8-f045-11ef-9114-87ea771c5c89"))
    private val templateId = UUID.fromString("c15d1888-f045-11ef-9114-c3ed20a5c03d")
    private val childId = PersonId(UUID.fromString("5a4f3ccc-5270-4d28-bd93-d355182b6768"))
    private val processId =
        ArchivedProcessId(UUID.fromString("c3c73bb2-f045-11ef-9114-03e2ccf106e6"))
    private val userId = UUID.fromString("d71daacc-18e1-4605-8847-677469203e27")
    private val questionId = "4eb862da-ac2c-412e-94fb-f2d7d5aad687"

    private fun validateXmlAgainstSchema(xml: String): List<SAXException> {
        val schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
        val schemaFile = File("sarmamodel/src/main/schema/yleinen/yleinen-1.0.xsd")
        val schema = schemaFactory.newSchema(StreamSource(schemaFile))
        val validator = schema.newValidator()
        val source = StreamSource(xml.reader())
        val errors = mutableListOf<SAXException>()
        validator.errorHandler =
            object : org.xml.sax.ErrorHandler {
                override fun warning(exception: SAXParseException) {
                    errors.add(exception)
                }

                override fun error(exception: SAXParseException) {
                    errors.add(exception)
                }

                override fun fatalError(exception: SAXParseException) {
                    errors.add(exception)
                }
            }
        try {
            validator.validate(source)
        } catch (e: SAXException) {
            errors.add(e)
        }
        return errors
    }

    @Test
    fun `createDocumentMetadata creates correct metadata for document`() {
        val document =
            ChildDocumentDetailsWithSsn(
                id = documentId,
                status = DocumentStatus.COMPLETED,
                publishedAt = HelsinkiDateTime.of(LocalDateTime.parse("2023-02-01T12:10:00")),
                archivedAt = null,
                pdfAvailable = true,
                content =
                    DocumentContent(
                        answers =
                            listOf(
                                AnsweredQuestion.TextAnswer(
                                    questionId = questionId,
                                    answer = "Jonkin sortin vastaus",
                                )
                            )
                    ),
                publishedContent =
                    DocumentContent(
                        answers =
                            listOf(
                                AnsweredQuestion.TextAnswer(
                                    questionId = questionId,
                                    answer = "Jonkin sortin vastaus",
                                )
                            )
                    ),
                child =
                    ChildBasicsWithSsn(
                        id = childId,
                        firstName = "Kaarina Veera Nelli",
                        lastName = "Karhula",
                        dateOfBirth = LocalDate.parse("2016-06-06"),
                        socialSecurityNumber = "160616A978U",
                    ),
                template =
                    DocumentTemplate(
                        id = DocumentTemplateId(templateId),
                        name = "VASU 2022-2023",
                        type = DocumentType.VASU,
                        placementTypes = setOf(PlacementType.PRESCHOOL),
                        language = OfficialLanguage.FI,
                        confidential = true,
                        legalBasis = "",
                        validity = DateRange(start = LocalDate.parse("2022-08-01"), end = null),
                        published = true,
                        processDefinitionNumber = null,
                        archiveDurationMonths = null,
                        content =
                            DocumentTemplateContent(
                                sections =
                                    listOf(
                                        Section(
                                            id = "8ab35a60-d243-49a2-bc99-8e0f0e15643f",
                                            label = "Eka osio",
                                            questions =
                                                listOf(
                                                    Question.TextQuestion(
                                                        id = questionId,
                                                        label = "Eka kysymys",
                                                        infoText = "",
                                                        multiline = true,
                                                    )
                                                ),
                                            infoText = "",
                                        )
                                    )
                            ),
                    ),
            )

        val documentMetadata =
            DocumentMetadata(
                documentId = documentId.raw,
                name = "VASU 2022-2023",
                createdAt = HelsinkiDateTime.of(LocalDateTime.parse("2025-02-21T13:19:49.943153")),
                createdBy =
                    EvakaUser(
                        id = EvakaUserId(userId),
                        name = "last_name_0.edflw first_name_0.edflw",
                        type = EvakaUserType.EMPLOYEE,
                    ),
                confidential = true,
                downloadPath =
                    "/employee/child-documents/child-documents/child_document_c3cc95f8-f045-11ef-9114-87ea771c5c89.pdf/pdf",
                receivedBy = ProcessMetadataController.DocumentOrigin.ELECTRONIC
            )

        val archivedProcess =
            ArchivedProcess(
                id = processId,
                processDefinitionNumber = "1234",
                year = 2023,
                number = 1,
                organization = "Espoon kaupungin esiopetus ja varhaiskasvatus",
                archiveDurationMonths = 1320,
                history =
                    listOf(
                        ArchivedProcessHistoryRow(
                            rowIndex = 1,
                            state = ArchivedProcessState.INITIAL,
                            enteredAt =
                                HelsinkiDateTime.of(LocalDateTime.parse("2023-02-01T12:10:00")),
                            enteredBy =
                                EvakaUser(
                                    id = EvakaUserId(userId),
                                    name = "last_name_0.edflw first_name_0.edflw",
                                    type = EvakaUserType.EMPLOYEE,
                                ),
                        ),
                        ArchivedProcessHistoryRow(
                            rowIndex = 2,
                            state = ArchivedProcessState.COMPLETED,
                            enteredAt =
                                HelsinkiDateTime.of(LocalDateTime.parse("2023-02-01T12:10:00")),
                            enteredBy =
                                EvakaUser(
                                    id = EvakaUserId(userId),
                                    name = "last_name_0.edflw first_name_0.edflw",
                                    type = EvakaUserType.EMPLOYEE,
                                ),
                        ),
                    ),
            )

        val filename = "child_document_c3cc95f8-f045-11ef-9114-87ea771c5c89.pdf"

        val metadata = createDocumentMetadata(document, documentMetadata, archivedProcess, filename)

        val metadataXml = marshalMetadata(metadata)

        val prettyMetadataXml = prettyPrintXml(metadataXml)
        //println(prettyMetadataXml)

        // Validate XML against schema
        val validationErrors = validateXmlAgainstSchema(prettyMetadataXml)
        assertThat(validationErrors)
            .withFailMessage("XML validation errors: %s", validationErrors.joinToString("\n"))
            .isEmpty()
    }
}
