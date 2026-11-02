// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.EmailEnv
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.pis.NotificationCategory
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.webpush.CitizenPushNotification
import evaka.core.webpush.CitizenPushNotifications
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class IncompleteIncomeReminders(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val citizenPushNotifications: CitizenPushNotifications,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
) {
    init {
        asyncJobRunner.registerHandler(::sendReminder)
    }

    fun scheduleReminders(tx: Database.Transaction, clock: EvakaClock): Int {
        tx.removeUnclaimedJobs(setOf(AsyncJobType(AsyncJob.SendIncompleteIncomeReminder::class)))

        tx.setStatementTimeout(Duration.ofSeconds(300))
        val recipients = tx.incompleteIncomeReminderRecipients(clock.today())

        asyncJobRunner.plan(
            tx,
            payloads = recipients.map { AsyncJob.SendIncompleteIncomeReminder(it) },
            runAt = clock.now(),
        )

        logger.info { "IncompleteIncomeReminders scheduled reminders: ${recipients.size}" }

        return recipients.size
    }

    fun sendReminder(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendIncompleteIncomeReminder,
    ) {
        val recipient =
            db.read { tx -> tx.getIncomeNotificationRecipient(msg.guardianId) } ?: return

        val stillValidRecipient =
            db.read { tx -> tx.incompleteIncomeReminderRecipients(clock.today(), msg.guardianId) }
                .contains(msg.guardianId)
        if (!stillValidRecipient) {
            logger.info {
                "Skipping IncompleteIncomeReminder: ${msg.guardianId} is no longer valid recipient"
            }
            return
        }

        logger.info { "IncompleteIncomeReminders: sending reminder to ${msg.guardianId}" }

        if (recipient.hasEmail) {
            Email.create(
                    dbc = db,
                    category = NotificationCategory.INCOME_NOTIFICATION,
                    personId = msg.guardianId,
                    fromAddress = emailEnv.sender(recipient.language),
                    content =
                        emailMessageProvider.incomeNotification(
                            IncomeNotificationType.INCOMPLETE_INCOME_REMINDER,
                            recipient.language,
                        ),
                    traceId = msg.guardianId.toString(),
                )
                ?.also { emailClient.send(it) }
        }

        // the notification row is created even without email or push so that the calendar
        // toast is shown and the yearly throttle applies
        db.transaction { tx ->
            citizenPushNotifications.plan(
                tx,
                clock.now(),
                msg.guardianId,
                CitizenPushNotification.Income(
                    IncomeNotificationType.INCOMPLETE_INCOME_REMINDER,
                    expirationDate = null,
                    deadline = null,
                ),
            )
            tx.createIncomeNotification(
                msg.guardianId,
                IncomeNotificationType.INCOMPLETE_INCOME_REMINDER,
                created = clock.now(),
            )
        }
    }
}
