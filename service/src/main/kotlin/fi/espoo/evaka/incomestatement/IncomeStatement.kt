package fi.espoo.evaka.incomestatement

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate

enum class OtherGrossIncome {
    PENSION,
    ADULT_EDUCATION_ALLOWANCE,
    SICKNESS_ALLOWANCE,
    PARENTAL_ALLOWANCE,
    HOME_CARE_ALLOWANCE,
    FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE,
    ALIMONY,
    INTEREST_AND_INVESTMENT_INCOME,
    RENTAL_INCOME,
}

enum class IncomeSource {
    INCOMES_REGISTER,
    ATTACHMENTS,
}

data class Gross(
    val incomeSource: IncomeSource,
    val estimatedIncome: EstimatedIncome?,
    val otherIncome: Set<OtherGrossIncome>,
)

data class SelfEmployed(
    val attachments: Boolean,
    val estimatedIncome: EstimatedIncome?
)

data class EstimatedIncome(
    val estimatedMonthlyIncome: Int,
    val incomeStartDate: LocalDate,
    val incomeEndDate: LocalDate?,
)

data class LimitedCompany(
    val incomeSource: IncomeSource
)

data class Accountant(
    val name: String,
    val address: String,
    val phone: String,
    val email: String,
)

data class Entrepreneur(
    val fullTime: Boolean,
    val startOfEntrepreneurship: LocalDate,
    val spouseWorksInCompany: Boolean,
    val startupGrant: Boolean,
    val selfEmployed: SelfEmployed?,
    val limitedCompany: LimitedCompany?,
    val partnership: Boolean,
    val lightEntrepreneur: Boolean,
    val accountant: Accountant?
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class IncomeStatementBody(
    open val startDate: LocalDate,
    open val endDate: LocalDate?,
) {
    @JsonTypeName("HIGHEST_FEE")
    data class HighestFee(
        override val startDate: LocalDate,
        override val endDate: LocalDate?
    ) : IncomeStatementBody(startDate, endDate)

    @JsonTypeName("INCOME")
    data class Income(
        override val startDate: LocalDate,
        override val endDate: LocalDate?,
        val gross: Gross?,
        val entrepreneur: Entrepreneur?,
        val student: Boolean,
        val alimonyPayer: Boolean,
        val otherInfo: String,
        val attachmentIds: List<AttachmentId>
    ) : IncomeStatementBody(startDate, endDate)
}

fun validateIncomeStatementBody(body: IncomeStatementBody): Boolean {
    if (body.endDate != null && body.startDate > body.endDate) return false
    return when (body) {
        is IncomeStatementBody.HighestFee -> true
        is IncomeStatementBody.Income ->
            if (body.gross == null && body.entrepreneur == null) false
            else
                body.entrepreneur.let { entrepreneur ->
                    entrepreneur == null ||
                        // At least one company type must be selected
                        (
                            (
                                entrepreneur.selfEmployed != null ||
                                    entrepreneur.limitedCompany != null ||
                                    entrepreneur.partnership ||
                                    entrepreneur.lightEntrepreneur
                                ) &&
                                // Accountant must be given if limitedCompany or partnership is selected
                                (
                                    (entrepreneur.limitedCompany == null && !entrepreneur.partnership) ||
                                        (entrepreneur.accountant != null)
                                    ) &&
                                // Accountant name, phone and email must be non-empty
                                entrepreneur.accountant.let { accountant ->
                                    accountant == null || (accountant.name != "" && accountant.phone != "" && accountant.email != "")
                                } &&
                                validateEstimatedIncome(entrepreneur.selfEmployed?.estimatedIncome)
                            )
                } && validateEstimatedIncome(body.gross?.estimatedIncome)
    }
}

private fun validateEstimatedIncome(estimatedIncome: EstimatedIncome?): Boolean =
    // Start and end dates must be in the right order
    estimatedIncome?.incomeEndDate == null || estimatedIncome.incomeStartDate <= estimatedIncome.incomeEndDate

data class Attachment(val id: AttachmentId, val name: String, val contentType: String)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class IncomeStatement(
    open val id: IncomeStatementId,
    open val startDate: LocalDate,
    open val endDate: LocalDate?,
    open val created: HelsinkiDateTime,
    open val updated: HelsinkiDateTime,
) {
    @JsonTypeName("HIGHEST_FEE")
    data class HighestFee(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        override val endDate: LocalDate?,
        override val created: HelsinkiDateTime,
        override val updated: HelsinkiDateTime,
    ) : IncomeStatement(id, startDate, endDate, created, updated)

    @JsonTypeName("INCOME")
    data class Income(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        override val endDate: LocalDate?,
        val gross: Gross?,
        val entrepreneur: Entrepreneur?,
        val student: Boolean,
        val alimonyPayer: Boolean,
        val otherInfo: String,
        override val created: HelsinkiDateTime,
        override val updated: HelsinkiDateTime,
        val attachments: List<Attachment>,
    ) : IncomeStatement(id, startDate, endDate, created, updated)
}
