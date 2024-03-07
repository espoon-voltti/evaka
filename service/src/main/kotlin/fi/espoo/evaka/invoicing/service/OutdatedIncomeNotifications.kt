// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.invoicing.data.insertIncome
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeRequest
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class OutdatedIncomeNotifications(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
    private val mapper: JsonMapper
) {
    init {
        asyncJobRunner.registerHandler(::sendEmail)
    }

    fun scheduleNotifications(tx: Database.Transaction, clock: EvakaClock): Int {
        tx.removeUnclaimedJobs(
            setOf(AsyncJobType(AsyncJob.SendOutdatedIncomeNotificationEmail::class))
        )

        val guardiansForInitialNotification =
            tx.expiringIncomes(
                    clock.now().toLocalDate(),
                    FiniteDateRange(clock.today(), clock.today().plusWeeks(4)),
                    IncomeNotificationType.INITIAL_EMAIL
                )
                .map { it.personId }

        val guardiansForReminderNotification =
            tx.expiringIncomes(
                    clock.now().toLocalDate(),
                    FiniteDateRange(clock.today(), clock.today().plusWeeks(2)),
                    IncomeNotificationType.REMINDER_EMAIL
                )
                .filter { !guardiansForInitialNotification.contains(it.personId) }
                .map { it.personId }

        val guardiansForExpirationNotification =
            tx.expiringIncomes(
                    clock.now().toLocalDate(),
                    FiniteDateRange(clock.today(), clock.today()),
                    IncomeNotificationType.EXPIRED_EMAIL
                )
                .filter { !guardiansForInitialNotification.contains(it.personId) }
                .filter { !guardiansForReminderNotification.contains(it.personId) }
                .map { it.personId }

        asyncJobRunner.plan(
            tx,
            payloads =
                guardiansForInitialNotification
                    .map {
                        AsyncJob.SendOutdatedIncomeNotificationEmail(
                            it,
                            IncomeNotificationType.INITIAL_EMAIL
                        )
                    }
                    .plus(
                        guardiansForReminderNotification.map {
                            AsyncJob.SendOutdatedIncomeNotificationEmail(
                                it,
                                IncomeNotificationType.REMINDER_EMAIL
                            )
                        }
                    )
                    .plus(
                        guardiansForExpirationNotification.map {
                            AsyncJob.SendOutdatedIncomeNotificationEmail(
                                it,
                                IncomeNotificationType.EXPIRED_EMAIL
                            )
                        }
                    ),
            runAt = clock.now()
        )

        logger.info(
            "OutdatedIncomeNotification scheduled notification emails: ${guardiansForInitialNotification.size } initial, ${guardiansForReminderNotification.size} reminders and ${guardiansForExpirationNotification.size} expired"
        )

        return guardiansForInitialNotification.size +
            guardiansForReminderNotification.size +
            guardiansForExpirationNotification.size
    }

    fun sendEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendOutdatedIncomeNotificationEmail
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

        logger.info("OutdatedIncomeNotifications: sending ${msg.type} email to ${msg.guardianId}")

        Email.create(
                dbc = db,
                emailType = EmailMessageType.OUTDATED_INCOME_NOTIFICATION,
                personId = msg.guardianId,
                fromAddress = emailEnv.sender(language),
                content = emailMessageProvider.incomeNotification(msg.type, language),
                traceId = msg.guardianId.toString()
            )
            ?.also { emailClient.send(it) }

        db.transaction {
            it.createIncomeNotification(msg.guardianId, msg.type)

            val firstDayAfterExpiration = clock.today().plusDays(1)
            if (
                msg.type == IncomeNotificationType.EXPIRED_EMAIL &&
                    !it.personHasActiveIncomeOnDate(msg.guardianId, firstDayAfterExpiration)
            ) {
                it.insertIncome(
                    clock = clock,
                    mapper = mapper,
                    income =
                        IncomeRequest(
                            personId = msg.guardianId,
                            effect = IncomeEffect.INCOMPLETE,
                            validFrom = firstDayAfterExpiration,
                            validTo = null,
                            data = emptyMap(),
                            notes = "Created automatically because previous income expired"
                        ),
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId
                )

                asyncJobRunner.plan(
                    it,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            msg.guardianId,
                            DateRange(firstDayAfterExpiration, null)
                        )
                    ),
                    runAt = clock.now()
                )
            }
        }
    }
}
