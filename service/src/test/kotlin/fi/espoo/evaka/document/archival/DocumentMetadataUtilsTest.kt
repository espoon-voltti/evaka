// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.caseprocess.*
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.childdocument.ChildBasics
import fi.espoo.evaka.document.childdocument.ChildDocumentDetails
import fi.espoo.evaka.document.childdocument.DocumentContent
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.sarma.model.PersonalDataType
import fi.espoo.evaka.shared.*
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.UiLanguage
import fi.espoo.evaka.user.EvakaUser
import fi.espoo.evaka.user.EvakaUserType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class DocumentMetadataUtilsTest {

    private val documentId =
        ChildDocumentId(UUID.fromString("c3cc95f8-f045-11ef-9114-87ea771c5c89"))
    private val templateId = UUID.fromString("c15d1888-f045-11ef-9114-c3ed20a5c03d")
    private val childId = PersonId(UUID.fromString("5a4f3ccc-5270-4d28-bd93-d355182b6768"))
    private val processId = CaseProcessId(UUID.fromString("c3c73bb2-f045-11ef-9114-03e2ccf106e6"))
    private val userId = UUID.fromString("d71daacc-18e1-4605-8847-677469203e27")

    private fun createTestDocument(
        documentType: ChildDocumentType = ChildDocumentType.VASU
    ): ChildDocumentDetails {
        return ChildDocumentDetails(
            id = documentId,
            status = DocumentStatus.COMPLETED,
            publishedAt = HelsinkiDateTime.of(LocalDateTime.parse("2023-02-01T12:10:00")),
            archivedAt = null,
            pdfAvailable = true,
            content = DocumentContent(answers = listOf()),
            publishedContent = DocumentContent(answers = listOf()),
            child =
                ChildBasics(
                    id = childId,
                    firstName = "Kaarina",
                    lastName = "Karhula",
                    dateOfBirth = LocalDate.parse("2016-06-06"),
                ),
            template =
                DocumentTemplate(
                    id = DocumentTemplateId(templateId),
                    name = "VASU 2022-2023",
                    type = documentType,
                    placementTypes = setOf(PlacementType.PRESCHOOL),
                    language = UiLanguage.FI,
                    confidentiality =
                        DocumentConfidentiality(durationYears = 10, basis = "Laki § 123"),
                    legalBasis = "",
                    validity = DateRange(start = LocalDate.parse("2022-08-01"), end = null),
                    published = true,
                    processDefinitionNumber = null,
                    archiveDurationMonths = null,
                    content = DocumentTemplateContent(sections = listOf()),
                    archiveExternally = true,
                ),
        )
    }

    private fun createTestDocumentMetadata(): DocumentMetadata {
        return DocumentMetadata(
            documentId = documentId.raw,
            name = "VASU 2022-2023",
            createdAt = HelsinkiDateTime.of(LocalDateTime.parse("2023-02-01T12:10:00")),
            createdBy =
                EvakaUser(
                    id = EvakaUserId(userId),
                    name = "Testi Testaaja",
                    type = EvakaUserType.EMPLOYEE,
                ),
            confidential = true,
            confidentiality = DocumentConfidentiality(durationYears = 10, basis = "Laki § 123"),
            downloadPath =
                "/employee/child-documents/child-documents/child_document_c3cc95f8-f045-11ef-9114-87ea771c5c89.pdf/pdf",
            receivedBy = DocumentOrigin.ELECTRONIC,
            sfiDeliveries = emptyList(),
        )
    }

    private fun createTestCaseProcess(completionDate: LocalDateTime?): CaseProcess {
        val initialHistory =
            listOf(
                CaseProcessHistoryRow(
                    rowIndex = 1,
                    state = CaseProcessState.INITIAL,
                    enteredAt = HelsinkiDateTime.of(LocalDateTime.parse("2023-02-01T12:10:00")),
                    enteredBy =
                        EvakaUser(
                            id = EvakaUserId(userId),
                            name = "Testi Testaaja",
                            type = EvakaUserType.EMPLOYEE,
                        ),
                )
            )

        val completeHistory =
            if (completionDate != null) {
                initialHistory +
                    CaseProcessHistoryRow(
                        rowIndex = 2,
                        state = CaseProcessState.COMPLETED,
                        enteredAt = HelsinkiDateTime.of(completionDate),
                        enteredBy =
                            EvakaUser(
                                id = EvakaUserId(userId),
                                name = "Testi Testaaja",
                                type = EvakaUserType.EMPLOYEE,
                            ),
                    )
            } else {
                initialHistory
            }

        return CaseProcess(
            id = processId,
            processDefinitionNumber = "1234",
            year = 2023,
            number = 1,
            organization = "Espoon kaupungin esiopetus ja varhaiskasvatus",
            archiveDurationMonths = 1320,
            history = completeHistory,
            migrated = false,
        )
    }

    @Test
    fun `createDocumentDescription sets correct document type and specifier for VASU`() {
        val document = createTestDocument(ChildDocumentType.VASU)
        val documentMetadata = createTestDocumentMetadata()
        val childIdentifier = ExternalIdentifier.SSN.getInstance("160616A978U")
        val childBirthDate = LocalDate.parse("2016-06-06")

        val result =
            createDocumentDescription(
                document,
                documentMetadata,
                null,
                childIdentifier,
                childBirthDate,
            )

        assertEquals("VASU 2022-2023", result.title)
        assertEquals("Suunnitelma", result.documentType)
        assertEquals("Varhaiskasvatussuunnitelma", result.documentTypeSpecifier)
        assertEquals(PersonalDataType.CONTAINS_SENSITIVE_PERSONAL_INFORMATION, result.personalData)
        assertEquals("fi", result.language)
        assertEquals("Kaarina", result.firstName)
        assertEquals("Karhula", result.lastName)
        assertEquals("160616A978U", result.socialSecurityNumber)
    }

    @Test
    fun `createDocumentDescription sets correct document type and specifier for LEOPS`() {
        val document = createTestDocument(ChildDocumentType.LEOPS)
        val documentMetadata = createTestDocumentMetadata()
        val childIdentifier = ExternalIdentifier.SSN.getInstance("160616A978U")
        val childBirthDate = LocalDate.parse("2016-06-06")

        val result =
            createDocumentDescription(
                document,
                documentMetadata,
                null,
                childIdentifier,
                childBirthDate,
            )

        assertEquals("Suunnitelma", result.documentType)
        assertEquals("Lapsen esiopetuksen oppimissuunnitelma LEOPS", result.documentTypeSpecifier)
    }

    @Test
    fun `createDocumentDescription sets correct document type and specifier for PEDAGOGICAL_ASSESSMENT`() {
        val document = createTestDocument(ChildDocumentType.PEDAGOGICAL_ASSESSMENT)
        val documentMetadata = createTestDocumentMetadata()
        val childIdentifier = ExternalIdentifier.SSN.getInstance("160616A978U")
        val childBirthDate = LocalDate.parse("2016-06-06")

        val result =
            createDocumentDescription(
                document,
                documentMetadata,
                null,
                childIdentifier,
                childBirthDate,
            )

        assertEquals("Arvio", result.documentType)
        assertEquals("Pedagoginen arvio", result.documentTypeSpecifier)
    }

    @Test
    fun `createDocumentDescription includes agents when caseProcess is provided`() {
        val document = createTestDocument()
        val documentMetadata = createTestDocumentMetadata()
        val caseProcess = createTestCaseProcess(null)
        val childIdentifier = ExternalIdentifier.SSN.getInstance("160616A978U")
        val childBirthDate = LocalDate.parse("2016-06-06")

        val result =
            createDocumentDescription(
                document,
                documentMetadata,
                caseProcess,
                childIdentifier,
                childBirthDate,
            )

        assertNotNull(result.agents)
        assertEquals(1, result.agents.agent.size)
        assertEquals("Testi Testaaja", result.agents.agent[0].name)
        assertEquals(
            "Espoon kaupungin esiopetus ja varhaiskasvatus",
            result.agents.agent[0].corporateName,
        )
        assertEquals("Henkilökunta", result.agents.agent[0].role)
    }

    @Test
    fun `createDocumentDescription handles non-SSN identifiers correctly`() {
        val document = createTestDocument()
        val documentMetadata = createTestDocumentMetadata()
        val childIdentifier = ExternalIdentifier.NoID
        val childBirthDate = LocalDate.parse("2016-06-06")

        val result =
            createDocumentDescription(
                document,
                documentMetadata,
                null,
                childIdentifier,
                childBirthDate,
            )

        assertNull(result.socialSecurityNumber)
    }

    @Test
    fun `createCaseFile uses completed state from history when available`() {
        val document = createTestDocument()
        val documentMetadata = createTestDocumentMetadata()
        val completionDateTime = LocalDateTime.parse("2023-02-15T14:30:00")
        val caseProcess = createTestCaseProcess(completionDateTime)

        val result = createCaseFile(documentMetadata, caseProcess, document)

        assertEquals(documentMetadata.createdAt?.asXMLGregorianCalendar(), result.caseCreated)

        assertEquals(completionDateTime.toLocalDate().toXMLGregorianCalendar(), result.caseFinished)
    }

    @Test
    fun `createCaseFile falls back to template end date when no completed state is available`() {
        val document =
            createTestDocument()
                .copy(
                    template =
                        createTestDocument()
                            .template
                            .copy(
                                validity =
                                    DateRange(
                                        start = LocalDate.parse("2022-08-01"),
                                        end = LocalDate.parse("2023-07-31"),
                                    )
                            )
                )

        val documentMetadata = createTestDocumentMetadata()

        // Create process without COMPLETED state
        val caseProcess = createTestCaseProcess(completionDate = null)

        val result = createCaseFile(documentMetadata, caseProcess, document)

        // Should use template end date
        val expectedFinishDate = document.template.validity.end?.toXMLGregorianCalendar()
        assertEquals(expectedFinishDate, result.caseFinished)
    }

    @Test
    fun `createCaseFile calculates next July 31 when no other date is available`() {
        val document =
            createTestDocument()
                .copy(
                    template =
                        createTestDocument()
                            .template
                            .copy(
                                validity =
                                    DateRange(start = LocalDate.parse("2022-08-01"), end = null)
                            )
                )

        val createdDate = LocalDateTime.parse("2023-02-01T12:10:00")
        val documentMetadata =
            createTestDocumentMetadata().copy(createdAt = HelsinkiDateTime.of(createdDate))

        val result = createCaseFile(documentMetadata, null, document)

        // For a document created on 2023-02-01, the next July 31 is 2023-07-31
        val expectedFinishDate = LocalDate.parse("2023-07-31").toXMLGregorianCalendar()
        assertEquals(expectedFinishDate, result.caseFinished)
    }

    @Test
    fun `createCaseFile calculates next July 31 correctly for dates after July 31`() {
        val document = createTestDocument()

        val createdDate = LocalDateTime.parse("2023-08-01T12:10:00")
        val documentMetadata =
            createTestDocumentMetadata().copy(createdAt = HelsinkiDateTime.of(createdDate))

        val result = createCaseFile(documentMetadata, null, document)

        // For a document created on 2023-08-01, the next July 31 is 2024-07-31
        val expectedFinishDate = LocalDate.parse("2024-07-31").toXMLGregorianCalendar()
        assertEquals(expectedFinishDate, result.caseFinished)
    }

    @Test
    fun `createDocumentMetadata sets disclosure policy timeSpan based on confidentiality duration`() {
        val document = createTestDocument()
        val confidentialityDuration = 25
        val documentMetadata =
            createTestDocumentMetadata()
                .copy(
                    confidential = true,
                    confidentiality =
                        DocumentConfidentiality(
                            durationYears = confidentialityDuration,
                            basis = "Test Basis",
                        ),
                )

        val result =
            createDocumentMetadata(
                document,
                documentMetadata,
                null,
                "test.pdf",
                ExternalIdentifier.SSN.getInstance("160616A978U"),
                LocalDate.parse("2016-06-06"),
            )

        // Verify the timeSpan in the disclosure policy matches the confidentiality duration
        val disclosurePolicy = result.standardMetadata.policies.disclosurePolicy
        val timeSpan = disclosurePolicy.policyConfiguration.rules.rule.timeSpan
        assertEquals(confidentialityDuration.toShort(), timeSpan)

        // Verify the confidentiality basis is correctly set
        val actionAnnotation =
            disclosurePolicy.policyConfiguration.rules.rule.action.actionAnnotation
        assertEquals("Test Basis", actionAnnotation)
    }

    @Test
    fun `createDocumentMetadata sets disclosure policy timeSpan to 100 when confidential but no duration specified`() {
        val document = createTestDocument()
        val documentMetadata =
            createTestDocumentMetadata()
                .copy(
                    confidential = true,
                    confidentiality = null, // No confidentiality duration specified
                )

        val result =
            createDocumentMetadata(
                document,
                documentMetadata,
                null,
                "test.pdf",
                ExternalIdentifier.SSN.getInstance("160616A978U"),
                LocalDate.parse("2016-06-06"),
            )

        // Verify the timeSpan defaults to 100 years
        val disclosurePolicy = result.standardMetadata.policies.disclosurePolicy
        val timeSpan = disclosurePolicy.policyConfiguration.rules.rule.timeSpan
        assertEquals(100.toShort(), timeSpan)

        // Verify the default basis is used
        val actionAnnotation =
            disclosurePolicy.policyConfiguration.rules.rule.action.actionAnnotation
        assertEquals("JulkL 24 § 1 mom. 32 k", actionAnnotation)
    }

    @Test
    fun `createDocumentMetadata sets disclosure policy timeSpan to 0 for non-confidential documents`() {
        val document = createTestDocument()
        val documentMetadata =
            createTestDocumentMetadata().copy(confidential = false, confidentiality = null)

        val result =
            createDocumentMetadata(
                document,
                documentMetadata,
                null,
                "test.pdf",
                ExternalIdentifier.SSN.getInstance("160616A978U"),
                LocalDate.parse("2016-06-06"),
            )

        // Verify the timeSpan is 0 for non-confidential documents
        val disclosurePolicy = result.standardMetadata.policies.disclosurePolicy
        val timeSpan = disclosurePolicy.policyConfiguration.rules.rule.timeSpan
        assertEquals(0.toShort(), timeSpan)
    }

    @Test
    fun `calculateNextJuly31 returns correct date`() {
        // Test with various dates before July 31
        val testCases =
            listOf(
                LocalDate.of(2023, 1, 1) to LocalDate.of(2023, 7, 31),
                LocalDate.of(2023, 7, 31) to LocalDate.of(2023, 7, 31),
                LocalDate.of(2023, 8, 1) to LocalDate.of(2024, 7, 31),
                LocalDate.of(2023, 12, 31) to LocalDate.of(2024, 7, 31),
            )

        testCases.forEach { (input, expected) ->
            val result = calculateNextJuly31(input)
            assertEquals(expected, result, "Failed for input date: $input")
        }
    }

    @Test
    fun `createRetentionPolicy sets correct policy for pedagogical assessment`() {
        val result = createRetentionPolicy(ChildDocumentType.PEDAGOGICAL_ASSESSMENT)

        assertEquals("RetentionPolicy", result.policyConfiguration.policyName)
        val rule = result.policyConfiguration.rules.rule
        assertEquals(28, rule.timeSpan)
        assertEquals("YearsFromCustomerBirthDate", rule.triggerEvent)
        assertEquals("AddTimeSpanToTarget", rule.action.actionType)
        assertEquals("Perusopetuslaki (628/1998) 16 a §", rule.action.actionAnnotation)
    }

    @Test
    fun `createRetentionPolicy sets correct policy for pedagogical report`() {
        val result = createRetentionPolicy(ChildDocumentType.PEDAGOGICAL_REPORT)

        assertEquals("RetentionPolicy", result.policyConfiguration.policyName)
        val rule = result.policyConfiguration.rules.rule
        assertEquals(28, rule.timeSpan)
        assertEquals("YearsFromCustomerBirthDate", rule.triggerEvent)
        assertEquals("AddTimeSpanToTarget", rule.action.actionType)
        assertEquals("Perusopetuslaki (628/1998) 16 a §", rule.action.actionAnnotation)
    }

    @Test
    fun `createRetentionPolicy sets correct policy for VASU document type`() {
        val result = createRetentionPolicy(ChildDocumentType.VASU)

        assertEquals("RetentionPolicy", result.policyConfiguration.policyName)
        val rule = result.policyConfiguration.rules.rule
        assertEquals(0, rule.timeSpan)
        assertEquals("InPerpetuity", rule.triggerEvent)
        assertEquals("AddTimeSpanToTarget", rule.action.actionType)
        assertEquals("KA/13089/07.01.01.03.01/2018", rule.action.actionAnnotation)
    }
}
