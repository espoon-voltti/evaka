package fi.espoo.evaka.incomestatement

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.shared.AttachmentId
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
sealed class IncomeStatementBody(open val startDate: LocalDate, open val attachmentIds: List<AttachmentId>) {
    @JsonTypeName("HIGHEST_FEE")
    data class HighestFee(
        override val startDate: LocalDate,
        override val attachmentIds: List<AttachmentId>
    ) : IncomeStatementBody(startDate, attachmentIds)

    @JsonTypeName("GROSS")
    data class Gross(
        override val startDate: LocalDate,
        override val attachmentIds: List<AttachmentId>,
        val incomeSource: IncomeSource,
        val otherIncome: Set<OtherGrossIncome>,
    ) : IncomeStatementBody(startDate, attachmentIds)

    @JsonTypeName("ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION")
    data class EntrepreneurSelfEmployedEstimation(
        override val startDate: LocalDate,
        override val attachmentIds: List<AttachmentId>,
        val estimatedMonthlyIncome: Int,
        val incomeStartDate: LocalDate,
        val incomeEndDate: LocalDate?,
    ) : IncomeStatementBody(startDate, attachmentIds)

    @JsonTypeName("ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS")
    data class EntrepreneurSelfEmployedAttachments(
        override val startDate: LocalDate,
        override val attachmentIds: List<AttachmentId>,
    ) : IncomeStatementBody(startDate, attachmentIds)

    @JsonTypeName("ENTREPRENEUR_LIMITED_COMPANY")
    data class EntrepreneurLimitedCompany(
        override val startDate: LocalDate,
        override val attachmentIds: List<AttachmentId>,
        val incomeSource: IncomeSource
    ) : IncomeStatementBody(startDate, attachmentIds)

    @JsonTypeName("ENTREPRENEUR_PARTNERSHIP")
    data class EntrepreneurPartnership(
        override val startDate: LocalDate,
        override val attachmentIds: List<AttachmentId>,
    ) : IncomeStatementBody(startDate, attachmentIds)
}

data class Attachment(val id: AttachmentId, val name: String, val contentType: String)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "incomeType")
sealed class IncomeStatement(
    open val id: IncomeStatementId,
    open val startDate: LocalDate,
    open val attachments: List<Attachment>
) {
    @JsonTypeName("HIGHEST_FEE")
    data class HighestFee(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        override val attachments: List<Attachment>,
    ) : IncomeStatement(id, startDate, attachments)

    @JsonTypeName("GROSS")
    data class Gross(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        val incomeSource: IncomeSource,
        val otherIncome: Set<OtherGrossIncome>,
        override val attachments: List<Attachment>,
    ) : IncomeStatement(id, startDate, attachments)

    @JsonTypeName("ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION")
    data class EntrepreneurSelfEmployedEstimation(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        val estimatedMonthlyIncome: Int,
        val incomeDateRange: DateRange,
        override val attachments: List<Attachment>,
    ) : IncomeStatement(id, startDate, attachments)

    @JsonTypeName("ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS")
    data class EntrepreneurSelfEmployedAttachments(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        override val attachments: List<Attachment>,
    ) : IncomeStatement(id, startDate, attachments)

    @JsonTypeName("ENTREPRENEUR_LIMITED_COMPANY")
    data class EntrepreneurLimitedCompany(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        val incomeSource: IncomeSource,
        override val attachments: List<Attachment>,
    ) : IncomeStatement(id, startDate, attachments)

    @JsonTypeName("ENTREPRENEUR_PARTNERSHIP")
    data class EntrepreneurPartnership(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        override val attachments: List<Attachment>,
    ) : IncomeStatement(id, startDate, attachments)
}
