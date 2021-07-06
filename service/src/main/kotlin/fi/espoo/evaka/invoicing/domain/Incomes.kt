// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.IncomeId
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class Income(
    val id: IncomeId? = null,
    val personId: UUID,
    val effect: IncomeEffect,
    val data: Map<IncomeType, IncomeValue>,
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
            .filter { (type, _) -> type.multiplier > 0 }
            .sumOf { (type, value) -> type.multiplier * value.monthlyAmount() }

    @JsonProperty("totalExpenses")
    fun totalExpenses(): Int =
        data.entries
            .filter { (type, _) -> type.multiplier < 0 }
            .sumOf { (type, value) -> -1 * type.multiplier * value.monthlyAmount() }

    @JsonProperty("total")
    fun total(): Int = incomeTotal(data)

    fun toDecisionIncome() = DecisionIncome(
        effect = effect,
        data = data.mapValues { (_, value) -> value.monthlyAmount() },
        total = total(),
        worksAtECHA = worksAtECHA,
        validFrom = validFrom,
        validTo = validTo
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionIncome(
    val effect: IncomeEffect,
    val data: Map<IncomeType, Int>,
    val total: Int,
    val worksAtECHA: Boolean = false,
    val validFrom: LocalDate?,
    val validTo: LocalDate?
) {
    @JsonProperty("totalIncome")
    fun totalIncome(): Int =
        data.entries
            .filter { (type, _) -> type.multiplier > 0 }
            .sumOf { (type, value) -> type.multiplier * value }

    @JsonProperty("totalExpenses")
    fun totalExpenses(): Int =
        data.entries
            .filter { (type, _) -> type.multiplier < 0 }
            .sumOf { (type, value) -> -1 * type.multiplier * value }
}

fun incomeTotal(data: Map<IncomeType, IncomeValue>) = data.entries
    .sumOf { (type, value) -> type.multiplier * value.monthlyAmount() }

@JsonIgnoreProperties(ignoreUnknown = true)
data class IncomeValue(val amount: Int, val coefficient: IncomeCoefficient) {
    @JsonProperty("monthlyAmount")
    fun monthlyAmount(): Int = (BigDecimal(amount) * coefficient.multiplier()).setScale(0, RoundingMode.HALF_UP).toInt()
}

enum class IncomeEffect {
    MAX_FEE_ACCEPTED,
    INCOMPLETE,
    INCOME,
    NOT_AVAILABLE
}

enum class IncomeType(val multiplier: Int) {
    MAIN_INCOME(1),
    SHIFT_WORK_ADD_ON(1),
    PERKS(1),
    SECONDARY_INCOME(1),
    PENSION(1),
    UNEMPLOYMENT_BENEFITS(1),
    SICKNESS_ALLOWANCE(1),
    PARENTAL_ALLOWANCE(1),
    HOME_CARE_ALLOWANCE(1),
    ALIMONY(1),
    OTHER_INCOME(1),
    ALL_EXPENSES(-1);
}

enum class IncomeCoefficient {
    MONTHLY_WITH_HOLIDAY_BONUS,
    MONTHLY_NO_HOLIDAY_BONUS,
    BI_WEEKLY_WITH_HOLIDAY_BONUS,
    BI_WEEKLY_NO_HOLIDAY_BONUS,
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
        YEARLY -> BigDecimal("0.0833") // 1 / 12
    }
}
