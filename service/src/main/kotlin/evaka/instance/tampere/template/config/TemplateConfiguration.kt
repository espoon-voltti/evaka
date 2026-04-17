// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.template.config

import evaka.core.decision.DecisionType
import evaka.core.shared.config.pdfTemplateEngine
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.template.ITemplateProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemplateConfiguration {
    @Bean fun templateEngine() = pdfTemplateEngine("tampere")

    @Bean fun templateProvider(): ITemplateProvider = TampereTemplateProvider()
}

internal class TampereTemplateProvider : ITemplateProvider {
    override fun getLocalizedFilename(type: DecisionType, lang: OfficialLanguage): String =
        when (type) {
            DecisionType.CLUB -> "Kerhopäätös"

            DecisionType.DAYCARE,
            DecisionType.DAYCARE_PART_TIME -> "Varhaiskasvatuspäätös"

            DecisionType.PRESCHOOL -> "Esiopetuspäätös"

            DecisionType.PRESCHOOL_DAYCARE,
            DecisionType.PRESCHOOL_CLUB -> "Esiopetusta_täydentävän_toiminnan_päätös"

            DecisionType.PREPARATORY_EDUCATION -> throw Error("Not supported")
        }

    override fun getFeeDecisionPath(): String = "fee-decision/decision"

    override fun getVoucherValueDecisionPath(): String = "fee-decision/voucher-value-decision"

    override fun getClubDecisionPath(): String = "club/decision"

    override fun getDaycareVoucherDecisionPath(): String = "daycare/voucher/decision"

    override fun getDaycareTransferDecisionPath(): String = "daycare/decision"

    override fun getDaycareDecisionPath(): String = "daycare/decision"

    override fun getPreschoolDecisionPath(): String = "daycare/decision"

    override fun getPreparatoryDecisionPath(): String = throw Error("Not supported")
}
