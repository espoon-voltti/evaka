// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.kangasala.template.config

import evaka.core.decision.DecisionType
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.template.ITemplateProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemplateConfiguration {

    @Bean fun templateProvider(): ITemplateProvider = KangasalaTemplateProvider()
}

class KangasalaTemplateProvider : ITemplateProvider {
    override fun getLocalizedFilename(type: DecisionType, lang: OfficialLanguage): String =
        when (type) {
            DecisionType.CLUB -> "Kerhopäätös"

            DecisionType.DAYCARE,
            DecisionType.DAYCARE_PART_TIME -> "Varhaiskasvatuspäätös"

            DecisionType.PRESCHOOL -> "Esiopetuspäätös"

            DecisionType.PRESCHOOL_DAYCARE,
            DecisionType.PRESCHOOL_CLUB -> "Esiopetusta_täydentävän_toiminnan_päätös"

            DecisionType.PREPARATORY_EDUCATION -> "Valmistavan_opetuksen_päätös"
        }

    override fun getFeeDecisionPath(): String = "kangasala/fee-decision/decision"

    override fun getVoucherValueDecisionPath(): String =
        "kangasala/fee-decision/voucher-value-decision"

    override fun getClubDecisionPath(): String = "kangasala/club/decision"

    override fun getDaycareVoucherDecisionPath(): String = "kangasala/daycare/voucher/decision"

    override fun getDaycareTransferDecisionPath(): String = "kangasala/daycare/decision"

    override fun getDaycareDecisionPath(): String = "kangasala/daycare/decision"

    override fun getPreschoolDecisionPath(): String = "kangasala/daycare/decision"

    override fun getPreparatoryDecisionPath(): String = "kangasala/preparatory/decision"
}
