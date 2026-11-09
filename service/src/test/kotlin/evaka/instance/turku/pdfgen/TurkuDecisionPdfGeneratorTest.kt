// SPDX-FileCopyrightText: 2026 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.pdfgen

import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.FeeAlterationType
import evaka.core.invoicing.domain.FeeAlterationWithEffect
import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.pdfgen.AbstractDecisionPdfGeneratorTest
import evaka.core.pdfgen.DecisionScenario
import evaka.core.pdfgen.FeeDecisionScenario
import evaka.core.pdfgen.VoucherValueDecisionScenario
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.template.ITemplateProvider
import evaka.instance.turku.TurkuTemplateProvider
import java.time.LocalDate

class TurkuDecisionPdfGeneratorTest : AbstractDecisionPdfGeneratorTest() {
    override val municipality = "turku"
    override val templateProvider: ITemplateProvider = TurkuTemplateProvider()
    override val settings = populatedSettings

    override fun reasoningVariants() = listOf(null, reasoning)

    override fun decisionScenarios() =
        listOf(
            DecisionScenario("kerho", DecisionType.CLUB, languages = finnishAndSwedish),
            DecisionScenario(
                "vaka",
                DecisionType.DAYCARE,
                languages = finnishAndSwedish,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "vaka_osa-aika",
                DecisionType.DAYCARE_PART_TIME,
                languages = finnishAndSwedish,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "vaka_liittyvä",
                DecisionType.PRESCHOOL_DAYCARE,
                languages = finnishAndSwedish,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "vaka_liittyvä_kerho",
                DecisionType.PRESCHOOL_CLUB,
                languages = finnishAndSwedish,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "esiopetus",
                DecisionType.PRESCHOOL,
                languages = finnishAndSwedish,
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
                "esiopetus_jarjestelma",
                DecisionType.PRESCHOOL,
                serviceNeed = standardServiceNeed,
                customize = { it.copy(decidedByName = "eVaka") },
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
            FeeDecisionScenario(
                "fee_valid_to",
                customize = {
                    it.copy(
                        validDuring = FiniteDateRange(LocalDate.now(), LocalDate.now().plusYears(1))
                    )
                },
            ),
            FeeDecisionScenario("fee_relief_rejected", FeeDecisionType.RELIEF_REJECTED),
            FeeDecisionScenario("fee_relief_partly", FeeDecisionType.RELIEF_PARTLY_ACCEPTED),
            FeeDecisionScenario("fee_relief_accepted", FeeDecisionType.RELIEF_ACCEPTED),
            FeeDecisionScenario(
                "fee_zero",
                languages = finnishAndSwedish,
                customize = { it.withOneChild(fee = 0) },
            ),
            FeeDecisionScenario(
                "fee_max",
                languages = finnishAndSwedish,
                customize = { it.withOneChild(fee = 28500) },
            ),
            FeeDecisionScenario(
                "fee_alteration",
                languages = finnishAndSwedish,
                customize = {
                    it.withOneChild(
                        fee = 28500,
                        feeAlterations =
                            listOf(
                                FeeAlterationWithEffect(FeeAlterationType.RELIEF, 50, false, -14250)
                            ),
                    )
                },
            ),
            FeeDecisionScenario(
                "fee_zero_long_name",
                languages = finnishAndSwedish,
                customize = { it.withOneChild(fee = 0, lastName = "Virtanen-Korhonen-Lehtinen") },
            ),
            FeeDecisionScenario(
                "fee_zero_two_children",
                languages = finnishAndSwedish,
                customize = {
                    val child = it.withOneChild(fee = 0).children.single()
                    it.copy(children = listOf(child, child))
                },
            ),
        ) + feeEdgeScenarios()

    override fun voucherValueDecisionScenarios() =
        listOf(
            VoucherValueDecisionScenario("voucher_value_normal"),
            VoucherValueDecisionScenario(
                "voucher_value_valid_to",
                customize = { it.copy(validTo = LocalDate.now().plusYears(1)) },
            ),
            VoucherValueDecisionScenario(
                "voucher_value_relief_accepted",
                VoucherValueDecisionType.RELIEF_ACCEPTED,
                customize = {
                    it.copy(
                        feeAlterations =
                            listOf(
                                FeeAlterationWithEffect(FeeAlterationType.RELIEF, 50, false, -10800)
                            )
                    )
                },
            ),
            VoucherValueDecisionScenario(
                "voucher_value_relief_partly",
                VoucherValueDecisionType.RELIEF_PARTLY_ACCEPTED,
                customize = {
                    it.copy(
                        feeAlterations =
                            listOf(
                                FeeAlterationWithEffect(FeeAlterationType.RELIEF, 50, false, -100)
                            )
                    )
                },
            ),
            VoucherValueDecisionScenario(
                "voucher_value_relief_rejected",
                VoucherValueDecisionType.RELIEF_REJECTED,
            ),
        ) + voucherEdgeScenarios()

    private fun FeeDecisionDetailed.withOneChild(
        fee: Int,
        feeAlterations: List<FeeAlterationWithEffect> = emptyList(),
        lastName: String = "Virtanen",
    ): FeeDecisionDetailed =
        copy(
            children =
                children.take(1).map { child ->
                    child.copy(
                        child = child.child.copy(firstName = "Matti", lastName = lastName),
                        serviceNeedDescriptionFi =
                            "Varhaiskasvatus enintään 35h/vko (enintään 147h/kk)",
                        serviceNeedDescriptionSv =
                            "Småbarnspedagogik högst 35 h/vecka (högst 147 h/månad)",
                        fee = fee,
                        feeAlterations = feeAlterations,
                        finalFee = fee + feeAlterations.sumOf { it.effect },
                    )
                }
        )
}
