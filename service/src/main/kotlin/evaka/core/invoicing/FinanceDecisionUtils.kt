// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing

import evaka.core.children.getChildIdsByGuardians
import evaka.core.children.getChildIdsByHeadsOfFamily
import evaka.core.invoicing.domain.DecisionIncome
import evaka.core.invoicing.domain.Income
import evaka.core.invoicing.domain.IncomeValue
import evaka.core.invoicing.service.IncomeCoefficientMultiplierProvider
import evaka.core.pis.getEmployeeWithRoles
import evaka.core.shared.ChildId
import evaka.core.shared.EmployeeId
import evaka.core.shared.PersonId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.NotFound
import java.math.BigDecimal
import java.math.RoundingMode

internal val FINANCE_DECISION_HANDLER_ROLES = setOf(UserRole.FINANCE_ADMIN)

fun validateFinanceDecisionHandler(tx: Database.Read, decisionHandlerId: EmployeeId) {
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
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
): DecisionIncome =
    DecisionIncome(
        id = income.id,
        effect = income.effect,
        data =
            income.data.mapValues { (_, value) ->
                calculateMonthlyAmount(
                    value.amount,
                    coefficientMultiplierProvider.multiplier(value.coefficient),
                )
            },
        totalIncome = calculateTotalIncome(income.data, coefficientMultiplierProvider),
        totalExpenses = calculateTotalExpense(income.data, coefficientMultiplierProvider),
        total = calculateIncomeTotal(income.data, coefficientMultiplierProvider),
        worksAtECHA = income.worksAtECHA,
    )

fun calculateIncomeTotal(
    data: Map<String, IncomeValue>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
): Int =
    data.entries.sumOf { (_, value) ->
        value.multiplier *
            calculateMonthlyAmount(
                value.amount,
                coefficientMultiplierProvider.multiplier(value.coefficient),
            )
    }

fun calculateTotalIncome(
    data: Map<String, IncomeValue>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
): Int =
    data.entries
        .filter { (_, value) -> value.multiplier > 0 }
        .sumOf { (_, value) ->
            value.multiplier *
                calculateMonthlyAmount(
                    value.amount,
                    coefficientMultiplierProvider.multiplier(value.coefficient),
                )
        }

fun calculateTotalExpense(
    data: Map<String, IncomeValue>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
): Int =
    data.entries
        .filter { (_, value) -> value.multiplier < 0 }
        .sumOf { (_, value) ->
            -1 *
                value.multiplier *
                calculateMonthlyAmount(
                    value.amount,
                    coefficientMultiplierProvider.multiplier(value.coefficient),
                )
        }

fun calculateMonthlyAmount(amount: Int, multiplier: BigDecimal): Int =
    (BigDecimal(amount) * multiplier).setScale(0, RoundingMode.HALF_UP).toInt()

fun partnerIsCodebtor(
    tx: Database.Read,
    partnerId: PersonId?,
    childIds: List<ChildId>,
    range: FiniteDateRange,
): Boolean {
    if (partnerId == null) return false

    val partnerAsGuardian = tx.getChildIdsByGuardians(setOf(partnerId))[partnerId] ?: emptySet()
    val partnerAsHead =
        tx.getChildIdsByHeadsOfFamily(setOf(partnerId), range)[partnerId] ?: emptyMap()
    if (partnerAsGuardian.isEmpty() && partnerAsHead.isEmpty()) return false

    return childIds.any { it in partnerAsGuardian || it in partnerAsHead }
}
