// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.pdfgen

import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.pdfgen.AbstractDecisionPdfGeneratorTest
import evaka.core.pdfgen.DecisionScenario
import evaka.core.pdfgen.FeeDecisionScenario
import evaka.core.pdfgen.VoucherValueDecisionScenario
import evaka.core.shared.domain.OfficialLanguage

private val finnishAndSwedish = setOf(OfficialLanguage.FI, OfficialLanguage.SV)

class EspooDecisionPdfGeneratorTest : AbstractDecisionPdfGeneratorTest() {
    override val municipality = "espoo"

    override fun feeDecisionScenarios() =
        listOf(FeeDecisionScenario("fee_normal", languages = finnishAndSwedish))

    override fun voucherValueDecisionScenarios() =
        listOf(
            VoucherValueDecisionScenario("voucher_value_normal", languages = finnishAndSwedish),
            VoucherValueDecisionScenario(
                "voucher_value_relief",
                VoucherValueDecisionType.RELIEF_ACCEPTED,
            ),
        )

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
                isVoucher = true,
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
                isVoucher = true,
            ),
            DecisionScenario("kerho", DecisionType.CLUB),
        )
}
