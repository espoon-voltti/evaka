// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.decision.service

import evaka.core.application.ServiceNeed
import evaka.core.application.ServiceNeedOption
import evaka.core.daycare.UnitManager
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
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.template.ITemplateProvider
import evaka.instance.tampere.AbstractTampereIntegrationTest
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.springframework.beans.factory.annotation.Autowired

private val settings =
    mapOf(
        SettingType.DECISION_MAKER_NAME to "Paula Palvelupäällikkö",
        SettingType.DECISION_MAKER_TITLE to "Asiakaspalvelupäällikkö",
    )

class DecisionServiceTest : AbstractTampereIntegrationTest() {

    @Autowired private lateinit var templateProvider: ITemplateProvider

    @Autowired private lateinit var pdfGenerator: PdfGenerator

    @CartesianTest
    fun createDecisionPdf(
        @CartesianTest.Enum(
            value = DecisionType::class,
            names = ["PREPARATORY_EDUCATION"],
            mode = CartesianTest.Enum.Mode.EXCLUDE,
        )
        decisionType: DecisionType,
        @CartesianTest.Enum(
            value = ProviderType::class,
            names = ["MUNICIPAL", "PRIVATE_SERVICE_VOUCHER"],
            mode = CartesianTest.Enum.Mode.INCLUDE,
        )
        providerType: ProviderType,
        @CartesianTest.Values(booleans = [false, true]) isTransferApplication: Boolean,
    ) {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfGenerator,
                settings,
                validDecision(decisionType, validDecisionUnit(providerType)),
                child = validChild(),
                isTransferApplication = isTransferApplication,
                serviceNeed =
                    when (decisionType) {
                        DecisionType.CLUB -> null

                        else ->
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
                    },
                lang = OfficialLanguage.FI,
                unitManager =
                    UnitManager(
                        "Päivi Päiväkodinjohtaja",
                        "paivi.paivakodinjohtaja@example.com",
                        "0451231234",
                    ),
                preschoolManager =
                    UnitManager(
                        "Päivi Päiväkodinjohtaja",
                        "paivi.paivakodinjohtaja@example.com",
                        "0451231234",
                    ),
            )

        val filename =
            "DecisionServiceTest-$decisionType-$providerType${if (isTransferApplication) "-transfer" else ""}.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun createDecisionPdfWithoutServiceNeed() {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfGenerator,
                settings,
                validDecision(DecisionType.DAYCARE, validDecisionUnit(ProviderType.MUNICIPAL)),
                child = validChild(),
                isTransferApplication = false,
                ServiceNeed(
                    startTime = "08:00",
                    endTime = "16:00",
                    shiftCare = false,
                    partTime = false,
                    serviceNeedOption = null,
                ),
                lang = OfficialLanguage.FI,
                unitManager =
                    UnitManager(
                        "Päivi Päiväkodinjohtaja",
                        "paivi.paivakodinjohtaja@example.com",
                        "0451231234",
                    ),
                preschoolManager =
                    UnitManager(
                        "Päivi Päiväkodinjohtaja",
                        "paivi.paivakodinjohtaja@example.com",
                        "0451231234",
                    ),
            )

        val filename = "DecisionServiceTest-DAYCARE-without-service-need-option.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun createDecisionPdfWithoutSettings() {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfGenerator,
                mapOf(),
                validDecision(DecisionType.DAYCARE, validDecisionUnit(ProviderType.MUNICIPAL)),
                child = validChild(),
                isTransferApplication = false,
                serviceNeed =
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
                    ),
                lang = OfficialLanguage.FI,
                unitManager =
                    UnitManager(
                        "Päivi Päiväkodinjohtaja",
                        "paivi.paivakodinjohtaja@example.com",
                        "0451231234",
                    ),
                preschoolManager =
                    UnitManager(
                        "Päivi Päiväkodinjohtaja",
                        "paivi.paivakodinjohtaja@example.com",
                        "0451231234",
                    ),
            )

        val filename = "DecisionServiceTest-DAYCARE-without-settings.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun createRestrictedDetailsEnabledPdf() {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfGenerator,
                settings,
                validDecision(DecisionType.DAYCARE, validDecisionUnit(ProviderType.MUNICIPAL)),
                child = validChild(true),
                isTransferApplication = false,
                serviceNeed =
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
                    ),
                lang = OfficialLanguage.FI,
                unitManager =
                    UnitManager(
                        "Päivi Päiväkodinjohtaja",
                        "paivi.paivakodinjohtaja@example.com",
                        "0451231234",
                    ),
                preschoolManager =
                    UnitManager(
                        "Päivi Päiväkodinjohtaja",
                        "paivi.paivakodinjohtaja@example.com",
                        "0451231234",
                    ),
            )

        val filename = "DecisionServiceTest-DAYCARE-restricted-details-enabled.pdf"
        writeReportsFile(filename, bytes)
    }
}

fun validDecision(type: DecisionType, decisionUnit: DecisionUnit) =
    Decision(
        DecisionId(UUID.randomUUID()),
        createdBy = "Päivi Päiväkodinjohtaja",
        type,
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusMonths(3),
        decisionUnit,
        applicationId = ApplicationId(UUID.randomUUID()),
        childId = ChildId(UUID.randomUUID()),
        childName = "Matti",
        documentKey = null,
        decisionNumber = 1,
        sentDate = LocalDate.now(),
        DecisionStatus.ACCEPTED,
        requestedStartDate = null,
        resolved = null,
        resolvedByName = null,
        documentContainsContactInfo = false,
        archivedAt = null,
    )

fun validDecisionUnit(providerType: ProviderType) =
    DecisionUnit(
        DaycareId(UUID.randomUUID()),
        name = "Vuoreksen kerho",
        daycareDecisionName = "Vuoreksen kerho",
        preschoolDecisionName = "Vuoreksen kerho",
        manager = null,
        streetAddress = "Rautiolanrinne 2",
        postalCode = "33870",
        postOffice = "Tampere",
        phone = "+35850 1234564",
        decisionHandler = "Vuoreksen kerho",
        decisionHandlerAddress = "Rautiolanrinne 2, 33870 Tampere",
        providerType,
    )

fun validChild(restrictedDetailsEnabled: Boolean = false) =
    PersonDTO(
        PersonId(UUID.randomUUID()),
        null,
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
        postalCode = "33870",
        postOffice = "Tampere",
        residenceCode = "",
        municipalityOfResidence = "Tampere",
        restrictedDetailsEnabled = restrictedDetailsEnabled,
    )
