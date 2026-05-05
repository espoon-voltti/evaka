// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.archival

import com.github.tomakehurst.wiremock.client.BasicCredentials
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToXml
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.matching.MultipartValuePatternBuilder
import evaka.core.application.ApplicationDetails
import evaka.core.application.ApplicationForm
import evaka.core.application.ApplicationOrigin
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.ChildDetails
import evaka.core.application.Guardian
import evaka.core.application.PersonBasics
import evaka.core.application.Preferences
import evaka.core.caseprocess.CaseProcess
import evaka.core.caseprocess.CaseProcessHistoryRow
import evaka.core.caseprocess.CaseProcessState
import evaka.core.caseprocess.DocumentConfidentiality
import evaka.core.caseprocess.DocumentMetadata
import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.Decision
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.decision.DecisionUnit
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentDeletionBasis
import evaka.core.document.DocumentTemplate
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.archival.ArchivalIntegrationClient
import evaka.core.document.childdocument.ChildBasics
import evaka.core.document.childdocument.ChildDocumentDecision
import evaka.core.document.childdocument.ChildDocumentDecisionStatus
import evaka.core.document.childdocument.ChildDocumentDetails
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.identity.ExternalIdentifier
import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.UnitData
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionPlacementDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionServiceNeed
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.pis.service.PersonDTO
import evaka.core.placement.PlacementType
import evaka.core.s3.Document
import evaka.core.s3.DocumentKey
import evaka.core.shared.ApplicationId
import evaka.core.shared.AreaId
import evaka.core.shared.CaseProcessId
import evaka.core.shared.ChildDocumentDecisionId
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionId
import evaka.core.shared.DocumentTemplateId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.feeThresholds2020
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.domain.UiLanguage
import evaka.core.user.EvakaUser
import evaka.core.user.EvakaUserType
import evaka.instance.tampere.AbstractTampereIntegrationTest
import evaka.trevaka.invoice.toPersonDetailed
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource

class TampereArchivalClientTest : AbstractTampereIntegrationTest() {

    @Autowired private lateinit var archivalIntegrationClient: ArchivalIntegrationClient

    @Test
    fun uploadDecision() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadDecisionToArchive(
                testCaseProcessApplication,
                testChildInfo,
                testDecisionDaycare,
                testDocumentDecisionDaycare,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-decision-daycare.xml",
            testDecisionDaycare.id.toString(),
            "vakapäätös tekstitiedostona",
        )
    }

    @Test
    fun uploadRejectedDecision() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadDecisionToArchive(
                testCaseProcessApplication,
                testChildInfo,
                testDecisionDaycare.copy(status = DecisionStatus.REJECTED),
                testDocumentDecisionDaycare,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-decision-daycare-rejected.xml",
            testDecisionDaycare.id.toString(),
            "vakapäätös tekstitiedostona",
        )
    }

    @Test
    fun uploadChildDocumentWithoutHistory() {
        assertThrows<IllegalStateException> {
            archivalIntegrationClient.uploadChildDocumentToArchive(
                testVasuDetails.id,
                emptyTestCaseProcessChildDocument,
                testChildInfo,
                testVasuDetails,
                testDocumentMetadataChildDocument,
                testDocumentChildDocument,
                testEvakaUser,
            )
        }
    }

    @Test
    fun uploadFeeDecisionToArchive() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadFeeDecisionToArchive(
                testCaseProcessApplication,
                testFeeDecision,
                testDocumentFeeDecision,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-fee-decision-daycare.xml",
            testFeeDecision.id.toString(),
            "maksupäätös tekstitiedostona",
        )
    }

    @Test
    fun uploadAnnulledFeeDecisionToArchive() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadFeeDecisionToArchive(
                testCaseProcessApplication,
                testFeeDecision.copy(status = FeeDecisionStatus.ANNULLED),
                testDocumentFeeDecision,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-fee-decision-daycare-annulled.xml",
            testFeeDecision.id.toString(),
            "maksupäätös tekstitiedostona",
        )
    }

    @Test
    fun uploadFeeDecisionWithNoDeciderToArchive() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadFeeDecisionToArchive(
                testCaseProcessApplication,
                noDeciderTestFeeDecision,
                testDocumentFeeDecision,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-fee-decision-daycare-no-decider.xml",
            noDeciderTestFeeDecision.id.toString(),
            "maksupäätös tekstitiedostona",
        )
    }

    @Test
    fun uploadVoucherValueDecisionToArchive() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadVoucherValueDecisionToArchive(
                testCaseProcessApplication,
                testVoucherValueDecision,
                testDocumentVoucherValueDecision,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-voucher-value-decision-daycare.xml",
            testVoucherValueDecision.id.toString(),
            "arvopäätös tekstitiedostona",
        )
    }

    @Test
    fun uploadAnnulledVoucherValueDecisionToArchive() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadVoucherValueDecisionToArchive(
                testCaseProcessApplication,
                testVoucherValueDecision.copy(status = VoucherValueDecisionStatus.ANNULLED),
                testDocumentVoucherValueDecision,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-voucher-value-decision-daycare-annulled.xml",
            testVoucherValueDecision.id.toString(),
            "arvopäätös tekstitiedostona",
        )
    }

    @Test
    fun uploadVoucherValueDecisionWithNoDeciderToArchive() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadVoucherValueDecisionToArchive(
                testCaseProcessApplication,
                noDeciderTestVoucherDecision,
                testDocumentVoucherValueDecision,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-voucher-value-decision-daycare-no-decider.xml",
            noDeciderTestVoucherDecision.id.toString(),
            "arvopäätös tekstitiedostona",
        )
    }

    @Test
    fun uploadChildDocument() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadChildDocumentToArchive(
                testVasuDetails.id,
                fullTestCaseProcessChildDocument,
                testChildInfo,
                testVasuDetails,
                testDocumentMetadataChildDocument,
                testDocumentChildDocument,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-child-document.xml",
            testVasuDetails.id.toString(),
        )
    }

    @Test
    fun uploadAcceptedChildDocumentDecision() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadChildDocumentToArchive(
                testChildDocumentDecisionDetails.id,
                fullTestCaseProcessChildDocument,
                testChildInfo,
                testChildDocumentDecisionDetails,
                testDocumentMetadataChildDocument,
                testDocumentChildDocumentDecision,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-child-document-decision.xml",
            testChildDocumentDecisionDetails.id.toString(),
            "pidennetyn oppivelvollisuuden päätös tekstitiedostona",
        )
    }

    @Test
    fun uploadAnnulledChildDocumentDecision() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadChildDocumentToArchive(
                testChildDocumentDecisionDetails.id,
                fullTestCaseProcessChildDocument,
                testChildInfo,
                testChildDocumentDecisionDetails.let {
                    it.copy(
                        decision = it.decision!!.copy(status = ChildDocumentDecisionStatus.ANNULLED)
                    )
                },
                testDocumentMetadataChildDocument,
                testDocumentChildDocumentDecision,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-child-document-decision-annulled.xml",
            testChildDocumentDecisionDetails.id.toString(),
            "pidennetyn oppivelvollisuuden päätös tekstitiedostona",
        )
    }

    @Test
    fun uploadRejectedChildDocumentDecision() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-success.xml")
                )
        )

        val archiveId =
            archivalIntegrationClient.uploadChildDocumentToArchive(
                testChildDocumentDecisionDetails.id,
                fullTestCaseProcessChildDocument,
                testChildInfo,
                testChildDocumentDecisionDetails.let {
                    it.copy(
                        decision = it.decision!!.copy(status = ChildDocumentDecisionStatus.REJECTED)
                    )
                },
                testDocumentMetadataChildDocument,
                testDocumentChildDocument,
                testEvakaUser,
            )
        assertEquals("archive-record-id-1", archiveId)

        validateMockContent(
            "archival-client/archival-post-record-request-child-document-decision-rejected.xml",
            testVasuDetails.id.toString(),
        )
    }

    @Test
    fun `uploadChildDocument with unexpected response body`() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-unexpected.xml")
                )
        )

        assertNull(
            archivalIntegrationClient.uploadChildDocumentToArchive(
                testVasuDetails.id,
                fullTestCaseProcessChildDocument,
                testChildInfo,
                testVasuDetails,
                testDocumentMetadataChildDocument,
                testDocumentChildDocument,
                testEvakaUser,
            )
        )
        validateMockContent(
            "archival-client/archival-post-record-request-child-document.xml",
            testVasuDetails.id.toString(),
        )
    }

    @Test
    fun `error invalid apikey`() {
        stubFor(
            post(urlEqualTo("/mock/frends/archival/records/add"))
                .willReturn(
                    aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/xml")
                        .withBodyFile("archival-client/archival-response-invalid-apikey.xml")
                )
        )

        val exception =
            assertThrows<IllegalStateException> {
                archivalIntegrationClient.uploadChildDocumentToArchive(
                    testVasuDetails.id,
                    fullTestCaseProcessChildDocument,
                    testChildInfo,
                    testVasuDetails,
                    testDocumentMetadataChildDocument,
                    testDocumentChildDocument,
                    testEvakaUser,
                )
            }
        assertEquals(
            "Unsuccessfully post record (status=401), response body: Errors(error=[Error(errorCode=invalid_apikey, errorSummary=API-avaimella ei ole oikeutta kirjoittaa kansioon)])",
            exception.message,
        )

        verify(postRequestedFor(urlEqualTo("/mock/frends/archival/records/add")))
    }
}

private fun PersonDTO.toChildBasics() = ChildBasics(id, firstName, lastName, dateOfBirth)

private fun PersonDTO.toChildDetails() =
    ChildDetails(
        PersonBasics(firstName, lastName, identity.toString()),
        dateOfBirth,
        null,
        null,
        "FI",
        "fi",
        allergies = "",
        diet = "",
        false,
        "",
    )

private fun PersonDTO.toGuardian() =
    Guardian(PersonBasics(firstName, lastName, identity.toString()), null, null, "", null)

private fun ChildDocumentDetails.toDocumentMetadata() =
    DocumentMetadata(
        documentId = template.id.raw,
        name = template.name,
        createdAtDate = null,
        createdAtTime = null,
        createdBy = null,
        confidential = template.confidentiality != null,
        confidentiality = template.confidentiality,
        downloadPath = "/employee/child-documents/$id/pdf",
        receivedBy = null,
        sfiDeliveries = emptyList(),
    )

private val testChildInfo =
    PersonDTO(
        id = PersonId(UUID.randomUUID()),
        duplicateOf = null,
        identity = ExternalIdentifier.SSN.getInstance("081222A9859"),
        ssnAddingDisabled = false,
        firstName = "John",
        lastName = "Smith",
        preferredName = "John",
        email = null,
        phone = "",
        backupPhone = "",
        language = null,
        dateOfBirth = LocalDate.of(2022, 12, 8),
        dateOfDeath = null,
        streetAddress = "",
        postalCode = "",
        postOffice = "",
        residenceCode = "",
        municipalityOfResidence = "",
    )

private val testAdultInfo =
    PersonDTO(
        id = PersonId(UUID.randomUUID()),
        duplicateOf = null,
        identity = ExternalIdentifier.SSN.getInstance("020998-958R"),
        ssnAddingDisabled = false,
        firstName = "Jane",
        lastName = "Smith",
        preferredName = "Jane",
        email = null,
        phone = "",
        backupPhone = "",
        language = null,
        dateOfBirth = LocalDate.of(1998, 9, 2),
        dateOfDeath = null,
        streetAddress = "",
        postalCode = "",
        postOffice = "",
        residenceCode = "",
        municipalityOfResidence = "",
    )

private val testEvakaUser =
    EvakaUser(AuthenticatedUser.SystemInternalUser.evakaUserId, "eVaka", EvakaUserType.SYSTEM)

private val testPreparerUser =
    EvakaUser(
        id = EvakaUserId(UUID.randomUUID()),
        name = "Esivalmistelija Esko",
        EvakaUserType.EMPLOYEE,
    )

private val testDeciderUser =
    EvakaUser(id = EvakaUserId(UUID.randomUUID()), name = "Päättäjä Pänü", EvakaUserType.EMPLOYEE)

private val testCaseProcessApplication =
    CaseProcess(
        id = CaseProcessId(UUID.randomUUID()),
        caseIdentifier = "1/12.06.01.17/2025",
        processDefinitionNumber = "12.06.01.17",
        year = 2025,
        number = 1,
        organization = "Tampereen kaupunki, varhaiskasvatus ja esiopetus",
        archiveDurationMonths = 10 * 12,
        migrated = false,
        history =
            listOf(
                CaseProcessHistoryRow(
                    rowIndex = 1,
                    state = CaseProcessState.INITIAL,
                    enteredAt = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(8, 45)),
                    enteredBy = testPreparerUser,
                ),
                CaseProcessHistoryRow(
                    rowIndex = 2,
                    state = CaseProcessState.PREPARATION,
                    enteredAt = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(8, 45)),
                    enteredBy = testPreparerUser,
                ),
                CaseProcessHistoryRow(
                    rowIndex = 3,
                    state = CaseProcessState.DECIDING,
                    enteredAt = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(8, 55)),
                    enteredBy = testDeciderUser,
                ),
                CaseProcessHistoryRow(
                    rowIndex = 4,
                    state = CaseProcessState.COMPLETED,
                    enteredAt = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(8, 55)),
                    enteredBy = testEvakaUser,
                ),
            ),
    )

private val testApplicationDaycare =
    ApplicationDetails(
        id = ApplicationId(UUID.randomUUID()),
        type = ApplicationType.DAYCARE,
        form =
            ApplicationForm(
                child = testChildInfo.toChildDetails(),
                guardian = testAdultInfo.toGuardian(),
                secondGuardian = null,
                otherPartner = null,
                otherChildren = emptyList(),
                preferences =
                    Preferences(
                        preferredUnits = emptyList(),
                        preferredStartDate = null,
                        connectedDaycarePreferredStartDate = null,
                        serviceNeed = null,
                        siblingBasis = null,
                        preparatory = false,
                        urgent = false,
                    ),
                maxFeeAccepted = false,
                otherInfo = "",
                clubDetails = null,
            ),
        status = ApplicationStatus.ACTIVE,
        origin = ApplicationOrigin.ELECTRONIC,
        childId = testChildInfo.id,
        guardianId = testAdultInfo.id,
        hasOtherGuardian = false,
        otherGuardianLivesInSameAddress = false,
        childRestricted = false,
        guardianRestricted = false,
        guardianDateOfDeath = null,
        checkedByAdmin = false,
        confidential = false,
        createdAt = HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(8, 55)),
        createdBy = null,
        modifiedAt = HelsinkiDateTime.of(LocalDate.of(2022, 1, 2), LocalTime.of(11, 12)),
        modifiedBy = null,
        sentDate = LocalDate.of(2022, 1, 3),
        sentTime = null,
        dueDate = LocalDate.of(2022, 5, 3),
        dueDateSetManuallyAt = null,
        transferApplication = false,
        additionalDaycareApplication = false,
        hideFromGuardian = false,
        allowOtherGuardianAccess = false,
        attachments = emptyList(),
    )

private val testDecisionDaycare =
    Decision(
        id = DecisionId(UUID.fromString("c74c1dad-f448-41ce-83af-e37d0c095286")),
        createdBy = "todo",
        type = DecisionType.DAYCARE,
        startDate = LocalDate.of(2022, 2, 1),
        endDate = LocalDate.of(2022, 7, 31),
        unit =
            DecisionUnit(
                id = DaycareId(UUID.randomUUID()),
                name = "asd",
                daycareDecisionName = "asd",
                preschoolDecisionName = "asd",
                manager = null,
                streetAddress = "",
                postalCode = "",
                postOffice = "",
                phone = null,
                decisionHandler = "",
                decisionHandlerAddress = "",
                providerType = ProviderType.MUNICIPAL,
            ),
        applicationId = testApplicationDaycare.id,
        childId = testChildInfo.id,
        childName = "${testChildInfo.lastName} ${testChildInfo.firstName}",
        documentKey = null,
        decisionNumber = 2398437,
        sentDate = LocalDate.of(2022, 1, 8),
        status = DecisionStatus.ACCEPTED,
        requestedStartDate = null,
        resolved = null,
        resolvedByName = null,
        documentContainsContactInfo = false,
        archivedAt = null,
    )

private val testDocumentDecisionDaycare =
    Document(
        DocumentKey.Decision(testDecisionDaycare.id, testDecisionDaycare.type, OfficialLanguage.FI)
            .value,
        "vakapäätös tekstitiedostona".toByteArray(Charsets.UTF_8),
        "text/plain",
    )

private val testFeeDecision =
    FeeDecisionDetailed(
        id = FeeDecisionId(UUID.fromString("385132a2-4be4-4b52-a3a6-08f27a1270e1")),
        children = emptyList(),
        validDuring = FiniteDateRange(LocalDate.of(2022, 2, 1), LocalDate.of(2022, 7, 31)),
        status = FeeDecisionStatus.SENT,
        decisionType = FeeDecisionType.NORMAL,
        headOfFamily = testAdultInfo.toPersonDetailed(),
        partner = null,
        headOfFamilyIncome = null,
        partnerIncome = null,
        familySize = 2,
        approvedAt = HelsinkiDateTime.of(LocalDate.of(2020, 1, 15), LocalTime.of(14, 43)),
        sentAt = HelsinkiDateTime.of(LocalDate.of(2020, 1, 5), LocalTime.of(8, 27)),
        feeThresholds = feeThresholds2020.getFeeDecisionThresholds(2),
        financeDecisionHandlerFirstName = "Arto",
        financeDecisionHandlerLastName = "Asiakasmaksusihteeri",
        documentContainsContactInfo = false,
        archivedAt = null,
    )

private val noDeciderTestFeeDecision =
    testFeeDecision.copy(
        financeDecisionHandlerFirstName = null,
        financeDecisionHandlerLastName = null,
    )

private val testDocumentFeeDecision =
    Document(
        DocumentKey.FeeDecision(testFeeDecision.id, OfficialLanguage.FI).value,
        "maksupäätös tekstitiedostona".toByteArray(Charsets.UTF_8),
        "text/plain",
    )

private val testVoucherValueDecision =
    VoucherValueDecisionDetailed(
        id = VoucherValueDecisionId(UUID.fromString("11479434-12da-4c59-a1e1-d9cd58c203a2")),
        validFrom = LocalDate.of(2022, 2, 1),
        validTo = LocalDate.of(2022, 7, 31),
        status = VoucherValueDecisionStatus.SENT,
        decisionType = VoucherValueDecisionType.NORMAL,
        headOfFamily = testAdultInfo.toPersonDetailed(),
        partner = null,
        headOfFamilyIncome = null,
        partnerIncome = null,
        childIncome = null,
        familySize = 2,
        feeThresholds = feeThresholds2020.getFeeDecisionThresholds(2),
        child = testChildInfo.toPersonDetailed(),
        placement =
            VoucherValueDecisionPlacementDetailed(
                unit =
                    UnitData(
                        id = DaycareId(UUID.randomUUID()),
                        name = "test daycare",
                        areaId = AreaId(UUID.randomUUID()),
                        areaName = "test area",
                        language = "fi",
                    ),
                type = PlacementType.DAYCARE,
            ),
        serviceNeed =
            VoucherValueDecisionServiceNeed(
                feeCoefficient = BigDecimal.ZERO,
                voucherValueCoefficient = BigDecimal.ZERO,
                feeDescriptionFi = "",
                feeDescriptionSv = "",
                voucherValueDescriptionFi = "",
                voucherValueDescriptionSv = "",
                missing = false,
            ),
        baseCoPayment = 0,
        siblingDiscount = 0,
        coPayment = 0,
        feeAlterations = emptyList(),
        finalCoPayment = 0,
        baseValue = 0,
        childAge = 0,
        assistanceNeedCoefficient = BigDecimal.ZERO,
        voucherValue = 0,
        approvedAt = HelsinkiDateTime.of(LocalDate.of(2020, 1, 15), LocalTime.of(14, 43)),
        sentAt = HelsinkiDateTime.of(LocalDate.of(2020, 1, 5), LocalTime.of(8, 27)),
        financeDecisionHandlerFirstName = "Marjo",
        financeDecisionHandlerLastName = "Maksumestari",
        documentContainsContactInfo = false,
        archivedAt = null,
    )

private val noDeciderTestVoucherDecision =
    testVoucherValueDecision.copy(
        financeDecisionHandlerFirstName = null,
        financeDecisionHandlerLastName = null,
    )

private val testDocumentVoucherValueDecision =
    Document(
        DocumentKey.VoucherValueDecision(testVoucherValueDecision.id).value,
        "arvopäätös tekstitiedostona".toByteArray(Charsets.UTF_8),
        "text/plain",
    )

private val testVasuDetails =
    ChildDocumentDetails(
        id = ChildDocumentId(UUID.fromString("8554e2a5-29bb-4e3c-9aca-59c4995c1d86")),
        status = DocumentStatus.COMPLETED,
        publishedAt = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(8, 45)),
        archivedAt = null,
        pdfAvailable = true,
        content = DocumentContent(answers = emptyList()),
        publishedContent = DocumentContent(answers = emptyList()),
        child = testChildInfo.toChildBasics(),
        template =
            DocumentTemplate(
                id = DocumentTemplateId(UUID.randomUUID()),
                name = "Varhaiskasvatussuunnitelma 2024-2025",
                type = ChildDocumentType.VASU,
                placementTypes = setOf(PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME),
                language = UiLanguage.FI,
                confidentiality =
                    DocumentConfidentiality(
                        durationYears = 100,
                        basis = "Varhaiskasvatuslaki 40 § 3 mom.",
                    ),
                legalBasis = "Varhaiskasvatuslaki (540/2018) 40§:n 3 mom.",
                validity = DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 7, 31)),
                published = true,
                processDefinitionNumber = "12.06.01.11",
                archiveDurationMonths = 1440,
                archiveExternally = true,
                endDecisionWhenUnitChanges = false,
                deletionRetentionDays = 10 * 365,
                deletionRetentionBasis = DocumentDeletionBasis.PLACEMENT_END,
                content = DocumentTemplateContent(sections = emptyList()),
            ),
        decisionMaker = null,
        decision = null,
    )

private val testChildDocumentDecisionDetails =
    ChildDocumentDetails(
        id = ChildDocumentId(UUID.fromString("8554e2a5-29bb-4e3c-9aca-59c4995c1d86")),
        status = DocumentStatus.COMPLETED,
        publishedAt = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(8, 45)),
        archivedAt = null,
        pdfAvailable = true,
        content = DocumentContent(answers = emptyList()),
        publishedContent = DocumentContent(answers = emptyList()),
        child = testChildInfo.toChildBasics(),
        template =
            DocumentTemplate(
                id = DocumentTemplateId(UUID.randomUUID()),
                name = "Päätös pidennetystä oppivelvollisuudesta",
                type = ChildDocumentType.OTHER_DECISION,
                placementTypes = setOf(PlacementType.PRESCHOOL, PlacementType.PRESCHOOL_DAYCARE),
                language = UiLanguage.FI,
                confidentiality =
                    DocumentConfidentiality(
                        durationYears = 100,
                        basis = "Varhaiskasvatuslaki 40 § 3 mom.",
                    ),
                legalBasis = "Varhaiskasvatuslaki (540/2018) 40§:n 3 mom.",
                validity = DateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 7, 31)),
                published = true,
                processDefinitionNumber = "12.06.01.26",
                archiveDurationMonths = 1440,
                archiveExternally = true,
                endDecisionWhenUnitChanges = false,
                deletionRetentionDays = 10 * 365,
                deletionRetentionBasis = DocumentDeletionBasis.PLACEMENT_END,
                content = DocumentTemplateContent(sections = emptyList()),
            ),
        decisionMaker = null,
        decision =
            ChildDocumentDecision(
                id = ChildDocumentDecisionId(UUID.randomUUID()),
                status = ChildDocumentDecisionStatus.ACCEPTED,
                annulmentReason = "",
                decisionNumber = 123,
                validity = DateRange(LocalDate.of(2025, 7, 31), LocalDate.of(2025, 7, 31)),
                createdAt = HelsinkiDateTime.of(LocalDate.of(2020, 1, 15), LocalTime.of(14, 43)),
                daycareName = "Ullanlinnan merenkulun instituutti",
            ),
    )

private val emptyTestCaseProcessChildDocument = null

private val testDocumentMetadataChildDocument = testVasuDetails.toDocumentMetadata()

private val testDocumentChildDocument =
    Document(
        DocumentKey.ChildDocument(testVasuDetails.id).value,
        "vasu tekstitiedostona".toByteArray(Charsets.UTF_8),
        "text/plain",
    )

private val testDocumentChildDocumentDecision =
    Document(
        DocumentKey.ChildDocument(testChildDocumentDecisionDetails.id).value,
        "pidennetyn oppivelvollisuuden päätös tekstitiedostona".toByteArray(Charsets.UTF_8),
        "text/plain",
    )

private val fullTestCaseProcessChildDocument =
    CaseProcess(
        id = CaseProcessId(UUID.randomUUID()),
        caseIdentifier = "1/12.06.01.11/2025",
        processDefinitionNumber = "12.06.01.11",
        year = 2025,
        number = 1,
        organization = "Varhaiskasvatustoiminta",
        archiveDurationMonths = 12 * 10,
        migrated = false,
        history =
            listOf(
                CaseProcessHistoryRow(
                    rowIndex = 1,
                    state = CaseProcessState.INITIAL,
                    enteredAt = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(8, 45)),
                    enteredBy = testPreparerUser,
                ),
                CaseProcessHistoryRow(
                    rowIndex = 2,
                    state = CaseProcessState.PREPARATION,
                    enteredAt = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(8, 45)),
                    enteredBy = testPreparerUser,
                ),
                CaseProcessHistoryRow(
                    rowIndex = 3,
                    state = CaseProcessState.DECIDING,
                    enteredAt = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(8, 55)),
                    enteredBy = testDeciderUser,
                ),
                CaseProcessHistoryRow(
                    rowIndex = 4,
                    state = CaseProcessState.COMPLETED,
                    enteredAt = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(8, 55)),
                    enteredBy = testEvakaUser,
                ),
            ),
    )

private fun validateMockContent(
    metadataPath: String,
    documentId: String,
    documentContent: String = "vasu tekstitiedostona",
) =
    verify(
        postRequestedFor(urlEqualTo("/mock/frends/archival/records/add"))
            .withBasicAuth(BasicCredentials("frends-user", "frends-pass"))
            .withHeader("Content-Type", containing("multipart/form-data; boundary="))
            .withoutHeader("X-API-key")
            .withoutHeader("X-API-transactionid") // only set when running AsyncJob
            .withRequestBodyPart(
                MultipartValuePatternBuilder()
                    .withHeader("Content-Disposition", equalTo("form-data; name=\"xml\""))
                    .withHeader("Content-Type", equalTo("application/xml; charset=utf-8"))
                    .withBody(
                        equalToXml(
                            ClassPathResource(metadataPath).getContentAsString(Charsets.UTF_8)
                        )
                    )
                    .build()
            )
            .withRequestBodyPart(
                MultipartValuePatternBuilder()
                    .withHeader(
                        "Content-Disposition",
                        equalTo("form-data; name=\"content\"; filename=\"$documentId\""),
                    )
                    .withHeader("Content-Type", equalTo("text/plain"))
                    .withBody(equalTo(documentContent))
                    .build()
            )
    )
