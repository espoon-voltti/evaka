// SPDX-FileCopyrightText: 2026 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.vesilahti.pdfgen

import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.pdfgen.AbstractDecisionPdfGeneratorTest
import evaka.core.pdfgen.DecisionScenario
import evaka.core.pdfgen.FeeDecisionScenario
import evaka.core.shared.template.ITemplateProvider
import evaka.instance.vesilahti.template.config.VesilahtiTemplateProvider

class VesilahtiDecisionPdfGeneratorTest : AbstractDecisionPdfGeneratorTest() {
    override val municipality = "vesilahti"
    override val templateProvider: ITemplateProvider = VesilahtiTemplateProvider()

    override fun decisionScenarios() =
        listOf(
            DecisionScenario("vaka", DecisionType.DAYCARE, serviceNeed = standardServiceNeed),
            DecisionScenario(
                "vaka_liittyvä",
                DecisionType.PRESCHOOL_DAYCARE,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "valmistava",
                DecisionType.PREPARATORY_EDUCATION,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "vaka_siirto",
                DecisionType.DAYCARE,
                isTransferApplication = true,
                serviceNeed = standardServiceNeed,
            ),
        )

    override fun feeDecisionScenarios() =
        listOf(
            FeeDecisionScenario("fee_normal"),
            FeeDecisionScenario("fee_relief_rejected", FeeDecisionType.RELIEF_REJECTED),
            FeeDecisionScenario("fee_relief_accepted", FeeDecisionType.RELIEF_ACCEPTED),
        ) + feeEdgeScenarios()
}
