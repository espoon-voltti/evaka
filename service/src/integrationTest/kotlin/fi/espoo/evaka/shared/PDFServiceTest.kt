// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.service.DaycareManager
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionSendAddress
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.DecisionUnit
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.config.PDFConfig
import fi.espoo.evaka.test.validPreschoolApplication
import fi.espoo.evaka.test.validVoucherApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.voltti.pdfgen.PDFService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID

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

private val child = PersonDTO(
    testChild_1.id,
    ExternalIdentifier.SSN.getInstance(testChild_1.ssn!!),
    null,
    null,
    null,
    null,
    null,
    null,
    testChild_1.dateOfBirth,
    null,
    null,
    null,
    null,
    null
)
private val guardian = PersonDTO(
    testAdult_1.id,
    ExternalIdentifier.SSN.getInstance(testAdult_1.ssn!!),
    null,
    "John Jonathan",
    "Doe",
    null,
    null,
    null,
    testAdult_1.dateOfBirth,
    null,
    null,
    null,
    null,
    null
)
private val manager = DaycareManager("Maija Manageri", "maija.manageri@test.com", "0401231234")
private val sendAddress = DecisionSendAddress("Kamreerintie 2", "02770", "Espoo", "Kamreerintie 2", "02770 Espoo", "")

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [PDFConfig::class, PDFService::class]
)
class PDFServiceTest {

    @Autowired
    lateinit var pdfService: PDFService

    @Test
    fun createFinnishPDFs() {
        createPDF(daycareTransferDecision, transferApplication, "fi")
        createPDF(daycareDecision, application, "fi")
        createPDF(daycareDecisionPartTime, application, "fi")
        createPDF(preschoolDaycareDecision, application, "fi")
        createPDF(preschoolDecision, application, "fi")
        createPDF(preparatoryDecision, application, "fi")
        createPDF(daycareDecision, validVoucherApplication, "fi")
    }

    @Test
    fun createSwedishPDFs() {
        createPDF(daycareTransferDecision, transferApplication, "sv")
        createPDF(daycareDecision, application, "sv")
        createPDF(daycareDecisionPartTime, application, "sv")
        createPDF(preschoolDaycareDecision, application, "sv")
        createPDF(preschoolDecision, application, "sv")
        createPDF(preparatoryDecision, application, "sv")
        createPDF(daycareDecision, validVoucherApplication, "sv")
    }

    fun createPDF(decision: Decision, application: ApplicationDetails, lang: String) {
        val decisionPdfByteArray =
            DecisionService.createDecisionPdf(pdfService, decision, application, guardian, child, lang, manager)

        val file = File.createTempFile(decision.id.toString(), ".pdf")
        println(file.absolutePath)
        FileOutputStream(file).use {
            it.write(decisionPdfByteArray)
        }


        // if there is no property for a key used in template, template prints ??key??
        // pages.forEach {
        //    val string = templateEngine.process(it)
        //    assertEquals(false, string.contains("??"))
        // }
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
        "unit name",
        "Daycare unit name",
        "Preschool unit name",
        "manager name",
        "unit address",
        "unit postal code",
        "ESPOO",
        "Handler",
        "Handler address",
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
