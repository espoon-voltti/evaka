// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.service.DaycareManager
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.DecisionUnit
import fi.espoo.evaka.decision.createDecisionPdf
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.config.PDFConfig
import fi.espoo.evaka.test.validPreschoolApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.voltti.pdfgen.PDFService
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID

val logger = KotlinLogging.logger { }

private val application = validPreschoolApplication
private val transferApplication = application.copy(
    transferApplication = true
)

private val daycareTransferDecision =
    createValidDecision(applicationId = transferApplication.id, type = DecisionType.DAYCARE)
private val daycareDecision = createValidDecision(applicationId = application.id, type = DecisionType.DAYCARE)
private val preschoolDaycareDecision =
    createValidDecision(applicationId = application.id, type = DecisionType.PRESCHOOL_DAYCARE)
private val daycareDecisionPartTime =
    createValidDecision(applicationId = application.id, type = DecisionType.DAYCARE_PART_TIME)
private val preschoolDecision = createValidDecision(applicationId = application.id, type = DecisionType.PRESCHOOL)
private val preparatoryDecision =
    createValidDecision(applicationId = application.id, type = DecisionType.PREPARATORY_EDUCATION)
private val clubDecision = createValidDecision(applicationId = application.id, type = DecisionType.CLUB)

private val voucherDecision = daycareDecision.copy(
    endDate = LocalDate.of(2019, 7, 31),
    unit = DecisionUnit(
        UUID.randomUUID(),
        "Suomenniemen palvelusetelipäiväkoti",
        "Suomenniemen palvelusetelipäiväkoti",
        "Suomenniemen palvelusetelipäiväkodin esiopetus",
        "Pirkko Sanelma Ullanlinna",
        "Hyväntoivonniementie 13 B",
        "02200",
        "ESPOO",
        "Suomenniemen palvelusetelipäiväkodin asiakaspalvelu",
        "Kartanonkujanpää 565, 02210 Espoo",
        providerType = ProviderType.PRIVATE_SERVICE_VOUCHER
    )
)

private val child = PersonDTO(
    testChild_1.id,
    ExternalIdentifier.SSN.getInstance(testChild_1.ssn!!),
    null,
    "Kullervo Kyöstinpoika",
    "Pöysti",
    null,
    null,
    "",
    null,
    testChild_1.dateOfBirth,
    null,
    "Kuusikallionrinne 26 A 4",
    "02270",
    "Espoo",
    null
)
private val guardian = PersonDTO(
    testAdult_1.id,
    ExternalIdentifier.SSN.getInstance(testAdult_1.ssn!!),
    null,
    "Kyösti Taavetinpoika",
    "Pöysti",
    "kyostipoysti@example.com",
    "+358914822",
    "+358914829",
    null,
    testAdult_1.dateOfBirth,
    null,
    "Kuusikallionrinne 26 A 4",
    "02270",
    "Espoo",
    null
)
private val manager = DaycareManager("Pirkko Päiväkodinjohtaja", "pirkko.paivakodinjohtaja@example.com", "0401231234")

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [PDFConfig::class, PDFService::class]
)
class PDFServiceTest {

    @Autowired
    lateinit var pdfService: PDFService

    @Test
    fun createFinnishPDFs() {

        createPDF(daycareTransferDecision, true, "fi")
        createPDF(daycareDecision, false, "fi")
        createPDF(daycareDecisionPartTime, false, "fi")
        createPDF(preschoolDaycareDecision, false, "fi")
        createPDF(preschoolDecision, false, "fi")
        createPDF(preparatoryDecision, false, "fi")
        createPDF(voucherDecision, false, "fi")
        createPDF(clubDecision, false, "fi")
    }

    @Test
    fun createSwedishPDFs() {

        createPDF(daycareTransferDecision, true, "sv")
        createPDF(daycareDecision, false, "sv")
        createPDF(daycareDecisionPartTime, false, "sv")
        createPDF(preschoolDaycareDecision, false, "sv")
        createPDF(preschoolDecision, false, "sv")
        createPDF(preparatoryDecision, false, "sv")
        createPDF(voucherDecision, false, "sv")
        createPDF(clubDecision, false, "sv")
    }

    private fun createPDF(decision: Decision, isTransferApplication: Boolean, lang: String) {
        val decisionPdfByteArray =
            createDecisionPdf(
                pdfService,
                decision,
                guardian,
                child,
                isTransferApplication,
                lang,
                manager
            )

        val file = File.createTempFile("decision_", ".pdf")
        assertNotNull(decisionPdfByteArray)

        FileOutputStream(file).use {
            it.write(decisionPdfByteArray)
        }

        logger.debug { "Generated $lang ${decision.type} (${decision.unit.providerType}${if (isTransferApplication) ", transfer application" else ""}) decision PDF to ${file.absolutePath}" }
    }
}

fun createValidDecision(
    id: UUID = UUID.randomUUID(),
    createdBy: String = "John Doe",
    type: DecisionType = DecisionType.DAYCARE,
    startDate: LocalDate = LocalDate.of(2019, 1, 1),
    endDate: LocalDate = LocalDate.of(2019, 12, 31),
    unit: DecisionUnit = DecisionUnit(
        UUID.randomUUID(),
        "Kuusenkerkän päiväkoti",
        "Kuusenkerkän päiväkoti",
        "Kuusenkerkän päiväkodin esiopetus",
        "Pirkko Päiväkodinjohtaja",
        "Kuusernkerkänpolku 123",
        "02200",
        "ESPOO",
        "Varhaiskasvatuksen palveluohjaus",
        "Kamreerintie 2, 02200 Espoo",
        providerType = ProviderType.MUNICIPAL
    ),
    applicationId: UUID = UUID.randomUUID(),
    childId: UUID = UUID.randomUUID(),
    documentUri: String? = null,
    otherGuardianDocumentUri: String? = null,
    decisionNumber: Long = 123,
    sentDate: LocalDate = LocalDate.now(),
    status: DecisionStatus = DecisionStatus.ACCEPTED,
    resolved: LocalDate? = null
): Decision {
    return Decision(
        id = id,
        createdBy = createdBy,
        type = type,
        startDate = startDate,
        endDate = endDate,
        unit = unit,
        applicationId = applicationId,
        childId = childId,
        documentUri = documentUri,
        otherGuardianDocumentUri = otherGuardianDocumentUri,
        decisionNumber = decisionNumber,
        sentDate = sentDate,
        status = status,
        childName = "Test Child",
        requestedStartDate = startDate,
        resolved = resolved
    )
}
