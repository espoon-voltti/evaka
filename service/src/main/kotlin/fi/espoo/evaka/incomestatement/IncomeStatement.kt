package fi.espoo.evaka.incomestatement

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.IncomeStatementId
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

data class Gross(
    val incomeSource: IncomeSource,
    val otherIncome: Set<OtherGrossIncome>,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface SelfEmployed {
    @JsonTypeName("ATTACHMENTS")
    object Attachments : SelfEmployed {
        @JvmStatic @JsonCreator
        fun deserialize() = Attachments
    }

    @JsonTypeName("ESTIMATION")
    data class Estimation(
        val estimatedMonthlyIncome: Int,
        val incomeStartDate: LocalDate,
        val incomeEndDate: LocalDate?,
    ) : SelfEmployed
}

data class LimitedCompany(
    val incomeSource: IncomeSource
)

data class Entrepreneur(
    val selfEmployed: SelfEmployed?,
    val limitedCompany: LimitedCompany?,
    val partnership: Boolean,
    val startupGrant: Boolean
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class IncomeStatementBody(
    open val startDate: LocalDate,
) {
    @JsonTypeName("HIGHEST_FEE")
    data class HighestFee(override val startDate: LocalDate) : IncomeStatementBody(startDate)

    @JsonTypeName("INCOME")
    data class Income(
        override val startDate: LocalDate,
        val gross: Gross?,
        val entrepreneur: Entrepreneur?,
        val otherInfo: String,
        val attachmentIds: List<AttachmentId>
    ) : IncomeStatementBody(startDate)
}

data class Attachment(val id: AttachmentId, val name: String, val contentType: String)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class IncomeStatement(
    open val id: IncomeStatementId,
    open val startDate: LocalDate,
) {
    @JsonTypeName("HIGHEST_FEE")
    data class HighestFee(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
    ) : IncomeStatement(id, startDate)

    @JsonTypeName("INCOME")
    data class Income(
        override val id: IncomeStatementId,
        override val startDate: LocalDate,
        val gross: Gross?,
        val entrepreneur: Entrepreneur?,
        val otherInfo: String,
        val attachments: List<Attachment>
    ) : IncomeStatement(id, startDate)
}
