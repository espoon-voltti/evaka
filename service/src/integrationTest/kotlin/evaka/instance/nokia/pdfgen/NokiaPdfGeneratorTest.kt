// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokia.pdfgen

import evaka.core.application.ServiceNeed
import evaka.core.application.ServiceNeedOption
import evaka.core.daycare.UnitManager
import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.invoicing.service.FeeDecisionPdfData
import evaka.core.invoicing.service.VoucherValueDecisionPdfData
import evaka.core.pdfgen.PdfGenerator
import evaka.core.placement.PlacementType
import evaka.core.setting.SettingType
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.template.ITemplateProvider
import evaka.instance.nokia.AbstractNokiaIntegrationTest
import evaka.instance.tampere.decision.service.validChild
import evaka.instance.tampere.decision.service.validDecision
import evaka.instance.tampere.decision.service.validDecisionUnit
import evaka.instance.tampere.invoice.service.validFeeDecision
import evaka.instance.tampere.invoice.service.validFeeDecisionChild
import evaka.instance.tampere.invoice.service.validVoucherValueDecision
import java.util.UUID
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.cartesian.ArgumentSets
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.springframework.beans.factory.annotation.Autowired

private val settings = emptyMap<SettingType, String>()

class NokiaPdfGeneratorTest : AbstractNokiaIntegrationTest() {

    @Autowired private lateinit var templateProvider: ITemplateProvider

    @Autowired private lateinit var pdfGenerator: PdfGenerator

    private fun createDecisionPdfValues() =
        ArgumentSets.argumentsForFirstParameter(supportedDecisionTypes())
            .argumentsForNextParameter(supportedProviderTypes())
            .argumentsForNextParameter(listOf(false, true))

    @CartesianTest
    @CartesianTest.MethodFactory("createDecisionPdfValues")
    fun createDecisionPdf(
        decisionType: DecisionType,
        providerType: ProviderType,
        isTransferApplication: Boolean,
    ) {
        val bytes =
            evaka.core.decision.createDecisionPdf(
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
            "nokia-decision-$decisionType-$providerType${if (isTransferApplication) "-transfer" else ""}.pdf"
        writeReportsFile(filename, bytes)
    }

    @ParameterizedTest
    @MethodSource("supportedPlacementTypes")
    fun generateFeeDecisionPdf(placementType: PlacementType) {
        val decision =
            validFeeDecision(
                children = listOf(validFeeDecisionChild().copy(placementType = placementType))
            )

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "nokia-fee-decision-with-placement-type-$placementType.pdf"
        writeReportsFile(filename, bytes)
    }

    @ParameterizedTest
    @MethodSource("supportedFeeDecisionTypes")
    fun generateFeeDecisionPdf(decisionType: FeeDecisionType) {
        val decision = validFeeDecision().copy(decisionType = decisionType)

        val bytes =
            pdfGenerator.generateFeeDecisionPdf(
                FeeDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "nokia-fee-decision-with-decision-type-$decisionType.pdf"
        writeReportsFile(filename, bytes)
    }

    @ParameterizedTest
    @MethodSource("supportedVoucherValueDecisionTypes")
    fun generateVoucherValueDecisionPdf(decisionType: VoucherValueDecisionType) {
        val decision = validVoucherValueDecision().copy(decisionType = decisionType)

        val bytes =
            pdfGenerator.generateVoucherValueDecisionPdf(
                VoucherValueDecisionPdfData(decision, settings, OfficialLanguage.FI)
            )

        val filename = "nokia-voucher-value-decision-with-decision-type-$decisionType.pdf"
        writeReportsFile(filename, bytes)
    }
}
