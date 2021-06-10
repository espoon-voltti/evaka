// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.template

class EvakaTemplateProvider : ITemplateProvider {
    override fun getFeeDecisionPath(): String = "fee-decision/decision"
    override fun getVoucherValueDecisionPath(): String = "fee-decision/voucher-value-decision"
    override fun getClubDecisionPath(): String = "club/decision"
    override fun getDaycareVoucherDecisionPath(): String = "daycare/voucher/decision"
    override fun getDaycareTransferDecisionPath(): String = "daycare/transfer/decision"
    override fun getDaycareDecisionPath(): String = "daycare/decision"
    override fun getPreschoolDecisionPath(): String = "preschool/decision"
    override fun getPreparatoryDecisionPath(): String = "preparatory/decision"
}
