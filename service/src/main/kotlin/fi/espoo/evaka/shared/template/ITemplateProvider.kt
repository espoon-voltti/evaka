package fi.espoo.evaka.shared.template

interface ITemplateProvider {
    fun getFeeDecisionPath(): String
    fun getVoucherValueDecisionPath(): String
    fun getClubDecisionPath(): String
    fun getDaycareVoucherDecisionPath(): String
    fun getDaycareTransferDecisionPath(): String
    fun getDaycareDecisionPath(): String
    fun getPreschoolDecisionPath(): String
    fun getPreparatoryDecisionPath(): String
}
