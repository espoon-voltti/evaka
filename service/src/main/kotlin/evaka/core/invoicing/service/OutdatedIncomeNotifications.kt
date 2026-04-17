// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.EmailEnv
import evaka.core.daycare.domain.Language
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.invoicing.data.insertIncome
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.invoicing.domain.IncomeRequest
import evaka.core.pis.EmailMessageType
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

private val logger = KotlinLogging.logger {}

@Service
class OutdatedIncomeNotifications(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
    private val mapper: JsonMapper,
) {
    init {
        asyncJobRunner.registerHandler(::sendEmail)
        asyncJobRunner.registerHandler(::createExpiredIncome)
    }

    fun scheduleNotifications(tx: Database.Transaction, clock: EvakaClock): Int {
        tx.removeUnclaimedJobs(
            setOf(AsyncJobType(AsyncJob.SendOutdatedIncomeNotificationEmail::class))
        )

        val guardiansForInitialNotification =
            tx.expiringIncomes(
                    clock.now().toLocalDate(),
                    FiniteDateRange(clock.today(), clock.today().plusWeeks(4)),
                    IncomeNotificationType.INITIAL_EMAIL,
                )
                .map { it.personId }

        val guardiansForReminderNotification =
            tx.expiringIncomes(
                    clock.now().toLocalDate(),
                    FiniteDateRange(clock.today(), clock.today().plusWeeks(2)),
                    IncomeNotificationType.REMINDER_EMAIL,
                )
                .filter { !guardiansForInitialNotification.contains(it.personId) }
                .map { it.personId }

        val guardiansForExpirationNotification =
            tx.expiringIncomes(
                    clock.now().toLocalDate(),
                    FiniteDateRange(clock.today().minusDays(1), clock.today().minusDays(1)),
                    IncomeNotificationType.EXPIRED_EMAIL,
                )
                .filter { !guardiansForInitialNotification.contains(it.personId) }
                .filter { !guardiansForReminderNotification.contains(it.personId) }

        asyncJobRunner.plan(
            tx,
            payloads =
                guardiansForInitialNotification
                    .map {
                        AsyncJob.SendOutdatedIncomeNotificationEmail(
                            it,
                            IncomeNotificationType.INITIAL_EMAIL,
                        )
                    }
                    .plus(
                        guardiansForReminderNotification.map {
                            AsyncJob.SendOutdatedIncomeNotificationEmail(
                                it,
                                IncomeNotificationType.REMINDER_EMAIL,
                            )
                        }
                    )
                    .plus(
                        guardiansForExpirationNotification.map {
                            AsyncJob.SendOutdatedIncomeNotificationEmail(
                                it.personId,
                                IncomeNotificationType.EXPIRED_EMAIL,
                            )
                        }
                    )
                    .plus(
                        guardiansForExpirationNotification.map {
                            AsyncJob.CreateExpiredIncome(it.personId, it.expirationDate)
                        }
                    ),
            runAt = clock.now(),
        )

        logger.info {
            "OutdatedIncomeNotification scheduled notification emails: ${guardiansForInitialNotification.size} initial, ${guardiansForReminderNotification.size} reminders and ${guardiansForExpirationNotification.size} expired"
        }

        return guardiansForInitialNotification.size +
            guardiansForReminderNotification.size +
            guardiansForExpirationNotification.size
    }

    fun sendEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendOutdatedIncomeNotificationEmail,
    ) {
        val language =
            db.read { tx ->
                tx.createQuery {
                        sql(
                            """
                            SELECT language
                            FROM person p
                            WHERE p.id = ${bind(msg.guardianId)}
                            AND email IS NOT NULL
                            """
                        )
                    }
                    .bind("guardianId", msg.guardianId)
                    .exactlyOneOrNull {
                        column<String?>("language")?.lowercase()?.let(Language::tryValueOf)
                            ?: Language.fi
                    }
            } ?: return

        logger.info {
            "OutdatedIncomeNotifications: sending ${msg.type} email to ${msg.guardianId}"
        }

        Email.create(
                dbc = db,
                emailType = EmailMessageType.INCOME_NOTIFICATION,
                personId = msg.guardianId,
                fromAddress = emailEnv.sender(language),
                content = emailMessageProvider.incomeNotification(msg.type, language),
                traceId = msg.guardianId.toString(),
            )
            ?.also { emailClient.send(it) }
            .also { db.transaction { it.createIncomeNotification(msg.guardianId, msg.type) } }
    }

    fun createExpiredIncome(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.CreateExpiredIncome,
    ) {
        db.transaction {
            val dayAfterExpiration = msg.incomeExpirationDate.plusDays(1)
            if (!it.personHasActiveIncomeOnDate(msg.guardianId, dayAfterExpiration)) {
                it.insertIncome(
                    now = clock.now(),
                    income =
                        IncomeRequest(
                            personId = msg.guardianId,
                            effect = IncomeEffect.INCOMPLETE,
                            validFrom = dayAfterExpiration,
                            validTo = null,
                            data = emptyMap(),
                            notes = "Created automatically because previous income expired",
                        ),
                    createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                )

                asyncJobRunner.plan(
                    it,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            msg.guardianId,
                            DateRange(dayAfterExpiration, null),
                        )
                    ),
                    runAt = clock.now(),
                )
            }
        }
    }
}
