// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.template

import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.shared.domain.OfficialLanguage

class EvakaTemplateProvider : ITemplateProvider {
    override fun getLocalizedFilename(
        type: DecisionType,
        lang: OfficialLanguage
    ): String =
        when (lang) {
            OfficialLanguage.SV ->
                when (type) {
                    DecisionType.CLUB -> "Kerhopäätös" // All clubs are in Finnish
                    DecisionType.DAYCARE,
                    DecisionType.DAYCARE_PART_TIME -> "Beslut_om_småbarnspedagogisk_verksamhet"
                    DecisionType.PRESCHOOL -> "Beslut_om_förskoleplats"
                    DecisionType.PRESCHOOL_DAYCARE -> "Anslutande_småbarnspedagogik"
                    DecisionType.PRESCHOOL_CLUB -> "Esiopetuksen_kerhopäätös (sv)"
                    DecisionType.PREPARATORY_EDUCATION ->
                        "Valmistava_päätös" // Svebi does not offer preparatory education
                }
            else ->
                when (type) {
                    DecisionType.CLUB -> "Kerhopäätös"
                    DecisionType.DAYCARE,
                    DecisionType.DAYCARE_PART_TIME -> "Varhaiskasvatuspäätös"
                    DecisionType.PRESCHOOL -> "Esiopetuspäätös"
                    DecisionType.PRESCHOOL_DAYCARE -> "Liittyvä_varhaiskasvatuspäätös"
                    DecisionType.PRESCHOOL_CLUB -> "Esiopetuksen_kerhopäätös"
                    DecisionType.PREPARATORY_EDUCATION -> "Valmistava_päätös"
                }
        }

    override fun getFeeDecisionPath(): String = "fee-decision/decision"

    override fun getVoucherValueDecisionPath(): String = "fee-decision/voucher-value-decision"

    override fun getClubDecisionPath(): String = "club/decision"

    override fun getDaycareVoucherDecisionPath(): String = "daycare/voucher/decision"

    override fun getDaycareTransferDecisionPath(): String = "daycare/transfer/decision"

    override fun getDaycareDecisionPath(): String = "daycare/decision"

    override fun getPreschoolDecisionPath(): String = "preschool/decision"

    override fun getPreparatoryDecisionPath(): String = "preparatory/decision"

    override fun getAssistanceNeedDecisionPath(): String = "assistance-need/decision"

    override fun getAssistanceNeedPreschoolDecisionPath(): String = "assistance-need-preschool/decision"
}
