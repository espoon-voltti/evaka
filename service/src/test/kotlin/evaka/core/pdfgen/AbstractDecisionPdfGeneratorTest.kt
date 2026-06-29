// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pdfgen

import evaka.core.application.ServiceNeed
import evaka.core.application.ServiceNeedOption
import evaka.core.daycare.UnitManager
import evaka.core.daycare.domain.Language
import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.Decision
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.decision.DecisionUnit
import evaka.core.decision.PdfReasoning
import evaka.core.decision.createDecisionPdf
import evaka.core.identity.ExternalIdentifier
import evaka.core.invoicing.domain.EmployeeWithName
import evaka.core.invoicing.domain.FeeDecisionChildDetailed
import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.PersonDetailed
import evaka.core.invoicing.domain.UnitData
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionPlacementDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionServiceNeed
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.invoicing.service.FeeDecisionPdfData
import evaka.core.invoicing.service.VoucherValueDecisionPdfData
import evaka.core.invoicing.testDecision1
import evaka.core.invoicing.testDecisionIncome
import evaka.core.invoicing.testFeeThresholds
import evaka.core.pis.service.PersonDTO
import evaka.core.placement.PlacementType
import evaka.core.setting.SettingType
import evaka.core.shared.ApplicationId
import evaka.core.shared.AreaId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionId
import evaka.core.shared.EmployeeId
import evaka.core.shared.PersonId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.config.pdfTemplateEngine
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.template.EvakaTemplateProvider
import evaka.core.shared.template.ITemplateProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertNotNull
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

private val logger = KotlinLogging.logger {}

private val testChild = DevPerson(ssn = "010617A123U", dateOfBirth = LocalDate.of(2017, 6, 1))

private val samplePartner =
    PersonDetailed(
        PersonId(UUID.randomUUID()),
        LocalDate.of(1980, 6, 14),
        null,
        "Mikko",
        "Meikäläinen",
        "140680-9239",
        "",
        "",
        "",
        "",
        null,
        "",
        null,
        restrictedDetailsEnabled = false,
    )

abstract class AbstractDecisionPdfGeneratorTest {
    protected open val templateProvider: ITemplateProvider = EvakaTemplateProvider()

    protected abstract val municipality: String

    protected abstract fun decisionScenarios(): List<DecisionScenario>

    protected open fun feeDecisionScenarios(): List<FeeDecisionScenario> = emptyList()

    protected open fun voucherValueDecisionScenarios(): List<VoucherValueDecisionScenario> =
        emptyList()

    private val pdfGenerator by lazy {
        PdfGenerator(templateProvider, pdfTemplateEngine(municipality))
    }

    protected open val settings: Map<SettingType, String> = emptyMap()

    protected val reasoning =
        PdfReasoning(
            "Tässä yleisen perustelun teksti",
            listOf("Eka yksilöllinen perustelu", "Toka yksilöllinen perustelu"),
        )

    protected open fun reasoningVariants(): List<PdfReasoning?> = listOf(null)

    protected val child: PersonDTO =
        PersonDTO(
            testChild.id,
            null,
            ExternalIdentifier.SSN.getInstance(testChild.ssn!!),
            false,
            "Kullervo Kyöstinpoika",
            "Pöysti",
            "",
            null,
            "",
            "",
            null,
            testChild.dateOfBirth,
            null,
            "Kuusikallionrinne 26 A 4",
            "02270",
            "Espoo",
            "",
            "",
        )

    private val restrictedChild: PersonDTO
        get() = child.copy(restrictedDetailsEnabled = true)

    protected val manager: UnitManager =
        UnitManager(
            "Pirkko Päiväkodinjohtaja",
            "pirkko.paivakodinjohtaja@example.com",
            "0401231234",
        )

    protected val populatedSettings: Map<SettingType, String> =
        mapOf(
            SettingType.DECISION_MAKER_NAME to "Paula Palvelupäällikkö",
            SettingType.DECISION_MAKER_TITLE to "Asiakaspalvelupäällikkö",
        )

    protected val standardServiceNeed: ServiceNeed =
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

    protected val serviceNeedWithoutOption: ServiceNeed =
        ServiceNeed(
            startTime = "08:00",
            endTime = "16:00",
            shiftCare = false,
            partTime = false,
            serviceNeedOption = null,
        )

    protected val finnishAndSwedish: Set<OfficialLanguage> =
        setOf(OfficialLanguage.FI, OfficialLanguage.SV)

    protected val emptyAddressHead: PersonDetailed =
        PersonDetailed(
            PersonId(UUID.randomUUID()),
            LocalDate.of(1982, 3, 31),
            null,
            "Maija",
            "Meikäläinen",
            "310382-956D",
            "",
            "",
            "",
            "",
            null,
            "",
            null,
            restrictedDetailsEnabled = false,
        )

    @TestFactory
    fun applicationDecisionPdfs(): List<DynamicTest> =
        decisionScenarios().flatMap { scenario ->
            val effectiveSettings = scenario.settings ?: settings
            val scenarioChild = if (scenario.restrictedDetails) restrictedChild else child
            scenario.languages.flatMap { lang ->
                val decision =
                    scenario.customize(
                        createValidDecision(
                            type = scenario.decisionType,
                            unit =
                                createValidDecisionUnit(
                                    providerType = scenario.providerType,
                                    language = unitLanguage(lang),
                                ),
                        )
                    )
                reasoningVariants().map { reasoningOrNull ->
                    val variant = if (reasoningOrNull == null) "vanha" else "uusi"
                    val label = "${scenario.name}_${lang.name}_$variant"
                    DynamicTest.dynamicTest(label) {
                        val bytes =
                            createDecisionPdf(
                                templateProvider,
                                pdfGenerator,
                                effectiveSettings,
                                decision,
                                scenarioChild,
                                scenario.isTransferApplication,
                                scenario.serviceNeed,
                                lang,
                                manager,
                                manager,
                                reasoningOrNull,
                            )
                        assertNotNull(bytes)
                        writeTempFile("decision_${municipality}_$label", bytes)
                    }
                }
            }
        }

    @TestFactory
    fun feeDecisionPdfs(): List<DynamicTest> =
        feeDecisionScenarios().flatMap { scenario ->
            val effectiveSettings = scenario.settings ?: settings
            val decision =
                scenario.customize(createValidFeeDecision(municipality, scenario.decisionType))
            scenario.languages.map { lang ->
                val label = "${scenario.name}_${lang.name}"
                DynamicTest.dynamicTest(label) {
                    val bytes =
                        pdfGenerator.generateFeeDecisionPdf(
                            FeeDecisionPdfData(decision, effectiveSettings, lang)
                        )
                    assertNotNull(bytes)
                    writeTempFile("fee_decision_${municipality}_$label", bytes)
                }
            }
        }

    @TestFactory
    fun voucherValueDecisionPdfs(): List<DynamicTest> =
        voucherValueDecisionScenarios().flatMap { scenario ->
            val effectiveSettings = scenario.settings ?: settings
            val decision =
                scenario.customize(
                    createValidVoucherValueDecision(municipality, scenario.decisionType)
                )
            scenario.languages.map { lang ->
                val label = "${scenario.name}_${lang.name}"
                DynamicTest.dynamicTest(label) {
                    val bytes =
                        pdfGenerator.generateVoucherValueDecisionPdf(
                            VoucherValueDecisionPdfData(decision, effectiveSettings, lang)
                        )
                    assertNotNull(bytes)
                    writeTempFile("voucher_value_decision_${municipality}_$label", bytes)
                }
            }
        }

    private fun unitLanguage(lang: OfficialLanguage): Language =
        if (lang == OfficialLanguage.SV) Language.sv else Language.fi

    protected fun feeEdgeScenarios(): List<FeeDecisionScenario> =
        listOf(
            FeeDecisionScenario(
                "fee_no_income",
                customize = {
                    it.copy(
                        headOfFamilyIncome = null,
                        partnerIncome = null,
                        children = it.children.map { c -> c.copy(childIncome = null) },
                    )
                },
            ),
            FeeDecisionScenario(
                "fee_no_partner",
                customize = { it.copy(partner = null, partnerIncome = null) },
            ),
            FeeDecisionScenario(
                "fee_empty_address",
                customize = { it.copy(headOfFamily = emptyAddressHead) },
            ),
        )

    protected fun voucherEdgeScenarios(): List<VoucherValueDecisionScenario> =
        listOf(
            VoucherValueDecisionScenario(
                "voucher_value_no_income",
                customize = {
                    it.copy(headOfFamilyIncome = null, partnerIncome = null, childIncome = null)
                },
            ),
            VoucherValueDecisionScenario(
                "voucher_value_no_partner",
                customize = { it.copy(partner = null, partnerIncome = null) },
            ),
            VoucherValueDecisionScenario(
                "voucher_value_empty_address",
                customize = { it.copy(headOfFamily = emptyAddressHead) },
            ),
        )

    protected fun writeTempFile(prefix: String, bytes: ByteArray) {
        val file = File.createTempFile("${prefix}_", ".pdf")
        FileOutputStream(file).use { it.write(bytes) }
        logger.debug { "Generated PDF to ${file.absolutePath}" }
    }
}

data class DecisionScenario(
    val name: String,
    val decisionType: DecisionType,
    val languages: Set<OfficialLanguage> = setOf(OfficialLanguage.FI),
    val providerType: ProviderType = ProviderType.MUNICIPAL,
    val isTransferApplication: Boolean = false,
    val restrictedDetails: Boolean = false,
    val settings: Map<SettingType, String>? = null,
    val serviceNeed: ServiceNeed? = null,
    val customize: (Decision) -> Decision = { it },
)

data class FeeDecisionScenario(
    val name: String,
    val decisionType: FeeDecisionType = FeeDecisionType.NORMAL,
    val languages: Set<OfficialLanguage> = setOf(OfficialLanguage.FI),
    val settings: Map<SettingType, String>? = null,
    val customize: (FeeDecisionDetailed) -> FeeDecisionDetailed = { it },
)

data class VoucherValueDecisionScenario(
    val name: String,
    val decisionType: VoucherValueDecisionType = VoucherValueDecisionType.NORMAL,
    val languages: Set<OfficialLanguage> = setOf(OfficialLanguage.FI),
    val settings: Map<SettingType, String>? = null,
    val customize: (VoucherValueDecisionDetailed) -> VoucherValueDecisionDetailed = { it },
)

private fun createValidDecisionUnit(
    providerType: ProviderType = ProviderType.MUNICIPAL,
    language: Language = Language.fi,
): DecisionUnit =
    DecisionUnit(
        DaycareId(UUID.randomUUID()),
        "Test Daycare",
        "Test Daycare",
        "Test Daycare Preschool",
        "Test Manager",
        "Test Street 1",
        "00000",
        "Test City",
        "+358 0000000",
        "Test Decision Handler",
        "Test Street 1, 00000 Test City",
        providerType = providerType,
        language = language,
    )

private fun createValidDecision(type: DecisionType, unit: DecisionUnit): Decision =
    Decision(
        id = DecisionId(UUID.randomUUID()),
        createdBy = "John Doe",
        type = type,
        startDate = LocalDate.of(2019, 1, 1),
        endDate = LocalDate.of(2019, 12, 31),
        unit = unit,
        applicationId = ApplicationId(UUID.randomUUID()),
        childId = ChildId(UUID.randomUUID()),
        documentKey = null,
        decisionNumber = 123,
        sentDate = LocalDate.now(),
        status = DecisionStatus.ACCEPTED,
        childName = "Test Child",
        requestedStartDate = LocalDate.of(2019, 1, 1),
        resolved = null,
        resolvedByName = null,
        documentContainsContactInfo = false,
        archivedAt = null,
    )

private fun createValidFeeDecision(
    municipality: String,
    decisionType: FeeDecisionType,
): FeeDecisionDetailed {
    return FeeDecisionDetailed(
        id = testDecision1.id,
        status = testDecision1.status,
        decisionNumber = testDecision1.decisionNumber,
        decisionType = decisionType,
        validDuring = testDecision1.validDuring,
        headOfFamily =
            PersonDetailed(
                id = PersonId(UUID.randomUUID()),
                dateOfBirth = LocalDate.of(1980, 1, 1),
                firstName = "John",
                lastName = "Doe",
                streetAddress = "Testikatu 1",
                postalCode = "00000",
                postOffice = municipality.replaceFirstChar { it.uppercase() },
                restrictedDetailsEnabled = false,
            ),
        partner = samplePartner,
        partnerIsCodebtor = true,
        headOfFamilyIncome = testDecisionIncome,
        partnerIncome = testDecisionIncome,
        familySize = 3,
        feeThresholds = testFeeThresholds.getFeeDecisionThresholds(3),
        approvedAt = HelsinkiDateTime.of(LocalDate.of(2019, 4, 15), LocalTime.of(10, 15, 30)),
        approvedBy = EmployeeWithName(EmployeeId(UUID.randomUUID()), "Pirkko", "Päättäjä"),
        children =
            testDecision1.children.mapIndexed { index, it ->
                FeeDecisionChildDetailed(
                    child =
                        PersonDetailed(
                            id = PersonId(UUID.randomUUID()),
                            dateOfBirth = LocalDate.of(2017, 1, 1),
                            firstName = "Johnny_$index",
                            lastName = "Doe",
                            restrictedDetailsEnabled = false,
                        ),
                    placementType = it.placement.type,
                    placementUnit =
                        UnitData(
                            id = DaycareId(UUID.randomUUID()),
                            name = "Leppäkerttu-konserni, päiväkoti Pupu Tupuna",
                            language = "fi",
                            areaId = AreaId(UUID.randomUUID()),
                            areaName = "Test Area",
                        ),
                    serviceNeedOptionId = it.serviceNeed.optionId,
                    serviceNeedFeeCoefficient = it.serviceNeed.feeCoefficient,
                    serviceNeedDescriptionFi = it.serviceNeed.descriptionFi,
                    serviceNeedDescriptionSv = it.serviceNeed.descriptionSv,
                    serviceNeedMissing = it.serviceNeed.missing,
                    baseFee = it.baseFee,
                    siblingDiscount = it.siblingDiscount,
                    fee = it.fee,
                    feeAlterations = it.feeAlterations,
                    finalFee = it.finalFee,
                    childIncome = testDecisionIncome,
                )
            },
        financeDecisionHandlerFirstName = null,
        financeDecisionHandlerLastName = null,
        documentContainsContactInfo = false,
        archivedAt = null,
    )
}

private fun createValidVoucherValueDecision(
    municipality: String,
    decisionType: VoucherValueDecisionType,
): VoucherValueDecisionDetailed {
    return VoucherValueDecisionDetailed(
        id = VoucherValueDecisionId(testDecision1.id.raw),
        approvedAt = HelsinkiDateTime.of(LocalDate.of(2019, 4, 15), LocalTime.of(10, 15, 30)),
        approvedBy = EmployeeWithName(EmployeeId(UUID.randomUUID()), "Erkki", "Pelimerkki"),
        decisionNumber = testDecision1.decisionNumber,
        decisionType = decisionType,
        status = VoucherValueDecisionStatus.WAITING_FOR_SENDING,
        headOfFamily =
            PersonDetailed(
                id = PersonId(UUID.randomUUID()),
                dateOfBirth = LocalDate.of(1980, 1, 1),
                firstName = "Anselmi Aataminpoika",
                lastName = "Guggenheim",
                streetAddress = "Testikatu 1",
                postalCode = "00000",
                postOffice = municipality.replaceFirstChar { it.uppercase() },
                restrictedDetailsEnabled = false,
            ),
        partner = samplePartner,
        partnerIsCodebtor = true,
        validFrom = LocalDate.of(2020, 1, 1),
        validTo = LocalDate.of(2020, 12, 31),
        financeDecisionHandlerFirstName = null,
        financeDecisionHandlerLastName = null,
        familySize = 3,
        feeThresholds = testFeeThresholds.getFeeDecisionThresholds(3),
        headOfFamilyIncome = testDecisionIncome,
        partnerIncome = testDecisionIncome,
        childIncome = testDecisionIncome,
        child =
            PersonDetailed(
                id = PersonId(UUID.randomUUID()),
                dateOfBirth = LocalDate.of(2017, 1, 1),
                firstName = "Iisakki Anselminpoika",
                lastName = "Guggenheim",
                restrictedDetailsEnabled = false,
            ),
        childAge = 3,
        placement =
            VoucherValueDecisionPlacementDetailed(
                UnitData(
                    id = DaycareId(UUID.randomUUID()),
                    name = "Test Daycare",
                    language = "fi",
                    areaId = AreaId(UUID.randomUUID()),
                    areaName = "Test Area",
                ),
                PlacementType.DAYCARE,
            ),
        serviceNeed =
            VoucherValueDecisionServiceNeed(
                feeCoefficient = BigDecimal("1.00"),
                voucherValueCoefficient = BigDecimal("1.00"),
                feeDescriptionFi = "palveluntarve puuttuu, korkein maksu",
                feeDescriptionSv = "vårdbehovet saknas, högsta avgift",
                voucherValueDescriptionFi = "yli 25h/viikko",
                voucherValueDescriptionSv = "mer än 25 h/vecka",
                missing = false,
            ),
        voucherValue = 120000,
        assistanceNeedCoefficient = BigDecimal("1"),
        baseCoPayment = 900,
        baseValue = 90000,
        coPayment = 12000,
        feeAlterations = emptyList(),
        finalCoPayment = 12000,
        siblingDiscount = 0,
        documentContainsContactInfo = false,
        archivedAt = null,
    )
}
