// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.template.config

import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.shared.template.ITemplateProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemplateConfiguration {
    @Bean fun templateProvider(): ITemplateProvider = EVakaOuluTemplateProvider()
}

internal class EVakaOuluTemplateProvider : ITemplateProvider {
    override fun getFeeDecisionPath(): String = "oulu/fee-decision/decision"

    override fun getVoucherValueDecisionPath(): String = "oulu/fee-decision/voucher-value-decision"

    override fun getClubDecisionPath(): String = "oulu/club/decision"

    override fun getDaycareVoucherDecisionPath(): String = "oulu/daycare/voucher/decision"

    override fun getDaycareTransferDecisionPath(): String = "oulu/daycare/decision"

    override fun getDaycareDecisionPath(): String = "oulu/daycare/decision"

    override fun getPreschoolDecisionPath(): String = "oulu/preschool/decision"

    override fun getPreparatoryDecisionPath(): String = "oulu/preparatory/decision"

    override fun getLocalizedFilename(type: DecisionType, lang: OfficialLanguage): String =
        when (type) {
            DecisionType.CLUB -> "Kerhopäätös"

            DecisionType.DAYCARE,
            DecisionType.DAYCARE_PART_TIME -> "Varhaiskasvatuspäätös"

            DecisionType.PRESCHOOL -> "Esiopetuspäätös"

            DecisionType.PRESCHOOL_DAYCARE,
            DecisionType.PRESCHOOL_CLUB -> "Esiopetukseen liittyvän toiminnan päätös"

            DecisionType.PREPARATORY_EDUCATION -> "Valmistavan opetuksen päätös"
        }
}
