// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.decision.service

import evaka.core.application.ServiceNeed
import evaka.core.application.ServiceNeedOption
import evaka.core.daycare.UnitManager
import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.Decision
import evaka.core.decision.DecisionSendAddress
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
import evaka.core.shared.template.ITemplateProvider
import evaka.instance.turku.TurkuTemplateProvider
import java.io.FileOutputStream
import java.nio.file.Paths
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

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
        templateProvider = TurkuTemplateProvider()
        pdfService = PdfGenerator(templateProvider, pdfTemplateEngine("turku"))
    }

    @ParameterizedTest
    @EnumSource(value = DecisionType::class, names = [], mode = EnumSource.Mode.EXCLUDE)
    fun createDecisionPdf(decisionType: DecisionType) {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfService,
                settings,
                validDecision(decisionType, validDecisionUnit(ProviderType.MUNICIPAL)),
                child = validChild(),
                isTransferApplication = false,
                serviceNeed =
                    when (decisionType) {
                        DecisionType.CLUB -> {
                            null
                        }

                        else -> {
                            ServiceNeed(
                                startTime = "08:00",
                                endTime = "16:00",
                                shiftCare = false,
                                partTime = false,
                                ServiceNeedOption(
                                    ServiceNeedOptionId(UUID.randomUUID()),
                                    "Palveluntarve 1 pitkänimi Palveluntarve 1 pitkänimi Palveluntarve",
                                    "Palveluntarve 1",
                                    "Palveluntarve 1",
                                    null,
                                ),
                            )
                        }
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

        val filepath =
            "${Paths.get("build").toAbsolutePath()}/reports/DecisionServiceTest-$decisionType.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun createDecisionPdfWithoutSettings() {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfService,
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

        val filepath =
            "${Paths.get("build").toAbsolutePath()}/reports/DecisionServiceTest-DAYCARE-without-settings.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun createDaycareTransferDecisionPdf() {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfService,
                settings,
                validDecision(DecisionType.DAYCARE, validDecisionUnit(ProviderType.MUNICIPAL)),
                child = validChild(),
                isTransferApplication = true,
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

        val filepath =
            "${Paths.get("build").toAbsolutePath()}/reports/DecisionServiceTest-DAYCARE-transfer.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun createDaycareVoucherDecisionPdf() {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfService,
                settings,
                validDecision(
                    DecisionType.DAYCARE,
                    validDecisionUnit(ProviderType.PRIVATE_SERVICE_VOUCHER),
                ),
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

        val filepath =
            "${Paths.get("build").toAbsolutePath()}/reports/DecisionServiceTest-DAYCARE-voucher.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun createRestrictedDetailsEnabledPdf() {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfService,
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

        val filepath =
            "${Paths.get("build").toAbsolutePath()}/reports/DecisionServiceTest-DAYCARE-restricted-details-enabled.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun createDecisionPdfBySystemInternalUser() {
        val bytes =
            createDecisionPdf(
                templateProvider,
                pdfService,
                settings,
                validDecision(DecisionType.PRESCHOOL, validDecisionUnit(ProviderType.MUNICIPAL))
                    .copy(createdBy = "eVaka"),
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

        val filepath =
            "${Paths.get("build").toAbsolutePath()}/reports/DecisionServiceTest-PRESCHOOL-by-system-internal-user.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }
}

private fun validDecision(type: DecisionType, decisionUnit: DecisionUnit) =
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

private fun validDecisionUnit(providerType: ProviderType) =
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

private fun validGuardian(restrictedDetailsEnabled: Boolean = false) =
    PersonDTO(
        PersonId(UUID.randomUUID()),
        duplicateOf = null,
        ExternalIdentifier.SSN.getInstance("070682-924A"),
        ssnAddingDisabled = false,
        firstName = "Maija",
        lastName = "Meikäläinen",
        preferredName = "Maija",
        email = null,
        phone = "",
        backupPhone = "",
        language = null,
        dateOfBirth = LocalDate.of(1982, 6, 7),
        streetAddress = "Kokinpellonraitti 3",
        postalCode = "33870",
        postOffice = "Tampere",
        residenceCode = "",
        restrictedDetailsEnabled = restrictedDetailsEnabled,
        municipalityOfResidence = "Tampere",
    )

private fun validChild(restrictedDetailsEnabled: Boolean = false) =
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
        postalCode = "33870",
        postOffice = "Tampere",
        residenceCode = "",
        restrictedDetailsEnabled = restrictedDetailsEnabled,
        municipalityOfResidence = "Tampere",
    )

private fun validAddress() = DecisionSendAddress("Kotikatu", "20100", "Turku", "", "", "")
