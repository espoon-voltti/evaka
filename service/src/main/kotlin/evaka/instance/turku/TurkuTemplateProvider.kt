// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku

import evaka.core.decision.DecisionType
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.template.ITemplateProvider

class TurkuTemplateProvider : ITemplateProvider {
    override fun getFeeDecisionPath(): String = "fee-decision/decision"

    override fun getVoucherValueDecisionPath(): String = "fee-decision/voucher-value-decision"

    override fun getClubDecisionPath(): String = "club/decision"

    override fun getDaycareVoucherDecisionPath(): String = "daycare/voucher/decision"

    override fun getDaycareTransferDecisionPath(): String = "daycare/decision"

    override fun getDaycareDecisionPath(): String = "daycare/decision"

    override fun getPreschoolDecisionPath(): String = "preschool/decision"

    override fun getPreparatoryDecisionPath(): String = "preparatory/decision"

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
