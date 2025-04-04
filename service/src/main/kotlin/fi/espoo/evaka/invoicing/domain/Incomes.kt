// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.attachment.Attachment
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate
import org.jdbi.v3.json.Json

@JsonIgnoreProperties(ignoreUnknown = true)
data class Income(
    val id: IncomeId,
    val personId: PersonId,
    val effect: IncomeEffect,
    val data: Map<String, IncomeValue>,
    @get:JsonProperty("isEntrepreneur") val isEntrepreneur: Boolean = false,
    val worksAtECHA: Boolean = false,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val notes: String,
    val createdAt: HelsinkiDateTime,
    val createdBy: EvakaUser,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: EvakaUser,
    // applicationId is no longer used, but left here for historical reasons
    val applicationId: ApplicationId? = null,
    @Json val attachments: List<Attachment>,
    val totalIncome: Int,
    val totalExpenses: Int,
    val total: Int,
)

data class IncomeRequest(
    val personId: PersonId,
    val effect: IncomeEffect,
    val data: Map<String, IncomeValue>,
    @get:JsonProperty("isEntrepreneur") val isEntrepreneur: Boolean = false,
    val worksAtECHA: Boolean = false,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val notes: String,
    val attachments: List<Attachment> = listOf(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionIncome(
    val id: IncomeId? = null,
    val effect: IncomeEffect,
    val data: Map<String, Int>,
    val totalIncome: Int,
    val totalExpenses: Int,
    val total: Int,
    val worksAtECHA: Boolean,
)

fun decisionIncomesEqual(income1: DecisionIncome?, income2: DecisionIncome?): Boolean {
    if (income1?.id != null && income2?.id != null) {
        // New logic: Incomes with different IDs are considered different
        return income1 == income2
    }

    // Old logic: compare contents only
    val i1 =
        income1?.takeIf {
            it.effect != IncomeEffect.NOT_AVAILABLE && it.effect != IncomeEffect.INCOMPLETE
        }
    val i2 =
        income2?.takeIf {
            it.effect != IncomeEffect.NOT_AVAILABLE && it.effect != IncomeEffect.INCOMPLETE
        }
    return i1?.copy(id = null) == i2?.copy(id = null)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class IncomeValue(
    val amount: Int,
    val coefficient: IncomeCoefficient,
    val multiplier: Int,
    val monthlyAmount: Int,
)

enum class IncomeEffect : DatabaseEnum {
    MAX_FEE_ACCEPTED,
    INCOMPLETE,
    INCOME,
    NOT_AVAILABLE;

    override val sqlType: String = "income_effect"
}

data class IncomeType(
    val nameFi: String,
    val multiplier: Int,
    val withCoefficient: Boolean,
    val isSubType: Boolean,
)

@ConstList("incomeCoefficients")
enum class IncomeCoefficient {
    MONTHLY_WITH_HOLIDAY_BONUS,
    MONTHLY_NO_HOLIDAY_BONUS,
    BI_WEEKLY_WITH_HOLIDAY_BONUS,
    BI_WEEKLY_NO_HOLIDAY_BONUS,
    DAILY_ALLOWANCE_21_5,
    DAILY_ALLOWANCE_25,
    YEARLY;

    companion object {
        fun default(): IncomeCoefficient = MONTHLY_NO_HOLIDAY_BONUS
    }
}
