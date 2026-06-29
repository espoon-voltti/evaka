// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.pdfgen

import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.pdfgen.AbstractDecisionPdfGeneratorTest
import evaka.core.pdfgen.DecisionScenario
import evaka.core.pdfgen.FeeDecisionScenario
import evaka.core.pdfgen.VoucherValueDecisionScenario

class EspooDecisionPdfGeneratorTest : AbstractDecisionPdfGeneratorTest() {
    override val municipality = "espoo"

    override fun reasoningVariants() = listOf(null, reasoning)

    override fun feeDecisionScenarios() =
        listOf(FeeDecisionScenario("fee_normal", languages = finnishAndSwedish)) +
            feeEdgeScenarios()

    override fun voucherValueDecisionScenarios() =
        listOf(
            VoucherValueDecisionScenario("voucher_value_normal", languages = finnishAndSwedish),
            VoucherValueDecisionScenario(
                "voucher_value_relief",
                VoucherValueDecisionType.RELIEF_ACCEPTED,
            ),
        ) + voucherEdgeScenarios()

    override fun decisionScenarios() =
        listOf(
            DecisionScenario(
                "vaka_siirto",
                DecisionType.DAYCARE,
                languages = finnishAndSwedish,
                isTransferApplication = true,
            ),
            DecisionScenario("vaka", DecisionType.DAYCARE, languages = finnishAndSwedish),
            DecisionScenario(
                "vaka_osa-aika",
                DecisionType.DAYCARE_PART_TIME,
                languages = finnishAndSwedish,
            ),
            DecisionScenario(
                "vaka_palse_osa-aika",
                DecisionType.DAYCARE_PART_TIME,
                languages = finnishAndSwedish,
                providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
            ),
            DecisionScenario(
                "vaka_liittyvä",
                DecisionType.PRESCHOOL_DAYCARE,
                languages = finnishAndSwedish,
            ),
            DecisionScenario("esiopetus", DecisionType.PRESCHOOL, languages = finnishAndSwedish),
            DecisionScenario("valmistava", DecisionType.PREPARATORY_EDUCATION),
            DecisionScenario(
                "vaka_palse",
                DecisionType.DAYCARE,
                languages = finnishAndSwedish,
                providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
            ),
            DecisionScenario("kerho", DecisionType.CLUB),
        )
}
