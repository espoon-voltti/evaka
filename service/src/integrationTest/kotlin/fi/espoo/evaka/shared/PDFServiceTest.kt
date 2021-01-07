// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
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
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.voltti.pdfgen.PDFService
import fi.espoo.voltti.pdfgen.Page
import fi.espoo.voltti.pdfgen.Template
import fi.espoo.voltti.pdfgen.process
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.context.Context
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.Locale
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [PDFConfig::class, PDFService::class])
class PDFServiceTest {
    @Autowired
    lateinit var templateEngine: ITemplateEngine

    @Autowired
    lateinit var pdfService: PDFService

    @Test
    fun createFinnishPDFs() {
        render(daycareTransferDecision, transferApplication, "fi")
        render(daycareDecision, application, "fi")
        render(daycareDecisionPartTime, application, "fi")
        render(preschoolDaycareDecision, application, "fi")
        render(preschoolDecision, application, "fi")
        render(preparatoryDecision, application, "fi")
    }

    @Test
    fun createSwedishPDFs() {
        render(daycareTransferDecision, transferApplication, "sv")
        render(daycareDecision, application, "sv")
        render(daycareDecisionPartTime, application, "sv")
        render(preschoolDaycareDecision, application, "sv")
        render(preschoolDecision, application, "sv")
        render(preparatoryDecision, application, "sv")
    }

    fun render(decision: Decision, application: ApplicationDetails, lang: String) {
        val templates = when (decision.type) {
            DecisionType.DAYCARE, DecisionType.PRESCHOOL_DAYCARE, DecisionType.DAYCARE_PART_TIME -> {
                if (application.transferApplication) {
                    listOf(
                        "daycare/transfer/daycare-decision-page1",
                        "daycare/transfer/daycare-decision-page2",
                        "daycare/transfer/daycare-correction",
                        "daycare/transfer/daycare-acceptance-form"
                    )
                } else {
                    listOf(
                        "daycare/daycare-decision-page1",
                        "daycare/daycare-decision-page2",
                        "daycare/daycare-correction",
                        "daycare/daycare-acceptance-form"
                    )
                }
            }
            DecisionType.PRESCHOOL -> listOf(
                "preschool/preschool-decision-page1",
                "preschool/preschool-decision-page2",
                "preschool/preschool-correction",
                "preschool/preschool-acceptance-form"
            )
            DecisionType.PREPARATORY_EDUCATION -> listOf(
                "preparatory/preparatory-decision-page1",
                "preparatory/preparatory-decision-page2",
                "preparatory/preparatory-correction",
                "preparatory/preparatory-acceptance-form"
            )
            else -> error("Daycare decision had a decision type of CLUB_DECISION.")
        }
        val pages = templates.mapIndexed { i, template ->
            Page(Template(template), createContext(lang, decision, application, child, guardian, manager, sendAddress, i + 1))
        }

        val documentBytes = pdfService.render(pages)

        val file = if (System.getProperty("os.name")?.contains("windows", true) == true) {
            File("C:/Temp/${decision.type}-$lang-transfer=${application.transferApplication}.pdf")
        } else {
            File("/tmp/${decision.type}-$lang-transfer=${application.transferApplication}.pdf")
        }
        writeTofile(documentBytes, file)

        // if there is no property for a key used in template, template prints ??key??
        pages.forEach {
            val string = templateEngine.process(it)
            assertEquals(false, string.contains("??"))
        }
    }

    private fun writeTofile(documentBytes: ByteArray, file: File) {
        FileOutputStream(file).use {
            it.write(documentBytes)
        }
    }

    private fun createContext(
        lang: String,
        decision: Decision,
        application: ApplicationDetails,
        child: PersonDTO,
        guardian: PersonDTO,
        manager: DaycareManager,
        sendAddress: DecisionSendAddress,
        pageNumber: Int
    ): Context {
        val isPartTimeDecision: Boolean = decision.type === DecisionType.DAYCARE_PART_TIME

        val legacyApplication = DecisionService.PdfTemplateApplication(
            form = DaycareFormV0.fromForm2(application.form, application.type, false, false),
            childDateOfBirth = child.dateOfBirth
        )
        return Context().apply {
            locale = createTemplateLocale(lang)
            setVariable("decision", decision)
            setVariable("application", legacyApplication)
            setVariable("guardian", guardian)
            setVariable("manager", manager)
            setVariable("sendAddress", sendAddress)
            setVariable("pageNumber", pageNumber)
            setVariable("isPartTimeDecision", isPartTimeDecision)
        }
    }

    private fun createTemplateLocale(lang: String): Locale {
        return Locale.Builder().setLanguage(lang).build()
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
