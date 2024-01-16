// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.associateOrphanAttachments
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate

@ConstList("otherIncomes")
enum class OtherIncome {
    PENSION,
    ADULT_EDUCATION_ALLOWANCE,
    SICKNESS_ALLOWANCE,
    PARENTAL_ALLOWANCE,
    HOME_CARE_ALLOWANCE,
    FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE,
    ALIMONY,
    INTEREST_AND_INVESTMENT_INCOME,
    RENTAL_INCOME,
    UNEMPLOYMENT_ALLOWANCE,
    LABOUR_MARKET_SUBSIDY,
    ADJUSTED_DAILY_ALLOWANCE,
    JOB_ALTERNATION_COMPENSATION,
    REWARD_OR_BONUS,
    RELATIVE_CARE_SUPPORT,
    BASIC_INCOME,
    FOREST_INCOME,
    FAMILY_CARE_COMPENSATION,
    REHABILITATION,
    EDUCATION_ALLOWANCE,
    GRANT,
    APPRENTICESHIP_SALARY,
    ACCIDENT_INSURANCE_COMPENSATION,
    OTHER_INCOME
}

enum class IncomeSource {
    INCOMES_REGISTER,
    ATTACHMENTS
}

data class Gross(
    val incomeSource: IncomeSource,
    val estimatedMonthlyIncome: Int,
    val otherIncome: Set<OtherIncome>,
    val otherIncomeInfo: String
)

data class SelfEmployed(val attachments: Boolean, val estimatedIncome: EstimatedIncome?)

data class EstimatedIncome(
    val estimatedMonthlyIncome: Int,
    val incomeStartDate: LocalDate,
    val incomeEndDate: LocalDate?
)

data class LimitedCompany(val incomeSource: IncomeSource)

data class Accountant(val name: String, val address: String, val phone: String, val email: String)

data class Entrepreneur(
    val fullTime: Boolean,
    val startOfEntrepreneurship: LocalDate,
    val spouseWorksInCompany: Boolean,
    val startupGrant: Boolean,
    val checkupConsent: Boolean,
    val selfEmployed: SelfEmployed?,
    val limitedCompany: LimitedCompany?,
    val partnership: Boolean,
    val lightEntrepreneur: Boolean,
    val accountant: Accountant?
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class IncomeStatementBody(open val startDate: LocalDate, open val endDate: LocalDate?) {
    @JsonTypeName("HIGHEST_FEE")
    data class HighestFee(override val startDate: LocalDate, override val endDate: LocalDate?) :
        IncomeStatementBody(startDate, endDate)

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

    @JsonTypeName("CHILD_INCOME")
    data class ChildIncome(
        override val startDate: LocalDate,
        override val endDate: LocalDate?,
        val otherInfo: String,
        val attachmentIds: List<AttachmentId>
    ) : IncomeStatementBody(startDate, endDate)
}

fun validateIncomeStatementBody(body: IncomeStatementBody): Boolean {
    if (body.endDate != null && body.startDate > body.endDate) return false
    return when (body) {
        is IncomeStatementBody.HighestFee -> true
        is IncomeStatementBody.ChildIncome -> true
        is IncomeStatementBody.Income ->
            if (body.gross == null && body.entrepreneur == null) {
                false
            } else {
                body.entrepreneur.let { entrepreneur ->
                    entrepreneur == null ||
                        // At least one company type must be selected
                        ((entrepreneur.selfEmployed != null ||
                            entrepreneur.limitedCompany != null ||
                            entrepreneur.partnership ||
                            entrepreneur.lightEntrepreneur) &&
                            // Accountant must be given if limitedCompany or partnership is selected
                            ((entrepreneur.limitedCompany == null && !entrepreneur.partnership) ||
                                (entrepreneur.accountant != null)) &&
                            // Accountant name, phone and email must be non-empty
                            entrepreneur.accountant.let { accountant ->
                                accountant == null ||
                                    (accountant.name != "" &&
                                        accountant.phone != "" &&
                                        accountant.email != "")
                            } &&
                            validateEstimatedIncome(entrepreneur.selfEmployed?.estimatedIncome))
                }
            }
    }
}

fun createIncomeStatement(
    dbc: Database.Connection,
    incomeStatementPersonId: PersonId,
    uploadedBy: AuthenticatedUser.Citizen,
    body: IncomeStatementBody
): IncomeStatementId {
    if (!validateIncomeStatementBody(body)) throw BadRequest("Invalid income statement")

    if (
        dbc.read { tx ->
            tx.incomeStatementExistsForStartDate(incomeStatementPersonId, body.startDate)
        }
    ) {
        throw BadRequest("An income statement for this start date already exists")
    }

    return dbc.transaction { tx ->
        val incomeStatementId = tx.createIncomeStatement(incomeStatementPersonId, body)
        when (body) {
            is IncomeStatementBody.Income ->
                tx.associateOrphanAttachments(
                    uploadedBy.evakaUserId,
                    AttachmentParent.IncomeStatement(incomeStatementId),
                    body.attachmentIds
                )
            is IncomeStatementBody.ChildIncome -> {
                tx.associateOrphanAttachments(
                    uploadedBy.evakaUserId,
                    AttachmentParent.IncomeStatement(incomeStatementId),
                    body.attachmentIds
                )
            }
            else -> {}
        }
        incomeStatementId
    }
}

private fun validateEstimatedIncome(estimatedIncome: EstimatedIncome?): Boolean =
    // Start and end dates must be in the right order
    estimatedIncome?.incomeEndDate == null ||
        estimatedIncome.incomeStartDate <= estimatedIncome.incomeEndDate

data class Attachment(
    val id: AttachmentId,
    val name: String,
    val contentType: String,
    val uploadedByEmployee: Boolean
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class IncomeStatement(val type: IncomeStatementType) {
    abstract val id: IncomeStatementId
    abstract val personId: PersonId
    abstract val firstName: String
    abstract val lastName: String
    abstract val startDate: LocalDate
    abstract val endDate: LocalDate?
    abstract val created: HelsinkiDateTime
    abstract val updated: HelsinkiDateTime
    abstract val handled: Boolean
    abstract val handlerNote: String

    @JsonTypeName("HIGHEST_FEE")
    data class HighestFee(
        override val id: IncomeStatementId,
        override val personId: PersonId,
        override val firstName: String,
        override val lastName: String,
        override val startDate: LocalDate,
        override val endDate: LocalDate?,
        override val created: HelsinkiDateTime,
        override val updated: HelsinkiDateTime,
        override val handled: Boolean,
        override val handlerNote: String
    ) : IncomeStatement(IncomeStatementType.HIGHEST_FEE)

    @JsonTypeName("INCOME")
    data class Income(
        override val id: IncomeStatementId,
        override val personId: PersonId,
        override val firstName: String,
        override val lastName: String,
        override val startDate: LocalDate,
        override val endDate: LocalDate?,
        val gross: Gross?,
        val entrepreneur: Entrepreneur?,
        val student: Boolean,
        val alimonyPayer: Boolean,
        val otherInfo: String,
        override val created: HelsinkiDateTime,
        override val updated: HelsinkiDateTime,
        override val handled: Boolean,
        override val handlerNote: String,
        val attachments: List<Attachment>
    ) : IncomeStatement(IncomeStatementType.INCOME)

    @JsonTypeName("CHILD_INCOME")
    data class ChildIncome(
        override val id: IncomeStatementId,
        override val personId: PersonId,
        override val firstName: String,
        override val lastName: String,
        override val startDate: LocalDate,
        override val endDate: LocalDate?,
        val otherInfo: String,
        override val created: HelsinkiDateTime,
        override val updated: HelsinkiDateTime,
        override val handled: Boolean,
        override val handlerNote: String,
        val attachments: List<Attachment>
    ) : IncomeStatement(IncomeStatementType.CHILD_INCOME)
}
