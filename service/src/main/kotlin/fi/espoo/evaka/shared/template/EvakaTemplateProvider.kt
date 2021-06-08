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
