// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.invoice.service

import evaka.core.invoicing.domain.DecisionIncome
import evaka.core.invoicing.domain.EmployeeWithName
import evaka.core.invoicing.domain.FeeAlteration
import evaka.core.invoicing.domain.FeeAlterationType
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
import evaka.core.invoicing.domain.toFeeAlterationsWithEffects
import evaka.core.invoicing.service.FeeDecisionPdfData
import evaka.core.invoicing.service.VoucherValueDecisionPdfData
import evaka.core.pdfgen.PdfGenerator
import evaka.core.placement.PlacementType
import evaka.core.setting.SettingType
import evaka.core.shared.AreaId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.FeeAlterationId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.OfficialLanguage
import evaka.instance.tampere.AbstractTampereIntegrationTest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.springframework.beans.factory.annotation.Autowired

private val settings =
    mapOf(
        SettingType.DECISION_MAKER_NAME to "Paula Palvelupäällikkö",
        SettingType.DECISION_MAKER_TITLE to "Asiakaspalvelupäällikkö",
    )

internal class PDFServiceTest : AbstractTampereIntegrationTest() {

    @Autowired private lateinit var pdfGenerator: PdfGenerator

    @ParameterizedTest
    @EnumSource(PlacementType::class)
    fun generateFeeDecisionPdfFromPlacementType(placementType: PlacementType) {
        val decision =
            validFeeDecision(
                children = listOf(validFeeDecisionChild().copy(placementType = placementType))
            )

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "PDFServiceTest-fee-decision-placement-type-$placementType.pdf"
        writeReportsFile(filename, bytes)
    }

    @CartesianTest
    fun generateFeeDecisionPdfFromDecisionType(
        @CartesianTest.Enum(
            PlacementType::class,
            mode = CartesianTest.Enum.Mode.INCLUDE,
            names = ["DAYCARE", "PRESCHOOL_CLUB"],
        )
        placementType: PlacementType,
        @CartesianTest.Enum(
            FeeDecisionType::class,
            mode = CartesianTest.Enum.Mode.EXCLUDE,
            names = ["NORMAL"],
        )
        decisionType: FeeDecisionType,
    ) {
        val decision =
            validFeeDecision(listOf(validFeeDecisionChild().copy(placementType = placementType)))
                .copy(decisionType = decisionType)

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename =
            "PDFServiceTest-fee-decision-placement-type-$placementType-decision-type-$decisionType.pdf"
        writeReportsFile(filename, bytes)
    }

    @ParameterizedTest
    @EnumSource(FeeDecisionType::class)
    fun generateFeeDecisionPdfWithPlacementTypeDaycareAndPreschoolClub(
        decisionType: FeeDecisionType
    ) {
        val decision =
            validFeeDecision(
                    children =
                        listOf(
                            validFeeDecisionChild(validChild().copy(firstName = "Matti"))
                                .copy(
                                    placementType = PlacementType.DAYCARE,
                                    serviceNeedDescriptionFi = "Kokopäiväinen",
                                    serviceNeedDescriptionSv = "Kokopäiväinen (sv)",
                                    baseFee = 29500,
                                    fee = 29500,
                                    finalFee = 29500,
                                ),
                            validFeeDecisionChild(validChild().copy(firstName = "Mikko"))
                                .copy(
                                    placementType = PlacementType.PRESCHOOL_CLUB,
                                    serviceNeedDescriptionFi = "Esiopetuksen kerho 1-3h päivässä",
                                    serviceNeedDescriptionSv =
                                        "Esiopetuksen kerho 1-3h päivässä (sv)",
                                    baseFee = 14000,
                                    fee = 7000,
                                    finalFee = 7000,
                                ),
                        )
                )
                .copy(decisionType = decisionType)

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename =
            "PDFServiceTest-fee-decision-placement-type-DAYCARE-and-PRESCHOOL_CLUB-decision-type-$decisionType.pdf"
        writeReportsFile(filename, bytes)
    }

    @ParameterizedTest
    @EnumSource(VoucherValueDecisionType::class)
    fun generateVoucherValueDecisionPdfFromDecisionType(decisionType: VoucherValueDecisionType) {
        val decision = validVoucherValueDecision().copy(decisionType = decisionType)
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfGenerator.generateVoucherValueDecisionPdf(data)

        val filename = "PDFServiceTest-voucher-value-decision-type-$decisionType.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateFeeDecisionPdfWithHeadOfFamilyIncome() {
        val decision = validFeeDecision().copy(headOfFamilyIncome = testDecisionIncome)

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "PDFServiceTest-fee-decision-head-of-family-income.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateFeeDecisionPdfWithPartner() {
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
                        ),
                    partnerIsCodebtor = true,
                )

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "PDFServiceTest-fee-decision-partner.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateFeeDecisionPdfWithPartnerIncome() {
        val decision =
            validFeeDecision()
                .copy(
                    headOfFamilyIncome = validDecisionIncome(314100),
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
                        ),
                    partnerIncome = validDecisionIncome(214000),
                    partnerIsCodebtor = true,
                )

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "PDFServiceTest-fee-decision-partner-income.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateFeeDecisionPdfChildIncome() {
        val decision =
            validFeeDecision()
                .copy(
                    children =
                        listOf(validFeeDecisionChild().copy(childIncome = testDecisionIncome))
                )

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "PDFServiceTest-fee-decision-child-income.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateFeeDecisionPdfWithAllIncomes() {
        val decision =
            validFeeDecision()
                .copy(
                    headOfFamilyIncome = validDecisionIncome(income = 300000),
                    partner =
                        PersonDetailed(
                            PersonId(UUID.randomUUID()),
                            LocalDate.of(1982, 6, 25),
                            null,
                            "Mikko",
                            "Meikäläinen",
                            "250682-983U",
                            "Meikäläisenkuja 6 B 7",
                            "33730",
                            "TAMPERE",
                            "",
                            null,
                            "",
                            null,
                            restrictedDetailsEnabled = false,
                        ),
                    partnerIncome = validDecisionIncome(income = 200000),
                    children =
                        listOf(
                            validFeeDecisionChild()
                                .copy(
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
                                    childIncome = validDecisionIncome(income = 10000),
                                ),
                            validFeeDecisionChild()
                                .copy(
                                    child =
                                        PersonDetailed(
                                            PersonId(UUID.randomUUID()),
                                            LocalDate.of(2018, 1, 1),
                                            null,
                                            "Marko",
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
                                        )
                                ),
                            validFeeDecisionChild()
                                .copy(
                                    child =
                                        PersonDetailed(
                                            PersonId(UUID.randomUUID()),
                                            LocalDate.of(2018, 1, 1),
                                            null,
                                            "Miia",
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
                                    childIncome = validDecisionIncome(income = 25000),
                                ),
                        ),
                )

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "PDFServiceTest-fee-decision-incomes.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateFeeDecisionPdfWithEmptyAddress() {
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
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "PDFServiceTest-fee-decision-empty-address.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateFeeDecisionPdfWithFeeAlterations() {
        val decision =
            validFeeDecision(
                children = listOf(validFeeDecisionChild(feeAlterations = validFeeAlterations()))
            )

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "PDFServiceTest-fee-decision-fee-alterations.pdf"
        writeReportsFile(filename, bytes)
    }

    @ParameterizedTest
    @EnumSource(PlacementType::class)
    fun generateVoucherValueDecisionPdfFromPlacementType(placementType: PlacementType) {
        val decision =
            validVoucherValueDecision(
                placement =
                    validVoucherValueDecisionPlacementDetailed(placementType = placementType)
            )
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfGenerator.generateVoucherValueDecisionPdf(data)

        val filename = "PDFServiceTest-voucher-value-decision-placement-type-$placementType.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateVoucherValueDecisionPdfWithHeadOfFamilyIncome() {
        val decision = validVoucherValueDecision().copy(headOfFamilyIncome = testDecisionIncome)
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfGenerator.generateVoucherValueDecisionPdf(data)

        val filename = "PDFServiceTest-voucher-value-decision-head-of-family-income.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateVoucherValueDecisionPdfWithChildIncome() {
        val decision = validVoucherValueDecision().copy(childIncome = testDecisionIncome)
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfGenerator.generateVoucherValueDecisionPdf(data)

        val filename = "PDFServiceTest-voucher-value-decision-child-income.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateVoucherValueDecisionPdfWithPartner() {
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
                        ),
                    partnerIsCodebtor = true,
                )
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfGenerator.generateVoucherValueDecisionPdf(data)

        val filename = "PDFServiceTest-voucher-value-decision-partner.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateVoucherValueDecisionPdfWithEmptyAddress() {
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

        val bytes = pdfGenerator.generateVoucherValueDecisionPdf(data)

        val filename = "PDFServiceTest-voucher-value-decision-empty-address.pdf"
        writeReportsFile(filename, bytes)
    }

    @Test
    fun generateVoucherValueDecisionPdfWithFeeAlterations() {
        val decision = validVoucherValueDecision(feeAlterations = validFeeAlterations())
        val data = VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)

        val bytes = pdfGenerator.generateVoucherValueDecisionPdf(data)

        val filename = "PDFServiceTest-voucher-value-decision-fee-alterations.pdf"
        writeReportsFile(filename, bytes)
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

private fun validDecisionIncome(income: Int = 314100) =
    DecisionIncome(
        effect = IncomeEffect.INCOME,
        data = mapOf("MAIN_INCOME" to income),
        totalIncome = income,
        totalExpenses = 0,
        total = income,
        worksAtECHA = false,
    )

fun validFeeDecision(children: List<FeeDecisionChildDetailed> = listOf(validFeeDecisionChild())) =
    FeeDecisionDetailed(
        id = FeeDecisionId(UUID.randomUUID()),
        children = children,
        validDuring = FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1)),
        status = FeeDecisionStatus.WAITING_FOR_SENDING,
        decisionNumber = 100200,
        decisionType = FeeDecisionType.NORMAL,
        headOfFamily =
            PersonDetailed(
                PersonId(UUID.randomUUID()),
                LocalDate.of(1982, 3, 31),
                null,
                "Maija",
                "Meikäläinen",
                "310382-956D",
                "Meikäläisenkuja 6 B 7",
                "33730",
                "TAMPERE",
                "",
                null,
                "",
                null,
                restrictedDetailsEnabled = false,
            ),
        partner = null,
        headOfFamilyIncome = null,
        partnerIncome = null,
        familySize = 1 + children.size,
        feeThresholds =
            FeeDecisionThresholds(
                minIncomeThreshold = 387400,
                maxIncomeThreshold = 662640,
                incomeMultiplier = BigDecimal("0.1070"),
                maxFee = 29500,
                minFee = 2800,
            ),
        documentKey = null,
        approvedBy = EmployeeWithName(EmployeeId(UUID.randomUUID()), "Markus", "Maksusihteeri"),
        approvedAt = HelsinkiDateTime.now(),
        sentAt = null,
        financeDecisionHandlerFirstName = null,
        financeDecisionHandlerLastName = null,
        documentContainsContactInfo = false,
        archivedAt = null,
    )

fun validFeeDecisionChild(
    child: PersonDetailed = validChild(),
    feeAlterations: List<FeeAlteration> = emptyList(),
) =
    FeeDecisionChildDetailed(
        child = child,
        placementType = PlacementType.DAYCARE,
        placementUnit =
            UnitData(
                id = DaycareId(UUID.randomUUID()),
                name = "Yksikkö 1",
                areaId = AreaId(UUID.randomUUID()),
                areaName = "Alue 1",
                language = "fi",
            ),
        serviceNeedOptionId = ServiceNeedOptionId(UUID.randomUUID()),
        serviceNeedFeeCoefficient = BigDecimal.ONE,
        serviceNeedDescriptionFi = "Kokopäiväinen",
        serviceNeedDescriptionSv = "Kokopäiväinen (sv)",
        serviceNeedMissing = false,
        baseFee = 29500,
        siblingDiscount = 0,
        fee = 29500,
        feeAlterations = toFeeAlterationsWithEffects(29500, feeAlterations),
        finalFee = 29500,
        childIncome = null,
    )

private fun validChild() =
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
    )

fun validVoucherValueDecision(
    child: PersonDetailed = validChild(),
    placement: VoucherValueDecisionPlacementDetailed =
        validVoucherValueDecisionPlacementDetailed(PlacementType.DAYCARE),
    feeAlterations: List<FeeAlteration> = emptyList(),
) =
    VoucherValueDecisionDetailed(
        id = VoucherValueDecisionId(UUID.randomUUID()),
        validFrom = LocalDate.now(),
        validTo = LocalDate.now().plusYears(1), // validTo is nullable but actually never is null
        status = VoucherValueDecisionStatus.WAITING_FOR_SENDING,
        decisionNumber = 100200,
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
                "33730",
                "TAMPERE",
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
        familySize = 2,
        feeThresholds =
            FeeDecisionThresholds(
                minIncomeThreshold = 387400,
                maxIncomeThreshold = 662640,
                incomeMultiplier = BigDecimal("0.1070"),
                maxFee = 29500,
                minFee = 2800,
            ),
        child = child,
        placement = placement,
        serviceNeed =
            VoucherValueDecisionServiceNeed(
                feeCoefficient = BigDecimal.ONE,
                voucherValueCoefficient = BigDecimal.ONE,
                feeDescriptionFi = "Kokopäiväinen",
                feeDescriptionSv = "Kokopäiväinen (sv)",
                voucherValueDescriptionFi = "Kokopäiväinen",
                voucherValueDescriptionSv = "Kokopäiväinen (sv)",
                missing = false,
            ),
        baseCoPayment = 29500,
        siblingDiscount = 0,
        coPayment = 29500,
        feeAlterations = toFeeAlterationsWithEffects(29500, feeAlterations),
        finalCoPayment = 29500,
        baseValue = 130200,
        childAge = 1,
        assistanceNeedCoefficient = BigDecimal.ZERO,
        voucherValue = 130200,
        documentKey = null,
        approvedBy = EmployeeWithName(EmployeeId(UUID.randomUUID()), "Markus", "Maksusihteeri"),
        approvedAt = HelsinkiDateTime.now(),
        sentAt = null,
        created = HelsinkiDateTime.now(),
        financeDecisionHandlerFirstName = null,
        financeDecisionHandlerLastName = null,
        documentContainsContactInfo = false,
        archivedAt = null,
    )

private fun validVoucherValueDecisionPlacementDetailed(placementType: PlacementType) =
    VoucherValueDecisionPlacementDetailed(
        UnitData(
            DaycareId(UUID.randomUUID()),
            name = "Touhula Ylöjärvi",
            areaId = AreaId(UUID.randomUUID()),
            areaName = "Länsi",
            language = "fi",
        ),
        type = placementType,
    )

private fun validFeeAlterations(childId: ChildId = ChildId(UUID.randomUUID())) =
    FeeAlterationType.entries.flatMap {
        listOf(
            FeeAlteration(
                id = FeeAlterationId(UUID.randomUUID()),
                personId = childId,
                type = it,
                amount = 5,
                isAbsolute = false,
                validFrom = LocalDate.now(),
                validTo = null,
                notes = "",
                modifiedAt = null,
                modifiedBy = null,
            ),
            FeeAlteration(
                id = FeeAlterationId(UUID.randomUUID()),
                personId = childId,
                type = it,
                amount = 50,
                isAbsolute = true,
                validFrom = LocalDate.now(),
                validTo = null,
                notes = "",
                modifiedAt = null,
                modifiedBy = null,
            ),
        )
    }
