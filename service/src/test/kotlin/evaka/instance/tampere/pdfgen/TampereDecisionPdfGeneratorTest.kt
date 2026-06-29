// SPDX-FileCopyrightText: 2026 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.pdfgen

import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.pdfgen.AbstractDecisionPdfGeneratorTest
import evaka.core.pdfgen.DecisionScenario
import evaka.core.pdfgen.FeeDecisionScenario
import evaka.core.pdfgen.VoucherValueDecisionScenario
import evaka.core.placement.PlacementType
import evaka.core.shared.template.ITemplateProvider
import evaka.instance.tampere.template.config.TampereTemplateProvider

class TampereDecisionPdfGeneratorTest : AbstractDecisionPdfGeneratorTest() {
    override val municipality = "tampere"
    override val templateProvider: ITemplateProvider = TampereTemplateProvider()
    override val settings = populatedSettings

    override fun decisionScenarios() =
        listOf(
            DecisionScenario("kerho", DecisionType.CLUB),
            DecisionScenario("vaka", DecisionType.DAYCARE, serviceNeed = standardServiceNeed),
            DecisionScenario(
                "vaka_osa-aika",
                DecisionType.DAYCARE_PART_TIME,
                serviceNeed = standardServiceNeed,
            ),
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
                "vaka_liittyvä_kerho",
                DecisionType.PRESCHOOL_CLUB,
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
            DecisionScenario(
                "vaka_turvakielto",
                DecisionType.DAYCARE,
                restrictedDetails = true,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "vaka_ilman_palveluntarveoptiota",
                DecisionType.DAYCARE,
                serviceNeed = serviceNeedWithoutOption,
            ),
            DecisionScenario(
                "vaka_ilman_asetuksia",
                DecisionType.DAYCARE,
                settings = emptyMap(),
                serviceNeed = standardServiceNeed,
            ),
        )

    override fun feeDecisionScenarios() =
        listOf(
            FeeDecisionScenario("fee_normal"),
            FeeDecisionScenario("fee_relief_rejected", FeeDecisionType.RELIEF_REJECTED),
            FeeDecisionScenario("fee_relief_partly", FeeDecisionType.RELIEF_PARTLY_ACCEPTED),
            FeeDecisionScenario("fee_relief_accepted", FeeDecisionType.RELIEF_ACCEPTED),
            FeeDecisionScenario(
                "fee_preschool_club",
                customize = {
                    it.copy(
                        children =
                            listOf(
                                it.children
                                    .first()
                                    .copy(placementType = PlacementType.PRESCHOOL_CLUB)
                            )
                    )
                },
            ),
            FeeDecisionScenario(
                "fee_daycare_and_preschool_club",
                customize = {
                    val first = it.children.first()
                    it.copy(
                        children =
                            listOf(
                                first.copy(placementType = PlacementType.DAYCARE),
                                first.copy(placementType = PlacementType.PRESCHOOL_CLUB),
                            )
                    )
                },
            ),
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
