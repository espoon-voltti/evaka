// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.FINANCE_DECISION_HANDLER_ROLES
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.getEmployeesByRoles
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    "/finance-decisions", // deprecated
    "/employee/finance-decisions",
)
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
