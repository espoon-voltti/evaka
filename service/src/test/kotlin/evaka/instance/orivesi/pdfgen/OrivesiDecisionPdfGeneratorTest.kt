// SPDX-FileCopyrightText: 2026 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi.pdfgen

import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.pdfgen.AbstractDecisionPdfGeneratorTest
import evaka.core.pdfgen.DecisionScenario
import evaka.core.pdfgen.FeeDecisionScenario
import evaka.core.pdfgen.VoucherValueDecisionScenario
import evaka.core.shared.template.ITemplateProvider
import evaka.instance.orivesi.template.config.OrivesiTemplateProvider

class OrivesiDecisionPdfGeneratorTest : AbstractDecisionPdfGeneratorTest() {
    override val municipality = "orivesi"
    override val templateProvider: ITemplateProvider = OrivesiTemplateProvider()

    override fun decisionScenarios() =
        listOf(
            DecisionScenario("vaka", DecisionType.DAYCARE, serviceNeed = standardServiceNeed),
            DecisionScenario(
                "esiopetus",
                DecisionType.PRESCHOOL,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "vaka_liittyvä",
                DecisionType.PRESCHOOL_DAYCARE,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "vaka_siirto",
                DecisionType.DAYCARE,
                isTransferApplication = true,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "vaka_palse",
                DecisionType.DAYCARE,
                providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
                serviceNeed = standardServiceNeed,
            ),
        )

    override fun feeDecisionScenarios() =
        listOf(
            FeeDecisionScenario("fee_normal"),
            FeeDecisionScenario("fee_relief_rejected", FeeDecisionType.RELIEF_REJECTED),
            FeeDecisionScenario("fee_relief_partly", FeeDecisionType.RELIEF_PARTLY_ACCEPTED),
            FeeDecisionScenario("fee_relief_accepted", FeeDecisionType.RELIEF_ACCEPTED),
        ) + feeEdgeScenarios()

    override fun voucherValueDecisionScenarios() =
        listOf(
            VoucherValueDecisionScenario("voucher_value_normal"),
            VoucherValueDecisionScenario(
                "voucher_value_relief_rejected",
                VoucherValueDecisionType.RELIEF_REJECTED,
            ),
            VoucherValueDecisionScenario(
                "voucher_value_relief_partly",
                VoucherValueDecisionType.RELIEF_PARTLY_ACCEPTED,
            ),
            VoucherValueDecisionScenario(
                "voucher_value_relief_accepted",
                VoucherValueDecisionType.RELIEF_ACCEPTED,
            ),
        ) + voucherEdgeScenarios()
}
