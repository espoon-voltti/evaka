// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.pis.getEmployeeWithRoles
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound

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
