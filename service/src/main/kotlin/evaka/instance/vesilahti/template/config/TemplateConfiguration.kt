// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.vesilahti.template.config

import evaka.core.decision.DecisionType
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.template.ITemplateProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemplateConfiguration {

    @Bean fun templateProvider(): ITemplateProvider = VesilahtiTemplateProvider()
}

class VesilahtiTemplateProvider : ITemplateProvider {
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

    override fun getFeeDecisionPath(): String = "vesilahti/fee-decision/decision"

    override fun getVoucherValueDecisionPath(): String = throw UnsupportedOperationException()

    override fun getClubDecisionPath(): String = "vesilahti/club/decision"

    override fun getDaycareVoucherDecisionPath(): String = throw UnsupportedOperationException()

    override fun getDaycareTransferDecisionPath(): String = "vesilahti/daycare/decision"

    override fun getDaycareDecisionPath(): String = "vesilahti/daycare/decision"

    override fun getPreschoolDecisionPath(): String = throw UnsupportedOperationException()

    override fun getPreparatoryDecisionPath(): String = "vesilahti/preparatory/decision"
}
