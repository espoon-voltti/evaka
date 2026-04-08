// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.controller

import evaka.core.Audit
import evaka.core.invoicing.FINANCE_DECISION_HANDLER_ROLES
import evaka.core.pis.Employee
import evaka.core.pis.getEmployeesByRoles
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/finance-decisions")
class FinanceDecisionController(private val accessControl: AccessControl) {

    @GetMapping("/selectable-handlers")
    fun getSelectableFinanceDecisionHandlers(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<Employee> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_SELECTABLE_FINANCE_DECISION_HANDLERS,
                    )
                    tx.getEmployeesByRoles(roles = FINANCE_DECISION_HANDLER_ROLES, unitId = null)
                }
            }
            .also { Audit.FinanceDecisionHandlersRead.log() }
    }
}
