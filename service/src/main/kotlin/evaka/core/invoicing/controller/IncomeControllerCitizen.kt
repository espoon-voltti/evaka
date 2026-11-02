// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.controller

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.invoicing.service.dismissExpiringIncomeCta
import evaka.core.invoicing.service.dismissIncompleteIncomeReminderCta
import evaka.core.invoicing.service.expiringIncomes
import evaka.core.invoicing.service.getActiveIncompleteIncomeReminderCta
import evaka.core.invoicing.service.getDismissedIncomeExpirationDates
import evaka.core.invoicing.service.isOwnIncompleteIncomeReminder
import evaka.core.shared.IncomeNotificationId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/income")
class IncomeControllerCitizen(private val accessControl: AccessControl) {
    data class CitizenIncomeCtas(
        val expiringIncomeDate: LocalDate?,
        val incompleteIncomeReminder: IncomeNotificationId?,
    )

    @GetMapping("/cta")
    fun getIncomeCtas(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): CitizenIncomeCtas {
        val audit = AuditContext().add(user.id)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_INCOME_CTAS,
                        user.id,
                    )
                    val dismissedExpirationDates = tx.getDismissedIncomeExpirationDates(user.id)
                    val expiringIncomeDate =
                        tx.expiringIncomes(
                                clock.today(),
                                FiniteDateRange(clock.today(), clock.today().plusWeeks(4)),
                                null,
                                user.id,
                            )
                            .map { it.expirationDate }
                            .filterNot { it in dismissedExpirationDates }
                            .minOrNull()
                    val incompleteIncomeReminder =
                        tx.getActiveIncompleteIncomeReminderCta(user.id, clock.now())
                    CitizenIncomeCtas(expiringIncomeDate, incompleteIncomeReminder)
                }
            }
            .also { ctas ->
                ctas.incompleteIncomeReminder?.let { audit.add(it) }
                audit.log(Audit.IncomeCtasRead, clock)
            }
    }

    data class DismissExpiringIncomeCtaRequest(val expirationDate: LocalDate)

    @PostMapping("/cta/expiring-income/dismiss")
    fun dismissExpiringIncomeCta(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: DismissExpiringIncomeCtaRequest,
    ) {
        val audit = AuditContext().add(user.id).observeDate(body.expirationDate)
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.DISMISS_INCOME_CTA,
                    user.id,
                )
                tx.dismissExpiringIncomeCta(user.id, body.expirationDate)
            }
        }
        audit.log(Audit.IncomeCtaDismiss, clock)
    }

    @PostMapping("/cta/incomplete-income-reminder/{notificationId}/dismiss")
    fun dismissIncompleteIncomeReminderCta(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable notificationId: IncomeNotificationId,
    ) {
        val audit = AuditContext().add(user.id).add(notificationId)
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.DISMISS_INCOME_CTA,
                    user.id,
                )
                if (!tx.isOwnIncompleteIncomeReminder(user.id, notificationId)) throw NotFound()
                tx.dismissIncompleteIncomeReminderCta(user.id, notificationId)
            }
        }
        audit.log(Audit.IncomeCtaDismiss, clock)
    }
}
