// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.controller

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.invoicing.service.expiringIncomes
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/income")
class IncomeControllerCitizen(private val accessControl: AccessControl) {
    @GetMapping("/expiring")
    fun getExpiringIncome(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<LocalDate> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_EXPIRED_INCOME_DATES,
                        user.id,
                    )
                    tx.expiringIncomes(
                            clock.today(),
                            FiniteDateRange(clock.today(), clock.today().plusWeeks(4)),
                            null,
                            user.id,
                        )
                        .map { it.expirationDate }
                }
            }
            .also {
                Audit.IncomeExpirationDatesRead.log(
                    targetId = AuditId(user.id),
                    meta = mapOf("count" to it.size),
                )
            }
    }
}
