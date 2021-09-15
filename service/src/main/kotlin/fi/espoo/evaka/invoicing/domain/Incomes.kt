// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.IncomeId
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ExcludeCodeGen
@JsonIgnoreProperties(ignoreUnknown = true)
data class Income(
    val id: IncomeId? = null,
    val personId: UUID,
    val effect: IncomeEffect,
    val data: Map<String, IncomeValue>,
    @get:JsonProperty("isEntrepreneur") val isEntrepreneur: Boolean = false,
    val worksAtECHA: Boolean = false,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val notes: String,
    val updatedAt: Instant? = null,
    val updatedBy: String? = null,
    val applicationId: ApplicationId? = null
) {
    @JsonProperty("totalIncome")
    fun totalIncome(): Int =
        data.entries
            .filter { (_, value) -> value.multiplier > 0 }
            .sumOf { (_, value) -> value.multiplier * value.monthlyAmount() }

    @JsonProperty("totalExpenses")
    fun totalExpenses(): Int =
        data.entries
            .filter { (_, value) -> value.multiplier < 0 }
            .sumOf { (_, value) -> -1 * value.multiplier * value.monthlyAmount() }

    @JsonProperty("total")
    fun total(): Int = incomeTotal(data)

    fun toDecisionIncome() = DecisionIncome(
        effect = effect,
        data = data.mapValues { (_, value) -> value.monthlyAmount() },
        totalIncome = totalIncome(),
        totalExpenses = totalExpenses(),
        total = total(),
        worksAtECHA = worksAtECHA,
        validFrom = validFrom,
        validTo = validTo
    )
}

@ExcludeCodeGen
@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionIncome(
    val effect: IncomeEffect,
    val data: Map<String, Int>,
    val totalIncome: Int,
    val totalExpenses: Int,
    val total: Int,
    val worksAtECHA: Boolean = false,
    val validFrom: LocalDate?,
    val validTo: LocalDate?
)

fun incomeTotal(data: Map<String, IncomeValue>) = data.entries
    .sumOf { (_, value) -> value.multiplier * value.monthlyAmount() }

@JsonIgnoreProperties(ignoreUnknown = true)
data class IncomeValue(val amount: Int, val coefficient: IncomeCoefficient, val multiplier: Int) {
    @JsonProperty("monthlyAmount")
    fun monthlyAmount(): Int = (BigDecimal(amount) * coefficient.multiplier()).setScale(0, RoundingMode.HALF_UP).toInt()
}

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

    // values are taken from Effica
    fun multiplier(): BigDecimal = when (this) {
        MONTHLY_WITH_HOLIDAY_BONUS -> BigDecimal("1.0417") // = 12.5 / 12
        MONTHLY_NO_HOLIDAY_BONUS -> BigDecimal("1.0000") // = 12 / 12
        BI_WEEKLY_WITH_HOLIDAY_BONUS -> BigDecimal("2.2323") // = ???
        BI_WEEKLY_NO_HOLIDAY_BONUS -> BigDecimal("2.1429") // = ???
        DAILY_ALLOWANCE_21_5 -> BigDecimal("21.5")
        DAILY_ALLOWANCE_25 -> BigDecimal("25")
        YEARLY -> BigDecimal("0.0833") // 1 / 12
    }
}
