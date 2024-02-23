// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.attachment.IncomeAttachment
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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
    val updatedAt: HelsinkiDateTime,
    val updatedBy: String,
    // applicationId is no longer used, but left here for historical reasons
    val applicationId: ApplicationId? = null,
    @Json val attachments: List<IncomeAttachment>,
    val totalIncome: Int,
    val totalExpenses: Int,
    val total: Int
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
    val attachments: List<IncomeAttachment> = listOf()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionIncome(
    val effect: IncomeEffect,
    val data: Map<String, Int>,
    val totalIncome: Int,
    val totalExpenses: Int,
    val total: Int,
    val worksAtECHA: Boolean
) {
    fun effectiveComparable(): DecisionIncome? {
        return when (this.effect) {
            IncomeEffect.NOT_AVAILABLE,
            IncomeEffect.INCOMPLETE -> null
            else -> this
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class IncomeValue(
    val amount: Int,
    val coefficient: IncomeCoefficient,
    val multiplier: Int,
    val monthlyAmount: Int
)

enum class IncomeEffect {
    MAX_FEE_ACCEPTED,
    INCOMPLETE,
    INCOME,
    NOT_AVAILABLE
}

data class IncomeType(
    val nameFi: String,
    val multiplier: Int,
    val withCoefficient: Boolean,
    val isSubType: Boolean
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
