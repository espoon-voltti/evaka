// SPDX-FileCopyrightText: 2026 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.pdfgen

import evaka.core.application.ServiceNeed
import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.FeeAlterationType
import evaka.core.invoicing.domain.FeeAlterationWithEffect
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.pdfgen.AbstractDecisionPdfGeneratorTest
import evaka.core.pdfgen.DecisionScenario
import evaka.core.pdfgen.FeeDecisionScenario
import evaka.core.pdfgen.VoucherValueDecisionScenario
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.template.ITemplateProvider
import evaka.instance.oulu.template.config.OuluTemplateProvider
import java.time.LocalDate

private val serviceNeedWithoutOption =
    ServiceNeed(
        startTime = "08:00",
        endTime = "16:00",
        shiftCare = false,
        partTime = false,
        serviceNeedOption = null,
    )

class OuluDecisionPdfGeneratorTest : AbstractDecisionPdfGeneratorTest() {
    override val municipality = "oulu"
    override val templateProvider: ITemplateProvider = OuluTemplateProvider()
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
                "vaka_liittyvä",
                DecisionType.PRESCHOOL_DAYCARE,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario(
                "vaka_liittyvä_kerho",
                DecisionType.PRESCHOOL_CLUB,
                serviceNeed = standardServiceNeed,
            ),
            DecisionScenario("esiopetus", DecisionType.PRESCHOOL),
            DecisionScenario("valmistava", DecisionType.PREPARATORY_EDUCATION),
            DecisionScenario(
                "valmistava_ilman_palveluntarveoptiota",
                DecisionType.PREPARATORY_EDUCATION,
                serviceNeed = serviceNeedWithoutOption,
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
                customize = { it.copy(createdBy = "eVaka") },
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
        )

    override fun voucherValueDecisionScenarios() =
        listOf(
            VoucherValueDecisionScenario("voucher_value_normal"),
            VoucherValueDecisionScenario(
                "voucher_value_empty_address",
                customize = { it.copy(headOfFamily = emptyAddressHead) },
            ),
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
        )
}
