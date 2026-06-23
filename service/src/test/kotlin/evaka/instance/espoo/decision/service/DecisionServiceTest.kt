// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.decision.service

import evaka.core.application.ServiceNeed
import evaka.core.application.ServiceNeedOption
import evaka.core.daycare.UnitManager
import evaka.core.daycare.domain.Language
import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.Decision
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.decision.DecisionUnit
import evaka.core.decision.createDecisionPdf
import evaka.core.identity.ExternalIdentifier
import evaka.core.pdfgen.PdfGenerator
import evaka.core.pis.service.PersonDTO
import evaka.core.setting.SettingType
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.config.pdfTemplateEngine
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.template.EvakaTemplateProvider
import evaka.core.shared.template.ITemplateProvider
import java.io.FileOutputStream
import java.nio.file.Paths
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junitpioneer.jupiter.cartesian.CartesianTest

private val settings =
    mapOf(
        SettingType.DECISION_MAKER_NAME to "Paula Palvelupäällikkö",
        SettingType.DECISION_MAKER_TITLE to "Asiakaspalvelupäällikkö",
    )

@Tag("PDFGenerationTest")
class DecisionServiceTest {
    private lateinit var templateProvider: ITemplateProvider
    private lateinit var pdfService: PdfGenerator

    @BeforeEach
    fun setup() {
        templateProvider = EvakaTemplateProvider()
        pdfService = PdfGenerator(templateProvider, pdfTemplateEngine("espoo"))
    }

    @CartesianTest
    fun createDecisionPdf(
        @CartesianTest.Enum(DecisionType::class) decisionType: DecisionType,
        @CartesianTest.Enum(OfficialLanguage::class) lang: OfficialLanguage,
    ) {
        renderToReports(
            decisionType,
            ProviderType.MUNICIPAL,
            isTransferApplication = false,
            lang,
            "DecisionServiceTest-$decisionType-$lang.pdf",
        )
    }

    @ParameterizedTest
    @EnumSource(OfficialLanguage::class)
    fun createDaycareTransferDecisionPdf(lang: OfficialLanguage) {
        renderToReports(
            DecisionType.DAYCARE,
            ProviderType.MUNICIPAL,
            isTransferApplication = true,
            lang,
            "DecisionServiceTest-DAYCARE-transfer-$lang.pdf",
        )
    }

    @ParameterizedTest
    @EnumSource(OfficialLanguage::class)
    fun createDaycareVoucherDecisionPdf(lang: OfficialLanguage) {
        renderToReports(
            DecisionType.DAYCARE,
            ProviderType.PRIVATE_SERVICE_VOUCHER,
            isTransferApplication = false,
            lang,
            "DecisionServiceTest-DAYCARE-voucher-$lang.pdf",
        )
    }

    private fun renderToReports(
        decisionType: DecisionType,
        providerType: ProviderType,
        isTransferApplication: Boolean,
        lang: OfficialLanguage,
        filename: String,
    ) {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfService,
                settings,
                validDecision(decisionType, validDecisionUnit(providerType, lang)),
                child = validChild(),
                isTransferApplication = isTransferApplication,
                serviceNeed =
                    when (decisionType) {
                        DecisionType.CLUB,
                        DecisionType.PRESCHOOL,
                        DecisionType.PREPARATORY_EDUCATION -> null

                        else -> validServiceNeed()
                    },
                lang = lang,
                unitManager = validManager(),
                preschoolManager = validManager(),
            )

        val filepath = "${Paths.get("build").toAbsolutePath()}/reports/$filename"
        FileOutputStream(filepath).use { it.write(bytes) }
    }
}

private fun validManager() =
    UnitManager("Päivi Päiväkodinjohtaja", "paivi.paivakodinjohtaja@example.com", "0451231234")

private fun validServiceNeed() =
    ServiceNeed(
        startTime = "08:00",
        endTime = "16:00",
        shiftCare = false,
        partTime = false,
        ServiceNeedOption(
            ServiceNeedOptionId(UUID.randomUUID()),
            "Palveluntarve 1",
            "Palveluntarve 1",
            "Palveluntarve 1",
            null,
        ),
    )

private fun validDecision(type: DecisionType, decisionUnit: DecisionUnit) =
    Decision(
        DecisionId(UUID.randomUUID()),
        createdBy = "Pekka Palveluohjaaja",
        type,
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusMonths(3),
        decisionUnit,
        applicationId = ApplicationId(UUID.randomUUID()),
        childId = ChildId(UUID.randomUUID()),
        childName = "Matti",
        documentKey = null,
        decisionNumber = 12345,
        sentDate = LocalDate.now(),
        DecisionStatus.ACCEPTED,
        requestedStartDate = null,
        resolved = null,
        resolvedByName = null,
        documentContainsContactInfo = false,
        archivedAt = null,
    )

private fun validDecisionUnit(providerType: ProviderType, lang: OfficialLanguage) =
    DecisionUnit(
        DaycareId(UUID.randomUUID()),
        name = "Tapiolan päiväkoti",
        daycareDecisionName = "Tapiolan päiväkoti",
        preschoolDecisionName = "Tapiolan päiväkoti",
        manager = null,
        streetAddress = "Itätuulenkuja 5",
        postalCode = "02100",
        postOffice = "Espoo",
        phone = "+35850 1234564",
        decisionHandler = "Tapiolan päiväkoti",
        decisionHandlerAddress = "Itätuulenkuja 5, 02100 Espoo",
        providerType,
        language =
            when (lang) {
                OfficialLanguage.FI -> Language.fi
                OfficialLanguage.SV -> Language.sv
            },
    )

private fun validChild() =
    PersonDTO(
        PersonId(UUID.randomUUID()),
        duplicateOf = null,
        ExternalIdentifier.SSN.getInstance("010115A9532"),
        ssnAddingDisabled = false,
        firstName = "Matti",
        lastName = "Meikäläinen",
        preferredName = "Matti",
        email = null,
        phone = "",
        backupPhone = "",
        language = null,
        dateOfBirth = LocalDate.of(2015, 1, 1),
        streetAddress = "Kokinpellonraitti 3",
        postalCode = "02100",
        postOffice = "Espoo",
        residenceCode = "",
        restrictedDetailsEnabled = false,
        municipalityOfResidence = "Espoo",
    )
