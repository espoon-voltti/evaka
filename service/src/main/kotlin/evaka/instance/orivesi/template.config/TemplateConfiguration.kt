// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi.template.config

import evaka.core.decision.DecisionType
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.template.ITemplateProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemplateConfiguration {

    @Bean fun templateProvider(): ITemplateProvider = OrivesiTemplateProvider()
}

class OrivesiTemplateProvider : ITemplateProvider {
    override fun getLocalizedFilename(type: DecisionType, lang: OfficialLanguage): String =
        when (type) {
            DecisionType.CLUB -> throw Error("Not supported")

            DecisionType.DAYCARE,
            DecisionType.DAYCARE_PART_TIME -> "Varhaiskasvatuspäätös"

            DecisionType.PRESCHOOL -> "Esiopetuspäätös"

            DecisionType.PRESCHOOL_DAYCARE,
            DecisionType.PRESCHOOL_CLUB -> "Esiopetusta_täydentävän_toiminnan_päätös"

            DecisionType.PREPARATORY_EDUCATION -> throw Error("Not supported")
        }

    override fun getFeeDecisionPath(): String = "orivesi/fee-decision/decision"

    override fun getVoucherValueDecisionPath(): String =
        "orivesi/fee-decision/voucher-value-decision"

    override fun getClubDecisionPath(): String = throw Error("Not supported")

    override fun getDaycareVoucherDecisionPath(): String = "orivesi/daycare/voucher/decision"

    override fun getDaycareTransferDecisionPath(): String = "orivesi/daycare/decision"

    override fun getDaycareDecisionPath(): String = "orivesi/daycare/decision"

    override fun getPreschoolDecisionPath(): String = "orivesi/daycare/decision"

    override fun getPreparatoryDecisionPath(): String = throw Error("Not supported")
}
