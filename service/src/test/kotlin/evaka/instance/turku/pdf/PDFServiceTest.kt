// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.pdf

import evaka.core.invoicing.domain.DecisionIncome
import evaka.core.invoicing.domain.EmployeeWithName
import evaka.core.invoicing.domain.FeeAlterationType
import evaka.core.invoicing.domain.FeeAlterationWithEffect
import evaka.core.invoicing.domain.FeeDecisionChildDetailed
import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionThresholds
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.invoicing.domain.PersonDetailed
import evaka.core.invoicing.domain.UnitData
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionPlacementDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionServiceNeed
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.invoicing.service.FeeDecisionPdfData
import evaka.core.invoicing.service.VoucherValueDecisionPdfData
import evaka.core.pdfgen.PdfGenerator
import evaka.core.placement.PlacementType
import evaka.core.setting.SettingType
import evaka.core.shared.AreaId
import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.config.PDFConfig
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.OfficialLanguage
import evaka.instance.turku.TurkuTemplateProvider
import java.io.FileOutputStream
import java.math.BigDecimal
import java.nio.file.Paths
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

private val reportsPath: String = "${Paths.get("build").toAbsolutePath()}/reports"

private val settings =
    mapOf(
        SettingType.DECISION_MAKER_NAME to "Paula Palvelupäällikkö",
        SettingType.DECISION_MAKER_TITLE to "Asiakaspalvelupäällikkö",
    )

@Tag("PDFGenerationTest")
internal class PDFServiceTest {
    private lateinit var pdfService: PdfGenerator

    @BeforeEach
    fun setup() {
        pdfService = PdfGenerator(TurkuTemplateProvider(), PDFConfig.templateEngine("turku"))
    }

    @Test
    fun generateFeeDecisionPdf() {
        val decision = validFeeDecision()

        val bytes =
            pdfService.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filepath = "$reportsPath/PDFServiceTest-fee-decision.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateFeeDecisionPdfWithIncome() {
        val decision = validFeeDecision().copy(headOfFamilyIncome = testDecisionIncome)

        val bytes =
            pdfService.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filepath = "$reportsPath/PDFServiceTest-fee-decision-head-of-family-income.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @ParameterizedTest
    @EnumSource(FeeDecisionType::class)
    fun generateFeeDecisionPdfType(decisionType: FeeDecisionType) {
        val decision = validFeeDecision().copy(decisionType = decisionType)

        val bytes =
            pdfService.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filepath = "$reportsPath/PDFServiceTest-fee-decision-type-$decisionType.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateFeeDecisionPdfPartner() {
        val decision =
            validFeeDecision()
                .copy(
                    partner =
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
                )

        val bytes =
            pdfService.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filepath = "$reportsPath/PDFServiceTest-fee-decision-partner.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateFeeDecisionPdfValidTo() {
        val validFrom = LocalDate.now()
        val validTo = validFrom.plusYears(1)
        val decision = validFeeDecision().copy(validDuring = FiniteDateRange(validFrom, validTo))

        val bytes =
            pdfService.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filepath = "$reportsPath/PDFServiceTest-fee-decision-valid-to.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateFeeDecisionPdfEmptyAddress() {
        val decision =
            validFeeDecision()
                .copy(
                    headOfFamily =
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
                )

        val bytes =
            pdfService.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filepath = "$reportsPath/PDFServiceTest-fee-decision-empty-address.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateVoucherValueDecisionPdf() {
        val decision = validVoucherValueDecision()
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfService.generateVoucherValueDecisionPdf(data)

        val filepath = "$reportsPath/PDFServiceTest-voucher-value-decision.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateVoucherValueDecisionPdfWithIncome() {
        val decision = validVoucherValueDecision().copy(headOfFamilyIncome = testDecisionIncome)
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfService.generateVoucherValueDecisionPdf(data)

        val filepath =
            "$reportsPath/PDFServiceTest-voucher-value-decision-head-of-family-income.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateVoucherValueDecisionPdfPartner() {
        val decision =
            validVoucherValueDecision()
                .copy(
                    partner =
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
                )
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfService.generateVoucherValueDecisionPdf(data)

        val filepath = "$reportsPath/PDFServiceTest-voucher-value-decision-partner.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateVoucherValueDecisionPdfValidTo() {
        val decision = validVoucherValueDecision().copy(validTo = LocalDate.now().plusYears(1))
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfService.generateVoucherValueDecisionPdf(data)

        val filepath = "$reportsPath/PDFServiceTest-voucher-value-decision-valid-to.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateVoucherValueDecisionPdfEmptyAddress() {
        val decision =
            validVoucherValueDecision()
                .copy(
                    headOfFamily =
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
                )
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfService.generateVoucherValueDecisionPdf(data)

        val filepath = "$reportsPath/PDFServiceTest-voucher-value-decision-empty-address.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateReliefAcceptedVoucherValueDecisionPdfValidTo() {
        val decision =
            validVoucherValueDecision()
                .copy(
                    validTo = LocalDate.now().plusYears(1),
                    decisionType = VoucherValueDecisionType.RELIEF_ACCEPTED,
                    feeAlterations =
                        listOf(FeeAlterationWithEffect(FeeAlterationType.RELIEF, 50, false, -10800)),
                )
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfService.generateVoucherValueDecisionPdf(data)

        val filepath =
            "$reportsPath/PDFServiceTest-relief-accepted-voucher-value-decision-valid-to.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateReliefPartlyAcceptedVoucherValueDecisionPdfValidTo() {
        val decision =
            validVoucherValueDecision()
                .copy(
                    validTo = LocalDate.now().plusYears(1),
                    decisionType = VoucherValueDecisionType.RELIEF_PARTLY_ACCEPTED,
                    feeAlterations =
                        listOf(FeeAlterationWithEffect(FeeAlterationType.RELIEF, 50, false, -100)),
                )
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfService.generateVoucherValueDecisionPdf(data)

        val filepath =
            "$reportsPath/PDFServiceTest-relief-partly-accepted-voucher-value-decision-valid-to.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }

    @Test
    fun generateReliefRejectedVoucherValueDecisionPdfValidTo() {
        val decision =
            validVoucherValueDecision()
                .copy(
                    validTo = LocalDate.now().plusYears(1),
                    decisionType = VoucherValueDecisionType.RELIEF_REJECTED,
                )
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfService.generateVoucherValueDecisionPdf(data)

        val filepath =
            "$reportsPath/PDFServiceTest-relief-rejected-voucher-value-decision-valid-to.pdf"
        FileOutputStream(filepath).use { it.write(bytes) }
    }
}

private val testDecisionIncome =
    DecisionIncome(
        effect = IncomeEffect.INCOME,
        data = mapOf("MAIN_INCOME" to 314100),
        totalIncome = 314100,
        totalExpenses = 0,
        total = 314100,
        worksAtECHA = false,
    )

private fun validFeeDecision() =
    FeeDecisionDetailed(
        FeeDecisionId(UUID.randomUUID()),
        children =
            listOf(
                FeeDecisionChildDetailed(
                    child =
                        PersonDetailed(
                            PersonId(UUID.randomUUID()),
                            LocalDate.of(2018, 1, 1),
                            null,
                            "Matti",
                            "Meikäläinen",
                            null,
                            "",
                            "",
                            "",
                            "",
                            null,
                            "",
                            null,
                            restrictedDetailsEnabled = false,
                        ),
                    placementType = PlacementType.DAYCARE,
                    placementUnit =
                        UnitData(
                            DaycareId(UUID.randomUUID()),
                            name = "Yksikkö 1",
                            areaId = AreaId(UUID.randomUUID()),
                            areaName = "Alue 1",
                            language = "fi",
                        ),
                    serviceNeedFeeCoefficient = BigDecimal.ONE,
                    serviceNeedDescriptionFi =
                        """
                        Palveluntarve 1 Palveluntarve 1 pitkänimi Palveluntarve 1 pitkänimi Palveluntarve 1 pitkänimi Palveluntarve 1 pitkänimi Palveluntarve1 pitkänimi Palveluntarve 1 pitkänimi Palveluntarve                        
                        """
                            .trimIndent(),
                    serviceNeedDescriptionSv = "Palveluntarve 1 (sv)",
                    serviceNeedOptionId = ServiceNeedOptionId(UUID.randomUUID()),
                    serviceNeedMissing = false,
                    baseFee = 21600,
                    siblingDiscount = 1,
                    fee = 21600,
                    feeAlterations =
                        listOf(
                            FeeAlterationWithEffect(FeeAlterationType.RELIEF, 50, false, -10800)
                        ),
                    finalFee = 10800,
                    childIncome = null,
                ),
                FeeDecisionChildDetailed(
                    child =
                        PersonDetailed(
                            PersonId(UUID.randomUUID()),
                            LocalDate.of(2020, 1, 1),
                            null,
                            "Liisa",
                            "Meikäläinen",
                            null,
                            "",
                            "",
                            "",
                            "",
                            null,
                            "",
                            null,
                            restrictedDetailsEnabled = false,
                        ),
                    placementType = PlacementType.DAYCARE,
                    placementUnit =
                        UnitData(
                            DaycareId(UUID.randomUUID()),
                            name = "Yksikkö 2",
                            areaId = AreaId(UUID.randomUUID()),
                            areaName = "Alue 2",
                            language = "fi",
                        ),
                    serviceNeedFeeCoefficient = BigDecimal.ONE,
                    serviceNeedDescriptionFi = "Palveluntarve 2 (fi)",
                    serviceNeedDescriptionSv = "Palveluntarve 2 (sv)",
                    serviceNeedOptionId = ServiceNeedOptionId(UUID.randomUUID()),
                    serviceNeedMissing = false,
                    baseFee = 10000,
                    siblingDiscount = 5000,
                    fee = 9600,
                    feeAlterations =
                        listOf(
                            FeeAlterationWithEffect(FeeAlterationType.DISCOUNT, 50, true, -5000)
                        ),
                    finalFee = 5000,
                    childIncome = null,
                ),
            ),
        validDuring = FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1)),
        FeeDecisionStatus.WAITING_FOR_SENDING,
        decisionNumber = 123456,
        FeeDecisionType.NORMAL,
        headOfFamily =
            PersonDetailed(
                PersonId(UUID.randomUUID()),
                LocalDate.of(1982, 3, 31),
                null,
                "Maija",
                "Meikäläinen",
                "310382-956D",
                "Meikäläisenkuja 6 B 7",
                "20100",
                "TURKU",
                "",
                null,
                "",
                null,
                restrictedDetailsEnabled = false,
            ),
        partner = null,
        headOfFamilyIncome = null,
        partnerIncome = null,
        familySize = 1,
        FeeDecisionThresholds(
            minIncomeThreshold = 1,
            maxIncomeThreshold = 2,
            incomeMultiplier = BigDecimal.ONE,
            maxFee = 1,
            minFee = 1,
        ),
        documentKey = null,
        approvedBy = EmployeeWithName(EmployeeId(UUID.randomUUID()), "Markus", "Maksusihteeri"),
        approvedAt = HelsinkiDateTime.now(),
        sentAt = null,
        financeDecisionHandlerFirstName = "Jonne",
        financeDecisionHandlerLastName = "Johtaja",
        documentContainsContactInfo = false,
        archivedAt = null,
    )

private fun validVoucherValueDecision() =
    VoucherValueDecisionDetailed(
        VoucherValueDecisionId(UUID.randomUUID()),
        LocalDate.now(),
        LocalDate.now().plusYears(1),
        VoucherValueDecisionStatus.WAITING_FOR_SENDING,
        decisionNumber = null,
        decisionType = VoucherValueDecisionType.NORMAL,
        headOfFamily =
            PersonDetailed(
                PersonId(UUID.randomUUID()),
                LocalDate.of(1982, 3, 31),
                null,
                "Maija",
                "Meikäläinen",
                "310382-956D",
                "Meikäläisenkuja 6 B 7",
                "20100",
                "TURKU",
                "",
                null,
                "",
                null,
                restrictedDetailsEnabled = false,
            ),
        partner = null,
        headOfFamilyIncome = null,
        partnerIncome = null,
        childIncome = null,
        familySize = 1,
        FeeDecisionThresholds(
            minIncomeThreshold = 1,
            maxIncomeThreshold = 2,
            incomeMultiplier = BigDecimal.ONE,
            maxFee = 1,
            minFee = 1,
        ),
        PersonDetailed(
            PersonId(UUID.randomUUID()),
            LocalDate.of(2018, 1, 1),
            null,
            "Olli",
            "Turkulainen",
            null,
            "",
            "",
            "",
            "",
            null,
            "",
            null,
            restrictedDetailsEnabled = false,
        ),
        VoucherValueDecisionPlacementDetailed(
            UnitData(
                DaycareId(UUID.randomUUID()),
                name = "Jäkärlän kerho",
                areaId = AreaId(UUID.randomUUID()),
                areaName = "Etelä",
                language = "fi",
            ),
            type = PlacementType.DAYCARE,
        ),
        VoucherValueDecisionServiceNeed(
            feeCoefficient = BigDecimal.ONE,
            voucherValueCoefficient = BigDecimal.ONE,
            feeDescriptionFi = "eka",
            feeDescriptionSv = "toka",
            voucherValueDescriptionFi =
                """
                kolmas neljäskolmas neljäskolmas neljäskolmas neljäskolmas neljäskolmas neljäskolmas neljäskolmas neljäskolmas
                """
                    .trimIndent(),
            voucherValueDescriptionSv = "neljäs",
            missing = false,
        ),
        baseCoPayment = 1,
        siblingDiscount = 1,
        coPayment = 1,
        feeAlterations = emptyList(),
        finalCoPayment = 1,
        baseValue = 1,
        childAge = 1,
        assistanceNeedCoefficient = BigDecimal.ONE,
        voucherValue = 1,
        documentKey = null,
        approvedBy = EmployeeWithName(EmployeeId(UUID.randomUUID()), "Markus", "Maksusihteeri"),
        approvedAt = HelsinkiDateTime.now(),
        sentAt = null,
        created = HelsinkiDateTime.now(),
        financeDecisionHandlerFirstName = "Satu",
        financeDecisionHandlerLastName = "Leppänen",
        documentContainsContactInfo = false,
        archivedAt = null,
    )
