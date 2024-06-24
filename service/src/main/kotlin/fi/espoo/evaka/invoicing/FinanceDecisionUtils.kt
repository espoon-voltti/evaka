// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.service.IncomeCoefficientMultiplierProvider
import fi.espoo.evaka.pis.getEmployeeWithRoles
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import java.math.BigDecimal
import java.math.RoundingMode

internal val FINANCE_DECISION_HANDLER_ROLES = setOf(UserRole.FINANCE_ADMIN)

fun validateFinanceDecisionHandler(
    tx: Database.Read,
    decisionHandlerId: EmployeeId
) {
    val employee =
        tx.getEmployeeWithRoles(decisionHandlerId)
            ?: throw NotFound("Decision handler $decisionHandlerId not found")
    if (
        employee.globalRoles.isEmpty() ||
        employee.globalRoles.all { role -> !FINANCE_DECISION_HANDLER_ROLES.contains(role) }
    ) {
        throw BadRequest("Decision handler $decisionHandlerId is not finance admin")
    }
}

fun mapIncomeToDecisionIncome(
    income: Income,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider
): DecisionIncome =
    DecisionIncome(
        effect = income.effect,
        data =
            income.data.mapValues { (_, value) ->
                calculateMonthlyAmount(
                    value.amount,
                    coefficientMultiplierProvider.multiplier(value.coefficient)
                )
            },
        totalIncome = calculateTotalIncome(income.data, coefficientMultiplierProvider),
        totalExpenses = calculateTotalExpense(income.data, coefficientMultiplierProvider),
        total = calculateIncomeTotal(income.data, coefficientMultiplierProvider),
        worksAtECHA = income.worksAtECHA
    )

fun calculateIncomeTotal(
    data: Map<String, IncomeValue>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider
): Int =
    data.entries.sumOf { (_, value) ->
        value.multiplier *
            calculateMonthlyAmount(
                value.amount,
                coefficientMultiplierProvider.multiplier(value.coefficient)
            )
    }

fun calculateTotalIncome(
    data: Map<String, IncomeValue>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider
): Int =
    data.entries
        .filter { (_, value) -> value.multiplier > 0 }
        .sumOf { (_, value) ->
            value.multiplier *
                calculateMonthlyAmount(
                    value.amount,
                    coefficientMultiplierProvider.multiplier(value.coefficient)
                )
        }

fun calculateTotalExpense(
    data: Map<String, IncomeValue>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider
): Int =
    data.entries
        .filter { (_, value) -> value.multiplier < 0 }
        .sumOf { (_, value) ->
            -1 *
                value.multiplier *
                calculateMonthlyAmount(
                    value.amount,
                    coefficientMultiplierProvider.multiplier(value.coefficient)
                )
        }

fun calculateMonthlyAmount(
    amount: Int,
    multiplier: BigDecimal
): Int = (BigDecimal(amount) * multiplier).setScale(0, RoundingMode.HALF_UP).toInt()
