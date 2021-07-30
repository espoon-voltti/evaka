package fi.espoo.evaka.incomestatement

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate

enum class OtherGrossIncome {
    SHIFT_WORK_ADD_ON,
    PERKS,
    SECONDARY_INCOME,
    PENSION,
    UNEMPLOYMENT_BENEFITS,
    SICKNESS_ALLOWANCE,
    PARENTAL_ALLOWANCE,
    HOME_CARE_ALLOWANCE,
    ALIMONY,
    OTHER_INCOME,
}

enum class IncomeSource {
    INCOMES_REGISTER,
    ATTACHMENTS,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "incomeType")
sealed class IncomeStatementBody(open val startDate: LocalDate) {
    @JsonTypeName("HIGHEST_FEE")
    data class HighestFee(
        override val startDate: LocalDate
    ) : IncomeStatementBody(startDate)

    @JsonTypeName("GROSS")
    data class Gross(
        override val startDate: LocalDate,
        val incomeSource: IncomeSource,
        val otherIncome: Set<OtherGrossIncome>,
    ) : IncomeStatementBody(startDate)

    @JsonTypeName("ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION")
    data class EntrepreneurSelfEmployedEstimation(
        override val startDate: LocalDate,
        val estimatedMonthlyIncome: Int,
        val incomeStartDate: LocalDate,
        val incomeEndDate: LocalDate?,
    ) : IncomeStatementBody(startDate)

    @JsonTypeName("ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS")
    data class EntrepreneurSelfEmployedAttachments(
        override val startDate: LocalDate,
    ) : IncomeStatementBody(startDate)

    @JsonTypeName("ENTREPRENEUR_LIMITED_COMPANY")
    data class EntrepreneurLimitedCompany(
        override val startDate: LocalDate,
        val incomeSource: IncomeSource
    ) : IncomeStatementBody(startDate)

    @JsonTypeName("ENTREPRENEUR_PARTNERSHIP")
    data class EntrepreneurPartnership(
        override val startDate: LocalDate,
    ) : IncomeStatementBody(startDate)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "incomeType")
sealed class IncomeStatement(open val id: IncomeStatementId, open val startDate: LocalDate) {
    @JsonTypeName("HIGHEST_FEE")
    data class HighestFee(
        override val id: IncomeStatementId,
        override val startDate: LocalDate
    ) : IncomeStatement(id, startDate)

    @JsonTypeName("GROSS")
    data class Gross(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        val incomeSource: IncomeSource,
        val otherIncome: Set<OtherGrossIncome>,
    ) : IncomeStatement(id, startDate)

    @JsonTypeName("ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION")
    data class EntrepreneurSelfEmployedEstimation(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        val estimatedMonthlyIncome: Int,
        val incomeDateRange: DateRange,
    ) : IncomeStatement(id, startDate)

    @JsonTypeName("ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS")
    data class EntrepreneurSelfEmployedAttachments(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
    ) : IncomeStatement(id, startDate)

    @JsonTypeName("ENTREPRENEUR_LIMITED_COMPANY")
    data class EntrepreneurLimitedCompany(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        val incomeSource: IncomeSource
    ) : IncomeStatement(id, startDate)

    @JsonTypeName("ENTREPRENEUR_PARTNERSHIP")
    data class EntrepreneurPartnership(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
    ) : IncomeStatement(id, startDate)
}
