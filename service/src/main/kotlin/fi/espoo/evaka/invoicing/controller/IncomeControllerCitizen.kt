// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.invoicing.service.expiringIncomes
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/income")
class IncomeControllerCitizen(
    private val accessControl: AccessControl
) {
    @GetMapping("/expiring")
    fun getExpiringIncome(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): List<LocalDate> =
        db
            .connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Person.READ_EXPIRED_INCOME_DATES,
                        user.id
                    )
                    it
                        .expiringIncomes(
                            clock.today(),
                            FiniteDateRange(clock.today(), clock.today().plusWeeks(4)),
                            null,
                            user.id
                        ).map { it.expirationDate }
                }
            }.also {
                Audit.IncomeExpirationDatesRead.log(
                    targetId = AuditId(user.id),
                    meta = mapOf("count" to it.size)
                )
            }
}
